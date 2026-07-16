from pathlib import Path

files = {
"README.md": r'''# SCP Additions

![SCP Additions banner](https://cdn.modrinth.com/data/cached_images/b9d834bc5afc41d56f44146c8c8521d6170d723c_0.webp)

SCP Additions is an SCP survival horror and facility-building mod for Minecraft 1.20.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.

## Links

- [Download on Modrinth](https://modrinth.com/mod/scp-additions)
- [Documentation Wiki](https://github.com/Bl4ues/SCP-Additions/wiki)
- [Wiki source mirror](docs/wiki/Home.md)
- [Changelog](CHANGELOG.md)
- [Issue tracker](https://github.com/Bl4ues/SCP-Additions/issues)

## Requirements

- Minecraft **1.20.1**
- Forge **47.4.10** or newer
- GeckoLib **4.4.9** or newer
- Java **17**

Optional client integrations:

- [Kleiders Custom Renderer API](https://modrinth.com/mod/kleiders-custom-renderer-api) renders SCP-914 **1:1** player skins. All other SCP-914 behavior works without it.
- [MoreMcmeta Emissive Textures](https://modrinth.com/mod/moremcmeta-emissive) enables supported emissive facility textures. Its MoreMcmeta base dependency is also required.

## Main features

- Functional SCPs including SCP-173, SCP-131, SCP-294, SCP-914, SCP-330, SCP-426, SCP-572, SCP-902, and SCP-1176.
- SCP Unity-inspired inventory, Status, Codex, health, stamina, movement, blink, and interaction systems.
- Animated facility doors, keycard readers, Tesla Gates, decontamination machinery, terminals, lights, props, and modular building blocks.
- Server-authoritative configuration center for modules, inventory rules, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes.
- Broad vanilla and modded integration through registry-based configuration, automatic item classification, and SCP-914 recipe inference.

## Installation

1. Install Minecraft 1.20.1 with Forge 47.4.10 or newer.
2. Install GeckoLib 4.4.9 or newer.
3. Place the SCP Additions JAR in the instance's `mods` folder.
4. Install the optional client integrations above only when their visual features are wanted.

The complete usage guide, controls, commands, configuration reference, and troubleshooting information are maintained in the [Wiki](https://github.com/Bl4ues/SCP-Additions/wiki). A source mirror is kept in [`docs/wiki`](docs/wiki/Home.md).

## Configuration

While connected to a world, open:

```text
Mods → SCP Additions → Config
```

or run:

```mcfunction
/scpadditions config
```

The integrated single-player owner and operators with permission level 2 or higher can edit supported systems with validation, automatic `.bak` backups, rollback, and runtime reload. Existing customized configuration files are never silently replaced by new defaults.

## Building from source

The project uses Java 17 and the included Gradle wrapper:

```bash
./gradlew clean build
```

The compiled JAR is written to `build/libs`.

## License and attribution

SCP Additions is released under the [Creative Commons Attribution-ShareAlike 3.0 license](LICENSE.md).

Content relating to the SCP Foundation, including the SCP Foundation logo and SCP concepts, originates from the [SCP Wiki](https://scp-wiki.wikidot.com/) and its respective authors and is available under Creative Commons Attribution-ShareAlike 3.0.

SCP Unity is the main visual and mechanical inspiration for the project. Special thanks to [SCP: Overtime](https://modrinth.com/mod/scp-overtime) for inspiring the original creation of SCP Additions. Various facility assets were adapted from or inspired by SCP Unity-related blocks in its 1.16.5 version.

SCP Additions is not an official Minecraft product and is not affiliated with Mojang, Microsoft, the SCP Wiki staff, SCP Unity, or Northwood Studios.
''',

"LICENSE.md": r'''# License

SCP Additions is licensed under the **Creative Commons Attribution-ShareAlike 3.0 Unported license (CC BY-SA 3.0)**.

The full license terms are available at:

- https://creativecommons.org/licenses/by-sa/3.0/
- https://creativecommons.org/licenses/by-sa/3.0/legalcode

Under this license, you may share and adapt the project provided that you:

1. give appropriate credit;
2. indicate whether changes were made; and
3. distribute adapted material under the same or a compatible license.

## SCP attribution

Content relating to the SCP Foundation, including the SCP Foundation logo and SCP concepts, originates from the [SCP Wiki](https://scp-wiki.wikidot.com/) and its respective authors. SCP Wiki content is also licensed under CC BY-SA 3.0.

When redistributing SCP Additions or a derivative work, preserve attribution to the SCP Wiki and the authors of the SCP concepts used by the project.

## Project attribution

Project: **SCP Additions**  
Author: **Bl4ues**  
Repository: https://github.com/Bl4ues/SCP-Additions

SCP Unity is the primary visual and mechanical inspiration for SCP Additions. SCP: Overtime inspired the project's original creation. Some facility assets were adapted from or inspired by SCP Unity-related blocks distributed with the 1.16.5 version of SCP: Overtime.

## Disclaimer

SCP Additions is an unofficial fan project. It is not affiliated with or endorsed by Mojang, Microsoft, the SCP Wiki staff, SCP Unity, Northwood Studios, or the creators of SCP: Containment Breach.

Third-party libraries and optional integrations remain subject to their own licenses.
''',

"docs/wiki/Home.md": r'''# SCP Additions Wiki

SCP Additions is an SCP survival horror and facility-building mod for Minecraft 1.20.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.

![SCP Additions banner](https://cdn.modrinth.com/data/cached_images/b9d834bc5afc41d56f44146c8c8521d6170d723c_0.webp)

## Start here

- [[Getting Started]] — requirements, installation, first launch, and default controls.
- [[Gameplay Systems]] — inventory, Status, Codex, contextual interactions, vitals, blink, and building content.
- [[SCPs and Facility Systems]] — SCP behavior, doors, readers, Tesla Gates, decontamination, and SCP-079.
- [[Configuration Center]] — the recommended in-game editing workflow.
- [[Configuration Files]] — manual JSON reference and default-file behavior.
- [[SCP-294]] — drinks, matching, colors, effects, and actions.
- [[SCP-914]] — explicit recipes, inferred transformations, entities, players, and skins.
- [[Commands and Gamerules]] — administration and world controls.
- [[Compatibility and Troubleshooting]] — updates, optional mods, resets, and common problems.
- [[Development and Licensing]] — building from source, contribution notes, credits, and license.

## Downloads and support

- [Modrinth](https://modrinth.com/mod/scp-additions)
- [GitHub repository](https://github.com/Bl4ues/SCP-Additions)
- [Changelog](https://github.com/Bl4ues/SCP-Additions/blob/master/CHANGELOG.md)
- [Issue tracker](https://github.com/Bl4ues/SCP-Additions/issues)

The Wiki documents the current **3.0.x** codebase. Existing worlds and customized configuration files should always be backed up before changing versions.
''',

"docs/wiki/_Sidebar.md": r'''**SCP Additions Wiki**

- [[Home]]
- [[Getting Started]]
- [[Gameplay Systems]]
- [[SCPs and Facility Systems]]
- [[Configuration Center]]
- [[Configuration Files]]
- [[SCP-294]]
- [[SCP-914]]
- [[Commands and Gamerules]]
- [[Compatibility and Troubleshooting]]
- [[Development and Licensing]]

---

- [Modrinth](https://modrinth.com/mod/scp-additions)
- [GitHub](https://github.com/Bl4ues/SCP-Additions)
- [Report an issue](https://github.com/Bl4ues/SCP-Additions/issues)
''',

"docs/wiki/Getting-Started.md": r'''# Getting Started

## Requirements

### Required

- Minecraft 1.20.1
- Forge 47.4.10 or newer
- GeckoLib 4.4.9 or newer
- Java 17

### Optional client integrations

- **Kleiders Custom Renderer API** renders the custom player skins selected by SCP-914 on **1:1**. Every other SCP-914 outcome works without it.
- **MoreMcmeta Emissive Textures** enables emissive overlays for supported lights and glowing facility textures. Its MoreMcmeta base dependency is also required.

## Installation

1. Create or select a Minecraft 1.20.1 Forge instance.
2. Install GeckoLib.
3. Place the SCP Additions JAR in the instance's `mods` directory.
4. Add optional integrations only when their visual features are wanted.
5. Start the game once to create the default configuration files.

The defaults are copied only when a matching file does not already exist. Updating the mod never silently overwrites a customized configuration.

## Default controls

All registered keybinds can be changed in Minecraft's Controls menu.

| Key | Action |
| --- | --- |
| `Tab` | Open the SCP Inventory. |
| `E` | Use the focused contextual interaction. Rules may also accept right-click. |
| `K` | Edit a hovered item, or edit the block/entity currently being viewed. |
| Press or hold `B` | Blink manually or keep the player's eyes closed. |
| Hold `G` | Dismiss owned SCP-131 followers within 64 blocks. |

## First configuration

Open a world, then use either:

```text
Mods → SCP Additions → Config
```

or:

```mcfunction
/scpadditions config
```

The integrated single-player owner may edit settings immediately. On a dedicated server, permission level 2 or higher is required.

## Recommended first checks

- Open the SCP Inventory with `Tab`.
- Confirm that health, stamina, and blink displays appear as expected.
- Open the configuration center and review the module switches.
- Back up the world and `config` directory before replacing defaults or changing versions.
''',

"docs/wiki/Gameplay-Systems.md": r'''# Gameplay Systems

## SCP Inventory

The SCP Inventory is server-authoritative and stored as a player capability. Its default limits are:

- 12 main-item slots;
- 12 key slots;
- 999 units of currency.

The main-item limit can be changed per player from 1 to 128 through commands.

Items are routed into general storage, consumables, usable/placeable items, harmful items, keys, ammunition, currency, equipment, or configured Codex documents. Explicit `item_rules` override automatic classification.

Without an explicit rule, the mod conservatively recognizes:

- consumables;
- vanilla armor slots;
- placeable blocks;
- common melee and ranged weapons;
- manually usable right-click items.

`USABLE` and `PLACEABLE` items temporarily enter a controlled vanilla hotbar session so their normal right-click or placement behavior can run. They return to SCP storage unless consumed.

`CODEX` is an internal runtime category. It cannot be assigned through generic item rules; documents must be created through `codex_documents`.

## Status panel

The Status panel shows:

- active effects, except those hidden by configuration;
- maximum health;
- armor and toughness;
- attack value;
- stamina;
- the player's blood type.

Effect-duration bars start full when an effect is applied and decrease relative to that effect's original duration.

## Codex

The Codex displays configured document items. A document can use:

- a packaged image resource;
- a packaged UTF-8 text resource;
- a world-scoped imported image;
- world-scoped directly written text;
- or a combination of image and text.

The editor accepts PNG, JPG, and JPEG images up to 2.5 MB and 4096×4096 pixels. Imported assets are stored under the active world's `scp_additions/codex_assets` directory; the JSON stores only safe relative references.

New definitions currently start with `minecraft:paper` as a temporary base item. The item can be changed to any registered vanilla or modded item. Unique mode adds `ScpCodexId` NBT so only the generated item becomes that document.

## Contextual interactions

Context rules attach SCP Unity-style prompts to blocks and entities. A rule can configure:

- action and display name;
- range and priority;
- icon and required item mode;
- local anchor and world offset;
- simulated click face;
- `E`, right-click, or both.

The server validates the interaction again before invoking the target. Press `K` while looking at a block or entity to use the visual editor.

Anchor controls include the arrow keys, `Page Up`, `Page Down`, and the mouse wheel. `Shift` changes the step to `0.10`, `Ctrl` changes it to `0.01`, and the normal step is `0.05`.

## Health, stamina, movement, and blink

The custom HUD can replace vanilla hearts and display health, stamina, and blink information.

The survival movement module slows ordinary walking and makes sprinting a committed, stamina-limited action. `NO_STAMINA` item effects prevent sprinting while the configured item is held, worn, or equipped.

Blinking is synchronized with the server. Manual blinking may be pressed or held. **Eye Sore** accelerates blink drain; **Lubricated Eye** removes and prevents Eye Sore, doubles the blink interval, and changes the blink presentation while active.

## Facility-building content

The creative inventory includes SCP Unity-inspired:

- Sector 1 and Sector 2 walls and floors;
- directional markings;
- doors and button panels;
- containment and office structures;
- ventilation pieces;
- lights, heaters, signs, televisions, terminals, and props.

Supported lights can use optional MoreMcmeta emissive overlays.
''',

"docs/wiki/SCPs-and-Facility-Systems.md": r'''# SCPs and Facility Systems

## SCP-173

SCP-173 movement and attacks are server-owned. Observation samples the center, faces, and corners of its hitbox and treats the complete forward half of the camera as visible.

Solid collision or visual shapes block observation. Glass, panes, iron bars, leaves, and supported facility-door windows remain transparent to the observation check. Creative and Spectator players do not activate or become targets for SCP-173.

Routine natural spawning can be enabled independently. A routine-spawned SCP-173 begins inert until seen and may despawn when no Survival player remains nearby.

Configured entity IDs and tags in `scp_173_targets` can both attract SCP-173 and hold it still while they have line of sight.

## SCP-131-A and SCP-131-B

Right-clicking an SCP-131 starts nearby SCP-131 entities following the same player. Ownership persists through save/reload. Holding the configurable dismiss key removes the player's group.

SCP-131 searches for nearby SCP-173 entities, moves to a useful viewing position, and acts as an observer while it has line of sight.

## Facility doors, buttons, and keycard readers

The facility set includes multiple animated door families with saved timing, collision, sounds, and passability. Intermediate animation states remain registered for old-world compatibility but are hidden from creative tabs.

One public **Keycard Reader** item supports Levels 1–6. With a **Screwdriver** in either hand:

- normal interaction selects the required level;
- crouch-interaction copies a reader's level;
- holding `Ctrl` applies the copied level to another reader.

Higher-level keycards satisfy their own level and every lower level. Readers search both vanilla storage and the SCP Inventory.

## Tesla Gates and terminal

Tesla Gates detect nearby entities and transition into a lethal discharge. Emergency Override increases range, shortens warning time, and strengthens feedback.

The Tesla Gate Terminal requires Security Credentials and controls global gamerules. Existing gates are periodically synchronized and intermediate saved shock states are recovered.

## SCP-079 facility control

SCP-079 control is disabled by default. When enabled, it can manipulate eligible connected heavy doors during chases or occasional unprovoked events. Bare doors and unrelated redstone do not grant control; a functional connected button, keycard reader, or Facility Pulse Node is required.

## Decontamination Checkpoint

The checkpoint detects players inside its modeled chamber, closes once per visit, removes active effects, applies Eye Sore unless protected, plays its cycle, fills the chamber with gas, and reopens through a saved block tick.

`PROTECTED_EYES` items and Lubricated Eye prevent the irritation. The `deconCheckpoint` gamerule additionally makes the checkpoint save the player's respawn position.

## Other anomalous content

- **SCP-330:** configurable candy outcomes and the two-candy limit.
- **SCP-426:** a toaster whose descriptions change around its anomalous identity.
- **SCP-572:** a sword that grants confidence beyond its actual combat quality.
- **SCP-902:** a sealed box with proximity and interaction behavior.
- **SCP-1176:** the Mellified Man, anomalous honey, blood-type outcomes, player-following music, and honey-colored visual feedback.
- **Blood type system:** assigns and displays the blood type used by SCP-1176 mechanics.

SCP-294 and SCP-914 have dedicated pages because their configuration systems are substantially larger.
''',

"docs/wiki/Configuration-Center.md": r'''# Configuration Center

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
''',

"docs/wiki/Configuration-Files.md": r'''# Configuration Files

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
''',

"docs/wiki/SCP-294.md": r'''# SCP-294

SCP-294 matches typed requests against configurable drink IDs and aliases.

## Matching

Matching is case- and punctuation-insensitive:

1. exact aliases are checked first;
2. optional partial matching follows;
3. remaining candidates use fuzzy similarity against `fuzzy_threshold`.

Every drink also accepts its ID path and the path with underscores converted to spaces.

## Drink definitions

A drink can configure:

- ID, enabled state, and aliases;
- result item and count;
- delay and pouring sound;
- coin use;
- cup color;
- whether the result is drinkable;
- refusal and action-bar text;
- potion effects;
- dispense and drink actions.

The configuration center includes a color picker and shows the configured cup color in drink and SCP-914 recipe lists.

## Supported actions

- `actionbar`
- `message`
- `sound`
- `particle`
- `visual_explosion`
- `effect`
- `remove_effect`
- `heal`
- `hurt`
- `kill`
- `set_fire`

Every action may use `delay_ticks`. The older `actions` array is accepted as an additional alias for `drink_actions`.

Entity spawning is intentionally not a supported SCP-294 output.

## Reloading

Use the configuration center's save action or:

```mcfunction
/scpadditions reload
```

Unavailable optional item, effect, sound, or particle IDs are reported without invalidating unrelated drink entries.
''',

"docs/wiki/SCP-914.md": r'''# SCP-914

SCP-914 processes items, entities, and players inside a configurable intake area relative to its winding key.

## Recipe priority

Explicit JSON recipes are authoritative. When an explicit recipe matches the selected setting and intake, it always wins.

Only when no explicit recipe matches may SCP-914 infer a conservative transformation from registered crafting recipes and material relationships. This provides broad vanilla and modded integration without making configured recipes unreliable.

## Intake behavior

Once a valid cycle starts, the complete detected item intake is consumed, even when only part of it was needed for the selected result.

If the machine is wound without a valid intake, it waits for up to **40 ticks (two seconds)**. A solo player can enter during that window. If nothing valid enters, the attempt is abandoned.

## Settings

- **Rough** — destructive disassembly or severe player outcome.
- **Coarse** — partial component recovery or destructive player outcome.
- **1:1** — equivalent-category transformations; players receive a random configured skin.
- **Fine** — constructive crafting/refinement; players receive temporary enhancement followed by collapse.
- **Very Fine** — stronger refinement and stronger temporary player enhancement followed by delayed collapse.

## Explicit recipe fields

- `setting`: `rough`, `coarse`, `1_to_1`, `fine`, or `very_fine`;
- `item_inputs` and counts;
- `entity_inputs`, counts, and optional `consume`;
- `item_outputs`;
- `weighted_item_outputs`, selecting one result by relative weight;
- `entity_outputs`;
- `chance` from 0.0 to 1.0;
- `copy_input_nbt`.

Fragment files in `914recipes.d` append recipes after the main file in filename order. Repeating an ID does not override an earlier recipe.

## Inferred behavior

The fallback resolver may:

- select crafting recipes that consume available materials;
- infer equipment disassembly by material tier;
- avoid extreme 1:1 quality jumps;
- use Fine behavior as a fallback for Very Fine where appropriate.

The resolver never overrides an explicit configured match.

## Entity recipes

Entity inputs and outputs remain explicit because they cannot be derived reliably from crafting recipes. The in-game list renders lightweight cached previews for visible entity rows and uses a generic fallback when a safe preview cannot be created.

## Player skins

The current default pool contains five numbered skins:

```text
skin1.png
skin2.png
skin3.png
skin4.png
skin5.png
```

Additional 64×64 or legacy 64×32 Minecraft skin PNGs may be placed in:

```text
config/scpadditions/scp914_skins
```

The server synchronizes only the selected filename, not PNG bytes. Every multiplayer client that renders the skin must have a PNG with the same filename and Kleiders Custom Renderer API installed.

## Assembly Kit

The SCP-914 Assembly Kit places the complete machine structure and refuses placement when the required volume is blocked.
''',

"docs/wiki/Commands-and-Gamerules.md": r'''# Commands and Gamerules

## Main commands

| Command | Effect |
| --- | --- |
| `/scpadditions config` | Open the native configuration center. |
| `/scpadditions reload` | Validate and reload all supported configuration files. |

Reloads reject malformed JSON and malformed IDs. Valid but unavailable optional registry IDs are reported as warnings and skipped.

## Inventory administration

Permission level 2 is required.

| Command | Effect |
| --- | --- |
| `/scpinventory reset [targets]` | Reset complete SCP Inventory state and main capacity. |
| `/scpinventory clear [targets]` | Clear main SCP item storage. |
| `/scpinventory clearmain [targets]` | Alias of `clear`. |
| `/scpinventory setmax [targets] <slots>` | Set main capacity from 1 to 128. |
| `/scpinventory maxslots ...` | Alias of `setmax`. |
| `/scpinventory getmax` | Report used/main capacity and key count. |

## Context interaction commands

The visual editor opened with `K` is recommended for normal use.

| Command | Effect |
| --- | --- |
| `/scpinventory context gui` | Open the editor for the viewed entity or block. |
| `/scpinventory context select` | Select the viewed block for command editing. |
| `/scpinventory context add` | Add a default rule for the selected target. |
| `/scpinventory context cancel` | Cancel the active session. |
| `/scpinventory context done` | Finish editing. |
| `/scpinventory context reload` | Reload `context_interactions.json`. |
| `/scpinventory context marker` | Show the current anchor marker. |
| `/scpinventory context set action <text>` | Set the action label. |
| `/scpinventory context set name <text>` | Set the manual name. |
| `/scpinventory context set range <0.25..64>` | Set reach. |
| `/scpinventory context input <mode>` | Set `both`, `e`, or `right_click`. |
| `/scpinventory context item <mode>` | Set `hand` or `card`. |
| `/scpinventory context clickface <face>` | Set the simulated click face. |
| `/scpinventory context rotate <mode>` | Set anchor rotation behavior. |
| `/scpinventory context anchor hit` | Use the selected hit point. |
| `/scpinventory context anchor here <distance>` | Place the anchor in front of the player. |
| `/scpinventory context anchor nudge <x> <y> <z>` | Apply local offsets. |

## Gamerules

| Gamerule | Default | Behavior |
| --- | --- | --- |
| `teslaGateOn` | `true` | Global Tesla Gate state. |
| `teslaGateManualOverride` | `false` | Enables Emergency Override and forces Tesla Gates on. |
| `scp079controlOn` | `false` | Allows SCP-079 to manipulate eligible connected heavy doors. |
| `deconCheckpoint` | `false` | Makes the Decontamination Checkpoint save a processed player's respawn position. |

Examples:

```mcfunction
/gamerule teslaGateOn false
/gamerule teslaGateManualOverride true
/gamerule scp079controlOn true
/gamerule deconCheckpoint true
```
''',

"docs/wiki/Compatibility-and-Troubleshooting.md": r'''# Compatibility and Troubleshooting

## Updating the mod

- Back up important worlds and configuration directories first.
- Existing published `scp_additions` registry IDs are retained where required for old-world compatibility.
- Intermediate animation blocks remain registered so old saves resolve them, but they are hidden from creative tabs.
- Existing customized configuration files are never automatically replaced.

## Missing optional mod content

Configuration entries may reference optional mods such as `scpo`. If an item, entity, effect, sound, or particle ID is unavailable, the affected entry is skipped and reported while unrelated entries remain active.

Malformed resource IDs are different: they invalidate the reload and keep the previous active configuration.

## Regenerating defaults

Close the game/server, back up the relevant JSON and `.bak`, delete only the file to regenerate, and start the installed version again.

Do not delete `914recipes.d`, Codex world assets, or the entire configuration directory unless those customizations are disposable.

## Codex image or text does not appear

1. Reopen the document editor and confirm `World image attached` or `world text attached`.
2. Press **Save Document** or **Save & Give Test Item**.
3. Confirm the document remains in the configuration list after reopening it.
4. Check the world's `scp_additions/codex_assets` directory.
5. Confirm the image is PNG/JPG/JPEG, at most 2.5 MB, and no larger than 4096×4096.

Unique generated documents must be created from the Codex editor. Do not assign `CODEX` through a generic item rule.

## SCP-914 skins do not render in multiplayer

The server synchronizes only the filename. Every relevant client must have the same PNG filename in `config/scpadditions/scp914_skins` and install Kleiders Custom Renderer API.

## Configuration save fails on Windows

The mod uses backup creation, atomic replacement where available, retries, and a safe fallback. If a save still fails:

- confirm the file is not read-only;
- close external editors holding the file;
- verify write permission for the instance directory;
- preserve the `.bak` before manual recovery.

## Reporting a bug

Use the [GitHub issue tracker](https://github.com/Bl4ues/SCP-Additions/issues) and include:

- SCP Additions version;
- Forge and GeckoLib versions;
- complete mod list;
- exact reproduction steps;
- `latest.log` and crash report when applicable;
- screenshots for visual or interface problems.
''',

"docs/wiki/Development-and-Licensing.md": r'''# Development and Licensing

## Building from source

SCP Additions uses Java 17 and the included Gradle wrapper:

```bash
./gradlew clean build
```

The compiled JAR is written to:

```text
build/libs
```

Run configurations generated for Forge may be used from IntelliJ IDEA for normal development testing.

## Repository workflow

Small, low-risk fixes, documentation changes, and isolated quality-of-life improvements may be committed directly to `master` after validation. Larger or riskier systems should normally be developed and tested in dedicated branches before integration.

Preserve public registry IDs and world compatibility unless a migration is deliberate and documented.

## License

SCP Additions is released under **Creative Commons Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)**.

- [License summary](https://creativecommons.org/licenses/by-sa/3.0/)
- [Legal code](https://creativecommons.org/licenses/by-sa/3.0/legalcode)
- [Repository license notice](https://github.com/Bl4ues/SCP-Additions/blob/master/LICENSE.md)

## SCP attribution

Content relating to the SCP Foundation, including the Foundation logo and SCP concepts, originates from the [SCP Wiki](https://scp-wiki.wikidot.com/) and its respective authors. SCP Wiki content is licensed under CC BY-SA 3.0.

## Credits

SCP Unity is the project's primary visual and mechanical inspiration. Special thanks to SCP: Overtime for inspiring the original creation of SCP Additions. Various facility assets were adapted from or inspired by SCP Unity-related blocks in its 1.16.5 version.

## Disclaimer

SCP Additions is an unofficial fan project. It is not affiliated with or endorsed by Mojang, Microsoft, the SCP Wiki staff, SCP Unity, Northwood Studios, or the creators of SCP: Containment Breach.
''',

"docs/wiki/README.md": r'''# GitHub Wiki source mirror

This directory contains the organized source pages intended for the SCP Additions GitHub Wiki.

GitHub stores an initialized Wiki in a separate Git repository named `SCP-Additions.wiki.git`. The normal repository Contents API cannot create the first Wiki page, so this mirror keeps every page versioned with the main project until the Wiki is initialized and published.

Expected Wiki files:

- `Home.md`
- `_Sidebar.md`
- `Getting-Started.md`
- `Gameplay-Systems.md`
- `SCPs-and-Facility-Systems.md`
- `Configuration-Center.md`
- `Configuration-Files.md`
- `SCP-294.md`
- `SCP-914.md`
- `Commands-and-Gamerules.md`
- `Compatibility-and-Troubleshooting.md`
- `Development-and-Licensing.md`

After the first page is created from the repository's **Wiki** tab, these files can be copied into the Wiki repository without rewriting their internal `[[Page Name]]` links.
''',
}

for relative, content in files.items():
    path = Path(relative)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")

print(f"Wrote {len(files)} documentation files")
