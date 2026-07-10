# SCP-914 legacy recipes to port

This file is a reminder list for rebuilding SCP-914 defaults as JSON-driven recipe definitions.

New JSON path:

```text
src/main/resources/data/<namespace>/scp914/recipes/<recipe_id>.json
```

Schema draft:

```json
{
  "enabled": true,
  "setting": "rough",
  "input": {
    "item": "scp_additions:level_1_keycard",
    "count": 1
  },
  "output": {
    "item": "scp_additions:pieces_of_paper",
    "count": 1
  },
  "chance": 1.0,
  "copy_input_nbt": false
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

Known legacy item transformations to port first:

```text
rough: scp_additions:level_1_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_2_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_3_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_4_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_5_keycard -> scp_additions:pieces_of_paper
rough: scp_additions:level_6_keycard -> scp_additions:pieces_of_paper
```

Legacy classes removed/replaced in this pass:

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

- The current JSON scaffold only processes item entities placed in the SCP-914 intake area.
- Legacy entity/living transformations were removed as hardcode. If we want entity support later, add a separate JSON type or an optional `entity_input` / `entity_output` branch.
- The old procedures used command-based `/kill @e[distance=..3]`; the new processor removes only the selected input item stack.
- The exact full legacy item/entity transformation list can still be recovered from Git history before this cleanup pass.
