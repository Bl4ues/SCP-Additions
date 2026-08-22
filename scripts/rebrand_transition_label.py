from pathlib import Path
import re

R = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str, *, required: bool = True) -> None:
    text = path.read_text(encoding='utf-8')
    if old not in text:
        if required and new not in text:
            raise RuntimeError(f'Expected transition-label anchor not found in {path.relative_to(R)}: {old!r}')
        return
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


# Public-facing discovery surfaces should make the rename obvious without turning the
# historical name back into the product name. This is especially useful while existing
# CurseForge/Modrinth/GitHub URLs still use the old slug.
readme = R / 'README.md'
replace_once(readme,
             '# SCP: Classified Directive\n',
             '# SCP: Classified Directive (Formerly SCP Additions)\n')
replace_once(readme,
             'SCP: Classified Directive is an SCP survival-horror and facility-building mod',
             'SCP: Classified Directive (formerly SCP Additions) is an SCP survival-horror and facility-building mod')

mods = R / 'src/main/resources/META-INF/mods.toml'
replace_once(mods,
             'SCP: Classified Directive is an SCP survival horror and facility-building mod',
             'SCP: Classified Directive (formerly SCP Additions) is an SCP survival horror and facility-building mod')

changelog = R / 'CHANGELOG.md'
replace_once(changelog,
             '# SCP: Classified Directive 4.0.0 — In Development\n',
             '# SCP: Classified Directive 4.0.0 — In Development\n\n> **Formerly SCP Additions.**\n')
replace_once(changelog,
             '- SCP: Classified Directive rebrand\n',
             '- SCP: Classified Directive rebrand (formerly SCP Additions)\n')
replace_once(changelog,
             '- Rebranded the project as **SCP: Classified Directive** to reflect its expanded scope;',
             '- Rebranded the project as **SCP: Classified Directive** (formerly **SCP Additions**) to reflect its expanded scope;')

whats_new = R / 'src/main/java/com/bl4ues/scpclassifieddirective/client/MainMenuWhatsNewPanelClient.java'
replace_once(whats_new,
             '            "SCP: Classified Directive rebrand",\n',
             '            "SCP: Classified Directive rebrand (formerly SCP Additions)",\n')

# Make the project rule executable instead of relying on a human remembering to update two
# lists every time. The Changelog Highlights and in-game What's New list must be identical.
changelog_text = changelog.read_text(encoding='utf-8')
match = re.search(r'## Highlights\n(?P<body>.*?)(?:\n## )', changelog_text, flags=re.S)
if not match:
    raise RuntimeError('Could not locate CHANGELOG.md Highlights section')
changelog_highlights = [
    line[2:].strip()
    for line in match.group('body').splitlines()
    if line.startswith('- ')
]

whats_text = whats_new.read_text(encoding='utf-8')
match = re.search(r'private static final List<String> HIGHLIGHTS = List\.of\((?P<body>.*?)\n    \);', whats_text, flags=re.S)
if not match:
    raise RuntimeError("Could not locate What's New HIGHLIGHTS list")
ui_highlights = re.findall(r'^\s*"((?:[^"\\]|\\.)*)",?\s*$', match.group('body'), flags=re.M)
ui_highlights = [bytes(item, 'utf-8').decode('unicode_escape') for item in ui_highlights]

if changelog_highlights != ui_highlights:
    raise RuntimeError(
        'CHANGELOG Highlights and in-game What\'s New differ:\n'
        f'CHANGELOG={changelog_highlights}\nUI={ui_highlights}'
    )

print('transition branding applied and Highlights/What\'s New verified')
