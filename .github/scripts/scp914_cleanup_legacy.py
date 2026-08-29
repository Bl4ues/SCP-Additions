from pathlib import Path
import json
import re

ROOT = Path('.')
JAVA = Path('src/main/java/com/bl4ues/scpclassifieddirective')
RES = Path('src/main/resources')

legacy_fields = {
    'SCP_914BLOCK',
    'SCP_914CLOCKWORKS',
    'SCP_914BODY',
    'SCP_914DIAL_1TO_1',
    'SCP_914_KEY_WIND',
    'SCP_914_INTAKE',
    'SCP_914_OUTPUT',
    'SCP_914_INTAKE_DOOR',
    'SCP_914_OUTPUT_DOOR',
    'SCP_914DIAL_ROUGH',
    'SCP_914DIAL_COARSE',
    'SCP_914DIAL_FINE',
    'SCP_914DIAL_VERY_FINE',
    'SCP_914CLOCKWORKS_2',
    'SCP_914_OUTPUT_DOOR_CLOSED',
    'SCP_914_INTAKE_DOOR_CLOSED',
    'SCP_914_ASSEMBLY_KIT',
}
legacy_classes = {
    'Scp914dialVeryFineBlock',
    'Scp914dialRoughBlock',
    'Scp914dialFineBlock',
    'Scp914dialCoarseBlock',
    'Scp914dial1to1Block',
    'Scp914clockworksBlock',
    'Scp914clockworks2Block',
    'Scp914bodyBlock',
    'Scp914blockBlock',
    'Scp914OutputDoorClosedBlock',
    'Scp914OutputDoorBlock',
    'Scp914OutputBlock',
    'Scp914KeyWindBlock',
    'Scp914IntakeDoorClosedBlock',
    'Scp914IntakeDoorBlock',
    'Scp914IntakeBlock',
    'Scp914AssemblyKitItem',
}
legacy_ids = {
    'scp_914block',
    'scp_914clockworks',
    'scp_914clockworks_2',
    'scp_914body',
    'scp_914dial_1to_1',
    'scp_914dial_rough',
    'scp_914dial_coarse',
    'scp_914dial_fine',
    'scp_914dial_very_fine',
    'scp_914_key_wind',
    'scp_914_intake',
    'scp_914_output',
    'scp_914_intake_door',
    'scp_914_output_door',
    'scp_914_intake_door_closed',
    'scp_914_output_door_closed',
    'scp_914_assembly_kit',
}

# Generated central registries: each declaration/import is deliberately one line.
for rel in [
    'src/main/java/com/bl4ues/scpclassifieddirective/init/ScpClassifiedDirectiveModBlocks.java',
    'src/main/java/com/bl4ues/scpclassifieddirective/init/ScpClassifiedDirectiveModItems.java',
]:
    path = Path(rel)
    lines = path.read_text().splitlines(keepends=True)
    kept = []
    for line in lines:
        if any(field in line for field in legacy_fields):
            continue
        if any(cls in line for cls in legacy_classes):
            continue
        kept.append(line)
    path.write_text(''.join(kept))

# Old generated blocks, assembly kit and SCP-914 component procedures are no
# longer part of the rebuilt single-block machine.
for cls in legacy_classes:
    candidates = list(JAVA.rglob(cls + '.java'))
    for path in candidates:
        if path.exists():
            path.unlink()

procedure_dir = JAVA / 'procedure'
if procedure_dir.exists():
    for path in procedure_dir.glob('Scp914*.java'):
        path.unlink()

# Remove old registry-id-specific resource files while preserving the rebuilt
# scp_914 item/block definition and scp914 GeckoLib model/animation/textures.
resource_roots = [
    RES / 'assets/scp_classified_directive/blockstates',
    RES / 'assets/scp_classified_directive/models/block',
    RES / 'assets/scp_classified_directive/models/item',
    RES / 'assets/scp_classified_directive/textures/block',
    RES / 'assets/scp_classified_directive/textures/item',
    RES / 'data/scp_classified_directive/loot_tables/blocks',
    RES / 'data/scp_classified_directive/recipes',
]
for root in resource_roots:
    if not root.exists():
        continue
    for path in list(root.rglob('*')):
        if not path.is_file():
            continue
        lower = path.name.lower()
        if any(token in lower for token in legacy_ids):
            path.unlink()

