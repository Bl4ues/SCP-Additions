from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Missing expected block: {label}")
    return text.replace(old, new, 1)

screen_path = Path("src/main/java/net/mcreator/scpadditions/client/gui/ScpSignEditorScreen.java")
screen = screen_path.read_text(encoding="utf-8")
screen = replace_once(screen,
'''    private static final float FONT_HEIGHT = 7.0F;
    private static final int TEXT_FIELD_Y_OFFSET = 5;

    private static final ImageArea CLEARANCE =
            new ImageArea(774, 85, 74, 52);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 257, 382, 72);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 340, 380, 46);
    private static final ImageArea ANOMALY =
            new ImageArea(522, 306, 363, 28);
''',
'''    private static final float FONT_HEIGHT = 7.5F;
    private static final int TEXT_FIELD_Y_OFFSET = 5;

    private static final ImageArea CLEARANCE =
            new ImageArea(778, 85, 66, 47);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 261, 370, 64);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 343, 365, 40);
    private static final ImageArea ANOMALY =
            new ImageArea(528, 299, 351, 23);
''', "screen text regions")
screen = replace_once(screen,
'''                drawSmallIcon(graphics, option.texture(), iconX,
                        getY() + 1, 18);
''',
'''                drawSmallIcon(graphics, option.texture(), iconX,
                        getY() - 1, 18);
''', "closed selector icon offset")
screen = replace_once(screen,
'''                graphics.drawString(font, ScpFonts.roboto(clipped),
                        getX() + ICON_SIZE + 7,
                        rowY + (ROW_HEIGHT - 8) / 2, textColor, false);
''',
'''                graphics.drawString(font, ScpFonts.roboto(clipped),
                        getX() + ICON_SIZE + 7,
                        rowY + (ROW_HEIGHT - 8) / 2 + 2, textColor, false);
''', "expanded trait label offset")
screen = replace_once(screen,
'''                            getX() + getWidth() - 12,
                            rowY + (ROW_HEIGHT - 8) / 2, ACCENT_TEXT);
''',
'''                            getX() + getWidth() - 12,
                            rowY + (ROW_HEIGHT - 8) / 2 + 2, ACCENT_TEXT);
''', "expanded selection order offset")
screen_path.write_text(screen, encoding="utf-8")

renderer_path = Path("src/main/java/net/mcreator/scpadditions/client/ScpSignSupportBlockEntityRenderer.java")
renderer = renderer_path.read_text(encoding="utf-8")
renderer = replace_once(renderer,
'''    private static final float FONT_HEIGHT = 7.0F;
    private static final int TEXT_COLOR = 0xFF000000;

    private static final ImageArea CLEARANCE =
            new ImageArea(774.0F, 85.0F, 74.0F, 52.0F);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64.0F, 257.0F, 382.0F, 72.0F);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65.0F, 340.0F, 380.0F, 46.0F);
    private static final ImageArea ANOMALY =
            new ImageArea(522.0F, 306.0F, 363.0F, 28.0F);
''',
'''    private static final float FONT_HEIGHT = 7.5F;
    private static final int TEXT_COLOR = 0xFF000000;

    private static final ImageArea CLEARANCE =
            new ImageArea(778.0F, 85.0F, 66.0F, 47.0F);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64.0F, 261.0F, 370.0F, 64.0F);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65.0F, 343.0F, 365.0F, 40.0F);
    private static final ImageArea ANOMALY =
            new ImageArea(528.0F, 299.0F, 351.0F, 23.0F);
''', "world text regions")
renderer_path.write_text(renderer, encoding="utf-8")

changelog_path = Path("CHANGELOG.md")
changelog = changelog_path.read_text(encoding="utf-8")
anchor = "- Upgraded the SCP Sign with a Screwdriver editor for the SCP number, containment class, clearance level, anomaly type, and up to three anomaly-trait pictograms rendered behind its glass;\n"
addition = anchor + "- Fine-tuned the SCP Sign typography and Anomaly Trait selector alignment against direct SCP Unity comparisons.\n"
if addition not in changelog:
    changelog = replace_once(changelog, anchor, addition, "facility signs changelog anchor")
changelog_path.write_text(changelog, encoding="utf-8")
