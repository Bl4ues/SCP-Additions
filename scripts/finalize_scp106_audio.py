import json
from pathlib import Path


def replace(path_name: str, old: str, new: str) -> None:
    path = Path(path_name)
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Missing replacement in {path_name}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


wrong = Path("src/main/resources/assets/scp_additions/sounds/10phase_2.ogg")
correct = Path("src/main/resources/assets/scp_additions/sounds/106phase_2.ogg")
if wrong.exists() and not correct.exists():
    wrong.rename(correct)

sounds_path = Path("src/main/resources/assets/scp_additions/sounds.json")
sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
sounds["scp_106_phase"] = {
    "subtitle": "subtitles.scp_additions.scp_106_phase",
    "sounds": [
        {"name": "scp_additions:106phase_1", "stream": False, "volume": 1.0},
        {"name": "scp_additions:106phase_2", "stream": False, "volume": 1.0},
        {"name": "scp_additions:106phase_3", "stream": False, "volume": 1.0},
    ],
}
sounds["scp_106_chase"] = {
    "sounds": [{"name": "scp_additions:106chase", "stream": True, "volume": 1.0}]
}
sounds["scp_106_stop"] = {
    "sounds": [{"name": "scp_additions:106stop", "stream": False, "volume": 1.0}]
}
sounds["enter"] = {
    "sounds": [{"name": "scp_additions:enter", "stream": True, "volume": 1.0}]
}
sounds_path.write_text(
    json.dumps(sounds, ensure_ascii=False, separators=(",", ":")) + "\n",
    encoding="utf-8",
)

translations = {
    "en_us.json": "SCP-106 phases through a surface",
    "pt_br.json": "SCP-106 atravessa uma superfície",
}
lang_root = Path("src/main/resources/assets/scp_additions/lang")
for filename, subtitle in translations.items():
    path = lang_root / filename
    if not path.exists():
        continue
    data = json.loads(path.read_text(encoding="utf-8"))
    data["subtitles.scp_additions.scp_106_phase"] = subtitle
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

service = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java"
replace(
    service,
    'for (String group : List.of("inventory", "interactions", "hud", "vitals", "blink", "scp_173"))',
    'for (String group : List.of("inventory", "interactions", "hud", "vitals", "blink", "audio", "scp_173"))',
)
replace(
    service,
    '        checkBoolean(root, "blink", "enabled", errors);\n        checkBoolean(root, "scp_173", "enabled", errors);',
    '        checkBoolean(root, "blink", "enabled", errors);\n        checkBoolean(root, "audio", "enter_sound_enabled", errors);\n        checkBoolean(root, "scp_173", "enabled", errors);',
)
replace(
    service,
    '\\"blink\\":{\\"enabled\\":true},\\"scp_173\\"',
    '\\"blink\\":{\\"enabled\\":true},\\"audio\\":{\\"enter_sound_enabled\\":true},\\"scp_173\\"',
)

extension = "src/main/java/net/mcreator/scpadditions/config/ui/Scp079ModulesScreenExtension.java"
replace(
    extension,
    '            new Row("blink", "enabled", "Blink System",\n                    "Enables automatic and manual blinking.", true),\n            new Row("scp_173", "enabled", "SCP-173",',
    '            new Row("blink", "enabled", "Blink System",\n                    "Enables automatic and manual blinking.", true),\n            new Row("audio", "enter_sound_enabled", "World Entry Sound",\n                    "Plays enter.ogg after joining or opening a world.", true),\n            new Row("scp_173", "enabled", "SCP-173",',
)

Path(
    "src/main/java/net/mcreator/scpadditions/config/ui/Scp106AudioModulesExtension.java"
).unlink(missing_ok=True)

for path in Path("src/main/resources").rglob("modules.json"):
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        continue
    data.setdefault("audio", {})["enter_sound_enabled"] = True
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

changelog = Path("CHANGELOG.md")
text = changelog.read_text(encoding="utf-8")
start = text.index("## SCP-106\n")
end = text.index("## SCP-173\n", start)
section = """## SCP-106

- Added SCP-106 as a new roaming threat, including its model, animations, sounds, and spawn egg;
- SCP-106 can appear naturally after the player has spent some time in the world, emerging from the ground or nearby walls before immediately beginning a hunt;
- Added close-range attacks that deal damage and apply Wither;
- Added a ranged attack that throws a straight trail of corrosion across the floor when the player creates distance but remains in clear view, dealing damage, Wither, and Slowness on a direct hit;
- Corrosion left by SCP-106 remains for longer and continuously slows Survival players who walk over it;
- SCP-106 can phase through solid blocks, moving more slowly while inside them and leaving temporary portals on the surfaces it enters and exits;
- If the player creates too much distance, SCP-106 can disappear and re-emerge ahead of the player's path, while sometimes using its ranged attack instead when it has a clear shot;
- Added positional phasing sounds and a private chase soundtrack heard only by the hunted player, with a smooth fade and ending cue when the hunt finishes;
- SCP-106 ignores Creative and Spectator players, and disappears when its target dies if no other valid player is nearby;
- Tesla Gates repel SCP-106 instead of killing it, forcing it to sink away and preventing the next two natural spawn checks;
- Hunts can end quickly or continue for several minutes depending on how long SCP-106 remains interested in the target; players can create distance, but cannot simply despawn or lose SCP-106 before the hunt ends.

"""
text = text[:start] + section + text[end:]
marker = "## Bug Fixes\n"
entry = (
    "## Audio and presentation\n\n"
    "- Reintroduced the world-entry sound and added a General & Modules option to disable it.\n\n"
)
if entry not in text:
    text = text.replace(marker, entry + marker, 1)
changelog.write_text(text, encoding="utf-8")

Path(".github/scp106-audio-trigger").unlink(missing_ok=True)
Path(".github/workflows/apply-scp106-audio-polish.yml").unlink(missing_ok=True)
Path("scripts/finalize_scp106_audio.py").unlink(missing_ok=True)
