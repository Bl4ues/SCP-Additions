from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

ROOT = Path.cwd()
JAVA_ROOT = ROOT / "src/main/java"
RESOURCES_ROOT = ROOT / "src/main/resources"


def replace_build_system() -> None:
    build_path = ROOT / "build.gradle"
    old_build = build_path.read_text(encoding="utf-8")
    marker = "processResources {"
    index = old_build.find(marker)
    if index < 0:
        raise SystemExit("processResources block not found")

    tail = old_build[index:].replace(
        "filesMatching('META-INF/mods.toml')",
        "filesMatching('META-INF/neoforge.mods.toml')",
    )

    top = """plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.gradle.userdev' version '7.1.38'
}

version = '3.0.7'
group = 'com.bl4ues.scpadditions'

base {
    archivesName = 'scp_additions-neoforge-1.21.1'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

accessTransformers {
    file 'src/main/resources/META-INF/accesstransformer.cfg'
}

runs {
    configureEach {
        systemProperty 'forge.logging.markers', 'REGISTRIES'
        systemProperty 'forge.logging.console.level', 'debug'
        workingDirectory project.layout.projectDirectory.dir('run').dir(name)
        modSource project.sourceSets.main
    }

    client {
        systemProperty 'neoforge.enabledGameTestNamespaces', 'scp_additions'
    }

    server {
        systemProperty 'neoforge.enabledGameTestNamespaces', 'scp_additions'
        argument '--nogui'
    }
}

repositories {
    maven {
        name = 'GeckoLib'
        url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/'
        content {
            includeGroup 'software.bernie.geckolib'
        }
    }
}

configurations {
    runtimeClasspath.extendsFrom localRuntime
}

dependencies {
    implementation 'net.neoforged:neoforge:21.1.235'
    implementation 'software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.2'
}

"""
    build_path.write_text(top + tail, encoding="utf-8")

    (ROOT / "settings.gradle").write_text(
        """pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}
""",
        encoding="utf-8",
    )

    (ROOT / "gradle.properties").write_text(
        """org.gradle.jvmargs=-Xmx4G
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.caching=true

minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.235
loader_version_range=[1,)
mod_id=scp_additions
mod_name=SCP Additions
mod_license=Creative Commons Attribution-ShareAlike 3.0
mod_version=3.0.7
mod_group_id=com.bl4ues.scpadditions
geckolib_version=4.9.2
""",
        encoding="utf-8",
    )

    (ROOT / "gradle/wrapper/gradle-wrapper.properties").write_text(
        """distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""",
        encoding="utf-8",
    )


def replace_metadata() -> None:
    old_metadata = RESOURCES_ROOT / "META-INF/mods.toml"
    new_metadata = RESOURCES_ROOT / "META-INF/neoforge.mods.toml"
    new_metadata.write_text(
        """modLoader="javafml"
loaderVersion="[1,)"
license="Creative Commons Attribution-ShareAlike 3.0"
issueTrackerURL="https://github.com/Bl4ues/SCP-Additions/issues"

[[mods]]
modId="scp_additions"
version="${mod_version}"
displayName="SCP Additions"
authors="Bl4ues"
logoFile="logo.png"
displayURL="https://github.com/Bl4ues/SCP-Additions"
description='''
SCP Additions is an SCP survival horror and facility-building mod for Minecraft 1.21.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.
'''

[[dependencies.scp_additions]]
modId="neoforge"
type="required"
versionRange="[21.1.235,)"
ordering="NONE"
side="BOTH"

[[dependencies.scp_additions]]
modId="minecraft"
type="required"
versionRange="[1.21.1]"
ordering="NONE"
side="BOTH"

[[dependencies.scp_additions]]
modId="geckolib"
type="required"
versionRange="[4.9.2,)"
ordering="AFTER"
side="BOTH"

[[dependencies.scp_additions]]
modId="kleiders_custom_renderer"
type="optional"
versionRange="[0,)"
ordering="AFTER"
side="CLIENT"

[[dependencies.scp_additions]]
modId="moremcmeta_emissive_plugin"
type="optional"
versionRange="[0,)"
ordering="AFTER"
side="CLIENT"
""",
        encoding="utf-8",
    )
    old_metadata.unlink(missing_ok=True)

    pack_path = RESOURCES_ROOT / "pack.mcmeta"
    if pack_path.exists():
        pack = json.loads(pack_path.read_text(encoding="utf-8"))
        pack.setdefault("pack", {})["pack_format"] = 34
        pack["pack"]["supported_formats"] = {"min_inclusive": 34, "max_inclusive": 48}
        pack_path.write_text(json.dumps(pack, indent=2) + "\n", encoding="utf-8")


