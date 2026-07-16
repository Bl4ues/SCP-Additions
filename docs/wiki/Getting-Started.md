# Getting Started

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
