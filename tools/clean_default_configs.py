from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CONFIG_ROOT = ROOT / "config"
CONTEXT_FILE = ROOT / "config" / "scpinventory" / "context_interactions.json"
CHANGELOG = ROOT / "CHANGELOG.md"

BANNED_PREFIXES = (
    "scp_ublocks:",
    "scp_unity_extra_blocks:",
    "scpinventory:",
    "scp_inventory:",
)
EXPECTED_LEGACY_IDS = {
    "scp_unity_extra_blocks:normal_door",
    "scp_unity_extra_blocks:door_open",
    "scp_unity_extra_blocks:left_log_door",
    "scp_unity_extra_blocks:left_log_door_open",
    "scp_unity_extra_blocks:right_log_door",
    "scp_unity_extra_blocks:right_log_door_open",
    "scp_unity_extra_blocks:office_door",
    "scp_unity_extra_blocks:office_door_open",
    "scp_unity_extra_blocks:bath_door",
    "scp_unity_extra_blocks:bath_door_open",
    "scp_unity_extra_blocks:ws_dclosed",
    "scp_unity_extra_blocks:ws_open",
    "scp_unity_extra_blocks:button_open",
    "scp_unity_extra_blocks:button_closed",
}


def compact(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def contains_banned(value: Any) -> bool:
    text = compact(value).lower()
    return any(prefix in text for prefix in BANNED_PREFIXES)


def is_legacy_pig_overlay(value: Any) -> bool:
    text = compact(value).lower()
    return (
        "minecraft:pig" in text
        and ("scp_131" in text or "scp131" in text)
        and "overlay" in text
    )


def clean_node(value: Any, removed: list[Any]) -> Any:
    if isinstance(value, list):
        cleaned: list[Any] = []
        for child in value:
            if contains_banned(child) or is_legacy_pig_overlay(child):
                removed.append(child)
                continue
            cleaned.append(clean_node(child, removed))
        return cleaned
    if isinstance(value, dict):
        return {key: clean_node(child, removed) for key, child in value.items()}
    return value


def count_namespace(value: Any, namespace: str) -> int:
    return compact(value).lower().count(namespace.lower())


def entry_id(value: Any) -> str:
    if not isinstance(value, dict):
        return ""
    raw = value.get("id")
    return raw if isinstance(raw, str) else ""


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def update_changelog() -> None:
    bullet = (
        "- Removed stale bundled context-interaction rules that still targeted the old "
        "`scp_unity_extra_blocks` registry namespace; the migrated `scp_additions` rules "
        "and optional `scpo` compatibility remain unchanged;\n"
    )
    text = CHANGELOG.read_text(encoding="utf-8")
    if bullet.strip() in text:
        return
    heading = "## Configuration and building\n"
    if heading not in text:
        raise RuntimeError("Could not find the Configuration and building changelog section")
    CHANGELOG.write_text(text.replace(heading, heading + "\n" + bullet, 1), encoding="utf-8")


def main() -> None:
    before_scpo = 0
    after_scpo = 0
    all_removed: list[tuple[str, Any]] = []
    changed_files: list[str] = []

    for path in sorted(CONFIG_ROOT.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        before_scpo += count_namespace(data, "scpo:")
        removed: list[Any] = []
        cleaned = clean_node(data, removed)
        after_scpo += count_namespace(cleaned, "scpo:")
        if removed:
            relative = path.relative_to(ROOT).as_posix()
            changed_files.append(relative)
            all_removed.extend((relative, entry) for entry in removed)
            write_json(path, cleaned)

    removed_ids = {entry_id(entry) for _, entry in all_removed if entry_id(entry)}
    if len(all_removed) != 14:
        raise RuntimeError(f"Expected exactly 14 audited legacy entries, removed {len(all_removed)}")
    if set(changed_files) != {CONTEXT_FILE.relative_to(ROOT).as_posix()}:
        raise RuntimeError(f"Unexpected config files changed: {changed_files}")
    if removed_ids != EXPECTED_LEGACY_IDS:
        missing = sorted(EXPECTED_LEGACY_IDS - removed_ids)
        unexpected = sorted(removed_ids - EXPECTED_LEGACY_IDS)
        raise RuntimeError(f"Legacy ID mismatch; missing={missing}, unexpected={unexpected}")
    if before_scpo != after_scpo or before_scpo != 11:
        raise RuntimeError(
            f"scpo compatibility changed unexpectedly: before={before_scpo}, after={after_scpo}"
        )

    for path in sorted(CONFIG_ROOT.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        raw = compact(data).lower()
        remaining = [prefix for prefix in BANNED_PREFIXES if prefix in raw]
        if remaining:
            raise RuntimeError(f"Legacy namespace remains in {path}: {remaining}")
        if is_legacy_pig_overlay(data):
            raise RuntimeError(f"Legacy pig/SCP-131 overlay remains in {path}")

    # Ensure the real integrated SCP-131 interactions remain intact.
    context = json.loads(CONTEXT_FILE.read_text(encoding="utf-8"))
    context_ids = {
        entry.get("id")
        for entry in context.get("interactions", [])
        if isinstance(entry, dict)
    }
    for required in ("scp_additions:scp_131_a", "scp_additions:scp_131_b"):
        if required not in context_ids:
            raise RuntimeError(f"Required integrated interaction was lost: {required}")

    # Keep the legitimate vanilla SCP-914 pig transformation. It is unrelated
    # to the old overlay compatibility shim.
    entities_file = CONFIG_ROOT / "scpadditions" / "914recipes.d" / "entities.json"
    entities = json.loads(entities_file.read_text(encoding="utf-8"))
    recipe_ids = {
        recipe.get("id")
        for recipe in entities.get("recipes", [])
        if isinstance(recipe, dict)
    }
    if "scp_additions:pig_fine_to_hoglin" not in recipe_ids:
        raise RuntimeError("Legitimate pig -> hoglin SCP-914 recipe was lost")

    update_changelog()

    print("Removed legacy default entries:")
    for file_name, entry in all_removed:
        print(f"- {file_name}: {entry_id(entry) or compact(entry)}")
    print(f"Preserved {after_scpo} scpo: references.")
    print("Preserved real SCP-131 interactions and the vanilla pig -> hoglin recipe.")


if __name__ == "__main__":
    main()
