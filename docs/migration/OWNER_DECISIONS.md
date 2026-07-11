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

The following systems must be configurable and individually disableable without removing their registry entries:

- SCP Inventory replacement
- custom HUD
- custom health behavior
- stamina
- horror movement behavior
- blink system
- SCP-173 behavior and natural spawning
- SCP-131 behavior and natural spawning
- facility content

## Facility integration

- Source namespace: `scp_unity_extra_blocks`
- Preferred door implementation: SCP Unity Extra Blocks
- Preferred button implementation: SCP Unity Extra Blocks
- Keycards and keycard readers remain based on SCP Additions
- Existing SCP Additions buttons are superseded for new maps, but their registry IDs remain for world compatibility
- All other existing SCP Additions blocks remain registered and supported

## Default module state

All modules are enabled by default:

- SCP Inventory
- HUD
- custom health
- stamina
- blink
- SCP-173
- SCP-131
- facility blocks

## Release

- Release name: **Mega SCP Unity Update**
- Version: **3.0.0**
- License: **Creative Commons Attribution-ShareAlike 3.0**
