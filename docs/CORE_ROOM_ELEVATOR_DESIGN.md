# Core Room Elevator: Technical Design and Model Contract

Status: implementation foundation for SCP Additions 3.1.0  
Target: Minecraft 1.20.1, Forge 47.4.10+, GeckoLib 4.4.9+

## 1. Goal

Implement a reusable vertical elevator system whose first presentation matches the central Core Room elevator from SCP: Unity as closely as practical while behaving as a native Minecraft system rather than a teleport effect.

The first elevator must provide:

- separate Up and Down contextual interaction targets on the cabin panel;
- equivalent call buttons on every floor;
- one-floor-at-a-time movement;
- animated doors and button feedback;
- an authored moving cabin with fixed top machinery and procedural shaft elements;
- any number of user-declared floors within a valid vertical shaft;
- a top controller that defines the upper anchor for cables and guide structures;
- smooth server-authoritative movement for players, mobs, items and other entities inside the cabin;
- real moving collision against the cabin, its doors and the surrounding world;
- persistence, multiplayer synchronization and safe recovery after chunk/world reloads.

The architecture should support other elevator skins and layouts later without duplicating the motion and collision systems.

## 2. Reference behavior

The supplied SCP: Unity capture shows the following interaction sequence:

1. The cabin rests level with a floor with its gate open.
2. Up and Down are distinct, compact targets on a narrow vertical panel.
3. Pressing a valid direction illuminates or depresses that button.
4. The gate closes completely before travel begins.
5. The cabin accelerates, travels vertically, decelerates and levels with the adjacent floor.
6. The gate opens only after the cabin reaches the stop.
7. A floor button can call the cabin when it is elsewhere.
8. A new movement input is not accepted until the prior door/travel cycle has completed.

The Core Room reference is a three-sublevel use case, but the Minecraft implementation must not hard-code three floors or fixed spacing.

## 3. Existing SCP Additions systems to reuse

The current codebase already provides useful pieces:

- `FacilityModule` owns non-MCreator facility registries and custom block entities.
- `ContextPromptClient`, `ContextInteractionRegistry` and `ContextInteractPacket` provide configurable contextual prompts.
- GeckoLib block/entity renderers are already established.
- `FacilityPropPartBlock` demonstrates invisible local collision/selection cells for large authored props.
- `SimpleChannel` networking and server-authoritative configuration synchronization are already present.

These systems should be extended rather than introducing a second unrelated interaction or rendering framework.

## 4. Blocking prerequisite: contextual sub-target identity

The current context system can visually select multiple rules attached to the same block or entity, but the selected rule is not included in `ContextInteractPacket`. The server retrieves the target registry entry and executes `rules.get(0)`.

That means two anchors on one elevator panel would both execute the first action. Before the elevator can expose separate buttons, contextual interactions need stable rule identity.

### Required context-system extension

Add a stable per-rule key separate from the registry target ID, for example:

```json
{
  "type": "entity",
  "id": "scp_additions:core_room_elevator",
  "actionId": "elevator_up",
  "anchor": {
    "position": [0.78, 1.35, -1.12],
    "rotateWith": "entity_yaw"
  },
  "selection": {
    "aimRadius": 0.11
  },
  "visual": {
    "scale": 0.55
  }
}
```

The chosen `actionId` must be sent to the server and resolved against the matching rule. Never trust an arbitrary client action: the server must verify that the rule exists for the target, that its range and line-of-sight conditions are valid, and that the requested direction is currently allowed.

Entity anchors also need to use local coordinates rotated by entity yaw. At present, entity anchors resolve only to the bounding-box center plus a world offset, which cannot represent two small buttons attached to a moving, rotatable cabin.

Recommended additions:

- `actionId`: stable rule key;
- entity-local `anchor.position` and `rotateWith: entity_yaw`;
- `selection.aimRadius`: tighter target acquisition than ordinary blocks;
- `visual.scale`: per-rule prompt/icon scale;
- optional dynamic availability and text provider for code-owned targets;
- packet payload containing the selected rule key;
- server-side exact-rule lookup instead of `rules.get(0)`;
- network protocol bump when the packet shape changes.

The generic JSON system remains useful for ordinary targets. Elevator rules should be code-owned defaults or protected built-ins because their enabled state, label and result are dynamic.

## 5. World structure

### 5.1 Top controller

