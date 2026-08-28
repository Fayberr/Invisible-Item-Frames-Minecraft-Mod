package net.fayber.invisibleitemframes;

import net.fayber.invisibleitemframes.client.InvisibleItemFramesClient;
import net.fayber.invisibleitemframes.client.InvisibleItemFramesClientKeybind;
import net.fayber.invisibleitemframes.sign.SignProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Handles the three-gesture interaction model for signs:
 *
 * <ul>
 *   <li>Plain right-click: click-through if enabled for the sign's current
 *       visibility state, otherwise vanilla (edit sign text).</li>
 *   <li>Shift + right-click: vanilla interact, or toggle visibility if the
 *       swap option is enabled.</li>
 *   <li>Keybind held + right-click: toggle visibility, or forced vanilla
 *       interact (even over click-through) if the swap option is enabled.</li>
 * </ul>
 *
 * See {@link GestureResolver} for the shared resolution logic (also used by
 * {@link ItemFrameInteractionHandler}).
 *
 * <p>Signs have no vanilla "invisible" flag, so this mod adds its own
 * {@link SignProperties#INVISIBLE} blockstate property (see
 * {@link net.fayber.invisibleitemframes.mixin.SignBlockMixin}) and flips it
 * with {@link Level#setBlockAndUpdate}, which also takes care of the
 * client-side chunk remesh a block entity data change alone would not
 * trigger.
 *
 * <p>Fabric's {@code UseBlockCallback} fires on both sides, so the keybind
 * check (client-only, real GLFW key state) can live directly in this
 * handler's client branch - unlike item frames, signs need no extra mixin.
 * On the client side, a non-PLAIN gesture sends an
 * {@link InvisibleItemFramesNetworking} payload and returns {@code FAIL},
 * which cancels vanilla's use processing (so the sign edit screen is not
 * predicted) without sending a vanilla use packet. The server receiver
 * performs the action authoritatively. For the shift-click editor this
 * payload route is essential: vanilla suppresses block use entirely while
 * sneaking with anything in either hand (e.g. a shield in the offhand), so
 * a PASSed shift-click would silently do nothing. An item in the MAIN hand
 * is respected instead: the gesture PASSes so vanilla sneak-placement and
 * dye use keep working. Vanilla clients (no mod installed) only ever
 * produce the PLAIN/SHIFT gestures via the normal vanilla packet; the
 * server-side branch below still recognises those for them.
 */
public final class SignInteractionHandler {
    private SignInteractionHandler() {}

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
                                                 BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SignBlock)) {
            return InteractionResult.PASS;
        }

        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
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
            if (!config.enableSignToggle) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                InvisibleItemFramesClient.sendToggleSign(pos);
                return InteractionResult.FAIL;
            }
            if (hasTogglePermission(player, config)) {
                toggleSign(level, pos, state, player);
            }
            return InteractionResult.SUCCESS;
        }

        boolean invisible = state.getValue(SignProperties.INVISIBLE);
        boolean clickThroughWouldApply = invisible ? config.clickThroughInvisibleSigns : config.clickThroughVisibleSigns;

        if (gesture == GestureResolver.Gesture.INTERACT) {
            // Modded clients must not rely on PASS for the editor: vanilla
            // suppresses block use entirely while sneaking with anything in
            // EITHER hand (a shield in the offhand is enough), so a PASSed
            // shift-click would silently do nothing. With an empty main hand
            // the client therefore sends the force-interact payload (the same
            // path the keybind role uses) and the server opens the editor
            // directly. With an item in the main hand, PASS keeps vanilla's
            // item behavior (sneak-placing a block against the sign, dyeing
            // its text, ...) - unless require_empty_hand_for_interaction is
            // turned off, in which case the editor is forced open regardless
            // of what's in hand.
            boolean interactHandOk = !config.requireEmptyHandForInteraction || handEmpty;
            if ((level.isClientSide() && interactHandOk) || (keybindDown && clickThroughWouldApply && interactHandOk)) {
                InvisibleItemFramesClient.sendForceInteractSign(pos);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }

        // gesture == PLAIN
        if (clickThroughWouldApply) {
            if (level.isClientSide()) {
                if (ItemFrameInteractionHandler.findClickThroughTarget(player, level, pos) == null) {
                    return InteractionResult.PASS;
                }
                InvisibleItemFramesClient.sendClickThroughSign(pos);
                return InteractionResult.FAIL;
            }
            return clickThrough(level, pos, player);
        }

        return InteractionResult.PASS;
    }

    /** Flips the sign's invisible property; caller has validated everything. */
    static void toggleSign(Level level, BlockPos pos, BlockState state, Player player) {
        boolean nowInvisible = !state.getValue(SignProperties.INVISIBLE);
        level.setBlockAndUpdate(pos, state.setValue(SignProperties.INVISIBLE, nowInvisible));
        InvisibleItemFramesMod.LOGGER.info("{} toggled a sign at ({}, {}, {}) {}",
                player.getGameProfile().name(), pos.getX(), pos.getY(), pos.getZ(),
                nowInvisible ? "invisible" : "visible");
    }

    /**
     * Toggle used by the network receiver: re-checks that the block is still
     * a sign before flipping it.
     */
    public static void toggleSignIfSigned(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SignBlock) {
            toggleSign(level, pos, state, player);
        }
    }

    /**
     * Click-through used by the network receiver: re-checks the sign, then
     * forwards the interaction to the block behind it.
     */
    public static void clickThroughIfSigned(Player player, Level level, BlockPos signPos) {
        if (level.getBlockState(signPos).getBlock() instanceof SignBlock) {
            clickThrough(level, signPos, player);
        }
    }

    /**
     * Forced-interact used by the network receiver when the keybind (swap
     * on) overrides click-through: runs vanilla's own sign-use handling
     * directly, bypassing this mod's click-through logic entirely.
     */
    public static void forceInteractIfSigned(Player player, Level level, BlockPos signPos) {
        BlockState state = level.getBlockState(signPos);
        if (!(state.getBlock() instanceof SignBlock)) {
            return;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(signPos), Direction.UP, signPos, false);
        state.useWithoutItem(level, player, hit);
    }

    /**
     * Forwards the interaction to whatever block the crosshair ray reaches
     * beyond the sign (typically the chest or other container it sits in
     * front of). Runs on the server only.
     */
    static InteractionResult clickThrough(Level level, BlockPos signPos, Player player) {
        BlockHitResult target = ItemFrameInteractionHandler.findClickThroughTarget(player, level, signPos);
        if (target == null) {
            return InteractionResult.PASS;
        }
        return level.getBlockState(target.getBlockPos())
                .useWithoutItem(level, player, target);
    }

    static boolean hasTogglePermission(Player player, InvisibleItemFramesConfig config) {
        if (!config.toggleRequiresPermission) {
            return true;
        }
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
