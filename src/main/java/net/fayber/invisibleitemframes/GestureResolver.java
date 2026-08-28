package net.fayber.invisibleitemframes;

// Turns a right-click gesture (sneak, the toggle keybind, empty hand or not)
// into one of the three logical actions below. Shared by
// ItemFrameInteractionHandler and SignInteractionHandler so frames and signs
// can't drift out of sync on what a given gesture means.
//
// - plain right-click: PLAIN (click-through if enabled, else vanilla interact)
// - shift + right-click: INTERACT normally, TOGGLE if swap_keybind_and_sneak_roles is on
// - keybind + right-click: TOGGLE normally, INTERACT (forced) if swap is on
//
// keybind wins over sneak if both are held. toggle can be gated to require an
// empty hand (off by default); if that's on and the hand isn't empty, this
// falls back to PLAIN instead of eating the click - e.g. placing an item into
// a frame while sneaking still works.
public final class GestureResolver {
    private GestureResolver() {}

    public enum Gesture {
        PLAIN,    // per-case click-through if enabled, otherwise vanilla interact
        TOGGLE,   // flip visibility
        INTERACT  // force vanilla interact (rotate item / edit sign), bypassing click-through
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
