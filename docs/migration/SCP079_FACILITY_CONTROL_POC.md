# SCP-079 Facility Control proof of concept

This phase keeps the legacy Facility Pulse Node but adds contextual heavy-door sabotage while the SCP-079 Facility Control block is powered.

## Eligible doors

Only the redstone-operated SCP Unity heavy doors are controlled:

- Default heavy door
- Yellow heavy door
- Black heavy door

Direct-use wooden doors are not affected.

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

## Anti-spam rules

- Context is evaluated once per second per player.
- A successful action creates an 8 to 15 second facility-wide cooldown in that dimension.
- The same door cannot be selected again for 30 seconds.
- Creative and spectator players are ignored.

## Panels

Functional SCP Unity panels that can physically power the selected door or its upper relay are synchronized:

- forced opening: `OPENING` then `OPEN`;
- forced closing: `CLOSING` then `CLOSED`.

Original and mirrored geometry is preserved. Locked panels are not changed.

An opened panel remains usable, allowing the player to press it and close the door afterward.

## Doors without panels

When SCP-079 opens a door without a functional panel, it places a temporary invisible Facility Pulse Node next to the upper relay. The source is removed after five seconds and never replaces a solid block.

SCP-079 does not force a panel-less door closed while another persistent redstone source is still powering it.

## Visual feedback

Every successful sabotage emits the same electric spark and smoke particles used by the Tesla Gate manual override.

## Test checklist

- [ ] powering Facility Control enables contextual sabotage;
- [ ] removing or unpowering Facility Control disables contextual sabotage;
- [ ] no action occurs without a mob actively targeting the player;
- [ ] an open door ahead can close while the player flees;
- [ ] a closed door between pursuer and player can open;
- [ ] panel state follows the forced door state;
- [ ] original and mirrored panels retain their geometry;
- [ ] the player can close a door after SCP-079 forced it open;
- [ ] electric spark and smoke particles appear at the selected door;
- [ ] direct-use wooden doors remain unaffected;
- [ ] temporary invisible power is removed after five seconds;
- [ ] actions respect global and per-door cooldowns.
