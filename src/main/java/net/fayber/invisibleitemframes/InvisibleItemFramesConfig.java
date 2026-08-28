package net.fayber.invisibleitemframes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod config, stored as {@code config/invisibleitemframes.json}. Values can be
 * changed in-game with {@code /invisibleitemframes config} or, in singleplayer,
 * from the ModMenu config screen.
 *
 * <p>On a modded client, right-click resolves to one of three actions based
 * on what is held down: the configurable toggle keybind, sneak (shift), or
 * neither. Which of {@code Interact} and {@code Toggle Visibility} sits on
 * sneak vs. the keybind is swappable (see the client-only config). Plain
 * click-through has its own independent enable flag per case (visible or
 * invisible, frame or sign) below. Vanilla clients (no mod installed) fall
 * back to the simpler sneak+empty-hand toggle gesture server-side; see
 * {@link ItemFrameInteractionHandler} and {@link SignInteractionHandler}.
 */
public final class InvisibleItemFramesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("invisibleitemframes.json");
    private static final Logger LOGGER = LoggerFactory.getLogger("invisibleitemframes");

    private static final InvisibleItemFramesConfig INSTANCE = new InvisibleItemFramesConfig();

    // Item frames.
    public boolean enableItemFrameToggle = true;
    public boolean affectGlowItemFrames = true;
    public boolean clickThroughVisibleFrames = false;
    public boolean clickThroughInvisibleFrames = true;

    // Signs.
    public boolean enableSignToggle = true;
    public boolean clickThroughVisibleSigns = false;
    public boolean clickThroughInvisibleSigns = false;

    // Multiplayer safety.
    public boolean toggleRequiresPermission = false;

    // Empty-hand gating for the two active gestures. Plain right-click
    // (click-through / vanilla fallback) is never gated by hand contents.
    public boolean requireEmptyHandForToggle = false;
    public boolean requireEmptyHandForInteraction = true;

    // Toggle keybind (client-only concept, but kept in the shared config so
    // the ModMenu/Cloth Config screens use the same read/write pattern as
    // everything else). Default: Left Alt, no extra modifiers. Keyboard only
    // (no mouse buttons - the gesture already involves the right mouse
    // button, so a second mouse button would be awkward to hold together).
    public boolean swapKeybindAndSneakRoles = false;
    public int keybindKeyCode = 342; // GLFW_KEY_LEFT_ALT
    public boolean keybindAlt = false;
    public boolean keybindControl = false;
    public boolean keybindShift = false;

    public static InvisibleItemFramesConfig get() {
        return INSTANCE;
    }

    /** Loads {@code config/invisibleitemframes.json} into the shared instance, then writes it back. */
    public static void load() {
        if (Files.exists(PATH)) {
            try {
                Raw raw = GSON.fromJson(Files.readString(PATH), Raw.class);
                if (raw != null) {
                    apply(raw);
                }
            } catch (Exception e) {
                LOGGER.error("[InvisibleItemFrames] Failed to read config, using defaults", e);
            }
        }
        save();
    }

    private static void apply(Raw raw) {
        InvisibleItemFramesConfig c = INSTANCE;
        if (raw.enable_item_frame_toggle != null) c.enableItemFrameToggle = raw.enable_item_frame_toggle;
        if (raw.affect_glow_item_frames != null) c.affectGlowItemFrames = raw.affect_glow_item_frames;
        if (raw.click_through_visible_frames != null) c.clickThroughVisibleFrames = raw.click_through_visible_frames;
        if (raw.click_through_invisible_frames != null) c.clickThroughInvisibleFrames = raw.click_through_invisible_frames;
        if (raw.enable_sign_toggle != null) c.enableSignToggle = raw.enable_sign_toggle;
        if (raw.click_through_visible_signs != null) c.clickThroughVisibleSigns = raw.click_through_visible_signs;
        if (raw.click_through_invisible_signs != null) c.clickThroughInvisibleSigns = raw.click_through_invisible_signs;
        if (raw.toggle_requires_permission != null) c.toggleRequiresPermission = raw.toggle_requires_permission;
        if (raw.require_empty_hand_for_toggle != null) c.requireEmptyHandForToggle = raw.require_empty_hand_for_toggle;
        if (raw.require_empty_hand_for_interaction != null) c.requireEmptyHandForInteraction = raw.require_empty_hand_for_interaction;
        if (raw.swap_keybind_and_sneak_roles != null) c.swapKeybindAndSneakRoles = raw.swap_keybind_and_sneak_roles;
        if (raw.keybind_key_code != null) c.keybindKeyCode = raw.keybind_key_code;
        if (raw.keybind_alt != null) c.keybindAlt = raw.keybind_alt;
        if (raw.keybind_control != null) c.keybindControl = raw.keybind_control;
        if (raw.keybind_shift != null) c.keybindShift = raw.keybind_shift;
    }

    public static void save() {
        Raw raw = new Raw();
        raw.enable_item_frame_toggle = INSTANCE.enableItemFrameToggle;
        raw.affect_glow_item_frames = INSTANCE.affectGlowItemFrames;
        raw.click_through_visible_frames = INSTANCE.clickThroughVisibleFrames;
        raw.click_through_invisible_frames = INSTANCE.clickThroughInvisibleFrames;
        raw.enable_sign_toggle = INSTANCE.enableSignToggle;
        raw.click_through_visible_signs = INSTANCE.clickThroughVisibleSigns;
        raw.click_through_invisible_signs = INSTANCE.clickThroughInvisibleSigns;
        raw.toggle_requires_permission = INSTANCE.toggleRequiresPermission;
        raw.require_empty_hand_for_toggle = INSTANCE.requireEmptyHandForToggle;
        raw.require_empty_hand_for_interaction = INSTANCE.requireEmptyHandForInteraction;
        raw.swap_keybind_and_sneak_roles = INSTANCE.swapKeybindAndSneakRoles;
        raw.keybind_key_code = INSTANCE.keybindKeyCode;
        raw.keybind_alt = INSTANCE.keybindAlt;
        raw.keybind_control = INSTANCE.keybindControl;
        raw.keybind_shift = INSTANCE.keybindShift;
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(raw));
        } catch (IOException e) {
            LOGGER.error("[InvisibleItemFrames] Failed to save config", e);
        }
    }

    /** Sets a key by name (command / ModMenu); returns false if unknown. */
    public static boolean set(String key, String value) {
        InvisibleItemFramesConfig c = INSTANCE;
        switch (key.toLowerCase()) {
            case "enable_item_frame_toggle" -> c.enableItemFrameToggle = parseBool(value);
            case "affect_glow_item_frames" -> c.affectGlowItemFrames = parseBool(value);
            case "click_through_visible_frames" -> c.clickThroughVisibleFrames = parseBool(value);
            case "click_through_invisible_frames" -> c.clickThroughInvisibleFrames = parseBool(value);
            case "enable_sign_toggle" -> c.enableSignToggle = parseBool(value);
            case "click_through_visible_signs" -> c.clickThroughVisibleSigns = parseBool(value);
            case "click_through_invisible_signs" -> c.clickThroughInvisibleSigns = parseBool(value);
            case "toggle_requires_permission" -> c.toggleRequiresPermission = parseBool(value);
            case "require_empty_hand_for_toggle" -> c.requireEmptyHandForToggle = parseBool(value);
            case "require_empty_hand_for_interaction" -> c.requireEmptyHandForInteraction = parseBool(value);
            case "swap_keybind_and_sneak_roles" -> c.swapKeybindAndSneakRoles = parseBool(value);
            case "keybind_alt" -> c.keybindAlt = parseBool(value);
            case "keybind_control" -> c.keybindControl = parseBool(value);
            case "keybind_shift" -> c.keybindShift = parseBool(value);
            case "keybind_key_code" -> c.keybindKeyCode = Integer.parseInt(value.trim());
            default -> {
                return false;
            }
        }
        save();
        return true;
    }

    /** Sets the keybind (key code + modifiers) directly; used by config screens. */
    public static void setKeybind(int keyCode, boolean alt, boolean control, boolean shift) {
        InvisibleItemFramesConfig c = INSTANCE;
        c.keybindKeyCode = keyCode;
        c.keybindAlt = alt;
        c.keybindControl = control;
        c.keybindShift = shift;
        save();
    }

    /** Reads a boolean key; the ModMenu screen uses this so key names stay in one place. */
    public static boolean getBool(String key) {
        InvisibleItemFramesConfig c = INSTANCE;
        return switch (key.toLowerCase()) {
            case "enable_item_frame_toggle" -> c.enableItemFrameToggle;
            case "affect_glow_item_frames" -> c.affectGlowItemFrames;
            case "click_through_visible_frames" -> c.clickThroughVisibleFrames;
            case "click_through_invisible_frames" -> c.clickThroughInvisibleFrames;
            case "enable_sign_toggle" -> c.enableSignToggle;
            case "click_through_visible_signs" -> c.clickThroughVisibleSigns;
            case "click_through_invisible_signs" -> c.clickThroughInvisibleSigns;
            case "toggle_requires_permission" -> c.toggleRequiresPermission;
            case "require_empty_hand_for_toggle" -> c.requireEmptyHandForToggle;
            case "require_empty_hand_for_interaction" -> c.requireEmptyHandForInteraction;
            case "swap_keybind_and_sneak_roles" -> c.swapKeybindAndSneakRoles;
            case "keybind_alt" -> c.keybindAlt;
            case "keybind_control" -> c.keybindControl;
            case "keybind_shift" -> c.keybindShift;
            default -> false;
        };
    }

    private static boolean parseBool(String v) {
        return v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes");
    }

    @Override
    public String toString() {
        return "enable_item_frame_toggle=" + enableItemFrameToggle
                + ", affect_glow_item_frames=" + affectGlowItemFrames
                + ", click_through_visible_frames=" + clickThroughVisibleFrames
                + ", click_through_invisible_frames=" + clickThroughInvisibleFrames
                + ", enable_sign_toggle=" + enableSignToggle
                + ", click_through_visible_signs=" + clickThroughVisibleSigns
                + ", click_through_invisible_signs=" + clickThroughInvisibleSigns
                + ", toggle_requires_permission=" + toggleRequiresPermission
                + ", require_empty_hand_for_toggle=" + requireEmptyHandForToggle
                + ", require_empty_hand_for_interaction=" + requireEmptyHandForInteraction
                + ", swap_keybind_and_sneak_roles=" + swapKeybindAndSneakRoles
                + ", keybind_key_code=" + keybindKeyCode
                + ", keybind_alt=" + keybindAlt
                + ", keybind_control=" + keybindControl
                + ", keybind_shift=" + keybindShift;
    }

    /** JSON shape on disk; boxed so missing keys keep their defaults. */
    private static class Raw {
        Boolean enable_item_frame_toggle;
        Boolean affect_glow_item_frames;
        Boolean click_through_visible_frames;
        Boolean click_through_invisible_frames;
        Boolean enable_sign_toggle;
        Boolean click_through_visible_signs;
        Boolean click_through_invisible_signs;
        Boolean toggle_requires_permission;
        Boolean require_empty_hand_for_toggle;
        Boolean require_empty_hand_for_interaction;
        Boolean swap_keybind_and_sneak_roles;
        Integer keybind_key_code;
        Boolean keybind_alt;
        Boolean keybind_control;
        Boolean keybind_shift;
    }
}
