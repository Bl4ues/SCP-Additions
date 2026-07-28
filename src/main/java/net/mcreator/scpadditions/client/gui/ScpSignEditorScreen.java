package net.mcreator.scpadditions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.ScpFonts;
import net.mcreator.scpadditions.facility.ScpSignData;
import net.mcreator.scpadditions.facility.ScpSignHazards;
import net.mcreator.scpadditions.network.ScpSignSavePacket;

import java.util.ArrayList;
import java.util.List;

/** Form editor and live 1024x640 preview for the SCP Sign Support. */
public final class ScpSignEditorScreen extends Screen {
    private static final ResourceLocation BASE = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/screens/scpsign/scp_sign_base.png");
    private static final int IMAGE_WIDTH = 1024;
    private static final int IMAGE_HEIGHT = 640;
    private static final int PANEL_BACKGROUND = 0xF01B2024;
    private static final int PANEL_EDGE = 0xFF657078;
    private static final int TEXT_PRIMARY = 0xFFE4E8EA;
    private static final int TEXT_MUTED = 0xFF879097;
    private static final int PREVIEW_TEXT = 0xFF000000;

    private static final ImageArea CLEARANCE =
            new ImageArea(783, 83, 57, 40);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 273, 355, 48);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 351, 354, 27);
    private static final ImageArea ANOMALY =
            new ImageArea(589, 298, 235, 15);
    private static final ImageArea[] HAZARDS = {
            new ImageArea(473, 375, 167, 164),
            new ImageArea(622, 375, 166, 164),
            new ImageArea(771, 375, 167, 164)
    };

    private final BlockPos signPos;
    private final ScpSignData initialData;
    private final List<CycleButton<ScpSignHazards.Option>> hazardButtons =
            new ArrayList<>();

    private EditBox scpNumberField;
    private CycleButton<ScpSignData.ContainmentClass> containmentButton;
    private EditBox customContainmentField;
    private CycleButton<Integer> clearanceButton;
    private CycleButton<ScpSignData.AnomalyType> anomalyButton;
    private EditBox customAnomalyField;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int previewLeft;
    private int previewTop;
    private float previewScale;

    private ScpSignEditorScreen(BlockPos signPos, ScpSignData data) {
        super(Component.translatable("screen.scp_additions.scp_sign_editor"));
        this.signPos = signPos.immutable();
        this.initialData = data == null ? ScpSignData.DEFAULT : data;
    }

    public static void open(BlockPos signPos, ScpSignData data) {
        Minecraft.getInstance().setScreen(
                new ScpSignEditorScreen(signPos, data));
    }

    @Override
    protected void init() {
        calculateLayout();
        hazardButtons.clear();

        int formX = panelLeft + 18;
        int fieldX = formX + 124;
        int fieldWidth = 220;
        int y = panelTop + 48;

        scpNumberField = new EditBox(font, fieldX, y, fieldWidth, 20,
                Component.translatable("screen.scp_additions.scp_sign_number"));
        scpNumberField.setMaxLength(ScpSignData.MAX_SCP_NUMBER_LENGTH);
        scpNumberField.setFilter(value -> value.length()
                <= ScpSignData.MAX_SCP_NUMBER_LENGTH
                && value.chars().allMatch(character -> character >= '0'
                        && character <= '9'));
        scpNumberField.setValue(initialData.scpNumber());
        addRenderableWidget(scpNumberField);
        y += 27;

        containmentButton = CycleButton
                .<ScpSignData.ContainmentClass>builder(value ->
                        Component.literal(value.displayName()))
                .withValues(List.of(ScpSignData.ContainmentClass.values()))
                .withInitialValue(initialData.containmentClass())
                .create(fieldX, y, fieldWidth, 20,
                        Component.translatable(
                                "screen.scp_additions.scp_sign_containment"),
                        (button, value) -> updateCustomVisibility());
        addRenderableWidget(containmentButton);
        y += 27;

        customContainmentField = new EditBox(font, fieldX, y,
                fieldWidth, 20, Component.translatable(
                "screen.scp_additions.scp_sign_custom_containment"));
        customContainmentField.setMaxLength(
                ScpSignData.MAX_CONTAINMENT_CLASS_LENGTH);
        customContainmentField.setValue(initialData.customContainmentClass());
        addRenderableWidget(customContainmentField);
        y += 27;

        clearanceButton = CycleButton.<Integer>builder(value ->
                        Component.literal(String.format("%02d", value)))
                .withValues(List.of(1, 2, 3, 4, 5, 6))
                .withInitialValue(initialData.clearanceLevel())
                .create(fieldX, y, fieldWidth, 20,
                        Component.translatable(
                                "screen.scp_additions.scp_sign_clearance"),
                        (button, value) -> {
                        });
        addRenderableWidget(clearanceButton);
        y += 27;

        anomalyButton = CycleButton
                .<ScpSignData.AnomalyType>builder(value ->
                        Component.literal(value.displayName()))
                .withValues(List.of(ScpSignData.AnomalyType.values()))
                .withInitialValue(initialData.anomalyType())
                .create(fieldX, y, fieldWidth, 20,
                        Component.translatable(
                                "screen.scp_additions.scp_sign_anomaly"),
                        (button, value) -> updateCustomVisibility());
        addRenderableWidget(anomalyButton);
        y += 27;

        customAnomalyField = new EditBox(font, fieldX, y,
                fieldWidth, 20, Component.translatable(
                "screen.scp_additions.scp_sign_custom_anomaly"));
        customAnomalyField.setMaxLength(ScpSignData.MAX_ANOMALY_TYPE_LENGTH);
        customAnomalyField.setValue(initialData.customAnomalyType());
        addRenderableWidget(customAnomalyField);
        y += 31;

        for (int slot = 0; slot < ScpSignData.HAZARD_SLOTS; slot++) {
            final int selectedSlot = slot;
            ScpSignHazards.Option initial = ScpSignHazards.option(
                    initialData.hazards().get(slot));
            CycleButton<ScpSignHazards.Option> button = CycleButton
                    .<ScpSignHazards.Option>builder(option ->
                            Component.literal(option.displayName()))
                    .withValues(ScpSignHazards.OPTIONS)
                    .withInitialValue(initial)
                    .create(fieldX, y, fieldWidth, 20,
                            Component.translatable(
                                    "screen.scp_additions.scp_sign_hazard",
                                    slot + 1),
                            (changed, value) -> selectHazard(
                                    selectedSlot, value));
            hazardButtons.add(addRenderableWidget(button));
            y += 27;
        }

        int bottomY = panelTop + panelHeight - 29;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        button -> saveAndClose())
                .bounds(panelLeft + panelWidth - 178, bottomY, 78, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"),
                        button -> onClose())
                .bounds(panelLeft + panelWidth - 94, bottomY, 78, 20)
                .build());

        updateCustomVisibility();
    }

    private void calculateLayout() {
        panelWidth = Math.min(980, Math.max(680, width - 20));
        panelHeight = Math.min(470, Math.max(410, height - 20));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        int availableWidth = panelWidth - 382;
        int availableHeight = panelHeight - 76;
        previewScale = Math.min(availableWidth / (float) IMAGE_WIDTH,
                availableHeight / (float) IMAGE_HEIGHT);
        previewScale = Math.max(0.15F, previewScale);
        previewLeft = panelLeft + 366
                + Math.max(0, (availableWidth
                - Math.round(IMAGE_WIDTH * previewScale)) / 2);
        previewTop = panelTop + 40
                + Math.max(0, (availableHeight
                - Math.round(IMAGE_HEIGHT * previewScale)) / 2);
    }

    private void updateCustomVisibility() {
        if (customContainmentField != null && containmentButton != null) {
            customContainmentField.visible = containmentButton.getValue()
                    == ScpSignData.ContainmentClass.CUSTOM;
        }
        if (customAnomalyField != null && anomalyButton != null) {
            customAnomalyField.visible = anomalyButton.getValue()
                    == ScpSignData.AnomalyType.CUSTOM;
        }
    }

    private void selectHazard(int selectedSlot,
            ScpSignHazards.Option selected) {
        if (selected == null || selected.isNone()) return;
        for (int slot = 0; slot < hazardButtons.size(); slot++) {
            if (slot != selectedSlot
                    && hazardButtons.get(slot).getValue().id()
                    .equals(selected.id())) {
                hazardButtons.get(slot).setValue(ScpSignHazards.NONE);
            }
        }
    }

    private ScpSignData currentData() {
        List<String> hazards = hazardButtons.stream()
                .map(button -> button.getValue().id()).toList();
        return new ScpSignData(scpNumberField.getValue(),
                containmentButton.getValue(), customContainmentField.getValue(),
                clearanceButton.getValue(), anomalyButton.getValue(),
                customAnomalyField.getValue(), hazards);
    }

    private void saveAndClose() {
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new ScpSignSavePacket(signPos, currentData()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth,
                panelTop + panelHeight, PANEL_BACKGROUND);
        outline(graphics, panelLeft, panelTop, panelWidth, panelHeight,
                PANEL_EDGE);

        graphics.drawString(font, ScpFonts.montserrat(Component.translatable(
                        "screen.scp_additions.scp_sign_editor")),
                panelLeft + 18, panelTop + 14, TEXT_PRIMARY, false);
        drawFormLabels(graphics);
        drawPreview(graphics, currentData());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawFormLabels(GuiGraphics graphics) {
        int x = panelLeft + 18;
        int y = panelTop + 54;
        String[] labels = {
                "SCP Number", "Containment Class", "Custom Class",
                "Clearance Level", "Anomaly Type", "Custom Type",
                "Hazard Slot 1", "Hazard Slot 2", "Hazard Slot 3"
        };
        int[] gaps = {27, 27, 27, 27, 27, 31, 27, 27, 27};
        for (int index = 0; index < labels.length; index++) {
            boolean customHidden = (index == 2 && !customContainmentField.visible)
                    || (index == 5 && !customAnomalyField.visible);
            if (!customHidden) {
                graphics.drawString(font, ScpFonts.roboto(labels[index]),
                        x, y, index >= 6 ? TEXT_PRIMARY : TEXT_MUTED, false);
            }
            y += gaps[index];
        }
    }

    private void drawPreview(GuiGraphics graphics, ScpSignData data) {
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
            if (option.isNone() || !resourceExists(option.texture())) continue;
            drawHazard(graphics, option.texture(), HAZARDS[slot]);
        }

        drawPreviewText(graphics,
                String.format("%02d", data.clearanceLevel()), CLEARANCE, true);
        drawPreviewText(graphics, data.scpLabel(), SCP_NUMBER, false);
        drawPreviewText(graphics, data.containmentLabel(), CONTAINMENT, false);
        drawPreviewText(graphics, data.anomalyLabel(), ANOMALY, true);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private void drawPreviewText(GuiGraphics graphics, String value,
            ImageArea area, boolean centered) {
        Component component = ScpFonts.kokoro(value)
                .withStyle(ChatFormatting.BOLD);
        int textWidth = Math.max(1, font.width(component));
        float scale = Math.min(area.width() / textWidth,
                area.height() / 9.0F);
        float x = centered
                ? area.x() + (area.width() - textWidth * scale) * 0.5F
                : area.x();

        graphics.pose().pushPose();
        graphics.pose().translate(x, area.y(), 2.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, component, 0, 0, PREVIEW_TEXT, false);
        graphics.pose().popPose();
    }

    private static void drawHazard(GuiGraphics graphics,
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
        return Minecraft.getInstance().getResourceManager()
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

    private record ImageArea(int x, int y, int width, int height) {
    }
}
