from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# Remove the development-only download, extraction, cache and runtime wiring.
build = read("build.gradle")
build = replace_once(
    build,
    """    flatDir {
        name = 'MoreMcmetaDevRuntime'
        dirs layout.buildDirectory.dir('moremcmeta-dev-runtime')
        dirs layout.buildDirectory.dir('moremcmeta-dev-runtime/plugins')
    }

""",
    "",
    "legacy flatDir repository",
)

prep_start = build.find("// MoreMcmeta bundles five Forge mods")
dependencies_start = build.find("dependencies {", prep_start)
if prep_start < 0 or dependencies_start < 0:
    raise RuntimeError("development runtime preparation block was not found")
build = build[:prep_start] + build[dependencies_start:]

runtime_start_text = """    // Optional client runtime used by runClient to validate emissive textures,
    // PBR materials, shaders and custom player/entity rendering. These are not
    // transitive requirements for users of SCP Additions.
"""
runtime_start = build.find(runtime_start_text)
kleider_line = "    runtimeOnly fg.deobf('maven.modrinth:oaG6aa1j:u7GegV7U') // Kleider's Custom Renderer 7.4.1\n"
kleider_start = build.find(kleider_line, runtime_start)
if runtime_start < 0 or kleider_start < 0:
    raise RuntimeError("optional development runtime dependency block was not found")
replacement_runtime_header = """    // Optional client runtime used by runClient to validate shader compatibility
    // and custom player/entity rendering. These are not transitive requirements
    // for users of SCP Additions.
"""
build = build[:runtime_start] + replacement_runtime_header + build[kleider_start:]
write("build.gradle", build)

# Keep only the resource-processing hooks that remain part of the project.
settings = read("settings.gradle")
settings = replace_once(
    settings,
    "apply from: file('gradle/moremcmeta-dev-runtime.gradle')\n",
    "",
    "settings legacy runtime hook",
)
settings = replace_once(
    settings,
    "apply from: file('gradle/emissive-pbr.gradle')",
    "apply from: file('gradle/native-emissives.gradle')",
    "native emissive hook rename",
)
write("settings.gradle", settings)

# The native pipeline no longer advertises or orders an optional plugin.
mods = read("src/main/resources/META-INF/mods.toml")
plugin_header = "[[dependencies.scp_additions]]\n    modId=\"moremcmeta_emissive_plugin\""
plugin_start = mods.find(plugin_header)
if plugin_start < 0:
    raise RuntimeError("optional emissive plugin metadata was not found")
remove_start = plugin_start
while remove_start > 0 and mods[remove_start - 1] == "\n":
    remove_start -= 1
plugin_end_marker = "    side=\"CLIENT\""
plugin_end = mods.find(plugin_end_marker, plugin_start)
if plugin_end < 0:
    raise RuntimeError("optional emissive plugin metadata end was not found")
plugin_end += len(plugin_end_marker)
while plugin_end < len(mods) and mods[plugin_end] == "\n":
    plugin_end += 1
mods = mods[:remove_start] + "\n" + mods[plugin_end:]
write("src/main/resources/META-INF/mods.toml", mods)

# CI must prove that a normal clean build works without the removed runtime.
workflow = read(".github/workflows/build.yml")
workflow = replace_once(
    workflow,
    "      - name: Build and verify development runtime\n        run: ./gradlew clean verifyMoreMcmetaDevRuntime build --stacktrace",
    "      - name: Build\n        run: ./gradlew clean build --stacktrace",
    "build workflow command",
)
write(".github/workflows/build.yml", workflow)

# Update active documentation and comments so the repository describes the
# native pipeline rather than a dependency that no longer exists.
readme = read("README.md")
readme = replace_once(
    readme,
    "Shader packs with LabPBR support can add material-aware emission and bloom to supported block textures. Built-in emissive block overlays remain full-bright without shaders or MoreMcmeta.",
    "Shader packs with LabPBR support can add material-aware emission and bloom to supported block textures. Built-in emissive block overlays remain full-bright without shaders or additional emissive-texture mods.",
    "README emissive description",
)
write("README.md", readme)

