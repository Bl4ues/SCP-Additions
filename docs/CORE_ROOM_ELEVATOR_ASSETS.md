# Core Room Elevator Asset Contract

This document records the authored Blockbench assets currently staged for the
Core Room elevator. It is an implementation contract, not a replacement for the
source `.bbmodel` files.

## Resource layout

```text
src/main/resources/assets/scp_additions/
├── geo/
│   ├── entity/core_room_elevator_carriage.geo.json
│   └── block/
│       ├── core_room_elevator_floor_station.geo.json
│       └── core_room_elevator_pulley.geo.json
├── animations/
│   ├── entity/core_room_elevator_carriage.animation.json
│   └── block/core_room_elevator_floor_station.animation.json
├── models/block/
│   ├── core_room_elevator_beams.json
│   └── core_room_floor.json
└── textures/
    ├── entity/core_room_elevator_carriage.png
    └── block/
        ├── core_room_elevator_floor_station.png
        ├── core_room_elevator_pulley.png
        ├── core_room_elevator_beams.png
        └── core_room_floor.png
```

The texture PNGs are intentionally not committed in the initial asset import
because the supplied files contained the exported JSON and screenshots, not the
actual texture images.

## Authored dimensions and connection points

### Moving carriage

- Geometry identifier: `geometry.core_room_elevator_carriage`
- Texture coordinate space: `256 × 256`
- Visible bounds: width `3`, height `4.5`, offset `[0, 1.75, 0]`
- Root bone: `cabin`, authored with a `-90°` Y rotation
- Cable attachment locators:
  - `cable_attachment_front`: `[-7, 53, 0]`
  - `cable_attachment_rear`: `[7, 53, 0]`
- Animated bones: `door_left`, `door_right`
- Context targets: `button_up`, `button_down`

### Floor station

- Geometry identifier: `geometry.core_room_elevator_floor_station`
- Texture coordinate space: `128 × 128`
- Authored height: `48` model units, exactly three Minecraft blocks
- Root bone: `station`
- Animated assembly: `door_plate` with child `back_door_plate`
- Context targets: `button_up`, `button_down`

### Repeating beams

- Vanilla block model, exactly `16` units high
- Four authored vertical profiles preserve the X/Z measurements exported from
  Blockbench
- Texture: `scp_additions:block/core_room_elevator_beams`

### Top pulley

- Geometry identifier: `geometry.core_room_elevator_pulley`
- Texture coordinate space: `128 × 128`
- Root bone: `pulley`, authored with a `-90°` Y rotation
- Fixed cable origin locators:
  - `cable_origin_front`: `[-7, 15, 0]`
  - `cable_origin_rear`: `[7, 15, 0]`

### Core Room floor

- Vanilla block model with a zero-thickness top plane
- Texture: `scp_additions:block/core_room_floor`

## Animation names

```text
animation.core_room_elevator_carriage.closed
animation.core_room_elevator_carriage.opening
animation.core_room_elevator_carriage.open
animation.core_room_elevator_carriage.closing

animation.core_room_elevator_floor_station.closed
animation.core_room_elevator_floor_station.opening
animation.core_room_elevator_floor_station.open
animation.core_room_elevator_floor_station.closing
```

The authored `0.75 s` transition timings and transforms are preserved. The
carriage's held-open animation explicitly stores the final authored opening pose
so it cannot snap back to its closed base pose when animation controllers switch
states.

## Procedural cable rendering

The renderer will create two variable-length cables each frame:

```text
cable_origin_front -> cable_attachment_front
cable_origin_rear  -> cable_attachment_rear
```

Only their endpoints come from the models. The cable length is never baked into
a Blockbench asset, allowing arbitrary distances between declared floors.
