# Changelog

# SCP: Classified Directive 4.0.0 — In Development

> [!WARNING]
> **World compatibility notice:** Worlds created with SCP Additions before the 4.0.0 rebrand are **not block-compatible** with SCP: Classified Directive. Because the registry namespace changed, blocks placed by older versions will not be preserved when those worlds are loaded under 4.0.0 and may be removed from the world. **Back up old worlds before updating.** Configuration, player-capability, and SavedData migrations remain supported where explicitly described below, but placed legacy blocks should be treated as non-migrating content.

## Highlights
- SCP: Classified Directive rebrand (formerly SCP Additions)
- SCP-106
- SCP-939
- SCP-1576
- Reworked SCP-079 facility control
- Core Room Elevator
- Reworked survival systems
- Custom hotbar and oxygen HUD
- Expanded SCP Inventory interfaces
- Creative Safe Zones
- Custom crosshair
- Custom death, saving, and multiplayer spectating
- Documents and expanded Codex
- Rebuilt contextual interactions
- Rebuilt Configuration Center
- Mod Integrations
- Reworked main menu, pause menu, and loading screens

## SCP: Classified Directive rebrand

- Rebranded the project as **SCP: Classified Directive** (formerly **SCP Additions**);
- Migrated the public mod ID and registry namespace from the legacy ID to `scp_classified_directive`;
- Consolidated the former resource namespaces into `scp_classified_directive`;
- Added Forge missing-mapping handling for legacy registry identifiers where Forge exposes them during migration; placed blocks from pre-4.0 worlds are not block-compatible across the namespace change and are not guaranteed to survive loading;
- Added serialized capability-key migration so existing SCP Inventory contents and legacy player variables survive the namespace change;
- Legacy configuration files now migrate embedded SCP resource identifiers to the unified namespace while preserving user customizations;
- Existing world SavedData storage keys remain recognized internally so SCP-294 and SCP-914 state survives the rebrand;
- Unified legacy configuration directories under `config/scp_classified_directive` without overwriting migrated files;
- Refactored Java packages, mod metadata, mixin identifiers, build output naming, embedded resource-pack naming, and project-facing text around the new identity.

## SCP-106

- Added SCP-106 as a new roaming threat;
- SCP-106 can appear naturally after the player has spent some time in the world, emerging from the ground or nearby walls before immediately beginning a hunt;
- Added close-range attacks that deal damage and apply Wither;
- Added a ranged attack that throws a straight trail of corrosion across the floor when the player creates distance but remains in clear view, dealing damage, Wither, and Slowness on a direct hit;
- Corrosion left by SCP-106 continuously slows Survival players who walk over it;
- SCP-106 can phase through solid blocks, moving more slowly while inside them and leaving temporary portals on the surfaces it enters and exits;
- If the player creates too much distance, SCP-106 can disappear and re-emerge ahead of the player's path;
- Tesla Gates repel SCP-106, forcing it to sink away and suppressing the next three natural spawn checks on Safe and the next two on Euclid/Keter; Thaumiel runs no natural roamer checks;
- Hunts can end quickly or continue for several minutes depending on how long SCP-106 remains interested in the target; players can create distance, but cannot lose SCP-106 before the hunt ends.

## SCP-939

