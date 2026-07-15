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
- Added Lubricated Eye, which prevents Eye Sore, doubles the blink interval, and turns the blink bar blue;
- Added the `PROTECTED_EYES` item effect for eye-protecting equipment.

## Interface and accessibility

- The SCP Inventory now remembers its selected section, Codex document, and scrollbar positions until the player leaves the world;
- Added an option to disable this interface memory;
- Added subtitles for all custom sounds.

## Configuration and building

- Added `/scpadditions reload` to validate and reload configurations without restarting;
- Invalid JSON and registry IDs now produce clear errors without partially applying the reload;
- Screwdrivers can now copy a Keycard Reader level and quickly apply it to other readers;
- Pick Block now returns public blocks instead of internal animation states.

## Performance and polish

- Interaction prompts now update more efficiently, stay steadier between nearby targets, and no longer appear through solid blocks;
- Reduced unnecessary background checks from Tesla Gates and SCP-131 sounds;
- Reduced repeated processing in SCP-079, SCP-1176, SCP-294, and SCP-902 systems;
- Simplified the blood type display without changing its appearance or behavior.

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

## Audio and presentation

- Rebalanced the volume of facility, SCP, terminal, and machine sounds;
- Fixed SCP-131 idle and movement sounds stopping, overlapping, or losing their position;
- Fixed the missing lang for Playing Card;
- Fixed Playing Card, Credit Card, Pieces of Paper, Coin, and Paper Cup appearing under Spawn Eggs tab; they now appear in SCP Additions Misc tab.

## SCP Inventory and items

- Fixed Tesla Gate Terminals failing to recognize Security Credentials stored in SCP Inventory;
- Fixed SCP-294 coins being lost or consumed incorrectly;
- Fixed equipped weapons returning to SCP Inventory after a durability-changing hit.

## Other fixes and adjustments

- Reduced how often SCP-079 attempts to control facility doors;
- Fixed saved Tesla Gates remaining stuck in a shock animation;
- Fixed Tesla Gates failing to respond to their terminals;
- Fixed the SCP-914 death sound playing for Coarse outcomes;
- Fixed the Decontamination being stuck locked after leaving the world while inside of an active one.

# SCP Additions 3.0.0 - Mega SCP Unity Update

## Release corrections

* Fixed some other placement and texture bugs;
* Restored setting-specific SCP-914 processing for players inside the intake while keeping item and non-player entity transformations on the modern JSON recipe engine;
* Restored Rough crushing damage, Coarse slowdown and delayed lethal damage, Fine temporary speed and jump enhancement followed by collapse, and Very Fine extreme temporary enhancement followed by lethal damage;
* Restored the SCP-914 1:1 metamorphosis advancement and player appearance transformation;
* Added `config/scpadditions/scp914_skins`, with the eleven legacy skins copied on first launch and support for additional 64x64 or legacy 64x32 PNG skins;
* Added random 1:1 skin selection, persistence, remote-player synchronization, and tracking synchronization;
* Kept Kleiders Custom Renderer optional: it is required only to render SCP-914-selected player skins;
* Added MoreMcmeta Emissive Textures as an optional client dependency for supported emissive facility textures.

## New Gameplay mechanics

### Inventory and HUD

* Added a full SCP Inventory interface with dedicated storage for general items, equipment, keycards, ammunition, weapons, documents, and currency;
* Added configurable item classification and pickup routing, including custom-only keycard and currency storage;
* Added equipment slots, shift-click equipment handling, context menus, item actions, dropping, moving, and usable-item sessions;
* Added Codex and Status panels with configurable document entries and item information;
* Added custom pickup prompts, inventory-full feedback, contextual interaction prompts, and configurable interaction icons;
* Added localized Roboto font rendering throughout the SCP Inventory and interaction interfaces;
* Added server-authoritative inventory synchronization, persistence, death handling, logout handling, and duplication safeguards;
* Added compatibility access services so keycard readers, Tesla terminals, and other systems can detect items stored in the custom inventory;
* Added configurable custom health and stamina HUDs;
* Added horror-style movement behavior, sprint stamina consumption, regeneration delay, exhausted sprint lock, and configurable stamina-blocking items;
* Added configurable modules for inventory, HUD, custom health, stamina, horror movement, blink, and SCP-173 behavior;

