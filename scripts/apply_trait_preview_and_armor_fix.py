from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"{label}: expected source block not found in {path}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


screen_path = "src/main/java/net/mcreator/scpadditions/client/gui/ScpSignEditorScreen.java"
replace_once(
    screen_path,
    '''        private void drawSmallIcon(GuiGraphics graphics,
                ResourceLocation texture, int x, int y, int size) {
            if (!resourceExists(texture)) return;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 1.0F);
            graphics.pose().scale(size / 256.0F,
                    size / 256.0F, 1.0F);
            graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                    256, 256, 256, 256);
            graphics.pose().popPose();
        }
''',
    '''        private void drawSmallIcon(GuiGraphics graphics,
                ResourceLocation texture, int x, int y, int size) {
            if (!resourceExists(texture)) return;

            // Only sample the pictogram inside the hazard triangle. The source
            // vertices are (128, 44), (52, 176) and (204, 176); rendering one
            // horizontal strip per destination row preserves that triangular
            // crop without bringing the border or caption into the preview.
            final int sourceTopX = 128;
            final int sourceTopY = 44;
            final int sourceBaseY = 176;
            final int sourceHalfWidth = 76;
            final int sourceHeight = sourceBaseY - sourceTopY;

            for (int row = 0; row < size; row++) {
                float progress = (row + 1.0F) / size;
                int sourceY = sourceTopY + Math.min(sourceHeight - 1,
                        (int) (progress * sourceHeight));
                int halfWidth = Math.max(1,
                        (int) Math.ceil(sourceHalfWidth * progress));
                int sourceX = sourceTopX - halfWidth;
                int sourceWidth = halfWidth * 2;

                int destinationHalfWidth = Math.max(1,
                        (int) Math.ceil(size * 0.5F * progress));
                int destinationWidth = Math.min(size,
                        destinationHalfWidth * 2);
                int destinationX = x + (size - destinationWidth) / 2;

                graphics.pose().pushPose();
                graphics.pose().translate(destinationX, y + row, 1.0F);
                graphics.pose().scale(destinationWidth / (float) sourceWidth,
                        1.0F, 1.0F);
                graphics.blit(texture, 0, 0, (float) sourceX,
                        (float) sourceY, sourceWidth, 1, 256, 256);
                graphics.pose().popPose();
            }
        }
''',
    "triangular anomaly trait preview",
)

vitals_path = "src/main/java/net/mcreator/scpadditions/vitals/client/ClientVitalsEvents.java"
replace_once(
    vitals_path,
    '''        if (!player.isCreative() && !player.isSpectator()
                && VitalsModule.healthHudEnabled()
                && event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
        }
''',
    '''        if (!player.isCreative() && !player.isSpectator()
                && VitalsModule.healthHudEnabled()
                && (event.getOverlay().id().equals(
                        VanillaGuiOverlay.PLAYER_HEALTH.id())
                || event.getOverlay().id().equals(
                        VanillaGuiOverlay.ARMOR_LEVEL.id()))) {
            event.setCanceled(true);
        }
''',
    "custom health armor overlay suppression",
)
