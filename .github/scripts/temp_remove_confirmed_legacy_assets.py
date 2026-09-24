#!/usr/bin/env python3
"""One-off, deliberately explicit list of legacy assets to remove after review."""
import json
import subprocess
from pathlib import Path

root = Path('src/main/resources/assets/scp_classified_directive')
custom_models = [
    'left_accept', 'left_wrong', 'right_accept', 'right_wrong',
    'scp079controlsystem', 'scp1025', 'scp902closed', 'scp902open',
    'scp914body', 'scp914clockworks2',
    'scp914dial1to1', 'scp914dialcoarse', 'scp914dialfine',
    'scp914dialrough', 'scp914dialveryfine',
    'scp914intake', 'scp914intakedoorclosed', 'scp914intakedooropened',
    'scp914keywind', 'scp914output',
    'scp914outputdoorclosed', 'scp914outputdooropened',
    'scp_keycard_reader', 'scp_keycard_reader_left',
    'teslaterminal', 'teslaterminaloff',
]
item_models = [f'level_{level}_keycard_reader' for level in range(1, 7)]
model_paths = ([root / 'models/custom' / (name + '.json')
                for name in custom_models]
               + [root / 'models/item' / (name + '.json')
                  for name in item_models])
texture_names = {
    'block': [
        'diagnostic_table', 'logdoorright', 'scp914body',
        'scp914door', 'scp914intakenoutput',
    ],
    'effect': ['alarm_light_bloom', 'alarm_source_glow'],
    'item': [
        '914door', '914intake', '914output', 'logdoorright',
        'scp914kit', 'terminalitem',
    ],
}
texture_paths = [root / 'textures' / folder / (name + '.png')
                 for folder, names in texture_names.items() for name in names]
sound_paths = [root / 'sounds/scp106dimension.ogg']

# Explicitly avoid the 012 image sequence: Scp012SubliminalOverlay loads
# textures/gui/012_overlay_ + index + .png at runtime. No generic filename
# search will prove dynamically constructed resource names to be unused.
# Also retain all PBR/metadata sidecars unless their base texture is deleted.
tracked = set(subprocess.check_output(['git', 'ls-files', '-z']).decode().strip('\0').split('\0'))
for texture in list(texture_paths):
    for suffix in ('_n', '_s', '_e', '_glowmask', '_normal', '_specular'):
        candidate = texture.with_name(texture.stem + suffix + texture.suffix)
        if candidate.as_posix() in tracked:
            texture_paths.append(candidate)
    mcmeta = Path(str(texture) + '.mcmeta')
    if mcmeta.as_posix() in tracked:
        texture_paths.append(mcmeta)
paths = model_paths + texture_paths + sound_paths
assert len(paths) == len(set(paths)), 'Duplicate deletion path'
for path in paths:
    assert path.is_file(), f'Expected tracked legacy file is missing: {path}'
    assert path.as_posix() in tracked, f'Untracked path: {path}'

manifest = json.loads((root / 'sounds.json').read_text())
for event in manifest.values():
    for sound in event.get('sounds', []):
        name = sound if isinstance(sound, str) else sound.get('name', '')
        assert name.split(':')[-1] != 'scp106dimension', 'Sound still declared in sounds.json'

# A dependency-aware conservative gate. Check ALL remaining source and JSON,
# but ignore the audit/cleanup scripts themselves and the files being removed.
text_ext = {'.java', '.json', '.toml', '.gradle', '.properties', '.cfg',
            '.mcmeta', '.md', '.txt', '.glsl', '.fsh', '.vsh', '.py', '.yml', '.yaml', '.js'}
remaining = []
for source in tracked:
    file = Path(source)
    if file in paths or source.startswith('.github/scripts/temp_') or source.startswith('.github/workflows/temp_'):
        continue
    if file.suffix.lower() not in text_ext:
        continue
    try:
        remaining.append(file.read_text(encoding='utf-8'))
    except (UnicodeDecodeError, OSError):
        pass
all_text = '\n'.join(remaining)
for path in paths:
    relative = path.relative_to(root).as_posix()
    base = relative.rsplit('.', 1)[0]
    if relative.startswith('models/'):
        exact = base[len('models/'):]
        assert '"' + exact + '"' not in all_text and "'" + exact + "'" not in all_text, f'Model still referenced: {path}'
    elif relative.startswith('textures/'):
        exact = base[len('textures/'):]
        assert 'textures/' + exact not in all_text, f'Texture still referenced: {path}'
        assert 'scp_classified_directive:' + exact not in all_text, f'Texture still referenced by model: {path}'
    elif relative.startswith('sounds/'):
        assert path.stem not in all_text, f'Sound still referenced: {path}'

for path in model_paths:
    print('DELETE MODEL', path.as_posix())
for path in texture_paths:
    print('DELETE TEXTURE', path.as_posix())
for path in sound_paths:
    print('DELETE SOUND', path.as_posix())
print('TOTAL', len(paths), 'models', len(model_paths), 'textures', len(texture_paths), 'sounds', len(sound_paths))
subprocess.run(['git', 'rm', '--', *(p.as_posix() for p in paths)], check=True)
