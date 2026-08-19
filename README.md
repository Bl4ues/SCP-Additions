# SCP Additions

![SCP Additions banner](https://cdn.modrinth.com/data/cached_images/b9d834bc5afc41d56f44146c8c8521d6170d723c_0.webp)

SCP Additions is an SCP survival-horror and facility-building mod for Minecraft 1.20.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.

## Development status

SCP Additions is currently developed and released for **Minecraft 1.20.1 with Forge**.

| Minecraft | Loader | Status | Java | Branch |
|---|---|---|---|---|
| 1.20.1 | Forge 47.4.10+ | Stable public edition | 17 | [`master`](../../tree/master) |

## Links

- [Download source snapshot](https://api.github.com/repos/Bl4ues/SCP-Additions/zipball/master)
- [Download on Modrinth](https://modrinth.com/mod/scp-additions)
- [Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/scp-additions)
- [Documentation Wiki](https://github.com/Bl4ues/SCP-Additions/wiki)
- [Wiki source mirror](docs/wiki/Home.md)
- [Changelog](CHANGELOG.md)
- [Issue tracker](https://github.com/Bl4ues/SCP-Additions/issues)
- [Roadmap](https://trello.com/b/UFdqpaC8/scp-additions-roadmap)

## Requirements

- Minecraft **1.20.1**
- Forge **47.4.10 or newer**
- GeckoLib **4.4.9 or newer**

Optional client integrations:

- [Kleiders Custom Renderer API](https://modrinth.com/mod/kleiders-custom-renderer-api) renders custom SCP-914 **1:1** player skins. All other SCP-914 behavior works without it.
- Shader packs with LabPBR support can add material-aware emission and bloom to supported block textures. Built-in emissive block overlays remain full-bright without shaders or additional emissive-texture mods.

## Main features

- Functional SCPs including SCP-012, SCP-079, SCP-106, SCP-131, SCP-173, SCP-294, SCP-714, SCP-914, and more.
- SCP Unity-inspired inventory, Status, Crafting, Codex, health, stamina, movement, blink, and interaction systems.
- Animated facility doors, keycard readers, Tesla Gates, decontamination machinery, terminals, lights, props, and modular building blocks.
- Server-authoritative configuration center for modules, inventory rules, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes.
- Broad vanilla and modded integration through registry-based configuration, automatic item classification, and SCP-914 recipe inference.
- Multiplayer synchronization for host-controlled gameplay configuration and custom inventory behavior.

## Installation

1. Install Minecraft **1.20.1** with Forge **47.4.10 or newer**.
2. Install GeckoLib **4.4.9 or newer**.
3. Place the SCP Additions JAR in the instance's `mods` folder.
4. Install optional integrations only when compatible versions are available for Minecraft 1.20.1 Forge.

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

Clone the repository or open the `master` branch, then use the included Gradle wrapper with Java 17:

```bash
./gradlew clean build
```

The compiled JAR is written to `build/libs`.

Development runs are available through ForgeGradle:

```bash
./gradlew runClient
./gradlew runServer
```
