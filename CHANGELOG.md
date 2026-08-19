# Changelog

# SCP Additions 4.0.0 — In Development

## Highlights
- SCP-106
- Reworked SCP-079 facility control
- Core Room Elevator
- Reworked survival systems
- Custom hotbar and oxygen HUD
- Expanded SCP Inventory interfaces
- Custom crosshair
- Custom death, saving, and multiplayer spectating
- Documents and expanded Codex
- Rebuilt contextual interactions
- Rebuilt Configuration Center
- Mod Integrations
- Reworked main menu, pause menu, and loading screens

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
- Prevented SCP-173 from scraping and shuffling against closed doors when no complete path to its target exists.

## SCP-131

- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally;
- SCP-131 instances now dismiss their player's follower group when they begin watching SCP-173, matching the manual dismiss action.

## SCP-079

- Replaced redstone-based facility access with a powered Facility Diagnostic Terminal and a Auxiliary Power Unit;
- SCP-079 now requires a physical computer in the world, begins hidden protocol discovery only after a powered diagnostic scan, learns faster from door use and Tesla activity, and gains AP regeneration only after completing access discovery;
- Added a processing-power system that limits how often SCP-079 can interfere with the facility and forces it to choose its actions more carefully;
- Added a strategic expenditure model that protects emergency reserves, tracks recent spending and repeated tactical lanes, permits brief high-power bursts, becomes increasingly conservative below 60% power, and reserves sub-30% expenditure for exceptional traps or critical device opportunities;
- SCP-079 now evaluates action utility against cost, remaining power, recent expenditure, repeated-action pressure, and the strategic importance of the requesting subsystem instead of spending whenever an action is merely affordable;
- SCP-079 now reacts differently depending on the threat chasing the player, using doors, temporary access denial, and nearby Tesla Gates when useful;
- Improved its SCP-012 trap behavior so repeated interference becomes increasingly difficult and less worthwhile;
- SCP-079 can now close and lock an open door to separate following SCP-131 instances when SCP-173 is waiting ahead;
- SCP-079 can spend processing power continuously to keep a useful door locked for longer, releasing it when the strategy is no longer useful or it cannot afford the upkeep;
- Added a positional hacking sound at each door, Tesla Gate, SCP-012 box, or other facility device successfully manipulated by SCP-079;
- Added optional Debug Tools displays for SCP-079's power, and its recent decisions.

## SCP-079 auxiliary isolation and SCiPNET reindexing

- Auxiliary generators require redstone, and contribute 0.1 AP/s each to cumulative SCP-079 regeneration;
- Updated terminal and generator tooltips for the new power, cache, and telemetry behavior.
- Auxiliary power isolation suspends SCP-079 actions and drains AP toward 25 without erasing learned facility access;
- Remote-session cache purge is now the sole operation that clears learned access and forces a five-minute SCiPNET index rebuild;
- Added gradual reconstruction telemetry, a compact technician-session warning, and a subtle unusual-network-activity advisory after SCP-079 gains access.

## Achievements

- Added **From the Trenches** for surviving an SCP-106 hunt;
- Added **Concrete and Rebar** for surviving an activated SCP-173 roamer encounter;
- Added the hidden **Eyes on me** achievement for witnessing SCP-131 stop to observe SCP-173;
- Added the hidden **What?** achievement for having SCP-714 prevent SCP-012 from taking hold;
- Reworked the Tesla Gate and SCP-330 achievements into non-lethal interaction goals, keeping the full achievement set obtainable in Hardcore runs;
- Reworked the custom Achievements panel to list the server's complete advancement catalog instead of inheriting vanilla visibility filtering, keeping every advancement category present even before its first completion;
- Unfinished advancements are now visually subdued, while unfinished hidden advancements are sorted to the end and shown only as **Hidden Achievement** placeholders with their rarity visible.

## Interface and presentation