def add_supplier_import(text: str) -> str:
    if "Supplier<" not in text or "import java.util.function.Supplier;" in text:
        return text
    package_end = text.find(";", text.find("package "))
    if package_end < 0:
        return text
    return text[: package_end + 1] + "\n\nimport java.util.function.Supplier;" + text[package_end + 1 :]


def migrate_java_sources() -> None:
    package_replacements = (
        ("net.minecraftforge.api.distmarker", "net.neoforged.api.distmarker"),
        ("net.minecraftforge.eventbus.api", "net.neoforged.bus.api"),
        ("net.minecraftforge.fml", "net.neoforged.fml"),
        ("net.minecraftforge", "net.neoforged.neoforge"),
    )

    for path in JAVA_ROOT.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        original = text

        for old, new in package_replacements:
            text = text.replace(old, new)

        text = text.replace("MinecraftForge", "NeoForge")
        text = text.replace(
            "import net.neoforged.neoforge.registries.RegistryObject;\n", ""
        )
        text = re.sub(r"\bRegistryObject\b", "Supplier", text)
        text = add_supplier_import(text)

        text = re.sub(
            r"new\s+ResourceLocation\(\s*([^,()]+?)\s*,\s*([^()]+?)\s*\)",
            r"ResourceLocation.fromNamespaceAndPath(\1, \2)",
            text,
        )
        text = re.sub(
            r"new\s+ResourceLocation\(\s*([^,()]+?)\s*\)",
            r"ResourceLocation.parse(\1)",
            text,
        )

        if text != original:
            path.write_text(text, encoding="utf-8")


def migrate_data_paths() -> None:
    biome_old = RESOURCES_ROOT / "data/scp_additions/forge/biome_modifier"
    biome_new = RESOURCES_ROOT / "data/scp_additions/neoforge/biome_modifier"
    if biome_old.exists():
        biome_new.parent.mkdir(parents=True, exist_ok=True)
        if biome_new.exists():
            shutil.rmtree(biome_new)
        biome_old.rename(biome_new)
        try:
            biome_old.parent.rmdir()
        except OSError:
            pass

    renames = {
        "advancements": "advancement",
        "functions": "function",
        "item_modifiers": "item_modifier",
        "loot_tables": "loot_table",
        "predicates": "predicate",
        "recipes": "recipe",
        "structures": "structure",
    }
    for namespace in (RESOURCES_ROOT / "data").iterdir():
        if not namespace.is_dir():
            continue
        for old, new in renames.items():
            source = namespace / old
            target = namespace / new
            if source.exists() and not target.exists():
                source.rename(target)

        tags = namespace / "tags"
        if tags.exists():
            for old, new in {
                "blocks": "block",
                "items": "item",
                "entity_types": "entity_type",
                "fluids": "fluid",
                "game_events": "game_event",
            }.items():
                source = tags / old
                target = tags / new
                if source.exists() and not target.exists():
                    source.rename(target)

    for path in RESOURCES_ROOT.rglob("*.json"):
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        updated = text.replace('"forge:', '"neoforge:')
        updated = updated.replace('"forge"', '"neoforge"')
        if updated != text:
            path.write_text(updated, encoding="utf-8")


def update_changelog() -> None:
    path = ROOT / "CHANGELOG.md"
    text = path.read_text(encoding="utf-8")
    heading = "# SCP Additions 3.0.7 — In Development\n"
    addition = """
## NeoForge 1.21.1 port

- Began a feature-parity port of SCP Additions 3.0.7 to NeoForge 1.21.1 on the dedicated `port/neoforge-1.21.1` branch.
- Migrated the project baseline to Java 21, NeoForge 21.1.235, NeoGradle, Minecraft 1.21.1 resource formats, and the NeoForge build of GeckoLib.

"""
    if addition.strip() not in text:
        text = text.replace(heading, heading + addition, 1)
        path.write_text(text, encoding="utf-8")


def write_port_workflow() -> None:
    workflow = ROOT / ".github/workflows/build.yml"
    workflow.write_text(
        """name: NeoForge 1.21.1 Build

on:
  push:
    branches:
      - port/neoforge-1.21.1
  pull_request:
    branches:
      - master

permissions:
  contents: read

jobs:
  build:
    if: github.event_name == 'push' || github.head_ref == 'port/neoforge-1.21.1'
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make Gradle wrapper executable
        run: chmod +x gradlew

      - name: Build NeoForge port
        run: ./gradlew clean build --stacktrace

      - name: Upload compiled JAR
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: scp-additions-neoforge-1.21.1-${{ github.sha }}
          path: build/libs/*.jar
          if-no-files-found: error
""",
        encoding="utf-8",
    )


replace_build_system()
replace_metadata()
migrate_java_sources()
migrate_data_paths()
update_changelog()
write_port_workflow()
print("NeoForge 1.21.1 baseline migration applied.")
