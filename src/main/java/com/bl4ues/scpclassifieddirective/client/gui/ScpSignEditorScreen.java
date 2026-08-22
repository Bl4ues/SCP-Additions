package com.bl4ues.scpclassifieddirective.client.gui;

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
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.client.ScpSignTemplateClient;
import com.bl4ues.scpclassifieddirective.facility.ScpSignData;
import com.bl4ues.scpclassifieddirective.facility.ScpSignHazards;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplateSummary;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplates;
import com.bl4ues.scpclassifieddirective.network.ScpSignSavePacket;
import com.bl4ues.scpclassifieddirective.network.ScpSignTemplateDeletePacket;
import com.bl4ues.scpclassifieddirective.network.ScpSignTemplateUploadPacket;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Unified editor for the Facility Sign. Template selection is part of the same
 * form as the SCP information controls instead of opening a separate screen.
 */
public final class ScpSignEditorScreen extends Screen {
    private static final ResourceLocation BASE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/screens/scpsign/scp_sign_base.png");
    private static final int IMAGE_WIDTH = 1024;
    private static final int IMAGE_HEIGHT = 640;

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
    private static final int PREVIEW_TEXT = 0xFF000000;
    private static final float FONT_HEIGHT = 7.5F;
    private static final int TEXT_FIELD_Y_OFFSET = 5;