- Added a default-enabled client-side **Custom Main Menu** preference that replaces Minecraft's title presentation with SCP Additions backgrounds, animated elements, What's New highlights, an Extras drawer for Realms and compatible mod-added buttons, and smooth screen transitions;
- Added a default-enabled client-side **Custom Pause Menu** preference with SCP Additions-styled navigation, Achievements, Statistics, Open to LAN, Settings, and mod access while preserving the corresponding vanilla actions and extensibility;
- Added a default-enabled client-side **Custom Loading Screen** preference that replaces vanilla spawn-region loading with the SCP Additions background and custom world-generation progress.
- Added a default-enabled **Custom Achievement Toasts** preference that replaces vanilla advancement popups with animated SCP Additions achievement cards;
- Replaced Forge's separate Mods screen in both custom title and pause menus with an animated native mod browser featuring fixed Off/A-Z/Z-A sorting controls, optional internal-component filtering, config-capability indicators, scrollable mod entries, styled metadata and descriptions, direct in-game config access when supported, and an anchored Open mods folder action;
- Added an SCP-themed Difficulty to the custom in-game Settings panel, mapping Safe/Euclid/Keter/Thaumiel to Easy/Normal/Hard/Peaceful with dedicated classification artwork and immediate world difficulty switching;
- Extended the custom **Open to LAN** panel to host controls injected into vanilla's LAN screen by compatible mods, preserving their callbacks and settings while keeping the SCP Additions presentation;
- SCP Inventory can now be closed with its configured open key, matching vanilla inventory-toggle behavior, and Crafting items can be dragged outside the interface to drop them into the world;
- Rebuilt the Configuration Center to match the new main-menu interface, with responsive two-column navigation, menu-style animated controls, preserved title-scene artwork, and smooth entry and exit transitions while retaining the existing configuration editors and validation logic;
- Added a default-enabled client-side **Disable Text Drop Shadows** preference that removes Minecraft's dark offset shadow pass;
- Added a default-enabled client-side **Hide Damage Indicator Particles** preference that suppresses the vanilla heart-like hit-feedback particles without affecting other particle effects;
- Added a default-enabled client-side **Facility Chat Interface** that keeps vanilla chat behavior while making it a stylized console;
- Reanchored dropped-item pickup prompts to the interpolated center of the actual ItemEntity instead of a floor/shadow offset, using the hand pickup prompt artwork's contact point as the visual hotspot so the icon and its labels follow moved or physics-adjusted drops together;
- Added an SCP Inventory-style interface for plain slot-only storage containers while the custom inventory is enabled, replacing supported chest, barrel, shulker, hopper, dispenser, and compatible modded storage screens with responsive Backpack/Keys/Codex and container lists;
- Added reversible **Enable/Disable** controls for compatible multi-entry Configuration Center lists; disabled entries remain visible but dimmed and can be restored without recreating them, including integrated and custom Roomba spawn floors, SCP-173 targets, hidden status effects, item rules, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes;
- Fixed Configuration Center presentation regressions from reversible entry toggles;
- Added an optional SCP: Unity-inspired custom crosshair, enabled by default, with independent in-game visibility settings;
- Added default-enabled modules that hide empty first-person hands and remove the vanilla experience bar, level indicator, experience-orb rendering, and XP pickup and level-up sounds;
- Added a default-enabled custom oxygen meter that replaces vanilla air bubbles, appears beneath the crosshair, shifts from light blue toward red as air runs out, adds a progressively stronger suffocation vignette, and darkens the screen further with each drowning-damage pulse until air recovery;
- Added a default-enabled custom hotbar, available only while SCP Inventory is enabled, that replaces the vanilla bar with a centered list of occupied slots, category labels, navy-and-gold selected-item styling, compact scrolling that skips empty slots, and one blank selection between the end and beginning of the list;
- Added the **Stow Held Item** control, bound to H by default, which returns an active Usable item or equipped Weapon to the SCP Inventory without dropping it;
- Added a default-enabled module that renders Action Bar messages in Roboto;
- Added native full-bright emissive overlays for  block textures without requiring an external emissive-texture mod, while retaining LabPBR material emission for compatible shader packs;
- Split configuration modules into personal presentation preferences and host-authoritative gameplay rules: each player can independently choose their HUD, crosshair, custom hotbar, voice profile, presentation audio, music suppression, and accessibility settings without operator permission, while mechanics that alter gameplay or the world remain controlled by the host;
- Added clear Configuration Center notices identifying per-player preferences and host-only settings, and kept server configuration editors locked for players without operator permission;
- Reorganized **General & Modules** into **Gameplay Features** and **Preferences** groups, and removed the obsolete SCP-173 behavior option from that screen without changing its underlying configuration or behavior.

## Death, saving, and spectating

