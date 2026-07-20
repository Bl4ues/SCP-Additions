from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"


def add_import(text: str, line: str) -> str:
    if line in text:
        return text
    package_end = text.find("\n", text.find("package ")) + 1
    return text[:package_end] + "\n" + line + "\n" + text[package_end:]


def rewrite_build() -> None:
    build = ROOT / "build.gradle"
    original = build.read_text(encoding="utf-8")
    if "net.minecraftforge.gradle" not in original:
        return

    process_start = original.index("processResources {")
    resource_pipeline = original[process_start:]
    resource_pipeline = resource_pipeline.replace(
        "inputs.property 'mod_version', project.version\n"
        "    filesMatching('META-INF/mods.toml') {\n"
        "        expand mod_version: project.version\n"
        "    }",
        "def replaceProperties = [\n"
        "            minecraft_version: minecraft_version,\n"
        "            minecraft_version_range: minecraft_version_range,\n"
        "            neo_version: neo_version,\n"
        "            loader_version_range: loader_version_range,\n"
        "            mod_id: mod_id,\n"
        "            mod_name: mod_name,\n"
        "            mod_license: mod_license,\n"
        "            mod_version: mod_version\n"
        "    ]\n"
        "    inputs.properties replaceProperties\n"
        "    filesMatching('META-INF/neoforge.mods.toml') {\n"
        "        expand replaceProperties\n"
        "    }",
    )

    prefix = """plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.gradle.userdev' version '7.1.38'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

sourceSets.main.resources {
    srcDir('src/generated/resources')
    exclude('**/*.bbmodel')
    exclude('src/generated/**/.cache')
}

minecraft.accessTransformers.file rootProject.file('src/main/resources/META-INF/accesstransformer.cfg')

runs {
    configureEach {
        systemProperty 'forge.logging.markers', 'REGISTRIES'
        systemProperty 'forge.logging.console.level', 'debug'
        workingDirectory project.layout.projectDirectory.dir('run').dir(name)
        modSource project.sourceSets.main
    }

    client {
        systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
    }

    server {
        systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
        argument '--nogui'
    }

    gameTestServer {
        systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id
    }

    data {
        arguments.addAll '--mod', project.mod_id, '--all', '--output',
                file('src/generated/resources/').getAbsolutePath(), '--existing',
                file('src/main/resources/').getAbsolutePath()
    }
}

repositories {
    maven {
        name = 'GeckoLib'
        url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/'
        content { includeGroup('software.bernie.geckolib') }
    }
}

configurations {
    runtimeClasspath.extendsFrom localRuntime
}

dependencies {
    implementation "net.neoforged:neoforge:${neo_version}"
    implementation "software.bernie.geckolib:geckolib-neoforge-${minecraft_version}:${geckolib_version}"
}

"""
    build.write_text(prefix + resource_pipeline, encoding="utf-8")

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
        """org.gradle.jvmargs=-Xmx5G
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.caching=true

neogradle.subsystems.parchment.minecraftVersion=1.21.1
neogradle.subsystems.parchment.mappingsVersion=2024.11.17

minecraft_version=1.21.1
minecraft_version_range=[1.21.1]
neo_version=21.1.235
loader_version_range=[1,)
geckolib_version=4.9.2

mod_id=scp_additions
mod_name=SCP Additions
mod_license=Creative Commons Attribution-ShareAlike 3.0
mod_version=3.0.7-neoforge-alpha.1
mod_group_id=com.bl4ues.scpadditions
""",
        encoding="utf-8",
    )

    (ROOT / "gradle/wrapper/gradle-wrapper.properties").write_text(
        """distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""",
        encoding="utf-8",
    )