- Added SCP-939 as a new roaming threat;
- SCP-939 is functionally blind and does not use ordinary visual target tracking for long-range pursuit; instead, it follows a shared acoustic-stimulus system that records sound evidence and drives `IDLE`, `HEARD_SOUND`, `INVESTIGATE`, `SEARCH`, `CONFIRMED_HUNT`, and `LOST_SEARCH` awareness states;
- Player movement, sprinting, jumping and landing, facility doors and buttons, block and interaction noises, breathing, forced gasps, and compatible voice-chat speech can create acoustic evidence with different intensity and range;
- SCP-939 can investigate the last known location of a sound, search around stale evidence, accelerate into a committed hunt when evidence becomes strong enough, and return to wandering when it loses the trail;
- Added a close-range bite attack and a committed pounce attack that physically launches SCP-939 toward prey;
- Successful SCP-939 attacks have a one-in-three chance to inflict **Bleeding**, turning otherwise survivable injuries into persistent blood loss until the victim is healed;
- Successful pounces knock the victim prone and pin them beneath SCP-939;
- Added an interactive struggle QTE while pinned, using the player's rebound left/right movement keys rather than hard-coded controls; successful inputs can kick SCP-939 away, while repeated failures allow the mauling attack to continue; its reaction window is more generous on Safe/Thaumiel, uses the baseline timing on Euclid, and is tighter on Keter;
- Other players can interrupt a pin by attacking SCP-939, immediately freeing the restrained victim;
- Added a dedicated breath reserve which becomes relevant only at immediate pass-by distance, entering at approximately 2.5 blocks with unobstructed line of sight and clearing around 2.75 blocks to prevent HUD flicker;
- Added a rebindable **Hold Breath** control and HUD meter; holding breath suppresses normal breathing acoustic stimuli until the reserve is exhausted, at which point the player is forced to gasp loudly and temporarily cannot continue holding their breath;
- Reduced ordinary breathing evidence to match the close-range Hold Breath encounter window while keeping the exhausted gasp intentionally loud and dangerous;
- Added optional Simple Voice Chat mimicry: players can allow SCP-939 to retain short voice fragments temporarily in memory and replay them from SCP-939's position; fragments expire automatically and are cleared when consent is revoked or the player disconnects;
- Added `/scp939 mimicry allow`, `/scp939 mimicry deny`, and `/scp939 mimicry status` session controls.

## SCP-1576

- Added SCP-1576 as a new Usable object;
- Holding the normal use control winds SCP-1576 for four seconds, while releasing the control early immediately cancels the winding attempt;
- Sneaking and using SCP-1576 on a supported surface places the object on the ground;
- Completing the wind-up starts 30 seconds of communication with dead players, then places that SCP-1576 instance on a two-minute cooldown;
- With Simple Voice Chat available, dead players speaking through the multiplayer death/spectating call are relayed positionally from the active SCP-1576 so nearby living players can hear them;
- Active SCP-1576 sessions emit acoustic evidence from the communicator's physical position, allowing hearing-based systems such as SCP-939 to react to the voices it reproduces.

## Anomalous Items

- Added Item #006, Vol. I as the first Log of Anomalous Items entry;
- Item #006's **Glowing Rock** now emits a soft light when placed.

## SCP-914

- Completely rebuilt SCP-914 as a single large machine;
- Reworked the configuration dial into a physical contextual control that can be held and dragged directly on the placed model, with smooth client motion, and snap-to-setting release behavior;
- Reworked the winding key into a physical contextual **Start** control anchored to the key;
- Rebuilt the refining cycle around the new model animation, and the existing configurable SCP-914 transformation recipes;
- Removed the obsolete SCP-914 assembly kit, component blocks, component items, GUI, generated procedures, models, textures, and contextual-interaction definitions.

## SCP-330

- Rebuilt SCP-330 using a new bowl and candy model based on Unity's design;
- Reduced its candy selection to blue, pink, and yellow, removed the former potion buffs, and increased nutrition and saturation while adding a small direct heal;
- Reworked the two-candy limit into a persistent hand-loss state: after taking a third candy, players cannot open inventories, use items, break or interact with blocks, activate buttons, interact with entities, or use contextual interactions until death;
- Removed the obsolete red and green candies, legacy candy procedures, textures, and model files.

## SCP-572

- Reworked SCP-572 as a physically poor weapon that deals only 2 damage at full charge and attacks slowly, replacing the former self-damage-on-swing behavior;
- SCP-572 now falsifies its apparent strength to the holder: its client tooltip reports **25 Attack Damage** and **4 Attack Speed**, and the local attack cooldown follows that apparent speed while the server keeps the real weak and slow values;
- While SCP-572 is held, both the vanilla heart display and the custom health HUD remain visually full, local damage flashes and hurt-camera feedback are suppressed, and the holder's vanilla or configured hurt voice is muted without hiding the real damage from the server or other players;
- SCP-572 is now non-stackable, has no durability, and survives fire and lava as an item entity.

