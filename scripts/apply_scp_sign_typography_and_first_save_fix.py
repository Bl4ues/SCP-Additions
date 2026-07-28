from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"{label}: expected source block not found in {path}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


gui = "src/main/java/net/mcreator/scpadditions/client/gui/ScpSignEditorScreen.java"
replace_once(
    gui,
    '''    private static final int PREVIEW_TEXT = 0xFF000000;

    private static final ImageArea CLEARANCE =
            new ImageArea(783, 83, 57, 40);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 273, 355, 48);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 351, 354, 27);
    private static final ImageArea ANOMALY =
            new ImageArea(589, 298, 235, 15);
''',
    '''    private static final int PREVIEW_TEXT = 0xFF000000;
    private static final float FONT_HEIGHT = 8.0F;

    private static final ImageArea CLEARANCE =
            new ImageArea(783, 82, 57, 43);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 265, 355, 56);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 346, 354, 34);
    private static final ImageArea ANOMALY =
            new ImageArea(535, 294, 343, 20);
''',
    "GUI sign text areas",
)
replace_once(
    gui,
    '''        float scale = Math.min(area.width() / (float) textWidth,
                area.height() / 9.0F);
        float x = centered
                ? area.x() + (area.width() - textWidth * scale) * 0.5F
                : area.x();

        graphics.pose().pushPose();
        graphics.pose().translate(x, area.y(), 2.0F);
''',
    '''        float scale = Math.min(area.width() / (float) textWidth,
                area.height() / FONT_HEIGHT);
        float x = centered
                ? area.x() + (area.width() - textWidth * scale) * 0.5F
                : area.x();
        float renderedHeight = FONT_HEIGHT * scale;
        float y = area.y() + (area.height() - renderedHeight) * 0.5F;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 2.0F);
''',
    "GUI vertical text centering",
)

renderer = "src/main/java/net/mcreator/scpadditions/client/ScpSignSupportBlockEntityRenderer.java"
replace_once(
    renderer,
    '''    private static final float FONT_HEIGHT = 9.0F;
''',
    '''    private static final float FONT_HEIGHT = 8.0F;
''',
    "world sign font height",
)
replace_once(
    renderer,
    '''    private static final ImageArea CLEARANCE =
            new ImageArea(783.0F, 83.0F, 57.0F, 40.0F);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64.0F, 273.0F, 355.0F, 48.0F);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65.0F, 351.0F, 354.0F, 27.0F);
    private static final ImageArea ANOMALY =
            new ImageArea(589.0F, 298.0F, 235.0F, 15.0F);
''',
    '''    private static final ImageArea CLEARANCE =
            new ImageArea(783.0F, 82.0F, 57.0F, 43.0F);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64.0F, 265.0F, 355.0F, 56.0F);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65.0F, 346.0F, 354.0F, 34.0F);
    private static final ImageArea ANOMALY =
            new ImageArea(535.0F, 294.0F, 343.0F, 20.0F);
''',
    "world sign text areas",
)
replace_once(
    renderer,
    '''        float imageX = centered ? area.x() + area.width() * 0.5F : area.x();

        poseStack.pushPose();
        poseStack.translate(panelX(imageX), panelY(area.y()), CONTENT_Z);
''',
    '''        float imageX = centered ? area.x() + area.width() * 0.5F : area.x();
        float renderedImageHeight = FONT_HEIGHT * scale
                * IMAGE_HEIGHT / PANEL_HEIGHT;
        float imageY = area.y()
                + (area.height() - renderedImageHeight) * 0.5F;

        poseStack.pushPose();
        poseStack.translate(panelX(imageX), panelY(imageY), CONTENT_Z);
''',
    "world sign vertical text centering",
)

save_packet = "src/main/java/net/mcreator/scpadditions/network/ScpSignSavePacket.java"
replace_once(
    save_packet,
    '''            ItemStack screwdriver =
                    KeycardReaderInteractionEvents.screwdriver(player);
            if (screwdriver.isEmpty()) return;
            if (player.level().getBlockEntity(message.pos)
                    instanceof ScpSignSupportBlockEntity sign) {
                sign.setData(message.data);
            }
''',
    '''            if (player.level().getBlockEntity(message.pos)
                    instanceof ScpSignSupportBlockEntity sign) {
                ItemStack screwdriver =
                        KeycardReaderInteractionEvents.screwdriver(player);
                boolean firstPlacementSave =
                        sign.data().equals(ScpSignData.DEFAULT);
                if (screwdriver.isEmpty() && !firstPlacementSave) return;
                sign.setData(message.data);
            }
''',
    "first placement save authorization",
)

changelog = "CHANGELOG.md"
replace_once(
    changelog,
    '''- Made the SCP Sign editor open immediately after placement and replaced its vanilla cycling controls with styled dropdowns, including an ordered multi-select list with cropped pictogram previews.
''',
    '''- Made the SCP Sign editor open immediately after placement and replaced its vanilla cycling controls with styled dropdowns, including an ordered multi-select list with cropped pictogram previews;
- Increased and realigned the dynamic SCP Sign text to match the Unity reference, and fixed the initial editor save after placing a new sign.
''',
    "SCP sign changelog refinement",
)
