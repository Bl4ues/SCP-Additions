#!/usr/bin/env python3
"""Static CI guardrails for SCP: Classified Directive.

The checks deliberately distinguish dangerous stale namespace/package references
from legacy persistence identifiers which must remain readable for old worlds.
This file validates source structure only and never mutates project files.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN_NAMESPACE = "scp_classified_directive"
MIXIN_CONFIG = ROOT / "src/main/resources/scp_classified_directive.mixins.json"
MIXIN_ROOT = ROOT / "src/main/java/com/bl4ues/scpclassifieddirective/mixin"

LEGACY_NAMESPACE_DIRS = (
    "scp_additions",
    "scp_unity_extra_blocks",
    "scp_ublocks",
    "scpinventory",
)

# These spellings represent live ResourceLocations or obsolete Java packages.
# Bare legacy words are intentionally NOT forbidden because compatibility code
# must still recognize old SavedData, capability and configuration identifiers.
FORBIDDEN_TEXT = (
    "scp_additions:",
    "scp_unity_extra_blocks:",
    "scp_ublocks:",
    "scpinventory:",
    "net/mcreator/scpadditions",
    "net.mcreator.scpadditions",
    "com/bl4ues/scpinventory",
    "com.bl4ues.scpinventory",
)

TEXT_SUFFIXES = {
    ".java", ".json", ".toml", ".gradle", ".properties", ".cfg",
    ".mcmeta", ".md", ".txt", ".csv", ".yaml", ".yml",
}


def fail(errors: list[str]) -> None:
    print("SCP: Classified Directive integrity verification failed:")
    for error in errors:
        print(f" - {error}")
    raise SystemExit(1)


def check_legacy_directories(errors: list[str]) -> None:
    for kind in ("assets", "data"):
        root = ROOT / "src/main/resources" / kind
        for namespace in LEGACY_NAMESPACE_DIRS:
            path = root / namespace
            if path.exists():
                errors.append(f"legacy {kind} namespace directory still exists: {path.relative_to(ROOT)}")

    for path in (
        ROOT / "src/main/java/net/mcreator/scpadditions",
        ROOT / "src/main/java/com/bl4ues/scpinventory",
        ROOT / "config/scpadditions",
        ROOT / "config/scpinventory",
    ):
        if path.exists():
            errors.append(f"legacy source/config directory still exists: {path.relative_to(ROOT)}")


def iter_text_files() -> list[Path]:
    roots = (
        ROOT / "src/main/java",
        ROOT / "src/main/resources",
        ROOT / "config",
    )
    files: list[Path] = []
    for root in roots:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if path.is_file() and path.suffix.lower() in TEXT_SUFFIXES:
                files.append(path)
    return files


def check_forbidden_text(errors: list[str]) -> None:
    for path in iter_text_files():
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for token in FORBIDDEN_TEXT:
            if token in text:
                errors.append(
                    f"dangerous legacy token {token!r} in {path.relative_to(ROOT)}"
                )


def check_metadata(errors: list[str]) -> None:
    mods_toml = ROOT / "src/main/resources/META-INF/mods.toml"
    text = mods_toml.read_text(encoding="utf-8")
    if f'modId="{MAIN_NAMESPACE}"' not in text:
        errors.append("mods.toml does not declare the current modId")
    if 'modId="scp_additions"' in text:
        errors.append("mods.toml still declares the legacy modId")
    if 'displayName="SCP: Classified Directive"' not in text:
        errors.append("mods.toml display title is not the clean current project name")

    build_gradle = (ROOT / "build.gradle").read_text(encoding="utf-8")
    if "group = 'com.bl4ues.scpclassifieddirective'" not in build_gradle:
        errors.append("build.gradle does not use the current Java group")
    if "archivesBaseName = 'scp_classified_directive'" not in build_gradle:
        errors.append("build.gradle does not use the current archive base name")


def check_mixins(errors: list[str]) -> None:
    data = json.loads(MIXIN_CONFIG.read_text(encoding="utf-8"))
    package = data.get("package")
    if package != "com.bl4ues.scpclassifieddirective.mixin":
        errors.append(f"unexpected mixin package: {package!r}")
        return

    declared: list[str] = []
    for section in ("mixins", "client", "server"):
        values = data.get(section, [])
        if values is None:
            continue
        if not isinstance(values, list):
            errors.append(f"mixin section {section!r} is not a list")
            continue
        declared.extend(str(value) for value in values)

    duplicates = sorted({name for name in declared if declared.count(name) > 1})
    if duplicates:
        errors.append("duplicate mixin declarations: " + ", ".join(duplicates))

    for name in declared:
        source = MIXIN_ROOT / (name.replace(".", "/") + ".java")
        if not source.is_file():
            errors.append(f"declared mixin has no source file: {name} -> {source.relative_to(ROOT)}")

    stale = "client.ScpDeathScreenSpectateControlsMixin"
    replacement = "client.ScpDeathScreenLiveFeedMixin"
    if stale in declared:
        errors.append(f"stale pre-fix mixin is still declared: {stale}")
    if replacement not in declared:
        errors.append(f"death-screen live-feed mixin is not declared: {replacement}")
    stale_source = MIXIN_ROOT / "client/ScpDeathScreenSpectateControlsMixin.java"
    if stale_source.exists():
        errors.append(f"stale death-screen mixin source still exists: {stale_source.relative_to(ROOT)}")


def check_set_of_duplicates(errors: list[str]) -> None:
    """Catch simple immutable sets made invalid by namespace consolidation."""
    java_root = ROOT / "src/main/java"
    for path in java_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(r"(?s)Set\.of\((?P<body>.*?)\);", text):
            body = match.group("body")
            values = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', body)
            if "ScpClassifiedDirectiveMod.MODID" in body:
                values.append(MAIN_NAMESPACE)
            duplicates = sorted({value for value in values if values.count(value) > 1})
            if duplicates:
                errors.append(
                    f"duplicate value(s) in Set.of in {path.relative_to(ROOT)}: "
                    + ", ".join(duplicates)
                )


def check_native_emissive_initialization(errors: list[str]) -> None:
    path = ROOT / "src/main/java/com/bl4ues/scpclassifieddirective/client/render/NativeEmissiveModelEvents.java"
    text = path.read_text(encoding="utf-8")
    if '"scp_keycards"' in text:
        errors.append("NativeEmissiveModelEvents still owns the obsolete scp_keycards namespace")
    if re.search(
        r"private\s+static\s+final\s+RenderType\s+\w+\s*=\s*Sheets\.cutoutBlockSheet\(\)",
        text,
    ):
        errors.append("NativeEmissiveModelEvents initializes Sheets too early in a static field")


def changelog_highlights() -> list[str]:
    text = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    match = re.search(
        r"(?ms)^## Highlights\s*$\n(?P<body>.*?)(?=^##\s)",
        text,
    )
    if not match:
        return []
    return [
        line[2:].strip()
        for line in match.group("body").splitlines()
        if line.startswith("- ")
    ]


def whats_new_highlights() -> list[str]:
    path = ROOT / "src/main/java/com/bl4ues/scpclassifieddirective/client/MainMenuWhatsNewPanelClient.java"
    text = path.read_text(encoding="utf-8")
    match = re.search(
        r"(?ms)HIGHLIGHTS\s*=\s*List\.of\((?P<body>.*?)\);",
        text,
    )
    if not match:
        return []

    values: list[str] = []
    for raw in match.group("body").splitlines():
        raw = raw.strip().rstrip(",")
        if not raw:
            continue
        if not (raw.startswith('"') and raw.endswith('"')):
            continue
        values.append(json.loads(raw))
    return values


def check_highlights(errors: list[str]) -> None:
    changelog = changelog_highlights()
    whats_new = whats_new_highlights()
    if not changelog:
        errors.append("could not parse CHANGELOG Highlights")
        return
    if not whats_new:
        errors.append("could not parse in-game What's New highlights")
        return
    if changelog != whats_new:
        errors.append(
            "CHANGELOG Highlights and in-game What's New differ\n"
            f"   changelog={changelog!r}\n"
            f"   whats_new={whats_new!r}"
        )


def main() -> None:
    errors: list[str] = []
    check_legacy_directories(errors)
    check_forbidden_text(errors)
    check_metadata(errors)
    check_mixins(errors)
    check_set_of_duplicates(errors)
    check_native_emissive_initialization(errors)
    check_highlights(errors)

    if errors:
        fail(errors)

    print("SCP: Classified Directive project integrity verification passed.")


if __name__ == "__main__":
    main()
