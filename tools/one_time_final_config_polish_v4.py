from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source_path = ROOT / "tools/one_time_final_config_polish_v2.py"
source = source_path.read_text(encoding="utf-8")

start_marker = "replace_once(\n    mods,\n"
end_marker = "shutil.copyfile("
start = source.find(start_marker)
end = source.find(end_marker, start)
if start < 0 or end < 0:
    raise RuntimeError("Could not locate the malformed mods.toml block")

replacement = """replace_once(
    mods,
    "authors=\\\"Bl4ues\\\"\\n"
    "description='''\\n"
    "SCP Additions 3.0.3 — Quality of Life Update.\\n"
    "Combines the existing SCP Additions content with the SCP Inventory gameplay systems and SCP Unity-inspired facility content.\\n"
    "'''\\n",
    "authors=\\\"Bl4ues\\\"\\n"
    "logoFile=\\\"logo.png\\\"\\n"
    "description='''\\n"
    "SCP Additions is an SCP survival horror and facility-building mod for Minecraft 1.20.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.\\n"
    "'''\\n")
"""

source = source[:start] + replacement + source[end:]
source = source.replace(
    "    \"CHANGELOG.md\",\n    '''## Configuration center''',",
    "    \"CHANGELOG.md\",\n    '''## Native configuration center''',",
    1)
namespace = {"__file__": str(source_path), "__name__": "__main__"}
exec(compile(source, str(source_path), "exec"), namespace)

for temporary in [
    ROOT / "tools/one_time_final_config_polish_v3.py",
    ROOT / "tools/final_polish_failure.log",
    Path(__file__),
]:
    if temporary.exists():
        temporary.unlink()