changelog = read("CHANGELOG.md")
changelog = replace_once(
    changelog,
    "Added native full-bright emissive overlays for authored block textures, removing the MoreMcmeta requirement while retaining LabPBR material emission for compatible shader packs;",
    "Added native full-bright emissive overlays for authored block textures without requiring an external emissive-texture mod, while retaining LabPBR material emission for compatible shader packs;",
    "changelog emissive description",
)
write("CHANGELOG.md", changelog)

elevator = read("src/main/java/net/mcreator/scpadditions/facility/elevator/ElevatorAssets.java")
elevator = replace_once(
    elevator,
    "    // textures/block. Shader and MoreMcmeta look up the matching _n and _s maps\n",
    "    // textures/block. Shader packs look up the matching _n and _s maps\n",
    "elevator material comment",
)
write("src/main/java/net/mcreator/scpadditions/facility/elevator/ElevatorAssets.java", elevator)

model_events_path = "src/main/java/net/mcreator/scpadditions/client/render/NativeEmissiveModelEvents.java"
model_events = read(model_events_path)
model_events = replace_once(
    model_events,
    " * of base textures. Keeping the runtime name private prevents MoreMcmeta from\n * drawing the same overlay a second time when it happens to be installed.</p>",
    " * of base textures. Keeping the runtime name private prevents external\n * emissive-texture loaders from drawing the same overlay a second time.</p>",
    "native model wrapper documentation",
)
model_events = replace_once(
    model_events,
    " * therefore visible without MoreMcmeta or a shader pack. Compatible shader\n",
    " * therefore visible without an external emissive-texture mod or shader pack. Compatible shader\n",
    "native model full-bright documentation",
)
write(model_events_path, model_events)

# Rename and clean the resource processor now that it is the native implementation.
old_processor = ROOT / "gradle/emissive-pbr.gradle"
processor = old_processor.read_text(encoding="utf-8")
processor = replace_once(
    processor,
    "// from processed resources after native conversion so MoreMcmeta cannot draw a\n// second copy of the same overlay when it happens to be installed. Entity masks\n",
    "// from processed resources after native conversion so external emissive-texture\n// loaders cannot draw a second copy of the same overlay. Entity masks\n",
    "processor duplicate-overlay documentation",
)
processor = replace_once(
    processor,
    "            // MoreMcmeta treats opaque black as a non-emissive mask value, but\n",
    "            // Legacy emissive-mask conventions treat opaque black as a non-emissive value, but\n",
    "processor black-mask documentation",
)
processor = replace_once(
    processor,
    "            // Avoid duplicate overlays from MoreMcmeta Emissive. The public JAR\n            // contains only the private native mask and generated LabPBR map for\n            // baked block textures.\n",
    "            // The public JAR contains only the private native mask and generated\n            // LabPBR map for baked block textures, preventing duplicate overlays\n            // from external emissive-texture loaders.\n",
    "processor output documentation",
)
write("gradle/native-emissives.gradle", processor)
old_processor.unlink()

legacy_runtime = ROOT / "gradle/moremcmeta-dev-runtime.gradle"
if not legacy_runtime.is_file():
    raise RuntimeError("legacy development runtime script was not found")
legacy_runtime.unlink()

# Remove this one-shot integration machinery before the final commit.
for temporary in (
    "tools/hotfix/RUN_REMOVE_LEGACY_EMISSIVE_RUNTIME",
    "tools/hotfix/remove_legacy_emissive_runtime.py",
    ".github/workflows/remove-legacy-emissive-runtime.yml",
):
    path = ROOT / temporary
    if path.exists():
        path.unlink()

# Fail the integration if any active tracked text file or path still names the
# removed dependency. Deleted one-shot files are skipped automatically.
text_suffixes = {
    ".gradle", ".md", ".toml", ".yml", ".yaml", ".java", ".json",
    ".txt", ".properties", ".mcmeta", ".py", ".sh",
}
remaining = []
tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT)
for raw_path in tracked.split(b"\0"):
    if not raw_path:
        continue
    relative = raw_path.decode("utf-8")
    path = ROOT / relative
    if not path.exists():
        continue
    if "moremcmeta" in relative.lower():
        remaining.append(relative)
        continue
    if path.suffix.lower() not in text_suffixes:
        continue
    content = path.read_text(encoding="utf-8", errors="ignore")
    if "moremcmeta" in content.lower():
        remaining.append(relative)

if remaining:
    raise RuntimeError("legacy dependency references remain: " + ", ".join(sorted(set(remaining))))
