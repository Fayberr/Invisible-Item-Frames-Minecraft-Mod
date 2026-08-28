package net.fayber.invisibleitemframes;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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

// Three gestures on a sign: plain right-click (click-through if enabled,
// otherwise edit text), shift + right-click (edit text, or toggle if swapped),
// keybind + right-click (toggle, or forced edit if swapped). See
// GestureResolver for the shared logic (item frames use the same table).
//
// Signs don't have a vanilla "invisible" flag so we add our own blockstate
// property (SignProperties.INVISIBLE, see the SignBlockMixin) and flip it
// with setBlockAndUpdate, which also triggers the client-side chunk remesh
// that a block entity data change alone wouldn't.
//
// UseBlockCallback fires on both sides, so the keybind check (real GLFW key
// state, client only) lives right in the client branch below - no extra mixin
// needed for signs. On a non-PLAIN gesture the client sends a payload and
// returns FAIL instead of the vanilla use packet, and the server acts on the
// payload alone. This is also why the shift-click sign editor needs the
// payload route at all: vanilla suppresses block use entirely while sneaking
// with anything in either hand (even a shield in the offhand), so a plain
// PASS would silently do nothing for that case.
public final class SignInteractionHandler {
    private SignInteractionHandler() {}

    // set while we're replaying UseBlockCallback below purely to ask other
    // mods (land claim / protection mods hooked into the same event) whether
    // they'd allow the click; without this our own gesture logic would run a
    // second time and double-fire the action we're already handling
    private static boolean replaying = false;

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
                                                 BlockHitResult hitResult) {
        if (replaying) {
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
            // can't just PASS here on a modded client: vanilla suppresses block
            // use entirely while sneaking with anything in either hand (even a
            // shield in the offhand), so a plain PASS would silently eat the
            // shift-click. with an empty main hand we force it open via the
            // same payload the keybind uses. with an item in hand, PASS keeps
            // vanilla behavior (sneak-placing a block, dyeing the sign, ...)
            // unless require_empty_hand_for_interaction is off, in which case
            // we force the editor open regardless of what's in hand
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

    // flips the sign's invisible property; caller has validated everything
    static void toggleSign(Level level, BlockPos pos, BlockState state, Player player) {
        boolean nowInvisible = !state.getValue(SignProperties.INVISIBLE);
        level.setBlockAndUpdate(pos, state.setValue(SignProperties.INVISIBLE, nowInvisible));
        InvisibleItemFramesMod.LOGGER.info("{} toggled a sign at ({}, {}, {}) {}",
                player.getGameProfile().name(), pos.getX(), pos.getY(), pos.getZ(),
                nowInvisible ? "invisible" : "visible");
    }

    // toggle used by the network receiver (keybind path): re-checks it's
    // still a sign and gives other mods a chance to veto before flipping it,
    // since this path never goes through the normal event chain otherwise
    public static void toggleSignIfSigned(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SignBlock && otherModsAllow(player, level, pos)) {
            toggleSign(level, pos, state, player);
        }
    }

    // click-through used by the network receiver: re-checks the sign, then
    // forwards the interaction to the block behind it
    public static void clickThroughIfSigned(Player player, Level level, BlockPos signPos) {
        if (level.getBlockState(signPos).getBlock() instanceof SignBlock) {
            clickThrough(level, signPos, player);
        }
    }

    // forced-interact used by the network receiver when the keybind (swap on)
    // overrides click-through: runs vanilla's own sign-use handling directly,
    // bypassing this mod's click-through logic entirely
    public static void forceInteractIfSigned(Player player, Level level, BlockPos signPos) {
        BlockState state = level.getBlockState(signPos);
        if (!(state.getBlock() instanceof SignBlock)) {
            return;
        }
        if (!otherModsAllow(player, level, signPos)) {
            return;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(signPos), Direction.UP, signPos, false);
        state.useWithoutItem(level, player, hit);
    }

    // forwards the interaction to whatever block the crosshair ray reaches
    // beyond the sign (typically the chest or other container it sits in
    // front of). runs on the server only
    static InteractionResult clickThrough(Level level, BlockPos signPos, Player player) {
        BlockHitResult target = ItemFrameInteractionHandler.findClickThroughTarget(player, level, signPos);
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (!otherModsAllow(player, level, target.getBlockPos())) {
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

    // Replays UseBlockCallback for pos with our own listener silenced (see the
    // "replaying" guard above), so any land claim / protection mod hooked into
    // the same event still gets a say. Needed for anything triggered by the
    // network payload above: those actions run outside the normal click flow,
    // so without this a keybind click could toggle or edit a sign, or reach
    // through to a protected chest, in a claim that would otherwise block it.
    static boolean otherModsAllow(Player player, Level level, BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        replaying = true;
        try {
            return UseBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND, hit)
                    == InteractionResult.PASS;
        } finally {
            replaying = false;
        }
    }
}
