from pathlib import Path

changelog = Path("CHANGELOG.md")
text = changelog.read_text(encoding="utf-8")
anchor = (
    "- Added a default-enabled module that renders Action Bar messages in Roboto;\n"
)
addition = (
    "- Added native full-bright emissive overlays for authored block textures, "
    "removing the MoreMcmeta requirement while retaining LabPBR material emission "
    "for compatible shader packs;\n"
)

count = text.count(anchor)
if count != 1:
    raise RuntimeError(
        f"Expected one Action Bar changelog anchor, found {count}"
    )

if addition not in text:
    text = text.replace(anchor, anchor + addition, 1)
    changelog.write_text(text, encoding="utf-8")
