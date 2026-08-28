package net.fayber.invisibleitemframes;

/**
 * Resolves a physical right-click gesture (sneak state, the configurable
 * toggle keybind, and whether the hand is empty) into one of three logical
 * actions, shared by {@link ItemFrameInteractionHandler} and
 * {@link SignInteractionHandler} so both stay in sync with the confirmed
 * gesture table:
 *
 * <ul>
 *   <li>Plain right-click: {@link Gesture#PLAIN} - per-case click-through if
 *       enabled, otherwise vanilla interact.</li>
 *   <li>Shift + right-click: {@link Gesture#INTERACT} (vanilla interact) by
 *       default, or {@link Gesture#TOGGLE} if the swap option is on.</li>
 *   <li>Keybind held + right-click: {@link Gesture#TOGGLE} by default, or
 *       {@link Gesture#INTERACT} (forced, bypassing click-through) if the
 *       swap option is on.</li>
 * </ul>
 *
 * <p>The keybind takes priority over sneak when both are held. Toggle always
 * requires an empty hand; if the hand is not empty when a gesture would
 * otherwise resolve to toggle, this falls back to {@link Gesture#PLAIN} so
 * the click is not eaten (e.g. placing an item into a frame while sneaking).
 */
public final class GestureResolver {
    private GestureResolver() {}

    public enum Gesture {
        /** Per-case click-through if enabled, otherwise vanilla interact. */
        PLAIN,
        /** Flip visibility. */
        TOGGLE,
        /** Force vanilla interact (rotate item / edit sign text), even if click-through would otherwise apply. */
        INTERACT
    }

    public static Gesture resolve(boolean shiftDown, boolean keybindDown, boolean swap, boolean handEmpty) {
        if (keybindDown) {
            if (swap) {
                return Gesture.INTERACT;
            }
            return handEmpty ? Gesture.TOGGLE : Gesture.PLAIN;
        }
        if (shiftDown) {
            if (swap) {
                return handEmpty ? Gesture.TOGGLE : Gesture.PLAIN;
            }
            return Gesture.INTERACT;
        }
        return Gesture.PLAIN;
    }
}
