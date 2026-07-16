# Configuration Center

SCP Additions includes a native server-authoritative configuration center.

Open it while connected to a world through:

```text
Mods → SCP Additions → Config
```

or:

```mcfunction
/scpadditions config
```

## Permissions

Configuration writes require:

- the integrated single-player owner; or
- operator permission level 2 or higher.

Clients edit a server snapshot. Saves are validated and applied by the server.

## Available editors

- **General & Modules** — inventory, interactions, HUD, vitals, movement, blink, and SCP-173 switches.
- **Inventory, Equipment & Codex** — item rules, equipment effects, hidden Status effects, SCP-173 targets, and documents.
- **Contextual Interactions** — block/entity prompts and anchors.
- **SCP-294 Drinks** — aliases, results, colors, effects, actions, and matching behavior.
- **SCP-914 Recipes** — explicit item/entity transformations and machine settings.

## Save behavior

Supported saves provide:

- JSON validation;
- automatic `.bak` backup creation;
- Windows-resilient replacement;
- rollback when reload fails;
- runtime reload without restarting the game;
- warnings for unavailable optional registry IDs.

Malformed JSON and malformed identifiers are rejected. Existing unknown fields are preserved wherever the editor does not own them.

## Item and interaction editor with K

Pressing `K` is the recommended quick workflow:

- hover an item in the SCP or vanilla inventory to edit its generic category and equipment effects;
- look at a block or entity to edit its contextual interaction;
- use `Forget` to remove an explicit item or interaction rule.

Documents are not assigned through the generic item-category editor. Use **Codex Documents** instead.

## Codex workflow

1. Open **Inventory, Equipment & Codex → Codex Documents**.
2. Create a paper-based temporary definition or edit an existing one.
3. Choose ordinary item matching or **Unique Generated Item**.
4. Import a PNG/JPG/JPEG image, write text, or reference packaged resources.
5. Press **Save Document** to persist and reload the definition.
6. In unique mode, use **Save & Give Test Item** to save first and then generate the NBT-tagged document.

## Reload Snapshot

`Reload Snapshot` discards unsaved local edits and requests the current server configuration again.
