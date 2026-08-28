package net.fayber.invisibleitemframes.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fayber.invisibleitemframes.InvisibleItemFramesNetworking;
import net.minecraft.core.BlockPos;

// client-only glue: forwards one interaction intent to the server as an
// InvisibleItemFramesNetworking payload. only ever loaded on the physical
// client (called from level.isClientSide() branches), so a dedicated server
// never touches this class.
public final class InvisibleItemFramesClient {
    private InvisibleItemFramesClient() {}

    public static void sendToggleSign(BlockPos signPos) {
        ClientPlayNetworking.send(new InvisibleItemFramesNetworking.InteractPayload(
                InvisibleItemFramesNetworking.InteractPayload.TOGGLE_SIGN, signPos.asLong(), -1));
    }

    public static void sendClickThroughSign(BlockPos signPos) {
        ClientPlayNetworking.send(new InvisibleItemFramesNetworking.InteractPayload(
                InvisibleItemFramesNetworking.InteractPayload.CLICK_THROUGH_SIGN, signPos.asLong(), -1));
    }

    public static void sendToggleFrame(int frameEntityId) {
        ClientPlayNetworking.send(new InvisibleItemFramesNetworking.InteractPayload(
                InvisibleItemFramesNetworking.InteractPayload.TOGGLE_FRAME, 0L, frameEntityId));
    }

    public static void sendClickThroughFrame(int frameEntityId) {
        ClientPlayNetworking.send(new InvisibleItemFramesNetworking.InteractPayload(
                InvisibleItemFramesNetworking.InteractPayload.CLICK_THROUGH_FRAME, 0L, frameEntityId));
    }

    public static void sendForceInteractSign(BlockPos signPos) {
        ClientPlayNetworking.send(new InvisibleItemFramesNetworking.InteractPayload(
                InvisibleItemFramesNetworking.InteractPayload.FORCE_INTERACT_SIGN, signPos.asLong(), -1));
    }

    public static void sendForceInteractFrame(int frameEntityId) {
        ClientPlayNetworking.send(new InvisibleItemFramesNetworking.InteractPayload(
                InvisibleItemFramesNetworking.InteractPayload.FORCE_INTERACT_FRAME, 0L, frameEntityId));
    }
}
