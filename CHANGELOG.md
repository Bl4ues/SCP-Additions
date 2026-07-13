# Changelog

## SCP Additions 3.0.1 — Door, SCP-173, and Audio Hotfix

- Replaced the generic centered hitbox used by every manual facility door with family-specific closed and open shapes ported from SCP Unity Extra Blocks;
- Corrected manual-door collision alignment for the Normal, Logistics, Office, Bathroom, and Workshop door families in every horizontal orientation;
- Made manual door transitions passable while closed endpoints remain solid, and explicitly exposed open/closed pathfinding state to mobs;
- Corrected SCP-173 visibility checks so the full two-block-tall geometry of closed facility doors blocks observation instead of being treated as transparent merely because their models use `noOcclusion`;
- Treated the Normal manual door's dark inset as an opaque decorative panel while preserving sight only through the Office door's actual window opening;
- Strengthened line-of-sight checks for solid and shaped blocks while preserving visibility through glass, panes, leaves, and genuine openings;
- Restored the intended rattle sound when SCP-173 naturally spawns;
- Prevented naturally spawned SCP-173 from selecting targets, navigating, moving, or producing dragging sounds before a non-creative player actually sees it;
- Restricted the initial activation to confirmed player observation, while other configured observers can still freeze SCP-173 normally after activation;
- Kept the initial SCP-173 scare and blink HUD activation gated behind confirmed player vision and proximity;
- Prevented the SCP-914 death sound from playing for Coarse outcomes, whose lethal result represents delayed internal bleeding rather than an immediate machine kill;
- Added the new `enter.ogg` UI sound, played once for the joining player on every world login;
- Finalized the 3.0.1 audio mix by raising the entry sting and terminal power-on, power-off, and override-start cues to 27.0 while preserving the approved values for all other sounds;
- Fixed Tesla Gate Terminal authentication so Security Credentials stored in the SCP Inventory Keys section are recognized, with a matching server-side authorization check;
- Finalized SCP-294 currency handling so the canonical coin is recognized even with stale configs, configured coins route into SCP Inventory, payments consume exactly one escrowed coin, and unused coins refund to the authoritative backend without vanilla mirrors;
- Fixed equipped WEAPON items returning to SCP Inventory after one durability-changing hit;
- Reworked Decontamination Checkpoint detection around the model's actual interior bounds instead of a generic proximity cube;
- Reworked Decontamination Checkpoint particles to rise from the exact two floor-vent rectangles in every horizontal orientation rather than following players;
- Added per-checkpoint latching, single-session output, and non-repeating reload transitions to prevent repeated sounds, particles, and processing while a player remains inside;
- Added the missing English display name for the SCP Additions Playing Card;
- Updated the mod version to 3.0.1.


## SCP Additions 3.0.0 — Mega SCP Unity Update

The largest SCP Additions update so far. SCP Inventory and SCP Unity Extra Blocks have been consolidated into SCP Additions as internal systems, so only one mod file is required.

### Release candidate corrections

- Fixed first-run `context_interactions.json` generation so the complete bundled list of 99 interaction definitions is copied instead of the obsolete short in-code example;
- Made the bundled context interaction JSON the single authoritative default used by both the runtime registry and the in-game editor;
- Restored setting-specific SCP-914 processing for players inside the intake while keeping item and non-player entity transformations on the modern JSON recipe engine;
- Restored Rough crushing damage, Coarse slowdown and delayed lethal damage, Fine temporary speed and jump enhancement followed by collapse, and Very Fine extreme temporary enhancement followed by lethal damage;
- Restored the SCP-914 1:1 metamorphosis advancement and player appearance transformation;
- Added `config/scpadditions/scp914_skins`, with the eleven legacy skins copied on first launch and support for additional 64x64 or legacy 64x32 PNG skins;
- Added random 1:1 skin selection, persistence, remote-player synchronization, and tracking synchronization;
- Kept Kleiders Custom Renderer optional: it is required only to render SCP-914-selected player skins;
- Added MoreMcmeta Emissive Textures as an optional client dependency for supported emissive facility textures;
- Clarified that GeckoLib 4.4.9 or newer is required while Kleiders Custom Renderer and MoreMcmeta Emissive Textures are optional client enhancements;
- Added the missing SCP-914 Coarse death message and Metamorphosis advancement title translations;
- Restored the SCP-914 death sound when a setting-specific player outcome actually kills the player;

### Integrated SCP Inventory systems

- Added the full SCP Inventory interface with dedicated storage for general items, equipment, keycards, ammunition, weapons, documents, and currency;
- Added configurable item classification and pickup routing, including custom-only keycard and currency storage;
- Added equipment slots, shift-click equipment handling, context menus, item actions, dropping, moving, and usable-item sessions;
- Added Codex and Status panels with configurable document entries and item information;
- Added custom pickup prompts, inventory-full feedback, contextual interaction prompts, and configurable interaction icons;
- Added localized Roboto font rendering throughout the SCP Inventory and interaction interfaces without replacing Minecraft's global font;
- Added server-authoritative inventory synchronization, persistence, death handling, logout handling, and duplication safeguards;
- Added compatibility access services so keycard readers, Tesla terminals, and other systems can detect items stored in the custom inventory;
- Added configurable custom health and stamina HUDs;
- Added horror-style movement behavior, sprint stamina consumption, regeneration delay, exhausted sprint lock, and configurable stamina-blocking items;
- Added configurable modules for inventory, HUD, custom health, stamina, horror movement, blink, and SCP-173 behavior;