## SCP-714

- Reworked SCP-714's stamina drawback so the ring no longer immediately empties or disables stamina; sprinting begins at normal stamina cost and progressively becomes more exhausting with exposure, reaching **2.5×** stamina drain at the most advanced pre-coma stage;
- SCP-714 can now be placed on a supported floor with **Sneak + Use**.

## SCP-173

- Reduced SCP-173's rendered height to approximately two blocks;
- Updated and Reworked SCP-173's movement audio;
- Added a synchronized two-layer encounter score triggered by SCP-173 reveal scares: the first layer dominates during direct danger, the second paranoia layer crossfades in as the Blink HUD disappears, and the score lingers after the statue leaves before fading out;
- Prevented SCP-173 from scraping and shuffling against closed doors when no complete path to its target exists.

## SCP-131

- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally;
- SCP-131 instances now dismiss their player's follower group when they begin watching SCP-173, matching the manual dismiss action.
- The first time a player makes an SCP-131 follow in a world, `scp131found.ogg` now plays once for every player currently connected to that world save.

## Legacy remakes

- Remade several older SCP models that no longer matched the newer content, including SCP-426, SCP-902-A, SCP-914, and SCP-1176;
- Remade Keycard textures.

## Stealth and movement

- Added a default-enabled server-side **Advanced Crouch & Stealth** module that replaces instant first-person crouch height changes with smooth lowering and rising transitions while retaining Minecraft's canonical server-authoritative collision poses;
- Crouching players now automatically enter Minecraft's low crawling pose when moving into spaces too short for the normal crouch hitbox, then transition back through crouching and standing as clearance becomes available;
- Added a reusable visual-perception framework for hostile mobs: standing, crouching, and crawling expose configurable visibility values, while local light, distance, line of sight, sustained exposure, invisibility, and per-entity multipliers affect whether a player is acquired as a target;
- Added entity-specific perception traits for **Omniscient**, **Blind**, and **Night Vision** behavior, with integrated defaults that leave SCP-106 unaffected by hiding, keep SCP-939's acoustic system authoritative instead of granting it visual detection, make hiding from SCP-173 less forgiving than hiding from ordinary mobs, and let spiders ignore darkness penalties;
- Added public visibility overrides and modifier hooks so future hiding spots, smoke, equipment, and anomalous invisibility can participate in perception without replacing mob AI;
- Added a dedicated **Stealth & Perception** Configuration Center editor for posture visibility, darkness behavior, close-range detection, acquisition delay, and addable per-entity sensory rules and multipliers.

## SCP-079

- Replaced redstone-based facility access with a powered Facility Diagnostic Terminal and a Auxiliary Power Unit;
- SCP-079 now requires a physical computer in the world, begins hidden protocol discovery only after a powered diagnostic scan, learns faster from door use and Tesla activity, and gains AP regeneration only after completing access discovery;
- Added a processing-power system that limits how often SCP-079 can interfere with the facility and forces it to choose its actions more carefully;
- Added difficulty-scaled processing costs: SCP-079 actions consume more AP on Thaumiel and Safe, use their authored baseline cost on Euclid, and consume less AP on Keter;
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
- Added **Afterlife Communicator** for players who use SCP-1576 to speak with dead players.
- Added the hidden **Eyes on me** achievement for witnessing SCP-131 stop to observe SCP-173;
- Added the hidden **What?** achievement for having SCP-714 prevent SCP-012 from taking hold;
- Reworked the Tesla Gate and SCP-330 achievements into non-lethal interaction goals, keeping the full achievement set obtainable in Hardcore runs;
- Reworked the custom Achievements panel to list the server's complete advancement catalog instead of inheriting vanilla visibility filtering, keeping every advancement category present even before its first completion;
- Unfinished advancements are now visually subdued, while unfinished hidden advancements are sorted to the end and shown only as **Hidden Achievement** placeholders with their rarity visible.

