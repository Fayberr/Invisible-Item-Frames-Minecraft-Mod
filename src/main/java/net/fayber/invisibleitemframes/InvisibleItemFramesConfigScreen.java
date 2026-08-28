package net.fayber.invisibleitemframes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Hand-rolled ModMenu config screen, used when Cloth Config is not
 * installed. Every control writes through
 * {@link InvisibleItemFramesConfig#set(String, String)}, which updates the
 * in-memory config and saves it to {@code config/invisibleitemframes.json}.
 */
public class InvisibleItemFramesConfigScreen extends Screen {
    private final Screen parent;

    public InvisibleItemFramesConfigScreen(Screen parent) {
        super(Component.literal("Invisible Item Frames Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int spacing = 22;
        int startY = 25;

        this.addRenderableWidget(booleanButton("enable_item_frame_toggle", "Enable Frame Toggle", centerX, startY));
        this.addRenderableWidget(booleanButton("affect_glow_item_frames", "Affect Glow Item Frames", centerX, startY + spacing));
        this.addRenderableWidget(booleanButton("click_through_visible_frames", "Click Through Visible Frames", centerX, startY + spacing * 2));
        this.addRenderableWidget(booleanButton("click_through_invisible_frames", "Click Through Invisible Frames", centerX, startY + spacing * 3));
        this.addRenderableWidget(booleanButton("enable_sign_toggle", "Enable Sign Toggle", centerX, startY + spacing * 4));
        this.addRenderableWidget(booleanButton("click_through_signs", "Click Through Signs", centerX, startY + spacing * 5));
        this.addRenderableWidget(booleanButton("toggle_requires_permission", "Toggle Requires Permission", centerX, startY + spacing * 6));

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button ->
                this.minecraft.setScreen(this.parent))
                .bounds(centerX - 100, this.height - 30, 200, 20)
                .build());
    }

    /** A toggle that flips the named boolean config key and saves it. */
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
        this.minecraft.setScreen(this.parent);
    }
}
