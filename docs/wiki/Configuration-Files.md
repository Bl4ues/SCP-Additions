# Configuration Files

Configuration files are created under the instance's `config` directory. Bundled defaults are copied only when a file does not already exist. Updating the mod does not overwrite customized files.

| File | Purpose |
| --- | --- |
| `config/scpadditions/modules.json` | Gameplay and interface module switches. |
| `config/scpinventory/scpinventory.json` | Item routing, equipment effects, Status filters, Codex documents, and SCP-173 targets. |
| `config/scpinventory/context_interactions.json` | Block and entity prompt rules. |
| `config/scpadditions/294drinks.json` | SCP-294 matching and drink definitions. |
| `config/scpadditions/914recipes.json` | SCP-914 machine settings and main recipe list. |
| `config/scpadditions/914recipes.d/*.json` | Additional SCP-914 recipe fragments. |
| `config/scpadditions/scp914_skins/*.png` | SCP-914 1:1 skin pool. |

Use `/scpadditions reload` after external manual edits. In-game saves reload their affected systems automatically.

## Module switches

`modules.json` independently controls:

- SCP Inventory and remembered UI state;
- contextual interactions and Creative visibility;
- custom HUD;
- custom health, stamina, and survival movement;
- blinking;
- SCP-173 behavior and natural spawning.

Disabling the custom HUD does not automatically disable stamina gameplay.

## Inventory configuration

The principal sections of `scpinventory.json` are:

| Section | Meaning |
| --- | --- |
| `item_rules` | Generic item routing. Explicit rules override automatic classification. |
| `item_effects` | `NO_STAMINA` and `PROTECTED_EYES`. |
| `hidden_status_effects` | Effects hidden from the Status panel without removing gameplay behavior. |
| `codex_documents` | Item-backed documents, unique NBT IDs, images, and text. |
| `scp_173_targets` | Entity IDs or `#namespace:tag` observer/target entries. |

Generic item types include `MISCELLANEOUS`, `HARMFUL`, `CONSUMABLE`, `USABLE`, `PLACEABLE`, `KEY`, `COIN`, `AMMO`, `HEAD`, `ACCESSORY`, `ACCESSORY_HAND`, `CHEST`, `LEGS`, `FEET`, and `WEAPON`.

`CODEX` is internal and is not accepted as a generic item-rule type. Create documents through `codex_documents`.

## Codex assets

Packaged `image` and `text` fields use resource locations supplied by a mod or resource pack.

World-scoped imports are stored in:

```text
<world>/scp_additions/codex_assets/images
<world>/scp_additions/codex_assets/texts
```

The JSON stores `world_image` and `world_text` safe relative keys. Supported imported images are PNG, JPG, and JPEG up to 2.5 MB and 4096×4096 pixels. Text is UTF-8.

## Context interactions

The visual `K` editor is safer than manual editing. Important fields include:

- `type`: `block` or `entity`;
- `id`: registry ID;
- `range` and `priority`;
- `icon` and `useItem`;
- `text.action`, name mode, and visibility;
- local anchor position and optional world offset;
- click face and rotation mode;
- allowed input methods.

## Restoring shipped defaults

To regenerate a default file:

1. close the game/server;
2. back up the customized file and its `.bak`;
3. delete only the file to regenerate;
4. start the same installed mod version again.

Do not delete an entire configuration directory unless every customization in it is disposable.