## Interface and presentation

- Added a default-enabled client-side **Custom Main Menu** preference that replaces Minecraft's title presentation with SCP: Classified Directive backgrounds, animated elements, What's New highlights, an Extras drawer for Realms and compatible mod-added buttons, and smooth screen transitions;
- Added a default-enabled client-side **Custom Pause Menu** preference with SCP: Classified Directive-styled navigation, Achievements, Statistics, Open to LAN, Settings, and mod access while preserving the corresponding vanilla actions and extensibility;
- Added a default-enabled client-side **Custom Loading Screen** preference that replaces vanilla spawn-region loading with the SCP: Classified Directive background and custom world-generation progress.
- Rebuilt the **New Game** flow opened from the custom Main Menu with the same animated presentation language while retaining Minecraft's real `CreateWorldScreen` and `WorldCreationUiState` as the authoritative creation backend;
- Added responsive **GAME**, **WORLD**, and **MORE** creation cards for world naming, Survival/Creative/Apollyon selection, Thaumiel/Safe/Euclid/Keter difficulty presentation, cheats, registered world presets, customization, seed, structures, bonus chest, and the reserved **ARC-Site 48 (Coming Soon)** preset;
- Added a registry-driven custom Game Rules editor, a styled Experiments flow that preserves vanilla data-pack state handling, the normal Data Packs workflow, and a **MOD OPTIONS** area that rehosts compatible controls injected into world creation by other mods while preserving their original callbacks;
- Restyled **Add Server**, **Edit Server**, and **Direct Connection** opened from the custom multiplayer panel with responsive SCP: Classified Directive panels while retaining Minecraft's native validation, resource-pack controls, callbacks, and compatible injected widgets;
- Added a default-enabled **Custom Achievement Toasts** preference that replaces vanilla advancement popups with animated SCP: Classified Directive achievement cards;
- Replaced Forge's separate Mods screen in both custom title and pause menus with an animated native mod browser featuring fixed Off/A-Z/Z-A sorting controls, optional internal-component filtering, config-capability indicators, scrollable mod entries, styled metadata and descriptions, direct in-game config access when supported, and an anchored Open mods folder action;
- Added an SCP-themed Difficulty to the custom in-game Settings panel, mapping Safe/Euclid/Keter/Thaumiel to Easy/Normal/Hard/Peaceful with dedicated classification artwork and immediate world difficulty switching;
- Extended the custom **Open to LAN** panel to host controls injected into vanilla's LAN screen by compatible mods, preserving their callbacks and settings while keeping the SCP: Classified Directive presentation;
- Reworked SCP Inventory controls around Minecraft's existing **Inventory** binding as a QoL change: while the module is enabled, the player's normal Inventory key or mouse binding opens and closes SCP Inventory in Survival and Adventure, and opens SCP Inventory first in Creative with a dedicated **Creative Inventory** button leading to the vanilla creative interface; removed the former dedicated **Tab** inventory keybind;
- Removed **E** as a separate Contextual Interactions input option; contextual actions now use Minecraft's normal **Use / Place** binding, legacy `allowE` configurations migrate to that input automatically, and physical controls that deliberately have no generic click remain unaffected;
- Added **Hold Item** to equipment context menus in SCP Inventory, allowing equipment and accessories to be moved temporarily into the active hand without changing their equipment category or Quick Equip behavior;
- Crafting items can now be dragged outside the SCP Inventory interface to drop them into the world;
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
- Added the **Stow Held Item** control, bound to H by default, which returns active Usable, Placeable, or temporarily held Equipment items to SCP Inventory and still unequips an equipped Weapon when appropriate;
- Added a default-enabled module that renders Action Bar messages in Roboto;
- Added native full-bright emissive overlays for  block textures without requiring an external emissive-texture mod, while retaining LabPBR material emission for compatible shader packs;
- Split configuration modules into personal presentation preferences and host-authoritative gameplay rules: each player can independently choose their HUD, crosshair, custom hotbar, voice profile, presentation audio, music suppression, and accessibility settings without operator permission, while mechanics that alter gameplay or the world remain controlled by the host;
- Added clear Configuration Center notices identifying per-player preferences and host-only settings, and kept server configuration editors locked for players without operator permission;
- Reorganized **General & Modules** into **Gameplay Features** and **Preferences** groups, and removed the obsolete SCP-173 behavior option from that screen without changing its underlying configuration or behavior.

