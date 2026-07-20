#!/usr/bin/env python3
from pathlib import Path

path = Path("CHANGELOG.md")
text = path.read_text(encoding="utf-8")
start_marker = "# SCP Additions 3.0.7 — In Development\n"
end_marker = "# SCP Additions 3.0.6 — Hazards & Survival Update\n"

start = text.index(start_marker)
end = text.index(end_marker, start)

section = """# SCP Additions 3.0.7 — In Development

## Platform expansion

- Expanded SCP Additions beyond its original Forge-only scope with native NeoForge and Fabric ports for Minecraft 1.21.1;
- Kept the stable Forge 1.20.1 edition on `master` while developing the NeoForge and Fabric editions in isolated loader branches with Java 21;
- Added loader-specific build and runtime validation for Forge, NeoForge, and Fabric without merging incompatible platform code into the stable branch;
- Added documentation for the supported Minecraft versions, loaders, Java versions, dependencies, development branches, and current release status;
- Marked both 1.21.1 editions as alpha ports pending complete runtime and manual feature-parity validation.

## Multiplayer and configuration synchronization

- Made the host's gameplay configuration authoritative for connected clients, including item rules and effects, hidden Status effects, Codex definitions, contextual interactions, entity interaction rules, and SCP-173 target configuration;
- Added configuration synchronization on login and after supported save, delete, and reload operations;
- Cleared synchronized host snapshots when clients disconnect so single-player and later servers cannot inherit stale settings;
- Synchronized Survival `USABLE` and `WEAPON` tool sessions with the real selected hotbar slot, fixing mining and item-use behavior that could disagree between client and server.

## Gameplay fixes and refinements

- Added a generic configurable SCP-294 cup fallback for drink output IDs that do not have a dedicated item model, preventing missing-texture outputs while preserving drink behavior;
- Corrected SCP-173 observer handling so players retain broad on-screen observation, configured generic mobs must face the statue directly, and SCP-131 uses its own intentional viewing threshold;
- Changed the SCP-572 advancement title to **The Chosen One**;
- Removed the bundled legacy SCP-1499 pig context rule and ignored exact obsolete copies so old defaults do not reappear;
- Standardized Tesla Gate detection and lethal discharge to the same exact 3×3×3 volume centered on the gate;
- Set Tesla Gate damage to 200, with a five-tick normal activation delay and a one-tick manual-override delay.

## Minecraft 1.21.1 port corrections

- Registered custom gamerules before world creation and added defensive Tesla Gate rule access, fixing a NeoForge crash on the first server tick of a new world;
- Prevented missing or rejected advancement data from blocking a player from joining a world;
- Migrated 1.20.1 datapack directories to the singular Minecraft 1.21.1 layout, including advancements, recipes, loot tables, structures, functions, predicates, item modifiers, and registry tags;
- Migrated advancement icons and recipe results to the Minecraft 1.21.1 ItemStack JSON format;
- Updated the bundled resource/data pack metadata for Minecraft 1.21.1 compatibility;
- Added runtime checks that build each port, validate all migrated JSON resources, create a dedicated world, and reject server-tick crashes before a port can be considered ready.

## Internal cleanup

- Consolidated the pre-3.0 SCP-294 drink items into the configurable generic cup, preserving drink IDs, colors, effects, actions, aliases, delays, and messages while removing 61 obsolete `/give` entries;
- Added old-world item remapping and legacy SCP-294 config normalization for the removed drink registry IDs;
- Removed legacy drink classes, item-only procedures, item models, translations, and obsolete crafting/SCP-914 recipes that depended on them;
- Removed unregistered SCP-059, Delta Radiation, and Geiger resource leftovers, unreachable legacy overlays, and Java helpers with no runtime caller or event-bus role;
- Consolidated duplicate registry-facing resources from integrated legacy namespaces, shared SCP-294 geometry, byte-identical custom models, and safely redirectable texture copies;
- Corrected three pre-existing broken resource references uncovered by the audit: the terminal `click_1` sound, the left-button screen texture namespace, and a mirrored closing-button model parent.

"""

path.write_text(text[:start] + section + text[end:], encoding="utf-8")
