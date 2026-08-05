# Changelog

# SCP Additions 3.1.0 — In Development

## Interface and presentation

- Added an optional SCP: Unity-inspired custom crosshair, enabled by default, with independent in-game visibility, RGB and alpha controls, a live preview, default reset, cancel, and save-and-reload controls;
- Added default-enabled modules that hide empty first-person hands and remove the vanilla experience bar, level indicator, experience-orb rendering, and XP pickup and level-up sounds;
- Added a default-enabled custom oxygen meter that replaces vanilla air bubbles, appears beneath the crosshair, shifts from light blue toward red as air runs out, adds a progressively stronger suffocation vignette, and darkens the screen further with each drowning-damage pulse until air recovery;
- Added a default-enabled custom hotbar, available only while SCP Inventory is enabled, that replaces the vanilla bar with a centered list of occupied slots, category labels, navy-and-gold selected-item styling, compact scrolling that skips empty slots, and one blank selection between the end and beginning of the list;
- Added the **Stow Held Item** control, bound to H by default, which returns an active Usable item or equipped Weapon to the SCP Inventory without dropping it;
- Added a default-enabled module that renders Action Bar messages in Roboto;
- Added native full-bright emissive overlays for authored block textures without requiring an external emissive-texture mod, while retaining LabPBR material emission for compatible shader packs;
- Split configuration modules into personal presentation preferences and host-authoritative gameplay rules: each player can independently choose their HUD, crosshair, custom hotbar, voice profile, presentation audio, music suppression, and accessibility settings without operator permission, while mechanics that alter gameplay or the world remain controlled by the host;
- Added clear Configuration Center notices identifying per-player preferences and host-only settings, and kept server configuration editors locked for players without operator permission;
- Reorganized **General & Modules** into **Gameplay Features** and **Preferences** groups, and removed the obsolete SCP-173 behavior option from that screen without changing its underlying configuration or behavior.

## Creative inventory organization

- Standardized the creative-tab names as **SCP Additions - SCPs**, **SCP Additions - Items**, and **SCP Additions - Blocks**;
- Organized facility content under ten headers in this order: **Functional**, **Decoration**, **General**, **Core Room**, and **Zones**;
- Added **Alternative Metal Wall (Bottom)**, **Rest Area Corner Floor**, and **Kitchen Corner Floor** to **LCZ - Sublevel 1**;
- Added fixed-orientation automatic transitions from Rest Area Corner Floor to Metal Floor and from Blue Floor to Kitchen Corner Floor, using the original Blue/Metal floor corner, edge, inner-corner, and full-connection mapping.

## Facility durability

- Standardized Foundation facility blocks at an effective hardness of 37.5 and required an iron-tier or better pickaxe, making an unenchanted iron pickaxe break them in the same time an unenchanted diamond pickaxe breaks obsidian;
- Made Foundation structures, props, panels, readers, elevator components, and redstone-operated heavy doors immune to explosion destruction, while manual doors remain vulnerable.

## Codex and documents

- Added a dedicated Document item to the creative tab;
- Added an in-world document editor with **SCP Document**, **Facility Document**, and **Blank Document** templates, editable titles and Codex categories, three header labels and values, a Markdown body, an optional photograph, and an optional caption;
- Made new Config Center Codex definitions default to the dedicated Document item and unique-item matching while retaining support for selecting any registered item and for legacy full-page image documents.

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

## SCP-330

- Rebuilt SCP-330 as a GeckoLib block using a new bowl and candy model based on Unity's design;
- Reduced its candy selection to blue, pink, and yellow, removed the former potion buffs, and increased nutrition and saturation while adding a small direct heal;
- Reworked the two-candy limit into a persistent hand-loss state: after taking a third candy, players cannot open inventories, use items, break or interact with blocks, activate buttons, interact with entities, or use contextual interactions until death;
- Removed the obsolete red and green candies, legacy candy procedures, textures, and model files.

## SCP-173

- Reduced SCP-173's rendered height to approximately two blocks;
- Updated and Reworked SCP-173's movement audio;
- Added a synchronized two-layer encounter score triggered by SCP-173 reveal scares: the first layer dominates during direct danger, the second paranoia layer crossfades in as the Blink HUD disappears, and the score lingers after the statue leaves before fading out;
- Prevented SCP-173 from scraping and shuffling against closed doors when no complete path to its target exists; it now waits silently and resumes as soon as a route becomes available.

