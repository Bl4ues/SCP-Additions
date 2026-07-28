from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if text.count(old) != 1:
        raise SystemExit(f"Expected exactly one {label} block in {path}, found {text.count(old)}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


screen = Path("src/main/java/net/mcreator/scpadditions/client/gui/ScpSignEditorScreen.java")
renderer = Path("src/main/java/net/mcreator/scpadditions/client/ScpSignSupportBlockEntityRenderer.java")
changelog = Path("CHANGELOG.md")

old_screen_areas = '''    private static final int PREVIEW_TEXT = 0xFF000000;
    private static final float FONT_HEIGHT = 8.0F;

    private static final ImageArea CLEARANCE =
            new ImageArea(783, 82, 57, 43);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 265, 355, 56);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 346, 354, 34);
    private static final ImageArea ANOMALY =
            new ImageArea(535, 294, 343, 20);
'''
new_screen_areas = '''    private static final int PREVIEW_TEXT = 0xFF000000;
    private static final float FONT_HEIGHT = 7.0F;
    private static final int TEXT_FIELD_Y_OFFSET = 5;

    private static final ImageArea CLEARANCE =
            new ImageArea(774, 85, 74, 52);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 257, 382, 72);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 340, 380, 46);
    private static final ImageArea ANOMALY =
            new ImageArea(522, 306, 363, 28);
'''
replace_once(screen, old_screen_areas, new_screen_areas,
             "screen text-area constants")

for field_name in ("scpNumberField", "customContainmentField", "customAnomalyField"):
    old = f'''        {field_name} = configureField(new EditBox(font, fieldX, y,\n'''
    new = f'''        {field_name} = configureField(new EditBox(font, fieldX,\n                y + TEXT_FIELD_Y_OFFSET,\n'''
    replace_once(screen, old, new, f"{field_name} vertical alignment")

old_renderer_areas = '''    private static final float CONTENT_Z = 15.83F / 16.0F;
    private static final float FONT_HEIGHT = 8.0F;
    private static final int TEXT_COLOR = 0xFF000000;

    private static final ImageArea CLEARANCE =
            new ImageArea(783.0F, 82.0F, 57.0F, 43.0F);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64.0F, 265.0F, 355.0F, 56.0F);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65.0F, 346.0F, 354.0F, 34.0F);
    private static final ImageArea ANOMALY =
            new ImageArea(535.0F, 294.0F, 343.0F, 20.0F);
'''
new_renderer_areas = '''    private static final float CONTENT_Z = 15.83F / 16.0F;
    private static final float FONT_HEIGHT = 7.0F;
    private static final int TEXT_COLOR = 0xFF000000;

    private static final ImageArea CLEARANCE =
            new ImageArea(774.0F, 85.0F, 74.0F, 52.0F);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64.0F, 257.0F, 382.0F, 72.0F);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65.0F, 340.0F, 380.0F, 46.0F);
    private static final ImageArea ANOMALY =
            new ImageArea(522.0F, 306.0F, 363.0F, 28.0F);
'''
replace_once(renderer, old_renderer_areas, new_renderer_areas,
             "world-renderer text-area constants")

old_changelog = "- Increased and realigned the dynamic SCP Sign text to match the Unity reference, and fixed the initial editor save after placing a new sign."
new_changelog = "- Fine-tuned the dynamic SCP Sign text scale and positioning against side-by-side Unity references, corrected the free-text field alignment, and fixed the initial editor save after placing a new sign."
replace_once(changelog, old_changelog, new_changelog,
             "SCP Sign changelog entry")
