# Changelog

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
