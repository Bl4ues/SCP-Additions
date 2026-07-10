# SCP-914 legacy recipes to port

This file is a reminder list for rebuilding SCP-914 defaults as config-driven recipe definitions.

Runtime config path created by the mod:

```text
config/scpadditions/914recipes.json
```

Repository template path:

```text
config/scpadditions/914recipes.json
```

Schema draft:

```json
{
  "version": 1,
  "machine": {
    "intake_offset": [-4, 0, -3],
    "output_offset": [4, 0, -3],
    "search_radius": 2.0,
    "start_delay_ticks": 30,
    "finish_delay_ticks": 160
  },
  "recipes": [
    {
      "id": "scp_additions:iron_and_redstone_to_compass_1_to_1",
      "enabled": true,
      "setting": "1_to_1",
      "item_inputs": [
        { "item": "minecraft:iron_ingot", "count": 4 },
        { "item": "minecraft:redstone", "count": 1 }
      ],
      "entity_inputs": [],
      "item_outputs": [
        { "item": "minecraft:compass", "count": 1 }
      ],
      "entity_outputs": [],
      "chance": 1.0,
      "copy_input_nbt": false,
      "actionbar": "SCP-914 combines the inputs."
    }
  ]
}
```

Supported setting values:

```text
rough
coarse
1_to_1
fine
very_fine
```

Current behavior:

- SCP-914 now checks for a valid configured recipe before starting refinement.
- If there is no valid input, it does not start the refinement sequence.
- Intake/output positions are orientation-aware.
- The old default offsets remain the base: intake `[-4, 0, -3]`, output `[4, 0, -3]`.
- Those offsets rotate according to the Wind Key block facing, so the machine can be built in different orientations.
- Recipes can require multiple item stacks.
- Recipes can require entities/living subjects.
- Recipes can mix item and entity inputs.
- Recipes can output items and/or entities.
- The old `/kill @e[distance=..3]` behavior is gone; only matched inputs are consumed.

Known legacy item transformations to port first:

```text
rough: scp_additions:level_1_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_2_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_3_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_4_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_5_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_6_keycard -> scp_additions:pieces_of_paper
```

Legacy classes removed/replaced:

```text
RoughItemsProcedure
CoarseItemsProcedure
OneToOneItemsProcedure
FineItemsProcedure
VeryFineItemsProcedure
RoughAliveProcedure
CoarseAliveProcedure
OnetoOneAliveProcedure
FineAliveProcedure
VeryFineAliveProcedure
Scp914WindKeyRoughProcedure
Scp914WindKeyCoarseProcedure
Scp914WindKey1to1Procedure
Scp914WindKeyFineProcedure
Scp914WindKeyVeryFineProcedure
```

Notes for next pass:

- The exact full legacy item/entity transformation list can still be recovered from Git history before this cleanup pass.
- Player-as-input behavior is intentionally conservative: matched players are not discarded by the generic processor.
- If we want player refinement later, add explicit player effects/damage/teleport handling instead of treating players like disposable entities.
