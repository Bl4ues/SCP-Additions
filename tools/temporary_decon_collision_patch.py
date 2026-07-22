from pathlib import Path


path = Path("CHANGELOG.md")
text = path.read_text(encoding="utf-8")
new_entry = (
    "- Rebuilt the Decontamination Checkpoint collision as an invisible model-matched shell, removing the obstruction through the chamber center and allowing any visible section to break the complete structure;\n"
    "- Prevented Decontamination Checkpoints and Tesla Gates from changing animation states while being mined, so breaking progress no longer resets repeatedly;\n"
)
if new_entry not in text:
    marker = (
        "## Interface fixes\n\n"
        "- Moved the SCP-173 blink vignette behind the complete HUD while preserving the Hazmat Suit visor above view effects as the ordering rule for future equipped-item overlays.\n"
    )
    if marker not in text:
        raise SystemExit("Could not locate the 3.0.8 Interface fixes section")
    text = text.replace(
        marker,
        "## Interface fixes\n\n" + new_entry
        + "- Moved the SCP-173 blink vignette behind the complete HUD while preserving the Hazmat Suit visor above view effects as the ordering rule for future equipped-item overlays.\n",
        1,
    )
    path.write_text(text, encoding="utf-8")
