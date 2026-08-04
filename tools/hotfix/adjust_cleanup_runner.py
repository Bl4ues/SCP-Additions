from pathlib import Path

script_path = Path("tools/hotfix/remove_legacy_emissive_runtime.py")
script = script_path.read_text(encoding="utf-8")

start = script.index("# CI must prove that a normal clean build works without the removed runtime.")
end = script.index("# Update active documentation and comments", start)
script = script[:start] + script[end:]

script = script.replace(
    '    ".github/workflows/remove-legacy-emissive-runtime.yml",\n',
    '    "tools/hotfix/adjust_cleanup_runner.py",\n',
    1,
)

needle = """    relative = raw_path.decode("utf-8")
    path = ROOT / relative
    if not path.exists():
"""
replacement = """    relative = raw_path.decode("utf-8")
    path = ROOT / relative
    if relative.startswith(".github/workflows/"):
        continue
    if not path.exists():
"""
if script.count(needle) != 1:
    raise RuntimeError("tracked-file scan block was not found exactly once")
script = script.replace(needle, replacement, 1)

script_path.write_text(script, encoding="utf-8")
