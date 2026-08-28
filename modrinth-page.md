# Invisible Item Frames - Modrinth page copy

Source of truth for the two text fields on
https://modrinth.com/mod/invisible-item-frames-mod (project id `ppenVjSy`).
Update here first, then PATCH `/v2/project/ppenVjSy` with
`{"description": ..., "body": ...}`. Voice: match Fabian's other mod pages
(Timber, Graves) - short, concrete, no enumerations.

## description (short summary line)

Hide item frames and signs while keeping the item or text visible. Click through them to reach the block behind.

## body (full page)

# Invisible Item Frames

Hide item frames and signs while keeping what's in them visible: the item in the frame, the text on the sign. Plain right-clicks pass through to the block behind, so a hidden frame or sign never gets in the way.

## Features

- Hide any item frame with a keybind (Left Alt by default) or Shift. The item inside keeps showing.
- Same for signs: the sign disappears, the text stays.
- Click-through on plain right-clicks, set separately for visible and invisible frames and signs.
- Glow item frames work too.
- The keybind is set in the mod's config screen, not the vanilla Controls menu.

## How it works

- Plain right-click clicks through to the block behind, if enabled for that frame or sign.
- Shift + right-click does the normal action: edit a sign, put an item in a frame, even when click-through is on.
- Keybind + right-click toggles visibility. Needs an empty main hand.

## Commands

- `/invisibleitemframes config` shows the current config. Requires operator level 2.
- `/invisibleitemframes config set <key> <value>` changes a setting.

## Configuration

The config lives in `config/invisibleitemframes.json`. You can change it in-game with the commands above or, in singleplayer, from the Mods screen. The in-game config GUI needs Mod Menu and Cloth Config installed; both are optional and only needed for the GUI, never for the mod itself.

## Details

- Install on both the client and the server: sign invisibility changes the blockstate definition, so both sides need to agree on it.
- Frame hiding uses vanilla's own invisible flag and signs get a small blockstate property, so everything saves and syncs like vanilla.
- Toggles and click-through are decided on the server. With the mod installed, the click is sent as a dedicated packet, so other mods cannot swallow it.
