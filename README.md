# SCP Additions

![SCP Additions banner](https://cdn.modrinth.com/data/cached_images/b9d834bc5afc41d56f44146c8c8521d6170d723c_0.webp)

SCP Additions is an SCP survival-horror and facility-building mod. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.

The project is expanding beyond its original Forge-only scope. The current public edition remains **Forge for Minecraft 1.20.1**, while native **NeoForge and Fabric ports for Minecraft 1.21.1** are being developed and validated in separate branches.

## Editions and development status

| Minecraft | Loader | Status | Java | Branch |
|---|---|---|---|---|
| 1.20.1 | Forge 47.4.10+ | Stable public edition | 17 | [`master`](../../tree/master) |
| 1.21.1 | NeoForge 21.1.235 | Alpha port in active validation | 21 | [`neoforge-1.21.1-port-3.0.7`](../../tree/neoforge-1.21.1-port-3.0.7) |
| 1.21.1 | Fabric Loader 0.17.3+ | Alpha port in active validation | 21 | [`fabric-1.21.1-port-3.0.7`](../../tree/fabric-1.21.1-port-3.0.7) |

The 1.21.1 branches are intended to reach feature parity with the Forge edition, but they should not be treated as stable releases until their runtime and manual QA are complete. Loader branches remain isolated and are not merged into `master`.

## Links

- [Download on Modrinth](https://modrinth.com/mod/scp-additions)
- [Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/scp-additions)
- [Documentation Wiki](https://github.com/Bl4ues/SCP-Additions/wiki)
- [Wiki source mirror](docs/wiki/Home.md)
- [Changelog](CHANGELOG.md)
- [Issue tracker](https://github.com/Bl4ues/SCP-Additions/issues)

## Requirements

### Forge 1.20.1

- Minecraft **1.20.1**
- Forge **47.4.10** or newer
- GeckoLib **4.4.9** or newer
- Java **17**

### NeoForge 1.21.1 alpha

- Minecraft **1.21.1**
- NeoForge **21.1.235**
- GeckoLib **4.9.2**
- Java **21**

### Fabric 1.21.1 alpha

- Minecraft **1.21.1**
- Fabric Loader **0.17.3** or newer
- Fabric API **0.116.14+1.21.1** or newer compatible version
- GeckoLib **4.9.2**
- Java **21**

Optional client integrations:

- [Kleiders Custom Renderer API](https://modrinth.com/mod/kleiders-custom-renderer-api) renders SCP-914 **1:1** player skins when a compatible edition is available. All other SCP-914 behavior works without it.
- [MoreMcmeta Emissive Textures](https://modrinth.com/mod/moremcmeta-emissive) enables supported emissive facility textures where the loader and Minecraft version are compatible. Its MoreMcmeta base dependency is also required.

## Main features

- Functional SCPs including SCP-012, SCP-079, SCP-131, SCP-173, SCP-294, SCP-914, and more.
- SCP Unity-inspired inventory, Status, Crafting, Codex, health, stamina, movement, blink, and interaction systems.
- Animated facility doors, keycard readers, Tesla Gates, decontamination machinery, terminals, lights, props, and modular building blocks.
- Server-authoritative configuration center for modules, inventory rules, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes.
- Broad vanilla and modded integration through registry-based configuration, automatic item classification, and SCP-914 recipe inference.
- Multiplayer synchronization for host-controlled gameplay configuration and custom inventory behavior.

## Installation

1. Choose the edition matching the exact Minecraft version and loader of the instance.
2. Install the required GeckoLib edition and, for Fabric, Fabric API.
3. Place the matching SCP Additions JAR in the instance's `mods` folder.
4. Install optional integrations only when compatible versions exist for the selected loader.

Do not use a Forge JAR on NeoForge or Fabric, or mix the 1.20.1 and 1.21.1 editions.

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

Clone or open the branch for the intended loader, then use its included Gradle wrapper:

```bash
./gradlew clean build
```

Use Java 17 for `master` and Java 21 for both 1.21.1 branches. The compiled JAR is written to `build/libs`.

Development runs are available through each branch's Gradle configuration:

- Forge and NeoForge: `runClient` and `runServer`.
- Fabric: `runClient` and `runServer` through Fabric Loom.

## License and attribution

SCP Additions is released under the [Creative Commons Attribution-ShareAlike 3.0 license](LICENSE.md).

Content relating to the SCP Foundation, including the SCP Foundation logo and SCP concepts, originates from the [SCP Wiki](https://scp-wiki.wikidot.com/) and its respective authors and is available under Creative Commons Attribution-ShareAlike 3.0.

SCP Unity is the main visual and mechanical inspiration for the project. Special thanks to [SCP: Overtime](https://modrinth.com/mod/scp-overtime) for inspiring the original creation of SCP Additions. Various facility assets were adapted from or inspired by SCP Unity-related blocks in its 1.16.5 version.

SCP Additions is not an official Minecraft product and is not affiliated with Mojang, Microsoft, the SCP Wiki staff, or Aerie Gaming Studios.
