package net.mcreator.scpadditions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.ScpFonts;
import net.mcreator.scpadditions.client.ScpSignTemplateClient;
import net.mcreator.scpadditions.facility.ScpSignData;
import net.mcreator.scpadditions.facility.ScpSignTemplateSummary;
import net.mcreator.scpadditions.facility.ScpSignTemplates;
import net.mcreator.scpadditions.network.ScpSignSavePacket;
import net.mcreator.scpadditions.network.ScpSignTemplateDeletePacket;
import net.mcreator.scpadditions.network.ScpSignTemplateUploadPacket;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Selects the sign artwork before exposing template-specific controls. Built-in
 * notices and world-saved custom images all use the single SCP Sign block.
 */
public final class ScpSignTemplateEditorScreen extends Screen {
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
    private static final int ERROR_TEXT = 0xFFFF7777;
    private static final int SUCCESS_TEXT = 0xFF79D58B;

    private final BlockPos signPos;
    private final ScpSignData initialData;

    private List<ScpSignTemplateSummary> options = List.of();
    private String selectedId;
    private int observedRevision;

    private TemplateDropdown templateDropdown;
    private EditBox customNameField;
    private EditorButton editInformationButton;
    private EditorButton uploadButton;
    private EditorButton deleteButton;
    private EditorButton saveButton;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int previewLeft;
    private int previewTop;
    private int previewWidth;
    private int previewHeight;
    private String status = "";
    private boolean statusError;

    private ScpSignTemplateEditorScreen(BlockPos signPos, ScpSignData data) {
        super(Component.literal("SCP Sign Editor"));
        this.signPos = signPos.immutable();
        this.initialData = data == null ? ScpSignData.DEFAULT : data;
        this.selectedId = this.initialData.templateId();
    }

    public static void open(BlockPos signPos, ScpSignData data) {
        Minecraft.getInstance().setScreen(
                new ScpSignTemplateEditorScreen(signPos, data));
    }

    @Override
    protected void init() {
        calculateLayout();
        refreshOptions(false);

        int leftX = panelLeft + 22;
        int controlX = leftX + 118;
        int controlWidth = Math.min(280, panelWidth / 2 - 154);
        int y = panelTop + 60;

        templateDropdown = addRenderableWidget(new TemplateDropdown(
                controlX, y, controlWidth, 22));
        y += 48;

        editInformationButton = addRenderableWidget(new EditorButton(
                controlX, y, controlWidth, 22,
                Component.literal("Edit SCP Information..."),
                ButtonStyle.NEUTRAL, this::openInformationEditor));

        customNameField = new EditBox(font, controlX, y + 3,
                controlWidth, 20, Component.literal("Template name"));
        customNameField.setBordered(false);
        customNameField.setMaxLength(ScpSignTemplates.MAX_NAME_LENGTH);
        customNameField.setValue("Custom Sign");
        customNameField.setTextColor(TEXT_PRIMARY);
        addRenderableWidget(customNameField);
        y += 34;

        uploadButton = addRenderableWidget(new EditorButton(
                controlX, y, controlWidth, 22,
                Component.literal("Choose PNG and Upload"),
                ButtonStyle.PRIMARY, this::chooseAndUpload));
        deleteButton = addRenderableWidget(new EditorButton(
                controlX, y, controlWidth, 22,
                Component.literal("Delete Custom Template"),
                ButtonStyle.DANGER, this::deleteSelected));

        int bottomY = panelTop + panelHeight - 32;
        saveButton = addRenderableWidget(new EditorButton(
                panelLeft + panelWidth - 190, bottomY, 82, 22,
                Component.literal("Save"), ButtonStyle.PRIMARY,
                this::saveAndClose));
        addRenderableWidget(new EditorButton(
                panelLeft + panelWidth - 100, bottomY, 82, 22,
                Component.literal("Cancel"), ButtonStyle.NEUTRAL,
                this::onClose));

        observedRevision = ScpSignTemplateClient.revision();
        updateControlVisibility();
    }

