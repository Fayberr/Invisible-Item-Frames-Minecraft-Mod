# Invisible Item Frames - Modrinth page copy

Source of truth for the two text fields on
https://modrinth.com/mod/invisible-item-frames-mod (project id `ppenVjSy`).
Update here first, then PATCH `/v2/project/ppenVjSy` with
`{"description": ..., "body": ...}`. Voice: match Fabian's other mod pages
(Timber, Graves) - short, concrete, no enumerations.

## description (short summary line)

Gives the Capability to hide Item Frames and Signs. Also gives the option to make them clicktrough.

(Edited by Fabian on the page on 2026-08-28. His wording, keep it when syncing.)

## body (full page)

# Invisible Item Frames

Hide item frames and signs without losing what's in them: the item in the frame and the text on the sign stay visible.

## Features

- Hide any item frame or sign. Glow item frames work too.
- Toggle with a keybind (Left Alt by default) or Shift. Toggling needs an empty main hand. The keybind is set in the mod's config screen, not the vanilla Controls menu.
- Plain right-click clicks through to the block behind. You can turn that on separately for visible and invisible frames and signs.
- Shift + right-click does the normal action, so you can still edit a sign or put an item in a frame.

## Commands

- `/invisibleitemframes config` shows the current config. Requires operator level 2.
- `/invisibleitemframes config set <key> <value>` changes a setting.

## Notes

- The config lives in `config/invisibleitemframes.json`. In singleplayer you can also open it from the Mods screen; that GUI needs Mod Menu and Cloth Config, but they are optional and only needed for the GUI.
- Install on both the client and the server.
