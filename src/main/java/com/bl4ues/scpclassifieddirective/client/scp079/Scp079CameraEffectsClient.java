package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Low-light and display-switch effects composited before the curved CRT pass. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CameraEffectsClient {
    private static final long INTERFERENCE_NANOS = 300_000_000L;
    private static final long AUDIO_FADE_IN_NANOS = 105_000_000L;
    private static final long AUDIO_FADE_OUT_NANOS = 165_000_000L;

    private static Vec3 lastFeedPosition;
    private static DisplayMode lastMode = DisplayMode.INACTIVE;
    private static long interferenceStartedAt;
    private static long interferenceUntil;

    private Scp079CameraEffectsClient() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        DisplayMode mode = mode(minecraft);
        if (mode == DisplayMode.INACTIVE) {
            lastMode = DisplayMode.INACTIVE;
            lastFeedPosition = null;
            interferenceStartedAt = 0L;
            interferenceUntil = 0L;
            return;
        }

        if (mode != lastMode) {
            // Local Host and the boot terminal are not surveillance feeds. Do
            // not pretend they are camera switches on entry; the authored
            // interference belongs to the hand-off after boot completes.
            boolean initialLocal = lastMode == DisplayMode.INACTIVE
                    && mode == DisplayMode.LOCAL;
            if (mode != DisplayMode.BOOT && !initialLocal) {
                startTransition(INTERFERENCE_NANOS);
            }
            if (lastMode != DisplayMode.BOOT
                    && (mode == DisplayMode.MAP
                    || mode == DisplayMode.LOCAL
                    && lastMode == DisplayMode.CAMERA)) {
                Scp079PlayableAudioClient.playDisplaySwitch();
            }
            lastMode = mode;
        }

        if (mode == DisplayMode.CAMERA) {
            Vec3 current = Scp079PlayableClient.viewPosition();
            if (lastFeedPosition == null
                    || current.distanceToSqr(lastFeedPosition) > 0.25D) {
                // Authored cross-floor/cross-zone waits are started before the
                // state reaches us. Any remaining direct feed change uses only
                // the short mask, including same-floor switches.
                startTransition(INTERFERENCE_NANOS);
                lastFeedPosition = current;
            }
        } else {
            lastFeedPosition = null;
        }
    }

    public static float transitionEnvelope() {
        if (!Scp079PlayableClient.active() || interferenceStartedAt <= 0L) {
            return 0.0F;
        }
        long now = System.nanoTime();
        if (now >= interferenceUntil) return 0.0F;
        float fadeIn = smootherStep(Mth.clamp(
                (now - interferenceStartedAt) / (float) AUDIO_FADE_IN_NANOS,
                0.0F, 1.0F));
        float fadeOut = smootherStep(Mth.clamp(
                (interferenceUntil - now) / (float) AUDIO_FADE_OUT_NANOS,
                0.0F, 1.0F));
        return Mth.clamp(fadeIn * fadeOut, 0.0F, 1.0F);
    }

    /**
     * Night-vision sensor changes deliberately use the same interference and
     * transition loop as a camera feed hand-off, instead of silently fading
     * between colour and monochrome like a phone accessibility setting.
     */
    static void triggerSensorTransition() {
        if (Scp079PlayableClient.cameraMode()) {
            startTransition(INTERFERENCE_NANOS);
        }
    }

    private static void startTransition(long durationNanos) {
        long now = System.nanoTime();
        interferenceStartedAt = now;
        interferenceUntil = now + Math.max(INTERFERENCE_NANOS,
                durationNanos);
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * Draw after the entire 079 HUD. The interference is a mask, not a handful
     * of decorative scan lines: while a network hand-off is in progress no old
     * or new feed/UI state should visibly leak through it.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!Scp079PlayableClient.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        float lowLight = Scp079NightVisionPostProcessor.strength();
        if (Scp079PlayableClient.cameraMode() && lowLight > 0.04F
                && System.nanoTime() >= interferenceUntil) {
            int alpha = Mth.clamp(Math.round(lowLight * 255.0F), 0, 255);
            Scp079UiTheme.draw(event.getGuiGraphics(), minecraft.font,
                    "NIGHT-VISION MODE ACTIVE", 24, height - 50,
                    1.08F, (alpha << 24) | 0x0079DDF3);
        }
        renderInterference(event.getGuiGraphics(), width, height);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!Scp079PlayableClient.active()
                || !(event.getScreen() instanceof Scp079FacilityMapScreen
                || event.getScreen() instanceof Scp079LeaveRoleScreen)) return;
        renderInterference(event.getGuiGraphics(),
                event.getScreen().width, event.getScreen().height);
    }

    private static void renderInterference(GuiGraphics graphics,
            int width, int height) {
        long now = System.nanoTime();
        if (now >= interferenceUntil || width <= 0 || height <= 0) return;

        long frame = now / 7_000_000L;
        // Opaque carrier first. This is the part that actually masks the camera
        // teleport; the animated noise below makes it read as video interference.
        graphics.fill(0, 0, width, height, 0xFF061017);

        for (int y = 0; y < height; y += 4) {
            int hash = mix((int) (frame * 31L + y * 131L));
            int luminance = 12 + (hash >>> 24 & 0x2F);
            int red = Math.max(3, luminance / 3);
            int green = Math.min(94, luminance + 7);
            int blue = Math.min(118, luminance + 18);
            int color = 0xFF000000 | red << 16 | green << 8 | blue;
            graphics.fill(0, y, width, Math.min(height, y + 3), color);
        }

        for (int i = 0; i < 34; i++) {
            int hash = mix((int) (frame * 911L + i * 3571L));
            int y = Math.floorMod(hash, height);
            int strip = 1 + Math.floorMod(hash >>> 7, 7);
            int x = Math.floorMod(hash >>> 13, Math.max(1, width));
            int run = Math.max(10, Math.floorMod(hash >>> 19,
                    Math.max(11, width / 2)));
            int left = Math.max(0, x - run / 3);
            int right = Math.min(width, left + run);
            int color = switch (i % 4) {
                case 0 -> 0xFFE7F7FF;
                case 1 -> 0xFF76AFC4;
                case 2 -> 0xFF172D38;
                default -> 0xFF9BC5D3;
            };
            graphics.fill(left, y, right, Math.min(height, y + strip), color);
        }

        // A few displaced full-width tears keep long 0.8/1.5 s transfers from
        // looking like a frozen loading screen.
        for (int i = 0; i < 7; i++) {
            int y = Math.floorMod((int) (frame * (23L + i * 4L) + i * 71L),
                    height);
            int h = 1 + Math.floorMod((int) (frame + i * 5L), 4);
            graphics.fill(0, y, width, Math.min(height, y + h),
                    i % 2 == 0 ? 0xFFD4EEF7 : 0xFF0A1D27);
        }
    }

    private static int mix(int value) {
        int x = value;
        x ^= x >>> 16;
        x *= 0x7FEB352D;
        x ^= x >>> 15;
        x *= 0x846CA68B;
        return x ^ x >>> 16;
    }

    private static DisplayMode mode(Minecraft minecraft) {
        if (!Scp079PlayableClient.active()) return DisplayMode.INACTIVE;
        if (minecraft.screen instanceof Scp079BootSequenceScreen) {
            return DisplayMode.BOOT;
        }
        if (minecraft.screen instanceof Scp079FacilityMapScreen
                || minecraft.screen instanceof Scp079LeaveRoleScreen) {
            return DisplayMode.MAP;
        }
        return Scp079PlayableClient.cameraMode()
                ? DisplayMode.CAMERA : DisplayMode.LOCAL;
    }

    private enum DisplayMode { INACTIVE, LOCAL, BOOT, MAP, CAMERA }
}
