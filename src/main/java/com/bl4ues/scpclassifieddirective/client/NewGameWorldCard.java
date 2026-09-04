package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** World-generation controls for the custom New Game flow. */
final class NewGameWorldCard {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9FA6AD;
    private static final int ACCENT = 0xFFC99B18;
    private static final int CARD = 0xB20B0E12;
    private static final int BORDER = 0x70444C57;

    private final CreateWorldScreen screen;
    private final WorldCreationUiState ui;
    private final Consumer<GuiEventListener> registrar;

    private NewGameWidgets.Dropdown<WorldCreationUiState.WorldTypeEntry> worldType;
    private NewGameWidgets.ActionButton customize;
    private EditBox seed;
    private NewGameWidgets.Toggle structures;
    private NewGameWidgets.Toggle bonusChest;

    NewGameWorldCard(CreateWorldScreen screen, WorldCreationUiState ui,
            Consumer<GuiEventListener> registrar) {
        this.screen = screen;
        this.ui = ui;
        this.registrar = registrar;
        build();
    }

    private void build() {
        List<NewGameWidgets.Dropdown.Entry<WorldCreationUiState.WorldTypeEntry>> entries =
                worldTypeEntries();
        worldType = register(new NewGameWidgets.Dropdown<>(
                0, 0, 300, 30, entries, ui.getWorldType(),
                this::selectWorldType,
                entry -> entry == null ? Component.literal("Default")
                        : entry.describePreset()));

        customize = register(new NewGameWidgets.ActionButton(
                0, 0, 150, 30, ScpFonts.roboto("Customize"),
                this::openCustomize));

        Font font = Minecraft.getInstance().font;
        seed = register(new EditBox(font, 0, 0, 300, 30,
                ScpFonts.roboto("Seed")));
        seed.setBordered(false);
        seed.setMaxLength(128);
        seed.setTextColor(TEXT);
        seed.setTextColorUneditable(MUTED);
        seed.setHint(ScpFonts.roboto("Leave blank for a random seed"));
        seed.setValue(ui.getSeed());
        seed.setResponder(ui::setSeed);

        structures = register(new NewGameWidgets.Toggle(
                0, 0, 300, 32, "Generate Structures",
                ui.isGenerateStructures(), ui::setGenerateStructures));
        bonusChest = register(new NewGameWidgets.Toggle(
                0, 0, 300, 32, "Bonus Chest",
                ui.isBonusChest(), ui::setBonusChest));
        refreshAvailability();
    }

    void refreshEntries() {
        worldType.setEntries(worldTypeEntries());
        worldType.setSelected(ui.getWorldType());
        seed.setValue(ui.getSeed());
        structures.setValue(ui.isGenerateStructures());
        bonusChest.setValue(ui.isBonusChest());
        refreshAvailability();
    }

    int height(int width) {
        return width < 620 ? 300 : 224;
    }

    void position(int x, int y, int width) {
        int pad = Mth.clamp(Math.round(width * 0.035F), 18, 30);
        int inner = Math.max(120, width - pad * 2);
        boolean narrow = width < 620;

        if (narrow) {
            int controlW = inner;
            worldType.setX(x + pad);
            worldType.setY(y + 50);
            worldType.setWidth(controlW);
            customize.setX(x + pad);
            customize.setY(y + 86);
            customize.setWidth(controlW);
            seed.setX(x + pad);
            seed.setY(y + 140);
            seed.setWidth(controlW);
            structures.setX(x + pad);
            structures.setY(y + 186);
            structures.setWidth(controlW);
            bonusChest.setX(x + pad);
            bonusChest.setY(y + 224);
            bonusChest.setWidth(controlW);
        } else {
            int gap = Mth.clamp(Math.round(width * 0.035F), 22, 34);
            int column = (inner - gap) / 2;
            int left = x + pad;
            int right = left + column + gap;

            worldType.setX(left);
            worldType.setY(y + 50);
            worldType.setWidth(column);
            customize.setX(left);
            customize.setY(y + 88);
            customize.setWidth(column);

            seed.setX(right);
            seed.setY(y + 50);
            seed.setWidth(column);
            structures.setX(right);
            structures.setY(y + 96);
            structures.setWidth(column);
            bonusChest.setX(right);
            bonusChest.setY(y + 134);
            bonusChest.setWidth(column);
        }
        refreshAvailability();
    }

    void setVisibleInViewport(int top, int bottom) {
        for (GuiEventListener listener : widgets()) {
            if (listener instanceof AbstractWidget widget) {
                widget.visible = widget.getY() + widget.getHeight() > top
                        && widget.getY() < bottom;
            }
        }
    }