## Death, saving, and spectating

- Added a default-enabled client-side **Custom Death Screen** inspired by SCP: Containment Breach and SCP: Unity, presenting designation, Safe/Euclid/Keter/Thaumiel difficulty, last save method, vanilla cause of death, animated red organic background treatment, progressive audio muffling, and SCP: Classified Directive-styled Load Game and Main Menu actions;
- Added a default-enabled server-side **Persistent Death Bodies** module that leaves an inert, interactable player body at the death location, using a short fake-ragdoll collapse and several low, randomized prone poses as a physical target for future SCP and recovery interactions;
- Persistent Death Bodies can now be searched through an integrated hand prompt; each corpse opens player-named storage containing the items left by that player, using the normal container presentation when SCP Inventory was disabled and the SCP Inventory storage presentation when it was enabled;
- Added **Quicksave**, bound to F6 by default and fully rebindable, which records the player's current position as their respawn point on Safe and Thaumiel difficulties;
- Reworked checkpoint availability around difficulty: Safe and Thaumiel allow Quicksave and Decontamination Checkpoint saves, Euclid allows Decontamination Checkpoints but not Quicksave, Keter disables both while leaving vanilla, command, and compatible modded respawn-point methods available, and Apollyon disables all respawn-point saving from SCP: Classified Directive, vanilla, commands, and compatible mods as part of its permanent-death rules;
- Added a shared save pipeline that recognizes respawn points set through SCP: Classified Directive, vanilla, commands, and compatible mods, tracks the latest save method, plays the save cue, and shows an animated Roboto **Saving...** indicator with the rotating SCP: Classified Directive loading emblem;
- Added a short Load Game return transition with FOV and screen effects, accompanied by the world-entry cue, with an Accessibility option to disable the motion effect;
- Added multiplayer **Spectate** support to the custom Death Screen: when another player remains alive, dead players can open a third-person **Live Personnel Feed**, orbit the camera, switch between surviving players, and view CCTV-style scan noise and interference during feed changes;
- Live Personnel Feed target selection is server-authoritative and supports surviving players outside normal tracking distance or in other dimensions by streaming the selected player's region to the dead observer while keeping the custom Death Screen active.

## Mod Integrations

- Added a dedicated **Mod Integrations** section to the Configuration Center for optional behavior-level integrations with detected mods; unavailable integrations remain visible but disabled;
- Added **Simple Voice Chat** integration for multiplayer death and spectating: living players keep normal voice-chat behavior, while dead players share a non-positional dead-only call that cannot be heard by living players;
- Live Personnel Feed observers also receive the voice-chat audio heard by their selected surviving player, including that survivor's own microphone, while remaining isolated from ordinary proximity and group routing at the dead observer's server-side position;
- Extended the Simple Voice Chat integration to SCP-939: living speech now produces acoustic evidence for its hearing system, while explicit session consent can additionally provide temporary in-memory voice fragments for SCP-939 mimicry without writing captured audio to disk;
- Added the first integration for **MineZero / Return by Death**, replacing its automatic death rewind with the SCP: Classified Directive death/spectate flow while using SCP: Classified Directive saves as MineZero checkpoints;
- MineZero-integrated multiplayer sessions keep dead players in the spectate flow while survivors remain; after a team wipe, dead players vote before the latest valid checkpoint rewinds the session;
- With MineZero integration active, SCP-714's terminal coma becomes recoverable while another living player remains: the wearer is forced into an attackable sleeping state on the floor, and another living player can use the **Remove / SCP-714** contextual prompt to take the ring and wake them immediately; if the comatose wearer becomes the last living player, the coma becomes fatal normally;
- Extended MineZero checkpoint snapshots with SCP: Classified Directive player capabilities, SCP Inventory state, persistent facility/SCP-079 data, and tracked SCP: Classified Directive block and BlockEntity changes so the mod's custom state rewinds with the world;
- Added save-safety checks for MineZero-integrated sessions, preventing checkpoints while players are in active combat, dangerous damage/effect states, SCP-914 processing, SCP-330 hand-loss conditions, or an existing death session where saving could create a deterministic death loop;
- MineZero's own automatic and alternate checkpoint paths are suppressed while the integration is active so checkpoints remain synchronized with the SCP: Classified Directive save system.

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
- Reworked **Bleeding** into a reusable persistent injury instead of an SCP-012-specific damage loop: blood-loss pulses occur at irregular intervals, vary in severity, continue until death or actual healing, and can now be applied by other authored attacks.