### Blink and visual systems

- Added automatic blinking with configurable timing;
- Added a manual hold-to-blink keybind;
- Added blink meter, vignette, blackout, and post-blink screen effects;
- Added synchronized server-side eye state used by SCP-173 and other observers;
- Added the Eye Sore effect, which accelerates blink drain;
- Added SCP-173 threat confirmation, reveal feedback, and temporary paranoia retention;

### SCP-173

- Added a fully functional SCP-173 entity with GeckoLib model and animation support;
- Added observation checks for players, configured mobs, raiders, and SCP-131;
- Added sampled line-of-sight checks that correctly handle transparent and solid blocks;
- Added deterministic snap movement, direct pursuit, path fallback, and side-step fallback behavior;
- Added immediate movement reevaluation when observers blink or close their eyes;
- Added contact-only neck snap damage and a dedicated death message;
- Added configurable natural spawning, isolated routine-spawn behavior, inactivity until first observation, and unseen despawning;
- Added target recovery after player respawn and isolated post-kill despawning;
- Added frozen air and water behavior while observed;
- Added movement scrape, rattle, scare, horror, death, and neck-snap sounds;
- Added 1730 health, strong armor, toughness, knockback resistance, and custom durability rules;

### SCP-131-A and SCP-131-B

- Added SCP-131-A and SCP-131-B as spawnable entities;
- Added GeckoLib models, animations, translucent rendering, and glowing eye layers;
- Added idle voice sounds and custom SCP-styled interaction notices;
- Added right-click following behavior;
- Added a physical G-key hold action to stop owned SCP-131 followers;
- Added persistent following ownership across save and reload;
- Added SCP-131-B following nearby idle SCP-131-A entities;
- Added SCP-131 observation behavior against SCP-173;

### SCP Unity facility content

- Integrated the SCP Unity Extra Blocks architectural set into SCP Additions;
- Added Tesla, Archival, Office, Skyroom, and Security structural blocks;
- Added Alarm Lamps, Wall Lights, Heaters, Sign Supports, TVs, Trashbins, and other facility props;
- Added animated Default, Yellow, Black, Normal, Logistics, Office, Bathroom, and Workshop door families;
- Added family-specific opening and closing animations, timing, sounds, drops, collision behavior, and direct or redstone activation rules;
- Added a unified SCP Unity Blocks creative tab with a curated block order;
- Added functional door buttons with mirrored left and right models;
- Updated paired button behavior so an existing opposite panel synchronizes without automatically creating a second panel behind the wall;
- Fixed left button selection geometry and multiple door/button synchronization issues;
- Added legacy mapping from SCP Unity Extra Blocks registry IDs to their SCP Additions equivalents;
- Preserved intermediate door animation states for saved-world compatibility while hiding them from the creative tab;
- Added Sector 1 and Sector 2 floors, walls, directional floor arrows, wall details, and an open ventilation model from the SCP UBlocks assets;
- Added legacy mapping from `scp_ublocks` block and item IDs to their SCP Additions equivalents;

### Keycards, readers, and tools

- Integrated keycards with the custom inventory without maintaining duplicate vanilla mirror stacks;
- Fixed survival keycard jitter caused by repeated mirror synchronization;
- Updated shared inventory checks so readers detect custom-only keycards directly;
- Added a screwdriver texture and item model;
- Added crouch-and-screwdriver reader configuration through both direct interaction and the custom contextual interaction system;
- Preserved six keycard clearance levels and higher-level access to lower-level readers;

### Configuration and data

- Added `config/scpadditions/modules.json` for gameplay module controls;
- Added `config/scpinventory/scpinventory.json` for inventory, HUD, stamina, movement, blink, and item behavior;
- Added `config/scpinventory/context_interactions.json` for contextual block and entity interactions;
- Added expanded default SCP-294 drink definitions;
- Added expanded default SCP-914 item and entity recipes;
- Added first-run copying of bundled default configuration files;
- Existing local configuration files are not overwritten automatically;
- Kept SCP-294 currency handling exclusive to either the custom inventory or vanilla inventory, depending on the selected inventory mode;

### Compatibility and technical changes

- SCP Additions is now the only Forge mod entrypoint and the only required SCP Additions project JAR;
- SCP Inventory and SCP Unity Extra Blocks are included internally and must not be installed separately;
- Updated the mod version to 3.0.0;
- Added GeckoLib 4.4.9 or newer as a required dependency;
- Updated the minimum Forge version to 47.4.10;
- Preserved existing published SCP Additions registry IDs for old-world compatibility;
- Added compatibility mappings for migrated facility content;
- Added extensive configuration defaults, migration documentation, and build validation;
- Fixed multiple item duplication, pickup routing, keycard synchronization, usable-session, interface, rendering, button, door, font, and configuration-generation issues.