- Added a default-enabled client-side **Custom Death Screen** inspired by SCP: Containment Breach and SCP: Unity, presenting designation, Safe/Euclid/Keter/Thaumiel difficulty, last save method, vanilla cause of death, animated red organic background treatment, progressive audio muffling, and SCP Additions-styled Load Game and Main Menu actions;
- Added a default-enabled server-side **Persistent Death Bodies** module that leaves an inert, interactable player body at the death location, using a short fake-ragdoll collapse and several low, randomized prone poses as a physical target for future SCP and recovery interactions;
- Persistent Death Bodies can now be searched through an integrated hand prompt; each corpse opens player-named storage containing the items left by that player, using the normal container presentation when SCP Inventory was disabled and the SCP Inventory storage presentation when it was enabled;
- Added **Quicksave**, bound to F6 by default and fully rebindable, which records the player's current position as their respawn point on Safe and Thaumiel difficulties;
- Reworked checkpoint availability around difficulty: Safe and Thaumiel allow Quicksave and Decontamination Checkpoint saves, Euclid allows Decontamination Checkpoints but not Quicksave, and Keter disables both while leaving vanilla, command, and compatible modded respawn-point methods available;
- Added a shared save pipeline that recognizes respawn points set through SCP Additions, vanilla, commands, and compatible mods, tracks the latest save method, plays the save cue, and shows an animated Roboto **Saving...** indicator with the rotating SCP Additions loading emblem;
- Added a short Load Game return transition with FOV and screen effects, accompanied by the world-entry cue, with an Accessibility option to disable the motion effect;
- Added multiplayer **Spectate** support to the custom Death Screen: when another player remains alive, dead players can open a third-person **Live Personnel Feed**, orbit the camera, switch between surviving players, and view CCTV-style scan noise and interference during feed changes;
- Live Personnel Feed target selection is server-authoritative and supports surviving players outside normal tracking distance or in other dimensions by streaming the selected player's region to the dead observer while keeping the custom Death Screen active.

## Mod Integrations

- Added a dedicated **Mod Integrations** section to the Configuration Center for optional behavior-level integrations with detected mods; unavailable integrations remain visible but disabled;
- Added **Simple Voice Chat** integration for multiplayer death and spectating: living players keep normal voice-chat behavior, while dead players share a non-positional dead-only call that cannot be heard by living players;
- Live Personnel Feed observers also receive the voice-chat audio heard by their selected surviving player, including that survivor's own microphone, while remaining isolated from ordinary proximity and group routing at the dead observer's server-side position;
- Added the first integration for **MineZero / Return by Death**, replacing its automatic death rewind with the SCP Additions death/spectate flow while using SCP Additions saves as MineZero checkpoints;
- MineZero-integrated multiplayer sessions keep dead players in the spectate flow while survivors remain; after a team wipe, dead players vote before the latest valid checkpoint rewinds the session;
- Extended MineZero checkpoint snapshots with SCP Additions player capabilities, SCP Inventory state, persistent facility/SCP-079 data, and tracked SCP Additions block and BlockEntity changes so the mod's custom state rewinds with the world;
- Added save-safety checks for MineZero-integrated sessions, preventing checkpoints while players are in active combat, dangerous damage/effect states, SCP-914 processing, SCP-330 hand-loss conditions, or an existing death session where saving could create a deterministic death loop;
- MineZero's own automatic and alternate checkpoint paths are suppressed while the integration is active so checkpoints remain synchronized with the SCP Additions save system.

## Codex and documents

- Added a dedicated Document item to the creative tab;
- Added an in-world document editor with **SCP Document**, **Facility Document**, and **Blank Document** templates, editable titles and Codex categories, three header labels and values, a Markdown body, an optional photograph, and an optional caption;
- Made new Config Center Codex definitions default to the dedicated Document item and unique-item matching while retaining support for selecting any registered item and for legacy full-page image documents.

## Survival

- Added a default-enabled module that hides the vanilla hunger bar and converts food nutrition directly into health;
- Added a default-enabled module that removes vanilla status-effect icons from the HUD while preserving inventory and SCP Conditions displays;
- Added a default-disabled module that prevents entity attacks and empty-air punches unless a Weapon is equipped, while leaving block mining available;
- Replaced hunger-based natural healing with delayed regeneration: one health point every six seconds after 15 seconds without damage;
- Saturation now reduces the regeneration delay to five seconds and restores the normal four-second interval, while Hunger prevents natural regeneration;
- Made the custom health module hide both the vanilla heart display and armor bar while its replacement HUD is active;

## Facility construction