## Facility construction

- Added Roombas;
- Added a wall-mounted **Document Holder** that stores one Document item;
- Added the **Object Containment Unit**, a keycard-secured containment pedestal with configurable Level 1–6 access;
- Added a modular, animated Core Room elevator based on SCP: Unity, with automatic floor discovery, a moving carriage, landing gates, procedural cables, and one-floor-at-a-time travel;
- Added the per-player, per-world `coreroomdiscovery.ogg` cue, played once when that player first sees a Core Room Floor Station;
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

- Standardized the creative-tab names as **SCP: Classified Directive - Anomalies**, **SCP: Classified Directive - Items**, and **SCP: Classified Directive - Blocks**;
- Added direct **Facility**, **Anomalies**, and **Items** shortcuts below the Creative Inventory button shown by SCP Inventory in Creative mode;
- Organized the Items tab under five graphical headers in this order: **Access & Security**, **Equipment**, **Tools & Utility**, **Consumable**, and **Miscellaneous**;
- Moved the blue, pink, and yellow SCP-330 candies from vanilla **Food & Drinks** into the Items tab's **Consumable** section;
- Organized facility content under ten headers in this order: **Functional**, **Decoration**, **General**, **Core Room**, and **Zones**;
- Added **Alternative Metal Wall (Bottom)**, **Rest Area Corner Floor**, and **Kitchen Corner Floor** to **LCZ - Sublevel 1**;
- Added fixed-orientation automatic transitions from Rest Area Corner Floor to Metal Floor and from Blue Floor to Kitchen Corner Floor.

## Creative Safe Zones

- Added the Creative-only **Safe Zone Tool** to the Items tab's **Tools & Utility** section, with Debug Stick styling and an in-game usage tooltip;
- Left-clicking selects the first corner, right-clicking an opposite corner creates a persistent dimension-bound Safe Zone, and sneaking while right-clicking the air cancels the active selection;
- Added a white selection outline, green outlines for supported automatic soundtrack sources, and post-creation outlines for up to 32 completed Safe Zones within 96 blocks while the tool is held in either hand;
- Sneaking while right-clicking inside a Safe Zone opens its editor, which supports deletion, Area Music state, and a soundtrack dropdown containing the standard **Offices** and **SCP-131 Containment** tracks; detected special tracks are revealed and selected by default but can be freely replaced;
- Safe Zones prevent hostile spawn placement, final spawn positions, direct entity insertion, hostile targeting, and physical entry, while redirecting threats to another eligible nearby player where possible;
- Roamer scheduling now excludes protected players and preserves the exact remaining countdown while every eligible player is inside a Safe Zone;
- SCP-106 ends an abandoned Safe Zone pursuit with its normal sinking sequence, while SCP-173 waits until it is out of view and SCP-939 retreats from observers before disappearing when no nearby unprotected target remains;
- Added automatic soundtrack detection for SCP-914, SCP-1176, SCP-079, SCP-012, SCP-426, SCP-294, and Core Room Floor Stations, mapped to their corresponding soundtrack files in the mod sounds folder;
- Safe Zone music uses one continuous streamed loop, starts at a non-zero `0.001` volume, fades in over three seconds, and fades out over three seconds after leaving or changing tracks.