## SCP Additions 2.0.2

Hotfix update for the 2.0.1 hotfix.

- Updated the mod version to 2.0.2;
- Fixed bundled SCP-294 and SCP-914 JSON templates being processed as single-line/minified files in the final build output;
- Added build-time pretty printing for packaged `config/scpadditions/*.json` templates so newly generated configs are readable for humans;
- Fixed the inactive Tesla Gate transparency issue by rendering the inactive gate model with the translucent render layer instead of cutout;
- Kept the active/shocking Tesla Gate models on cutout rendering because they were already rendering correctly.

## SCP Additions 2.0.1

Hotfix update for the 2.0.0 overhaul.

- Updated the mod version to 2.0.1;
- Fixed the missing Level 1 Keycard language entry;
- Fixed Security Credentials not being detected by the Tesla Gate Terminal when the item had NBT data;
- Fixed Level 1-6 Keycards not being detected by keycard readers when the item had NBT data;
- Improved keycard reader checks so NBT-modified keycards are accepted as the correct keycard item;
- Fixed Tesla Gate transparency issues with shaders by explicitly registering the Tesla Gate block render layers as cutout on the client;
- Fixed the default SCP-294 drink configuration not being included in the final built `.jar`;
- Expanded the bundled SCP-294 default drink list with the legacy drink outputs and several SCP:CB-inspired dangerous outputs;
- Expanded the bundled SCP-914 default recipe list with keycard, vanilla item, SCP Additions item, and entity transformations;
- Improved SCP-294 config generation so the first generated `294drinks.json` can be copied from the full packaged template instead of falling back to the minimal internal example;
- Improved SCP-914 config generation so the first generated `914recipes.json` can be copied from the full packaged template instead of falling back to the minimal internal example;
- Included the default `config/scpadditions` templates in the build output;
- Kept existing local SCP-294/SCP-914 configs untouched, so custom configs are not overwritten automatically.

## SCP Additions 2.0.0

Major overhaul for the 1.20.1 version of SCP Additions.

- Updated the mod version to 2.0.0;
- Added a full SCP Unity-inspired Tesla Gate Terminal interface;
- Added a new functional Tesla Gate Terminal model and a separate decorative OFF terminal;
- Added Security Credentials as Tesla Gate Terminal admin access instead of a general keycard level;
- Added global Tesla Gate controls through the terminal;
- Added Emergency Override mode for Tesla Gates;
- Added new Tesla Gate Terminal sounds, including UI clicks, popups, standby sounds, override activation, and a terminal ambient loop;
- Added a stronger overcharge sound and extra particles for Tesla Gates while Emergency Override is active;
- Improved Tesla Gate placement so the model is placed one block higher when needed;
- Improved Decontamination Checkpoint placement using the same one-block-up placement correction;
- Improved Tesla Gate collision behavior;
- Improved keycard logic so higher-level keycards open their own level and all lower-level readers;
- Added a custom SCP-294 GUI using the new screen texture, coin panel, input screen, and enter button;
- Added dynamic SCP-294 drink cups with configurable liquid colors and drink behavior;
- Converted SCP-294 drinks to a runtime JSON configuration;
- Added SCP-294 fuzzy drink matching, aliases, effects, actions, sounds, and custom death messages;
- Removed entity spawning from SCP-294 drink outputs, keeping SCP-294 focused on drinks;
- Added a custom SCP-914 GUI with a draggable dial and snapping settings;
- Converted SCP-914 recipes to a runtime JSON configuration;
- Added support for SCP-914 recipe fragments in `914recipes.d`;
- Added SCP-914 weighted outputs, item recipes, entity recipes, machine offsets, and configurable processing behavior;
- Added an SCP-914 Assembly Kit item that places the full saved SCP-914 structure from an NBT structure file;
- Added visual blocked-space feedback when the SCP-914 Assembly Kit cannot place the structure;
- Improved SCP-914 orientation, intake/output range offsets, and machine processing logic;
- Improved SCP-914 door behavior during processing;
- Added multiple new default SCP-914 item and entity transformations;
- Improved several GUI textures and screen overlays;
- Made `kleiders_custom_renderer` optional;
- Removed MCreator project metadata from the repository;
- Removed SCP-059 content from the mod;
- Removed SCP-059-1 growth/removal systems;
- Removed the Hazmat Suit, Geiger Counter, radiation-related SCP-059 systems, and related legacy content;
- Removed obsolete hardcoded SCP-294 and SCP-914 procedures;
- Fixed multiple rendering, transparency, placement, and GUI behavior issues;
- Fixed Tesla Gate Terminal state synchronization when opening terminals;
- Fixed Tesla Gates being off while Emergency Override was active;
- Fixed several outdated or misleading item names and tooltips.
