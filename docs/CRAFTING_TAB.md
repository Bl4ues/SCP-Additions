# SCP Inventory Crafting Tab

The Crafting tab is a server-authoritative portable 3x3 crafting interface.

## Layout

- `://CRAFTING_RECIPES` uses the left panel.
- `://CRAFTING_GRID` uses the right panel.
- The grid is positioned in the upper part of the right panel.
- A compact, scrollable view of the SCP Inventory backpack occupies the lower part.
- Backpack items and crafting-grid items support drag and drop.

## Learning recipes

The learned list starts empty and displays `No learnt recipes`.

A recipe is learned after the player successfully crafts it:

- manually through the portable 3x3 grid; or
- through a normal vanilla/modded crafting grid detected by Forge's crafting event.

Future blueprint items should call:

```java
ScpCraftingService.learn(serverPlayer, recipeId);
```

Knowledge, pinned recipes, and the 3x3 grid are persisted per player.

## Recipe ordering

Recipes are grouped in this exact order:

1. pinned recipes, alphabetically;
2. unpinned recipes whose ingredients are currently available, alphabetically;
3. unpinned recipes with missing ingredients, alphabetically.

The pin is drawn by the GUI and does not use a Unicode emoji.

## Recipe rows

Each row displays:

- output icon;
- output name;
- ingredient icons;
- `xN` for repeated ingredients;
- dimmed missing ingredients;
- a dimmed output when the recipe cannot currently be filled.

Clicking an available recipe moves ingredients into the correct 3x3 layout. Clicking an unavailable recipe flashes missing ingredient icons red without mutating the inventory.

## Crafting authority

All movement, automatic filling, consumption, remaining-container items, result insertion, recipe learning, and pin changes are revalidated on the server. Crafting is rejected when the result and remaining items cannot fit into the SCP Inventory.
