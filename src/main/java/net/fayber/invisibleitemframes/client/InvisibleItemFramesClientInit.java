package net.fayber.invisibleitemframes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fayber.invisibleitemframes.InvisibleItemFramesMod;

/**
 * Client entrypoint. Force-loads the sign renderer at client boot so the
 * {@code AbstractSignRendererMixin} is applied (and verified) immediately
 * instead of the first time a sign renders - a version mismatch then fails
 * loudly in the log at startup rather than silently leaving signs visible.
 */
public class InvisibleItemFramesClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        try {
            Class.forName("net.minecraft.client.renderer.blockentity.AbstractSignRenderer");
        } catch (ClassNotFoundException e) {
            InvisibleItemFramesMod.LOGGER.error(
                    "AbstractSignRenderer not found; invisible signs will keep rendering their model", e);
        }
        InvisibleItemFramesMod.LOGGER.info("[InvisibleItemFrames] Client init done");
    }
}
