#!/usr/bin/env python3
"""Conservative repository/JAR audit for SCP Additions.

This script does not delete anything. It reports:
- largest source and JAR files;
- JAR namespace totals and byte-identical duplicate entries;
- direct item registrations and the legacy SCP-294 drink block;
- Java classes with no external textual references;
- assets with no direct textual reference.

The last two groups are candidates only. Minecraft resolves many resources by
registry convention, runtime IDs, blockstates, model parents, and data-driven
configuration, so every candidate still needs manual review before removal.
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
import zipfile
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "build" / "reports" / "asset-audit"
SKIP_DIRS = {".git", ".gradle", ".idea", "build", "run"}
TEXT_SUFFIXES = {
    ".java", ".json", ".toml", ".gradle", ".properties", ".mcmeta",
    ".md", ".txt", ".yml", ".yaml", ".cfg", ".accesswidener",
}
ASSET_EXTENSIONS = {".png", ".jpg", ".jpeg", ".ogg", ".wav", ".json", ".mcmeta"}


@dataclass
class LegacyItem:
    constant: str
    item_id: str
    class_name: str
    resource_id_references: list[str]
    class_references: list[str]
    model_path: str | None
    model_textures: list[str]
    imported_procedures: list[str]


def repo_files() -> Iterable[Path]:
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        rel = path.relative_to(ROOT)
        if any(part in SKIP_DIRS for part in rel.parts):
            continue
        yield path


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8", errors="replace")


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def human_size(value: int) -> str:
    units = ["B", "KiB", "MiB", "GiB"]
    size = float(value)
    for unit in units:
        if size < 1024.0 or unit == units[-1]:
            return f"{size:.2f} {unit}"
        size /= 1024.0
    return f"{value} B"


def collect_text_index(files: list[Path]) -> tuple[dict[str, str], dict[str, set[str]]]:
    texts: dict[str, str] = {}
    token_index: dict[str, set[str]] = defaultdict(set)
    token_pattern = re.compile(r"[A-Za-z0-9_.:/#-]{3,}")
    for path in files:
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        relative = rel(path)
        text = read_text(path)
        texts[relative] = text
        for token in set(token_pattern.findall(text)):
            token_index[token].add(relative)
    return texts, token_index


def find_occurrence_files(texts: dict[str, str], needle: str, excluded: set[str] | None = None) -> list[str]:
    excluded = excluded or set()
    return sorted(path for path, text in texts.items() if path not in excluded and needle in text)


def parse_legacy_items(texts: dict[str, str]) -> list[LegacyItem]:
    mod_items_path = "src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModItems.java"
    source = texts.get(mod_items_path, "")
    lines = source.splitlines()
    start = next((i for i, line in enumerate(lines) if "CUP_OF_ALCOHOL" in line), None)
    end = next((i for i, line in enumerate(lines) if "IRON_C" in line and "REGISTRY.register" in line), None)
    if start is None or end is None or end < start:
        return []

    registration = re.compile(
        r"RegistryObject<Item>\s+(?P<constant>[A-Z0-9_]+)\s*=\s*"
        r"REGISTRY\.register\(\"(?P<id>[a-z0-9_]+)\",\s*\(\)\s*->\s*new\s+"
        r"(?P<class>[A-Za-z0-9_]+)\("
    )
    result: list[LegacyItem] = []
    for line in lines[start : end + 1]:
        match = registration.search(line)
        if not match:
            continue
        item_id = match.group("id")
        class_name = match.group("class")
        class_path = f"src/main/java/net/mcreator/scpadditions/item/{class_name}.java"
        model_path = f"src/main/resources/assets/scp_additions/models/item/{item_id}.json"
        model_textures: list[str] = []
        model_file = ROOT / model_path
        if model_file.exists():
            try:
                model = json.loads(read_text(model_file))
                textures = model.get("textures", {})
                if isinstance(textures, dict):
                    model_textures = sorted({str(value) for value in textures.values()})
            except json.JSONDecodeError:
                pass

        procedures: list[str] = []
        class_text = texts.get(class_path, "")
        for proc in re.findall(
            r"import\s+net\.mcreator\.scpadditions\.procedures\.([A-Za-z0-9_]+);",
            class_text,
        ):
            procedures.append(proc)

        resource_id = f"scp_additions:{item_id}"
        excluded_resource_paths = {
            mod_items_path,
            model_path,
            "src/main/resources/assets/scp_additions/lang/en_us.json",
            "src/main/resources/assets/scp_additions/lang/en_us_3_0.json",
        }
        resource_refs = find_occurrence_files(texts, resource_id, excluded_resource_paths)
        class_refs = find_occurrence_files(texts, class_name, {mod_items_path, class_path})
        result.append(
            LegacyItem(
                constant=match.group("constant"),
                item_id=item_id,
                class_name=class_name,
                resource_id_references=resource_refs,
                class_references=class_refs,
                model_path=model_path if model_file.exists() else None,
                model_textures=model_textures,
                imported_procedures=sorted(set(procedures)),
            )
        )
    return result


def asset_reference_key(path: Path) -> tuple[str, str] | None:
    relative = path.relative_to(ROOT / "src" / "main" / "resources" / "assets")
    if len(relative.parts) < 3:
        return None
    namespace = relative.parts[0]
    category = relative.parts[1]
    rest = Path(*relative.parts[2:])
    if category == "textures" and rest.suffix.lower() in {".png", ".jpg", ".jpeg"}:
        return f"{namespace}:{rest.with_suffix('').as_posix()}", "texture"
    if category == "models" and rest.suffix.lower() == ".json":
        return f"{namespace}:{rest.with_suffix('').as_posix()}", "model"
    if category == "sounds" and rest.suffix.lower() in {".ogg", ".wav"}:
        return f"{namespace}:{rest.with_suffix('').as_posix()}", "sound"
    return None


def collect_asset_candidates(files: list[Path], texts: dict[str, str]) -> list[dict[str, object]]:
    assets_root = ROOT / "src" / "main" / "resources" / "assets"
    candidates: list[dict[str, object]] = []
    for path in files:
        try:
            path.relative_to(assets_root)
        except ValueError:
            continue
        if path.suffix.lower() not in ASSET_EXTENSIONS:
            continue
        key = asset_reference_key(path)
        if key is None:
            continue
        resource_key, kind = key
        relative = rel(path)
        references = find_occurrence_files(texts, resource_key, {relative})
        if references:
            continue
        candidates.append(
            {
                "path": relative,
                "kind": kind,
                "resource_key": resource_key,
                "size": path.stat().st_size,
            }
        )
    candidates.sort(key=lambda row: int(row["size"]), reverse=True)
    return candidates


def collect_java_candidates(files: list[Path], texts: dict[str, str]) -> list[dict[str, object]]:
    candidates: list[dict[str, object]] = []
    class_pattern = re.compile(r"\b(?:public\s+)?(?:final\s+)?class\s+([A-Za-z0-9_]+)\b")
    for path in files:
        if path.suffix != ".java":
            continue
        relative = rel(path)
        text = texts.get(relative, "")
        match = class_pattern.search(text)
        if not match:
            continue
        class_name = match.group(1)
        references = find_occurrence_files(texts, class_name, {relative})
        if references:
            continue
        candidates.append({"path": relative, "class_name": class_name, "size": path.stat().st_size})
    candidates.sort(key=lambda row: str(row["path"]))
    return candidates


def jar_audit() -> dict[str, object]:
    jars = sorted((ROOT / "build" / "libs").glob("*.jar"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not jars:
        return {"jar": None, "entries": [], "largest": [], "namespaces": {}, "duplicates": []}
    jar = jars[0]
    entries: list[dict[str, object]] = []
    hashes: dict[tuple[str, int], list[str]] = defaultdict(list)
    namespaces: Counter[str] = Counter()
    with zipfile.ZipFile(jar) as archive:
        for info in archive.infolist():
            if info.is_dir():
                continue
            data = archive.read(info.filename)
            digest = hashlib.sha256(data).hexdigest()
            entries.append(
                {
                    "name": info.filename,
                    "size": info.file_size,
                    "compressed_size": info.compress_size,
                    "sha256": digest,
                }
            )
            hashes[(digest, info.file_size)].append(info.filename)
            parts = info.filename.split("/")
            if len(parts) >= 2 and parts[0] in {"assets", "data"}:
                namespaces[f"{parts[0]}/{parts[1]}"] += info.file_size
            else:
                namespaces[parts[0]] += info.file_size

    duplicates: list[dict[str, object]] = []
    for (digest, size), names in hashes.items():
        if len(names) < 2 or size == 0:
            continue
        duplicates.append(
            {
                "sha256": digest,
                "size_each": size,
                "copies": len(names),
                "potential_uncompressed_waste": size * (len(names) - 1),
                "entries": sorted(names),
            }
        )
    duplicates.sort(key=lambda row: int(row["potential_uncompressed_waste"]), reverse=True)
    largest = sorted(entries, key=lambda row: int(row["size"]), reverse=True)[:150]
    return {
        "jar": rel(jar),
        "jar_size": jar.stat().st_size,
        "entry_count": len(entries),
        "largest": largest,
        "namespaces": dict(namespaces.most_common()),
        "duplicates": duplicates,
    }


def write_markdown(report: dict[str, object]) -> None:
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    output = REPORT_DIR / "report.md"
    lines: list[str] = [
        "# SCP Additions asset audit",
        "",
        "> This is a conservative static report. Zero textual references do not prove that a Minecraft resource is unused.",
        "",
    ]

    jar = report["jar"]
    lines += ["## Built JAR", ""]
    if jar.get("jar"):
        lines += [
            f"- File: `{jar['jar']}`",
            f"- Size: **{human_size(int(jar['jar_size']))}**",
            f"- Entries: **{jar['entry_count']}**",
            "",
            "### Largest namespaces",
            "",
            "| Namespace/root | Uncompressed size |",
            "| --- | ---: |",
        ]
        for name, size in list(jar["namespaces"].items())[:30]:
            lines.append(f"| `{name}` | {human_size(int(size))} |")
        lines += ["", "### Largest JAR entries", "", "| Entry | Size | Compressed |", "| --- | ---: | ---: |"]
        for row in jar["largest"][:75]:
            lines.append(
                f"| `{row['name']}` | {human_size(int(row['size']))} | {human_size(int(row['compressed_size']))} |"
            )
        lines += ["", "### Byte-identical duplicate groups", ""]
        duplicate_waste = sum(int(row["potential_uncompressed_waste"]) for row in jar["duplicates"])
        lines.append(f"Potential uncompressed duplicate content: **{human_size(duplicate_waste)}**")
        lines.append("")
        for row in jar["duplicates"][:100]:
            lines.append(
                f"- {row['copies']} copies × {human_size(int(row['size_each']))} "
                f"(potential waste {human_size(int(row['potential_uncompressed_waste']))})"
            )
            for name in row["entries"]:
                lines.append(f"  - `{name}`")
    else:
        lines.append("No built JAR was found.")

    lines += ["", "## Legacy SCP-294 item registrations", ""]
    legacy_items: list[dict[str, object]] = report["legacy_items"]
    lines.append(f"Detected **{len(legacy_items)}** registrations from `CUP_OF_ALCOHOL` through `IRON_C`.")
    lines.append("")
    lines.append("| ID | Class | Runtime/data references outside registration/lang/model | Imported procedures |")
    lines.append("| --- | --- | --- | --- |")
    for item in legacy_items:
        refs = "<br>".join(f"`{value}`" for value in item["resource_id_references"]) or "—"
        procs = "<br>".join(f"`{value}`" for value in item["imported_procedures"]) or "—"
        lines.append(f"| `scp_additions:{item['item_id']}` | `{item['class_name']}` | {refs} | {procs} |")

    lines += ["", "## Java classes with no external textual reference", ""]
    java_candidates: list[dict[str, object]] = report["java_candidates"]
    lines.append(f"Detected **{len(java_candidates)}** candidates.")
    lines.append("")
    for row in java_candidates[:400]:
        lines.append(f"- `{row['path']}` ({human_size(int(row['size']))})")

    lines += ["", "## Assets with no direct textual resource-location reference", ""]
    asset_candidates: list[dict[str, object]] = report["asset_candidates"]
    lines.append(f"Detected **{len(asset_candidates)}** candidates. Registry-convention resources can legitimately appear here.")
    lines.append("")
    for row in asset_candidates[:500]:
        lines.append(
            f"- `{row['path']}` — `{row['resource_key']}` — {human_size(int(row['size']))}"
        )

    lines += ["", "## Largest source files", "", "| File | Size |", "| --- | ---: |"]
    for row in report["largest_source_files"][:100]:
        lines.append(f"| `{row['path']}` | {human_size(int(row['size']))} |")

    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    files = list(repo_files())
    texts, _ = collect_text_index(files)
    legacy_items = parse_legacy_items(texts)
    source_sizes = sorted(
        ({"path": rel(path), "size": path.stat().st_size} for path in files),
        key=lambda row: int(row["size"]),
        reverse=True,
    )
    report: dict[str, object] = {
        "legacy_items": [asdict(item) for item in legacy_items],
        "java_candidates": collect_java_candidates(files, texts),
        "asset_candidates": collect_asset_candidates(files, texts),
        "largest_source_files": source_sizes,
        "jar": jar_audit(),
    }
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    (REPORT_DIR / "report.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    write_markdown(report)
    print(f"Wrote {REPORT_DIR / 'report.md'}")
    print(f"Wrote {REPORT_DIR / 'report.json'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
