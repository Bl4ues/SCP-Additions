# Changelog

# SCP Additions 3.0.3 — Quality of Life Update

## Creative mode

- Custom interaction prompts now appear and work in Creative by default;
- Added an option to hide and disable custom interactions in Creative;
- Inventory-full and SCP-131 notices now appear in Creative.

## Controls and guidance

- SCP-131 followers now use a configurable dismissal key, with notices showing the selected key;
- Manual blink now uses `B` by default instead of the jump key;
- The first SCP-173 activation in a world now explains how to blink manually using the selected key.

## Eye effects and decontamination

- Decontamination Checkpoints now cause Eye Sore for 30 seconds;
- Added Lubricated Eye, which prevents Eye Sore and doubles the blink interval;
- Added the `PROTECTED_EYES` item effect for eye-protecting equipment.

## Interface and accessibility

- The SCP Inventory now remembers its selected section, Codex document, and scrollbar positions until the player leaves the world;
- Added an option to disable this interface memory;
- Moved SCP-1176 blood type display into the Status panel and removed the old inventory overlays;
- Added subtitles for all custom sounds.

## Configuration and building

- Added a native in-game configuration center accessible from the Forge mod list or `/scpadditions config`, without requiring an external configuration-menu mod;
- Added dedicated visual editors for modules, inventory and equipment rules, Status filters, SCP-173 targets, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes;
- The SCP-914 editor supports searchable item selection, dynamic intake/output lists, machine-setting selection, recipe duplication and file placement, weighted results, and a compact collapsed section for additional fields;
- New SCP-914 recipes created in-game are stored in `914recipes.d/in_game_editor.json` by default, while existing advanced, entity, and unknown fields are preserved;
- In-game saves are validated and applied on the server with atomic writes, `.bak` backups, runtime reload, and rollback if the new configuration cannot be applied;
- Configuration editors and their network packets now require operator permission level 2 or the integrated single-player owner;
- Added `/scpadditions reload` to validate and reload configurations without restarting;
- Invalid JSON and malformed IDs now produce clear errors without partially applying the reload;
- Missing optional mod entries are now reported and ignored while valid configuration entries continue loading;
- SCP-914 now skips only recipes whose items or entities are unavailable instead of keeping unusable recipes;
- Configuration saves now preserve the previous file as a `.bak` backup;
- Screwdrivers can now copy a Keycard Reader level and quickly apply it to other readers;
- Screwdrivers now show their controls and copied Reader level in their tooltip;
- Context prompts now use the normal block and held-item interaction path, including crouch interactions;
- Pick Block now returns public blocks instead of internal animation states.

## Performance and polish

- Interaction prompts now update more efficiently, stay steadier between nearby targets, and no longer appear through solid blocks;
- Reduced unnecessary background checks from Tesla Gates and SCP-131 sounds;
- Reduced repeated processing in SCP-079, SCP-1176, SCP-294, and SCP-902 systems;

# SCP Additions 3.0.2 — Stability Hotfix

## Facility doors

- Fixed a crash when placing or loading redstone-powered facility doors directly beside one another.

# SCP Additions 3.0.1 — Hotfix

## Additions

- Added a new ambient sound that plays when entering a world.

## SCP-173

- Fixed SCP-173 moving or killing players while clearly being watched, including cases where its visible and real positions did not match;
- Neck snaps now require actual physical contact with the target, as intended;
- Limited automatic blink movement to six blocks, removed unintended extra movement, and kept manual blinking continuous;
- Fixed observation through solid blocks, facility doors, and door animations, while preserving sight through the windows in Normal and Office doors;
- Fixed SCP-173 activating or moving before being seen by a Survival player; Creative and Spectator players no longer activate it;
- Restored the natural-spawn rattle and corrected the jumpscare and blink HUD triggers;
- Fixed SCP-131 and other configured creatures sometimes failing to keep SCP-173 frozen.

## Facility doors

- Fixed collision and passage for Normal, Logistics, Office, Bathroom, and Workshop doors, including mobs failing to use visibly open doorways (it's not perfect yet, because minecraft);
- Door opening and closing sounds now come from the door's position.

## Decontamination Checkpoint

- Fixed players being detected outside the chamber or missed while inside it;
- Fixed repeated sounds, processing, and particle effects during the same visit, as well as checkpoints failing to reactivate after players left;
- Improved the decontamination gas so it lasts for the full cycle and smoothly fills the chamber from the floor grilles.