### Blink and visual systems

* Added automatic blinking with configurable timing;
* Added a manual hold-to-blink keybind;
* Added blink meter, vignette, blackout, and post-blink screen effects;
* Added synchronized server-side eye state used by SCP-173 and other observers;
* Added the Eye Sore effect, which accelerates blink drain;
* Added SCP-173 threat confirmation, reveal feedback, and temporary paranoia retention;

## SCP-173

* Added a fully functional SCP-173 entity;
* Added observation checks for players, configured mobs, raiders, and SCP-131;
* Added sampled line-of-sight checks that correctly handle transparent and solid blocks;
* Added deterministic snap movement, direct pursuit, path fallback, and side-step fallback behavior;
* Added immediate movement reevaluation when observers blink or close their eyes;
* Added contact-only neck snap damage;
* Added natural spawning, isolated routine-spawn behavior, inactivity until first observation, and unseen despawning;
* Added target recovery after player respawn and isolated post-kill despawning;
* Added movement scrape, rattle, scare, horror, death, and neck-snap sounds.

## SCP-131-A and SCP-131-B

* Added SCP-131-A and SCP-131-B as spawnable entities;
* Added idle voice sounds and custom SCP-styled interaction notices;
* Added right-click following behavior;
* Added persistent following ownership across save and reload;
* Added SCP-131-B following nearby idle SCP-131-A entities;
* Added SCP-131 observation behavior against SCP-173;

## SCP Unity Facility content

* Added Tesla, Archival, Office, Skyroom, and Security area blocks;
* Added Alarm Lamps, Wall Lights, Heaters, Sign Supports, TVs, Trashbins, and other facility props;
* Added animated Default, Yellow, Black, Normal, Logistics, Office, Bathroom, and Workshop doors;
* Added opening and closing animations, timing, sounds, drops, collision behavior, and direct or redstone activation rules;
* Added functional door buttons with mirrored left and right models;
* Updated paired button behavior so an existing opposite panel synchronizes without automatically creating a second panel behind the wall.

## Keycards, readers, and tools

* Integrated keycards with the custom inventory without maintaining duplicate vanilla mirror stacks;
* Updated shared inventory checks so readers detect custom-only keycards directly;
* Added a screwdriver;
* Added crouch-and-screwdriver reader configuration through both direct interaction and the custom contextual interaction system.

## Configuration and data

* Added `config/scpadditions/modules.json` for gameplay module controls;
* Added `config/scpinventory/scpinventory.json` for inventory, HUD, stamina, movement, blink, and item behavior;
* Added `config/scpinventory/context_interactions.json` for contextual block and entity interactions;
* Added expanded default SCP-294 drink definitions;
* Added expanded default SCP-914 item and entity recipes;
* Added first-run copying of bundled default configuration files;
* Existing local configuration files are not overwritten automatically;
* Kept SCP-294 currency handling exclusive to either the custom inventory or vanilla inventory, depending on the selected inventory mode;

## Technical changes

* Updated the mod version to 3.0.0;
* Added GeckoLib 4.4.9 or newer as a required dependency;
* Updated the minimum Forge version to 47.4.10;
* Preserved existing published SCP Additions registry IDs for old-world compatibility;
* Added compatibility mappings for migrated facility content;
* Added extensive configuration defaults, migration documentation, and build validation;
* Fixed multiple item duplication, pickup routing, keycard synchronization, usable-session, interface, rendering, button, door, font, and configuration-generation issues.


# SCP Additions 2.0.2 — Hotfix

## Hotfix update for the 2.0.1 hotfix.

- Fixed bundled SCP-294 and SCP-914 JSON templates being processed as single-line/minified files in the final build output;
- Added build-time pretty printing for packaged `config/scpadditions/*.json` templates so newly generated configs are readable for humans;
- (Hopefully) fixed the inactive Tesla Gate transparency issue by rendering the inactive gate model with the translucent render layer instead of cutout, as it was rendering oddly with shaders, for some reason.


