# SCPs and Facility Systems

## SCP-173

SCP-173 movement and attacks are server-owned. Observation samples the center, faces, and corners of its hitbox and treats the complete forward half of the camera as visible.

Solid collision or visual shapes block observation. Glass, panes, iron bars, leaves, and supported facility-door windows remain transparent to the observation check. Creative and Spectator players do not activate or become targets for SCP-173.

Routine natural spawning can be enabled independently. A routine-spawned SCP-173 begins inert until seen and may despawn when no Survival player remains nearby.

Configured entity IDs and tags in `scp_173_targets` can both attract SCP-173 and hold it still while they have line of sight.

## SCP-131-A and SCP-131-B

Right-clicking an SCP-131 starts nearby SCP-131 entities following the same player. Ownership persists through save/reload. Holding the configurable dismiss key removes the player's group.

SCP-131 searches for nearby SCP-173 entities, moves to a useful viewing position, and acts as an observer while it has line of sight.

## Facility doors, buttons, and keycard readers

The facility set includes multiple animated door families with saved timing, collision, sounds, and passability. Intermediate animation states remain registered for old-world compatibility but are hidden from creative tabs.

One public **Keycard Reader** item supports Levels 1–6. With a **Screwdriver** in either hand:

- normal interaction selects the required level;
- crouch-interaction copies a reader's level;
- holding `Ctrl` applies the copied level to another reader.

Higher-level keycards satisfy their own level and every lower level. Readers search both vanilla storage and the SCP Inventory.

## Tesla Gates and terminal

Tesla Gates detect nearby entities and transition into a lethal discharge. Emergency Override increases range, shortens warning time, and strengthens feedback.

The Tesla Gate Terminal requires Security Credentials and controls global gamerules. Existing gates are periodically synchronized and intermediate saved shock states are recovered.

## SCP-079 facility control

SCP-079 control is disabled by default. When enabled, it can manipulate eligible connected heavy doors during chases or occasional unprovoked events. Bare doors and unrelated redstone do not grant control; a functional connected button, keycard reader, or Facility Pulse Node is required.

## Decontamination Checkpoint

The checkpoint detects players inside its modeled chamber, closes once per visit, removes active effects, applies Eye Sore unless protected, plays its cycle, fills the chamber with gas, and reopens through a saved block tick.

`PROTECTED_EYES` items and Lubricated Eye prevent the irritation. The `deconCheckpoint` gamerule additionally makes the checkpoint save the player's respawn position.

## Other anomalous content

- **SCP-330:** configurable candy outcomes and the two-candy limit.
- **SCP-426:** a toaster whose descriptions change around its anomalous identity.
- **SCP-572:** a sword that grants confidence beyond its actual combat quality.
- **SCP-902:** a sealed box with proximity and interaction behavior.
- **SCP-1176:** the Mellified Man, anomalous honey, blood-type outcomes, player-following music, and honey-colored visual feedback.
- **Blood type system:** assigns and displays the blood type used by SCP-1176 mechanics.

SCP-294 and SCP-914 have dedicated pages because their configuration systems are substantially larger.