# Remove obsolete language entries for the deleted component ids.
lang_dir = RES / 'assets/scp_classified_directive/lang'
if lang_dir.exists():
    for path in lang_dir.glob('*.json'):
        try:
            data = json.loads(path.read_text())
        except Exception:
            continue
        changed = False
        for key in list(data):
            lower = key.lower()
            if any(token in lower for token in legacy_ids):
                del data[key]
                changed = True
        if changed:
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n')

# Remove direct references to the obsolete assembly-kit model/recipe if their
# filenames do not happen to live in one of the conventional roots above.
for path in list(RES.rglob('*')):
    if path.is_file() and any(token in path.name.lower() for token in legacy_ids):
        # Never touch the new scp_914 files; none of the old tokens equals it.
        path.unlink()

# Add a concise 4.0 changelog section. This is a feature rebuild, not a bugfix.
changelog = Path('CHANGELOG.md')
text = changelog.read_text()
section = '''## SCP-914

- Completely rebuilt SCP-914 as a single large GeckoLib machine with hidden multiblock reservation and collision cells instead of the former collection of visible component blocks;
- Added obstruction-aware placement for the full machine footprint, with blocked cells highlighted before placement;
- Reworked the configuration dial into a physical contextual control that can be held and dragged directly on the placed model, with smooth client motion, mechanical detents, server-authoritative settings, gear feedback, and snap-to-setting release behavior;
- Reworked the winding key into a physical contextual **Start** control anchored to the key itself;
- Rebuilt the 15-second refining cycle around the new model animation, physical intake/output chamber volumes, door timing, machine audio, and the existing configurable SCP-914 transformation recipes;
- Removed the obsolete SCP-914 assembly kit, component blocks, component items, generated procedures, models, textures, and contextual-interaction definitions.

'''
if '## SCP-914\n' not in text:
    marker = '## SCP-1176\n'
    if marker not in text:
        raise SystemExit('CHANGELOG SCP-1176 insertion marker missing')
    text = text.replace(marker, section + marker, 1)
    changelog.write_text(text)

# After the old generated layer is gone there must be no Java dependency on its
# fields/classes. Recipe/processor classes are intentionally retained.
remaining_java = []
for path in JAVA.rglob('*.java'):
    content = path.read_text(errors='ignore')
    hits = [name for name in legacy_fields | legacy_classes if name in content]
    if hits:
        remaining_java.append((str(path), sorted(hits)))
if remaining_java:
    for path, hits in remaining_java:
        print('LEGACY JAVA REF:', path, ', '.join(hits))
    raise SystemExit('Legacy SCP-914 Java references remain')

# Text resources should not still target deleted registry ids. CHANGELOG is not
# under resources and therefore historical prose cannot trip this guard.
remaining_resources = []
text_suffixes = {'.json', '.mcmeta', '.toml', '.properties', '.txt', '.lang'}
for path in RES.rglob('*'):
    if not path.is_file() or path.suffix.lower() not in text_suffixes:
        continue
    content = path.read_text(errors='ignore').lower()
    hits = [token for token in legacy_ids if token in content]
    if hits:
        remaining_resources.append((str(path), sorted(hits)))
if remaining_resources:
    for path, hits in remaining_resources:
        print('LEGACY RESOURCE REF:', path, ', '.join(hits))
    raise SystemExit('Legacy SCP-914 resource references remain')

# The temporary patch machinery deletes itself in the final cleanup commit.
for rel in [
    '.github/workflows/scp914-finish-context.yml',
    '.github/scripts/scp914_finish_context.py',
    '.github/scripts/scp914_cleanup_legacy.py',
]:
    path = Path(rel)
    if path.exists():
        path.unlink()