    private static final ImageArea CLEARANCE =
            new ImageArea(778, 85, 66, 47);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 261, 370, 64);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 343, 365, 40);
    private static final ImageArea ANOMALY =
            new ImageArea(528, 299, 351, 23);
    private static final ImageArea[] TRAITS = {
            new ImageArea(473, 375, 167, 164),
            new ImageArea(622, 375, 166, 164),
            new ImageArea(771, 375, 167, 164)
    };

    private final BlockPos signPos;
    private final ScpSignData initialData;

    private List<ScpSignTemplateSummary> templateOptions = List.of();
    private String selectedTemplateId;
    private int observedTemplateRevision;

    private TemplateDropdown templateDropdown;
    private EditBox customTemplateNameField;
    private EditorButton uploadButton;
    private EditorButton deleteTemplateButton;
    private EditorButton saveButton;

    private EditBox scpNumberField;
    private StyledDropdown<ScpSignData.ContainmentClass> containmentDropdown;
    private EditBox customContainmentField;
    private StyledDropdown<Integer> clearanceDropdown;
    private StyledDropdown<ScpSignData.AnomalyType> anomalyDropdown;
    private EditBox customAnomalyField;
    private TraitMultiSelect traitSelector;
    private ExpandableSelector openSelector;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int previewLeft;
    private int previewTop;
    private float previewScale;
    private int templateControlY;
    private int firstInformationControlY;
    private int traitControlY;
    private String status = "";
    private boolean statusError;

    private ScpSignEditorScreen(BlockPos signPos, ScpSignData data) {
        super(Component.literal("Facility Sign Editor"));
        this.signPos = signPos.immutable();
        this.initialData = data == null ? ScpSignData.DEFAULT : data;
        this.selectedTemplateId = this.initialData.templateId();
    }

    public static void open(BlockPos signPos, ScpSignData data) {
        Minecraft.getInstance().setScreen(
                new ScpSignEditorScreen(signPos, data));
    }

    @Override
    protected void init() {
        calculateLayout();
        openSelector = null;
        refreshTemplateOptions(false);

        int formX = panelLeft + 18;
        int fieldX = formX + 124;
        int fieldWidth = 214;

        templateControlY = panelTop + 46;
        templateDropdown = addRenderableWidget(new TemplateDropdown(
                fieldX, templateControlY, fieldWidth, 20));

        firstInformationControlY = templateControlY + 29;
        int y = firstInformationControlY;

        scpNumberField = configureField(new EditBox(font, fieldX,
                y + TEXT_FIELD_Y_OFFSET,
                fieldWidth, 20, Component.translatable(
                "screen.scp_classified_directive.scp_sign_number")));
        scpNumberField.setMaxLength(ScpSignData.MAX_SCP_NUMBER_LENGTH);
        scpNumberField.setFilter(value -> value.length()
                <= ScpSignData.MAX_SCP_NUMBER_LENGTH
                && value.chars().allMatch(character -> character >= '0'
                        && character <= '9'));
        scpNumberField.setValue(initialData.scpNumber());
        addRenderableWidget(scpNumberField);
        y += 27;

        containmentDropdown = addRenderableWidget(new StyledDropdown<>(
                fieldX, y, fieldWidth, 20,
                List.of(ScpSignData.ContainmentClass.values()),
                initialData.containmentClass(),
                value -> Component.literal(value.displayName()),
                value -> updateControlVisibility()));
        y += 27;

        customContainmentField = configureField(new EditBox(font, fieldX,
                y + TEXT_FIELD_Y_OFFSET,
                fieldWidth, 20, Component.translatable(
                "screen.scp_classified_directive.scp_sign_custom_containment")));
        customContainmentField.setMaxLength(
                ScpSignData.MAX_CONTAINMENT_CLASS_LENGTH);
        customContainmentField.setValue(initialData.customContainmentClass());
        addRenderableWidget(customContainmentField);
        y += 27;

        clearanceDropdown = addRenderableWidget(new StyledDropdown<>(
                fieldX, y, fieldWidth, 20, List.of(1, 2, 3, 4, 5, 6),
                initialData.clearanceLevel(),
                value -> Component.literal(String.format("%02d", value)),
                value -> {
                }));
        y += 27;

        anomalyDropdown = addRenderableWidget(new StyledDropdown<>(
                fieldX, y, fieldWidth, 20,
                List.of(ScpSignData.AnomalyType.values()),
                initialData.anomalyType(),
                value -> Component.literal(value.displayName()),
                value -> updateControlVisibility()));
        y += 27;

        customAnomalyField = configureField(new EditBox(font, fieldX,
                y + TEXT_FIELD_Y_OFFSET,
                fieldWidth, 20, Component.translatable(
                "screen.scp_classified_directive.scp_sign_custom_anomaly")));
        customAnomalyField.setMaxLength(ScpSignData.MAX_ANOMALY_TYPE_LENGTH);
        customAnomalyField.setValue(initialData.customAnomalyType());
        addRenderableWidget(customAnomalyField);
        y += 31;

        traitControlY = y;
        traitSelector = addRenderableWidget(new TraitMultiSelect(
                fieldX, traitControlY, fieldWidth, 20,
                initialData.hazards()));

        customTemplateNameField = configureField(new EditBox(font, fieldX,
                firstInformationControlY + TEXT_FIELD_Y_OFFSET,
                fieldWidth, 20, Component.literal("Template name")));
        customTemplateNameField.setMaxLength(
                ScpSignTemplates.MAX_NAME_LENGTH);
        customTemplateNameField.setValue("Custom Sign");
        addRenderableWidget(customTemplateNameField);

        uploadButton = addRenderableWidget(new EditorButton(fieldX,
                firstInformationControlY + 31, fieldWidth, 20,
                Component.literal("Choose PNG and Upload"),
                ButtonStyle.PRIMARY, this::chooseAndUpload));

        deleteTemplateButton = addRenderableWidget(new EditorButton(fieldX,
                firstInformationControlY, fieldWidth, 20,
                Component.literal("Delete Custom Template"),
                ButtonStyle.DANGER, this::deleteSelectedTemplate));

        int bottomY = panelTop + panelHeight - 29;
        saveButton = addRenderableWidget(new EditorButton(
                panelLeft + panelWidth - 178, bottomY, 78, 20,
                Component.translatable("gui.done"),
                ButtonStyle.PRIMARY, this::saveAndClose));
        addRenderableWidget(new EditorButton(
                panelLeft + panelWidth - 94, bottomY, 78, 20,
                Component.translatable("gui.cancel"),
                ButtonStyle.NEUTRAL, this::onClose));

        observedTemplateRevision = ScpSignTemplateClient.revision();
        updateControlVisibility();
    }

    private EditBox configureField(EditBox field) {
        field.setBordered(false);
        field.setTextColor(TEXT_PRIMARY);
        field.setTextColorUneditable(TEXT_MUTED);
        return field;
    }

    private void calculateLayout() {
        panelWidth = Math.min(980, Math.max(700, width - 20));
        panelHeight = Math.min(500, Math.max(430, height - 20));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        int availableWidth = panelWidth - 390;
        int availableHeight = panelHeight - 82;
        previewScale = Math.min(availableWidth / (float) IMAGE_WIDTH,
                availableHeight / (float) IMAGE_HEIGHT);
        previewScale = Math.max(0.15F, previewScale);
        previewLeft = panelLeft + 372
                + Math.max(0, (availableWidth
                - Math.round(IMAGE_WIDTH * previewScale)) / 2);
        previewTop = panelTop + 48
                + Math.max(0, (availableHeight
                - Math.round(IMAGE_HEIGHT * previewScale)) / 2);
    }

    @Override
    public void tick() {
        super.tick();
        int revision = ScpSignTemplateClient.revision();
        if (revision != observedTemplateRevision) {
            observedTemplateRevision = revision;
            refreshTemplateOptions(true);
            if (templateDropdown != null) {
                templateDropdown.ensureSelectionVisible();
            }
            updateControlVisibility();
        }
    }

    private void refreshTemplateOptions(boolean preferChanged) {
        templateOptions = ScpSignTemplateClient.options();
        String changedId = ScpSignTemplateClient.lastChangedId();
        if (preferChanged && ScpSignTemplates.isCustom(changedId)
                && templateOption(changedId) != null) {
            selectedTemplateId = changedId;
            status = "Custom template saved to this world.";
            statusError = false;
        }
        if (templateOption(selectedTemplateId) == null
                || ScpSignTemplates.CREATE_CUSTOM.equals(
                selectedTemplateId)) {
            selectedTemplateId = ScpSignTemplates.INFORMATION;
        }
    }

    private ScpSignTemplateSummary templateOption(String id) {
        if (id == null) return null;
        return templateOptions.stream()
                .filter(option -> option.id().equals(id))
                .findFirst().orElse(null);
    }

    private void selectTemplate(String id) {
        selectedTemplateId = id;
        status = "";
        updateControlVisibility();
        if (ScpSignTemplates.isCustom(id)) {
            ScpSignTemplateClient.texture(id);
        }
    }

    private void updateControlVisibility() {
        if (scpNumberField == null) return;
        boolean information = ScpSignTemplates.INFORMATION.equals(
                selectedTemplateId);
        boolean createCustom = ScpSignTemplates.CREATE_CUSTOM.equals(
                selectedTemplateId);
        boolean savedCustom = ScpSignTemplates.isCustom(
                selectedTemplateId);

        scpNumberField.visible = information;
        containmentDropdown.visible = information;
        customContainmentField.visible = information
                && containmentDropdown.getValue()
                == ScpSignData.ContainmentClass.CUSTOM;
        clearanceDropdown.visible = information;
        anomalyDropdown.visible = information;
        customAnomalyField.visible = information
                && anomalyDropdown.getValue()
                == ScpSignData.AnomalyType.CUSTOM;
        traitSelector.visible = information;

        customTemplateNameField.visible = createCustom;
        uploadButton.visible = createCustom;
        deleteTemplateButton.visible = savedCustom;
        saveButton.active = !createCustom;
    }

    private ScpSignData currentData() {
        return new ScpSignData(scpNumberField.getValue(),
                containmentDropdown.getValue(),
                customContainmentField.getValue(),
                clearanceDropdown.getValue(), anomalyDropdown.getValue(),
                customAnomalyField.getValue(), traitSelector.selectedIds())
                .withTemplateId(selectedTemplateId);
    }

    private void saveAndClose() {
        if (ScpSignTemplates.CREATE_CUSTOM.equals(selectedTemplateId)) {
            return;
        }
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new ScpSignSavePacket(signPos, currentData()));
        onClose();
    }

    private void chooseAndUpload() {
        String selectedPath;
        try {
            selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
                    "Choose sign artwork (PNG)", "", null,
                    "PNG image (*.png)", false);
        } catch (RuntimeException | LinkageError exception) {
            fail("Could not open the native file chooser.");
            return;
        }
        if (selectedPath == null || selectedPath.isBlank()) return;

        File file = new File(selectedPath);
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
            fail("Select a PNG image.");
            return;
        }

        try {
            if (!file.isFile() || file.length() > 16_000_000L) {
                fail("The selected PNG is too large or unavailable.");
                return;
            }
            byte[] png = normalizeImage(file);
            if (png.length > ScpSignTemplates.MAX_IMAGE_BYTES) {
                fail("The resized PNG is too large. Simplify the image.");
                return;
            }
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                    new ScpSignTemplateUploadPacket(signPos,
                            customTemplateNameField.getValue(), png));
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

    private void deleteSelectedTemplate() {
        if (!ScpSignTemplates.isCustom(selectedTemplateId)) return;
        String deletedId = selectedTemplateId;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new ScpSignTemplateDeletePacket(signPos, deletedId));
        selectedTemplateId = ScpSignTemplates.INFORMATION;
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
        if (openSelector != null) {
            if (openSelector.handleExpandedClick(mouseX, mouseY, button)) {
                return true;
            }
            if (!openSelector.isMouseOver(mouseX, mouseY)) {
                openSelector.setOpen(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (openSelector != null
                && openSelector.handleExpandedScroll(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        drawPanel(graphics);
        drawFormLabels(graphics);
        drawFieldFrames(graphics);
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
                        Component.literal("Facility Sign Editor")),
                panelLeft + 18, panelTop + 14, TEXT_PRIMARY, false);
    }

    private void drawFieldFrames(GuiGraphics graphics) {
        if (scpNumberField.visible) drawField(graphics, scpNumberField);
        if (customContainmentField.visible) {
            drawField(graphics, customContainmentField);
        }
        if (customAnomalyField.visible) {
            drawField(graphics, customAnomalyField);
        }
        if (customTemplateNameField.visible) {
            drawField(graphics, customTemplateNameField);
        }
    }

    private static void drawField(GuiGraphics graphics, EditBox field) {
        int frameY = field.getY() - TEXT_FIELD_Y_OFFSET;
        graphics.fill(field.getX() - 3, frameY - 1,
                field.getX() + field.getWidth() + 3,
                frameY + field.getHeight() + 1, FIELD_BACKGROUND);
        outline(graphics, field.getX() - 3, frameY - 1,
                field.getWidth() + 6, field.getHeight() + 2, FIELD_EDGE);
    }

    private void drawFormLabels(GuiGraphics graphics) {
        int x = panelLeft + 18;
        graphics.drawString(font, ScpFonts.roboto("Sign Template"),
                x, templateControlY + 6, TEXT_PRIMARY, false);

        if (ScpSignTemplates.INFORMATION.equals(selectedTemplateId)) {
            drawInformationLabels(graphics, x);
        } else if (ScpSignTemplates.CREATE_CUSTOM.equals(
                selectedTemplateId)) {
            graphics.drawString(font, ScpFonts.roboto("Template Name"),
                    x, firstInformationControlY + 6,
                    TEXT_PRIMARY, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "Required proportion: 8:5 (recommended 1024 x 640)."),
                    x, firstInformationControlY + 63,
                    TEXT_MUTED, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "Other proportions are resized automatically."),
                    x, firstInformationControlY + 77,
                    TEXT_MUTED, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "PNG only. The normalized image is saved in this world."),
                    x, firstInformationControlY + 91,
                    TEXT_MUTED, false);
        } else if (ScpSignTemplates.isCustom(selectedTemplateId)) {
            ScpSignTemplateSummary summary =
                    templateOption(selectedTemplateId);
            graphics.drawString(font, ScpFonts.roboto("World Template"),
                    x, firstInformationControlY + 6,
                    TEXT_PRIMARY, false);
            graphics.drawString(font, ScpFonts.roboto(
                            summary == null ? "Custom Sign" : summary.name()),
                    x, firstInformationControlY + 31,
                    ACCENT_TEXT, false);
            graphics.drawString(font, ScpFonts.roboto(
                            "Deleting it removes it from this world's library."),
                    x, firstInformationControlY + 59,
                    TEXT_MUTED, false);
        } else {
            graphics.drawString(font, ScpFonts.roboto(
                            "This built-in notice has no editable fields."),
                    x, firstInformationControlY + 6,
                    TEXT_MUTED, false);
        }

        if (!status.isBlank()) {
            graphics.drawString(font, ScpFonts.roboto(status),
                    panelLeft + 18, panelTop + panelHeight - 46,
                    statusError ? ERROR_TEXT : SUCCESS_TEXT, false);
        }
    }

    private void drawInformationLabels(GuiGraphics graphics, int x) {
        int y = firstInformationControlY + 6;
        Component[] labels = {
                Component.translatable("screen.scp_classified_directive.scp_sign_number"),
                Component.translatable(
                        "screen.scp_classified_directive.scp_sign_containment"),
                Component.translatable(
                        "screen.scp_classified_directive.scp_sign_custom_containment"),
                Component.translatable(
                        "screen.scp_classified_directive.scp_sign_clearance"),
                Component.translatable(
                        "screen.scp_classified_directive.scp_sign_anomaly"),
                Component.translatable(
                        "screen.scp_classified_directive.scp_sign_custom_anomaly")
        };
        int[] gaps = {27, 27, 27, 27, 27, 31};
        for (int index = 0; index < labels.length; index++) {
            boolean customHidden = index == 2
                    && !customContainmentField.visible
                    || index == 5 && !customAnomalyField.visible;
            if (!customHidden) {
                graphics.drawString(font, ScpFonts.roboto(labels[index]),
                        x, y, TEXT_MUTED, false);
            }
            y += gaps[index];
        }

        graphics.drawString(font, ScpFonts.roboto("Anomaly Traits"),
                x, traitControlY + 6, TEXT_PRIMARY, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "Choose up to 3. Selection order fills the sign left to right."),
                panelLeft + 18, traitControlY + 26, TEXT_MUTED, false);
    }

    private void drawPreview(GuiGraphics graphics) {
        int previewWidth = Math.round(IMAGE_WIDTH * previewScale);
        int previewHeight = Math.round(IMAGE_HEIGHT * previewScale);
        graphics.fill(previewLeft - 5, previewTop - 20,
                previewLeft + previewWidth + 5,
                previewTop + previewHeight + 5, 0xFF111518);
        outline(graphics, previewLeft - 5, previewTop - 20,
                previewWidth + 10, previewHeight + 25, FIELD_EDGE);
        graphics.drawString(font, ScpFonts.roboto("Preview"),
                previewLeft, previewTop - 14, TEXT_PRIMARY, false);

        if (ScpSignTemplates.INFORMATION.equals(selectedTemplateId)) {
            drawInformationPreview(graphics, currentData());
            return;
        }

        if (ScpSignTemplates.CREATE_CUSTOM.equals(selectedTemplateId)) {
            graphics.fill(previewLeft, previewTop,
                    previewLeft + previewWidth,
                    previewTop + previewHeight, 0xFF252B2F);
            graphics.drawCenteredString(font, ScpFonts.roboto(
                            "Choose a PNG to create this template"),
                    previewLeft + previewWidth / 2,
                    previewTop + previewHeight / 2 - 4, TEXT_MUTED);
            return;
        }

        ResourceLocation texture =
                ScpSignTemplateClient.texture(selectedTemplateId);
        if (texture == null) {
            graphics.fill(previewLeft, previewTop,
                    previewLeft + previewWidth,
                    previewTop + previewHeight, 0xFF252B2F);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto("Loading template preview..."),
                    previewLeft + previewWidth / 2,
                    previewTop + previewHeight / 2 - 4, TEXT_MUTED);
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(previewLeft, previewTop, 0.0F);
        graphics.pose().scale(previewScale, previewScale, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private void drawInformationPreview(GuiGraphics graphics,
            ScpSignData data) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(previewLeft, previewTop, 0.0F);
        graphics.pose().scale(previewScale, previewScale, 1.0F);
        graphics.blit(BASE, 0, 0, 0.0F, 0.0F,
                IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);

        for (int slot = 0; slot < ScpSignData.HAZARD_SLOTS; slot++) {
            ScpSignHazards.Option option = ScpSignHazards.option(
                    data.hazards().get(slot));
            ResourceLocation texture = option.texture();
            if (!resourceExists(texture)) {
                texture = ScpSignHazards.NONE.texture();
            }
            if (resourceExists(texture)) {
                drawTraitImage(graphics, texture, TRAITS[slot]);
            }
        }

        drawPreviewText(graphics,
                String.format("%02d", data.clearanceLevel()),
                CLEARANCE, true);
        drawPreviewText(graphics, data.scpLabel(), SCP_NUMBER, false);
        drawPreviewText(graphics, data.containmentLabel(),
                CONTAINMENT, false);
        drawPreviewText(graphics, data.anomalyLabel(), ANOMALY, true);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private void drawPreviewText(GuiGraphics graphics, String value,
            ImageArea area, boolean centered) {
        Component component = ScpFonts.scpSign(value);
        int textWidth = Math.max(1, font.width(component));
        float scale = Math.min(area.width() / (float) textWidth,
                area.height() / FONT_HEIGHT);
        float x = centered
                ? area.x() + (area.width() - textWidth * scale) * 0.5F
                : area.x();
        float renderedHeight = FONT_HEIGHT * scale;
        float y = area.y() + (area.height() - renderedHeight) * 0.5F;

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 2.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, component, 0, 0,
                PREVIEW_TEXT, false);
        graphics.pose().popPose();
    }

    private static void drawTraitImage(GuiGraphics graphics,
            ResourceLocation texture, ImageArea area) {
        graphics.pose().pushPose();
        graphics.pose().translate(area.x(), area.y(), 1.0F);
        graphics.pose().scale(area.width() / 256.0F,
                area.height() / 256.0F, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                256, 256, 256, 256);
        graphics.pose().popPose();
    }

    private static boolean resourceExists(ResourceLocation texture) {
        return texture != null
                && Minecraft.getInstance().getResourceManager()
                .getResource(texture).isPresent();
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

    private interface ExpandableSelector {
        void setOpen(boolean open);

        boolean handleExpandedClick(double mouseX, double mouseY,
                int button);

        boolean handleExpandedScroll(double mouseX, double mouseY,
                double delta);

        boolean isMouseOver(double mouseX, double mouseY);
    }

    private final class EditorButton extends AbstractButton {
        private final ButtonStyle style;
        private final Runnable action;

        private EditorButton(int x, int y, int width, int height,
                Component label, ButtonStyle style, Runnable action) {
            super(x, y, width, height, label);
            this.style = style;
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
            int background = !active ? 0xFF22282D
                    : isHoveredOrFocused()
                    ? CONTROL_HOVER : CONTROL_BACKGROUND;
            int edge = switch (style) {
                case PRIMARY -> isHoveredOrFocused()
                        ? ACCENT_TEXT : ACCENT;
                case DANGER -> isHoveredOrFocused()
                        ? 0xFFFFA0A0 : 0xFFB65353;
                default -> isHoveredOrFocused()
                        ? 0xFFD8E0E4 : CONTROL_EDGE;
            };
            int text = !active ? TEXT_MUTED
                    : style == ButtonStyle.PRIMARY ? ACCENT_TEXT
                    : style == ButtonStyle.DANGER
                    ? 0xFFFFA0A0 : TEXT_PRIMARY;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(),
                    getWidth(), getHeight(), edge);
            if (style == ButtonStyle.PRIMARY) {
                graphics.fill(getX() + 1, getY() + 1,
                        getX() + 4, getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, text);
        }
    }

    private final class TemplateDropdown extends AbstractButton
            implements ExpandableSelector {
        private static final int ROW_HEIGHT = 32;
        private static final int MAX_VISIBLE_ROWS = 8;
        private static final int THUMB_WIDTH = 40;
        private static final int THUMB_HEIGHT = 25;

        private boolean open;
        private int scrollOffset;

        private TemplateDropdown(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        public void setOpen(boolean shouldOpen) {
            if (open == shouldOpen) return;
            if (shouldOpen) {
                if (openSelector != null && openSelector != this) {
                    openSelector.setOpen(false);
                }
                openSelector = this;
                ensureSelectionVisible();
            } else if (openSelector == this) {
                openSelector = null;
            }
            open = shouldOpen;
        }

        @Override
        public void onPress() {
            setOpen(!open);
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = isHoveredOrFocused() || open
                    ? CONTROL_HOVER : CONTROL_BACKGROUND;
            int edge = open ? ACCENT
                    : isHoveredOrFocused()
                    ? 0xFFD8E0E4 : CONTROL_EDGE;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(),
                    getWidth(), getHeight(), edge);

            ScpSignTemplateSummary selected =
                    templateOption(selectedTemplateId);
            int textX = getX() + 7;
            if (selected != null
                    && !ScpSignTemplates.CREATE_CUSTOM.equals(
                    selected.id())) {
                ResourceLocation texture =
                        ScpSignTemplateClient.texture(selected.id());
                if (texture != null) {
                    int thumbHeight = 14;
                    int thumbWidth = Math.round(
                            thumbHeight * 1024.0F / 640.0F);
                    drawThumbnail(graphics, texture,
                            getX() + 4, getY() + 3,
                            thumbWidth, thumbHeight);
                    textX = getX() + thumbWidth + 9;
                }
            }

            String label = selected == null
                    ? "SCP Information Sign" : selected.name();
            String clipped = font.plainSubstrByWidth(label,
                    Math.max(1, getX() + getWidth() - 25 - textX));
            graphics.drawString(font, ScpFonts.roboto(clipped), textX,
                    getY() + (getHeight() - 8) / 2,
                    TEXT_PRIMARY, false);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(open ? "▲" : "▼"),
                    getX() + getWidth() - 11,
                    getY() + (getHeight() - 8) / 2, TEXT_MUTED);

            if (open) renderExpanded(graphics, mouseX, mouseY);
        }

        private void renderExpanded(GuiGraphics graphics,
                int mouseX, int mouseY) {
            int visible = visibleRows();
            int top = listTop();
            int listHeight = visible * ROW_HEIGHT + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            graphics.fill(getX(), top, getX() + getWidth(),
                    top + listHeight, 0xFF171C20);
            outline(graphics, getX(), top,
                    getWidth(), listHeight, ACCENT);

            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                if (index >= templateOptions.size()) break;
                ScpSignTemplateSummary option =
                        templateOptions.get(index);
                int rowY = top + 1 + row * ROW_HEIGHT;
                boolean hovered = mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY
                        && mouseY < rowY + ROW_HEIGHT;
                boolean selected = option.id().equals(
                        selectedTemplateId);
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1,
                        rowY + ROW_HEIGHT,
                        selected ? 0xFF4B3F27
                                : hovered ? 0xFF3D484F
                                : 0xFF242B30);

                int textX = getX() + 7;
                if (ScpSignTemplates.CREATE_CUSTOM.equals(option.id())) {
                    graphics.fill(getX() + 5, rowY + 5,
                            getX() + 5 + THUMB_WIDTH,
                            rowY + 5 + THUMB_HEIGHT,
                            0xFF30383E);
                    outline(graphics, getX() + 5, rowY + 5,
                            THUMB_WIDTH, THUMB_HEIGHT, CONTROL_EDGE);
                    graphics.drawCenteredString(font,
                            ScpFonts.roboto("+"),
                            getX() + 5 + THUMB_WIDTH / 2,
                            rowY + 13, ACCENT_TEXT);
                    textX = getX() + THUMB_WIDTH + 11;
                } else {
                    ResourceLocation texture =
                            ScpSignTemplateClient.texture(option.id());
                    if (texture != null) {
                        drawThumbnail(graphics, texture,
                                getX() + 5, rowY + 4,
                                THUMB_WIDTH, THUMB_HEIGHT);
                        textX = getX() + THUMB_WIDTH + 11;
                    }
                }

                String clipped = font.plainSubstrByWidth(option.name(),
                        Math.max(1, getX() + getWidth() - 8 - textX));
                graphics.drawString(font, ScpFonts.roboto(clipped),
                        textX, rowY + (ROW_HEIGHT - 8) / 2,
                        selected ? ACCENT_TEXT : TEXT_PRIMARY, false);
            }
            graphics.pose().popPose();
        }

        private void drawThumbnail(GuiGraphics graphics,
                ResourceLocation texture, int x, int y,
                int width, int height) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0F);
            graphics.pose().scale(width / 1024.0F,
                    height / 640.0F, 1.0F);
            graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                    1024, 640, 1024, 640);
            graphics.pose().popPose();
            RenderSystem.disableBlend();
        }

        @Override
        public boolean handleExpandedClick(double mouseX,
                double mouseY, int button) {
            if (!open || button != 0) return false;
            int top = listTop();
            int visible = visibleRows();
            if (mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= top
                    && mouseY < top + visible * ROW_HEIGHT + 2) {
                int row = (int) ((mouseY - top - 1) / ROW_HEIGHT);
                if (row >= 0 && row < visible) {
                    int index = scrollOffset + row;
                    if (index < templateOptions.size()) {
                        selectTemplate(templateOptions.get(index).id());
                    }
                }
                setOpen(false);
                return true;
            }
            return false;
        }

        @Override
        public boolean handleExpandedScroll(double mouseX,
                double mouseY, double delta) {
            if (!open || templateOptions.size() <= visibleRows()) {
                return false;
            }
            int top = listTop();
            int listHeight = visibleRows() * ROW_HEIGHT + 2;
            if (mouseX < getX() || mouseX >= getX() + getWidth()
                    || mouseY < top
                    || mouseY >= top + listHeight) {
                return false;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll(),
                    scrollOffset + (delta > 0.0D ? -1 : 1)));
            return true;
        }

        private int visibleRows() {
            return Math.min(MAX_VISIBLE_ROWS, templateOptions.size());
        }

        private int maxScroll() {
            return Math.max(0,
                    templateOptions.size() - visibleRows());
        }

        private int listTop() {
            int listHeight = visibleRows() * ROW_HEIGHT + 2;
            int below = getY() + getHeight() + 2;
            return below + listHeight
                    <= ScpSignEditorScreen.this.height - 6
                    ? below : getY() - listHeight - 2;
        }

        private void ensureSelectionVisible() {
            ScpSignTemplateSummary selected =
                    templateOption(selectedTemplateId);
            int selectedIndex = selected == null
                    ? 0 : templateOptions.indexOf(selected);
            selectedIndex = Math.max(0, selectedIndex);
            if (selectedIndex < scrollOffset) {
                scrollOffset = selectedIndex;
            }
            if (selectedIndex >= scrollOffset + visibleRows()) {
                scrollOffset = selectedIndex - visibleRows() + 1;
            }
            scrollOffset = Math.max(0,
                    Math.min(maxScroll(), scrollOffset));
        }
    }

    private final class StyledDropdown<T> extends AbstractButton
            implements ExpandableSelector {
        private static final int ROW_HEIGHT = 22;
        private static final int MAX_VISIBLE_ROWS = 8;

        private final List<T> values;
        private final Function<T, Component> labelFunction;
        private final Consumer<T> onChange;
        private T value;
        private boolean open;
        private int scrollOffset;

        private StyledDropdown(int x, int y, int width, int height,
                List<T> values, T initialValue,
                Function<T, Component> labelFunction,
                Consumer<T> onChange) {
            super(x, y, width, height,
                    labelFunction.apply(initialValue));
            this.values = List.copyOf(values);
            this.labelFunction = labelFunction;
            this.onChange = onChange;
            this.value = initialValue;
        }

        private T getValue() {
            return value;
        }

        private void setValue(T newValue, boolean notify) {
            if (newValue == null
                    || Objects.equals(value, newValue)) return;
            value = newValue;
            setMessage(labelFunction.apply(newValue));
            if (notify) onChange.accept(newValue);
        }

        @Override
        public void setOpen(boolean shouldOpen) {
            if (open == shouldOpen) return;
            if (shouldOpen) {
                if (openSelector != null && openSelector != this) {
                    openSelector.setOpen(false);
                }
                openSelector = this;
                ensureSelectionVisible();
            } else if (openSelector == this) {
                openSelector = null;
            }
            open = shouldOpen;
        }

        @Override
        public void onPress() {
            setOpen(!open);
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = isHoveredOrFocused() || open
                    ? CONTROL_HOVER : CONTROL_BACKGROUND;
            int edge = open ? ACCENT
                    : isHoveredOrFocused()
                    ? 0xFFD8E0E4 : CONTROL_EDGE;
            graphics.fill(getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(),
                    background);
            outline(graphics, getX(), getY(),
                    getWidth(), getHeight(), edge);

            String label = labelFunction.apply(value).getString();
            String clipped = font.plainSubstrByWidth(label,
                    Math.max(1, getWidth() - 30));
            graphics.drawString(font, ScpFonts.roboto(clipped),
                    getX() + 7,
                    getY() + (getHeight() - 8) / 2,
                    TEXT_PRIMARY, false);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(open ? "▲" : "▼"),
                    getX() + getWidth() - 11,
                    getY() + (getHeight() - 8) / 2, TEXT_MUTED);

            if (open) renderExpanded(graphics, mouseX, mouseY);
        }

        private void renderExpanded(GuiGraphics graphics,
                int mouseX, int mouseY) {
            int top = listTop();
            int visible = visibleRows();
            int listHeight = visible * ROW_HEIGHT + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 300.0F);
            graphics.fill(getX(), top, getX() + getWidth(),
                    top + listHeight, 0xFF171C20);
            outline(graphics, getX(), top,
                    getWidth(), listHeight, ACCENT);

            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                if (index >= values.size()) break;
                T option = values.get(index);
                int rowY = top + 1 + row * ROW_HEIGHT;
                boolean hovered = mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY
                        && mouseY < rowY + ROW_HEIGHT;
                boolean selected = Objects.equals(option, value);
                int rowColor = selected ? 0xFF4B3F27
                        : hovered ? 0xFF3D484F : 0xFF242B30;
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1,
                        rowY + ROW_HEIGHT, rowColor);
                String label = labelFunction.apply(option).getString();
                String clipped = font.plainSubstrByWidth(label,
                        Math.max(1, getWidth() - 14));
                graphics.drawString(font, ScpFonts.roboto(clipped),
                        getX() + 7,
                        rowY + (ROW_HEIGHT - 8) / 2,
                        selected ? ACCENT_TEXT : TEXT_PRIMARY,
                        false);
            }
            graphics.pose().popPose();
        }

        @Override
        public boolean handleExpandedClick(double mouseX,
                double mouseY, int button) {
            if (!open || button != 0) return false;
            int top = listTop();
            int visible = visibleRows();
            if (mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= top
                    && mouseY < top + visible * ROW_HEIGHT + 2) {
                int row = (int) ((mouseY - top - 1) / ROW_HEIGHT);
                if (row >= 0 && row < visible) {
                    int index = scrollOffset + row;
                    if (index < values.size()) {
                        setValue(values.get(index), true);
                    }
                }
                setOpen(false);
                return true;
            }
            return false;
        }

        @Override
        public boolean handleExpandedScroll(double mouseX,
                double mouseY, double delta) {
            if (!open || values.size() <= visibleRows()) return false;
            int top = listTop();
            int listHeight = visibleRows() * ROW_HEIGHT + 2;
            if (mouseX < getX() || mouseX >= getX() + getWidth()
                    || mouseY < top
                    || mouseY >= top + listHeight) {
                return false;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll(),
                    scrollOffset + (delta > 0.0D ? -1 : 1)));
            return true;
        }

        private int visibleRows() {
            return Math.min(MAX_VISIBLE_ROWS, values.size());
        }

        private int maxScroll() {
            return Math.max(0, values.size() - visibleRows());
        }

        private int listTop() {
            int listHeight = visibleRows() * ROW_HEIGHT + 2;
            int below = getY() + getHeight() + 2;
            return below + listHeight
                    <= ScpSignEditorScreen.this.height - 6
                    ? below : getY() - listHeight - 2;
        }

        private void ensureSelectionVisible() {
            int selected = Math.max(0, values.indexOf(value));
            if (selected < scrollOffset) scrollOffset = selected;
            if (selected >= scrollOffset + visibleRows()) {
                scrollOffset = selected - visibleRows() + 1;
            }
            scrollOffset = Math.max(0,
                    Math.min(maxScroll(), scrollOffset));
        }
    }

    private final class TraitMultiSelect extends AbstractButton
            implements ExpandableSelector {
        private static final int MAX_SELECTED = 3;
        private static final int ROW_HEIGHT = 28;
        private static final int MAX_VISIBLE_ROWS = 8;
        private static final int ICON_SIZE = 24;

        private final List<ScpSignHazards.Option> options;
        private final List<ScpSignHazards.Option> selected =
                new ArrayList<>();
        private boolean open;
        private int scrollOffset;

        private TraitMultiSelect(int x, int y, int width, int height,
                List<String> initialIds) {
            super(x, y, width, height,
                    Component.literal("No traits selected"));
            options = ScpSignHazards.OPTIONS.stream()
                    .filter(option -> !option.isNone()).toList();
            for (String id : initialIds) {
                ScpSignHazards.Option option =
                        ScpSignHazards.option(id);
                if (!option.isNone() && !selected.contains(option)
                        && selected.size() < MAX_SELECTED) {
                    selected.add(option);
                }
            }
            updateMessage();
        }

        private List<String> selectedIds() {
            return selected.stream()
                    .map(ScpSignHazards.Option::id).toList();
        }

        private void updateMessage() {
            setMessage(Component.literal(selected.isEmpty()
                    ? "No traits selected"
                    : selected.size() + " / "
                    + MAX_SELECTED + " selected"));
        }

        @Override
        public void setOpen(boolean shouldOpen) {
            if (open == shouldOpen) return;
            if (shouldOpen) {
                if (openSelector != null && openSelector != this) {
                    openSelector.setOpen(false);
                }
                openSelector = this;
                ensureSelectionVisible();
            } else if (openSelector == this) {
                openSelector = null;
            }
            open = shouldOpen;
        }

        @Override
        public void onPress() {
            setOpen(!open);
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = isHoveredOrFocused() || open
                    ? CONTROL_HOVER : CONTROL_BACKGROUND;
            int edge = open ? ACCENT
                    : isHoveredOrFocused()
                    ? 0xFFD8E0E4 : CONTROL_EDGE;
            graphics.fill(getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(),
                    background);
            outline(graphics, getX(), getY(),
                    getWidth(), getHeight(), edge);

            int iconX = getX() + 3;
            for (ScpSignHazards.Option option : selected) {
                drawSmallIcon(graphics, option.texture(),
                        iconX, getY() - 1, 18);
                iconX += 20;
            }
            int textX = selected.isEmpty()
                    ? getX() + 7 : iconX + 2;
            String clipped = font.plainSubstrByWidth(
                    getMessage().getString(),
                    Math.max(1, getX() + getWidth() - 24 - textX));
            graphics.drawString(font, ScpFonts.roboto(clipped),
                    textX, getY() + (getHeight() - 8) / 2,
                    selected.isEmpty() ? TEXT_MUTED : TEXT_PRIMARY,
                    false);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(open ? "▲" : "▼"),
                    getX() + getWidth() - 11,
                    getY() + (getHeight() - 8) / 2,
                    TEXT_MUTED);

            if (open) renderExpanded(graphics, mouseX, mouseY);
        }

        private void renderExpanded(GuiGraphics graphics,
                int mouseX, int mouseY) {
            int top = listTop();
            int visible = visibleRows();
            int listHeight = visible * ROW_HEIGHT + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 300.0F);
            graphics.fill(getX(), top, getX() + getWidth(),
                    top + listHeight, 0xFF171C20);
            outline(graphics, getX(), top,
                    getWidth(), listHeight, ACCENT);

            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                if (index >= options.size()) break;
                ScpSignHazards.Option option = options.get(index);
                int rowY = top + 1 + row * ROW_HEIGHT;
                int selectedIndex = selected.indexOf(option);
                boolean isSelected = selectedIndex >= 0;
                boolean enabled = isSelected
                        || selected.size() < MAX_SELECTED;
                boolean hovered = enabled
                        && mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY
                        && mouseY < rowY + ROW_HEIGHT;
                int rowColor = isSelected ? 0xFF4B3F27
                        : hovered ? 0xFF3D484F
                        : enabled ? 0xFF242B30 : 0xFF1D2226;
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1,
                        rowY + ROW_HEIGHT, rowColor);

                drawSmallIcon(graphics, option.texture(),
                        getX() + 2, rowY + 2, ICON_SIZE);
                int textColor = !enabled ? 0xFF5F686E
                        : isSelected ? ACCENT_TEXT : TEXT_PRIMARY;
                String clipped = font.plainSubstrByWidth(
                        option.displayName(),
                        Math.max(1,
                                getWidth() - ICON_SIZE - 31));
                graphics.drawString(font, ScpFonts.roboto(clipped),
                        getX() + ICON_SIZE + 7,
                        rowY + (ROW_HEIGHT - 8) / 2 + 2,
                        textColor, false);
                if (isSelected) {
                    graphics.drawCenteredString(font,
                            ScpFonts.roboto(Integer.toString(
                                    selectedIndex + 1)),
                            getX() + getWidth() - 12,
                            rowY + (ROW_HEIGHT - 8) / 2 + 2,
                            ACCENT_TEXT);
                }
            }

            if (options.size() > visible) {
                int trackX = getX() + getWidth() - 5;
                int trackTop = top + 3;
                int trackHeight = listHeight - 6;
                graphics.fill(trackX, trackTop, trackX + 2,
                        trackTop + trackHeight, 0xFF30383E);
                int thumbHeight = Math.max(8,
                        trackHeight * visible / options.size());
                int maxScroll = Math.max(1,
                        options.size() - visible);
                int thumbY = trackTop
                        + (trackHeight - thumbHeight)
                        * scrollOffset / maxScroll;
                graphics.fill(trackX, thumbY, trackX + 2,
                        thumbY + thumbHeight, ACCENT);
            }
            graphics.pose().popPose();
        }

        @Override
        public boolean handleExpandedClick(double mouseX,
                double mouseY, int button) {
            if (!open || button != 0) return false;
            int top = listTop();
            int visible = visibleRows();
            if (mouseX >= getX()
                    && mouseX < getX() + getWidth()
                    && mouseY >= top
                    && mouseY < top + visible * ROW_HEIGHT + 2) {
                int row = (int) ((mouseY - top - 1) / ROW_HEIGHT);
                if (row >= 0 && row < visible) {
                    int index = scrollOffset + row;
                    if (index < options.size()) {
                        toggle(options.get(index));
                    }
                }
                return true;
            }
            return false;
        }

        private void toggle(ScpSignHazards.Option option) {
            int selectedIndex = selected.indexOf(option);
            if (selectedIndex >= 0) {
                selected.remove(selectedIndex);
            } else if (selected.size() < MAX_SELECTED) {
                selected.add(option);
            }
            updateMessage();
        }

        @Override
        public boolean handleExpandedScroll(double mouseX,
                double mouseY, double delta) {
            if (!open || options.size() <= visibleRows()) return false;
            int top = listTop();
            int listHeight = visibleRows() * ROW_HEIGHT + 2;
            if (mouseX < getX()
                    || mouseX >= getX() + getWidth()
                    || mouseY < top
                    || mouseY >= top + listHeight) {
                return false;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll(),
                    scrollOffset + (delta > 0.0D ? -1 : 1)));
            return true;
        }

        private int visibleRows() {
            return Math.min(MAX_VISIBLE_ROWS, options.size());
        }

        private int maxScroll() {
            return Math.max(0, options.size() - visibleRows());
        }

        private int listTop() {
            int listHeight = visibleRows() * ROW_HEIGHT + 2;
            int below = getY() + getHeight() + 2;
            return below + listHeight
                    <= ScpSignEditorScreen.this.height - 6
                    ? below : getY() - listHeight - 2;
        }

        private void ensureSelectionVisible() {
            if (selected.isEmpty()) return;
            int firstSelected = options.indexOf(selected.get(0));
            if (firstSelected >= 0) {
                scrollOffset = Math.max(0,
                        Math.min(maxScroll(), firstSelected));
            }
        }

        private void drawSmallIcon(GuiGraphics graphics,
                ResourceLocation texture, int x, int y, int size) {
            if (!resourceExists(texture)) return;

            final int sourceTopX = 128;
            final int sourceTopY = 44;
            final int sourceBaseY = 176;
            final int sourceHalfWidth = 76;
            final int sourceHeight = sourceBaseY - sourceTopY;

            for (int row = 0; row < size; row++) {
                float progress = (row + 1.0F) / size;
                int sourceY = sourceTopY + Math.min(
                        sourceHeight - 1,
                        (int) (progress * sourceHeight));
                int halfWidth = Math.max(1,
                        (int) Math.ceil(sourceHalfWidth * progress));
                int sourceX = sourceTopX - halfWidth;
                int sourceWidth = halfWidth * 2;

                int destinationHalfWidth = Math.max(1,
                        (int) Math.ceil(size * 0.5F * progress));
                int destinationWidth = Math.min(size,
                        destinationHalfWidth * 2);
                int destinationX = x
                        + (size - destinationWidth) / 2;

                graphics.pose().pushPose();
                graphics.pose().translate(
                        destinationX, y + row, 1.0F);
                graphics.pose().scale(
                        destinationWidth / (float) sourceWidth,
                        1.0F, 1.0F);
                graphics.blit(texture, 0, 0,
                        (float) sourceX, (float) sourceY,
                        sourceWidth, 1, 256, 256);
                graphics.pose().popPose();
            }
        }
    }

    private enum ButtonStyle {
        NEUTRAL,
        PRIMARY,
        DANGER
    }

    private record ImageArea(int x, int y, int width, int height) {
    }
}
