# SCP-012 asset contract

SCP-012 uses one public block/item and eight hidden animation-stage blocks.

## Block models

The authored model sequence is:

- `scp_012_closed.json`
- `scp_012_opening_1.json`
- `scp_012_opening_2.json`
- `scp_012_opening_3.json`
- `scp_012_opening_4.json`
- `scp_012_open.json`

The closing sequence reuses `opening_4` through `opening_1` in reverse. The four intermediate stages each last fifteen ticks, making the complete opening or closing animation exactly sixty ticks (three seconds).

The raw Blockbench exports are stored as internal geometry parents. Public wrapper models provide the namespaced texture mappings without modifying the authored geometry.

## Textures

- `assets/scp_additions/textures/block/scp_012.png` — 512×512 main texture.
- `assets/scp_additions/textures/block/012_sign.png` — 256×256 sign texture.
- `assets/scp_additions/textures/item/012.png` — flat inventory/hand icon.

## Sounds

- `012_open.ogg` and `012_close.ogg` accompany the three-second box animation.
- `012_trance.ogg` loops while the local player is influenced.
- `012_damage.ogg` begins in the damage zone and fades out if the player escapes.
- `bleed_1.ogg` through `bleed_3.ogg` are randomized bleeding milestones.
- `on_mount_golgotha.ogg` is registered for later monitor use but is not triggered by the current SCP-012 sequence.

## Optional overlays

`012_overlay_1.png` through `012_overlay_5.png` may be placed under `assets/scp_additions/textures/gui/`. Missing files are ignored cleanly. Available overlays flash randomly together with the code-generated psychosis effects.