    void renderBackground(GuiGraphics graphics, int x, int y, int width,
            float alpha) {
        int h = height(width);
        graphics.fill(x, y, x + width, y + h, applyAlpha(CARD, alpha));
        graphics.fill(x, y, x + 4, y + h, applyAlpha(ACCENT, alpha * 0.9F));
        graphics.fill(x + 18, y + 18, x + width - 18, y + 19,
                applyAlpha(BORDER, alpha));
        label(graphics, "WORLD TYPE", worldType.getX(), y + 32, alpha);
        if (width >= 620) {
            label(graphics, "SEED", seed.getX(), y + 32, alpha);
        } else {
            label(graphics, "SEED", seed.getX(), y + 122, alpha);
        }
    }

    void renderControls(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        if (seed.visible) {
            drawEditSurface(graphics, seed);
            seed.render(graphics, mouseX, mouseY, partialTick);
        }
        if (customize.visible) customize.render(graphics, mouseX, mouseY, partialTick);
        if (structures.visible) structures.render(graphics, mouseX, mouseY, partialTick);
        if (bonusChest.visible) bonusChest.render(graphics, mouseX, mouseY, partialTick);
        if (worldType.visible) worldType.render(graphics, mouseX, mouseY, partialTick);
    }

    List<GuiEventListener> widgets() {
        return List.of(worldType, customize, seed, structures, bonusChest);
    }

    private void selectWorldType(WorldCreationUiState.WorldTypeEntry entry) {
        if (entry == null) return;
        ui.setWorldType(entry);
        refreshAvailability();
    }

    private void openCustomize() {
        PresetEditor editor = ui.getPresetEditor();
        if (editor == null) return;
        Screen target = editor.createEditScreen(screen, ui.getSettings());
        if (target != null) Minecraft.getInstance().setScreen(target);
    }

    private void refreshAvailability() {
        customize.active = ui.getPresetEditor() != null;
        boolean debug = ui.isDebug();
        structures.active = !debug;
        bonusChest.active = !debug && !ui.isHardcore();
        if (debug) {
            structures.setValue(false);
            bonusChest.setValue(false);
        } else {
            structures.setValue(ui.isGenerateStructures());
            bonusChest.setValue(ui.isBonusChest());
        }
    }

    private List<NewGameWidgets.Dropdown.Entry<WorldCreationUiState.WorldTypeEntry>>
            worldTypeEntries() {
        Set<WorldCreationUiState.WorldTypeEntry> unique = new LinkedHashSet<>();
        unique.addAll(ui.getNormalPresetList());
        unique.addAll(ui.getAltPresetList());
        List<NewGameWidgets.Dropdown.Entry<WorldCreationUiState.WorldTypeEntry>> out =
                new ArrayList<>();
        for (WorldCreationUiState.WorldTypeEntry entry : unique) {
            out.add(new NewGameWidgets.Dropdown.Entry<>(entry,
                    entry.describePreset(), true));
        }
        out.add(NewGameWidgets.Dropdown.Entry.disabled(null,
                "ARC-Site 48 (Coming Soon)"));
        return out;
    }

    private <T extends GuiEventListener> T register(T listener) {
        registrar.accept(listener);
        return listener;
    }

    private static void label(GuiGraphics graphics, String text,
            int x, int y, float alpha) {
        graphics.drawString(Minecraft.getInstance().font,
                ScpFonts.montserrat(text), x, y,
                applyAlpha(MUTED, alpha), false);
    }

    private static void drawEditSurface(GuiGraphics graphics, EditBox box) {
        float alpha = ConfigCenterVisuals.contentAlpha();
        int x = box.getX();
        int y = box.getY();
        int w = box.getWidth();
        int h = box.getHeight();
        graphics.fill(x, y, x + w, y + h,
                applyAlpha(0xC00B0E12, alpha));
        int border = box.isFocused() ? ACCENT : BORDER;
        graphics.fill(x, y, x + w, y + 1, applyAlpha(border, alpha));
        graphics.fill(x, y + h - 1, x + w, y + h, applyAlpha(border, alpha));
        graphics.fill(x, y, x + 1, y + h, applyAlpha(border, alpha));
        graphics.fill(x + w - 1, y, x + w, y + h, applyAlpha(border, alpha));
    }

    private static int applyAlpha(int color, float alpha) {
        int source = (color >>> 24) & 0xFF;
        int out = Mth.clamp(Math.round(source * Mth.clamp(alpha, 0.0F, 1.0F)),
                0, 255);
        return (out << 24) | (color & 0x00FFFFFF);
    }
}
