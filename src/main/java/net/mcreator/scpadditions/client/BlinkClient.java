package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.entity.Scp173Sounds;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.network.BlinkInputStatePacket;

public final class BlinkClient {
    private static final ResourceLocation EYE_ICON = new ResourceLocation("scpinventory", "textures/gui/eye.png");
    private static final ResourceLocation VIGNETTE = new ResourceLocation("scpinventory", "textures/gui/vignette.png");
    private static final int VIGNETTE_SOURCE_WIDTH = 1920;
    private static final int VIGNETTE_SOURCE_HEIGHT = 1080;
    private static final int BASE_BLINK_INTERVAL_TICKS = 200;
    private static final int LUBRICATED_BLINK_INTERVAL_TICKS = BASE_BLINK_INTERVAL_TICKS * 2;
    private static final int BLINK_DARK_TICKS = 6;
    private static final int POST_BLINK_COVER_TICKS = 2;
    private static final int SCARE_SOUND_COOLDOWN_TICKS = 120;
    private static final int HORROR_SOUND_COOLDOWN_TICKS = 120;
    private static final int FADE_OUT_TICKS = 70;
    private static final float EYE_SORE_BLINK_DRAIN_MULTIPLIER = 1.5F;
    private static final int TRACK = 0x7710181B;
    private static final int TRACK_DARK = 0xAA0B1012;
    private static final int BORDER = 0x996A6C6C;
    private static final int BLINK_LEFT = 0xCC879193;
    private static final int BLINK_RIGHT = 0xDDEDF6F7;
    private static final int EYE_SORE_TRACK_DARK = 0xAA1E0909;
    private static final int EYE_SORE_BORDER = 0xAA8E2727;
    private static final int EYE_SORE_LEFT = 0xDDBB2525;
    private static final int EYE_SORE_RIGHT = 0xFFFF7070;
    private static final int LUBRICATED_TRACK_DARK = 0xAA071D2A;
    private static final int LUBRICATED_BORDER = 0xAA2C83B8;
    private static final int LUBRICATED_LEFT = 0xDD3B9EDB;
    private static final int LUBRICATED_RIGHT = 0xFF8BD8FF;

    private static boolean active;
    private static int timerTicks = BASE_BLINK_INTERVAL_TICKS;
    private static int currentIntervalTicks = BASE_BLINK_INTERVAL_TICKS;
    private static int darkTicks;
    private static int postBlinkCoverTicks;
    private static int scareSoundCooldownTicks;
    private static int horrorSoundCooldownTicks;
    private static int fadeOutTicks;
    private static boolean lastSyncedClosed;
    private static int blinkSyncTicks;
    private static float visualAlpha;
    private static float blinkDrainRemainder;

    private BlinkClient() {
    }

    public static void setActive(boolean value) {
        value &= ScpAdditionsModulesConfig.get().blink.enabled;
        if (active == value) return;
        active = value;
        if (active) {
            currentIntervalTicks = getBlinkIntervalTicks(Minecraft.getInstance());
            timerTicks = currentIntervalTicks;
            blinkDrainRemainder = 0.0F;
            fadeOutTicks = 0;
            visualAlpha = 1.0F;
            playHorrorSound();
        } else {
            darkTicks = 0;
            postBlinkCoverTicks = 0;
            blinkDrainRemainder = 0.0F;
            fadeOutTicks = FADE_OUT_TICKS;
            visualAlpha = Math.max(visualAlpha, 1.0F);
            syncBlinkClosed(false, true);
        }
    }

    public static boolean isBlinkClosedLocally() {
        return ScpAdditionsModulesConfig.get().blink.enabled
                && active
                && (darkTicks > 0 || postBlinkCoverTicks > 0 || Scp173Keybinds.BLINK.isDown());
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!ScpAdditionsModulesConfig.get().blink.enabled) {
            setActive(false);
            darkTicks = 0;
            postBlinkCoverTicks = 0;
            visualAlpha = 0.0F;
            fadeOutTicks = 0;
            blinkDrainRemainder = 0.0F;
            syncBlinkClosed(false, false);
            return;
        }
        if (mc == null || !mc.isPaused()) {
            if (scareSoundCooldownTicks > 0) scareSoundCooldownTicks--;
            if (horrorSoundCooldownTicks > 0) horrorSoundCooldownTicks--;
        }
        if (mc.player == null || mc.level == null || mc.player.isSpectator()) {
            setActive(false);
            darkTicks = 0;
            postBlinkCoverTicks = 0;
            visualAlpha = 0.0F;
            fadeOutTicks = 0;
            blinkDrainRemainder = 0.0F;
            syncBlinkClosed(false, false);
            return;
        }

