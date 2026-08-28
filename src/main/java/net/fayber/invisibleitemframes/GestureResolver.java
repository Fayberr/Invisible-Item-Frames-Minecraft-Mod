package net.fayber.invisibleitemframes;

// Turns a physical right-click gesture (sneak, the configurable toggle
// keybind, empty hand or not) into one of three logical actions. Shared by
// ItemFrameInteractionHandler and SignInteractionHandler so both stay in
// sync:
//
// - plain right-click -> PLAIN (per-case click-through if enabled, otherwise
//   vanilla interact)
// - shift + right-click -> INTERACT by default, or TOGGLE if swapped
// - keybind + right-click -> TOGGLE by default, or INTERACT (forced, skips
//   click-through) if swapped
//
// keybind wins over sneak if both are held. toggle normally works with
// anything in hand; when require_empty_hand_for_toggle is on and the hand
// isn't empty, we fall back to PLAIN instead so the click isn't eaten
// (e.g. placing an item into a frame while sneaking).
public final class GestureResolver {
    private GestureResolver() {}

    public enum Gesture {
        // per-case click-through if enabled, otherwise vanilla interact
        PLAIN,
        // flip visibility
        TOGGLE,
        // force vanilla interact (rotate item / edit sign text), even over click-through
        INTERACT
    }

    public static Gesture resolve(boolean shiftDown, boolean keybindDown, boolean swap, boolean handEmpty,
                                   boolean requireEmptyHandForToggle) {
        boolean toggleHandOk = !requireEmptyHandForToggle || handEmpty;
        if (keybindDown) {
            if (swap) {
                return Gesture.INTERACT;
            }
            return toggleHandOk ? Gesture.TOGGLE : Gesture.PLAIN;
        }
        if (shiftDown) {
            if (swap) {
                return toggleHandOk ? Gesture.TOGGLE : Gesture.PLAIN;
            }
            return Gesture.INTERACT;
        }
        return Gesture.PLAIN;
    }
}
