package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.network.ItemConfigDeletePacket;
import com.bl4ues.scpinventory.network.ItemConfigOpenPacket;
import com.bl4ues.scpinventory.network.ItemConfigSavePacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ItemConfigScreen extends Screen {
    private static final int PANEL_W = 270;
    private static final int PANEL_H = 250;
    private static final int MARGIN = 10;
    private static final ScpItemType[] TYPES = ScpItemType.values();

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
        super(Component.literal("SCP Item Config Editor"));
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
        int x = left + 12;
        int y = top + 58;

        typeButton = addRenderableWidget(Button.builder(Component.literal(typeText()), b -> {
            type = TYPES[(type.ordinal() + 1) % TYPES.length];
            b.setMessage(Component.literal(typeText()));
        }).bounds(x, y, PANEL_W - 24, 20).build());

        y += 30;
        staminaButton = addRenderableWidget(Button.builder(Component.literal(staminaText()), b -> {
            noStamina = !noStamina;
            b.setMessage(Component.literal(staminaText()));
        }).bounds(x, y, PANEL_W - 24, 20).build());

        y += 30;
        protectedEyesButton = addRenderableWidget(Button.builder(Component.literal(protectedEyesText()), b -> {
            protectedEyes = !protectedEyes;
            b.setMessage(Component.literal(protectedEyesText()));
        }).bounds(x, y, PANEL_W - 24, 20).build());

        int bottomY = top + PANEL_H - 30;
        forgetButton = addRenderableWidget(Button.builder(Component.literal("Forget"), b -> forgetRule()).bounds(left + 12, bottomY, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save()).bounds(left + PANEL_W - 154, bottomY, 68, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(left + PANEL_W - 80, bottomY, 68, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xCC111317);
        g.fill(left, top, left + PANEL_W, top + 24, 0xE525282D);
        g.drawString(font, "SCP Item Config Editor", left + 12, top + 8, 0xFFE8E8E8, false);
        g.drawString(font, (existing ? "Editing " : "New ") + compact(itemId, 32), left + 12, top + 30, 0xFFB5C7FF, false);
        g.drawString(font, "Item category", left + 12, top + 48, 0xFFB7B7B7, false);
        g.drawString(font, "Effects", left + 12, top + 84, 0xFFB7B7B7, false);
        g.drawString(font, "Type writes item_rules[].", left + 12, top + 148, 0xFF999999, false);
        g.drawString(font, "Effects write item_effects[].", left + 12, top + 162, 0xFF999999, false);
        g.drawString(font, "Protected Eyes blocks external eye irritation.", left + 12, top + 176, 0xFF999999, false);
        g.drawString(font, "Use CODEX for document items.", left + 12, top + 190, 0xFF88DDEE, false);
        g.drawString(font, "Forget asks twice, then removes this item.", left + 12, top + 204, 0xFFFF9D9D, false);
        super.render(g, mouseX, mouseY, partialTick);
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
            if (forgetButton != null) {
                forgetButton.setMessage(Component.literal("Confirm"));
            }
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new ItemConfigDeletePacket(itemId));
        Minecraft.getInstance().setScreen(null);
    }

    private String typeText() {
        return "Type: " + type.name();
    }

    private String staminaText() {
        return noStamina ? "No Stamina: On" : "No Stamina: Off";
    }

    private String protectedEyesText() {
        return protectedEyes ? "Protected Eyes: On" : "Protected Eyes: Off";
    }

    private int panelLeft() {
        return Math.max(MARGIN, width - PANEL_W - MARGIN);
    }

    private int panelTop() {
        return Math.max(MARGIN, (height - PANEL_H) / 2);
    }

    private static ScpItemType parseType(String value) {
        try {
            return ScpItemType.valueOf(value == null ? "MISCELLANEOUS" : value.trim().toUpperCase());
        } catch (Exception ignored) {
            return ScpItemType.MISCELLANEOUS;
        }
    }

    private static String compact(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }
}