## Roamer spawning and developer tools

- Added natural spawn cycles for SCP-173, SCP-106, and SCP-939, with separate `173spawn`, `106spawn`, and `939spawn` gamerules;
- Reworked natural encounters around one global scheduler per SCP instead of one timer per player; when a global check succeeds, one valid Survival player is selected at random as the encounter target;
- Safe, Euclid, and Keter now use progressively more aggressive initial and recurring encounter-check intervals, while Thaumiel disables natural roamer checks entirely;
- Each valid Survival player shortens the global check interval by 10%, capped at a 50% reduction with five or more players; player-count changes proportionally rescale the remaining timer instead of restarting it;
- When another roamer is already active, additional roamer encounters become less likely, but rare overlapping encounters are still possible;
- SCP-106 and SCP-173 stop their matching spawn timer while active and restart it after dying or despawning, while SCP-939 keeps checking with its existing population penalty so overlapping SCP-939 encounters remain possible;
- Added `/disableAllRoamers`, `/enableAllRoamers`, `/despawnAllRoamers`, `/despawnRoamer <scp173|scp106|scp939>`, and `/roamerForceSpawn <scp173|scp106|scp939>`;
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
- Added a default-enabled module that suppresses Minecraft's ambient soundtrack without blocking SCP: Classified Directive' contextual music;
- Added a default-enabled main menu soundtrack across the title screen and other menus until a world is opened;
- Added a default-enabled client-side **Contextual Damage Feedback** preference: direct hits retain the normal hurt sound and camera reaction, while continuous damage such as Wither, Poison, and Bleeding reduces health without replaying the direct-hit sound or shaking the view;
- Added temporary ground splatters for damage feedback, using randomized scale and rotation, smooth ten-second fading, positional splatter audio with slight pitch variation, full-block-only placement, smaller pools for continuous damage, progressively larger pools for Bleeding as health falls, no blood for Poison, and dark blood coloration for Wither.

## Contextual interactions

- Added item-specific contextual interactions and inherited alternate variants, allowing one block or entity to expose different actions according to the item held without duplicating its complete configuration;
- Added a state-aware integrated Document Holder prompt that shows only the hand icon, with no text, and appears only while the holder can currently accept, return, or close a document;
- Rebuilt the in-world **K** interaction editor with responsive **Prompt** and **Anchor** pages, default and variant navigation, duplication and removal controls, held-item selection, independent icons, item requirements, and per-variant anchors;
- Added the `config.png` contextual icon and native Screwdriver prompts for keycard readers, Tesla Gate Terminals, Core Room elevator floor stations, editable facility signs, SCP-131-A/B, and Roombas;
- Added Configuration Center badges that identify interactions with alternate variants and item-specific actions;
- Expanded the Contextual Interactions list into a merged catalog that visibly distinguishes **Integrated**, **Override**, and **Custom** rules, surfaces integrated defaults missing from older external configs, and shows each alternate interaction together with its required item.
- Added an **Off-screen prompts** option to the contextual-interaction editor, allowing selected block prompts to remain available at the edge of the screen while behind the player;
- Kept off-screen prompts disabled by default while enabling them in the bundled configuration for door buttons and keycard readers;
- Added pickup feedback to contextual interactions whose action is exactly **Take**, so SCP-714, SCP-1576, and future item-recovery prompts use the custom pickup cue when **Custom Item Interaction Sounds** is enabled and the vanilla pickup cue when it is disabled;
- Added a warning in the visual anchor editor when the selected block probably has no native right-click interaction.

## Configuration integration

- Added update-safe integrated defaults for SCP-914 recipes involving SCP: Classified Directive content, allowing new bundled transformations to appear in existing installations without resetting `914recipes.json` while keeping configured recipes and fragments authoritative;
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