`ElevatorTopControllerBlock` and `ElevatorTopControllerBlockEntity`

The controller is placed at the shaft top and stores the authoritative installation state:

- installation UUID;
- facing/orientation;
- validated, bottom-to-top floor list;
- active carriage UUID;
- current and target floor indices;
- queued floor calls;
- movement phase and fault information;
- model/style ID;
- optional speed and door timing values;
- last known carriage position for recovery.

The controller also provides the fixed top gantry model and the world-space origins from which cables and iron guide structures descend.

Only one controller owns an installation. Floor markers link upward to it during validation.

### 5.2 Floor marker

`ElevatorFloorMarkerBlock` and `ElevatorFloorMarkerBlockEntity`

Place one marker at every stop. It defines:

- the exact Y coordinate at which the cabin floor is level with that floor;
- doorway facing;
- installation/controller link;
- optional display label or floor number;
- the exterior call-panel position;
- clearance envelope around the opening.

The lowest marker is the bottom limit. A separate bottom-controller block is unnecessary.

Markers are structural declarations, not arbitrary proximity detectors. Validation should require every marker in one installation to share the controller's horizontal shaft axis and facing within a small configured tolerance.

### 5.3 Moving carriage

`CoreRoomElevatorEntity`

Use a dedicated non-living entity for the moving cabin. The entity owns:

- installation UUID and controller position;
- current world position;
- server-authoritative phase;
- movement start/end Y, start game time, speed and acceleration;
- target floor;
- door progress/state;
- active button feedback;
- local collision manifest;
- model/style ID.

Do not implement the entire moving cabin as one animated block entity. A block entity is position-bound and would force either frame-by-frame block replacement or client-only visual movement detached from real collision.

Do not mount occupants as passengers. Players must retain normal movement, view control, combat and interaction while riding the platform.

## 6. State machine

Use an explicit server-authoritative state machine:

```text
IDLE_OPEN
  -> DOOR_CLOSING
  -> READY_TO_MOVE
  -> MOVING
  -> LEVELING
  -> DOOR_OPENING
  -> IDLE_OPEN
```

`FAULT` is entered when the installation becomes invalid, an obstruction cannot be resolved, required chunks cannot be kept available, or the carriage/controller link is lost.

Rules:

- movement input is accepted only in `IDLE_OPEN`;
- the door is not logically closed until the close animation finishes;
- the door is not logically open until the open animation finishes;
- a cabin button requests exactly the adjacent floor in its direction;
- an exterior button calls the cabin to that marker when the cabin is elsewhere;
- a request for the current floor opens the door if safe;
- impossible directions at the highest/lowest stop are disabled and unlit;
- repeated calls are deduplicated;
- queued calls are reevaluated only after the active cycle ends;
- breaking a required controller/marker places the installation into a recoverable fault instead of deleting occupants or teleporting the carriage.

## 7. Motion profile

Cabin travel is physical entity movement, not a GeckoLib translation animation.

Use an acceleration-limited triangular or trapezoidal profile:

- short trips accelerate and immediately decelerate;
- longer trips accelerate to maximum speed, cruise, then decelerate;
- final leveling snaps only a very small residual error after velocity reaches zero;
- the server evaluates exact position from the movement plan and game time;
- clients receive the same movement-plan parameters and interpolate from them;
- occasional authoritative corrections prevent accumulated drift.

`ElevatorFoundation.MotionPlan` already defines this shared profile independently of renderer and world code.

Recommended initial tuning for testing, not final reference values:

- maximum speed: `0.115` blocks/tick (2.3 blocks/second);
- acceleration: `0.0065` blocks/tick²;
- leveling tolerance: `0.002` blocks;
- door close/open duration: approximately 28 to 36 ticks each.

Final values should be tuned against the supplied capture after the real model and sounds are in game.

## 8. Collision and occupant transport

This is the core quality requirement.

### 8.1 Collision representation

The visible mesh and collision are separate assets. Define the cabin with a compact list of local AABBs:

- floor slab;
- rear wall;
- left and right side walls;
- ceiling;
- front-left and front-right posts;
- left and right door leaves while closed or closing;
- optional railing/window frames where they materially block movement.

Transform these boxes by cabin position and yaw each tick. Do not generate collision from every model cube.

### 8.2 World collision

Before moving, sweep the complete carriage collision envelope from previous to next position against world block collision shapes.