    private void calculateLayout() {
        panelWidth = Math.min(940, Math.max(700, width - 24));
        panelHeight = Math.min(470, Math.max(390, height - 24));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        previewWidth = Math.min(512, panelWidth / 2 + 20);
        previewHeight = Math.round(previewWidth * 640.0F / 1024.0F);
        previewLeft = panelLeft + panelWidth - previewWidth - 24;
        previewTop = panelTop + 72;
    }

    @Override
    public void tick() {
        super.tick();
        int currentRevision = ScpSignTemplateClient.revision();
        if (currentRevision != observedRevision) {
            observedRevision = currentRevision;
            refreshOptions(true);
            if (templateDropdown != null) templateDropdown.ensureVisible();
            updateControlVisibility();
        }
    }

    private void refreshOptions(boolean preferChanged) {
        options = ScpSignTemplateClient.options();
        String changed = ScpSignTemplateClient.lastChangedId();
        if (preferChanged && ScpSignTemplates.isCustom(changed)
                && option(changed) != null) {
            selectedId = changed;
            status = "Custom template saved to this world.";
            statusError = false;
        }
        if (option(selectedId) == null
                || ScpSignTemplates.CREATE_CUSTOM.equals(selectedId)) {
            selectedId = ScpSignTemplates.INFORMATION;
        }
    }

    private ScpSignTemplateSummary option(String id) {
        if (id == null) return null;
        return options.stream().filter(value -> value.id().equals(id))
                .findFirst().orElse(null);
    }

    private void select(String id) {
        selectedId = id;
        status = "";
        updateControlVisibility();
        if (ScpSignTemplates.isCustom(id)) {
            ScpSignTemplateClient.texture(id);
        }
    }

    private void updateControlVisibility() {
        if (editInformationButton == null) return;
        boolean information = ScpSignTemplates.INFORMATION.equals(selectedId);
        boolean createCustom = ScpSignTemplates.CREATE_CUSTOM.equals(selectedId);
        boolean savedCustom = ScpSignTemplates.isCustom(selectedId);

        editInformationButton.visible = information;
        customNameField.visible = createCustom;
        uploadButton.visible = createCustom;
        deleteButton.visible = savedCustom;
        saveButton.active = !createCustom;
    }

    private void openInformationEditor() {
        ScpSignEditorScreen.open(signPos,
                initialData.withTemplateId(ScpSignTemplates.INFORMATION));
    }

