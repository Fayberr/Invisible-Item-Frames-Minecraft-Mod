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

// Same three-gesture interaction model as item frames (see GestureResolver
// and ItemFrameInteractionHandler), applied to signs.
//
// Signs have no vanilla "invisible" flag, so we add our own blockstate
// property (SignProperties.INVISIBLE, registered in SignBlockMixin) and flip
// it with Level#setBlockAndUpdate, which also handles the client-side chunk
// remesh that a plain block entity data change wouldn't trigger.
//
// UseBlockCallback fires on both sides just like UseEntityCallback, so the
// keybind check lives directly in this handler's client branch too - no
// extra mixin needed here either. Non-PLAIN gestures send a payload and
// return FAIL so nothing gets predicted client-side, same as frames.
//
// One sign-specific wrinkle: vanilla suppresses block use entirely while
// sneaking with ANYTHING in either hand (a shield in the offhand is enough),
// so a PASSed shift-click for the editor would silently do nothing. That's
// why the client sends the force-interact payload instead whenever the main
// hand is empty. An item in the main hand still PASSes through, so vanilla's
// own sneak-placement/dye-use behavior keeps working.
public final class SignInteractionHandler {
    private SignInteractionHandler() {}

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
                                                 BlockHitResult hitResult) {
        // Don't re-run our own gesture logic on a block we're only visiting
        // because a click-through forward landed on it (see forwardBlockInteraction).
        if (ItemFrameInteractionHandler.isForwardingInteraction()) {
            return InteractionResult.PASS;
        }
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
            // client can't just PASS here for the editor (see class comment on
            // the sneak+offhand quirk) - it sends force-interact instead, same
            // path the keybind role uses
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

    // used by the network receiver: re-checks the block is still a sign before flipping it
    public static void toggleSignIfSigned(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SignBlock) {
            toggleSign(level, pos, state, player);
        }
    }

    // used by the network receiver: re-checks the sign, then forwards to the block behind it
    public static void clickThroughIfSigned(Player player, Level level, BlockPos signPos) {
        if (level.getBlockState(signPos).getBlock() instanceof SignBlock) {
            clickThrough(level, signPos, player);
        }
    }

    // used by the network receiver when the keybind (swap on) overrides click-through -
    // runs vanilla's own sign-use handling directly, skipping our click-through logic
    public static void forceInteractIfSigned(Player player, Level level, BlockPos signPos) {
        BlockState state = level.getBlockState(signPos);
        if (!(state.getBlock() instanceof SignBlock)) {
            return;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(signPos), Direction.UP, signPos, false);
        state.useWithoutItem(level, player, hit);
    }

    // forwards to whatever block the crosshair reaches beyond the sign
    // (typically the chest/container it's mounted on); server only
    static InteractionResult clickThrough(Level level, BlockPos signPos, Player player) {
        BlockHitResult target = ItemFrameInteractionHandler.findClickThroughTarget(player, level, signPos);
        if (target == null) {
            return InteractionResult.PASS;
        }
        return ItemFrameInteractionHandler.forwardBlockInteraction(player, level, target);
    }

    static boolean hasTogglePermission(Player player, InvisibleItemFramesConfig config) {
        if (!config.toggleRequiresPermission) {
            return true;
        }
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
