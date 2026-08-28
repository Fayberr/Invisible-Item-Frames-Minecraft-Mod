package net.fayber.invisibleitemframes;

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
            if (!level.isClientSide() && hasPermission(player, config)) {
                boolean nowInvisible = !state.getValue(SignProperties.INVISIBLE);
                level.setBlockAndUpdate(pos, state.setValue(SignProperties.INVISIBLE, nowInvisible));
            }
            return InteractionResult.SUCCESS;
        }

        if (config.clickThroughSigns) {
            return ItemFrameInteractionHandler.tryClickThrough(player, level, hitResult.getLocation());
        }

        return InteractionResult.PASS;
    }

    private static boolean hasPermission(Player player, InvisibleItemFramesConfig config) {
        if (!config.toggleRequiresPermission) {
            return true;
        }
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
