# SCP Additions

SCP Additions is a Forge mod for Minecraft 1.20.1 that expands SCP-themed gameplay with containment machinery, facility security systems, anomalous objects, custom survival mechanics, and SCP Unity-inspired construction content.

Version 3.0 consolidates the former SCP Inventory and SCP Unity Extra Blocks projects directly into SCP Additions. Only the SCP Additions JAR is required; the two standalone project JARs should not be installed alongside it.

## Requirements

### Required

- Minecraft 1.20.1
- Forge 47.4.10 or newer
- GeckoLib 4.4.9 or newer

### Optional client-side enhancements

- **Kleiders Custom Renderer**: renders the custom player skins selected by SCP-914 on the 1:1 setting. All other SCP-914 player effects work without it.
- **MoreMcmeta Emissive Textures**: enables emissive overlays for supported facility lights and glowing block textures. Install the MoreMcmeta base dependency required by the plugin as well.

## Major features

### SCP Inventory and survival systems

- Custom SCP-styled inventory with dedicated general, equipment, keycard, ammunition, weapon, document, and currency storage.
- Configurable item classification, pickup routing, context menus, item actions, equipment handling, and usable-item sessions.
- Codex and Status panels.
- Custom pickup prompts, interaction prompts, inventory-full feedback, and localized Roboto interface rendering.
- Custom health and stamina HUDs.
- Horror-style movement, sprint stamina, regeneration delay, exhausted sprint lock, and configurable stamina-blocking items.
- Server-authoritative synchronization, persistence, death handling, and duplication safeguards.

### Blink system and SCP entities

- Automatic and manual hold-to-blink behavior with synchronized eye state.
- Blink meter, vignette, blackout, post-blink cover, and Eye Sore drain acceleration.
- SCP-173 with GeckoLib rendering, observation checks, deterministic snap movement, path fallback, neck-snap damage, configurable natural spawning, durability rules, and a complete sound set.
- SCP-131-A and SCP-131-B with animated models, glowing eyes, following behavior, persistent ownership, custom notices, and SCP-173 observation behavior.

### Facility systems

- Tesla Gates with global terminal controls and Emergency Override behavior.
- SCP Unity-inspired Tesla Gate Terminal interface with Security Credentials and custom sounds.
- Six keycard clearance levels and configurable keycard readers.
- Screwdriver-based reader configuration through direct or contextual interaction.
- Decontamination Checkpoints.
- SCP-079 facility controls.
- SCP Unity-inspired architectural blocks, lights, props, doors, buttons, and facility sounds.
- Animated Default, Yellow, Black, Normal, Logistics, Office, Bathroom, and Workshop doors.
- Sector 1 and Sector 2 floors, walls, directional arrows, wall details, and ventilation pieces.

### SCP machinery and anomalous objects

- **SCP-294**: configurable drink machine with dynamic cups, fuzzy matching, aliases, effects, actions, sounds, custom death messages, and data-driven drink definitions.
- **SCP-914**: configurable refinement machine with item and entity recipes, weighted outputs, recipe fragments, draggable dial GUI, machine offsets, and an assembly kit for placing the complete structure. Players inside the intake receive setting-specific Rough, Coarse, 1:1, Fine, and Very Fine outcomes independently of the JSON recipe list.
- **Additional SCP content**: SCP-330, SCP-426, SCP-572, SCP-902, SCP-1176, and related items and mechanics.

## Configuration

Major systems are configurable without rebuilding the mod:

- `config/scpadditions/modules.json`
- `config/scpadditions/294drinks.json`
- `config/scpadditions/914recipes.json`
- `config/scpadditions/914recipes.d/*.json`
- `config/scpinventory/scpinventory.json`
- `config/scpinventory/context_interactions.json`
- `config/scpadditions/scp914_skins/*.png`

Bundled defaults are copied only when a configuration file does not already exist. Existing local configurations are not overwritten automatically.

### SCP-914 1:1 skin pool

On first launch, the mod creates `config/scpadditions/scp914_skins` and copies the eleven legacy SCP-914 skins into it. Add any compatible 64x64 or legacy 64x32 Minecraft skin PNG to this directory to include it in the random 1:1 pool.

The selected filename is stored and synchronized by the server. For multiplayer rendering, each client must have a PNG with the same filename in its own `scp914_skins` directory and must install the optional Kleiders Custom Renderer.

To regenerate the newest defaults, back up and remove the relevant generated configuration files before starting the game again.

## Compatibility

- Existing published SCP Additions registry IDs are preserved for older worlds.
- Migrated SCP Unity Extra Blocks and SCP UBlocks IDs are remapped to their SCP Additions equivalents when possible.
- Intermediate animated door states remain registered for saved-world compatibility but are hidden from the creative inventory.
- SCP Inventory and facility resource namespaces remain bundled internally where required by models, textures, animations, fonts, and sounds.

Always back up important worlds before updating between major versions.

## Building from source

The project uses Java 17 and the included Gradle wrapper:

```bash
./gradlew clean build
```

The compiled JAR is generated under `build/libs`.

## License and SCP attribution

Content relating to the SCP Foundation, including SCP concepts, is licensed under Creative Commons Attribution-ShareAlike 3.0 and originates from the SCP Wiki and its authors.

SCP Additions is also released under Creative Commons Attribution-ShareAlike 3.0.

This is not an official Minecraft product and is not affiliated with Mojang, Microsoft, or the SCP Wiki staff.
