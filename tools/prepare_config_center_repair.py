from pathlib import Path

p = Path('tools/apply_config_center_repairs.py')
s = p.read_text(encoding='utf-8')
old = ('s = replace_exact(s, old_post, new_post, "Crosshair init-post ownership", 1)\n'
       's = replace_exact(s, old_post, new_post, "Crosshair render-post ownership", 1)')
new = 's = replace_exact(s, old_post, new_post, "Crosshair screen ownership", 2)'
if old not in s:
    raise SystemExit('repair-script crosshair assertion patch not found')
p.write_text(s.replace(old, new), encoding='utf-8')
