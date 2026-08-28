package net.fayber.invisibleitemframes;

import net.minecraft.server.permissions.Permissions;
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
 * Handles shift right-click-with-empty-hand toggling of item frame
 * visibility, and (when configured) forwards interactions to whatever block
 * is behind the frame instead of the frame itself.
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
        boolean toggleGesture = held.isEmpty() && player.isShiftKeyDown();

        if (toggleGesture && config.enableItemFrameToggle) {
            if (!level.isClientSide() && hasPermission(player, config)) {
                frame.setInvisible(!frame.isInvisible());
            }
            return InteractionResult.SUCCESS;
        }

        boolean clickThrough = frame.isInvisible()
                ? config.clickThroughInvisibleFrames
                : config.clickThroughVisibleFrames;
        if (clickThrough) {
            return tryClickThrough(player, level, hitResult.getLocation());
        }

        return InteractionResult.PASS;
    }

    private static boolean hasPermission(Player player, InvisibleItemFramesConfig config) {
        if (!config.toggleRequiresPermission) {
            return true;
        }
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * Re-raycasts from just past the original hit point out to the player's
     * reach, so a click on a frame/sign that would otherwise consume the
     * interaction instead reaches whatever block is actually behind it. This
     * works regardless of how the frame or sign is mounted (wall, ceiling,
     * standing), unlike hardcoding each attachment direction.
     */
    static InteractionResult tryClickThrough(Player player, Level level, Vec3 hitLocation) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 start = hitLocation.add(look.scale(0.05));
        double reach = player.blockInteractionRange() + 1.0;
        Vec3 end = player.getEyePosition().add(look.scale(reach));

        BlockHitResult behind = level.clip(
                new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (behind.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        return level.getBlockState(behind.getBlockPos()).useWithoutItem(level, player, behind);
    }
}
