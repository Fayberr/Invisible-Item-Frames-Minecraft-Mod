package net.fayber.invisibleitemframes;

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

/**
 * Handles the three-gesture interaction model for item frames:
 *
 * <ul>
 *   <li>Plain right-click: click-through if enabled for the frame's current
 *       visibility state, otherwise vanilla (rotate the held item).</li>
 *   <li>Shift + right-click: vanilla interact, or toggle visibility if the
 *       swap option is enabled.</li>
 *   <li>Keybind held + right-click: toggle visibility, or forced vanilla
 *       interact (even over click-through) if the swap option is enabled.</li>
 * </ul>
 *
 * See {@link GestureResolver} for the shared resolution logic (also used by
 * {@link SignInteractionHandler}).
 *
 * <p>Fabric's {@code UseEntityCallback} fires on both sides in fabric-api
 * 26.x: fabric's own client {@code MinecraftMixin} invokes the event at the
 * entity-interact step of {@code startUseItem}, before
 * {@code MultiPlayerGameMode.interact} runs at all. So the keybind check
 * (client-only, real GLFW key state) can live directly in this handler's
 * client branch - item frames need no extra mixin. On the client side, a
 * non-PLAIN gesture sends an {@link InvisibleItemFramesNetworking} payload
 * and returns {@code FAIL}; fabric then cancels {@code startUseItem} before
 * the vanilla interact call, so no vanilla interact packet is sent and
 * nothing is rotated or clicked through. The server receiver performs the
 * action authoritatively. Vanilla clients (no mod installed) only ever
 * produce the PLAIN/SHIFT gestures via the normal vanilla packet; the
 * server-side branch below still recognises those for them.
 *
 * <p>Frame visibility reuses vanilla's own {@link Entity#setInvisible(boolean)}
 * / {@link Entity#isInvisible()}, the same flag that makes an item frame
 * placed with {@code Silent} + {@code Invisible} NBT invisible in survival.
 * That means the state is synced and saved by vanilla with no extra code
 * here: the item and its rotation keep rendering, only the frame's own
 * model is hidden.
 */
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

    /**
     * Forced-interact used by the network receiver when the keybind (swap
     * on) overrides click-through: runs vanilla's own frame-interact
     * handling directly, bypassing this mod's click-through logic entirely.
     */
    public static void forceInteract(Player player, ItemFrame frame, InteractionHand hand) {
        frame.interact(player, hand, Vec3.ZERO);
    }

    /**
     * Forwards the interaction to whatever block the crosshair ray reaches
     * beyond the frame. Runs on the server only.
     */
    static InteractionResult clickThrough(Player player, Level level, BlockPos framePos) {
        BlockHitResult target = findClickThroughTarget(player, level, framePos);
        if (target == null) {
            return InteractionResult.PASS;
        }
        return level.getBlockState(target.getBlockPos())
                .useWithoutItem(level, player, target);
    }

    /**
     * Walks the player's crosshair ray out to their reach and returns the
     * first block BEYOND {@code skippedPos}, or null if there is none.
     *
     * <p>The ray starts at the eye and skips over the frame's or sign's own
     * position, so the result is exactly "what would this click have reached
     * if the frame or sign were not there". This works regardless of how the
     * frame or sign is mounted (wall, ceiling, or standing). Earlier versions
     * ray-cast from just past the original hit point instead, which could
     * start inside the sign's own hitbox and re-hit the sign, making
     * click-through open the sign editor rather than the block behind it.
     */
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
