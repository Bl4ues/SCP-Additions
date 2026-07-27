from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}: {old!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


facility = "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java"
replace_once(
    facility,
    '        addUBlockCreativeItem(ordered, "sl1_lamp");\n'
    '        addUBlockCreativeItem(ordered, "sl1_flickering_lamp");\n\n'
    '        addDivider(ordered,\n'
    '                "creative_tab.scp_additions.facility.props",',
    '        addDivider(ordered,\n'
    '                "creative_tab.scp_additions.facility.props",'
)
replace_once(
    facility,
    '        addUBlockCreativeItem(ordered, "sl1_ceiling");\n'
    '        addUBlockCreativeItem(ordered, "sl1_ceiling_alt");\n'
    '        addUBlockCreativeItem(ordered, "sl_1_floor_detail_small");',
    '        addUBlockCreativeItem(ordered, "sl1_ceiling");\n'
    '        addUBlockCreativeItem(ordered, "sl1_ceiling_alt");\n'
    '        addUBlockCreativeItem(ordered, "sl1_lamp");\n'
    '        addUBlockCreativeItem(ordered, "sl1_flickering_lamp");\n'
    '        addUBlockCreativeItem(ordered, "sl_1_floor_detail_small");'
)

changelog = "CHANGELOG.md"
replace_once(
    changelog,
    '- Renamed the former **SCP Unity Blocks** creative tab to **SCP Facility Blocks**;\n'
    '- Organized facility content under colored **Functional**, **Props**, **General**, **LCZ - Sublevel 1**, and **LCZ - Sublevel 2** dividers;\n'
    '- Moved facility construction controls out of the general SCP Additions tab and into the Functional section;\n'
    '- Made every SCP Additions creative-tab icon cycle through the visible items in its own tab.',
    '- Standardized the creative-tab names as **SCP Additions - SCPs**, **SCP Additions - Items**, and **SCP Additions - Blocks**;\n'
    '- Organized facility content under colored **Functional**, **Props**, **General**, **LCZ - Sublevel 1**, and **LCZ - Sublevel 2** dividers;\n'
    '- Moved facility construction controls out of **SCP Additions - Items** and into the Functional section of **SCP Additions - Blocks**;\n'
    '- Placed both SL1 Ceiling Lamps under the **LCZ - Sublevel 1** section;\n'
    '- Made every SCP Additions creative-tab icon cycle through the visible items in its own tab.'
)
