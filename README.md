# SCP Additions

![SCP Additions banner](https://cdn.modrinth.com/data/cached_images/b9d834bc5afc41d56f44146c8c8521d6170d723c_0.webp)

SCP Additions is a Forge mod for Minecraft 1.20.1 focused on SCP survival-horror gameplay and facility building. It includes functional SCPs, containment machinery, security systems, a custom inventory and HUD, animated doors, and a large SCP Unity-inspired construction set.

This README is the technical reference for the current 3.0.x codebase. For a visual showcase, screenshots, GIFs, and recommended companion mods, see the [Modrinth page](https://modrinth.com/mod/scp-additions).

## Contents

- [Requirements](#requirements)
- [Default controls](#default-controls)
- [System reference](#system-reference)
- [Configuration](#configuration)
- [Gamerules](#gamerules)
- [Commands](#commands)
- [Compatibility and updates](#compatibility-and-updates)
- [Building from source](#building-from-source)
- [Acknowledgements and license](#acknowledgements-and-license)

## Requirements

### Required

- Minecraft 1.20.1
- Forge 47.4.10 or newer
- [GeckoLib 4.4.9](https://modrinth.com/mod/geckolib) or newer

### Optional client-side enhancements

- [Kleiders Custom Renderer API](https://modrinth.com/mod/kleiders-custom-renderer-api) renders the custom player skins selected by SCP-914 on the **1:1** setting. Every other SCP-914 outcome works without it.
- [MoreMcmeta Emissive Textures](https://modrinth.com/mod/moremcmeta-emissive) enables emissive overlays for supported facility lights and glowing textures. Its MoreMcmeta base dependency is also required.

## Default controls

The registered keybinds can be changed in Minecraft's Controls menu unless noted otherwise.

| Key | Action |
| --- | --- |
| `Tab` | Open the SCP Inventory. |
| `E` | Use the focused contextual interaction. Rules may also accept right-click. |
| `K` | Open the item editor for a hovered inventory item, or the interaction editor for the block/entity being viewed. |
| Hold `Space` | Keep the player's eyes closed for a manual blink. |
| Hold `G` for one second | Dismiss owned SCP-131 followers within 64 blocks. This control is currently fixed rather than remappable. |

## System reference

### SCP Inventory, Status, and Codex

![SCP Inventory](https://cdn.modrinth.com/data/cached_images/5111fa210e13cf68f8c6ba5872024d74b67c3eda.jpeg)

The SCP Inventory is server-authoritative and stored as a player capability. Its default limits are 12 main-item slots, 12 key slots, and 999 units of currency; the main-item limit can be changed per player from 1 to 128 with commands.

Items are routed by category:

- general items, consumables, usable items, and harmful items;
- keycards, ammunition, currency, and Codex documents;
- head, chest, leg, foot, accessory, and weapon equipment.

Explicit rules in `scpinventory.json` take priority. Without a rule, edible/drinkable items are classified as consumables, vanilla armor uses its normal equipment slot, and remaining items are miscellaneous. `USABLE` items temporarily enter a controlled vanilla hotbar session so their normal right-click behavior can run, then return to SCP storage without leaving a duplicate behind.

The **Status** panel displays active conditions except effects hidden by config. The **Codex** lists configured document items and can render a packaged page texture, a UTF-8 text resource, or both.

### Contextual interactions

![Contextual interaction prompt](https://cdn.modrinth.com/data/cached_images/dd77041ddbffbedb7abf6849b5e975136277f9f8.jpeg)

Context rules attach an SCP Unity-style prompt to a block or entity. Each rule defines its reach, text, icon, anchor position, accepted input, and the face used for the simulated interaction. Candidate prompts are scored by screen position, distance, direct targeting, and priority; a larger `priority` value gives a rule more preference.

The interaction itself is validated again on the server before the block or entity receives it. Dropped items use a separate pickup prompt.

Pressing `K` is the recommended editing workflow:

- look at a block or entity to edit its prompt visually;
- hover an item in the SCP or vanilla inventory to edit its category and `NO_STAMINA` behavior;
- save from the editor to write the JSON and refresh the relevant runtime registry.

The block editor also supports precise anchor movement with the arrow keys, `Page Up`/`Page Down`, and the mouse wheel. `Shift` changes the step to `0.10`, `Ctrl` changes it to `0.01`, and the normal step is `0.05`.

### Health, stamina, movement, and blink

The custom HUD can replace vanilla hearts and add SCP Unity-style health, stamina, and blink displays. Gameplay and rendering are separate module switches: the HUD can be hidden without necessarily disabling stamina rules.

The survival movement module slows ordinary walking and makes sprinting a committed, stamina-limited action. Stamina has drain, regeneration delay, and an exhausted sprint lock. Items marked with `NO_STAMINA` block sprinting while held, worn, or equipped in the SCP Inventory.

Blinking is synchronized with the server. It supports automatic blinks, manual hold-to-blink, blackout and vignette effects, and the **Eye Sore** effect, which drains the blink meter faster.

### SCP-173

![SCP-173](https://cdn.modrinth.com/data/cached_images/69aab10949892ad489c5499ab17525c8b2c6ad68.jpeg)

SCP-173 uses server-owned movement and attacks, with a short-lived client camera signal used only as an additional observation safety lock. Observation samples the center, faces, and corners of its hitbox and treats the complete forward half of the camera as visible, so SCP-173 is frozen even near the edge of the screen.

Its line-of-sight logic distinguishes real obstructions from transparent geometry:

- solid collision or visual shapes block observation;
- glass, panes, iron bars, and leaves remain transparent;
- animated facility doors use their actual visual state rather than a generic full block;
- the windows in Normal and Office doors can be used to observe SCP-173;
- the half of a door animation nearest the open state is treated as visually open.

When no valid observer remains, SCP-173 uses a direct snap step, path direction fallback, and side-step fallback. One automatic blink has a hard six-block travel budget; holding a manual blink keeps the eyes closed and therefore allows continued movement. A neck snap is possible only when the real server hitboxes overlap and no observation lock remains.

Creative and Spectator players neither activate nor become targets for SCP-173. Entity IDs and tags added to `scp_173_targets` can both attract it and hold it still while they have sight of it.

#### Natural spawn cycle

When natural spawning is enabled:

1. An eligible Survival player runs a check every 6,000 ticks (five minutes).
2. The check is skipped if any living SCP-173 already exists in any loaded server dimension.
3. The remaining check has a one-in-three chance to continue.
4. Placement is attempted 10–30 blocks in front of the player, with fallback attempts around the player, on a solid floor with enough collision-free space.
5. A routine-spawned SCP-173 begins inert and cannot select a target until a Survival player actually sees it.
6. If it is no longer observed, has no nearby Survival player, and remains more than 20 blocks away for 20 seconds, that routine spawn despawns.

Manually spawned SCP-173 entities are not marked as routine spawns and are not governed by that natural-spawn cleanup cycle.

### SCP-131-A and SCP-131-B

![SCP-131-A and SCP-131-B](https://cdn.modrinth.com/data/cached_images/1728c75e5be1fb6c08d6856afea58db71c01aad9.jpeg)

Right-clicking an SCP-131 starts nearby SCP-131 entities following the same player. Ownership persists through save/reload, distant followers can return to their owner, and holding `G` dismisses the player's group. Idle variants can also follow one another.

SCP-131 scans for nearby SCP-173 entities, moves to a useful viewing position, and acts as a configured observer while it has line of sight.

### Facility doors, buttons, and keycard readers

The facility set includes animated Default, Yellow, Black, Normal, Logistics, Office, Bathroom, and Workshop door families. Door frames are real saved block states with their own timing, collision, sounds, and passability. Heavy Default/Yellow/Black doors are controlled through redstone-facing facility systems; smaller door families support direct use.

Button panels can synchronize with an opposite panel already present on the other side of a wall. Intermediate animation states remain registered for saved-world compatibility, but only public endpoints appear in the creative inventory.

There is one public **Keycard Reader** item. Placement uses the clicked wall face and the clicked half to select the visible left/right model. It starts at Level 1; crouch and interact with the reader while holding a **Screwdriver** in either hand to select Levels 1–6. A higher-level keycard satisfies its own level and every lower level, and readers search both vanilla storage and the SCP Inventory.

### Tesla Gates and terminal

Tesla Gates detect nearby entities and transition into a lethal discharge. Standard mode uses a two-block detection radius and a five-tick warning delay. **Emergency Override** expands the radius to 3.5 blocks, reduces the delay to one tick, and uses stronger audio/particles.

The Tesla Gate Terminal requires **Security Credentials** and controls the system through gamerules, so its state is global for the world. Existing gates are periodically resynchronized when players are nearby; old gates without a scheduled update and gates saved in an intermediate shock frame are recovered automatically.

### SCP-079 facility control

SCP-079 control is disabled by default. When enabled, it may close an open heavy door ahead of a fleeing player or open a closed door between a pursuing mob and its target. It can manipulate only the Default, Yellow, and Black heavy-door families, and only when the door has a connected functional button, keycard reader, or Facility Pulse Node. Bare doors and unrelated redstone do not grant SCP-079 control.

The system evaluates opportunities every 5–10 seconds, waits 8–15 seconds after a successful global action, and gives each affected door a 30-second cooldown. Its configured action chances are 20% to close a door during a valid chase, 30% to open one for a pursuer, and 3% for an unprovoked close while the player is moving toward an open door.

### Decontamination Checkpoint

The checkpoint detects players only inside its modeled chamber. It closes once per visit, removes all active status effects, plays a five-second decontamination cycle, fills the chamber with gas, and reopens. The reopening uses a saved block tick, so stopping the server or leaving the world while it is closed does not leave it locked permanently.

With `deconCheckpoint` enabled, processing also sets the player's respawn position.

### SCP-294

SCP-294 matches typed requests against configurable drink IDs and aliases. Matching is case- and punctuation-insensitive: exact aliases are tested first, optional partial matches follow, and remaining candidates use fuzzy similarity against `fuzzy_threshold`.

A drink definition controls the output item, delay, sound, coin use, cup color, whether the result can be drunk, direct potion effects, and action lists. `dispense_actions` run when the request is produced; `drink_actions` run when the configured result is consumed. Supported actions are documented in the [SCP-294 configuration](#scp-294-drinks) section.

### SCP-914

SCP-914 scans a configurable intake area relative to its winding key. Dropped items and non-player entities use the JSON recipe engine. The first complete recipe for the selected setting is chosen, its inputs are reserved, and outputs are placed at the configured output position after the start delay.

Players are deliberately handled outside the JSON recipes:

- **Rough** and **Coarse** apply destructive outcomes;
- **1:1** selects a random configured player skin and grants the Metamorphosis advancement;
- **Fine** gives a temporary speed/jump enhancement followed by a lethal collapse;
- **Very Fine** gives stronger temporary enhancements followed by a delayed lethal outcome.

The **SCP-914 Assembly Kit** places the complete machine structure and refuses placement when the required volume is blocked.

### Other anomalous content

- **SCP-330:** four candy types with distinct effects and the two-candy limit.
- **SCP-426:** a toaster whose interactions alter how it is described to the player.
- **SCP-572:** a sword that grants confidence far beyond its actual combat quality.
- **SCP-902:** a closed box with proximity and interaction behavior.
- **SCP-1176:** the Mellified Man, special honey, and outcomes linked to the player's blood type.
- **Blood type system:** assigns and displays a player blood type used by SCP-1176-related mechanics.

### Building content

The SCP Unity-inspired creative inventory contains Sector 1 and Sector 2 floors and walls, directional floor markings, wall details, ventilation pieces, containment and office structures, lights, heaters, sign supports, televisions, trash bins, terminals, doors, buttons, and other facility props. Supported lights use optional MoreMcmeta emissive overlays.

## Configuration

Configuration files are created under the instance's `config` directory. Bundled defaults are copied only when a file does not already exist; updates do not replace an existing customized file. Back up a file before deleting it to regenerate the defaults shipped by the installed version.

The repository contains the complete defaults:

| File | Purpose | Applying changes |
| --- | --- | --- |
| [`config/scpadditions/modules.json`](config/scpadditions/modules.json) | Enables or disables gameplay modules. | Restart the game/server. |
| [`config/scpinventory/scpinventory.json`](config/scpinventory/scpinventory.json) | Item routing, stamina blockers, Status filters, Codex entries, and SCP-173 entity targets. | The `K` item editor saves and reloads its changes immediately; restart after manual edits for a reliable full reload. |
| [`config/scpinventory/context_interactions.json`](config/scpinventory/context_interactions.json) | Block/entity prompt rules. | The editor applies saves immediately; after manual edits use `/scpinventory context reload`. |
| [`config/scpadditions/294drinks.json`](config/scpadditions/294drinks.json) | SCP-294 matching and drink definitions. | Restart the game/server. |
| [`config/scpadditions/914recipes.json`](config/scpadditions/914recipes.json) | SCP-914 machine geometry/timing and main recipe list. | Restart the game/server. |
| [`config/scpadditions/914recipes.d/*.json`](config/scpadditions/914recipes.d/entities.json) | Additional SCP-914 recipe fragments. | Restart the game/server. |
| `config/scpadditions/scp914_skins/*.png` | SCP-914 1:1 player-skin pool. | The directory is scanned when 1:1 chooses a skin. |

All time values named `*_ticks` and effect durations are Minecraft ticks; 20 ticks equal one second.

### Module switches

`modules.json` contains independent switches:

```json
{
  "inventory": { "enabled": true },
  "interactions": {
    "enabled": true,
    "disable_in_creative": false
  },
  "hud": { "enabled": true },
  "vitals": {
    "custom_health_enabled": true,
    "stamina_enabled": true,
    "horror_movement_enabled": true
  },
  "blink": { "enabled": true },
  "scp_173": {
    "natural_spawn_enabled": true,
    "enabled": true
  }
}
```

`hud.enabled` controls custom vitals rendering. It does not by itself disable stamina gameplay. `interactions.disable_in_creative` hides and disables contextual interaction prompts for Creative players when set to `true`; they remain available by default. Spectator players never receive these interactions. `scp_173.enabled` disables SCP-173 behavior as a whole, while `natural_spawn_enabled` keeps the entity functional but stops the routine spawn cycle.

### Inventory, item, Status, Codex, and SCP-173 rules

The main sections in `scpinventory.json` are:

| Section | Meaning |
| --- | --- |
| `item_rules` | Maps an item registry ID to an SCP Inventory category. |
| `item_effects` | Applies special inventory-aware behavior; currently `NO_STAMINA`. |
| `hidden_status_effects` | Hides matching effects from the Status panel without removing the gameplay effect. |
| `codex_documents` | Defines item-backed documents and their page resources. |
| `scp_173_targets` | Adds non-player entity IDs or `#namespace:tag` entries as SCP-173 targets and observers. |

Canonical item types are `MISCELLANEOUS`, `HARMFUL`, `CONSUMABLE`, `USABLE`, `KEY`, `CODEX`, `COIN`, `AMMO`, `HEAD`, `ACCESSORY`, `ACCESSORY_HAND`, `CHEST`, `LEGS`, `FEET`, and `WEAPON`.

Example:

```json
{
  "item_rules": [
    { "id": "example:facility_key", "type": "KEY" },
    { "id": "example:radio", "type": "USABLE" },
    { "id": "example:helmet", "type": "HEAD" }
  ],
  "item_effects": [
    { "id": "example:heavy_armor", "effects": ["NO_STAMINA"] }
  ],
  "hidden_status_effects": [
    "minecraft:bad_omen"
  ],
  "codex_documents": [
    {
      "id": "example:incident_report",
      "category": "Incident Reports",
      "name": "Incident 173-A",
      "image": "example:textures/gui/codex/incident_173_a.png",
      "text": "example:texts/codex/incident_173_a.txt",
      "image_width": 1279,
      "image_height": 1920
    }
  ],
  "scp_173_targets": [
    "minecraft:villager",
    "#minecraft:raiders"
  ]
}
```

Codex `image` and `text` values are resource locations supplied by a mod or resource pack, not ordinary paths beside the config file. Optional `creator`, `timestamp`, `uuid`, `nbt_key`, and `nbt_value` fields restrict a definition to documents with matching NBT values. `image_width` and `image_height` define the source aspect ratio; their defaults are 1279×1920.

### Context interaction rules

The safest way to create a rule is to press `K` while looking at its target and save through the visual editor. A manual block rule has this structure:

```json
{
  "interactions": [
    {
      "type": "block",
      "id": "minecraft:oak_door",
      "range": 2.8,
      "priority": 30,
      "icon": "hand",
      "useItem": "hand",
      "text": {
        "action": "Open",
        "nameMode": "manual",
        "name": "Door",
        "showAction": true,
        "showName": true
      },
      "anchor": {
        "position": [0.5, 0.5, 0.05],
        "worldOffset": [0.0, 0.0, 0.0],
        "rotateWith": "auto"
      },
      "click": { "face": "front" },
      "input": {
        "allowE": true,
        "allowRightClick": true
      }
    }
  ]
}
```

Important fields:

- `type`: `block` or `entity`.
- `id`: target registry ID.
- `range`: prompt reach in blocks, with a minimum effective value of `0.25`.
- `priority`: preference when multiple valid prompts compete; larger values are favored.
- `icon`: `hand`, `pickup`, or `default` for the standard hand icon; `card` for the keycard icon; or a texture resource location such as `example:textures/gui/custom_icon.png`.
- `useItem`: prompt item mode, `hand` or `card`; it also supplies the default icon when `icon` is omitted.
- `text.nameMode`: `manual` uses `text.name`; `auto` uses the block/item/entity display name.
- `anchor.position`: local block coordinates, normally from `0.0` to `1.0`. `[0.5, 0.5, 0.05]` is centered near the unrotated front face.
- `anchor.worldOffset`: optional final world-space offset, mainly useful for manual rules.
- `anchor.rotateWith`: `auto`, `facing`, `horizontal_facing`, `axis`, or `none`.
- `click.face`: `front`, `back`, `player`, `north`, `south`, `east`, `west`, `up`, or `down`.
- `input`: independently enables the context key and right-click.

### SCP-294 drinks

The full shipped library is in [`294drinks.json`](config/scpadditions/294drinks.json). A compact definition looks like this:

```json
{
  "version": 2,
  "matching": {
    "allow_partial": true,
    "fuzzy_threshold": 0.66
  },
  "drinks": [
    {
      "id": "example:strong_coffee",
      "enabled": true,
      "aliases": ["strong coffee", "double coffee"],
      "result": { "item": "scp_additions:cup_of_coffee", "count": 1 },
      "delay_ticks": 40,
      "sound": "scp_additions:scp294pouring",
      "consumes_coin": true,
      "give_result": true,
      "drinkable": true,
      "refuse_message": "I shouldn't drink that.",
      "cup_color": "#2B1608",
      "actionbar": "The coffee is unusually strong.",
      "effects": [
        {
          "id": "minecraft:speed",
          "duration": 200,
          "amplifier": 0,
          "ambient": false,
          "visible": true,
          "show_icon": true
        }
      ],
      "drink_actions": [
        { "type": "remove_effect", "effect": "minecraft:hunger" }
      ],
      "dispense_actions": []
    }
  ]
}
```

Each drink automatically accepts its ID path and the same path with underscores replaced by spaces, in addition to `aliases`. `fuzzy_threshold` is clamped from `0.0` to `1.0`; a larger value demands a closer match.

Supported action types:

| Type | Main fields | Result |
| --- | --- | --- |
| `actionbar` | `message` | Shows text above the hotbar. |
| `message` | `message` | Sends a normal player message. |
| `sound` | `sound` | Plays a registered sound at the machine/action position. |
| `particle` | `particle`, `count`, `radius` | Emits `smoke`, `flash`, `flame`, `happy`, `splash`, `cloud`, or the default explosion particle. |
| `visual_explosion` | `sound`, `count`, `radius` | Plays an explosion presentation without world damage. |
| `effect` | `effect`, `duration`, `amplifier`, `ambient`, `visible`, `show_icon` | Adds a status effect. |
| `remove_effect` | `effect` | Removes a status effect. |
| `heal` | `amount` | Restores health. |
| `hurt` | `amount` | Deals generic damage. |
| `kill` | — | Applies SCP-294 lethal damage. |
| `set_fire` | `seconds` | Sets the consumer on fire. |

Every action can use `delay_ticks`. The older `actions` array is accepted as an additional alias for `drink_actions`. Entity spawning is intentionally not a supported SCP-294 output.

### SCP-914 recipes

The main file owns both the `machine` section and its recipes. Fragment files in `914recipes.d` add only recipes and are loaded afterward in filename order.

```json
{
  "version": 2,
  "machine": {
    "intake_offset": [-5, 0, -3],
    "output_offset": [5, 0, -3],
    "search_radius": 1.5,
    "start_delay_ticks": 30,
    "finish_delay_ticks": 160
  },
  "recipes": [
    {
      "id": "example:keycard_refinement",
      "enabled": true,
      "setting": "fine",
      "item_inputs": [
        { "item": "scp_additions:level_2_keycard", "count": 1 }
      ],
      "weighted_item_outputs": [
        { "weight": 3, "item": "scp_additions:level_3_keycard", "count": 1 },
        { "weight": 1, "item": "scp_additions:playing_card", "count": 1 }
      ],
      "chance": 1.0,
      "copy_input_nbt": false
    }
  ]
}
```

Machine fields:

- `intake_offset` and `output_offset` are exactly three integers: local lateral, vertical, and forward offsets from the winding key, rotated with the machine facing.
- `search_radius` controls the half-size of the intake search area and has a minimum of `0.5`.
- `start_delay_ticks` delays the transformation after the cycle begins.
- `finish_delay_ticks` controls the remaining refining time after transformation.

Recipe fields:

- `setting`: `rough`, `coarse`, `1_to_1`, `fine`, or `very_fine`.
- `item_inputs`: item IDs and counts. `input` is accepted as a single-input form.
- `entity_inputs`: entity IDs, counts, and optional `consume` booleans.
- `item_outputs`: all listed item outputs. `output` is accepted as a single-output form.
- `weighted_item_outputs`: selects exactly one listed output by relative integer weight. When present, it takes precedence over `item_outputs`.
- `entity_outputs`: entity IDs and counts spawned at the output.
- `chance`: clamped from `0.0` to `1.0`. Inputs are consumed even when this roll fails.
- `copy_input_nbt`: copies the first item input's NBT to generated item outputs.

Only the first complete recipe matching the selected setting is processed per cycle. Fragments append recipes; repeating an ID in a fragment does not override an earlier recipe. To replace an existing rule, edit/disable the earlier entry or ensure the desired recipe is encountered first.

### SCP-914 1:1 skins

On first launch, the mod creates `config/scpadditions/scp914_skins`, copies eleven bundled skins, and writes its own `README.txt`. Add any 64×64 or legacy 64×32 Minecraft skin PNG to the directory to include it in the random pool.

The server stores and synchronizes only the selected filename, not the PNG bytes. For multiplayer rendering, every client must have a PNG with the same filename in its own `scp914_skins` directory and must install Kleiders Custom Renderer API.

## Gamerules

| Gamerule | Default | Behavior |
| --- | --- | --- |
| `teslaGateOn` | `true` | Global Tesla Gate operating state. |
| `teslaGateManualOverride` | `false` | Enables Emergency Override. Turning it on also forces `teslaGateOn` on. |
| `scp079controlOn` | `false` | Allows SCP-079 to manipulate eligible connected heavy doors. |
| `deconCheckpoint` | `false` | Makes a Decontamination Checkpoint save a processed player's respawn position. |

Examples:

```mcfunction
/gamerule teslaGateOn false
/gamerule teslaGateManualOverride true
/gamerule scp079controlOn true
/gamerule deconCheckpoint true
```

## Commands

### Inventory administration

The inventory maintenance branch is registered with Minecraft permission level 2.

| Command | Effect |
| --- | --- |
| `/scpinventory reset` | Reset your complete SCP Inventory, equipment mirrors, and main capacity back to 12. |
| `/scpinventory reset <targets>` | Reset the selected players. |
| `/scpinventory clear` | Clear only your main SCP item storage. |
| `/scpinventory clear <targets>` | Clear only the selected players' main item storage. |
| `/scpinventory clearmain [targets]` | Alias of `clear`. |
| `/scpinventory setmax <slots>` | Set your main capacity from 1 to 128. |
| `/scpinventory setmax <targets> <slots>` | Set the selected players' main capacity. |
| `/scpinventory maxslots ...` | Alias of `setmax`. |
| `/scpinventory getmax` | Report your used/main capacity and key count. |

### Context interaction editor

These commands operate on a player editing session. For normal use, `K` and the visual GUI are faster.

| Command | Effect |
| --- | --- |
| `/scpinventory context gui` | Open the editor for the viewed entity or block, preferring an entity hit. |
| `/scpinventory context select` | Select the viewed block up to six blocks away for command-based editing. |
| `/scpinventory context add` | Create the default rule after selecting an unconfigured block. |
| `/scpinventory context cancel` | Cancel the pending selection/session. |
| `/scpinventory context done` | Finish the current editing session. |
| `/scpinventory context reload` | Reload `context_interactions.json` immediately. |
| `/scpinventory context marker` | Show the current anchor marker. |
| `/scpinventory context set action <text>` | Set the action label. |
| `/scpinventory context set name <text>` | Set the manual display name. |
| `/scpinventory context set range <0.25..64>` | Set prompt reach. |
| `/scpinventory context input <mode>` | Choose `both`, `e`, or `right_click`. `key`, `rightclick`, and `mouse` are also accepted aliases. |
| `/scpinventory context item <mode>` | Select `hand` or `card` as the prompt item mode. |
| `/scpinventory context clickface <face>` | Set `front`, `back`, `player`, a cardinal direction, `up`, or `down`. |
| `/scpinventory context rotate <mode>` | Set `auto`, `facing`, `horizontal_facing`, `axis`, or `none`. |
| `/scpinventory context anchor hit` | Move the anchor to the selected hit point. |
| `/scpinventory context anchor here <0.25..16>` | Place the anchor the given distance in front of the player. |
| `/scpinventory context anchor nudge <x> <y> <z>` | Move the anchor by local offsets from `-16` to `16`. |

## Compatibility and updates

- Existing published `scp_additions` registry IDs are retained where required for older worlds.
- Intermediate door and machinery animation states remain registered so saved worlds can resolve them, but they are hidden from creative tabs.
- Some bundled models, textures, fonts, and sounds use internal resource namespaces. Resource-pack authors should preserve the exact resource locations referenced by models and configuration entries.
- Existing configuration files are never automatically replaced with a newer shipped default.

Back up important worlds and configuration directories before changing versions.

## Building from source

The project uses Java 17 and the included Gradle wrapper:

```bash
./gradlew clean build
```

The compiled JAR is written to `build/libs`.

## Acknowledgements and license

[SCP Unity](https://www.scp-unity.com/) left a lasting impression through its atmosphere, interface, and facility design, and is the main visual and mechanical inspiration for this project.

Special thanks to [SCP: Overtime](https://modrinth.com/mod/scp-overtime) for inspiring the creation of SCP Additions. Various facility textures were also adapted from or inspired by SCP Unity-related blocks in its 1.16.5 version.

Content relating to the SCP Foundation, including the SCP Foundation logo and SCP concepts, originates from the [SCP Wiki](https://scp-wiki.wikidot.com/) and its respective authors and is licensed under [Creative Commons Attribution-ShareAlike 3.0](https://creativecommons.org/licenses/by-sa/3.0/).

SCP Additions is also released under Creative Commons Attribution-ShareAlike 3.0. It is not an official Minecraft product and is not affiliated with Mojang, Microsoft, or the SCP Wiki staff.
