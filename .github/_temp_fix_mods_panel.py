from pathlib import Path

path = Path('src/main/java/net/mcreator/scpadditions/client/PauseMenuModsPanelClient.java')
text = path.read_text()

replacements = [
    ('layout.detailWidth / 2', 'layout.detailWidth() / 2', 2),
    ('layout.detailHeight / 2', 'layout.detailHeight() / 2', 1),
    ('layout.detailWidth - 28', 'layout.detailWidth() - 28', 1),
    ('new DetailLine(ScpFonts.titillium("DESCRIPTION"),\n                ACCENT_BRIGHT, y)',
     'new DetailLine(ScpFonts.titillium("DESCRIPTION").getVisualOrderText(),\n                ACCENT_BRIGHT, y)', 1),
    ('new DetailLine(ScpFonts.titillium(label), ACCENT_BRIGHT, y)',
     'new DetailLine(ScpFonts.titillium(label).getVisualOrderText(),\n                ACCENT_BRIGHT, y)', 1),
]

for old, new, expected in replacements:
    actual = text.count(old)
    if actual != expected:
        raise SystemExit(f'expected {expected} occurrences, found {actual}: {old!r}')
    text = text.replace(old, new, expected)

path.write_text(text)
