# SCP Unity Blocks integration

## Public namespace

All migrated block, item, sound-event and creative-tab registry IDs are owned by `scp_additions`.

Examples:

- `scp_additions:tesla_bottom`
- `scp_additions:alarm_lamp`
- `scp_additions:default_door`
- `scp_additions:button_closed`

The original `scp_unity_extra_blocks` namespace remains inside the combined JAR only as a resource library for models, textures and audio.

## Legacy world migration

`FacilityLegacyMappings` listens for missing block and item mappings from `scp_unity_extra_blocks` and remaps every known path to its `scp_additions` equivalent. Published standalone registry paths remain represented internally, including animation-frame blocks.

Do not remove an animation-frame registry entry merely because it is hidden from the creative tab. A saved world may contain a door while it is between endpoint states.

## Creative inventory

The single public tab is `SCP Unity Blocks`.

It exposes:

- architectural pieces;
- props and lights;
- stable door endpoints;
- locked and usable door buttons.

Animation frames, upper helper blocks and transient button states remain registered but hidden.

## Doors

Door behavior is centralized in `FacilityModule` instead of being distributed among generated MCreator procedures.

The following are preserved per family:

- registry paths for every visual frame;
- facing during state transitions;
- opening and closing frame order;
- family-specific timing;
- family-specific sounds;
- endpoint drops and pick-block behavior;
- collision becoming passable during opening and solid during closing.

Heavy default, yellow and black doors are signal-controlled. Normal, logistics, office, bathroom and workshop doors retain direct interaction.

## Buttons

The door button places its opposite-side partner two blocks through the wall, matching the standalone layout. Opening and open states emit redstone power. Breaking either side removes the partner without a duplicate drop.
