package net.fayber.invisibleitemframes;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Cloth Config based config screen (the nicer ModMenu GUI). Optional
 * dependency: when Cloth Config is installed, ModMenu opens this instead of
 * the hand-rolled {@link InvisibleItemFramesConfigScreen}.
 */
public final class InvisibleItemFramesClothScreen {
    private InvisibleItemFramesClothScreen() {}

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Invisible Item Frames"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory frames = builder.getOrCreateCategory(Component.literal("Item Frames"));
        frames.addEntry(bool(eb, "enable_item_frame_toggle", "Enable Toggle", true,
                "Shift right-click an item frame with an empty hand to hide or show it."));
        frames.addEntry(bool(eb, "affect_glow_item_frames", "Affect Glow Item Frames", true,
                "Whether glow item frames can also be toggled invisible."));
        frames.addEntry(bool(eb, "click_through_visible_frames", "Click Through Visible Frames", false,
                "Interactions on a visible frame reach the block behind it instead."));
        frames.addEntry(bool(eb, "click_through_invisible_frames", "Click Through Invisible Frames", true,
                "Interactions on an invisible frame reach the block behind it instead."));

        ConfigCategory signs = builder.getOrCreateCategory(Component.literal("Signs"));
        signs.addEntry(bool(eb, "enable_sign_toggle", "Enable Toggle", true,
                "Shift right-click a sign with an empty hand to hide or show it."));
        signs.addEntry(bool(eb, "click_through_signs", "Click Through Signs", false,
                "Interactions on a sign reach the block it is mounted on instead."));

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(bool(eb, "toggle_requires_permission", "Toggle Requires Permission", false,
                "Only operators may toggle frame and sign visibility."));

        return builder.build();
    }

    private static AbstractConfigListEntry bool(ConfigEntryBuilder eb, String key, String label,
                                                 boolean defaultValue, String tooltip) {
        boolean current = InvisibleItemFramesConfig.getBool(key);
        return eb.startBooleanToggle(Component.literal(label), current)
                .setDefaultValue(defaultValue)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(value -> InvisibleItemFramesConfig.set(key, String.valueOf(value)))
                .build();
    }
}
