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
 * <p>Shift right-clicking an item frame or sign with an empty hand always
 * toggles its visibility; that gesture is not configurable. What is
 * configurable is whether the toggle is enabled at all, and whether
 * interactions click through to whatever is behind the frame or sign.
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
    public boolean clickThroughSigns = false;

    // Multiplayer safety.
    public boolean toggleRequiresPermission = false;

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
        if (raw.click_through_signs != null) c.clickThroughSigns = raw.click_through_signs;
        if (raw.toggle_requires_permission != null) c.toggleRequiresPermission = raw.toggle_requires_permission;
    }

    public static void save() {
        Raw raw = new Raw();
        raw.enable_item_frame_toggle = INSTANCE.enableItemFrameToggle;
        raw.affect_glow_item_frames = INSTANCE.affectGlowItemFrames;
        raw.click_through_visible_frames = INSTANCE.clickThroughVisibleFrames;
        raw.click_through_invisible_frames = INSTANCE.clickThroughInvisibleFrames;
        raw.enable_sign_toggle = INSTANCE.enableSignToggle;
        raw.click_through_signs = INSTANCE.clickThroughSigns;
        raw.toggle_requires_permission = INSTANCE.toggleRequiresPermission;
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
            case "click_through_signs" -> c.clickThroughSigns = parseBool(value);
            case "toggle_requires_permission" -> c.toggleRequiresPermission = parseBool(value);
            default -> {
                return false;
            }
        }
        save();
        return true;
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
            case "click_through_signs" -> c.clickThroughSigns;
            case "toggle_requires_permission" -> c.toggleRequiresPermission;
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
                + ", click_through_signs=" + clickThroughSigns
                + ", toggle_requires_permission=" + toggleRequiresPermission;
    }

    /** JSON shape on disk; boxed so missing keys keep their defaults. */
    private static class Raw {
        Boolean enable_item_frame_toggle;
        Boolean affect_glow_item_frames;
        Boolean click_through_visible_frames;
        Boolean click_through_invisible_frames;
        Boolean enable_sign_toggle;
        Boolean click_through_signs;
        Boolean toggle_requires_permission;
    }
}