- stop and enter `FAULT` if a solid obstruction occupies the shaft;
- prevent tunneling at higher speed through swept-volume checks;
- exclude installation-owned marker/controller proxy cells where appropriate;
- validate the full route during installation scans, then continue checking while moving because players can alter the shaft afterward.

### 8.3 Supported entities

Determine occupants from the carriage's previous and swept bounds. An entity is supported when its feet are on or immediately above the floor slab and its horizontal footprint overlaps the walkable platform.

For each supported entity:

1. preserve its horizontal input and velocity;
2. add the carriage's vertical delta to the requested movement;
3. resolve the entity against the moved cabin and world collision together;
4. update fall distance/on-ground state consistently;
5. retain the support relation through a small grace tolerance to avoid one-tick flicker;
6. release support immediately when the entity jumps or exits the platform.

Items, mobs, projectiles where sensible, and players use the same support calculation. Special-case only entities that intentionally ignore collision, such as spectators or no-physics entities.

### 8.4 Moving wall/ceiling contact

Entities merely standing on the floor are not the only case. The solver must also handle:

- a descending ceiling contacting an entity above the cabin;
- a rising floor pushing an entity that partially intersects the doorway;
- closing doors contacting an entity;
- occupants leaning into shaft walls during travel;
- multiple entities pushing one another inside the cabin.

Resolve moving-cabin AABBs against entity AABBs using swept displacement along the cabin's movement axis. Never use repeated `teleportTo`, command teleportation, frame-by-frame block replacement, or `startRiding` as the normal transport mechanism.

### 8.5 Client smoothness

The server owns all collision and final positions. The client renders the cabin from the synchronized movement plan and uses the tracked platform delta for local-player camera continuity. Server correction remains authoritative but should usually be too small to see.

A dedicated client hook may need to account for the local player's platform delta before ordinary interpolation so the camera does not lag one frame behind the floor.

## 9. Chunk and persistence safety

- Keep the controller, carriage and all chunks intersected by the current swept route loaded while moving.
- Use a bounded Forge chunk ticket owned by the installation; release it after the complete door-opening cycle.
- Persist controller state and carriage identity.
- On world load, reconcile controller and carriage:
  - one valid carriage: resume or safely level at the nearest floor;
  - missing carriage: recreate only when the shaft is clear;
  - duplicate carriages: retain the UUID referenced by the controller and remove only unoccupied duplicates;
  - invalid structure: enter `FAULT` and preserve occupant safety.
- Never resume movement before every relevant chunk and floor marker has been validated.

## 10. Rendering architecture

### 10.1 Cabin

Render the moving carriage through a GeckoLib entity renderer. Translate the complete model with entity position. GeckoLib handles authored sub-part animation only:

- left/right doors;
- button depression;
- indicator lights;
- optional subtle mechanical vibration.

### 10.2 Top gantry

The controller block entity renders the fixed top assembly. It never moves.

### 10.3 Cables and iron guides

Do not author cables at one fixed shaft height.

Render them procedurally between named controller and carriage attachment points using one of two approaches:

1. repeated model segments aligned along Y; or
2. a custom vertex renderer with tiled UVs.

The second option is preferable for cables because it avoids visible segment seams and supports any floor spacing. Iron guide bars may use repeated authored segments if the joins are designed to tile.

Cables terminate at `cable_mount_left` and `cable_mount_right`; guides use the corresponding guide locators. Their world lengths are computed from controller Y to carriage Y every frame.

### 10.4 Culling and bounds

The carriage renderer's culling bounds cover only the cabin. Cables/top structures are rendered by the controller and use a shaft-aware render bounding box. Avoid assigning one enormous all-height bounding box to every floor marker.

## 11. Blockbench model contract

### 11.1 Coordinate system

Use a GeckoLib Blockbench project.

- 16 model units = 1 Minecraft block.
- Default model front is north.
- Model origin is the horizontal center of the cabin.
- Y = 0 is the upper surface of the walkable floor, not the underside of the cabin.
- All pivots must stay on rational model-unit coordinates where possible.
- Apply no permanent cabin travel translation in Blockbench.

### 11.2 Initial dimensional envelope

These are implementation targets inferred from the reference capture, not claims about the original Unity asset:

