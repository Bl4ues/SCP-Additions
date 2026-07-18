# SCP-012 asset contract

SCP-012 uses one public block/item and seven hidden animation-stage blocks. The closing sequence reuses the opening-stage models in reverse, so only five authored block models are required.

## Blockbench source

Keep the editable project at:

`art/model_sources/scp_012.bbmodel`

Use the Java Block/Item format and preserve the physical pivot/origin across every exported stage. The current Blockbench project shows a 512×512 texture; keep the model's declared texture dimensions and exported PNG dimensions consistent.

## Authored block models

Replace these placeholder JSON files:

- `src/main/resources/assets/scp_additions/models/block/scp_012_closed.json`
- `src/main/resources/assets/scp_additions/models/block/scp_012_opening_1.json`
- `src/main/resources/assets/scp_additions/models/block/scp_012_opening_2.json`
- `src/main/resources/assets/scp_additions/models/block/scp_012_opening_3.json`
- `src/main/resources/assets/scp_additions/models/block/scp_012_open.json`

The three closing blockstates already reuse `opening_3`, `opening_2`, and `opening_1` in reverse order.

## Texture

Recommended path:

`src/main/resources/assets/scp_additions/textures/block/scp_012.png`

Reference it from every exported model as:

`scp_additions:block/scp_012`

Multiple textures are supported, provided every model uses the same namespaced resource paths.

## Item model

The public item model is:

`src/main/resources/assets/scp_additions/models/item/scp_012.json`

It currently inherits the closed block model. Add item display transforms there or export them with the closed model if the inventory/hand presentation needs dedicated positioning.

## Timing

Each transitional block lasts four game ticks. The complete opening or closing sequence therefore uses three visible intermediate stages over twelve ticks before reaching its final state.

## Current implementation boundary

The foundation includes staged opening/closing, command control, SCP-079 automatic opening, compatible heavy-door opening, ten-block attraction, short-range pathfinding, camera lock, contact damage, a temporary code-generated subliminal overlay, and complete SCP-714 immunity.

Authored SCP-012 sounds and authored subliminal overlay textures are intentionally not registered until their final files and desired timing are supplied.
