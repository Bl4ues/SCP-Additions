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
- Migrated registry aliases, registered armor materials, spawn eggs, menu factories, GUI layers, config-screen extension points, damage events, entity ticks, creative tabs, and item abilities.
- Migrated SCP-294 block/item/entity capabilities, tooltip and food APIs, GUI input signatures, effect/attribute holders, recipes, block interactions, and other Minecraft 1.21.1 API changes.
- The validated migration output is now materialized directly in this branch; local builds no longer depend on GitHub Actions modifying an ephemeral checkout first.
- CI must build the checked-in source directly and pass dedicated-server, client, asset, and feature-parity checks.

## Completion rule

This branch is not feature-complete until it builds, launches client and dedicated server, preserves old-world mappings, and passes a parity checklist covering every SCP, inventory/crafting/status/codex workflow, configuration editor, facility block, animated door, keycard reader, survival system, sound, overlay, packet path, and compatibility hook.