    private void saveAndClose() {
        if (ScpSignTemplates.CREATE_CUSTOM.equals(selectedId)) return;
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new ScpSignSavePacket(signPos,
                        initialData.withTemplateId(selectedId)));
        onClose();
    }

    private void chooseAndUpload() {
        String selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
                "Choose sign artwork (PNG)", "",
                new String[]{"*.png"}, "PNG images", false);
        if (selectedPath == null || selectedPath.isBlank()) return;

        try {
            File file = new File(selectedPath);
            if (!file.isFile() || file.length() > 16_000_000L) {
                fail("The selected PNG is too large or unavailable.");
                return;
            }
            byte[] png = normalizeImage(file);
            if (png.length > ScpSignTemplates.MAX_IMAGE_BYTES) {
                fail("The resized PNG is too large. Simplify the image.");
                return;
            }
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                    new ScpSignTemplateUploadPacket(signPos,
                            customNameField.getValue(), png));
            status = "Uploading template to the world...";
            statusError = false;
        } catch (IOException | RuntimeException exception) {
            fail("Could not read that PNG image.");
        }
    }

    private static byte[] normalizeImage(File file) throws IOException {
        BufferedImage source = ImageIO.read(file);
        if (source == null) throw new IOException("Unsupported image");
        if (source.getWidth() > 8192 || source.getHeight() > 8192) {
            throw new IOException("Image dimensions are too large");
        }
        BufferedImage target = new BufferedImage(
                ScpSignTemplates.TARGET_WIDTH,
                ScpSignTemplates.TARGET_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0,
                    ScpSignTemplates.TARGET_WIDTH,
                    ScpSignTemplates.TARGET_HEIGHT, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(target, "PNG", output)) {
            throw new IOException("PNG encoder unavailable");
        }
        return output.toByteArray();
    }

    private void deleteSelected() {
        if (!ScpSignTemplates.isCustom(selectedId)) return;
        String deleted = selectedId;
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new ScpSignTemplateDeletePacket(signPos, deleted));
        selectedId = ScpSignTemplates.INFORMATION;
        status = "Deleting custom template from this world...";
        statusError = false;
        updateControlVisibility();
    }

    private void fail(String message) {
        status = message;
        statusError = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (templateDropdown != null && templateDropdown.open
                && templateDropdown.handleListClick(mouseX, mouseY, button)) {
            return true;
        }
        if (templateDropdown != null && templateDropdown.open
                && !templateDropdown.isMouseOver(mouseX, mouseY)) {
            templateDropdown.open = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (templateDropdown != null
                && templateDropdown.handleListScroll(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        drawPanel(graphics);
        drawLabels(graphics);
        drawPreview(graphics);
        drawFieldFrame(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (templateDropdown != null && templateDropdown.open) {
            templateDropdown.renderList(graphics, mouseX, mouseY);
        }
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth,
                panelTop + panelHeight, PANEL_BACKGROUND);
        outline(graphics, panelLeft, panelTop, panelWidth, panelHeight,
                PANEL_EDGE);
        graphics.drawString(font, ScpFonts.montserrat(
                        Component.literal("SCP Sign Editor")),
                panelLeft + 20, panelTop + 16, TEXT_PRIMARY, false);
        graphics.fill(panelLeft + 18, panelTop + 45,
                panelLeft + panelWidth - 18, panelTop + 46, 0xFF343D43);
    }

    private void drawLabels(GuiGraphics graphics) {
        int x = panelLeft + 22;
        graphics.drawString(font, ScpFonts.roboto("Sign Template"),
                x, panelTop + 67, TEXT_PRIMARY, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "Built-in templates appear first; world templates follow."),
                x, panelTop + 92, TEXT_MUTED, false);

        if (ScpSignTemplates.INFORMATION.equals(selectedId)) {
            graphics.drawString(font, ScpFonts.roboto(
                            "Edit the SCP number, containment class, clearance,"),
                    x, panelTop + 132, TEXT_MUTED, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "anomaly type and hazard symbols in the information editor."),
                    x, panelTop + 146, TEXT_MUTED, false);
        } else if (ScpSignTemplates.CREATE_CUSTOM.equals(selectedId)) {
            graphics.drawString(font, ScpFonts.roboto("Template Name"),
                    x, panelTop + 116, TEXT_PRIMARY, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "Required proportion: 8:5 (recommended 1024 x 640)."),
                    x, panelTop + 178, TEXT_MUTED, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "Images with another proportion are resized automatically."),
                    x, panelTop + 192, TEXT_MUTED, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "PNG only. The normalized image is stored in this world."),
                    x, panelTop + 206, TEXT_MUTED, false);
        } else if (ScpSignTemplates.isCustom(selectedId)) {
            ScpSignTemplateSummary summary = option(selectedId);
            graphics.drawString(font, ScpFonts.roboto("World Template"),
                    x, panelTop + 116, TEXT_PRIMARY, false);
            graphics.drawString(font, ScpFonts.roboto(summary == null
                            ? "Custom Sign" : summary.name()),
                    x, panelTop + 132, ACCENT_TEXT, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "Deleting it removes it from the shared world library."),
                    x, panelTop + 178, TEXT_MUTED, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "Signs already using a deleted template fall back safely."),
                    x, panelTop + 192, TEXT_MUTED, false);
        } else {
            graphics.drawString(font, ScpFonts.roboto(
                            "This built-in notice has no additional editable fields."),
                    x, panelTop + 126, TEXT_MUTED, false);
        }

        if (!status.isBlank()) {
            graphics.drawString(font, ScpFonts.roboto(status), x,
                    panelTop + panelHeight - 55,
                    statusError ? ERROR_TEXT : SUCCESS_TEXT, false);
        }
    }

    private void drawFieldFrame(GuiGraphics graphics) {
        if (customNameField == null || !customNameField.visible) return;
        graphics.fill(customNameField.getX() - 3,
                customNameField.getY() - 4,
                customNameField.getX() + customNameField.getWidth() + 3,
                customNameField.getY() + customNameField.getHeight() + 1,
                FIELD_BACKGROUND);
        outline(graphics, customNameField.getX() - 3,
                customNameField.getY() - 4,
                customNameField.getWidth() + 6,
                customNameField.getHeight() + 5, FIELD_EDGE);
    }

    private void drawPreview(GuiGraphics graphics) {
        graphics.fill(previewLeft - 8, previewTop - 27,
                previewLeft + previewWidth + 8,
                previewTop + previewHeight + 8, 0xFF111518);
        outline(graphics, previewLeft - 8, previewTop - 27,
                previewWidth + 16, previewHeight + 35, FIELD_EDGE);
        graphics.drawString(font, ScpFonts.roboto("Preview"),
                previewLeft, previewTop - 19, TEXT_PRIMARY, false);

        ResourceLocation texture = null;
        if (!ScpSignTemplates.CREATE_CUSTOM.equals(selectedId)) {
            texture = ScpSignTemplateClient.texture(selectedId);
        }
        if (texture == null) {
            graphics.fill(previewLeft, previewTop,
                    previewLeft + previewWidth,
                    previewTop + previewHeight, 0xFF252B2F);
            graphics.drawCenteredString(font, ScpFonts.roboto(
                            ScpSignTemplates.CREATE_CUSTOM.equals(selectedId)
                                    ? "Choose a PNG to create this template"
                                    : "Loading template preview..."),
                    previewLeft + previewWidth / 2,
                    previewTop + previewHeight / 2 - 4, TEXT_MUTED);
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(previewLeft, previewTop, 0.0F);
        graphics.pose().scale(previewWidth / 1024.0F,
                previewHeight / 640.0F, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                1024, 640, 1024, 640);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
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

    private enum ButtonStyle {
        PRIMARY,
        NEUTRAL,
        DANGER
    }

    private final class EditorButton extends AbstractButton {
        private final ButtonStyle style;
        private final Runnable action;

        private EditorButton(int x, int y, int width, int height,
                Component message, ButtonStyle style, Runnable action) {
            super(x, y, width, height, message);
            this.style = style;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = !active ? 0xFF22282D
                    : isHoveredOrFocused() ? CONTROL_HOVER : CONTROL_BACKGROUND;
            int edge = style == ButtonStyle.PRIMARY ? ACCENT
                    : style == ButtonStyle.DANGER ? 0xFFB65353 : CONTROL_EDGE;
            int text = !active ? TEXT_MUTED
                    : style == ButtonStyle.PRIMARY ? ACCENT_TEXT
                    : style == ButtonStyle.DANGER ? 0xFFFFA0A0 : TEXT_PRIMARY;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(), edge);
            graphics.drawCenteredString(font, ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, text);
        }
    }

    private final class TemplateDropdown extends AbstractButton {
        private static final int ROW_HEIGHT = 42;
        private static final int MAX_ROWS = 7;
        private boolean open;
        private int scrollOffset;

        private TemplateDropdown(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        public void onPress() {
            open = !open;
            ensureVisible();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = isHoveredOrFocused() || open
                    ? CONTROL_HOVER : CONTROL_BACKGROUND;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(),
                    open ? ACCENT : CONTROL_EDGE);
            ScpSignTemplateSummary selected = option(selectedId);
            String label = selected == null ? "SCP Information Sign"
                    : selected.name();
            String clipped = font.plainSubstrByWidth(label,
                    Math.max(1, getWidth() - 28));
            graphics.drawString(font, ScpFonts.roboto(clipped), getX() + 7,
                    getY() + 7, TEXT_PRIMARY, false);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(open ? "▲" : "▼"),
                    getX() + getWidth() - 11, getY() + 7, TEXT_MUTED);
        }

        private void renderList(GuiGraphics graphics, int mouseX,
                int mouseY) {
            int visible = Math.min(MAX_ROWS, options.size());
            int top = listTop(visible);
            int listHeight = visible * ROW_HEIGHT + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 500.0F);
            graphics.fill(getX(), top, getX() + getWidth(),
                    top + listHeight, 0xFF171C20);
            outline(graphics, getX(), top, getWidth(), listHeight, ACCENT);

            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                if (index >= options.size()) break;
                ScpSignTemplateSummary summary = options.get(index);
                int rowY = top + 1 + row * ROW_HEIGHT;
                boolean hovered = mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                boolean selected = summary.id().equals(selectedId);
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1, rowY + ROW_HEIGHT,
                        selected ? 0xFF4B3F27
                                : hovered ? 0xFF3D484F : 0xFF242B30);

                ResourceLocation texture =
                        ScpSignTemplates.CREATE_CUSTOM.equals(summary.id())
                                ? null : ScpSignTemplateClient.texture(
                                summary.id());
                int textX = getX() + 7;
                if (texture != null) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(getX() + 4, rowY + 4, 0.0F);
                    graphics.pose().scale(48.0F / 1024.0F,
                            30.0F / 640.0F, 1.0F);
                    graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                            1024, 640, 1024, 640);
                    graphics.pose().popPose();
                    textX = getX() + 58;
                }
                String clipped = font.plainSubstrByWidth(summary.name(),
                        Math.max(1, getX() + getWidth() - 8 - textX));
                graphics.drawString(font, ScpFonts.roboto(clipped), textX,
                        rowY + 17,
                        selected ? ACCENT_TEXT : TEXT_PRIMARY, false);
            }
            graphics.pose().popPose();
        }

        private boolean handleListClick(double mouseX, double mouseY,
                int button) {
            if (!open || button != 0) return false;
            int visible = Math.min(MAX_ROWS, options.size());
            int top = listTop(visible);
            if (mouseX < getX() || mouseX >= getX() + getWidth()
                    || mouseY < top
                    || mouseY >= top + visible * ROW_HEIGHT + 2) {
                return false;
            }
            int row = (int) ((mouseY - top - 1) / ROW_HEIGHT);
            int index = scrollOffset + row;
            if (index >= 0 && index < options.size()) {
                select(options.get(index).id());
            }
            open = false;
            return true;
        }

        private boolean handleListScroll(double mouseX, double mouseY,
                double delta) {
            if (!open || options.size() <= MAX_ROWS) return false;
            int visible = Math.min(MAX_ROWS, options.size());
            int top = listTop(visible);
            if (mouseX < getX() || mouseX >= getX() + getWidth()
                    || mouseY < top
                    || mouseY >= top + visible * ROW_HEIGHT + 2) {
                return false;
            }
            scrollOffset = Math.max(0, Math.min(options.size() - visible,
                    scrollOffset + (delta > 0.0D ? -1 : 1)));
            return true;
        }

        private int listTop(int visible) {
            int listHeight = visible * ROW_HEIGHT + 2;
            int below = getY() + getHeight() + 2;
            return below + listHeight
                    <= ScpSignTemplateEditorScreen.this.height - 6
                    ? below : getY() - listHeight - 2;
        }

        private void ensureVisible() {
            int selected = Math.max(0, options.indexOf(option(selectedId)));
            int visible = Math.min(MAX_ROWS, options.size());
            if (selected < scrollOffset) scrollOffset = selected;
            if (selected >= scrollOffset + visible) {
                scrollOffset = selected - visible + 1;
            }
            scrollOffset = Math.max(0,
                    Math.min(Math.max(0, options.size() - visible),
                            scrollOffset));
        }
    }
}
