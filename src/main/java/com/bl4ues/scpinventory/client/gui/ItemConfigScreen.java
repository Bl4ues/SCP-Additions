package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.network.ItemConfigDeletePacket;
import com.bl4ues.scpinventory.network.ItemConfigOpenPacket;
import com.bl4ues.scpinventory.network.ItemConfigSavePacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class ItemConfigScreen extends Screen {
    private static final int PANEL_W = 310;
    private static final int PANEL_H = 270;
    private static final int MARGIN = 10;
    private static final int NAVY = 0xF000071F;
    private static final int NAVY_LIGHT = 0xE6141E42;
    private static final int BORDER = 0xFF46536C;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFFA9AFBA;
    private static final int SECTION = 0xFFD3D9E4;
    private static final int DANGER = 0xFFFF8F8F;
    private static final ScpItemType[] TYPES = Arrays.stream(ScpItemType.values())
            .filter(type -> type != ScpItemType.CODEX)
            .toArray(ScpItemType[]::new);

    private final String itemId;
    private final boolean existing;
    private ScpItemType type;
    private boolean noStamina;
    private boolean protectedEyes;
    private Button typeButton;
    private Button staminaButton;
    private Button protectedEyesButton;
    private Button forgetButton;
    private boolean confirmForget;

    public ItemConfigScreen(ItemConfigOpenPacket packet) {
        super(ScpFonts.roboto("SCP Item Config Editor"));
        this.itemId = packet.itemId();
        this.existing = packet.existing();
        this.type = parseType(packet.type());
        this.noStamina = packet.noStamina();
        this.protectedEyes = packet.protectedEyes();
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int x = left + 16;
        int y = top + 78;
        int width = PANEL_W - 32;

        typeButton = addRenderableWidget(Button.builder(ScpFonts.roboto(typeText()), b -> {
            int index = Arrays.asList(TYPES).indexOf(type);
            type = TYPES[(Math.max(0, index) + 1) % TYPES.length];
            b.setMessage(ScpFonts.roboto(typeText()));
        }).bounds(x, y, width, 22).build());

        y += 38;
        staminaButton = addRenderableWidget(Button.builder(ScpFonts.roboto(staminaText()), b -> {
            noStamina = !noStamina;
            b.setMessage(ScpFonts.roboto(staminaText()));
        }).bounds(x, y, width, 22).build());

        y += 30;
        protectedEyesButton = addRenderableWidget(Button.builder(ScpFonts.roboto(protectedEyesText()), b -> {
            protectedEyes = !protectedEyes;
            b.setMessage(ScpFonts.roboto(protectedEyesText()));
        }).bounds(x, y, width, 22).build());

        int bottomY = top + PANEL_H - 34;
        forgetButton = addRenderableWidget(Button.builder(ScpFonts.roboto("Forget"), b -> forgetRule())
                .bounds(left + 16, bottomY, 76, 22).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Save"), b -> save())
                .bounds(left + PANEL_W - 172, bottomY, 76, 22).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"), b -> onClose())
                .bounds(left + PANEL_W - 90, bottomY, 74, 22).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int left = panelLeft();
        int top = panelTop();

        g.fill(left, top, left + PANEL_W, top + PANEL_H, NAVY);
        g.fill(left, top, left + PANEL_W, top + 31, NAVY_LIGHT);
        g.fill(left, top + 30, left + PANEL_W, top + 31, ACCENT);
        drawBorder(g, left, top, PANEL_W, PANEL_H, BORDER);

        g.drawString(font, ScpFonts.roboto("SCP ITEM CONFIGURATION"), left + 14, top + 11, WHITE, false);

        ItemStack preview = getPreviewStack();
        int iconX = left + 16;
        int iconY = top + 43;
        g.fill(iconX - 3, iconY - 3, iconX + 19, iconY + 19, NAVY_LIGHT);
        drawBorder(g, iconX - 3, iconY - 3, 22, 22, BORDER);
        g.fill(iconX - 3, iconY - 3, iconX + 4, iconY - 2, ACCENT);
        if (!preview.isEmpty()) g.renderItem(preview, iconX, iconY);

        g.drawString(font, ScpFonts.roboto(existing ? "Editing explicit rule" : "Creating explicit rule"),
                left + 46, top + 44, MUTED, false);
        g.drawString(font, ScpFonts.roboto(compact(itemId, 42)), left + 46, top + 57, WHITE, false);

        g.drawString(font, ScpFonts.roboto("CATEGORY"), left + 16, top + 69, SECTION, false);
        g.drawString(font, ScpFonts.roboto("EQUIPMENT EFFECTS"), left + 16, top + 108, SECTION, false);

        g.drawString(font, ScpFonts.roboto("Category controls storage, actions and equipment routing."),
                left + 16, top + 177, MUTED, false);
        g.drawString(font, ScpFonts.roboto("PLACEABLE is used automatically for blocks."),
                left + 16, top + 190, MUTED, false);
        g.drawString(font, ScpFonts.roboto("Forget removes the explicit rule and restores auto-detection."),
                left + 16, top + 203, DANGER, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y, x + 1, y + height, color);
        g.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new ItemConfigSavePacket(
                itemId, type.name(), noStamina, protectedEyes));
        Minecraft.getInstance().setScreen(null);
    }

    private void forgetRule() {
        if (!confirmForget) {
            confirmForget = true;
            if (forgetButton != null) forgetButton.setMessage(ScpFonts.roboto("Confirm"));
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new ItemConfigDeletePacket(itemId));
        Minecraft.getInstance().setScreen(null);
    }

    private ItemStack getPreviewStack() {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private String typeText() {
        return "Category: " + type.getDisplayName();
    }

    private String staminaText() {
        return noStamina ? "No Stamina: ON" : "No Stamina: OFF";
    }

    private String protectedEyesText() {
        return protectedEyes ? "Protected Eyes: ON" : "Protected Eyes: OFF";
    }

    private int panelLeft() {
        return Math.max(MARGIN, width - PANEL_W - MARGIN);
    }

    private int panelTop() {
        return Math.max(MARGIN, (height - PANEL_H) / 2);
    }

    private static ScpItemType parseType(String value) {
        try {
            ScpItemType parsed = ScpItemType.valueOf(value == null ? "MISCELLANEOUS" : value.trim().toUpperCase());
            return parsed == ScpItemType.CODEX ? ScpItemType.MISCELLANEOUS : parsed;
        } catch (Exception ignored) {
            return ScpItemType.MISCELLANEOUS;
        }
    }

    private static String compact(String text, int max) {
        if (text == null || text.length() <= max) return text == null ? "" : text;
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }
}
