package net.fayber.invisibleitemframes;

import net.fayber.invisibleitemframes.client.InvisibleItemFramesClient;
import net.fayber.invisibleitemframes.sign.SignProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Handles shift right-click-with-empty-hand toggling of sign visibility, and
 * (when configured) forwards interactions to whatever block the sign is
 * mounted on instead of the sign itself.
 *
 * <p>Unlike item frames, signs have no vanilla "invisible" flag, so this mod
 * adds its own {@link SignProperties#INVISIBLE} blockstate property (see
 * {@link net.fayber.invisibleitemframes.mixin.SignBlockMixin}) and flips it
 * with {@link Level#setBlockAndUpdate}, which also takes care of the
 * client-side chunk remesh a block entity data change alone would not
 * trigger.
 *
 * <p>On the client side of a modded client the handler does not run the
 * action itself: it sends an {@link InvisibleItemFramesNetworking} payload
 * and returns {@code FAIL}, which cancels vanilla's use processing (so the
 * sign edit screen is not predicted) without sending a vanilla use packet.
 * The server receiver performs the action authoritatively. See
 * {@link InvisibleItemFramesNetworking} for why the event path alone was not
 * reliable enough.
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
        boolean toggleGesture = held.isEmpty() && player.isShiftKeyDown();

        if (toggleGesture && config.enableSignToggle) {
            if (level.isClientSide()) {
                InvisibleItemFramesClient.sendToggleSign(pos);
                return InteractionResult.FAIL;
            }
            if (hasTogglePermission(player, config)) {
                toggleSign(level, pos, state, player);
            }
            return InteractionResult.SUCCESS;
        }

        if (config.clickThroughSigns) {
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
