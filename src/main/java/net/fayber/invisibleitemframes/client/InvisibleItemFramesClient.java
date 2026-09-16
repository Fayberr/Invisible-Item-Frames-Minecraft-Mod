package net.fayber.invisibleitemframes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fayber.invisibleitemframes.InvisibleItemFramesNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.SignBlock;

// Client-only glue: forwards one interaction intent to the server as an
// InvisibleItemFramesNetworking#INTERACT_TYPE payload. Only ever
// class-loaded on the physical client (the common handlers call it from
// level.isClientSide() branches), so a dedicated server never loads
// the client networking classes.
public final class InvisibleItemFramesClient implements ClientModInitializer {
    public InvisibleItemFramesClient() {}

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyBlockModelAfterBake().register((model, context) -> {
                if (context.state().getBlock() instanceof SignBlock) {
                    return new InvisibleSignBlockStateModel(model);
                }
                return model;
            });
        });
    }

    // Forces the chunk section containing pos (and its neighbors, to avoid
    // seam gaps) to rebuild its baked mesh. Needed because SignBlockEntityMixin
    // can't rely on a BlockState diff to trigger the rebuild - see the comment
    // there for why sendBlockUpdated(pos, state, state, ...) is a no-op here.
    // Called only from a level.isClientSide() branch in that common mixin, so
    // this ClientLevel reference never gets resolved on a dedicated server.
    public static void signVisibilityChanged(BlockPos pos) {
        if (!(Minecraft.getInstance().level instanceof ClientLevel clientLevel)) {
            return;
        }
        clientLevel.setSectionDirtyWithNeighbors(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

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
