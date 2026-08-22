from pathlib import Path

path = Path(__file__).resolve().parent / 'rebrand_classified_directive.py'
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
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('rebrand runner hotfix applied')
