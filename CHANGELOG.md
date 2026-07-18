# Changelog

# SCP Additions 3.0.6 — In Development

## SCP Inventory overflow

- Changed Survival inventory routing so stacks that belong in the SCP Inventory are dropped into the world instead of remaining as an unintended vanilla-inventory fallback when the SCP Inventory cannot accept them;
- Moved the final vanilla-to-SCP routing pass to low event priority so durability and temporary-session synchronization can settle before overflow is evaluated;
- Changed normal equipment removal so a full SCP Inventory drops the removed item instead of silently retaining it or using vanilla storage as overflow.

## Hazmat Suit and fixes

- Reintroduced the Hazmat Suit;
- Added a four-second hold-to-equip sequence and a three-second timed removal sequence;
- Added a reusable timed-equipment progress bar based on the Blink Bar;
- Converted attempts to remove, shift, replace, or drop any internal Hazmat piece into the complete suit-removal sequence and return one public Hazmat Suit item;
- Added complete-set eye protection and a reusable sealed-protection check for future chemical, biological, radiation, and environmental hazards;
- Made the sealed suit reject splash-potion and lingering-potion effects (e.g. instant potion healing or damage), while leaving commands and deliberately internal effects available to their own systems;
- Prevented eating, drinking, drinking potions, eating cake, and every item classified as `CONSUMABLE` by the SCP Inventory while the mask is sealed;
- Kept explicit JSON item rules authoritative while separating SCP Inventory categories from physical hand-use checks, preventing Splash/Lingering Potions and other non-ingestible usable items from being mistaken for food or drink;
- Added a public-item tooltip describing sealing, light protection, and the stationary equip/removal controls;
- Added leather-equivalent armor points to the complete suit while keeping the hidden internal pieces non-repairable and effectively unbreakable;
- Made the complete Hazmat Suit consume stamina twice as quickly and display the stamina bar in red.

## SCP-714

- Added SCP-714 as an `ACCESSORYHAND` item with intrinsic `NO_STAMINA` and reusable `SCP_714_PROTECTION` behavior;
- Added persistent server-authoritative exhaustion that progressively slows the wearer over two minutes, generates a code-driven green-black vignette, freezes horizontal movement at full exposure, and causes a custom coma death after a final five-second grace period;
- Added subtle first-person fatigue messages at 90, 110, and 120 seconds without explicitly instructing the player to remove the ring;
- Added the final Blockbench 3D item model and 128×128 texture, with namespaced texture references for reliable loading;
- Added a reusable protection API for future mental and anomalous threats such as SCP-012.

## Development

- Expanded the Java 17 Gradle build workflow to validate feature branches and retain successful compiled JARs as workflow artifacts.

# SCP Additions 3.0.5 — Refinement Update

## Facility floors

- Added automatic connected-transition logic and model support between the blue and gray Sector 1 floor blocks;
- Added tooltips to both Sector 1 floor blocks explaining that they connect automatically;
- Reworked the vanilla fallback for the SL1 Small Floor Arrow and SL1 Big Floor Arrow to use a stable cutout pass with a muted gray-blue tint, preserving their faded-paint appearance without angle-dependent disappearance.

## Facility building

- Consolidated the SL1 Wall Detail into one adaptive building block: a single piece uses the bottom model, vertical stacks automatically use bottom and top endpoints, and longer columns fill their interior with the middle model;
- Kept the previous middle and top registry entries for world compatibility while removing them from the normal creative-building list;
- Renamed the adaptive piece to **SL1 Corner Wall Detail** and replaced the pane-shaped outlines on it and the **SL1 Pillar Wall Detail** with directional solid collision shapes that match their model footprints.
- Changed **SL1 Corner Wall Detail** placement to use the exact click position: the right half of a wall face keeps its natural orientation, the left half rotates it 90 degrees counter-clockwise, and the four quadrants of top or bottom faces select the corresponding corner; vertical stacks continue inheriting the facing of their neighboring segment.

## SCP-173

- Changed natural-spawn scheduling to begin a full interval when each player joins the server, preventing a spawn check from firing immediately because the player's accumulated tick counter happened to align with the old global check.

## Presentation and naming

- Standardized capitalization and removed inconsistent spacing from legacy SCP Unity block names, including Sector 1 and Sector 2 floors, walls, details, SCP-914 parts, and the Decontamination Checkpoint.

## Blink controls

- Fixed manual blink input after the 3.0.3 keybind change and restored its intended activation rule: the key only closes the player's eyes while the Blink Bar is active;
- Removed an obsolete duplicate `key.scpinventory.blink` mapping. The 3.0.3 change from `Space` to `B` updated the SCP-173 mapping but left the older SCP Inventory mapping using the same identifier, allowing Minecraft to load or rebind one instance while gameplay checked the other. I forgor 💀

## Technical changes

- Unified the in-game mod version with the Gradle project version so future builds no longer require the version to be updated separately in `mods.toml`. Just so I don't forget to update the in-game version, _again_.

# SCP Additions 3.0.4 — Hotfix

- Fixed a wrong centralized header position in the Status tab.

# SCP Additions 3.0.3 — Quality of Life Update

## Mod presentation

- Added the SCP Additions logo to Forge's Mods list.

