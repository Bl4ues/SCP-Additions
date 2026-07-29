# Changelog

# SCP Additions 3.1.0 — In Development

## Creative inventory organization

- Standardized the creative-tab names as **SCP Additions - SCPs**, **SCP Additions - Items**, and **SCP Additions - Blocks**;
- Organized facility content under ten headers in this order: **Functional**, **Decoration**, **General**, **Core Room**, and **Zones**.

## SCP-106

- Added SCP-106 as a new roaming threat;
- SCP-106 can appear naturally after the player has spent some time in the world, emerging from the ground or nearby walls before immediately beginning a hunt;
- Added close-range attacks that deal damage and apply Wither;
- Added a ranged attack that throws a straight trail of corrosion across the floor when the player creates distance but remains in clear view, dealing damage, Wither, and Slowness on a direct hit;
- Corrosion left by SCP-106 continuously slows Survival players who walk over it;
- SCP-106 can phase through solid blocks, moving more slowly while inside them and leaving temporary portals on the surfaces it enters and exits;
- If the player creates too much distance, SCP-106 can disappear and re-emerge ahead of the player's path;
- Tesla Gates repel SCP-106, forcing it to sink away and preventing the next two natural spawn checks;
- Hunts can end quickly or continue for several minutes depending on how long SCP-106 remains interested in the target; players can create distance, but cannot lose SCP-106 before the hunt ends.

## SCP-173

- Reduced SCP-173's rendered height to approximately two blocks;
- Reworked SCP-173's movement audio: `stone_scrap.ogg` now loops only while the statue moves, while one of `stone_scrap_1.ogg` through `stone_scrap_5.ogg` plays when it turns or stops; removed the natural-spawn rattle and its unused assets;
- Prevented SCP-173 from scraping and shuffling against closed doors when no complete path to its target exists; it now waits silently and resumes as soon as a route becomes available.

## SCP-131

- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally;
- SCP-131 instances now dismiss their player's follower group when they begin watching SCP-173, matching the manual dismiss action.

## SCP-079

- Added a processing-power system that limits how often SCP-079 can interfere with the facility and forces it to choose its actions more carefully;
- SCP-079 now reacts differently depending on the threat chasing the player, using doors, temporary access denial, and nearby Tesla Gates when useful;
- Improved its SCP-012 trap behavior so repeated interference becomes increasingly difficult and less worthwhile;
- SCP-079 can now close and lock an open door to separate following SCP-131 instances when SCP-173 is waiting ahead;
- SCP-079 can spend processing power continuously to keep a useful door locked for longer, releasing it when the strategy is no longer useful or it cannot afford the upkeep;
- Added a positional hacking sound at each door, Tesla Gate, SCP-012 box, or other facility device successfully manipulated by SCP-079;
- Added optional Debug Tools displays for SCP-079's power, and its recent decisions.

## Roamer spawning and developer tools

- Added natural spawn cycles for both SCP-173 and SCP-106, with separate `173spawn` and `106spawn` gamerules;
- SCP-106 begins checking for a possible encounter earlier than SCP-173, while both continue with recurring checks afterward;
- When one roamer is already active, the other becomes less likely to appear, but rare double encounters are still possible;
- Spawn timers stop while a matching roamer is active and restart after it dies or despawns;
- Added `/disableAllRoamers`, `/enableAllRoamers`, `/despawnAllRoamers`, `/despawnRoamer <scp173|scp106>`, and `/roamerForceSpawn <scp173|scp106>`;
- Added optional Debug Tools displays showing each roamer's state, next check, and latest result.

## Survival

- Added a default-enabled **Disable Hunger System** module that hides the vanilla hunger bar and converts food nutrition directly into health;
- Replaced hunger-based natural healing with delayed regeneration: one health point every six seconds after 15 seconds without damage;
- Saturation now reduces the regeneration delay to five seconds and restores the normal four-second interval, while Hunger prevents natural regeneration;
- Made the custom health module hide both the vanilla heart display and armor bar while its replacement HUD is active;
- Added a default-enabled **Hide Active Effect Indicators** module that removes vanilla status-effect icons from the HUD while preserving inventory and SCP Conditions displays.

## Audio and presentation

- Reintroduced the world-entry sound and added a General & Modules option to disable it;
- Added a default-enabled **Save Game Sound** module that plays `save_game.ogg` whenever commands, beds, respawn anchors, or facility systems set a player's respawn point;
- Made SCP-1176's music and SCP-106's chase soundtrack immediately stop vanilla background music and keep it suppressed until the complete active soundtrack has finished;
- Added a default-enabled module that replaces vanilla player hurt sounds with human voices;
- Added a default-disabled module that removes vanilla attack, critical, and sweep impact sounds against non-player mobs;
- Added a default-disabled **Disable Vanilla Music** module that suppresses Minecraft's ambient soundtrack without blocking SCP Additions' contextual music.

## Facility signs

- Added the Core Room Sign and Door Sign;
- Added a Screwdriver editor for three compacting, reorderable entries, including per-entry and whole-sign copy/paste memory stored on the tool;
- Upgraded the SCP Sign with a Screwdriver editor for the SCP number, containment class, clearance level, anomaly type, and up to three anomaly-trait pictograms rendered behind its glass;
- Fine-tuned the SCP Sign typography and Anomaly Trait selector alignment against direct SCP Unity comparisons;
- Corrected the free-text editor field backgrounds without moving their aligned text baselines.

## Core Room elevator