## SCP-131

- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally;
- SCP-131 instances now dismiss their player's follower group when they begin watching SCP-173, matching the manual dismiss action.

## SCP-079

- Replaced redstone-based facility access with a powered Facility Diagnostic Terminal and a placeholder Auxiliary Power Unit;
- Restyled the Facility Diagnostic Terminal as a fixed 4:3 ARC-Site-48 SCiPNET v3.2.6 CRT interface using the complete site crest, Montserrat headings, Anonymous Pro telemetry, matching online and offline diagnostic sections, and a five-minute remote-session cache lockout that prevents SCP-079 from restarting protocol discovery while active; corrected uncontained SCP signatures to represent enabled SCP profiles released from containment rather than only currently spawned entities;
- SCP-079 now requires a physical computer in the world, begins hidden protocol discovery only after a powered diagnostic scan, learns faster from door use and Tesla activity, and gains AP regeneration only after completing access discovery;
- Cutting auxiliary power now erases SCP-079's learned access and disables the diagnostic and Tesla buses, forcing the AI to investigate the system again after restoration;
- Expanded the SCP-079 developer HUD with a discovery-progress bar and moved the hidden **Not Your Decision** advancement to the first facility decision that actually affects a player;
- Matched the SCiPNET terminal ambient loop and UI feedback to the Tesla Gate Terminal, and added a positional powered `auxgen` loop to the Auxiliary Power Unit at a conservative initial volume;

- Added a processing-power system that limits how often SCP-079 can interfere with the facility and forces it to choose its actions more carefully;
- Added a strategic expenditure model that protects emergency reserves, tracks recent spending and repeated tactical lanes, permits brief high-power bursts, becomes increasingly conservative below 60% power, and reserves sub-30% expenditure for exceptional traps or critical device opportunities;
- SCP-079 now evaluates action utility against cost, remaining power, recent expenditure, repeated-action pressure, and the strategic importance of the requesting subsystem instead of spending whenever an action is merely affordable;
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
- Added a sixty-second no-access timeout for routine SCP-173 encounters trapped by sealed layouts, while preserving manually placed statues;
- Added `/disableAllRoamers`, `/enableAllRoamers`, `/despawnAllRoamers`, `/despawnRoamer <scp173|scp106>`, and `/roamerForceSpawn <scp173|scp106>`;
- Added optional Debug Tools displays showing each roamer's state, next check, and latest result;
- Added sparse natural Roomba encounters on approved facility flooring;
- Added the `roombaSpawn` gamerule to enable or disable natural Roomba encounters;
- Renamed the Configuration Center's mixed inventory hub to **Items, Entities & Codex** and added a **Roomba Spawning** editor.

## Survival

- Added a default-enabled module that hides the vanilla hunger bar and converts food nutrition directly into health;
- Added a default-enabled module that removes vanilla status-effect icons from the HUD while preserving inventory and SCP Conditions displays;
- Added a default-disabled module that prevents entity attacks and empty-air punches unless a Weapon is equipped, while leaving block mining available;
- Replaced hunger-based natural healing with delayed regeneration: one health point every six seconds after 15 seconds without damage;
- Saturation now reduces the regeneration delay to five seconds and restores the normal four-second interval, while Hunger prevents natural regeneration;
- Made the custom health module hide both the vanilla heart display and armor bar while its replacement HUD is active;

## Audio and presentation

- Reintroduced and updated the world-entry sound and added a General & Modules option to disable it;
- Added a default-enabled module that plays a sound whenever commands, beds, respawn anchors, or facility systems set a player's respawn point;
- Added a default-enabled module that replaces vanilla player hurt sounds with human voices;
- Added two mutually exclusive **Voice Profile** choices with an in-menu voice test, using profile-matched hurt reactions and a gasp after recovering from severe oxygen loss;
- Added a shared looping drowning vocal that begins with suffocation damage and fades out smoothly as breathing recovers, independently of the selected voice profile;
- Added a default-disabled module that removes vanilla attack, critical, and sweep impact sounds against non-player mobs;
- Added a default-disabled module that suppresses Minecraft's ambient soundtrack without blocking SCP Additions' contextual music.

