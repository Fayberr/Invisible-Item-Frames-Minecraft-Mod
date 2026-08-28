package net.fayber.invisibleitemframes;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fayber.invisibleitemframes.client.InvisibleItemFramesClient;
import net.fayber.invisibleitemframes.client.InvisibleItemFramesClientKeybind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Handles the three-gesture interaction model for item frames - see
// GestureResolver for what plain/shift/keybind resolve to.
//
// UseEntityCallback fires on both client and server in fabric-api 26.x
// (fabric's client MinecraftMixin calls it during startUseItem, before
// MultiPlayerGameMode.interact even runs), so we can check the keybind
// state right here in the client branch without needing a mixin. For any
// non-PLAIN gesture the client sends a payload and returns FAIL, which
// cancels startUseItem before the vanilla interact packet goes out - so
// nothing gets rotated/clicked-through client-side, the server just acts on
// our payload. Vanilla (unmodded) clients only ever produce PLAIN/SHIFT via
// the normal packet, which the server branch below still handles fine.
//
// Frame visibility just reuses vanilla's Entity#setInvisible/isInvisible -
// the same flag that makes a frame placed with Silent+Invisible NBT
// invisible in survival. So visibility syncs and saves for free; only the
// frame model is hidden, the held item and its rotation still render.
public final class ItemFrameInteractionHandler {
    private ItemFrameInteractionHandler() {}

    public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand,
                                                  Entity entity, EntityHitResult hitResult) {
        if (!(entity instanceof ItemFrame frame)) {
            return InteractionResult.PASS;
        }

        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        boolean isGlow = frame instanceof GlowItemFrame;
        if (isGlow && !config.affectGlowItemFrames) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean handEmpty = held.isEmpty();
        boolean shiftDown = player.isShiftKeyDown();
        // Keybind state only exists client-side; the server never sees it
        // directly, only through the payloads the client sends below.
        boolean keybindDown = level.isClientSide()
                && InvisibleItemFramesClientKeybind.isDown(config);

        GestureResolver.Gesture gesture = GestureResolver.resolve(
                shiftDown, keybindDown, config.swapKeybindAndSneakRoles, handEmpty,
                config.requireEmptyHandForToggle);

        if (gesture == GestureResolver.Gesture.TOGGLE) {
            if (!config.enableItemFrameToggle) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                InvisibleItemFramesClient.sendToggleFrame(frame.getId());
                return InteractionResult.FAIL;
            }
            if (SignInteractionHandler.hasTogglePermission(player, config)) {
                toggleFrame(frame);
            }
            return InteractionResult.SUCCESS;
        }

        boolean clickThroughWouldApply = frame.isInvisible()
                ? config.clickThroughInvisibleFrames
                : config.clickThroughVisibleFrames;

        if (gesture == GestureResolver.Gesture.INTERACT) {
            // Only the keybind (swap on) needs to force past click-through;
            // shift's plain "interact" role (swap off) is already what PASS
            // does on its own. require_empty_hand_for_interaction (default
            // on) gates this override the same way it gates the sign editor
            // open below, so the two settings behave consistently.
            boolean interactHandOk = !config.requireEmptyHandForInteraction || handEmpty;
            if (keybindDown && clickThroughWouldApply && interactHandOk) {
                InvisibleItemFramesClient.sendForceInteractFrame(frame.getId());
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }

        // gesture == PLAIN
        if (clickThroughWouldApply) {
            BlockPos framePos = BlockPos.containing(frame.getX(), frame.getY(), frame.getZ());
            if (level.isClientSide()) {
                if (findClickThroughTarget(player, level, framePos) == null) {
                    return InteractionResult.PASS;
                }
                InvisibleItemFramesClient.sendClickThroughFrame(frame.getId());
                return InteractionResult.FAIL;
            }
            return clickThrough(player, level, framePos);
        }

        return InteractionResult.PASS;
    }

    /** Flips the frame's invisibility flag; caller has validated everything. */
    static void toggleFrame(ItemFrame frame) {
        frame.setInvisible(!frame.isInvisible());
    }

    // used by the network receiver when the keybind (swap on) overrides click-through -
    // runs vanilla's own frame interact directly, skipping our click-through logic
    public static void forceInteract(Player player, ItemFrame frame, InteractionHand hand) {
        frame.interact(player, hand, Vec3.ZERO);
    }

    // forwards to whatever block the crosshair reaches beyond the frame; server only
    static InteractionResult clickThrough(Player player, Level level, BlockPos framePos) {
        BlockHitResult target = findClickThroughTarget(player, level, framePos);
        if (target == null) {
            return InteractionResult.PASS;
        }
        return forwardBlockInteraction(player, level, target);
    }

    // guards against our own onUseBlock reacting a second time to a click-through
    // forward that happens to land on another sign; single-threaded server tick,
    // so a plain flag is enough.
    private static boolean forwardingInteraction = false;

    static boolean isForwardingInteraction() {
        return forwardingInteraction;
    }

    // Re-fires UseBlockCallback for the block reached by click-through instead of
    // calling useWithoutItem() on it directly, so claim/protection mods listening
    // on that event still get a say for the block behind the frame or sign. Only
    // falls back to vanilla's own handling if nothing on the chain claimed it.
    static InteractionResult forwardBlockInteraction(Player player, Level level, BlockHitResult target) {
        forwardingInteraction = true;
        try {
            InteractionResult result = UseBlockCallback.EVENT.invoker()
                    .interact(player, level, InteractionHand.MAIN_HAND, target);
            if (result != InteractionResult.PASS) {
                return result;
            }
            return level.getBlockState(target.getBlockPos())
                    .useWithoutItem(level, player, target);
        } finally {
            forwardingInteraction = false;
        }
    }

    // Walks the crosshair ray out to reach and returns the first block BEYOND
    // skippedPos (the frame/sign's own position), or null if there isn't one.
    // Starting from the eye and skipping the frame/sign position works no matter
    // how it's mounted (wall/ceiling/standing). We used to ray-cast from just past
    // the original hit point instead, but that could start inside the sign's own
    // hitbox and re-hit the sign itself, making click-through open the sign editor
    // instead of reaching the block behind it.
    static BlockHitResult findClickThroughTarget(Player player, Level level, BlockPos skippedPos) {
        Vec3 look = player.getViewVector(1.0F);
        double reach = player.blockInteractionRange() + 1.0;
        Vec3 end = player.getEyePosition().add(look.scale(reach));
        Vec3 from = player.getEyePosition();

        // Re-raycast a few times so a hit on the skipped position (including
        // hits starting inside its own hitbox) always makes forward progress.
        for (int i = 0; i < 8; i++) {
            BlockHitResult hit = level.clip(
                    new ClipContext(from, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK) {
                return null;
            }
            if (!hit.getBlockPos().equals(skippedPos)) {
                return hit;
            }
            from = hit.getLocation().add(look.scale(0.11));
        }
        return null;
    }
}
