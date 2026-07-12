# Registry compatibility policy

## SCP Additions 2.x content

Published SCP Additions registry IDs must remain registered in 3.0 so existing worlds continue to load.

This includes blocks, items, block entities, menus, effects, sounds, structures and other serialized registry objects. A legacy object may be hidden from creative tabs or delegate to a newer implementation, but it must not be removed merely because a replacement exists.

## Existing Additions buttons

The SCP Unity Extra Blocks button system is preferred for new construction. Existing Additions button IDs remain registered for compatibility.

The migration may later:

- hide legacy buttons from the primary creative tab;
- mark them as legacy in translations or documentation;
- adapt their behavior to shared facility services;
- provide an explicit map-upgrade tool.

It must not silently replace blocks in loaded worlds without a tested migration path.

## SCP Inventory content

Standalone SCP Inventory world compatibility is not required. Migrated entities and future registry objects should use the `scp_additions` namespace.

Resource files may continue to live under `assets/scpinventory` while renderers and screens are migrated. Resource namespace retention does not require retaining the old registry namespace.

Planned entity IDs:

- `scp_additions:scp_173`
- `scp_additions:scp_131_a`
- `scp_additions:scp_131_b`

## SCP Unity Extra Blocks content

Standalone Extra Blocks world compatibility is not required. New registry entries should use `scp_additions`.

The `scp_unity_extra_blocks` resource namespace remains temporarily available to avoid premature mass-renaming of blockstates, models, textures and sounds.

## Disabled modules

Module toggles must not conditionally skip `DeferredRegister` entries. A disabled module may suppress:

- natural spawning;
- event handlers;
- HUD rendering;
- input handling;
- active gameplay effects;
- creative-tab exposure where safe.

This rule prevents missing-registry errors and client/server registry disagreement.
