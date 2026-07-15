# Automatic item classification and SCP-914 intake wait

## Automatic SCP Inventory categories

Explicit entries in `config/scpinventory/scpinventory.json` remain authoritative. When no item rule exists, SCP Additions now applies conservative fallback detection in this order:

1. Codex document definitions;
2. explicit `item_rules` entries;
3. datapack override tags;
4. consumable and armor detection;
5. weapon and manually usable item detection;
6. `MISCELLANEOUS` fallback.

Weapons are detected from standard sword, projectile-weapon, and trident classes, as well as bow, crossbow, and spear use animations. This also covers modded items that reuse the normal Minecraft weapon APIs.

Usable items include standard right-click items, recognized use animations, and non-block item classes that override Minecraft's air-use method. The result is cached by item class, so reflection is not repeated during normal inventory rendering or routing.

Modpacks and datapacks can override the fallback without adding one JSON rule per item:

- `scp_additions:auto_weapon` forces `WEAPON`;
- `scp_additions:auto_usable` forces `USABLE`;
- `scp_additions:auto_miscellaneous` disables automatic weapon/usable inference for the tagged item.

An explicit `item_rules` entry always takes priority over these tags and heuristics.

## SCP-914 intake wait

Winding SCP-914 still starts immediately when the selected setting already has a matching recipe or a player is inside the intake.

When no valid intake is present, the machine now checks once per tick for up to 40 ticks, equal to two seconds. This gives a solo player time to wind the key and enter the intake. If no recipe, entity input, or player becomes valid before the window expires, the attempt is abandoned without starting the refining cycle.

Only one pending activation is allowed per SCP-914 key position. Repeated interaction during the two-second window does not queue duplicate cycles. The existing winding-key sound remains immediate; a dedicated waiting sound can be associated with this state later.