# SCP Additions 2.0.1 — Hotfix

## Hotfix update for the 2.0.0 overhaul.

- Fixed the missing Level 1 Keycard language entry;
- Fixed Security Credentials not being detected by the Tesla Gate Terminal when the item had NBT data;
- Fixed Level 1-6 Keycards not being detected by keycard readers when the item had NBT data;
- Improved keycard reader checks so NBT-modified keycards are accepted as the correct keycard item;
- Fixed Tesla Gate transparency issues with shaders by explicitly registering the Tesla Gate block render layers as cutout on the client;
- Fixed the default SCP-294 drink configuration not being included in the final built;
- Expanded the bundled SCP-294 default drink list with the legacy drink outputs and several SCP:CB-inspired dangerous outputs;
- Expanded the bundled SCP-914 default recipe list with keycard, vanilla item, SCP Additions item, and entity transformations;
- Improved SCP-294 config generation so the first generated `294drinks.json` can be copied from the full packaged template instead of falling back to the minimal internal example;
- Improved SCP-914 config generation so the first generated `914recipes.json` can be copied from the full packaged template instead of falling back to the minimal internal example;
- Included the default `config/scpadditions` templates in the build output.

# SCP Additions 2.0.0

## Major overhaul for the 1.20.1 version of SCP Additions.

- Added a full SCP Unity-inspired Tesla Gate Terminal interface;
- Added a new functional Tesla Gate Terminal model and a separate decorative OFF terminal;
- Added Security Credentials as Tesla Gate Terminal admin access instead of turning interaction;
- Added global Tesla Gate controls through the terminal;
- Added Emergency Override mode for Tesla Gates;
- Added new Tesla Gate Terminal sounds, including UI clicks, popups, standby sounds, override activation, and a terminal ambient loop;
- Added a stronger overcharge sound and extra particles for Tesla Gates while Emergency Override is active;
- Improved Tesla Gate placement so the model is placed one block higher when needed;
- Improved Decontamination Checkpoint placement using the same one-block-up placement correction;
- Improved Tesla Gate collision behavior (though still needs some tweaks);
- Improved keycard logic so higher-level keycards open their own level and all lower-level readers when the keycard is in the inventory;
- Added a custom SCP-294 GUI using the new screen texture, coin panel, input screen, and enter button;
- Added dynamic SCP-294 drink cups with configurable liquid colors and drink behavior;
- Converted SCP-294 drinks to a runtime JSON configuration;
- Added SCP-294 fuzzy drink matching, aliases, effects, actions, sounds, and custom death messages;
- Added a custom SCP-914 GUI with a draggable dial and snapping settings;
- Converted SCP-914 recipes to a runtime JSON configuration;
- Added support for SCP-914 recipe fragments in `914recipes.d`;
- Added SCP-914 weighted outputs, item recipes, entity recipes, machine offsets, and configurable processing behavior;
- Added an SCP-914 Assembly Kit item that places the full saved SCP-914 structure from an NBT structure file;
- Added visual blocked-space feedback when the SCP-914 Assembly Kit cannot place the structure;
- Improved SCP-914 orientation, intake/output range offsets, and machine processing logic;
- Improved SCP-914 door behavior during processing;
- Added multiple new default SCP-914 item and entity transformations (including default interactions with other mods);
- Improved several GUI textures and screen overlays;
- Made `kleiders_custom_renderer` optional;
- Removed MCreator entirely from the project;
- Removed SCP-059 and its related content from the mod as it was really bugged and really not worth the headache to fix it;
- Removed the Hazmat Suit, Geiger Counter, radiation-related SCP-059 systems, and related legacy content;
- Removed obsolete hardcoded SCP-294 and SCP-914 procedures;
- Fixed multiple rendering, transparency, placement, and GUI behavior issues;
- Fixed Tesla Gate Terminal state synchronization when opening terminals;
- Fixed several outdated or misleading item names and tooltips.
