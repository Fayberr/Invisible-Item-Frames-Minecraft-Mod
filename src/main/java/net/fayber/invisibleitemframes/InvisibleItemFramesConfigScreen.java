package net.fayber.invisibleitemframes;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

// Hand-rolled ModMenu config screen, used when Cloth Config is not
// installed. Every control writes through
// InvisibleItemFramesConfig#set(String, String), which updates the
// in-memory config and saves it to config/invisibleitemframes.json.
public class InvisibleItemFramesConfigScreen extends Screen {
    private final Screen parent;
    private Button keybindButton;
    private boolean listeningForKeybind = false;

    public InvisibleItemFramesConfigScreen(Screen parent) {
        super(Component.literal("Invisible Item Frames Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int spacing = 22;
        int startY = 20;

        this.addRenderableWidget(booleanButton("enable_item_frame_toggle", "Enable Frame Toggle", centerX, startY));
        this.addRenderableWidget(booleanButton("affect_glow_item_frames", "Affect Glow Item Frames", centerX, startY + spacing));
        this.addRenderableWidget(booleanButton("click_through_visible_frames", "Click Through Visible Frames", centerX, startY + spacing * 2));
        this.addRenderableWidget(booleanButton("click_through_invisible_frames", "Click Through Invisible Frames", centerX, startY + spacing * 3));
        this.addRenderableWidget(booleanButton("enable_sign_toggle", "Enable Sign Toggle", centerX, startY + spacing * 4));
        this.addRenderableWidget(booleanButton("click_through_visible_signs", "Click Through Visible Signs", centerX, startY + spacing * 5));
        this.addRenderableWidget(booleanButton("click_through_invisible_signs", "Click Through Invisible Signs", centerX, startY + spacing * 6));
        this.addRenderableWidget(booleanButton("swap_keybind_and_sneak_roles", "Swap Keybind / Sneak Roles", centerX, startY + spacing * 7));
        this.addRenderableWidget(booleanButton("toggle_requires_permission", "Toggle Requires Permission", centerX, startY + spacing * 8));
        this.addRenderableWidget(booleanButton("require_empty_hand_for_toggle", "Require Empty Hand for Toggling", centerX, startY + spacing * 9));
        this.addRenderableWidget(booleanButton("require_empty_hand_for_interaction", "Require Empty Hand for Interaction", centerX, startY + spacing * 10));

        this.keybindButton = Button.builder(keybindText(), button -> {
            listeningForKeybind = true;
            keybindButton.setMessage(Component.literal("Toggle Keybind: press a key..."));
        }).bounds(centerX - 100, startY + spacing * 11, 200, 20).build();
        this.addRenderableWidget(keybindButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button ->
                this.minecraft.setScreenAndShow(this.parent))
                .bounds(centerX - 100, this.height - 30, 200, 20)
                .build());
    }

    private static Component keybindText() {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(config.keybindKeyCode);
        StringBuilder label = new StringBuilder("Toggle Keybind: ");
        if (config.keybindControl) label.append("Ctrl+");
        if (config.keybindAlt) label.append("Alt+");
        if (config.keybindShift) label.append("Shift+");
        label.append(key.getDisplayName().getString());
        return Component.literal(label.toString());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningForKeybind) {
            listeningForKeybind = false;
            int keyCode = event.key();
            // Ignore the modifier keys themselves as the base key; a plain
            // Escape cancels the capture without changing the binding.
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                keybindButton.setMessage(keybindText());
                return true;
            }
            boolean isModifierKey = keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT
                    || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
            if (!isModifierKey) {
                InvisibleItemFramesConfig.setKeybind(keyCode, false, false, false);
            }
            keybindButton.setMessage(keybindText());
            return true;
        }
        return super.keyPressed(event);
    }

    // A toggle that flips the named boolean config key and saves it.
    private Button booleanButton(String key, String label, int centerX, int y) {
        boolean current = InvisibleItemFramesConfig.getBool(key);
        return Button.builder(toggleText(label, current), button -> {
            boolean next = !InvisibleItemFramesConfig.getBool(key);
            InvisibleItemFramesConfig.set(key, String.valueOf(next));
            button.setMessage(toggleText(label, next));
        })
                .bounds(centerX - 100, y, 200, 20)
                .build();
    }

    private static Component toggleText(String prefix, boolean value) {
        return Component.literal(prefix + ": " + (value ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }
}
