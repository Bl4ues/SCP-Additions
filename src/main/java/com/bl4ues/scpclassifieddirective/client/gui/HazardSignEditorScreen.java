package com.bl4ues.scpclassifieddirective.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.facility.ScpSignHazards;
import com.bl4ues.scpclassifieddirective.network.HazardSignSavePacket;

import java.util.List;

/** Dedicated portrait sign editor using the Facility Sign visual language. */
public final class HazardSignEditorScreen extends Screen {
    private static final ResourceLocation BASE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/screens/scpsign/hazard_warning.png");
    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 1024;
    private static final ImageArea PICTOGRAM =
            new ImageArea(96, 365, 448, 448);

    private static final int PANEL_BACKGROUND = 0xF01B2024;
    private static final int PANEL_EDGE = 0xFF657078;
    private static final int FIELD_BACKGROUND = 0xFF13181C;
    private static final int FIELD_EDGE = 0xFF4B555C;
    private static final int CONTROL_BACKGROUND = 0xFF343D43;
    private static final int CONTROL_HOVER = 0xFF56636B;
    private static final int CONTROL_EDGE = 0xFF667178;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int ACCENT_TEXT = 0xFFE5D49A;
    private static final int TEXT_PRIMARY = 0xFFE4E8EA;
    private static final int TEXT_MUTED = 0xFF879097;

    private static final int ROW_HEIGHT = 32;

    private final BlockPos signPos;
    private String selectedHazard;
    private int scrollOffset;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int previewLeft;
    private int previewTop;
    private int previewWidth;
    private int previewHeight;

    private HazardSignEditorScreen(BlockPos signPos, String hazardId) {
        super(Component.literal("Hazard Sign Editor"));
        this.signPos = signPos.immutable();
        this.selectedHazard = ScpSignHazards.normalizeId(hazardId);
    }

    public static void open(BlockPos signPos, String hazardId) {
        Minecraft.getInstance().setScreen(
                new HazardSignEditorScreen(signPos, hazardId));
    }

    @Override
    protected void init() {
        calculateLayout();
        ensureSelectionVisible();
        int buttonY = panelTop + panelHeight - 29;
        addRenderableWidget(new EditorButton(
                panelLeft + panelWidth - 178, buttonY, 78, 20,
                Component.translatable("gui.done"), true,
                this::saveAndClose));
        addRenderableWidget(new EditorButton(
                panelLeft + panelWidth - 94, buttonY, 78, 20,
                Component.translatable("gui.cancel"), false, this::onClose));
    }

    private void calculateLayout() {
        panelWidth = Math.min(900, Math.max(680, width - 24));
        panelHeight = Math.min(540, Math.max(430, height - 24));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        listLeft = panelLeft + 18;
        listTop = panelTop + 78;
        listWidth = Math.min(390, Math.max(292, panelWidth / 2 - 44));
        listHeight = panelHeight - 128;

        int previewAreaLeft = listLeft + listWidth + 32;
        int previewAreaRight = panelLeft + panelWidth - 18;
        int availableWidth = previewAreaRight - previewAreaLeft;
        int availableHeight = panelHeight - 106;
        float scale = Math.min(availableWidth / (float) IMAGE_WIDTH,
                availableHeight / (float) IMAGE_HEIGHT);
        previewWidth = Math.max(1, Math.round(IMAGE_WIDTH * scale));
        previewHeight = Math.max(1, Math.round(IMAGE_HEIGHT * scale));
        previewLeft = previewAreaLeft
                + Math.max(0, (availableWidth - previewWidth) / 2);
        previewTop = panelTop + 56
                + Math.max(0, (availableHeight - previewHeight) / 2);
    }

