# Commands and Gamerules

## Main commands

| Command | Effect |
| --- | --- |
| `/scpadditions config` | Open the native configuration center. |
| `/scpadditions reload` | Validate and reload all supported configuration files. |

Reloads reject malformed JSON and malformed IDs. Valid but unavailable optional registry IDs are reported as warnings and skipped.

## Inventory administration

Permission level 2 is required.

| Command | Effect |
| --- | --- |
| `/scpinventory reset [targets]` | Reset complete SCP Inventory state and main capacity. |
| `/scpinventory clear [targets]` | Clear main SCP item storage. |
| `/scpinventory clearmain [targets]` | Alias of `clear`. |
| `/scpinventory setmax [targets] <slots>` | Set main capacity from 1 to 128. |
| `/scpinventory maxslots ...` | Alias of `setmax`. |
| `/scpinventory getmax` | Report used/main capacity and key count. |

## Context interaction commands

The visual editor opened with `K` is recommended for normal use.

| Command | Effect |
| --- | --- |
| `/scpinventory context gui` | Open the editor for the viewed entity or block. |
| `/scpinventory context select` | Select the viewed block for command editing. |
| `/scpinventory context add` | Add a default rule for the selected target. |
| `/scpinventory context cancel` | Cancel the active session. |
| `/scpinventory context done` | Finish editing. |
| `/scpinventory context reload` | Reload `context_interactions.json`. |
| `/scpinventory context marker` | Show the current anchor marker. |
| `/scpinventory context set action <text>` | Set the action label. |
| `/scpinventory context set name <text>` | Set the manual name. |
| `/scpinventory context set range <0.25..64>` | Set reach. |
| `/scpinventory context input <mode>` | Set `both`, `e`, or `right_click`. |
| `/scpinventory context item <mode>` | Set `hand` or `card`. |
| `/scpinventory context clickface <face>` | Set the simulated click face. |
| `/scpinventory context rotate <mode>` | Set anchor rotation behavior. |
| `/scpinventory context anchor hit` | Use the selected hit point. |
| `/scpinventory context anchor here <distance>` | Place the anchor in front of the player. |
| `/scpinventory context anchor nudge <x> <y> <z>` | Apply local offsets. |

## Gamerules

| Gamerule | Default | Behavior |
| --- | --- | --- |
| `teslaGateOn` | `true` | Global Tesla Gate state. |
| `teslaGateManualOverride` | `false` | Enables Emergency Override and forces Tesla Gates on. |
| `scp079controlOn` | `false` | Allows SCP-079 to manipulate eligible connected heavy doors. |
| `deconCheckpoint` | `false` | Makes the Decontamination Checkpoint save a processed player's respawn position. |

Examples:

```mcfunction
/gamerule teslaGateOn false
/gamerule teslaGateManualOverride true
/gamerule scp079controlOn true
/gamerule deconCheckpoint true
```
