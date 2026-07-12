# SCP Additions 3.0 — Owner Decisions

## Compatibility

- Existing SCP Additions worlds must remain compatible: **yes**
- Existing standalone SCP Inventory worlds must remain compatible: **no**
- Legacy SCP Inventory registry IDs must be preserved: **no**

## Content

- SCP Inventory features to migrate: **all current features**
- Features excluded from the release: **none**
- Duplicate features to remove: **none identified at this stage**
- Incomplete content to hide from release: **none identified at this stage**

The following gameplay systems must be configurable and individually disableable without removing their registry entries:

- SCP Inventory replacement
- custom HUD
- custom health behavior
- stamina
- horror movement behavior
- blink system
- SCP-173 behavior and natural spawning

Facility blocks, decorative props and explicitly spawned SCP-131 entities are ordinary content and remain registered and available without module toggles. Configuration toggles are reserved for systems that materially alter global gameplay behavior.

SCP-131-A and SCP-131-B do **not** currently have natural spawning. The 3.0 migration must preserve that behavior and must not invent a natural spawn system unless it is designed and approved later.

## Inventory and machine integration

- SCP Additions coins become SCP Inventory-owned currency when the custom inventory module is enabled.
- Real coins must not be retained in the vanilla player inventory merely as mirrors.
- SCP-294 must detect, insert and consume coins directly through the shared inventory/currency integration layer.
- Coin pickup, payment and refund paths must remain server-authoritative and duplication-safe.
- Ammunition, weapons, accessories and temporary usable-item sessions keep their own compatibility rules; the coin policy must not be generalized to those categories without explicit review.

## Facility integration

- Source namespace: `scp_unity_extra_blocks`
- Preferred door implementation: SCP Unity Extra Blocks
- Preferred button implementation: SCP Unity Extra Blocks
- Keycards and keycard readers remain based on SCP Additions
- Existing SCP Additions buttons are superseded for new maps, but their registry IDs remain for world compatibility
- All other existing SCP Additions blocks remain registered and supported
- Facility blocks and props are always available content rather than a configurable gameplay module

## Default gameplay module state

All gameplay modules are enabled by default:

- SCP Inventory
- HUD
- custom health
- stamina
- blink
- SCP-173

## Release

- Release name: **Mega SCP Unity Update**
- Version: **3.0.0**
- License: **Creative Commons Attribution-ShareAlike 3.0**
