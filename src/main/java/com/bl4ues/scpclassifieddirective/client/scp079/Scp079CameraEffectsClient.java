package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Low-light, CRT, hand-off and signal-loss effects for playable SCP-079. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CameraEffectsClient {
    public static final int SIGNAL_TEMPORARY = 0;
    public static final int SIGNAL_CAMERA_DISABLED = 1;
    public static final int SIGNAL_HOST_DESTROYED = 2;

    private static final long INTERFERENCE_NANOS = 300_000_000L;
    private static final long AUDIO_FADE_IN_NANOS = 105_000_000L;
    private static final long AUDIO_FADE_OUT_NANOS = 165_000_000L;
    private static final long CRT_FRAME_NANOS = 92_000_000L;
    private static final int CRT_FRAME_COUNT = 5;
    private static final int CRT_TEXTURE_WIDTH = 640;
    private static final int CRT_TEXTURE_HEIGHT = 360;
    private static final ResourceLocation NO_SIGNAL = resource(
            "textures/screens/nosignal.png");
    private static final ResourceLocation[] CRT_STATIC = new ResourceLocation[] {
            resource("textures/screens/crt_static_1.png"),
            resource("textures/screens/crt_static_2.png"),
            resource("textures/screens/crt_static_3.png"),
            resource("textures/screens/crt_static_4.png"),
            resource("textures/screens/crt_static_5.png")
    };

    private static Vec3 lastFeedPosition;
    private static DisplayMode lastMode = DisplayMode.INACTIVE;
    private static long interferenceStartedAt;
    private static long interferenceUntil;
    private static long signalStartedAt;
    private static long signalUntil;
    private static int signalKind = -1;

    private Scp079CameraEffectsClient() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.nanoTime();

        if (signalKind >= 0 && now >= signalUntil) {
            int completed = signalKind;
            signalKind = -1;
            signalStartedAt = 0L;
            signalUntil = 0L;
            if (completed == SIGNAL_CAMERA_DISABLED
                    && Scp079PlayableClient.active()
                    && Scp079PlayableClient.networkAvailable()) {
                Scp079FacilityMapScreen.open();
            }
        }

        if (signalEffectActive()) {
            // A dead/no-signal display is a modal hardware state. Do not allow
            // map, leave-role or inventory screens to become an alternate input
            // path under the visual mask.
            if (minecraft.screen != null && Scp079PlayableClient.active()) {
                minecraft.setScreen(null);
            }
        }

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

    /** Called by the server-authoritative interruption packet. */
    public static void beginSignalInterruption(int kind, int durationTicks) {
        if (kind < SIGNAL_TEMPORARY || kind > SIGNAL_HOST_DESTROYED) return;
        long now = System.nanoTime();
        long duration = Math.max(1, durationTicks) * 50_000_000L;
        signalKind = kind;
        signalStartedAt = now;
        signalUntil = now + duration;
        interferenceStartedAt = 0L;
        interferenceUntil = 0L;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null && Scp079PlayableClient.active()) {
            minecraft.setScreen(null);
        }
    }

    public static boolean controlsBlocked() {
        return signalEffectActive();
    }

    public static boolean signalEffectActive() {
        return signalKind >= 0 && System.nanoTime() < signalUntil;
    }

    public static boolean hostFailureActive() {
        return signalKind == SIGNAL_HOST_DESTROYED && signalEffectActive();
    }

    public static float transitionEnvelope() {
        long now = System.nanoTime();
        if (signalEffectActive()) {
            float fadeIn = smootherStep(Mth.clamp(
                    (now - signalStartedAt) / (float) AUDIO_FADE_IN_NANOS,
                    0.0F, 1.0F));
            float fadeOut = smootherStep(Mth.clamp(
                    (signalUntil - now) / (float) AUDIO_FADE_OUT_NANOS,
                    0.0F, 1.0F));
            return Mth.clamp(fadeIn * fadeOut, 0.0F, 1.0F);
        }
        if (!Scp079PlayableClient.active() || interferenceStartedAt <= 0L) {
            return 0.0F;
        }
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

    static void startTransition(long durationNanos) {
        if (signalEffectActive()) return;
        long now = System.nanoTime();
        interferenceStartedAt = now;
        interferenceUntil = now + Math.max(INTERFERENCE_NANOS,
                durationNanos);
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!Scp079PlayableClient.active() && !signalEffectActive()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        float lowLight = Scp079NightVisionPostProcessor.strength();
        if (!signalEffectActive() && Scp079PlayableClient.cameraMode()
                && lowLight > 0.04F && System.nanoTime() >= interferenceUntil) {
            int alpha = Mth.clamp(Math.round(lowLight * 255.0F), 0, 255);
            Scp079UiTheme.draw(event.getGuiGraphics(), minecraft.font,
                    "NIGHT-VISION MODE ACTIVE", 24, height - 50,
                    1.08F, (alpha << 24) | 0x0079DDF3);
        }
        renderComposite(event.getGuiGraphics(), width, height);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        boolean authored079Screen = event.getScreen() instanceof Scp079FacilityMapScreen
                || event.getScreen() instanceof Scp079LeaveRoleScreen
                || event.getScreen() instanceof Scp079BootSequenceScreen;
        if ((!Scp079PlayableClient.active() && !signalEffectActive())
                || (!authored079Screen && !signalEffectActive())) return;
        renderComposite(event.getGuiGraphics(),
                event.getScreen().width, event.getScreen().height);
    }

    private static void renderComposite(GuiGraphics graphics,
            int width, int height) {
        if (signalEffectActive()) {
            renderNoSignal(graphics, width, height);
            renderSignalNoise(graphics, width, height);
        } else {
            renderInterference(graphics, width, height);
        }
        renderCrtStatic(graphics, width, height);
    }

    private static void renderNoSignal(GuiGraphics graphics,
            int width, int height) {
        if (width <= 0 || height <= 0) return;
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(NO_SIGNAL, 0, 0, width, height,
                0.0F, 0.0F, CRT_TEXTURE_WIDTH, CRT_TEXTURE_HEIGHT,
                CRT_TEXTURE_WIDTH, CRT_TEXTURE_HEIGHT);
        RenderSystem.disableBlend();
    }

    private static void renderCrtStatic(GuiGraphics graphics,
            int width, int height) {
        if (width <= 0 || height <= 0
                || (!Scp079PlayableClient.active() && !signalEffectActive())) return;
        long now = System.nanoTime();
        float transition = transitionEnvelope();

        // At rest the authored alpha-static remains intentionally faint. The
        // two slow oscillations avoid a mechanical opacity pulse while keeping
        // the effect in the requested ~20-30% range. During feed hand-offs or
        // signal loss it becomes much more visible, but never fully opaque.
        double slow = Math.sin(now / 740_000_000.0D);
        double drift = Math.sin(now / 1_930_000_000.0D + 1.27D);
        float idleAlpha = Mth.clamp((float) (0.25D + slow * 0.032D
                + drift * 0.018D), 0.20F, 0.30F);
        float transitionAlpha = signalEffectActive()
                ? Mth.clamp(0.82F + (float) slow * 0.055F, 0.74F, 0.88F)
                : Mth.clamp(0.70F + (float) slow * 0.06F, 0.62F, 0.78F);
        float alpha = Mth.lerp(transition, idleAlpha, transitionAlpha);

        long frameDuration = transition > 0.05F
                ? Math.max(38_000_000L, CRT_FRAME_NANOS / 2L)
                : CRT_FRAME_NANOS;
        int frame = (int) Math.floorMod(now / frameDuration, CRT_FRAME_COUNT);

        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(CRT_STATIC[frame], 0, 0, width, height,
                0.0F, 0.0F, CRT_TEXTURE_WIDTH, CRT_TEXTURE_HEIGHT,
                CRT_TEXTURE_WIDTH, CRT_TEXTURE_HEIGHT);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void renderSignalNoise(GuiGraphics graphics,
            int width, int height) {
        if (width <= 0 || height <= 0) return;
        long frame = System.nanoTime() / 8_000_000L;
        RenderSystem.enableBlend();
        for (int i = 0; i < 26; i++) {
            int hash = mix((int) (frame * 811L + i * 4093L));
            int y = Math.floorMod(hash, height);
            int strip = 1 + Math.floorMod(hash >>> 7, 5);
            int x = Math.floorMod(hash >>> 12, Math.max(1, width));
            int run = Math.max(12, Math.floorMod(hash >>> 18,
                    Math.max(13, width / 2)));
            int left = Math.max(0, x - run / 4);
            int right = Math.min(width, left + run);
            int color = switch (i % 4) {
                case 0 -> 0xB8E7F7FF;
                case 1 -> 0xA276AFC4;
                case 2 -> 0x96172D38;
                default -> 0xA89BC5D3;
            };
            graphics.fill(left, y, right, Math.min(height, y + strip), color);
        }
        for (int i = 0; i < 6; i++) {
            int y = Math.floorMod((int) (frame * (19L + i * 5L) + i * 83L),
                    height);
            graphics.fill(0, y, width, Math.min(height, y + 2),
                    i % 2 == 0 ? 0xA8D4EEF7 : 0x9A0A1D27);
        }
        RenderSystem.disableBlend();
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

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private enum DisplayMode { INACTIVE, LOCAL, BOOT, MAP, CAMERA }
}
