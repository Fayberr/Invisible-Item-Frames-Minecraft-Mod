# Invisible Item Frames

A Fabric mod that lets you toggle the visibility of item frames and signs,
and optionally click through them to interact with whatever is behind them.
The held item in a frame and a sign's text always keep rendering, only the
frame or sign's own model is hidden.

## Usage

Three gestures on an item frame or a sign, each independently configurable:

- **Plain right-click**: if click-through is enabled for that exact case
  (visible frame, invisible frame, visible sign, invisible sign are four
  separate toggles), the click passes through to whatever the frame or sign
  is mounted on. Otherwise it behaves exactly like vanilla (rotate the held
  item in a frame, edit a sign's text).
- **Shift + right-click**: toggles visibility, if the toggle is enabled.
- **Toggle keybind + right-click** (default: Left Alt): also toggles
  visibility, as an alternative to sneaking. Bound and shown in the mod's
  own config screen, not the vanilla Controls menu.

By default, toggling works with anything in hand, while interacting (rotating
a frame's item, or opening the sign editor) only works with an empty hand -
otherwise vanilla's own item behavior (dyeing a sign, sneak-placing a block,
...) takes over instead. Both are configurable independently, see
`require_empty_hand_for_toggle` and `require_empty_hand_for_interaction`
below.

`swap_keybind_and_sneak_roles` swaps which of Shift and the keybind toggles
visibility and which one always forces vanilla interact (bypassing
click-through, so you can still rotate an item or edit a sign even when
click-through is on). Plain right-click's behaviour is never affected by the
swap.

## Commands

- `/invisibleitemframes config` shows the current config. Requires operator
  level 2.
- `/invisibleitemframes config get` shows the current config.
- `/invisibleitemframes config set <key> <value>` sets one option live.

## Configuration

All options live in `config/invisibleitemframes.json` next to the other mod
configs. Change the file and restart, set them live with
`/invisibleitemframes config set`, or edit them from the Mods screen via
ModMenu in singleplayer.

- `enable_item_frame_toggle` (true): whether Shift/keybind right-click
  toggles item frame visibility at all.
- `affect_glow_item_frames` (true): whether glow item frames can also be
  toggled invisible. When false, glow item frames are left alone entirely.
- `click_through_visible_frames` (false): plain right-clicks on a visible
  frame reach the block behind it instead of the frame.
- `click_through_invisible_frames` (true): plain right-clicks on an already
  invisible frame reach the block behind it instead of the frame.
- `enable_sign_toggle` (true): whether Shift/keybind right-click toggles
  sign visibility at all.
- `click_through_visible_signs` (false): plain right-clicks on a visible
  sign reach the block it is mounted on instead of the sign.
- `click_through_invisible_signs` (false): plain right-clicks on an
  invisible sign reach the block it is mounted on instead of the sign.
- `swap_keybind_and_sneak_roles` (false): off, Shift toggles visibility and
  the keybind forces interact; on, the keybind toggles visibility and Shift
  forces interact.
- `keybind_key_code`, `keybind_alt`, `keybind_control`, `keybind_shift`: the
  toggle keybind (default: Left Alt, no other modifiers). Set from the mod's
  config screen; the raw JSON fields are keyboard-only (no mouse buttons).
- `toggle_requires_permission` (false): only operators (level 2) may toggle
  frame and sign visibility.
- `require_empty_hand_for_toggle` (false): when true, toggling visibility
  only works with an empty hand; with an item in hand the click falls back
  to plain right-click behavior instead.
- `require_empty_hand_for_interaction` (true): when true, rotating a frame's
  item or opening the sign editor via shift/keybind only works with an empty
  hand; with an item in hand, vanilla's own item behavior (dyeing a sign,
  sneak-placing a block, ...) takes over instead. When false, interaction is
  forced open even with an item in hand.

## Details

- Item frame invisibility reuses vanilla's own entity invisibility flag (the
  same one set by placing a frame with `Invisible:1b` NBT), so it is synced
  and saved by vanilla automatically.
- Sign invisibility adds a small blockstate property so the block renders
  nothing while toggled, the same trick vanilla uses for
  `minecraft:end_portal`. The sign's block entity (its text) keeps rendering
  regardless, since that is looked up by block entity type, not render shape.
- Because sign invisibility changes the block's state definition, **install
  this mod on every client, not just the server.** A vanilla client would
  actually render an invisible item frame correctly on its own (vanilla
  supports that flag natively), but it does not know about the extra sign
  property and would desync on sign blockstates without the mod.
- Click-through works by walking the player's crosshair ray out to their
  reach and skipping the frame or sign itself, so it targets exactly the
  block the click would have reached without the frame or sign, regardless
  of how it is mounted (wall, ceiling, or standing).
- Toggling, click-through, and the forced-interact override are decided on
  the server. With the mod installed on the client, the click is sent as a
  dedicated packet outside the vanilla interaction chain, so other mods
  cannot swallow it.
- The toggle keybind is picked from the mod's own config screen (Cloth
  Config's key-code picker if installed, or a simple "press a key" capture
  in the fallback screen), never the vanilla Controls menu, so it cannot
  collide with a vanilla or another mod's binding.
- Holding the keybind and right-clicking always reaches the server (item
  frames use a client-side mixin to catch it, since Fabric's entity-use
  event only fires on the server; signs use their existing block-use event,
  which fires on both sides).

## Requirements

- Fabric Loader 0.19.3 or newer for Minecraft 26.2.
- Fabric API for 26.2.
- Java 25.
- Optional: Mod Menu and Cloth Config (for the in-game GUI).

## Building

JDK 25 and Gradle 9.7 or newer (Loom 1.17.19).

```bash
./gradlew build
```

The jar is in `build/libs/`.

## License

GPL-3.0-or-later