## Facility signs

- Added the Facility Direction Sign and Door Sign, both editable with the Screwdriver;
- Upgraded the SCP Sign into a unified glass-backed sign system whose first editor option selects between **SCP Information Sign**, **SCP-914 Usage Notice**, **Area Under Construction Sign**, and reusable custom world templates;
- Preserved the SCP Information editor for the SCP number, containment class, clearance level, anomaly type, and up to three anomaly-trait pictograms;
- Added template previews to the selector and support for naming, uploading, reusing, and deleting custom PNG sign templates stored with the server world;
- Custom artwork uses an 8:5 canvas, recommends 1024×640 images, and automatically resizes other image proportions before upload;
- Retired the separate SCP-914 Usage Notice and Area Under Construction Sign creative items while retaining compatibility registrations for existing worlds;
- Reworked the shared sign frame so every template can be placed in left, centered, or right wall-relative positions according to the clicked third of the block.

## Facility construction

- Added Roombas;
- Refined Roomba obstacle turns with a short contact pause, gradual mechanical acceleration and braking, and a brief settling pause before forward movement resumes;
- Added a modular, animated Core Room elevator based on SCP: Unity, with automatic floor discovery, a moving carriage, landing gates, procedural cables, and one-floor-at-a-time travel; Also, if clicked with a Screwdriver in the Elevator Station, a sector and level can be configured to be displayed while arriving in the destination, e.g. "Light Containment Zone - Sublevel 01" just like in SCP Unity;
- Added optional Screwdriver-configured arrival displays per elevator floor station, with animated sector and level announcements shown to passengers as the doors open;
- Added SL1 Ceiling and SL1 Ceiling Alt construction blocks;
- Added an SL1 Ceiling Lamp that emits light while powered by redstone, with subtle positional startup, shutdown, and electrical-loop audio;
- Added an SL1 Flickering Ceiling Lamp with the same redstone control and irregular defective-light flickering;
- Added decorative Emergency Button, Fire Extinguisher, Wet Floor Sign, and Non-potable Water Faucet facility props;
- Added clear tooltips to facility props that have no gameplay function;
- Standardized wall-mounted signs, props, door buttons, and keycard readers so they follow the clicked surface, require solid wall support, and break when that support is removed;
- Added dedicated inventory textures for enabled and disabled Tesla Gate Terminals, renamed the disabled variant, and made Screwdriver use switch between their visual states;
- Moved Vent directly after Trashbin in the facility creative tab and removed the obsolete Alarm Lamp block and its resources;
- Updated props' item textures.

## Contextual interactions

- Added an **Off-screen prompts** option to the contextual-interaction editor, allowing selected block prompts to remain available at the edge of the screen while behind the player;
- Kept off-screen prompts disabled by default while enabling them in the bundled configuration for door buttons and keycard readers;
- Added a warning in the visual anchor editor when the selected block probably has no native right-click interaction.

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
- Moved the SCP-173 blink vignette behind the complete HUD while preserving the Hazmat Suit visor above view effects as the ordering rule for future equipped-item overlays;
- Replaced the Floor Station's unreliable automatic emissive lookup with an explicit full-bright GeckoLib render pass for its authored button glowmask;
- Fixed SCP-914 recipe selection when several compatible items are placed in the intake, preferring the transformation that uses the complete input instead of whichever matching recipe appears first;
- Fixed the Blink system remaining active after being disabled or appearing in Creative and Spectator modes, with the server now clearing stale blink states and enforcing the module setting;
- Fixed TV placement so it follows the clicked wall face instead of the player's viewing direction, and rebuilt its collision to match the visible model;
- Fixed Trashbin transparent sections clipping through its own faces by using alpha cutout rendering, expanded its collision to include the upper structure, and moved its front icon clear of the casing;
- Rendered SCP-012 with alpha cutout;
- Removed unintended shader reflections from the SL1 floor arrows and added dedicated handheld item textures for both arrow sizes;
- Removed duplicated Heater base geometry that reused the top emissive region;
- Separated the Decontamination Checkpoint's window trim from the wall planes to prevent z-fighting with shaders.

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
