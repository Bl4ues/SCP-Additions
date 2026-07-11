# UI, Inventory and Interaction Policy

## Module toggles

The integrated systems are independently configurable in `config/scpadditions/modules.json`.

- `inventory.enabled` controls the custom SCP Inventory capability, pickup routing and inventory screen.
- `interactions.enabled` controls the SCP Inventory custom interaction system.
- `hud.enabled` is the master switch for custom HUD rendering.
- `vitals.custom_health_enabled` controls the custom health system and its HUD component.
- `vitals.stamina_enabled` controls stamina behavior and its HUD component.
- `vitals.horror_movement_enabled` controls the horror movement behavior.

Registry entries and stored capability data must remain available while a module is disabled. A toggle disables behavior and presentation; it must not unregister content or delete saved data.

The inventory and interaction toggles are independent. Custom interactions must degrade safely when the SCP Inventory is disabled and must never assume that capability slots are available. Interactions that only require vanilla state may remain functional.

## HUD behavior

The vanilla health hearts are suppressed only when both conditions are true:

1. `hud.enabled` is true.
2. `vitals.custom_health_enabled` is true.

If either setting is false, vanilla hearts remain visible. Disabling only stamina must not affect vanilla health rendering.

The stamina HUD is rendered only when both `hud.enabled` and `vitals.stamina_enabled` are true.

## Font scope

Roboto is not a global Minecraft font.

It may be used only for:

- every internal text element rendered inside the custom SCP Inventory window;
- the `GRANTED` and `DENIED` permission text rendered by Tesla Terminal screens.

Normal Minecraft menus, chat, subtitles, item names, tooltips and unrelated SCP Additions screens must keep the vanilla font unless a separate screen has an explicitly approved font.

The standalone resource `assets/minecraft/font/default.json` must not be migrated. The combined build explicitly excludes it as a safeguard.

Custom UI text must select `scpinventory:roboto` through the component font style or another screen-local font mechanism.

## Inventory migration requirements

The custom inventory migration must preserve the final generated standalone behavior rather than reimplementing a simplified approximation. The first functional inventory batch must include, as one coherent unit:

- capability attachment, serialization, clone and synchronization;
- the 12-slot inventory model, equipment, keys and codex state;
- item classification and pickup routing;
- duplication-safe usable-item sessions;
- coin storage without vanilla coin mirrors;
- mode-aware SCP-294 currency extraction;
- vanilla fallback behavior while the inventory module is disabled;
- custom inventory screen and its Roboto-scoped internal text.

## Interaction migration requirements

The custom interaction system will be migrated after its dependency graph has been mapped. It must preserve the standalone interaction selection, target detection, networking, validation and configuration behavior. It must consult `interactions.enabled` before opening or executing custom interactions.