- outer cabin width: approximately 48 units / 3.0 blocks;
- outer cabin depth: approximately 48 units / 3.0 blocks;
- outer cabin height: approximately 52 units / 3.25 blocks above the floor origin, plus under-floor machinery as needed;
- clear interior width/depth: approximately 36 units / 2.25 blocks;
- clear doorway: approximately 24 units wide by 36 units high;
- front threshold/ramp extension: approximately 8 units / 0.5 block.

Build a low-detail greybox first. Collision dimensions must be approved before final UV work because the collision manifest is authored from these dimensions.

### 11.3 Required cabin bones

The `.bbmodel` and exported `.geo.json` must contain these stable names:

```text
root
└─ cabin
   ├─ shell
   │  ├─ floor
   │  ├─ ceiling
   │  ├─ wall_left
   │  ├─ wall_right
   │  ├─ wall_rear
   │  ├─ frame_front
   │  └─ windows_and_grilles
   ├─ door_left
   ├─ door_right
   ├─ controls
   │  ├─ button_up
   │  ├─ button_down
   │  ├─ button_up_light
   │  └─ button_down_light
   └─ locators
      ├─ cable_mount_left
      ├─ cable_mount_right
      ├─ guide_mount_left
      └─ guide_mount_right
```

The constants already exist in `ElevatorFoundation.ModelBones` for the names consumed directly by code. Additional visual-only children are allowed.

### 11.4 Separate models/assets

Prepare these assets rather than one monolithic shaft model:

1. `core_room_elevator_cabin.bbmodel`
   - moving cabin, doors, controls, lights and attachment locators;
2. `core_room_elevator_top.bbmodel`
   - fixed top gantry, pulleys, cable origins and structural cap;
3. `core_room_elevator_floor_marker.bbmodel` or a normal block model
   - exterior call panel and floor threshold/frame pieces;
4. tileable cable texture or cable cross-section definition;
5. tileable iron-guide segment where a repeated model is preferred;
6. dedicated inventory/placement models for the top controller and floor marker.

### 11.5 Animations

Required:

- `door_open`: both leaves move from closed to fully open;
- `door_close`: exact reverse or separately tuned closing motion;
- `button_up_press` and `button_down_press`: brief travel and return;
- optional `idle`: extremely subtle machinery motion only if visible in the reference.

Guidelines:

- keep door animations approximately 1.4 to 1.8 seconds for first-pass tuning;
- use linear or gently eased mechanical travel, not elastic overshoot;
- door pivots belong to the moving leaves, not the model origin;
- lights are separate bones/material regions so code can hide, show or render them emissively;
- logical collision state follows server phase, not the client animation clock.

Cabin travel is never exported as an animation.

### 11.6 Textures and materials

Recommended layout:

- one main opaque/cutout cabin texture;
- one emissive overlay for arrows and ceiling lighting;
- optional separate glass texture only where actual translucency is required;
- grille and perforated-metal holes should prefer cutout over translucent blending;
- base textures must remain readable without the optional MoreMcmeta emissive plugin;
- preserve padding between UV islands to prevent mip bleeding;
- do not mirror directional arrow/text UVs.

### 11.7 Modeler deliverables

The implementation should not begin final renderer integration until the model package contains:

- source `.bbmodel` files;
- exported `.geo.json` files;
- animation JSON;
- base and emissive textures;
- one orthographic dimension sheet showing origin, total bounds, doorway and floor height;
- one pivot/bone screenshot;
- one collision table listing local min/max coordinates for every collision AABB;
- confirmation that all required bone and locator names are exact.

## 12. Sound contract

Prepare separate positional sounds for:

- button press/invalid press;
- door closing loop or start/end;
- door opening loop or start/end;
- motor start;
- travel loop;
- deceleration/leveling;
- arrival chime or voice, where appropriate.

The server triggers state transitions; clients play the corresponding sound at the carriage/controller. Travel loops follow the moving entity. Avoid one long baked sequence because floor spacing and travel time are configurable.

## 13. Proposed classes and resources

```text
facility/elevator/
  ElevatorFoundation.java
  ElevatorInstallationValidator.java
  ElevatorTopControllerBlock.java
  ElevatorTopControllerBlockEntity.java
  ElevatorFloorMarkerBlock.java
  ElevatorFloorMarkerBlockEntity.java
  CoreRoomElevatorEntity.java
  ElevatorMotionController.java
  ElevatorCollisionManifest.java
  ElevatorCollisionSolver.java
  ElevatorRequestQueue.java
  ElevatorSavedData.java                 (only if cross-controller indexing is needed)

client/elevator/
  CoreRoomElevatorRenderer.java
  CoreRoomElevatorGeoModel.java
  ElevatorTopRenderer.java
  ElevatorCableRenderer.java
  ElevatorClientMotion.java

network/
  ElevatorStatePacket.java
  ElevatorInteractionResultPacket.java  (only for immediate local feedback)
```

