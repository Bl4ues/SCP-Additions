#!/usr/bin/env python3
"""Conservative, read-only inventory of potentially unused game resources."""
import collections
import json
import re
import subprocess
from pathlib import Path

root = Path.cwd()
namespace = 'scp_classified_directive'
asset_root = Path('src/main/resources/assets') / namespace
tracked = [Path(p) for p in subprocess.check_output(['git', 'ls-files', '-z']).decode().split('\0') if p]
text_suffixes = {'.java', '.json', '.toml', '.gradle', '.properties', '.cfg', '.mcmeta', '.md', '.txt', '.glsl', '.fsh', '.vsh', '.py', '.yml', '.yaml', '.js'}
text_files = {}
for path in tracked:
    if str(path).startswith('.github/scripts/temp_unused_resource_audit') or str(path).startswith('.github/workflows/temp_unused_resource_audit'):
        continue
    if path.suffix.lower() not in text_suffixes:
        continue
    try:
        text_files[str(path)] = path.read_text(encoding='utf-8')
    except (UnicodeDecodeError, OSError):
        continue

sounds_manifest = json.loads((asset_root / 'sounds.json').read_text(encoding='utf-8'))
manifest_sounds = set()
for name, entry in sounds_manifest.items():
    for sound in entry.get('sounds', []):
        if isinstance(sound, str):
            full = sound
        elif isinstance(sound, dict):
            full = sound.get('name', '')
        else:
            continue
        if full.startswith(namespace + ':'):
            full = full.split(':', 1)[1]
        manifest_sounds.add(full)

counts = collections.Counter()
candidates = collections.defaultdict(list)
review = collections.defaultdict(list)
for path in tracked:
    if not path.is_relative_to(asset_root):
        continue
    rel = path.relative_to(asset_root).as_posix()
    name = path.name
    if rel.startswith('textures/') and path.suffix.lower() in ('.png', '.tga', '.jpg', '.jpeg', '.webp'):
        category = 'TEXTURE'
        ident = rel[len('textures/'):].rsplit('.', 1)[0]
        # PBR companion images, atlases and textures with dedicated metadata must be protected.
        pbr_suffix = ('_n', '_s', '_glowmask', '_e', '_emissive', '_specular', '_normal', '_pbr')
        if ident.endswith(pbr_suffix) or (root / (str(path) + '.mcmeta')).exists():
            review['PBR_OR_METADATA'].append(rel)
            continue
    elif rel.startswith('sounds/') and path.suffix.lower() in ('.ogg', '.wav'):
        category = 'SOUND'
        ident = rel[len('sounds/'):].rsplit('.', 1)[0]
        if ident in manifest_sounds:
            counts['manifest_sound'] += 1
            continue
    elif rel.startswith('geo/') and rel.endswith('.geo.json'):
        category = 'GEO'
        ident = rel
    elif rel.startswith('animations/') and rel.endswith('.animation.json'):
        category = 'ANIMATION'
        ident = rel
    elif rel.startswith('models/') and rel.endswith('.json'):
        category = 'MODEL'
        ident = rel[len('models/'):].rsplit('.', 1)[0]
    else:
        continue
    counts[category] += 1
    logical = rel.rsplit('.', 1)[0]
    basename = path.stem
    if basename.endswith('.geo') or basename.endswith('.animation'):
        basename = basename.rsplit('.', 1)[0]
    # Exact resource references and basename mentions in source/JSON/metadata count as use.
    # Merely missing from code search is NOT proof that a resource is safe to delete.
    exact_tokens = [rel, logical]
    if category == 'TEXTURE':
        exact_tokens += [ident, 'textures/' + ident]
    if category == 'SOUND':
        exact_tokens += [ident, ident + '.ogg', 'sounds/' + ident]
    if category == 'MODEL':
        exact_tokens += [ident, 'models/' + ident]
    exact_hit = []
    named_hit = []
    pat = re.compile(r'(?<![A-Za-z0-9_])' + re.escape(basename) + r'(?![A-Za-z0-9_])', re.IGNORECASE)
    for source, content in text_files.items():
        if source == str(path):
            continue
        if any(token in content for token in exact_tokens):
            exact_hit.append(source)
        elif pat.search(content):
            named_hit.append(source)
    if exact_hit:
        counts[category + '_explicit'] += 1
    elif named_hit:
        review[category + '_NAME_ONLY'].append(rel)
    else:
        candidates[category].append(rel)

print('=== AUDIT START ===')
print('HEAD', subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip())
print('TEXT_FILES', len(text_files), 'MANIFEST_SOUND_PATHS', len(manifest_sounds))
print('COUNTS', dict(sorted(counts.items())))
for kind in ('SOUND', 'GEO', 'ANIMATION', 'MODEL', 'TEXTURE'):
    names = sorted(candidates[kind])
    print(f'=== CANDIDATE {kind} ({len(names)}) ===')
    for name in names:
        print('CANDIDATE', kind, (asset_root / name).as_posix(), 'BYTES', (root / asset_root / name).stat().st_size)
for kind, names in sorted(review.items()):
    print(f'=== REVIEW {kind} ({len(names)}) ===')
    for name in sorted(names)[:60]:
        print('REVIEW', kind, (asset_root / name).as_posix())
print('=== AUDIT END ===')
