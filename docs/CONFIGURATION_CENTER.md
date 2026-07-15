# SCP Additions Configuration Center

SCP Additions includes a native in-game configuration center for its JSON-backed systems. It does not require Configured, Catalogue, or another configuration-menu mod.

## Opening the configuration center

While connected to a world, open **Mods → SCP Additions → Config** or run:

```text
/scpadditions config
```

Access is restricted to the integrated single-player owner and players with operator permission level 2 or higher. Existing item and contextual-interaction editors enforce the same permission on the server, including when packets are sent by a modified client.

## Safety model

The client never selects or writes arbitrary filesystem paths. It receives a structured snapshot of supported settings and sends structured changes back to the server. The server validates all edited sections, writes only known configuration files, reloads the affected systems, and reports errors without applying a partial state.

Configuration writes are atomic. Existing files receive a `.bak` copy before replacement. If validation or runtime reload fails after a save begins, the previous files are restored without overwriting the known-good backup.

Malformed existing JSON is rejected instead of being silently replaced by fallback defaults.

## Editors

The center includes dedicated editors for:

- gameplay modules and interface switches;
- SCP Inventory item categories and equipment effects;
- hidden Status effects;
- SCP-173 targets;
- Codex documents;
- contextual interactions;
- SCP-294 drinks;
- SCP-914 recipes.

Manual JSON editing remains supported for advanced, automated, or version-controlled modpack workflows.

## SCP-914 recipe editor

The SCP-914 editor is designed around the common intake → setting → output workflow.

- Search the item registry by translated item name or resource ID.
- Add any number of intake or output entries with the `+` controls.
- Adjust stack counts independently.
- Select the required machine setting: Rough, Coarse, 1:1, Fine, or Very Fine.
- Use ordinary outputs or weighted output selection.
- Duplicate recipes or move them between supported recipe files.
- Keep less common fields inside the collapsed **Additional recipe settings** section.

The additional section contains fields such as execution chance, input-NBT copying, action-bar text, and weighted-result controls. Existing entity inputs, entity outputs, and unknown JSON fields are preserved when a recipe is edited.

New recipes created in-game are stored in:

```text
config/scpadditions/914recipes.d/in_game_editor.json
```

This keeps user-created recipes separate from the shipped main recipe library in `914recipes.json`.

## Configuration files

The center manages the supported portions of these files:

```text
config/scpadditions/modules.json
config/scpinventory/scpinventory.json
config/scpinventory/context_interactions.json
config/scpadditions/294drinks.json
config/scpadditions/914recipes.json
config/scpadditions/914recipes.d/*.json
```

Use `/scpadditions reload` after external manual edits. Saves made through the in-game center reload their affected systems automatically.