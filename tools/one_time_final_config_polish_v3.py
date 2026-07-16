from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source_path = ROOT / "tools/one_time_final_config_polish_v2.py"
source = source_path.read_text(encoding="utf-8")

broken = '''replace_once(
    mods,
    '''authors="Bl4ues"
description='''
SCP Additions 3.0.3 — Quality of Life Update.
Combines the existing SCP Additions content with the SCP Inventory gameplay systems and SCP Unity-inspired facility content.
''' ''',
    '''authors="Bl4ues"
logoFile="logo.png"
description='''
SCP Additions is an SCP survival horror and facility-building mod for Minecraft 1.20.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.
''' ''')'''

fixed = '''replace_once(
    mods,
    """authors="Bl4ues"
description='''
SCP Additions 3.0.3 — Quality of Life Update.
Combines the existing SCP Additions content with the SCP Inventory gameplay systems and SCP Unity-inspired facility content.
'''
""",
    """authors="Bl4ues"
logoFile="logo.png"
description='''
SCP Additions is an SCP survival horror and facility-building mod for Minecraft 1.20.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.
'''
""")'''

if broken not in source:
    raise RuntimeError("Could not locate the malformed mods.toml replacement block")
source = source.replace(broken, fixed, 1)
namespace = {"__file__": str(source_path), "__name__": "__main__"}
exec(compile(source, str(source_path), "exec"), namespace)

this_file = Path(__file__)
if this_file.exists():
    this_file.unlink()
