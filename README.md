# Invisible Item Frames

A Fabric mod that lets you shift right-click an item frame or a sign with an
empty hand to toggle its visibility, and optionally makes frames and signs
click-through so interactions reach whatever is behind them.

## Usage

- Shift right-click an item frame with an empty hand: hides the frame itself.
  The held item and its rotation keep rendering, exactly like a vanilla
  `Silent` + `Invisible` item frame placed with NBT.
- Shift right-click a sign with an empty hand: hides the sign's post/plank
  model. The sign's text keeps rendering.
- The gesture (shift + empty hand) always applies and is not configurable.
  What is configurable is whether the toggle is enabled at all, and whether
  interactions click through to the block behind.

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

- `enable_item_frame_toggle` (true): whether shift right-click with an empty
  hand toggles item frame visibility at all.
- `affect_glow_item_frames` (true): whether glow item frames can also be
  toggled invisible. When false, glow item frames are left alone entirely.
- `click_through_visible_frames` (false): interactions on a still-visible
  frame reach the block behind it instead of the frame.
- `click_through_invisible_frames` (true): interactions on an already
  invisible frame reach the block behind it instead of the frame.
- `enable_sign_toggle` (true): whether shift right-click with an empty hand
  toggles sign visibility at all.
- `click_through_signs` (false): interactions on a sign reach the block it is
  mounted on instead of the sign.
- `toggle_requires_permission` (false): only operators (level 2) may toggle
  frame and sign visibility.

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
- Click-through works by re-raycasting from just past the original hit point
  out to the player's reach, so it works the same way regardless of how the
  frame or sign is mounted (wall, ceiling, or standing).

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
