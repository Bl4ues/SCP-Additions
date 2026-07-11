# SCP Additions 3.0 Architecture

## Canonical mod

SCP Additions is the only Forge mod entrypoint and the only public release artifact.

- Canonical mod ID: `scp_additions`
- Canonical version: `3.0.0`
- Minecraft: `1.20.1`
- Java: `17`
- Forge: `47.4.10`

The migrated projects become internal modules rather than additional `@Mod` entrypoints.

## Resource namespaces

During migration, the combined JAR may contain resources under three namespaces:

- `scp_additions`
- `scpinventory`
- `scp_unity_extra_blocks`

Keeping the source namespaces temporarily avoids rewriting hundreds of model, texture, animation and sound references before the related Java systems are functional. Registry entries created for new migrated content should use `scp_additions` unless a compatibility requirement explicitly says otherwise.

## Planned modules

### Core

Owns shared configuration, lifecycle, networking conventions, compatibility services and module bootstrapping.

### Inventory

Migrated from SCP Inventory:

- player capability and persistence
- item classification
- inventory routing
- equipment and key slots
- ammunition and weapon mirrors
- SCP Inventory-owned coin storage without vanilla coin mirrors
- usable-item sessions
- inventory GUI, context menu and codex

Coin handling is intentionally different from ammunition and weapon compatibility:

1. The real coin exists only in SCP Inventory storage while the custom inventory module is active.
2. Pickup routes the Additions coin directly into that storage.
3. SCP-294 queries and consumes currency through a server-authoritative shared currency service.
4. The SCP-294 GUI may display a virtual inserted-coin state, but it must not create a second real coin stack.
5. Failed or cancelled transactions must have an explicit refund path and must never duplicate currency.

### Vitals

- custom HUD
- custom health behavior
- stamina
- horror movement behavior
- visual effects

### Blink

- blink timer and input
- blink HUD
- integration hooks used by SCP-173

### Entities

- SCP-173
- SCP-131-A
- SCP-131-B
- GeckoLib models, animations and render layers

Only SCP-173 currently has a natural-spawn implementation. SCP-131 remains spawnable through explicit placement or commands until a separate spawn design is approved.

### Facility

Migrated from SCP Unity Extra Blocks:

- facility decoration
- doors
- preferred button system
- lights and environmental blocks
- facility sounds

Existing SCP Additions keycard readers remain the access-control foundation.

## Integration rules

1. Do not add a second or third `@Mod` entrypoint.
2. Do not copy standalone build scripts into SCP Additions.
3. Do not compile SCP Inventory through generated source patches in the final project.
4. Migrate the already generated canonical Inventory sources into normal source files.
5. Keep registry entries present even when a module is disabled.
6. Configuration toggles disable behavior, HUD, spawning or creative exposure—not registry creation.
7. Preserve every published SCP Additions registry ID needed by existing worlds.
8. SCP Inventory and SCP Unity Extra Blocks IDs may be redesigned because standalone world compatibility is not required.
9. Shared gameplay checks must use compatibility services instead of directly scanning only the vanilla inventory.
10. Coin payment and mutation must use a dedicated server-side currency abstraction rather than a read-only item scan.
11. Every migration phase must build before the next phase begins.

## Networking

The existing SCP Additions channel remains active. SCP Inventory networking may initially be migrated as a separate internal channel if preserving packet order is safer. Channel consolidation is optional and should happen only after functional parity.

## MCreator policy

The existing SCP Additions Java source is the canonical source for 3.0. Regenerating the entire project from MCreator after integration is not supported because it may overwrite manual modules and compatibility code.
