package net.fayber.invisibleitemframes;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Modifier;
import me.shedaniel.clothconfig2.api.ModifierKeyCode;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Cloth Config based config screen (the nicer ModMenu GUI). Optional
// dependency - ModMenu opens this instead of the hand-rolled
// InvisibleItemFramesConfigScreen when Cloth Config is installed.
public final class InvisibleItemFramesClothScreen {
    private InvisibleItemFramesClothScreen() {}

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Invisible Item Frames"));

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory frames = builder.getOrCreateCategory(Component.literal("Item Frames"));
        frames.addEntry(bool(eb, "enable_item_frame_toggle", "Enable Toggle", true,
                "Shift right-click (or the keybind, if swapped) with an empty hand toggles a frame's visibility."));
        frames.addEntry(bool(eb, "affect_glow_item_frames", "Affect Glow Item Frames", true,
                "Whether glow item frames can also be toggled invisible."));
        frames.addEntry(bool(eb, "click_through_visible_frames", "Click Through Visible Frames", false,
                "Plain right-clicks on a visible frame reach the block behind it instead."));
        frames.addEntry(bool(eb, "click_through_invisible_frames", "Click Through Invisible Frames", true,
                "Plain right-clicks on an invisible frame reach the block behind it instead."));

        ConfigCategory signs = builder.getOrCreateCategory(Component.literal("Signs"));
        signs.addEntry(bool(eb, "enable_sign_toggle", "Enable Toggle", true,
                "Shift right-click (or the keybind, if swapped) with an empty hand toggles a sign's visibility."));
        signs.addEntry(bool(eb, "click_through_visible_signs", "Click Through Visible Signs", false,
                "Plain right-clicks on a visible sign reach the block it is mounted on instead."));
        signs.addEntry(bool(eb, "click_through_invisible_signs", "Click Through Invisible Signs", false,
                "Plain right-clicks on an invisible sign reach the block it is mounted on instead."));

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(keybind(eb));
        general.addEntry(bool(eb, "swap_keybind_and_sneak_roles", "Swap Keybind / Sneak Roles", false,
                "Off: Shift right-click interacts, keybind + right-click toggles visibility. "
                        + "On: Shift right-click toggles visibility, keybind + right-click interacts "
                        + "(and always wins over click-through)."));
        general.addEntry(bool(eb, "toggle_requires_permission", "Toggle Requires Permission", false,
                "Only operators may toggle frame and sign visibility."));
        general.addEntry(bool(eb, "require_empty_hand_for_toggle", "Require Empty Hand for Visibility Toggling", false,
                "Off: toggling visibility works even with an item in hand. On: toggling only works with an empty hand."));
        general.addEntry(bool(eb, "require_empty_hand_for_interaction", "Require Empty Hand for Interaction", true,
                "On: rotating a frame's item or opening the sign editor via shift/keybind only works with an empty "
                        + "hand (an item in hand keeps vanilla's own behavior, like dyeing a sign). Off: interaction "
                        + "is forced even with an item in hand."));

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

    private static AbstractConfigListEntry keybind(ConfigEntryBuilder eb) {
        InvisibleItemFramesConfig config = InvisibleItemFramesConfig.get();
        ModifierKeyCode current = ModifierKeyCode.of(
                InputConstants.Type.KEYSYM.getOrCreate(config.keybindKeyCode),
                Modifier.of(config.keybindAlt, config.keybindControl, config.keybindShift));
        ModifierKeyCode defaultValue = ModifierKeyCode.of(
                InputConstants.Type.KEYSYM.getOrCreate(342 /* GLFW_KEY_LEFT_ALT */), Modifier.none());

        return eb.startModifierKeyCodeField(Component.literal("Toggle Keybind"), current)
                .setAllowMouse(false)
                .setModifierDefaultValue(() -> defaultValue)
                .setTooltip(Component.literal(
                        "Hold this key while right-clicking to toggle visibility (or interact, if swapped)."))
                .setModifierSaveConsumer(value -> InvisibleItemFramesConfig.setKeybind(
                        value.getKeyCode().getValue(),
                        value.getModifier().hasAlt(),
                        value.getModifier().hasControl(),
                        value.getModifier().hasShift()))
                .build();
    }
}
