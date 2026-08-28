package net.fayber.invisibleitemframes;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
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

// Same three-gesture model as signs: plain right-click (click-through if
// enabled, otherwise rotate the held item), shift + right-click (vanilla
// interact, or toggle if swapped), keybind + right-click (toggle, or forced
// interact if swapped). See GestureResolver for the shared logic.
//
// UseEntityCallback fires on both sides, and fabric's own client mixin
// invokes it at the entity-interact step of startUseItem before
// MultiPlayerGameMode.interact runs, so the keybind check (client-only, real
// GLFW key state) lives right in the client branch below - frames need no
// extra mixin. On a non-PLAIN gesture the client sends a payload and returns
// FAIL, which cancels startUseItem before the vanilla interact call (nothing
// rotated or clicked through), and the server acts on the payload alone.
//
// Frame visibility just reuses vanilla's own Entity.setInvisible/isInvisible
// - the same flag that makes a frame placed with Silent+Invisible NBT
// invisible in survival - so it's synced and saved for free; the held item
// keeps rendering, only the frame's own model is hidden.
public final class ItemFrameInteractionHandler {
    private ItemFrameInteractionHandler() {}

    // set while we're replaying UseEntityCallback below purely to ask other
    // mods whether they'd allow the click; see the matching guard in
    // SignInteractionHandler for why this is needed
    private static boolean replaying = false;

    public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand,
                                                  Entity entity, EntityHitResult hitResult) {
        if (replaying) {
            return InteractionResult.PASS;
        }
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

    // flips the frame's invisibility flag; caller has validated everything
    static void toggleFrame(ItemFrame frame) {
        frame.setInvisible(!frame.isInvisible());
    }

    // forced-interact used by the network receiver when the keybind (swap on)
    // overrides click-through: runs vanilla's own frame-interact handling
    // directly, bypassing this mod's click-through logic entirely
    public static void forceInteract(Player player, ItemFrame frame, InteractionHand hand) {
        frame.interact(player, hand, Vec3.ZERO);
    }

    // forwards the interaction to whatever block the crosshair ray reaches
    // beyond the frame. runs on the server only
    static InteractionResult clickThrough(Player player, Level level, BlockPos framePos) {
        BlockHitResult target = findClickThroughTarget(player, level, framePos);
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (!SignInteractionHandler.otherModsAllow(player, level, target.getBlockPos())) {
            return InteractionResult.PASS;
        }
        return level.getBlockState(target.getBlockPos())
                .useWithoutItem(level, player, target);
    }

    // Replays UseEntityCallback for the frame with our own listener silenced,
    // so other mods hooked into the same event (land claim / protection mods)
    // still get a say before the network-triggered toggle/force-interact below
    // mutates anything. Those actions run outside the normal click flow, so
    // without this a keybind click could bypass a claim that would otherwise
    // block interacting with the frame.
    static boolean otherModsAllow(Player player, Level level, Entity entity) {
        EntityHitResult hit = new EntityHitResult(entity);
        replaying = true;
        try {
            return UseEntityCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, entity, hit)
                    == InteractionResult.PASS;
        } finally {
            replaying = false;
        }
    }

    // walks the player's crosshair ray out to their reach and returns the
    // first block BEYOND skippedPos, or null if there is none.
    //
    // the ray starts at the eye and skips over the frame's or sign's own
    // position, so the result is exactly "what would this click have reached
    // if the frame or sign were not there". works regardless of how the frame
    // or sign is mounted (wall, ceiling, standing). raycasting from just past
    // the original hit point instead can start inside the sign's own hitbox
    // and re-hit the sign, making click-through open the sign editor instead
    // of reaching the block behind it - hence the loop below.
    static BlockHitResult findClickThroughTarget(Player player, Level level, BlockPos skippedPos) {
        Vec3 look = player.getViewVector(1.0F);
        double reach = player.blockInteractionRange() + 1.0;
        Vec3 end = player.getEyePosition().add(look.scale(reach));
        Vec3 from = player.getEyePosition();

        // re-raycast a few times so a hit on the skipped position (including
        // hits starting inside its own hitbox) always makes forward progress
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
