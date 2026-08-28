package net.fayber.invisibleitemframes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fayber.invisibleitemframes.InvisibleItemFramesConfig;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

// client-only, real-time check of the configurable toggle keybind (default
// Left Alt, no extra modifiers). called from SignInteractionHandler and
// ItemFrameInteractionHandler's client branches, both guarded by
// level.isClientSide() - a dedicated server never executes this class.
public final class InvisibleItemFramesClientKeybind {
    private InvisibleItemFramesClientKeybind() {}

    public static boolean isDown(InvisibleItemFramesConfig config) {
        if (config.keybindKeyCode <= 0) {
            return false;
        }
        Window window = Minecraft.getInstance().getWindow();
        if (!InputConstants.isKeyDown(window, config.keybindKeyCode)) {
            return false;
        }
        if (config.keybindAlt && !isAltDown(window)) {
            return false;
        }
        if (config.keybindControl && !isControlDown(window)) {
            return false;
        }
        if (config.keybindShift && !isShiftDown(window)) {
            return false;
        }
        return true;
    }

    private static boolean isAltDown(Window window) {
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static boolean isControlDown(Window window) {
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private static boolean isShiftDown(Window window) {
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
