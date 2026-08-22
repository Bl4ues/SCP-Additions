from pathlib import Path

root = Path(__file__).resolve().parents[1]
path = root / 'scripts/rebrand_classified_directive.py'
text = path.read_text(encoding='utf-8')

old = "    if tmp.exists(): tmp.rename(new/'inventory')\n"
new = """    if tmp.exists():
        target = new/'inventory'; target.mkdir(parents=True, exist_ok=True)
        for p in sorted(tmp.rglob('*')):
            if not p.is_file(): continue
            q = target/p.relative_to(tmp); q.parent.mkdir(parents=True, exist_ok=True)
            if q.exists():
                if q.read_bytes() != p.read_bytes(): raise RuntimeError(f'java package collision: {q.relative_to(j)}')
                p.unlink()
            else: shutil.move(str(p), str(q))
        shutil.rmtree(tmp, ignore_errors=True)
"""
if old not in text:
    raise RuntimeError('expected inventory rename line was not found')
text = text.replace(old, new, 1)
path.write_text(text, encoding='utf-8')

# Upper-snake identifiers are code branding, not persisted registry IDs.
for java_file in (root / 'src/main/java').rglob('*.java'):
    source = java_file.read_text(encoding='utf-8')
    branded = source.replace('SCP_ADDITIONS', 'SCP_CLASSIFIED_DIRECTIVE')
    if branded != source:
        java_file.write_text(branded, encoding='utf-8')

# The command itself is migrated by the main pass because it is a quoted literal.
# Keep project-facing documentation synchronized with that new command root.
readme = root / 'README.md'
readme_text = readme.read_text(encoding='utf-8')
readme.write_text(readme_text.replace('/scpadditions', '/scp_classified_directive'), encoding='utf-8')

print('rebrand runner hotfix applied')