- Added a modular, animated Core Room elevator based on SCP: Unity, with automatic floor discovery, a moving carriage, landing gates, procedural cables, and one-floor-at-a-time travel;
- Added Core Room Elevator Stations, a top Pulley, reusable Beam segments, and the Core Room Floor construction block;
- Aligned stations automatically generate model-matched shaft beams between floors and up to the pulley, while incomplete or obstructed layouts remain safely inactive;
- Completed elevator lines automatically create exactly one carriage at the lowest station and preserve one carriage per vertical line;
- Added precise multiblock collision shells for stations, pulleys, and beam segments, plus moving carriage collision that carries players, mobs, and items smoothly;
- Added compact contextual interaction targets to the separate up and down buttons inside the carriage and on each station.

## Facility construction

- Added SL1 Ceiling and SL1 Ceiling Alt construction blocks;
- Added an SL1 Ceiling Lamp that emits light while powered by redstone, with subtle positional startup, shutdown, and electrical-loop audio;
- Added an SL1 Flickering Ceiling Lamp with the same redstone control and irregular defective-light flickering;
- Added decorative Emergency Button, Fire Extinguisher, Wet Floor Sign, Non-potable Water Faucet, and SCP-914 Usage Notice facility props;
- Added clear tooltips to facility props that have no gameplay function;
- Standardized wall-mounted signs, props, door buttons, and keycard readers so they follow the clicked surface, require solid wall support, and break when that support is removed;
- Added dedicated inventory textures for enabled and disabled Tesla Gate Terminals, renamed the disabled variant, and made Screwdriver use switch between their visual states;
- Moved Vent directly after Trashbin in the facility creative tab and removed the obsolete Alarm Lamp block and its resources.

## Accessibility

- Colored module **ON** and **OFF** states green and red for faster visual scanning;
- Added a dedicated Accessibility screen to the Configuration Center, beginning with a Photosensitive Epilepsy section;
- Added **Reduce SCP-012 Visual Effects**, which disables the rapidly flashing interference and subliminal full-screen images during SCP-012 psychosis while preserving its gradual veil and smooth vignette.

## Bug Fixes

- Synchronized the SCP Inventory immediately on login, respawn, and dimension changes, and made Tesla Terminal authentication receive a server-authoritative Security Credentials snapshot when opened;
- Changed enabled Tesla Gates from ten-tick polling to continuous sensing without duplicate activation queues, and added swept trajectory checks so running entities cannot tunnel through the unchanged visible discharge arc between ticks;
- Assigned a blood type on first login for new and legacy players and preserved it across death instead of leaving Status as Unknown until the first respawn;
- Limited Tesla Gate damage to the visible, rotation-aware electrical arc while preserving the broader 3×3×3 volume only as its activation sensor;
- Rebuilt the Decontamination Checkpoint collision as an invisible model-matched shell, removing the obstruction through the chamber center and allowing any visible section to break the complete structure;
- Prevented Decontamination Checkpoints and Tesla Gates from changing animation states while being mined, so breaking progress no longer resets repeatedly;
- Moved the SCP-173 blink vignette behind the complete HUD while preserving the Hazmat Suit visor above view effects as the ordering rule for future equipped-item overlays.
- Fixed SCP-914 recipe selection when several compatible items are placed in the intake, preferring the transformation that uses the complete input instead of whichever matching recipe appears first;
- Fixed the Blink system remaining active after being disabled or appearing in Creative and Spectator modes, with the server now clearing stale blink states and enforcing the module setting.
- Fixed TV placement so it follows the clicked wall face instead of the player's viewing direction, and rebuilt its collision to match the visible model;
- Fixed Trashbin transparent sections clipping through its own faces by using alpha cutout rendering, expanded its collision to include the upper structure, and moved its front icon clear of the casing;
- Rendered SCP-012 with alpha cutout;
- Removed unintended shader reflections from the SL1 floor arrows and added dedicated handheld item textures for both arrow sizes;
- Removed duplicated Heater base geometry that reused the top emissive region;
- Separated the Decontamination Checkpoint's window trim from the wall planes to prevent z-fighting with optional renderers and shaders.

---
---
---

# SCP Additions 3.0.7 — Hotfix

## Multiplayer and configuration synchronization

- Made the host's gameplay configuration authoritative for connected clients, including item rules and effects, hidden Status effects, Codex definitions, contextual interactions, entity interaction rules, and SCP-173 target configuration;
- Added configuration synchronization on login and after supported save, delete, and reload operations;
- Cleared synchronized host snapshots when clients disconnect so single-player and later servers cannot inherit stale settings;
- Synchronized Survival `USABLE` and `WEAPON` tool sessions with the real selected hotbar slot, fixing mining and item-use behavior that could disagree between client and server.

## Gameplay fixes and refinements

- Corrected SCP-294 cups having missing-texture outputs;
- Corrected duplicated `Cup of Cup of` names on configurable SCP-294 drinks;
- Made item category changes update immediately after saving them through the in-game editor;
- Made context interactions ignore missing block and entity IDs instead of incorrectly assigning them to vanilla entries;
- Made SCP-173 immune to attacks dealing 6 damage or less while allowing stronger weapons to damage it;
- Prevented damaged SCP-173 instance from restoring all health when their chunk or world is loaded again;
- Corrected SCP-173 observer handling so players retain broad on-screen observation, configured generic mobs must face the statue directly, and SCP-131 uses its own intentional viewing threshold;
- Changed the SCP-572 advancement title to **The Chosen One**;
- Removed the bundled legacy context rule and ignored exact obsolete copies so old defaults do not reappear;