- Added Roombas;
- Added a wall-mounted **Document Holder** that stores one Document item;
- Added a modular, animated Core Room elevator based on SCP: Unity, with automatic floor discovery, a moving carriage, landing gates, procedural cables, and one-floor-at-a-time travel;
- Added a construction preview for Core Room elevators: holding a Floor Station or Pulley displays a vertical green/red particle guide for valid range and obstruction checks, while structural beams are generated only after a valid Pulley closes the column and are removed when that connection becomes invalid;
- Added optional Screwdriver-configured arrival displays per elevator floor station, with animated sector and level announcements shown to passengers as the doors open;
- Added SL1 Ceiling and SL1 Ceiling Alt construction blocks;
- Added an SL1 Ceiling Lamp that emits light while powered by redstone, with subtle positional startup, shutdown, and electrical-loop audio;
- Added an SL1 Flickering Ceiling Lamp with the same redstone control and irregular defective-light flickering;
- Added decorative Emergency Button, Fire Extinguisher, Wet Floor Sign, Non-potable Water Faucet facility props, Tesla Gate Terminal Table, Archivist's Table, and Archivist's Chair;
- Added clear tooltips to facility props that have no gameplay function;
- Standardized wall-mounted signs, props, door buttons, and keycard readers so they follow the clicked surface, require solid wall support, and break when that support is removed;
- Added dedicated inventory textures for enabled and disabled Tesla Gate Terminals, renamed the disabled variant, and made Screwdriver use switch between their visual states;
- Moved Vent directly after Trashbin in the facility creative tab and removed the obsolete Alarm Lamp block and its resources;
- Updated props' item textures;
- Standardized Foundation facility blocks at an effective hardness of 37.5 and required an iron-tier or better pickaxe, making an unenchanted iron pickaxe break them in the same time an unenchanted diamond pickaxe breaks obsidian;
- Made Foundation structures, props, panels, readers, elevator components, and redstone-operated heavy doors immune to explosion destruction, while manual doors remain vulnerable.


## Facility signs

- Added the Facility Direction Sign and Door Sign, both editable with the Screwdriver;
- Upgraded the SCP Sign into a unified sign system whose first editor option selects between **SCP Information Sign**, **SCP-914 Usage Notice**, **Area Under Construction Sign**, and reusable custom world templates;
- Preserved the SCP Information editor for the SCP number, containment class, clearance level, anomaly type, and up to three anomaly-trait pictograms;
- Added template previews to the selector and support for naming, uploading, reusing, and deleting custom PNG sign templates stored within the server world;
- Custom artwork uses an 8:5 canvas, and automatically resizes other image proportions before upload;
- Reworked the shared sign frame so every template can be placed in left, centered, or right wall-relative positions according to the clicked third of the block.

## Creative inventory organization

- Standardized the creative-tab names as **SCP Additions - SCPs**, **SCP Additions - Items**, and **SCP Additions - Blocks**;
- Organized facility content under ten headers in this order: **Functional**, **Decoration**, **General**, **Core Room**, and **Zones**;
- Added **Alternative Metal Wall (Bottom)**, **Rest Area Corner Floor**, and **Kitchen Corner Floor** to **LCZ - Sublevel 1**;
- Added fixed-orientation automatic transitions from Rest Area Corner Floor to Metal Floor and from Blue Floor to Kitchen Corner Floor.

## Roamer spawning and developer tools

- Added natural spawn cycles for both SCP-173 and SCP-106, with separate `173spawn` and `106spawn` gamerules;
- SCP-106 begins checking for a possible encounter earlier than SCP-173, while both continue with recurring checks afterward;
- When one roamer is already active, the other becomes less likely to appear, but rare double encounters are still possible;
- Spawn timers stop while a matching roamer is active and restart after it dies or despawns;
- Added `/disableAllRoamers`, `/enableAllRoamers`, `/despawnAllRoamers`, `/despawnRoamer <scp173|scp106>`, and `/roamerForceSpawn <scp173|scp106>`;
- Added optional Debug Tools displays showing each roamer's state, next check, and latest result;
- Added sparse natural Roomba encounters on approved facility flooring;
- Added the `roombaSpawn` gamerule to enable or disable natural Roomba encounters;
- Renamed the Configuration Center's mixed inventory hub to **Items, Entities & Codex** and added a **Roomba Spawning** editor.

## Audio and presentation

- Added a default-enabled client-side **Custom Item Interaction Sounds** preference, active only for SCP Inventory actions: custom prompt pickups and equipment use randomized pickup cues, while CONSUMABLE rules can select **Food** or **Drink** feedback in the item-category editor; vanilla interaction paths remain untouched and disabling the preference restores vanilla local pickup/eat/drink feedback;
- Reintroduced and updated the world-entry sound and added a General & Modules option to disable it;
- Added a default-enabled module that plays a sound whenever commands, beds, respawn anchors, or facility systems set a player's respawn point;
- Added a default-enabled module that replaces vanilla player hurt sounds with human voices;
- Added two mutually exclusive **Voice Profile** choices with an in-menu voice test, using profile-matched hurt reactions and a gasp after recovering from severe oxygen loss;
- Added a drowning vocal that begins with suffocation damage and fades out smoothly as breathing recovers;
- Added a default-disabled module that removes vanilla attack, critical, and sweep impact sounds against non-player mobs;
- Added a default-enabled module that suppresses Minecraft's ambient soundtrack without blocking SCP Additions' contextual music;
- Added a default-enabled main menu soundtrack across the title screen and other menus until a world is opened.

