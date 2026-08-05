# SCP Additions Changelog

## 3.1.0

### Facility durability

- Standardized Foundation facility blocks at an effective hardness of 37.5 and required an iron-tier or better pickaxe, making an unenchanted iron pickaxe break them in the same time an unenchanted diamond pickaxe breaks obsidian;
- Made Foundation structures, props, panels, readers, elevator components, and redstone-operated heavy doors immune to explosion destruction, while manual doors remain vulnerable.

### SCP-106

- Added SCP-106 as a new roaming threat with wall and floor phasing, repositioning, ranged attacks, pursuit music, Tesla suppression, and a temporary post-suppression cooldown;
- Added SCP-106's Pocket Dimension with configurable size, ceiling height, fog, falling pressure, exit paths, a central drain, and teleportation back to the facility;
- Added a configurable chance for SCP-106 to drag players into the Pocket Dimension instead of killing them;
- Added SCP-106 client rendering, animations, sounds, particles, subtitles, creative spawn item, Codex content, and advancements;
- Added SCP-106-specific hooks for Tesla Gates, Facility Doors, SCP-079, SCP-714, and SCP-131;
- SCP-106 now ignores Creative and Spectator players and ends pursuit immediately when its target switches to either mode;
- Repositioning now keeps SCP-106 at least eight blocks from its target and forces a relocation beyond twenty blocks when necessary;
- Tesla Gate hits now suppress SCP-106 rather than dealing conventional damage, with repeated suppressions producing a ten-minute cooldown;
- Improved SCP-106's wall-exit alignment, visual transitions, pool particles, material response, and animation behavior;
- Increased SCP-106 sound volumes and corrected low-volume hit, step, ranged splash, and chase assets;
- Added a shader-compatible reflective material pass for SCP-106 and reduced excessive specular response on skin and clothing;
- Added reflective rendering to SCP-106 pool particles where supported;
- Fixed SCP-106 immediately entering the floor and despawning when placed with its Creative spawn item;
- Fixed SCP-106 occasionally leaving walls at an incorrect sideways angle instead of facing its target.

### Core Room elevator

- Added the first functional Core Room elevator system with animated cabin, platform, cables, doors, call controls, configurable floors, and top-anchor recognition;
- Added contextual interaction zones for elevator call and travel buttons;
- Added synchronization and collision handling for players, mobs, and items riding inside the elevator;
- Added placeholder elevator textures and models for later visual refinement;
- Added client-side sound, movement, and animation hooks for elevator operation;
- Added server validation for elevator calls, destinations, shaft bounds, and passenger transport;
- Added elevator button, cabin, floor marker, and top-anchor blocks to the creative tab and registry;
- Added elevator documentation and usage notes to the project wiki;
- Fixed multiple movement, collision, rendering, synchronization, and floor-recognition issues discovered during testing.

### Facility systems and interface

- Added a configurable module that disables Minecraft's vanilla hunger system and hides the hunger HUD while enabled;
- Added a matching status-inventory parameter for the hunger setting;
- Renamed the disabled Tesla terminal to **Disabled Tesla Gate Terminal** and added the standard `Decorative Only` tooltip;
- Tesla Gate terminals can now be toggled between enabled and disabled forms with a screwdriver while preserving orientation and waterlogging;
- Replaced the Tesla terminal item icons with `teslaon.png` and `teslaoff.png`;
- Added Auxiliary Power integration to Tesla Gate terminals and a dedicated offline screen overlay;
- Added a Facility Diagnostic Terminal and Auxiliary Power Unit as placeholder blocks for the SCP-079 access system;
- Added SCiPNET diagnostics for uncontained SCP signatures, Tesla Gate status, manual override state, door-system endpoints, and Auxiliary Power availability;
- Added a remote-session cache purge with a persistent five-minute lockout;
- Added terminal and generator ambient audio, including the powered `auxgen` loop.

### SCP-012 and SCP-714

- SCP-012 now forces eligible doors along its influence route open using the same override behavior as other facility systems;
- Restored missing SCP-012 ambience, trance, damage, opening, closing, bleeding, and Mount Golgotha audio paths;
- SCP-714 now prevents and clears SCP-012 trance influence without removing bleeding that has already begun;
- SCP-714 no longer prevents SCP-079 from opening doors or interacting with SCP-012-related facility systems;
- Added accessibility handling for SCP-012 visual effects and photosensitivity warnings.

### SCP-131 and SCP-173

- Improved SCP-173 path recovery after doors are opened by SCP-079;
- SCP-173 now stops moving when it loses a valid path instead of continuing with stale navigation;
- SCP-131 instances now dismiss their player's follower group when they begin watching SCP-173, matching the manual dismiss action.

## SCP-079

- Replaced redstone-based facility access with a powered Facility Diagnostic Terminal and a placeholder Auxiliary Power Unit;
- Restyled the Facility Diagnostic Terminal as a fixed 4:3 ARC-Site-48 SCiPNET v3.2.6 CRT interface using the complete site crest, Montserrat headings, Titillium Web telemetry, compact status panels, system-log details, matching online and offline diagnostic structures, and a five-minute remote-session cache lockout that prevents SCP-079 from restarting protocol discovery while active; corrected uncontained SCP signatures to represent enabled SCP profiles released from containment rather than only currently spawned entities;
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
- SCP-079 now opens selected doors for hostile entities pursuing the player and can close doors to obstruct the player's escape;
- Added reusable tactical scoring and lane-pressure hooks for future SCP-079-controlled devices;
- Added a developer HUD bar for SCP-079 processing power and discovery progress;
- Added hidden advancement handling for the first SCP-079 decision that directly affects a player.

### Rendering and shaders

- Added native emissive support for selected block textures under compatible shader packs;
- Added a shader-only eyes/spidereyes pass for `_native_emissive` masks while shader packs are active;
- Added Iris API compatibility without introducing a hard Oculus dependency;
- Added per-client-chunk emissive caching, frustum culling, and shader-state rebuild handling;
- Fixed native emissive overlays rendering twice and causing z-fighting under shader packs;
- Preserved the normal cutout emissive overlay when shaders are disabled.

### Miscellaneous

- Increased volumes for `106hit.ogg`, `106ranged_splash.ogg`, `106step_1.ogg` through `106step_3.ogg`, and `wither.ogg`;
- Added `Decorative Only` tooltips where appropriate;
- Improved terminal names, item icons, and consistency across facility props;
- Added placeholder textures for newly introduced systems and elevator components;
- Updated README, CHANGELOG, Codex, and wiki documentation for version 3.1.0.
