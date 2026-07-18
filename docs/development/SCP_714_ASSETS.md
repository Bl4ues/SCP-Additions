# SCP-714 Asset Contract

This document fixes the registry ID, Blockbench project type, file paths and display requirements for SCP-714.

## Registry ID

```text
scp_additions:scp_714
```

The item is classified as `ACCESSORYHAND`, so equipping it through the SCP Inventory places its controlled mirror in the player's offhand.

## Blockbench source

Keep the editable project at:

```text
art/model_sources/scp_714.bbmodel
```

The source file is useful for future edits but is not loaded by Minecraft and does not need to be packaged in the JAR.

## Project type

Create SCP-714 as a standard Blockbench **Java Block/Item** project.

Do not use:

- GeckoLib Animated Item;
- GeckoLib Armor;
- an entity model;
- a player or hand rig.

SCP-714 has no independent animation, so a normal vanilla item model is sufficient and more efficient. Minecraft will use the same exported model in the inventory, in the world and in first- or third-person hands.

## Exported model

The current 3D Blockbench export is stored at:

```text
src/main/resources/assets/scp_additions/models/item/scp_714.json
```

Every texture reference inside the exported JSON must use the resource location:

```text
scp_additions:item/scp_714
```

A typical texture section is:

```json
"textures": {
  "0": "scp_additions:item/scp_714",
  "particle": "scp_additions:item/scp_714"
}
```

The numeric texture key may differ depending on the Blockbench export. The resource location must remain the same.

## Texture

The diffuse texture is stored at:

```text
src/main/resources/assets/scp_additions/textures/item/scp_714.png
```

Current size:

```text
128x128
```

The texture dimensions must match the texture size configured in Blockbench.

## Geometry

Model the jade ring around its own center rather than around a Minecraft hand.

- Keep the ring center close to the project origin.
- Keep the rotation pivot at the physical center of the ring.
- Give the band enough thickness to remain visible in the GUI and on the ground.
- Use geometry for the silhouette and texture for fine scratches, translucency-like highlights and jade variation.
- Keep the item technically opaque for the first implementation. Transparent jade is likely to introduce sorting problems and is not required for the effect.

## Display transforms

Configure all relevant Blockbench display slots:

```text
GUI
Ground
Fixed
Third Person Right Hand
Third Person Left Hand
First Person Right Hand
First Person Left Hand
```

The left-hand transforms are especially important because `ACCESSORYHAND` equipment is mirrored into the offhand.

Recommended presentation:

- GUI: enlarged and tilted enough to reveal the opening and thickness of the band;
- ground: small, lying nearly flat;
- fixed: centered, suitable for item frames;
- third-person hands: positioned around or immediately above the hand/finger area, without covering the whole forearm;
- first-person hands: small and restrained so the ring does not obstruct the screen.

Minecraft mirrors some hand transforms automatically, but explicitly preview both left and right hands in Blockbench before export.

The current export contains GUI, ground, fixed and third-person left/right transforms. First-person left/right transforms still need to be judged in-game and added in Blockbench if the defaults are not suitable.

## Files not required

SCP-714 does not require:

```text
geo/item/scp_714.geo.json
animations/item/scp_714.animation.json
```

It also does not require a vignette texture. The fatigue vignette is generated entirely by code.

## Gameplay timing represented by the visual effect

```text
0-90 seconds: progressively increasing fatigue and movement slowdown
90 seconds: "I'm feeling really tired..."
110 seconds: "Maybe I should take a nap..."
120 seconds: "Just... a little rest..."; screen fully black; horizontal movement frozen
120-125 seconds: final opportunity to remove SCP-714
125 seconds: coma death
```

Removing the ring at any time immediately clears the accumulated exposure, movement penalty and vignette.

## Runtime test

After adding the exported JSON and PNG:

```mcfunction
/give @s scp_additions:scp_714
/gamemode survival
```

Equip it in the SCP Inventory accessory slot and verify:

- the item enters the `Accessory` equipment slot;
- the ring appears in the offhand using the left-hand transform;
- stamina becomes unavailable;
- movement slows continuously;
- the code-generated vignette becomes stronger;
- removing it before the final five seconds restores the player immediately.