def rewrite_metadata() -> None:
    meta = ROOT / "src/main/resources/META-INF"
    old = meta / "mods.toml"
    if old.exists():
        old.unlink()

    (meta / "neoforge.mods.toml").write_text(
        """modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="Bl4ues"
logoFile="logo.png"
description='''
SCP Additions is an SCP survival horror and facility-building mod for Minecraft 1.21.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.
'''

[[accessTransformers]]
file="META-INF/accesstransformer.cfg"

[[dependencies.${mod_id}]]
modId="neoforge"
type="required"
versionRange="[${neo_version},)"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
type="required"
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="geckolib"
type="required"
versionRange="[4.9.2,)"
ordering="AFTER"
side="BOTH"

[[dependencies.${mod_id}]]
modId="kleiders_custom_renderer"
type="optional"
versionRange="[0,)"
ordering="AFTER"
side="CLIENT"

[[dependencies.${mod_id}]]
modId="moremcmeta_emissive_plugin"
type="optional"
versionRange="[0,)"
ordering="AFTER"
side="CLIENT"
""",
        encoding="utf-8",
    )


def rewrite_java_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    original = text

    replacements = (
        ("net.minecraftforge.api.distmarker.", "net.neoforged.api.distmarker."),
        ("net.minecraftforge.eventbus.api.", "net.neoforged.bus.api."),
        ("net.minecraftforge.fml.common.Mod", "net.neoforged.fml.common.Mod"),
        ("net.minecraftforge.fml.event.lifecycle.", "net.neoforged.fml.event.lifecycle."),
        ("net.minecraftforge.fml.loading.", "net.neoforged.fml.loading."),
        ("net.minecraftforge.fml.ModList", "net.neoforged.fml.ModList"),
        ("net.minecraftforge.client.event.", "net.neoforged.neoforge.client.event."),
        ("net.minecraftforge.client.gui.overlay.", "net.neoforged.neoforge.client.gui.overlay."),
        ("net.minecraftforge.client.extensions.common.", "net.neoforged.neoforge.client.extensions.common."),
        ("net.minecraftforge.common.capabilities.", "net.neoforged.neoforge.capabilities."),
        ("net.minecraftforge.common.util.", "net.neoforged.neoforge.common.util."),
        ("net.minecraftforge.common.extensions.", "net.neoforged.neoforge.common.extensions."),
        ("net.minecraftforge.common.MinecraftForge", "net.neoforged.neoforge.common.NeoForge"),
        ("net.minecraftforge.common.", "net.neoforged.neoforge.common."),
        ("net.minecraftforge.event.entity.", "net.neoforged.neoforge.event.entity."),
        ("net.minecraftforge.event.level.", "net.neoforged.neoforge.event.level."),
        ("net.minecraftforge.event.server.", "net.neoforged.neoforge.event.server."),
        ("net.minecraftforge.event.RegisterCommandsEvent", "net.neoforged.neoforge.event.RegisterCommandsEvent"),
        ("net.minecraftforge.event.AddReloadListenerEvent", "net.neoforged.neoforge.event.AddReloadListenerEvent"),
        ("net.minecraftforge.items.", "net.neoforged.neoforge.items."),
        ("net.minecraftforge.registries.", "net.neoforged.neoforge.registries."),
        ("net.minecraftforge.server.", "net.neoforged.neoforge.server."),
    )
    for before, after in replacements:
        text = text.replace(before, after)

    text = text.replace("import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;\n", "")
    text = text.replace("MinecraftForge.EVENT_BUS", "NeoForge.EVENT_BUS")

    text = text.replace("@Mod.EventBusSubscriber", "@EventBusSubscriber")
    text = text.replace("Mod.EventBusSubscriber.Bus.MOD", "EventBusSubscriber.Bus.MOD")
    text = text.replace("Mod.EventBusSubscriber.Bus.FORGE", "EventBusSubscriber.Bus.GAME")
    if "@EventBusSubscriber" in text:
        text = add_import(text, "import net.neoforged.fml.common.EventBusSubscriber;")

    if "RegistryObject<" in text:
        text = text.replace("import net.neoforged.neoforge.registries.RegistryObject;\n", "")
        text = text.replace("RegistryObject<", "Supplier<")
        text = add_import(text, "import java.util.function.Supplier;")

    if "ForgeRegistries" in text:
        text = text.replace(
            "import net.neoforged.neoforge.registries.ForgeRegistries;\n",
            "import net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.core.registries.Registries;\n",
        )
        values = {
            "ITEMS": "ITEM", "BLOCKS": "BLOCK", "SOUND_EVENTS": "SOUND_EVENT",
            "ENTITY_TYPES": "ENTITY_TYPE", "MOB_EFFECTS": "MOB_EFFECT",
            "MENU_TYPES": "MENU", "PARTICLE_TYPES": "PARTICLE_TYPE",
            "BLOCK_ENTITY_TYPES": "BLOCK_ENTITY_TYPE", "CREATIVE_MODE_TABS": "CREATIVE_MODE_TAB",
        }
        for old, new in values.items():
            text = text.replace(f"ForgeRegistries.{old}", f"BuiltInRegistries.{new}")
        for old, new in values.items():
            text = text.replace(f"ForgeRegistries.Keys.{old}", f"Registries.{new}")

    text = re.sub(r"new ResourceLocation\(([^,\n()]+),\s*([^\n()]+)\)", r"ResourceLocation.fromNamespaceAndPath(\1, \2)", text)
    text = re.sub(r"new ResourceLocation\(([^,\n()]+)\)", r"ResourceLocation.parse(\1)", text)

    if path.as_posix().endswith("/ScpAdditionsMod.java"):
        text = text.replace("public ScpAdditionsMod() {", "public ScpAdditionsMod(IEventBus bus) {")
        text = text.replace("        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();\n", "")

    if path.as_posix().endswith("/FacilityLegacyMappings.java"):
        text = text.replace("replacement != null && replacement.isPresent()", "replacement != null")

    if path.as_posix().endswith("/UBlocksModule.java"):
        text = text.replace(
            "item != null && isLegacyWallDetailPath(item.getId().getPath())",
            "item != null && isLegacyWallDetailPath(BuiltInRegistries.ITEM.getKey(item.get()).getPath())",
        )

    if path.as_posix().endswith("/ScpAdditionsModItems.java"):
        text = text.replace(
            "return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));",
            "return REGISTRY.register(BuiltInRegistries.BLOCK.getKey(block.get()).getPath(), () -> new BlockItem(block.get(), new Item.Properties()));",
        )

    if text != original:
        path.write_text(text, encoding="utf-8")
        return True
    return False


