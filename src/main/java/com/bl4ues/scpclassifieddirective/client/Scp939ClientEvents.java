package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.SimpleVoiceChatPresence;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.network.Scp939InputPacket;
import com.bl4ues.scpclassifieddirective.network.Scp939Network;

import java.util.Set;
import java.util.UUID;

/** Client input and authored HUD/camera feedback for SCP-939 interactions. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp939ClientEvents {
    private static final int BAR_WIDTH = 156;
    private static final int BAR_HEIGHT = 8;
    private static final int CROSSHAIR_GAP = 22;
    private static final int TRACK = 0x7710181B;
    private static final int TRACK_DARK = 0xAA0B1012;
    private static final int BORDER = 0x997A8790;
    private static final int FULL_BLUE = 0xFF9CEBFF;
    private static final int MID_BLUE = 0xFF68BFE0;
    private static final int WARNING_RED = 0xFFED5E55;
    private static final int CRITICAL_RED = 0xFFFF2A2A;
    private static final int CONSENT_REFRESH_TICKS = 20 * 10;

    private static final Set<String> PIN_HIDDEN_VANILLA_OVERLAYS = Set.of(
            "hotbar", "crosshair", "boss_event_progress", "player_health",
            "armor_level", "food_level", "mount_health", "air_level",
            "jump_bar", "experience_bar", "item_name", "potion_icons",
            "record_overlay", "player_list");

    private static boolean previousHold;
    private static boolean previousLeft;
    private static boolean previousRight;
    private static UUID consentPlayer;
    private static int consentRefreshTicks;

    private Scp939ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            previousHold = previousLeft = previousRight = false;
            consentPlayer = null;
            consentRefreshTicks = 0;
            Scp939ClientState.clear();
            return;
        }

        syncMimicConsent(minecraft.player.getUUID());

        boolean hold = Scp939Keybinds.HOLD_BREATH.isDown();
        if (hold != previousHold) {
            previousHold = hold;
            Scp939Network.sendInput(Scp939InputPacket.HOLD_BREATH, hold);
        }

        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();
        if (Scp939ClientState.pinned()) {
            if (left && !previousLeft) {
                Scp939Network.sendInput(Scp939InputPacket.STRUGGLE_LEFT, true);
            }
            if (right && !previousRight) {
                Scp939Network.sendInput(Scp939InputPacket.STRUGGLE_RIGHT, true);
            }
        }
        previousLeft = left;
        previousRight = right;
    }

    /** Called by the integration menu as the client-local mimicry switch. */
    public static void setMimicConsent(boolean allowed) {
        Scp939VoiceClientPreferences.completeSetup(allowed);
        consentRefreshTicks = CONSENT_REFRESH_TICKS;
        Minecraft minecraft = Minecraft.getInstance();
        if (!SimpleVoiceChatPresence.installed() || minecraft.player == null
                || minecraft.getConnection() == null) {
            return;
        }
        consentPlayer = minecraft.player.getUUID();
        Scp939Network.sendInput(Scp939InputPacket.MIMIC_CONSENT, allowed);
    }

    private static void syncMimicConsent(UUID playerId) {
        if (!SimpleVoiceChatPresence.installed()
                || !Scp939VoiceClientPreferences.setupComplete()) {
            return;
        }
        boolean newPlayer = !playerId.equals(consentPlayer);
        if (newPlayer) {
            consentPlayer = playerId;
            consentRefreshTicks = CONSENT_REFRESH_TICKS;
            Scp939Network.sendInput(Scp939InputPacket.MIMIC_CONSENT,
                    Scp939VoiceClientPreferences.allowMimicry());
            return;
        }
        if (--consentRefreshTicks <= 0) {
            consentRefreshTicks = CONSENT_REFRESH_TICKS;
            Scp939Network.sendInput(Scp939InputPacket.MIMIC_CONSENT,
                    Scp939VoiceClientPreferences.allowMimicry());
        }
    }

    /** Keep the pin sequence readable by removing ordinary gameplay HUD chrome. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBeforeOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!Scp939ClientState.pinned()) return;
        if (PIN_HIDDEN_VANILLA_OVERLAYS.contains(
                event.getOverlay().id().getPath())) {
            event.setCanceled(true);
        }
    }

    /** Rendered from the dedicated SCP-939 above-all overlay registration. */
    public static void renderOverlay(GuiGraphics graphics, int width,
            int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen != null) return;

        if (Scp939ClientState.pinned()) {
            renderStruggle(graphics, minecraft, width, height);
        } else if (Scp939ClientState.breathActive()) {
            renderBreath(graphics, minecraft, width, height);
        }
    }

    private static void renderBreath(GuiGraphics graphics,
            Minecraft minecraft, int width, int height) {
        int x = (width - BAR_WIDTH) / 2;
        int y = height / 2 + CROSSHAIR_GAP;
        float ratio = Scp939ClientState.breathReserve();
        drawBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, ratio);

        Component key = ScpFonts.roboto(
                Scp939Keybinds.HOLD_BREATH.getTranslatedKeyMessage());
        String action = Scp939ClientState.holdingBreath() ? "RELEASE" : "HOLD";
        Component actionText = ScpFonts.roboto(action);
        int keyWidth = minecraft.font.width(key) + 10;
        int actionWidth = minecraft.font.width(actionText);
        int totalWidth = actionWidth + 5 + keyWidth;
        int promptX = width / 2 - totalWidth / 2;
        int promptY = y + BAR_HEIGHT + 6;
        int keyX = promptX + actionWidth + 5;
        int keyColor = Scp939ClientState.holdingBreath()
                ? ConfigCenterVisuals.ACCENT_BRIGHT : ConfigCenterVisuals.TEXT;

        graphics.drawString(minecraft.font, actionText, promptX, promptY,
                ConfigCenterVisuals.MUTED, false);
        graphics.fill(keyX, promptY - 3, keyX + keyWidth,
                promptY + minecraft.font.lineHeight + 3, 0xB50B0E12);
        graphics.fill(keyX, promptY - 3, keyX + keyWidth, promptY - 2,
                keyColor);
        graphics.drawString(minecraft.font, key, keyX + 5, promptY,
                keyColor, false);
    }

    private static void renderStruggle(GuiGraphics graphics,
            Minecraft minecraft, int width, int height) {
        int panelWidth = 184;
        int panelHeight = 54;
        int x = width / 2 - panelWidth / 2;
        int y = Math.max(8, height - panelHeight - 18);
        int centerX = width / 2;

        graphics.fill(x, y, x + panelWidth, y + panelHeight,
                0xD20B0E12);
        graphics.fill(x, y, x + panelWidth, y + 2,
                ConfigCenterVisuals.ACCENT);
        graphics.fill(x, y, x + 3, y + panelHeight,
                ConfigCenterVisuals.ACCENT);

        graphics.drawString(minecraft.font, ScpFonts.roboto("BREAK FREE"),
                x + 11, y + 9, ConfigCenterVisuals.TEXT, false);

        Component key = ScpFonts.roboto(Scp939ClientState.expectedKey() == 0
                ? minecraft.options.keyLeft.getTranslatedKeyMessage()
                : minecraft.options.keyRight.getTranslatedKeyMessage());
        Component press = ScpFonts.roboto("PRESS");
        int keyWidth = Math.max(30, minecraft.font.width(key) + 14);
        int pressWidth = minecraft.font.width(press);
        int promptWidth = pressWidth + 5 + keyWidth;
        int promptX = x + panelWidth - 11 - promptWidth;
        int keyX = promptX + pressWidth + 5;
        int keyY = y + 5;

        graphics.drawString(minecraft.font, press, promptX, y + 9,
                ConfigCenterVisuals.MUTED, false);
        graphics.fill(keyX, keyY, keyX + keyWidth, keyY + 20,
                0xE00D1116);
        graphics.fill(keyX, keyY, keyX + keyWidth, keyY + 2,
                ConfigCenterVisuals.ACCENT_BRIGHT);
        graphics.drawCenteredString(minecraft.font, key,
                keyX + keyWidth / 2, keyY + 7, ConfigCenterVisuals.TEXT);

        int progress = Math.min(Scp939Entity.STRUGGLE_SUCCESS_REQUIRED,
                Scp939ClientState.pinProgress());
        int failures = Math.min(Scp939Entity.STRUGGLE_FAILURE_LIMIT,
                Scp939ClientState.pinFailures());
        drawPips(graphics, x + 11, y + 31, progress,
                Scp939Entity.STRUGGLE_SUCCESS_REQUIRED,
                ConfigCenterVisuals.GREEN);
        drawPipsRight(graphics, x + panelWidth - 11, y + 31, failures,
                Scp939Entity.STRUGGLE_FAILURE_LIMIT,
                ConfigCenterVisuals.RED);

        int windowTicks = minecraft.level == null
                ? Scp939Entity.STRUGGLE_WINDOW_TICKS
                : Scp939Entity.struggleWindowTicks(
                        minecraft.level.getDifficulty());
        int remaining = Mth.clamp(Scp939ClientState.pinWindowTicks(), 0,
                windowTicks);
        float timeRatio = remaining / (float) windowTicks;
        int barX = x + 11;
        int barY = y + 43;
        int barWidth = panelWidth - 22;
        graphics.fill(barX, barY, barX + barWidth, barY + 4, 0xFF242A31);
        int fill = Math.round(barWidth * timeRatio);
        int color = lerpColor(ConfigCenterVisuals.RED,
                ConfigCenterVisuals.ACCENT_BRIGHT, timeRatio);
        if (fill > 0) {
            graphics.fill(barX, barY, barX + fill, barY + 4, color);
        }

        // A tiny center notch makes it immediately legible as a timed input
        // without reintroducing the old wall of labels and counters.
        graphics.fill(centerX, barY - 1, centerX + 1, barY + 5,
                0x887A8790);
    }

    private static void drawPips(GuiGraphics graphics, int x, int y,
            int filled, int count, int color) {
        for (int i = 0; i < count; i++) {
            int left = x + i * 12;
            graphics.fill(left, y, left + 8, y + 5,
                    i < filled ? color : 0xFF343B44);
        }
    }

    private static void drawPipsRight(GuiGraphics graphics, int right, int y,
            int filled, int count, int color) {
        for (int i = 0; i < count; i++) {
            int left = right - (i + 1) * 12 + 4;
            graphics.fill(left, y, left + 8, y + 5,
                    i < filled ? color : 0xFF343B44);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!Scp939ClientState.pinned()) return;
        float time = Scp939ClientState.pinElapsedSeconds();
        float impact = 1.0F - Mth.clamp(time / 0.75F, 0.0F, 1.0F);
        float stress = 0.78F + Scp939ClientState.pinFailures() * 0.18F
                + impact * 0.90F;
        float rapid = time * 31.0F;
        float slow = time * 14.0F;
        event.setYaw(event.getYaw()
                + Mth.sin(rapid) * 0.48F * stress
                + Mth.sin(slow + 1.3F) * 0.30F * stress);
        event.setPitch(event.getPitch()
                + Mth.sin(time * 24.0F + 0.7F) * 0.70F * stress);
        event.setRoll(event.getRoll()
                + Mth.sin(time * 19.0F + 2.1F) * 0.88F * stress);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y,
            int width, int height, float ratio) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, TRACK);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, TRACK_DARK);

        int fillWidth = Math.max(0,
                Math.min(width - 2, Math.round((width - 2) * ratio)));
        int bright = oxygenColor(ratio);
        int dark = darken(bright, 0.60F);
        if (fillWidth > 0) {
            for (int i = 0; i < fillWidth; i++) {
                float progress = fillWidth <= 1
                        ? 1.0F : i / (float) (fillWidth - 1);
                graphics.fill(x + 1 + i, y + 1, x + 2 + i,
                        bottom - 1, lerpColor(dark, bright, progress));
            }

            int markerX = Math.min(right - 2, x + fillWidth);
            graphics.fill(markerX, y - 2, markerX + 1, bottom + 2,
                    withAlpha(bright, 0.88F));
        }

        graphics.fill(x, y, right, y + 1, BORDER);
        graphics.fill(x, bottom - 1, right, bottom, BORDER);
        graphics.fill(x, y, x + 1, bottom, BORDER);
        graphics.fill(right - 1, y, right, bottom, BORDER);
    }

    private static int oxygenColor(float ratio) {
        float value = Mth.clamp(ratio, 0.0F, 1.0F);
        if (value >= 0.50F) {
            return lerpColor(MID_BLUE, FULL_BLUE,
                    (value - 0.50F) / 0.50F);
        }
        if (value >= 0.20F) {
            return lerpColor(WARNING_RED, MID_BLUE,
                    (value - 0.20F) / 0.30F);
        }
        return lerpColor(CRITICAL_RED, WARNING_RED, value / 0.20F);
    }

    private static int darken(int color, float factor) {
        int alpha = color >>> 24;
        int red = Math.round(((color >> 16) & 0xFF) * factor);
        int green = Math.round(((color >> 8) & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int lerpColor(int from, int to, float amount) {
        float value = Mth.clamp(amount, 0.0F, 1.0F);
        int alpha = Math.round(((from >>> 24) & 0xFF)
                + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * value);
        int red = Math.round(((from >> 16) & 0xFF)
                + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * value);
        int green = Math.round(((from >> 8) & 0xFF)
                + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * value);
        int blue = Math.round((from & 0xFF)
                + ((to & 0xFF) - (from & 0xFF)) * value);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int withAlpha(int color, float alpha) {
        int value = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return (value << 24) | (color & 0x00FFFFFF);
    }
}