        boolean creative = mc.player.isCreative();
        if (creative) {
            // Creative players remain excluded from automatic SCP-173 activation.
            // Since the Blink Bar is inactive, manual blinking is unavailable too.
            setActive(false);
            visualAlpha = 0.0F;
            fadeOutTicks = 0;
            blinkDrainRemainder = 0.0F;
        } else {
            updateVisualFade();
        }

        if (mc.isPaused()) return;
        if (!active) {
            syncBlinkClosed(false, false);
            return;
        }

        updateBlinkInterval(mc);

        // Manual blinking is part of the active encounter system and is only
        // accepted while the Blink Bar is present.
        if (mc.screen == null && Scp173Keybinds.BLINK.isDown()) {
            holdBlinkClosed();
            syncBlinkClosed(true, false);
            return;
        }
        if (darkTicks > 0) {
            darkTicks--;
            syncBlinkClosed(true, false);
            if (darkTicks <= 0) postBlinkCoverTicks = POST_BLINK_COVER_TICKS;
            return;
        }
        if (postBlinkCoverTicks > 0) {
            postBlinkCoverTicks--;
            syncBlinkClosed(true, false);
            if (postBlinkCoverTicks <= 0) {
                timerTicks = currentIntervalTicks;
                blinkDrainRemainder = 0.0F;
                syncBlinkClosed(false, true);
            }
            return;
        }
        syncBlinkClosed(false, false);
        if (timerTicks > 0) timerTicks = Math.max(0, timerTicks - getBlinkDrainTicks(mc));
        if (timerTicks <= 0) {
            blink();
            syncBlinkClosed(true, true);
        }
    }

    public static void renderVignette(GuiGraphics graphics, int width, int height) {
        if (visualAlpha <= 0.01F) return;
        drawVignettePass(graphics, width, height, visualAlpha);
        drawVignettePass(graphics, width, height, visualAlpha * 0.45F);
    }

    public static void renderBlackout(GuiGraphics graphics, int width, int height) {
        if (darkTicks > 0 || postBlinkCoverTicks > 0) graphics.fill(0, 0, width, height, 0xFF000000);
    }

    public static void renderHud(GuiGraphics graphics, int width, int height, float partialTick) {
        if (visualAlpha > 0.01F) drawMeter(graphics, width, height, visualAlpha);
    }

    public static void playScareSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || scareSoundCooldownTicks > 0) return;
        scareSoundCooldownTicks = SCARE_SOUND_COOLDOWN_TICKS;
        mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), Scp173Sounds.SCARE.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F, false);
    }

    private static void playHorrorSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || horrorSoundCooldownTicks > 0) return;
        horrorSoundCooldownTicks = HORROR_SOUND_COOLDOWN_TICKS;
        mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), Scp173Sounds.HORROR.get(),
                SoundSource.HOSTILE, 0.85F, 1.0F, false);
    }

    private static void updateVisualFade() {
        if (active) {
            visualAlpha = 1.0F;
            fadeOutTicks = 0;
        } else if (fadeOutTicks > 0) {
            fadeOutTicks--;
            visualAlpha = fadeOutTicks / (float) FADE_OUT_TICKS;
        } else visualAlpha = 0.0F;
    }

    private static boolean hasEyeSore(Minecraft mc) {
        return mc != null && mc.player != null && mc.player.hasEffect(ScpAdditionsModMobEffects.EYE_SORE.get());
    }

    private static boolean hasLubricatedEye(Minecraft mc) {
        return mc != null && mc.player != null
                && mc.player.hasEffect(ScpAdditionsModMobEffects.LUBRICATED_EYE.get());
    }

    private static int getBlinkIntervalTicks(Minecraft mc) {
        return hasLubricatedEye(mc) ? LUBRICATED_BLINK_INTERVAL_TICKS : BASE_BLINK_INTERVAL_TICKS;
    }

    private static void updateBlinkInterval(Minecraft mc) {
        int nextInterval = getBlinkIntervalTicks(mc);
        if (nextInterval == currentIntervalTicks) return;
        if (timerTicks > 0 && darkTicks <= 0 && postBlinkCoverTicks <= 0) {
            timerTicks = Math.max(0, Math.min(nextInterval,
                    Math.round(timerTicks * (nextInterval / (float) currentIntervalTicks))));
        }
        currentIntervalTicks = nextInterval;
    }

    private static int getBlinkDrainTicks(Minecraft mc) {
        blinkDrainRemainder += hasEyeSore(mc) ? EYE_SORE_BLINK_DRAIN_MULTIPLIER : 1.0F;
        int wholeTicks = (int) blinkDrainRemainder;
        blinkDrainRemainder -= wholeTicks;
        return Math.max(1, wholeTicks);
    }

    private static void blink() {
        darkTicks = BLINK_DARK_TICKS;
        postBlinkCoverTicks = 0;
    }

    private static void holdBlinkClosed() {
        if (darkTicks <= 0 && postBlinkCoverTicks <= 0) darkTicks = BLINK_DARK_TICKS;
        postBlinkCoverTicks = 0;
        timerTicks = currentIntervalTicks;
        blinkDrainRemainder = 0.0F;
    }

    private static void syncBlinkClosed(boolean closed, boolean force) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null || mc.player == null || mc.level == null) {
            blinkSyncTicks = 0;
            lastSyncedClosed = closed;
            return;
        }
        blinkSyncTicks++;
        if (!force && closed == lastSyncedClosed && (!closed || blinkSyncTicks < 2)) return;
        blinkSyncTicks = 0;
        lastSyncedClosed = closed;
        boolean manual = closed && active && Scp173Keybinds.BLINK.isDown();
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(new BlinkInputStatePacket(closed, manual));
    }

    private static void drawVignettePass(GuiGraphics graphics, int width, int height, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F,
                VIGNETTE_SOURCE_WIDTH, VIGNETTE_SOURCE_HEIGHT, VIGNETTE_SOURCE_WIDTH, VIGNETTE_SOURCE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawMeter(GuiGraphics graphics, int width, int height, float alpha) {
        int barWidth = 300, barHeight = 10;
        int x = (width - barWidth) / 2, y = height - 86;
        int iconSize = 20;
        float fill = Math.max(0.0F, Math.min(1.0F, timerTicks / (float) currentIntervalTicks));
        boolean lubricatedEye = hasLubricatedEye(Minecraft.getInstance());
        boolean eyeSore = !lubricatedEye && hasEyeSore(Minecraft.getInstance());
        drawIcon(graphics, x - iconSize - 6, y - 5, iconSize, alpha);
        drawBar(graphics, x, y, barWidth, barHeight, fill, alpha, eyeSore, lubricatedEye);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, int height,
            float ratio, float alpha, boolean eyeSore, boolean lubricatedEye) {
        int right = x + width, bottom = y + height;
        int trackDark = lubricatedEye ? LUBRICATED_TRACK_DARK : eyeSore ? EYE_SORE_TRACK_DARK : TRACK_DARK;
        int border = lubricatedEye ? LUBRICATED_BORDER : eyeSore ? EYE_SORE_BORDER : BORDER;
        int left = lubricatedEye ? LUBRICATED_LEFT : eyeSore ? EYE_SORE_LEFT : BLINK_LEFT;
        int rightColor = lubricatedEye ? LUBRICATED_RIGHT : eyeSore ? EYE_SORE_RIGHT : BLINK_RIGHT;
        graphics.fill(x, y, right, bottom, applyAlpha(TRACK, alpha));
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, applyAlpha(trackDark, alpha));
        int fillWidth = Math.max(0, Math.min(width - 2, Math.round((width - 2) * ratio)));
        for (int i = 0; i < fillWidth; i++) {
            float t = fillWidth <= 1 ? 1.0F : i / (float) (fillWidth - 1);
            graphics.fill(x + 1 + i, y + 1, x + 2 + i, bottom - 1, applyAlpha(lerpColor(left, rightColor, t), alpha));
        }
        int markerX = Math.min(right - 2, x + 1 + fillWidth);
        graphics.fill(markerX, y - 2, markerX + 1, bottom + 2, applyAlpha(withAlpha(rightColor, 0.78F), alpha));
        graphics.fill(x, y, right, y + 1, applyAlpha(border, alpha));
        graphics.fill(x, bottom - 1, right, bottom, applyAlpha(border, alpha));
    }

    private static void drawIcon(GuiGraphics graphics, int x, int y, int size, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(EYE_ICON, x, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static int applyAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
    private static int withAlpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, Math.round(alpha * 255.0F))) << 24);
    }
    private static int lerpColor(int from, int to, float t) {
        int fa = from >>> 24 & 255, fr = from >>> 16 & 255, fg = from >>> 8 & 255, fb = from & 255;
        int ta = to >>> 24 & 255, tr = to >>> 16 & 255, tg = to >>> 8 & 255, tb = to & 255;
        return Math.round(fa + (ta - fa) * t) << 24 | Math.round(fr + (tr - fr) * t) << 16
                | Math.round(fg + (tg - fg) * t) << 8 | Math.round(fb + (tb - fb) * t);
    }
}