def rewrite_java() -> tuple[int, int]:
    files = list(JAVA_ROOT.rglob("*.java"))
    changed = sum(1 for path in files if rewrite_java_file(path))
    return changed, len(files)


def write_status(changed: int, total: int) -> None:
    (ROOT / "NEOFORGE_PORT_STATUS.md").write_text(
        f"""# NeoForge 1.21.1 Port — SCP Additions 3.0.7

Branch: `neoforge-1.21.1-port-3.0.7`

## Clean baseline

This port starts from the audited SCP Additions 3.0.7 master cleanup, including the generic SCP-294 cup consolidation, old-world remapping, legacy configuration normalization, and removal of unreachable Java/assets.

## Bootstrap applied

- Minecraft 1.21.1
- NeoForge 21.1.235
- NeoGradle 7.1.38
- Java 21
- GeckoLib NeoForge 4.9.2
- NeoForge metadata and retained access transformer
- Original custom resource-processing pipeline retained
- Mechanical Forge-to-NeoForge migration touched {changed} of {total} Java files

## Completion rule

This branch is not feature-complete until it builds, launches client and dedicated server, preserves old-world mappings, and passes a parity checklist covering every SCP, inventory/crafting/status/codex workflow, configuration editor, facility block, animated door, keycard reader, survival system, sound, overlay, packet path, and compatibility hook.
""",
        encoding="utf-8",
    )


if __name__ == "__main__":
    rewrite_build()
    rewrite_metadata()
    changed_files, total_files = rewrite_java()
    write_status(changed_files, total_files)
    print(f"NeoForge bootstrap changed {changed_files}/{total_files} Java files")
