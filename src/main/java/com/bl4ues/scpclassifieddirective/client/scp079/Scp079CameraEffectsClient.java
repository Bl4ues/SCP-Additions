package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
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
            startTransition();
            if (mode == DisplayMode.MAP
                    || mode == DisplayMode.LOCAL && lastMode == DisplayMode.CAMERA) {
                Scp079PlayableAudioClient.playDisplaySwitch();
            }
            lastMode = mode;
        }

        if (mode == DisplayMode.CAMERA) {
            Vec3 current = Scp079PlayableClient.viewPosition();
            if (lastFeedPosition == null
                    || current.distanceToSqr(lastFeedPosition) > 0.25D) {
                startTransition();
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

    private static void startTransition() {
        long now = System.nanoTime();
        interferenceStartedAt = now;
        interferenceUntil = now + INTERFERENCE_NANOS;
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!Scp079PlayableClient.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        if (Scp079PlayableClient.cameraMode()) {
            BlockPos pos = BlockPos.containing(Scp079PlayableClient.viewPosition());
            if (minecraft.level.getMaxLocalRawBrightness(pos) < 5) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE);
                event.getGuiGraphics().fill(0, 0, width, height, 0x182B7187);
                RenderSystem.defaultBlendFunc();
                Scp079UiTheme.draw(event.getGuiGraphics(), minecraft.font,
                        "LOW-LIGHT ENHANCEMENT", 24, height - 50,
                        1.08F, 0xFF79DDF3);
            }
        }
        renderInterference(event.getGuiGraphics(), width, height);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
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
        if (now >= interferenceUntil) return;
        long frame = now / 7_000_000L;
        for (int i = 0; i < 14; i++) {
            int y = Math.floorMod((int) (frame * 19L + i * 47L),
                    Math.max(1, height));
            int strip = 1 + Math.floorMod((int) (frame + i * 5L), 5);
            int offset = Math.floorMod(
                    (int) (frame * 11L + i * 31L), 91) - 45;
            int color = i % 3 == 0 ? 0xA4C6F4FF
                    : i % 3 == 1 ? 0x78182C38 : 0x624F96AC;
            graphics.fill(Math.max(0, offset), y,
                    Math.min(width, width + offset),
                    Math.min(height, y + strip), color);
        }
    }

    private static DisplayMode mode(Minecraft minecraft) {
        if (!Scp079PlayableClient.active()) return DisplayMode.INACTIVE;
        if (minecraft.screen instanceof Scp079FacilityMapScreen
                || minecraft.screen instanceof Scp079LeaveRoleScreen) {
            return DisplayMode.MAP;
        }
        return Scp079PlayableClient.cameraMode()
                ? DisplayMode.CAMERA : DisplayMode.LOCAL;
    }

    private enum DisplayMode { INACTIVE, LOCAL, MAP, CAMERA }
}
