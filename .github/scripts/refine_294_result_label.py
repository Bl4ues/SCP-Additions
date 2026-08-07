from pathlib import Path

path = Path('src/main/java/net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java')
text = path.read_text(encoding='utf-8')
old = '''                resultName = humanizeId(string(drink, "id", title));
'''
new = '''                resultName = "Cup of " + humanizeId(string(drink, "id", title));
'''
if text.count(old) != 1:
    raise SystemExit(f'expected one generic cup result label, found {text.count(old)}')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