## Contextual interactions

- Added item-specific contextual interactions and inherited alternate variants, allowing one block or entity to expose different actions according to the item held without duplicating its complete configuration;
- Added a state-aware integrated Document Holder prompt that shows only the hand icon, with no text, and appears only while the holder can currently accept, return, or close a document;
- Rebuilt the in-world **K** interaction editor with responsive **Prompt** and **Anchor** pages, default and variant navigation, duplication and removal controls, held-item selection, independent icons, item requirements, and per-variant anchors;
- Added the `config.png` contextual icon and native Screwdriver prompts for keycard readers, Tesla Gate Terminals, Core Room elevator floor stations, editable facility signs, SCP-131-A/B, and Roombas;
- Added Configuration Center badges that identify interactions with alternate variants and item-specific actions;
- Expanded the Contextual Interactions list into a merged catalog that visibly distinguishes **Integrated**, **Override**, and **Custom** rules, surfaces integrated defaults missing from older external configs, and shows each alternate interaction together with its required item.
- Added an **Off-screen prompts** option to the contextual-interaction editor, allowing selected block prompts to remain available at the edge of the screen while behind the player;
- Kept off-screen prompts disabled by default while enabling them in the bundled configuration for door buttons and keycard readers;
- Added a warning in the visual anchor editor when the selected block probably has no native right-click interaction.

## Configuration integration

- Added update-safe integrated defaults for SCP-914 recipes involving SCP Additions content, allowing new bundled transformations to appear in existing installations without resetting `914recipes.json` while keeping configured recipes and fragments authoritative;
- Applied the same layered-default system to contextual interactions, automatically exposing new bundled block and entity interactions to existing installations while preserving explicit overrides;
- Added explicit tombstone support through `"enabled": false`: matching SCP-914 recipe IDs and contextual interaction identities can suppress an integrated default without deleting or rewriting bundled data.

## Accessibility

- Colored module **ON** and **OFF** states green and red for faster visual scanning;
- Added a dedicated Accessibility screen to the Configuration Center, beginning with a Photosensitive Epilepsy section;
- Added **Reduce SCP-012 Visual Effects**, which disables the rapidly flashing interference and subliminal full-screen images during SCP-012 psychosis while preserving its gradual veil and smooth vignette;
- Added a **Motion Sickness** section with **Disable Load Transition**, allowing players to remove the animated FOV and screen effect used when returning from a saved death state.

## Bug Fixes

- Synchronized the SCP Inventory immediately on login, respawn, and dimension changes, and made Tesla Terminal authentication receive a server-authoritative Security Credentials snapshot when opened;
- Changed enabled Tesla Gates from ten-tick polling to continuous sensing without duplicate activation queues, and added swept trajectory checks so running entities cannot tunnel through the unchanged visible discharge arc between ticks;
- Assigned a blood type on first login for new and legacy players and preserved it across death instead of leaving Status as Unknown until the first respawn;
- Limited Tesla Gate damage to the electrical arc while preserving the broader 3×3×3 volume only as its activation sensor;
- Rebuilt the Decontamination Checkpoint collision as an invisible model-matched shell, removing the obstruction through the chamber center and allowing any visible section to break the complete structure;
- Prevented Decontamination Checkpoints and Tesla Gates from changing animation states while being mined, so breaking progress no longer resets repeatedly;
- Moved the SCP-173 blink vignette behind the complete HUD while preserving the Hazmat Suit visor above view effects as the ordering rule for future equipped-item overlays;
- Fixed SCP-914 recipe selection when several compatible items are placed in the intake, preferring the transformation that uses the complete input instead of whichever matching recipe appears first;
- Fixed the Blink system remaining active after being disabled or appearing in Creative and Spectator modes, with the server now clearing stale blink states and enforcing the module setting;
- Fixed TV placement so it follows the clicked wall face instead of the player's viewing direction, and rebuilt its collision to match the visible model;
- Fixed Trashbin transparent sections clipping through its own faces by using alpha cutout rendering, expanded its collision to include the upper structure, and moved its front icon clear of the casing;
- Removed unintended shader reflections from the SL1 floor arrows and added dedicated handheld item textures for both arrow sizes;
- Removed duplicated Heater base geometry that reused the top emissive region;
- Separated the Decontamination Checkpoint's window trim from the wall planes to prevent z-fighting with shaders.