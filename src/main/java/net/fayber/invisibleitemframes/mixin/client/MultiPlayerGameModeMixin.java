package net.fayber.invisibleitemframes.mixin.client;

import net.fayber.invisibleitemframes.InvisibleItemFramesConfig;
import net.fayber.invisibleitemframes.client.InvisibleItemFramesClient;
import net.fayber.invisibleitemframes.client.InvisibleItemFramesClientKeybind;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Item frame interactions have no client-side event hook: Fabric's
 * {@code UseEntityCallback} (used by {@code ItemFrameInteractionHandler})
 * fires on the server only, unlike {@code UseBlockCallback} which fires on
 * both sides for signs. So the keybind gesture (an arbitrary key the vanilla
 * {@code ServerboundInteractPacket} has no field for) needs its own
 * interception point: this mixin, injected at the head of the client-side
 * entity-interact entry point, mirroring how Fabric API's own
 * {@code MultiPlayerGameModeMixin#interactBlock} hooks
 * {@code MultiPlayerGameMode#useItemOn} for blocks.
 *
 * <p>Plain and shift-click gestures are NOT handled here - those already
 * reach the server fine through the normal vanilla packet (which carries the
 * sneak flag), and are resolved server-side by
 * {@code ItemFrameInteractionHandler}. This mixin only fires when the
 * configurable keybind is actually held, in which case it cancels the
 * vanilla method (so no vanilla packet is sent at all) and sends this mod's
 * own payload instead.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void invisibleitemframes$interceptFrameKeybind(Player player, Entity entity, EntityHitResult hitResult,
                                                             InteractionHand hand,
                                                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!(entity instanceof ItemFrame frame)) {
            return;
        }
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        if (frame instanceof GlowItemFrame && !config.affectGlowItemFrames) {
            return;
        }
        if (!InvisibleItemFramesClientKeybind.isDown(config)) {
            return;
        }

        if (config.swapKeybindAndSneakRoles) {
            // Keybind forces vanilla interact, bypassing click-through, when
            // click-through would otherwise apply. If it wouldn't apply, the
            // plain-click resolution already lands on vanilla interact, so
            // there is nothing to override - let the click proceed normally.
            boolean clickThroughWouldApply = frame.isInvisible()
                    ? config.clickThroughInvisibleFrames
                    : config.clickThroughVisibleFrames;
            if (clickThroughWouldApply) {
                InvisibleItemFramesClient.sendForceInteractFrame(frame.getId());
                cir.setReturnValue(InteractionResult.SUCCESS);
                cir.cancel();
            }
            return;
        }

        if (!config.enableItemFrameToggle) {
            return;
        }
        if (!player.getItemInHand(hand).isEmpty()) {
            // Not empty-handed: let the click proceed normally (e.g.
            // placing an item into the frame) rather than eating it.
            return;
        }
        InvisibleItemFramesClient.sendToggleFrame(frame.getId());
        cir.setReturnValue(InteractionResult.SUCCESS);
        cir.cancel();
    }
}
