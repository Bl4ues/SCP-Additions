# SCP Additions

![SCP Additions banner](https://cdn.modrinth.com/data/cached_images/b9d834bc5afc41d56f44146c8c8521d6170d723c_0.webp)

SCP Additions is an SCP survival-horror and facility-building mod for Minecraft 1.20.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.

## Development status

SCP Additions is currently developed and released for **Minecraft 1.20.1 with Forge**.

| Minecraft | Loader | Status | Java | Branch |
|---|---|---|---|---|
| 1.20.1 | Forge 47.4.10+ | Stable public edition | 17 | [`master`](../../tree/master) |

## Links

- [Download on Modrinth](https://modrinth.com/mod/scp-additions)
- [Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/scp-additions)
- [Documentation Wiki](https://github.com/Bl4ues/SCP-Additions/wiki)
- [Wiki source mirror](docs/wiki/Home.md)
- [Changelog](CHANGELOG.md)
- [Issue tracker](https://github.com/Bl4ues/SCP-Additions/issues)

## Requirements

- Minecraft **1.20.1**
- Forge **47.4.10 or newer**
- GeckoLib **4.4.9 or newer**

Optional client integrations:

- [Kleiders Custom Renderer API](https://modrinth.com/mod/kleiders-custom-renderer-api) renders custom SCP-914 **1:1** player skins. All other SCP-914 behavior works without it.
- [MoreMcmeta Emissive Textures](https://modrinth.com/mod/moremcmeta-emissive) enables supported emissive facility textures. Its MoreMcmeta base dependency is also required.

## Main features

- Functional SCPs including SCP-012, SCP-079, SCP-131, SCP-173, SCP-294, SCP-714, SCP-914, and more.
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

## License and attribution

SCP Additions is released under the [Creative Commons Attribution-ShareAlike 3.0 license](LICENSE.md).

Content relating to the SCP Foundation, including the SCP Foundation logo and SCP concepts, originates from the [SCP Wiki](https://scp-wiki.wikidot.com/) and its respective authors and is available under Creative Commons Attribution-ShareAlike 3.0.

SCP Unity is the main visual and mechanical inspiration for the project. Special thanks to [SCP: Overtime](https://modrinth.com/mod/scp-overtime) for inspiring the original creation of SCP Additions. Various facility assets were adapted from or inspired by SCP Unity-related blocks in its 1.16.5 version.

### Bundled fonts

The following font files are redistributed with SCP Additions and remain under their respective licenses; they are **not** relicensed under the project's Creative Commons Attribution-ShareAlike 3.0 license.

| Font | Use in the mod | Copyright and design credit | License |
|---|---|---|---|
| Roboto Regular 2.001047 | SCP Inventory interface text | Copyright 2015 Google Inc.; designed by Christian Robertson | [Apache License 2.0](https://github.com/googlefonts/roboto-2/blob/main/LICENSE) |
| Montserrat Regular 7.200 | SCP Inventory interface headings and labels | Copyright 2011 The Montserrat Project Authors; originally designed by Julieta Ulanovsky | [SIL Open Font License 1.1](https://github.com/JulietaUla/Montserrat/blob/master/OFL.txt) |
| Liberation Sans Bold 1.04 | Core Room Sign text | Digitized data © 2007 Ascender Corporation; designed by Steve Matteson; Liberation is a trademark of Red Hat, Inc. | [GNU GPL v2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html) with the [Liberation Fonts exceptions](https://github.com/liberationfonts/liberation-sans-narrow/blob/master/License.txt) |
| Anonymous Pro Regular 1.003 | Door Sign text | Copyright © 2009 Mark Simonson; Reserved Font Name: Anonymous Pro | [SIL Open Font License 1.1](https://www.marksimonson.com/fonts/view/anonymous/) |
| Jura Variable 5.106 | Door Sign numbers | Copyright 2019 The Jura Project Authors; designed by Daniel Johnson, Alexei Vanyashin, and Mirko Velimirovic | [SIL Open Font License 1.1](https://github.com/ossobuffo/jura/blob/master/OFL.txt) |

Copyright notices and license metadata embedded in the font files have been preserved. Distributions and forks that include these fonts must retain the applicable notices and license terms. Modified versions may also be subject to reserved-font-name and source-distribution requirements stated in their respective licenses.

SCP Additions is not an official Minecraft product and is not affiliated with Mojang, Microsoft, the SCP Wiki staff, or Aerie Gaming Studios.
