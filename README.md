# SCP Additions

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
