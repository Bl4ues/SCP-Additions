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

1. When the custom SCP Inventory module is enabled, the real coin exists only in the SCP Inventory capability.
2. With the custom inventory enabled, coin pickup routes directly into the capability and creates no vanilla mirror.
3. With the custom inventory enabled, SCP-294 must read and extract coins only from the capability. It must not fall back to vanilla inventory.
4. When the custom SCP Inventory module is disabled, coin pickup follows vanilla inventory behavior.
5. With the custom inventory disabled, SCP-294 must read and extract coins only from the vanilla inventory. It must ignore capability currency.
6. Capability and vanilla balances must never be added together or treated as simultaneous payment sources.
7. Switching modes must not copy currency automatically. Any migration of existing coins between stores must be explicit, server-authoritative and duplication-safe.
8. The SCP-294 GUI represents an inserted coin exactly once through its machine slot/state.
9. Failed or cancelled transactions must have an explicit refund path and must never duplicate currency.

The shared `PlayerCurrencyAccess` service owns this mode selection. The SCP Inventory capability will register its mutable backend during the inventory-core migration. SCP-294 will only be switched to this service after that backend is available, preventing the default-enabled inventory module from temporarily disabling coin payments.

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
11. Currency source selection is exclusive: capability when custom inventory is enabled, vanilla inventory when it is disabled.
12. Every migration phase must build before the next phase begins.

## Networking

The existing SCP Additions channel remains active. SCP Inventory networking may initially be migrated as a separate internal channel if preserving packet order is safer. Channel consolidation is optional and should happen only after functional parity.

## MCreator policy

The existing SCP Additions Java source is the canonical source for 3.0. Regenerating the entire project from MCreator after integration is not supported because it may overwrite manual modules and compatibility code.
