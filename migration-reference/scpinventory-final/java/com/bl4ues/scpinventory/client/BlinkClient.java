package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.effect.ModMobEffects;
import com.bl4ues.scpinventory.network.BlinkInputStatePacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.sound.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public final class BlinkClient {
    private static final ResourceLocation EYE_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/eye.png");
    private static final ResourceLocation VIGNETTE = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/vignette.png");
    private static final int VIGNETTE_SOURCE_WIDTH = 1920;
    private static final int VIGNETTE_SOURCE_HEIGHT = 1080;
    private static final int BLINK_INTERVAL_TICKS = 200;
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

    private static boolean active = false;
    private static int timerTicks = BLINK_INTERVAL_TICKS;
    private static int darkTicks = 0;
    private static int postBlinkCoverTicks = 0;
    private static int scareSoundCooldownTicks = 0;
    private static int horrorSoundCooldownTicks = 0;
    private static int fadeOutTicks = 0;
    private static boolean lastSyncedClosed = false;
    private static int blinkSyncTicks = 0;
    private static float visualAlpha = 0.0F;
    private static float blinkDrainRemainder = 0.0F;

    private BlinkClient() {
    }

    public static void setActive(boolean value) {
        if (active == value) {
            return;
        }
        active = value;
        if (active) {
            timerTicks = BLINK_INTERVAL_TICKS;
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
        return darkTicks > 0 || postBlinkCoverTicks > 0 || Keybinds.BLINK.isDown();
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !mc.isPaused()) {
            if (scareSoundCooldownTicks > 0) {
                scareSoundCooldownTicks--;
            }
            if (horrorSoundCooldownTicks > 0) {
                horrorSoundCooldownTicks--;
            }
        }
        if (mc.player == null || mc.level == null || mc.player.isCreative() || mc.player.isSpectator()) {
            setActive(false);
            visualAlpha = 0.0F;
            fadeOutTicks = 0;
            blinkDrainRemainder = 0.0F;
            return;
        }

        updateVisualFade();

        if (mc.isPaused()) {
            return;
        }
        if (!active) {
            syncBlinkClosed(false, false);
            return;
        }

        boolean blinkHeld = Keybinds.BLINK.isDown();
        if (blinkHeld) {
            holdBlinkClosed();
            syncBlinkClosed(true, false);
            return;
        }

        if (darkTicks > 0) {
            darkTicks--;
            syncBlinkClosed(true, false);
            if (darkTicks <= 0) {
                postBlinkCoverTicks = POST_BLINK_COVER_TICKS;
            }
            return;
        }

        if (postBlinkCoverTicks > 0) {
            postBlinkCoverTicks--;
            syncBlinkClosed(true, false);
            if (postBlinkCoverTicks <= 0) {
                timerTicks = BLINK_INTERVAL_TICKS;
                blinkDrainRemainder = 0.0F;
                syncBlinkClosed(false, true);
            }
            return;
        }

        syncBlinkClosed(false, false);

        if (timerTicks > 0) {
            timerTicks = Math.max(0, timerTicks - getBlinkDrainTicks(mc));
        }
        if (timerTicks <= 0) {
            blink();
            syncBlinkClosed(true, true);
        }
    }

    public static void renderVignette(GuiGraphics g, int width, int height) {
        if (visualAlpha <= 0.01F) {
            return;
        }
        drawVignettePass(g, width, height, visualAlpha);
        drawVignettePass(g, width, height, visualAlpha * 0.45F);
    }

    public static void renderBlackout(GuiGraphics g, int width, int height) {
        if (darkTicks <= 0 && postBlinkCoverTicks <= 0) {
            return;
        }
        g.fill(0, 0, width, height, 0xFF000000);
    }

    public static void renderHud(GuiGraphics g, int width, int height, float partialTick) {
        if (visualAlpha <= 0.01F) {
            return;
        }
        drawMeter(g, width, height, visualAlpha);
    }

    public static void playScareSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || scareSoundCooldownTicks > 0) {
            return;
        }
        scareSoundCooldownTicks = SCARE_SOUND_COOLDOWN_TICKS;
        mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), ModSounds.SCARE.get(), SoundSource.HOSTILE, 1.0F, 1.0F, false);
    }

    private static void playHorrorSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || horrorSoundCooldownTicks > 0) {
            return;
        }
        horrorSoundCooldownTicks = HORROR_SOUND_COOLDOWN_TICKS;
        mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), ModSounds.HORROR.get(), SoundSource.HOSTILE, 0.85F, 1.0F, false);
    }

    private static void updateVisualFade() {
        if (active) {
            visualAlpha = 1.0F;
            fadeOutTicks = 0;
            return;
        }
        if (fadeOutTicks > 0) {
            fadeOutTicks--;
            visualAlpha = fadeOutTicks / (float) FADE_OUT_TICKS;
        } else {
            visualAlpha = 0.0F;
        }
    }

    private static boolean hasEyeSore(Minecraft mc) {
        return mc != null && mc.player != null && mc.player.hasEffect(ModMobEffects.EYE_SORE.get());
    }

    private static int getBlinkDrainTicks(Minecraft mc) {
        float drain = hasEyeSore(mc) ? EYE_SORE_BLINK_DRAIN_MULTIPLIER : 1.0F;
        blinkDrainRemainder += drain;
        int wholeTicks = (int) blinkDrainRemainder;
        blinkDrainRemainder -= wholeTicks;
        return Math.max(1, wholeTicks);
    }

    private static void blink() {
        darkTicks = BLINK_DARK_TICKS;
        postBlinkCoverTicks = 0;
    }

    private static void holdBlinkClosed() {
        if (darkTicks <= 0 && postBlinkCoverTicks <= 0) {
            darkTicks = BLINK_DARK_TICKS;
        }
        postBlinkCoverTicks = 0;
        timerTicks = BLINK_INTERVAL_TICKS;
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
        if (!force && closed == lastSyncedClosed && (!closed || blinkSyncTicks < 2)) {
            return;
        }
        blinkSyncTicks = 0;
        lastSyncedClosed = closed;
        ModNetwork.CHANNEL.sendToServer(new BlinkInputStatePacket(closed));
    }

    private static void drawVignettePass(GuiGraphics g, int width, int height, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        g.blit(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, VIGNETTE_SOURCE_WIDTH, VIGNETTE_SOURCE_HEIGHT, VIGNETTE_SOURCE_WIDTH, VIGNETTE_SOURCE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawMeter(GuiGraphics g, int width, int height, float alpha) {
        int barW = 300;
        int barH = 10;
        int x = (width - barW) / 2;
        int y = height - 86;
        int iconSize = 20;
        int iconX = x - iconSize - 6;
        int iconY = y - 5;
        float fill = Math.max(0.0F, Math.min(1.0F, timerTicks / (float) BLINK_INTERVAL_TICKS));
        boolean eyeSore = hasEyeSore(Minecraft.getInstance());

        drawIcon(g, iconX, iconY, iconSize, alpha);
        drawBar(g, x, y, barW, barH, fill, alpha, eyeSore);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int width, int height, float ratio, float alpha, boolean eyeSore) {
        int right = x + width;
        int bottom = y + height;
        int trackDark = eyeSore ? EYE_SORE_TRACK_DARK : TRACK_DARK;
        int border = eyeSore ? EYE_SORE_BORDER : BORDER;
        int leftFill = eyeSore ? EYE_SORE_LEFT : BLINK_LEFT;
        int rightFill = eyeSore ? EYE_SORE_RIGHT : BLINK_RIGHT;

        g.fill(x, y, right, bottom, applyAlpha(TRACK, alpha));
        g.fill(x + 1, y + 1, right - 1, bottom - 1, applyAlpha(trackDark, alpha));

        int fillWidth = Math.max(0, Math.min(width - 2, Math.round((width - 2) * ratio)));
        if (fillWidth > 0) {
            for (int i = 0; i < fillWidth; i++) {
                float t = fillWidth <= 1 ? 1.0F : i / (float) (fillWidth - 1);
                g.fill(x + 1 + i, y + 1, x + 2 + i, bottom - 1, applyAlpha(lerpColor(leftFill, rightFill, t), alpha));
            }
        }

        int markerX = Math.min(right - 2, x + 1 + fillWidth);
        g.fill(markerX, y - 2, markerX + 1, bottom + 2, applyAlpha(withAlpha(rightFill, 0.78F), alpha));
    }

    private static void drawIcon(GuiGraphics g, int x, int y, int size, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        g.blit(EYE_ICON, x, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static int applyAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 0xFF) * alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static int lerpColor(int from, int to, float t) {
        int fa = (from >>> 24) & 0xFF;
        int fr = (from >>> 16) & 0xFF;
        int fg = (from >>> 8) & 0xFF;
        int fb = from & 0xFF;

        int ta = (to >>> 24) & 0xFF;
        int tr = (to >>> 16) & 0xFF;
        int tg = (to >>> 8) & 0xFF;
        int tb = to & 0xFF;

        int a = Math.round(fa + ((ta - fa) * t));
        int r = Math.round(fr + ((tr - fr) * t));
        int g = Math.round(fg + ((tg - fg) * t));
        int b = Math.round(fb + ((tb - fb) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
