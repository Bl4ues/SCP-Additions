from pathlib import Path


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match in {path} but found {count}: {old[:80]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


readme = Path("README.md")
replace_once(
    readme,
    "Explicit rules in `scpinventory.json` take priority. Without a rule, edible/drinkable items are classified as consumables, vanilla armor uses its normal equipment slot, and remaining items are miscellaneous. `USABLE` items temporarily enter a controlled vanilla hotbar session so their normal right-click behavior can run, then return to SCP storage without leaving a duplicate behind.",
    "Explicit rules in `scpinventory.json` always take priority. Without a rule, SCP Additions conservatively recognizes Codex documents, consumables, vanilla armor slots, placeable blocks, common melee/ranged weapons, and manually usable right-click items before falling back to miscellaneous. Datapacks can override the fallback with `scp_additions:auto_weapon`, `scp_additions:auto_usable`, and `scp_additions:auto_miscellaneous`; see [Automatic item classification](docs/AUTOMATIC_ITEM_CLASSIFICATION.md). `USABLE` and `PLACEABLE` items temporarily enter a controlled vanilla hotbar session so their normal right-click or placement behavior can run, then return to SCP storage unless the item was consumed."
)
replace_once(
    readme,
    "The **Status** panel displays active conditions except effects hidden by config, core combat/vital parameters, and the blood type assigned by the Mellified Man system. The **Codex** lists configured document items and can render a packaged page texture, a UTF-8 text resource, or both.",
    "The **Status** panel displays active conditions except effects hidden by config, core combat/vital parameters, and the blood type assigned by the Mellified Man system. The **Codex** lists configured document items and can render a packaged page texture, a UTF-8 text resource, or both. New Codex entries created through the configuration center currently start with `minecraft:paper` as a temporary default until dedicated document items are introduced in a future update; this is only an initial value and can be changed to any registered item."
)
replace_once(
    readme,
    "SCP-914 scans a configurable intake area relative to its winding key. Dropped items and non-player entities use the JSON recipe engine. The first complete recipe for the selected setting is chosen, its inputs are reserved, and outputs are placed at the configured output position after the start delay.",
    "SCP-914 scans a configurable intake area relative to its winding key. Explicit JSON recipes are authoritative and are checked before any inferred behavior. When no explicit recipe matches, the machine can derive conservative transformations from registered crafting recipes and material relationships, allowing broader vanilla and modded integration without overriding modpack-defined results. Once a cycle starts, the complete intake is consumed. If the machine is wound with no valid intake, it waits for up to 40 ticks (two seconds), allowing a solo player to enter before the attempt is abandoned."
)
replace_once(
    readme,
    "## Configuration\n\nConfiguration files are created under the instance's `config` directory.",
    "## Configuration\n\nSCP Additions includes a native, server-authoritative configuration center. While connected to a world, open **Mods → SCP Additions → Config** or run `/scpadditions config`. The integrated single-player owner and operators with permission level 2 or higher can edit modules, inventory/equipment rules, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes with validation, automatic `.bak` backups, rollback, and runtime reload. See the [Configuration Center guide](docs/CONFIGURATION_CENTER.md) for the complete workflow.\n\nConfiguration files are created under the instance's `config` directory."
)

changelog = Path("CHANGELOG.md")
replace_once(
    changelog,
    "# SCP Additions 3.0.3 — Quality of Life Update\n\n## Creative mode",
    "# SCP Additions 3.0.3 — Quality of Life Update\n\n## Native configuration center\n\n- Added a native, server-authoritative configuration center available from **Mods → SCP Additions → Config** and `/scpadditions config`;\n- Added dedicated editors for modules, item categories and equipment effects, hidden Status effects, SCP-173 targets, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes;\n- Added registry search, translated display names, item/entity icons, dynamic recipe inputs and outputs, a drink color picker, tooltips, and a unified SCP Unity-inspired interface using Roboto with a Montserrat main title;\n- Added validation, automatic `.bak` backups, transactional rollback, runtime reload, malformed-JSON rejection, and Windows-resilient file replacement;\n- Restricted configuration writes and legacy editor packets to the integrated owner or players with operator permission level 2 or higher;\n- New Codex entries currently use `minecraft:paper` as a temporary default item until dedicated document items are implemented in a future update. This default can be changed to any registered item before saving.\n\n## Creative mode"
)
replace_once(
    changelog,
    "## Interface and accessibility\n\n- The SCP Inventory now remembers its selected section, Codex document, and scrollbar positions until the player leaves the world;\n- Added an option to disable this interface memory;\n- Moved SCP-1176 blood type display into the Status panel and removed the old inventory overlays;\n- Added subtitles for all custom sounds.",
    "## Interface and accessibility\n\n- The SCP Inventory now remembers its selected section, Codex document, and scrollbar positions until the player leaves the world;\n- Added an option to disable this interface memory;\n- Moved SCP-1176 blood type display into the Status panel and removed the old inventory overlays;\n- Centered and polished the Status parameter layout while moving only the rendered player preview inside its original frame;\n- Status-effect duration bars now begin full when an effect is applied and drain proportionally to that effect's actual remaining duration;\n- Added a subtle outward-pulsing amber vignette for both positive and negative SCP-1176 honey outcomes;\n- Hid SCP-1176's internal synchronization marker from the vanilla effect list and custom Conditions panel;\n- Made SCP-1176's negative-result music follow the affected player's head and stop immediately on death or disconnect;\n- Added subtitles for all custom sounds."
)
replace_once(
    changelog,
    "## Configuration and building",
    "## Automatic item classification\n\n- Added conservative automatic recognition for placeable blocks, common melee/ranged weapons, and manually usable right-click items, including compatible modded subclasses;\n- Explicit `item_rules` remain authoritative over all automatic detection;\n- Added datapack override tags for forcing `WEAPON`, `USABLE`, or miscellaneous fallback behavior;\n- Consumed usable items such as spawn eggs no longer return to the player's hand after their controlled-use session.\n\n## SCP-914 integration and defaults\n\n- Added a two-second pending activation window when no valid intake is present, allowing solo players to wind the key and enter the machine;\n- Existing valid recipes and players in the intake still start immediately, and repeated key use cannot queue duplicate pending cycles;\n- Added a material-aware fallback resolver that can infer transformations from registered crafting recipes and related item tiers when no explicit recipe matches;\n- Explicit JSON recipes remain fully authoritative and always take priority over inferred transformations;\n- Once an SCP-914 cycle starts, every item in the intake is consumed even when only part of the intake contributed to the selected result;\n- Trimmed generic vanilla transformations from the bundled defaults so the fallback resolver handles broad behavior while explicit defaults remain focused on SCP Additions content and special entity transformations;\n- Reduced the bundled SCP-914 1:1 skin pool to five selected defaults and renumbered them consistently as `skin1.png` through `skin5.png`.\n\n## Configuration and building"
)
replace_once(
    changelog,
    "- Configuration saves now preserve the previous file as a `.bak` backup;",
    "- Configuration saves now preserve the previous file as a `.bak` backup and retry/fallback safely when Windows temporarily blocks atomic replacement;\n- Fixed block interaction anchors edited with `K` not being persisted or reloaded reliably;"
)

guide = Path("docs/CONFIGURATION_CENTER.md")
replace_once(
    guide,
    "Manual JSON editing remains supported for advanced, automated, or version-controlled modpack workflows.\n\n## SCP-914 recipe editor",
    "Manual JSON editing remains supported for advanced, automated, or version-controlled modpack workflows.\n\n## Codex documents\n\nThe Codex editor creates a new definition with `minecraft:paper` as its current default item ID. This is intentionally temporary until dedicated document items are introduced in a future update. It is not a restriction: the item ID can be changed to any registered vanilla or modded item before the configuration is saved. Existing Codex definitions keep their configured item IDs.\n\nCodex entries may use a packaged image resource, a UTF-8 text resource, or both. Advanced NBT conditions and image dimensions are preserved when an entry is edited.\n\n## SCP-914 recipe editor"
)

# Remove the one-time updater and its workflow from the resulting documentation commit.
Path("scripts/update_303_docs.py").unlink(missing_ok=True)
Path(".github/workflows/update-303-docs-once.yml").unlink(missing_ok=True)
