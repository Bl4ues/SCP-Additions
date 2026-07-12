# SCP-079 Facility Control proof of concept

This phase keeps the legacy Facility Pulse Node but adds contextual heavy-door sabotage while the SCP-079 Facility Control block is powered.

## Eligible doors

Only the redstone-operated SCP Unity heavy doors are controlled:

- Default heavy door
- Yellow heavy door
- Black heavy door

Direct-use wooden doors are not affected.

A heavy door is eligible only when one of these control interfaces is physically connected to its controller or upper relay heights:

- a functional SCP Unity button;
- a keycard reader of any level;
- an already placed legacy Facility Pulse Node.

Locked buttons, arbitrary redstone sources and bare doors do not grant SCP-079 access.

## Contextual actions

### Close a door ahead of a fleeing player

SCP-079 may close an open eligible door when:

- a survival player is sprinting;
- a nearby mob is actively targeting that player;
- the pursuer is behind the player's travel direction;
- an open heavy door is ahead of the player.

Chance per valid opportunity: 20 percent.

### Open a door for a pursuing threat

SCP-079 may open a closed eligible door when:

- a nearby mob is actively targeting a survival player;
- the mob and player are on opposite sides of the door.

Chance per valid opportunity: 30 percent.

### Unprovoked door harassment

Even without a pursuer, SCP-079 has a low chance to close an eligible open heavy door directly ahead of a moving player.

- the player must actually be moving toward the door;
- standing still or merely looking at the door does not roll the chance;
- chance per valid opportunity: 3 percent.

## Anti-spam rules

- Context is evaluated once per second per player.
- A successful action creates an 8 to 15 second facility-wide cooldown in that dimension.
- The same door cannot be selected again for 30 seconds.
- Creative and spectator players are ignored.

## Panels

Functional SCP Unity buttons that can physically power the selected door or its upper relay are synchronized:

- forced opening: `OPENING` then `OPEN`;
- forced closing: `CLOSING` then `CLOSED`.

Original, authored-left and legacy mirrored geometry is preserved. Locked panels are ignored.

Connected keycard readers are also valid:

- forced opening temporarily changes the reader to its green `ACCEPT` state;
- forced closing restores the reader's normal state;
- level, side, rotation and waterlogging are preserved.

An opened button remains usable, allowing the player to press it and close the door afterward.

## Legacy Facility Pulse Node

The legacy node is never created automatically by the sabotage system.

When a door has no functional button or keycard reader, an already placed node connected to its controller or relay authorizes SCP-079 to manipulate it. Opening powers that existing node temporarily; closing returns it to its inactive state.

## Persistent external redstone

A lever or other ordinary redstone source does not grant SCP-079 access. If a valid panel exists but an unrelated source is still holding the door open, a forced close is aborted and the panel is restored to its open state.

## Visual feedback

Every successful sabotage emits the same electric spark and smoke particles used by the Tesla Gate manual override.

## Test checklist

- [ ] powering Facility Control enables contextual sabotage;
- [ ] removing or unpowering Facility Control disables contextual sabotage;
- [ ] a functional button authorizes heavy-door manipulation;
- [ ] a keycard reader authorizes heavy-door manipulation and turns green on forced open;
- [ ] a locked button by itself does not authorize manipulation;
- [ ] a door with no control interface is ignored;
- [ ] an existing legacy Facility Pulse Node authorizes a panel-less door;
- [ ] no legacy node is created automatically;
- [ ] an open door ahead can close while the player flees;
- [ ] a closed door between pursuer and player can open;
- [ ] an open door may rarely close ahead of a moving player without a pursuer;
- [ ] standing still near an open door does not trigger unprovoked harassment;
- [ ] button state follows the forced door state;
- [ ] original and authored-left panels retain their geometry;
- [ ] the player can close a door after SCP-079 forced it open;
- [ ] persistent unrelated redstone prevents a contradictory forced close;
- [ ] electric spark and smoke particles appear at the selected door;
- [ ] direct-use wooden doors remain unaffected;
- [ ] actions respect global and per-door cooldowns.
