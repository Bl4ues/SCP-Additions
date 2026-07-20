# NeoForge 1.21.1 Port — SCP Additions 3.0.7

Branch: `neoforge-1.21.1-port-3.0.7`

## Clean baseline

This port starts from the audited SCP Additions 3.0.7 master cleanup, including the generic SCP-294 cup consolidation, old-world remapping, legacy configuration normalization, and removal of unreachable Java/assets.

## Port baseline

- Minecraft 1.21.1
- NeoForge 21.1.235
- NeoGradle 7.1.38
- Java 21
- GeckoLib NeoForge 4.9.2
- NeoForge metadata and retained access transformer
- Original custom resource-processing pipeline retained

## Migration progress

- Migrated the networking layer, tick bridges, player attachments, SCP Inventory attachment, saved-data APIs, item custom data, item-stack serialization, and common item API renames.
- Migrating registry aliases, registered armor materials, spawn eggs, menu factories, GUI layers, config-screen extension points, damage events, entity ticks, creative tabs, and item abilities.
- Compiler-frontier logs are generated after each repeatable migration pass.

## Completion rule

This branch is not feature-complete until it builds, launches client and dedicated server, preserves old-world mappings, and passes a parity checklist covering every SCP, inventory/crafting/status/codex workflow, configuration editor, facility block, animated door, keycard reader, survival system, sound, overlay, packet path, and compatibility hook.
