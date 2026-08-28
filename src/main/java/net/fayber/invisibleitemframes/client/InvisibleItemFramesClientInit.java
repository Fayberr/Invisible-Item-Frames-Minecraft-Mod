package net.fayber.invisibleitemframes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fayber.invisibleitemframes.InvisibleItemFramesMod;

// client entrypoint. force-loads the sign renderer class at boot so the
// AbstractSignRendererMixin gets applied (and verified) immediately instead
// of the first time a sign renders - a version mismatch then fails loudly at
// startup instead of just silently leaving signs visible.
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
