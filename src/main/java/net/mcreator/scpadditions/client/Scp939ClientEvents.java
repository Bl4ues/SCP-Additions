package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.compat.SimpleVoiceChatPresence;
import net.mcreator.scpadditions.config.ui.ConfigCenterVisuals;
import net.mcreator.scpadditions.network.Scp939InputPacket;
import net.mcreator.scpadditions.network.Scp939Network;

import java.util.UUID;

/** Client input and authored HUD/camera feedback for SCP-939 interactions. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
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

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        // Render exactly once in Forge's overlay pass. Without this guard the
        // translucent panels are drawn once for every vanilla overlay.
        if (event.getOverlay() != VanillaGuiOverlay.PORTAL.type()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen != null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        if (Scp939ClientState.breathActive()) {
            renderBreath(graphics, minecraft, width, height);
        }
        if (Scp939ClientState.pinned()) {
            renderStruggle(graphics, minecraft, width, height);
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
        graphics.fill(keyX, promptY + minecraft.font.lineHeight + 2,
                keyX + keyWidth, promptY + minecraft.font.lineHeight + 3,
                0x663A424D);
        graphics.drawString(minecraft.font, key, keyX + 5, promptY,
                keyColor, false);
    }

    private static void renderStruggle(GuiGraphics graphics,
            Minecraft minecraft, int width, int height) {
        int panelWidth = 196;
        int panelHeight = 82;
        int x = width / 2 - panelWidth / 2;
        int y = height / 2 + 18;
        int centerX = width / 2;

        graphics.fill(x, y, x + panelWidth, y + panelHeight,
                ConfigCenterVisuals.PANEL_STRONG);
        graphics.fill(x, y, x + panelWidth, y + 2,
                ConfigCenterVisuals.ACCENT);
        graphics.fill(x, y, x + 4, y + panelHeight,
                ConfigCenterVisuals.ACCENT);

        graphics.drawString(minecraft.font, ScpFonts.roboto("BREAK FREE"),
                x + 14, y + 10, ConfigCenterVisuals.TEXT, false);

        Component key = ScpFonts.roboto(Scp939ClientState.expectedKey() == 0
                ? minecraft.options.keyLeft.getTranslatedKeyMessage()
                : minecraft.options.keyRight.getTranslatedKeyMessage());
        int keyWidth = Math.max(54, minecraft.font.width(key) + 18);
        int keyX = centerX - keyWidth / 2;
        int keyY = y + 27;
        graphics.fill(keyX, keyY, keyX + keyWidth, keyY + 24,
                0xD00D1116);
        graphics.fill(keyX, keyY, keyX + keyWidth, keyY + 2,
                ConfigCenterVisuals.ACCENT_BRIGHT);
        graphics.fill(keyX, keyY + 23, keyX + keyWidth, keyY + 24,
                ConfigCenterVisuals.BORDER);
        graphics.drawCenteredString(minecraft.font, key,
                centerX, keyY + 8, ConfigCenterVisuals.TEXT);

        int progress = Math.min(3, Scp939ClientState.pinProgress());
        int failures = Math.min(3, Scp939ClientState.pinFailures());
        int pipY = y + 57;
        drawPips(graphics, x + 15, pipY, progress, ConfigCenterVisuals.GREEN);
        drawPips(graphics, x + panelWidth - 54, pipY, failures,
                ConfigCenterVisuals.RED);

        int remaining = Mth.clamp(Scp939ClientState.pinWindowTicks(), 0, 20);
        float timeRatio = remaining / 20.0F;
        int barX = x + 63;
        int barY = y + 61;
        int barWidth = 70;
        graphics.fill(barX, barY, barX + barWidth, barY + 5, 0xFF242A31);
        int fill = Math.round(barWidth * timeRatio);
        int color = lerpColor(ConfigCenterVisuals.RED,
                ConfigCenterVisuals.ACCENT_BRIGHT, timeRatio);
        if (fill > 0) graphics.fill(barX, barY, barX + fill, barY + 5, color);

        graphics.drawString(minecraft.font, ScpFonts.roboto("ESCAPE"),
                x + 14, y + 70, ConfigCenterVisuals.MUTED, false);
        Component danger = ScpFonts.roboto("DANGER");
        graphics.drawString(minecraft.font, danger,
                x + panelWidth - 14 - minecraft.font.width(danger), y + 70,
                ConfigCenterVisuals.MUTED, false);
    }

    private static void drawPips(GuiGraphics graphics, int x, int y,
            int filled, int color) {
        for (int i = 0; i < 3; i++) {
            int left = x + i * 13;
            graphics.fill(left, y, left + 9, y + 6,
                    i < filled ? color : 0xFF343B44);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!Scp939ClientState.pinned()) return;
        float time = Scp939ClientState.pinElapsedSeconds();
        float impact = 1.0F - Mth.clamp(time / 0.75F, 0.0F, 1.0F);
        float stress = 0.75F + Scp939ClientState.pinFailures() * 0.16F
                + impact * 0.85F;
        float rapid = time * 31.0F;
        float slow = time * 14.0F;
        event.setYaw(event.getYaw()
                + Mth.sin(rapid) * 0.42F * stress
                + Mth.sin(slow + 1.3F) * 0.28F * stress);
        event.setPitch(event.getPitch()
                + Mth.sin(time * 24.0F + 0.7F) * 0.62F * stress);
        event.setRoll(event.getRoll()
                + Mth.sin(time * 19.0F + 2.1F) * 0.78F * stress);
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
