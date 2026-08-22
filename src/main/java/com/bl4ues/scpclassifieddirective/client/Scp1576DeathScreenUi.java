package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Adds active SCP-1576 hosts above the existing dead-call portrait rail. */
public final class Scp1576DeathScreenUi {
    private static final ResourceLocation SCP1576_ICON = new ResourceLocation(
            "scp_classified_directive", "textures/item/scp1576.png");
    private static final int HEAD_MIN_SIZE = 18;
    private static final int HEAD_MAX_SIZE = 30;
    private static final int HEAD_GAP = 6;
    private static final int HOST_EXTRA_GAP = 8;
    private static final int BORDER = 0xFFC99B18;

    private Scp1576DeathScreenUi() {
    }

    public static void render(ScpDeathScreen screen, GuiGraphics graphics,
            int mouseX, int mouseY, float alpha) {
        if (screen == null || graphics == null || alpha <= 0.001F
                || !SimpleVoiceChatDeathScreenUi.visible()) {
            return;
        }

        List<Scp1576ClientState.SessionState> activeHosts = activeHosts();
        if (activeHosts.isEmpty()) return;

        Bounds feed = feed(screen);
        int cardRight = finalCardRight(screen);
        int railWidth = feed.left - cardRight;
        if (railWidth < HEAD_MIN_SIZE + 16) return;

        int headSize = Mth.clamp(railWidth - 18, HEAD_MIN_SIZE, HEAD_MAX_SIZE);
        int headX = feed.left - headSize - 9;
        if (headX < cardRight + 7) {
            headX = cardRight + Math.max(5, (railWidth - headSize) / 2);
        }

        int availableHeight = Math.max(0, feed.bottom - feed.top - 8);
        int maxVisible = Math.max(1,
                (availableHeight + HEAD_GAP) / (headSize + HEAD_GAP));
        int deadVisible = Math.min(maxVisible,
                DeathVoiceRosterClient.participants().size());
        int y = feed.bottom - headSize - 7
                - deadVisible * (headSize + HEAD_GAP) - HOST_EXTRA_GAP;

        Minecraft minecraft = Minecraft.getInstance();
        String hovered = null;
        for (Scp1576ClientState.SessionState state : activeHosts) {
            if (y < feed.top) break;
            renderHost(graphics, minecraft, state, headX, y, headSize, alpha);
            if (mouseX >= headX - 2 && mouseX < headX + headSize + 2
                    && mouseY >= y - 2 && mouseY < y + headSize + 2) {
                hovered = state.hostName();
            }
            y -= headSize + HEAD_GAP;
        }

        if (hovered != null && !hovered.isBlank()) {
            graphics.renderTooltip(minecraft.font, Component.literal(hovered),
                    mouseX, mouseY);
        }
    }

    private static List<Scp1576ClientState.SessionState> activeHosts() {
        Map<UUID, Scp1576ClientState.SessionState> unique = new LinkedHashMap<>();
        for (Scp1576ClientState.SessionState state
                : Scp1576ClientState.activeSessions()) {
            if (state.voiceOpen()) unique.putIfAbsent(state.hostId(), state);
        }
        return new ArrayList<>(unique.values());
    }

    private static void renderHost(GuiGraphics graphics, Minecraft minecraft,
            Scp1576ClientState.SessionState state, int x, int y, int size,
            float alpha) {
        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2,
                withAlpha(BORDER, alpha));
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1,
                withAlpha(0xE006090C, alpha));

        ResourceLocation skin = skin(minecraft, state.hostId());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(skin, x, y, size, size,
                8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(skin, x, y, size, size,
                40.0F, 8.0F, 8, 8, 64, 64);

        int iconSize = Math.min(14, size - 4);
        int iconX = x - iconSize - 4;
        int iconY = y + (size - iconSize) / 2;
        graphics.blit(SCP1576_ICON, iconX, iconY, iconSize, iconSize,
                0.0F, 0.0F, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static ResourceLocation skin(Minecraft minecraft, UUID hostId) {
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(hostId);
            if (info != null) return info.getSkinLocation();
        }
        return DefaultPlayerSkin.getDefaultSkin(hostId);
    }

    private static int finalCardRight(ScpDeathScreen screen) {
        int cardWidth = screen.width < 700
                ? Mth.clamp(Math.round(screen.width * 0.52F), 320, 410)
                : Mth.clamp(Math.round(screen.width * 0.42F), 340, 470);
        int left = Math.max(20, Math.round(screen.width * 0.035F));
        return left + cardWidth;
    }

    private static Bounds feed(ScpDeathScreen screen) {
        int left = Math.max(screen.width / 2 + 28,
                Math.round(screen.width * 0.53F));
        int right = Math.max(left + 120, screen.width - 28);
        int top = Math.max(46, Math.round(screen.height * 0.095F));
        int bottom = Math.max(top + 100, screen.height - 64);
        return new Bounds(left, top, right, bottom);
    }

    private static int withAlpha(int color, float alpha) {
        int sourceAlpha = color >>> 24 & 0xFF;
        int a = Mth.clamp(Math.round(sourceAlpha
                * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return a << 24 | color & 0x00FFFFFF;
    }

    private record Bounds(int left, int top, int right, int bottom) {
    }
}