## Configuration center
- Fixed dedicated servers crashing during network registration because sound packets referenced client-only `SoundInstance` classes;
- Fixed `inventory.enabled: false` still allowing background tick handlers and gameplay packets to move items into the SCP Inventory; the server now synchronizes this module state to clients, blocks stale actions, closes the custom screen, and safely returns already-stored items to the vanilla inventory when disabled;
- Removed a cut SCP-079 1.0 behavior that silently forced `teslaGateOn` back to `true`;
- Expanded the closed Decontamination Checkpoint collision across the complete doorway to prevent escaping around its sides during a gas cycle;
- Added a native configuration center available from **Mods → SCP Additions** and `/scpadditions config`;
- Added dedicated editors for modules, item categories and equipment effects, hidden Status effects, SCP-173 targets, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes;
- Added registry search, translated display names, item/entity icons, dynamic recipe inputs and outputs, a drink color picker, tooltips, and a unified SCP Unity-inspired interface using Roboto with a Montserrat main title;
- Added validation, automatic `.bak` backups, transactional rollback, runtime reload, malformed-JSON rejection, and Windows-resilient file replacement;
- Restricted configuration writes and legacy editor packets to the integrated owner or players with operator permission level 2 or higher;
- Removed `CODEX` from generic item-category editors and config-token parsing; documents are now created exclusively through `codex_documents`, avoiding empty document entries;
- Expanded world-scoped Codex image import from PNG-only to PNG/JPG/JPEG and raised the per-image limit from 900 KB to 2.5 MB;
- Unique generated Codex items are routed directly to the Documents area when the SCP Inventory module is enabled;
- World-scoped Codex PNG/text assets now report loading, missing and empty states instead of displaying the old synthetic document page;
- New Codex entries currently use `minecraft:paper` as a temporary default item until dedicated document items are implemented in a future update. This default can be changed to any registered item before saving;
- Added direct Codex text editing and PNG/JPG/JPEG drag-and-drop import. Imported assets are stored as real files in the current world's `scp_additions/codex_assets` folder and sent to clients on demand; JSON definitions contain only compact references, while packaged resources remain supported;
- Added optional unique-item matching for Codex definitions. Unique documents use an NBT identifier, retain the configured display name and can be generated with the editor's **Give Test Item** action, avoiding every ordinary copy of the base item becoming the same document.

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
- Centered and polished the Status parameter layout while moving only the rendered player preview inside its original frame;
- Status-effect duration bars now begin full when an effect is applied and drain proportionally to that effect's actual remaining duration;
- Added a subtle outward-pulsing amber vignette for both positive and negative SCP-1176 honey outcomes;
- Made SCP-1176's negative-result music follow the affected player's head and stop immediately on death or disconnect;
- Added subtitles for all custom sounds.

## Automatic item classification

- Added conservative automatic recognition for placeable blocks, common melee/ranged weapons, and manually usable right-click items, including compatible modded subclasses;
- Explicit `item_rules` remain authoritative over all automatic detection;
- Added datapack override tags for forcing `WEAPON`, `USABLE`, or miscellaneous fallback behavior;
- Consumed usable items such as spawn eggs no longer return to the player's hand after their controlled-use session.

## SCP-914 integration and defaults

- Added a two-second pending activation window when no valid intake is present, allowing solo players to wind the key and enter the machine;
- Existing valid recipes and players in the intake still start immediately, and repeated key use cannot queue duplicate pending cycles;
- Added a material-aware fallback resolver that can infer transformations from registered crafting recipes and related item tiers when no explicit recipe matches;
- Explicit JSON recipes remain fully authoritative and always take priority over inferred transformations;
- Once an SCP-914 cycle starts, every item in the intake is consumed even when only part of the intake contributed to the selected result;
- Trimmed generic vanilla transformations from the bundled defaults so the fallback resolver handles broad behavior while explicit defaults remain focused on SCP Additions content and special entity transformations;
- Reduced the bundled SCP-914 1:1 skin pool to five selected defaults and renumbered them consistently as `skin1.png` through `skin5.png`.

## Configuration and building

- Added `/scpadditions reload` to validate and reload configurations without restarting;
- Invalid JSON and malformed IDs now produce clear errors without partially applying the reload;
- Missing optional mod entries are now reported and ignored while valid configuration entries continue loading;
- SCP-914 now skips only recipes whose items or entities are unavailable instead of keeping unusable recipes;
- Configuration saves now preserve the previous file as a `.bak` backup and retry/fallback safely when Windows temporarily blocks atomic replacement;
- Fixed block interaction anchors edited with `K` not being persisted or reloaded reliably;
- Fixed the target identifier in the `K` interaction editor overlapping the Action label and field;
- Screwdrivers can now copy a Keycard Reader level and quickly apply it to other readers;
- Screwdrivers now show their controls and copied Reader level in their tooltip;
- Context prompts now use the normal block and held-item interaction path, including crouch interactions;
- Pick Block now returns public blocks instead of internal animation states.

## Performance and polish

- Interaction prompts now update more efficiently, stay steadier between nearby targets, and no longer appear through solid blocks;
- Reduced unnecessary background checks from Tesla Gates and SCP-131 sounds;
- Reduced repeated processing in SCP-079, SCP-1176, SCP-294, and SCP-902 systems.

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
* Added HUD elements for stamina, health, Blink, SCP-131 notices, inventory full notices, SCP-1176 status, and contextual interaction prompts;
* Added Codex support with configurable documents, world-scoped images, text, and generated unique document items;
* Added a dedicated Status screen for effects and parameters;
* Added a centralized configuration center and runtime reload support.
