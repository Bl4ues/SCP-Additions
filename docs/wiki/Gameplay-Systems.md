# Gameplay Systems

## SCP Inventory

The SCP Inventory is server-authoritative and stored as a player capability. Its default limits are:

- 12 main-item slots;
- 12 key slots;
- 999 units of currency.

The main-item limit can be changed per player from 1 to 128 through commands.

Items are routed into general storage, consumables, usable/placeable items, harmful items, keys, ammunition, currency, equipment, or configured Codex documents. Explicit `item_rules` override automatic classification.

Without an explicit rule, the mod conservatively recognizes:

- consumables;
- vanilla armor slots;
- placeable blocks;
- common melee and ranged weapons;
- manually usable right-click items.

`USABLE` and `PLACEABLE` items temporarily enter a controlled vanilla hotbar session so their normal right-click or placement behavior can run. They return to SCP storage unless consumed.

`CODEX` is an internal runtime category. It cannot be assigned through generic item rules; documents must be created through `codex_documents`.

## Status panel

The Status panel shows:

- active effects, except those hidden by configuration;
- maximum health;
- armor and toughness;
- attack value;
- stamina;
- the player's blood type.

Effect-duration bars start full when an effect is applied and decrease relative to that effect's original duration.

## Codex

The Codex displays configured document items. A document can use:

- a packaged image resource;
- a packaged UTF-8 text resource;
- a world-scoped imported image;
- world-scoped directly written text;
- or a combination of image and text.

The editor accepts PNG, JPG, and JPEG images up to 2.5 MB and 4096×4096 pixels. Imported assets are stored under the active world's `scp_additions/codex_assets` directory; the JSON stores only safe relative references.

New definitions currently start with `minecraft:paper` as a temporary base item. The item can be changed to any registered vanilla or modded item. Unique mode adds `ScpCodexId` NBT so only the generated item becomes that document.

## Contextual interactions

Context rules attach SCP Unity-style prompts to blocks and entities. A rule can configure:

- action and display name;
- range and priority;
- icon and required item mode;
- local anchor and world offset;
- simulated click face;
- `E`, right-click, or both.

The server validates the interaction again before invoking the target. Press `K` while looking at a block or entity to use the visual editor.

Anchor controls include the arrow keys, `Page Up`, `Page Down`, and the mouse wheel. `Shift` changes the step to `0.10`, `Ctrl` changes it to `0.01`, and the normal step is `0.05`.

## Health, stamina, movement, and blink

The custom HUD can replace vanilla hearts and display health, stamina, and blink information.

The survival movement module slows ordinary walking and makes sprinting a committed, stamina-limited action. `NO_STAMINA` item effects prevent sprinting while the configured item is held, worn, or equipped.

Blinking is synchronized with the server. Manual blinking may be pressed or held. **Eye Sore** accelerates blink drain; **Lubricated Eye** removes and prevents Eye Sore, doubles the blink interval, and changes the blink presentation while active.

## Facility-building content

The creative inventory includes SCP Unity-inspired:

- Sector 1 and Sector 2 walls and floors;
- directional markings;
- doors and button panels;
- containment and office structures;
- ventilation pieces;
- lights, heaters, signs, televisions, terminals, and props.

Supported lights can use optional MoreMcmeta emissive overlays.