    private int visibleRows() {
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, ScpSignHazards.OPTIONS.size() - visibleRows());
    }

    private void ensureSelectionVisible() {
        int selectedIndex = selectedIndex();
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        if (selectedIndex >= scrollOffset + visibleRows()) {
            scrollOffset = selectedIndex - visibleRows() + 1;
        }
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
    }

    private int selectedIndex() {
        List<ScpSignHazards.Option> options = ScpSignHazards.OPTIONS;
        for (int index = 0; index < options.size(); index++) {
            if (options.get(index).id().equals(selectedHazard)) return index;
        }
        return 0;
    }

    private void saveAndClose() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new HazardSignSavePacket(signPos, selectedHazard));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY,
                listLeft, listTop, listWidth, listHeight)) {
            int row = (int) ((mouseY - listTop) / ROW_HEIGHT);
            int index = scrollOffset + row;
            if (index >= 0 && index < ScpSignHazards.OPTIONS.size()) {
                selectedHazard = ScpSignHazards.OPTIONS.get(index).id();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inside(mouseX, mouseY,
                listLeft, listTop, listWidth, listHeight)) {
            scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(delta),
                    0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        drawPanel(graphics);
        drawPictogramList(graphics, mouseX, mouseY);
        drawPreview(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth,
                panelTop + panelHeight, PANEL_BACKGROUND);
        outline(graphics, panelLeft, panelTop, panelWidth, panelHeight,
                PANEL_EDGE);
        for (int x = panelLeft + 6; x < panelLeft + panelWidth - 4; x += 8) {
            for (int y = panelTop + 6; y < panelTop + panelHeight - 4; y += 8) {
                graphics.fill(x, y, x + 1, y + 1, 0x242F383E);
            }
        }

        graphics.drawString(font, ScpFonts.montserrat(
                        Component.literal("Hazard Sign Editor")),
                panelLeft + 18, panelTop + 14, TEXT_PRIMARY, false);
        graphics.drawString(font, ScpFonts.roboto("Anomaly Trait Pictogram"),
                listLeft, panelTop + 45, TEXT_PRIMARY, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "Choose one pictogram. The sign stays blank when None is selected."),
                listLeft, panelTop + 59, TEXT_MUTED, false);
    }

    private void drawPictogramList(GuiGraphics graphics, int mouseX,
            int mouseY) {
        graphics.fill(listLeft - 1, listTop - 1,
                listLeft + listWidth + 1, listTop + listHeight + 1,
                FIELD_BACKGROUND);
        outline(graphics, listLeft - 1, listTop - 1,
                listWidth + 2, listHeight + 2, FIELD_EDGE);

        graphics.enableScissor(listLeft, listTop,
                listLeft + listWidth, listTop + listHeight);
        int visible = visibleRows() + 1;
        for (int row = 0; row < visible; row++) {
            int index = scrollOffset + row;
            if (index >= ScpSignHazards.OPTIONS.size()) break;
            ScpSignHazards.Option option = ScpSignHazards.OPTIONS.get(index);
            int y = listTop + row * ROW_HEIGHT;
            boolean selected = option.id().equals(selectedHazard);
            boolean hovered = inside(mouseX, mouseY,
                    listLeft, y, listWidth, ROW_HEIGHT);
            int background = selected ? 0xFF3D3928
                    : hovered ? CONTROL_HOVER : CONTROL_BACKGROUND;
            graphics.fill(listLeft + 1, y + 1,
                    listLeft + listWidth - 1, y + ROW_HEIGHT - 1,
                    background);
            if (selected) {
                outline(graphics, listLeft + 1, y + 1,
                        listWidth - 2, ROW_HEIGHT - 2, ACCENT);
                graphics.fill(listLeft + 2, y + 2,
                        listLeft + 5, y + ROW_HEIGHT - 2, ACCENT);
            }

            int textX = listLeft + 10;
            if (!option.isNone() && resourceExists(option.texture())) {
                drawIcon(graphics, option.texture(),
                        listLeft + 8, y + 4, 24);
                textX = listLeft + 40;
            }
            String label = option.isNone()
                    ? "None / Clear pictogram" : option.displayName();
            String clipped = font.plainSubstrByWidth(label,
                    Math.max(1, listLeft + listWidth - 10 - textX));
            graphics.drawString(font, ScpFonts.roboto(clipped),
                    textX, y + 12,
                    selected ? ACCENT_TEXT : TEXT_PRIMARY, false);
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            int barX = listLeft + listWidth - 4;
            int thumbHeight = Math.max(18,
                    listHeight * visibleRows() / ScpSignHazards.OPTIONS.size());
            int travel = listHeight - thumbHeight;
            int thumbY = listTop + Math.round(travel
                    * (scrollOffset / (float) maxScroll()));
            graphics.fill(barX, listTop, barX + 2,
                    listTop + listHeight, 0xFF252B2F);
            graphics.fill(barX, thumbY, barX + 2,
                    thumbY + thumbHeight, ACCENT);
        }
    }

    private void drawPreview(GuiGraphics graphics) {
        int frameLeft = previewLeft - 8;
        int frameTop = previewTop - 22;
        graphics.fill(frameLeft, frameTop,
                previewLeft + previewWidth + 8,
                previewTop + previewHeight + 8, 0xFF111518);
        outline(graphics, frameLeft, frameTop,
                previewWidth + 16, previewHeight + 30, FIELD_EDGE);
        graphics.drawString(font, ScpFonts.roboto("Preview"),
                previewLeft, previewTop - 16, TEXT_PRIMARY, false);

        float scale = previewWidth / (float) IMAGE_WIDTH;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(previewLeft, previewTop, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(BASE, 0, 0, 0.0F, 0.0F,
                IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);

        ScpSignHazards.Option option = ScpSignHazards.option(selectedHazard);
        if (!option.isNone() && resourceExists(option.texture())) {
            graphics.pose().pushPose();
            graphics.pose().translate(PICTOGRAM.x(), PICTOGRAM.y(), 1.0F);
            graphics.pose().scale(PICTOGRAM.width() / 256.0F,
                    PICTOGRAM.height() / 256.0F, 1.0F);
            graphics.blit(option.texture(), 0, 0, 0.0F, 0.0F,
                    256, 256, 256, 256);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private static void drawIcon(GuiGraphics graphics,
            ResourceLocation texture, int x, int y, int size) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 1.0F);
        graphics.pose().scale(size / 256.0F, size / 256.0F, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                256, 256, 256, 256);
        graphics.pose().popPose();
    }

    private static boolean resourceExists(ResourceLocation texture) {
        return texture != null && Minecraft.getInstance().getResourceManager()
                .getResource(texture).isPresent();
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static void outline(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class EditorButton extends AbstractButton {
        private final boolean primary;
        private final Runnable action;

        private EditorButton(int x, int y, int width, int height,
                Component label, boolean primary, Runnable action) {
            super(x, y, width, height, label);
            this.primary = primary;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = isHoveredOrFocused()
                    ? CONTROL_HOVER : CONTROL_BACKGROUND;
            int edge = primary ? ACCENT
                    : isHoveredOrFocused() ? TEXT_PRIMARY : CONTROL_EDGE;
            int text = primary ? ACCENT_TEXT : TEXT_PRIMARY;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(), edge);
            if (primary) {
                graphics.fill(getX() + 1, getY() + 1,
                        getX() + 4, getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font, ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, text);
        }
    }

    private record ImageArea(int x, int y, int width, int height) {
    }
}