Likely resources:

```text
assets/scp_additions/geo/entity/core_room_elevator.geo.json
assets/scp_additions/geo/block/core_room_elevator_top.geo.json
assets/scp_additions/animations/entity/core_room_elevator.animation.json
assets/scp_additions/textures/entity/core_room_elevator.png
assets/scp_additions/textures/entity/core_room_elevator_emissive.png
assets/scp_additions/textures/block/core_room_elevator_top.png
assets/scp_additions/textures/block/elevator_cable.png
```

## 14. Implementation order

### Phase A: interaction prerequisite

- add stable contextual rule keys;
- support entity-local rotated anchors;
- support compact prompt scale and aim radius;
- send and validate selected rule identity server-side;
- bump context network protocol;
- add regression tests for multiple rules on one target.

### Phase B: installation graph

- register top controller and floor marker;
- persist installation UUIDs and links;
- scan and validate floors/shaft;
- expose clear debug diagnostics for invalid installations.

### Phase C: carriage and motion

- register the elevator entity;
- spawn/recover one carriage per valid controller;
- implement state machine and `MotionPlan`;
- synchronize movement plans and phases;
- implement call queue and one-floor direction behavior.

### Phase D: collision

- implement local collision manifest;
- implement world swept-envelope validation;
- implement supported-entity transport;
- implement moving wall/ceiling/door resolution;
- test player, multiple players, mobs, items and high-latency multiplayer.

### Phase E: presentation

- integrate authored models;
- render dynamic cables/guides;
- add doors, button feedback, emissive layers and sounds;
- tune dimensions and timings against the supplied capture.

### Phase F: hardening

- chunk unload/reload recovery;
- server restart during every state;
- obstruction/fault behavior;
- controller/marker removal during travel;
- dimension and world-border checks;
- performance profiling with several installations.

## 15. Acceptance tests

The feature is not complete until all of these pass:

### Interaction

- aiming at Up never triggers Down and vice versa;
- prompts remain attached to the moving buttons;
- exterior buttons call the cabin from every declared floor;
- highest Up and lowest Down refuse cleanly;
- button spam during door animation cannot corrupt state.

### Movement

- cabin travels exactly one adjacent floor per cabin-button press;
- no visible final-position bounce;
- client and server agree after latency and packet loss simulations;
- stopping/restarting the server during movement recovers safely.

### Occupants

- standing player remains visually and physically fixed to the floor;
- walking/jumping inside the moving cabin remains normal;
- two or more players do not desynchronize;
- mobs and dropped items move with the cabin;
- an entity partly in the doorway is resolved safely;
- no fall damage is accumulated from ordinary elevator travel;
- Spectator and no-physics entities are ignored appropriately.

### Collision

- closed doors block occupants and outside entities;
- open doors leave the complete doorway clear;
- cabin cannot pass through blocks added to the shaft;
- high-speed travel cannot tunnel through a one-block obstruction;
- moving floor, ceiling and walls cannot leave entities embedded.

### Construction and recovery

- arbitrary valid floor spacing works;
- duplicate floor height or mismatched facing reports an invalid installation;
- breaking a marker/controller cannot duplicate the cabin;
- unloaded/reloaded chunks preserve floor links and carriage state;
- only bounded chunks remain ticketed while moving.

### Performance

- an idle elevator performs no broad shaft scan every tick;
- collision work is limited to active carriage bounds and nearby entities;
- procedural cable rendering does not create one block entity per cable segment;
- several idle elevators have negligible server tick cost.

## 16. Decisions deliberately avoided

- No dependency on Create or another moving-contraption mod.
- No giant pre-sized model tied to one Core Room height.
- No passenger mounting workaround.
- No repeated player teleportation.
- No frame-by-frame placement of moving blocks.
- No client-only collision.
- No automatic collision extraction from model cubes.

These shortcuts make a prototype quick and the finished elevator visibly wrong. The system is intended to become shared infrastructure for later elevator styles, so the first implementation must establish the correct movement, interaction and persistence contracts.
