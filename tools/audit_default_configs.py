from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CONFIG_ROOT = ROOT / "config"
REPORT = ROOT / "tools" / "default_config_cleanup_report.json"

BANNED_PREFIXES = (
    "scp_ublocks:",
    "scp_unity_extra_blocks:",
    "scpinventory:",
    "scp_inventory:",
)


def compact(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def reason_for(value: Any, file_path: Path) -> list[str]:
    text = compact(value).lower()
    reasons: list[str] = []
    for prefix in BANNED_PREFIXES:
        if prefix in text:
            reasons.append(f"legacy namespace {prefix[:-1]}")

    # Old standalone SCP Inventory content represented SCP-131 through a pig
    # plus a custom overlay. The integrated mod has real SCP-131 entities, so
    # this compatibility shim should no longer ship in default configuration.
    if "minecraft:pig" in text and ("scp_131" in text or "scp131" in text) and "overlay" in text:
        reasons.append("legacy pig/SCP-131 overlay adaptation")

    return reasons


def walk(value: Any, file_path: Path, json_path: str, findings: list[dict[str, Any]]) -> None:
    if isinstance(value, list):
        for index, child in enumerate(value):
            child_path = f"{json_path}[{index}]"
            reasons = reason_for(child, file_path)
            if reasons:
                findings.append(
                    {
                        "file": file_path.relative_to(ROOT).as_posix(),
                        "path": child_path,
                        "reasons": reasons,
                        "entry": child,
                    }
                )
            walk(child, file_path, child_path, findings)
    elif isinstance(value, dict):
        for key, child in value.items():
            child_path = f"{json_path}.{key}" if json_path else key
            walk(child, file_path, child_path, findings)


def count_namespace(value: Any, namespace: str) -> int:
    return compact(value).lower().count(namespace.lower())


def main() -> None:
    findings: list[dict[str, Any]] = []
    scanned_files: list[str] = []
    scpo_references: dict[str, int] = {}
    parse_errors: list[dict[str, str]] = []

    for file_path in sorted(CONFIG_ROOT.rglob("*.json")):
        relative = file_path.relative_to(ROOT).as_posix()
        scanned_files.append(relative)
        try:
            data = json.loads(file_path.read_text(encoding="utf-8"))
        except Exception as exc:  # fail visibly without hiding malformed defaults
            parse_errors.append({"file": relative, "error": str(exc)})
            continue

        walk(data, file_path, "$", findings)
        count = count_namespace(data, "scpo:")
        if count:
            scpo_references[relative] = count

    report = {
        "scanned_file_count": len(scanned_files),
        "scanned_files": scanned_files,
        "legacy_entry_count": len(findings),
        "legacy_entries": findings,
        "preserved_scpo_reference_count": sum(scpo_references.values()),
        "preserved_scpo_references_by_file": scpo_references,
        "parse_errors": parse_errors,
    }
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    if parse_errors:
        raise SystemExit("One or more bundled configuration files are malformed; see the audit report.")

    print(f"Scanned {len(scanned_files)} JSON files.")
    print(f"Found {len(findings)} legacy entries.")
    print(f"Preserved {sum(scpo_references.values())} scpo: references.")


if __name__ == "__main__":
    main()
