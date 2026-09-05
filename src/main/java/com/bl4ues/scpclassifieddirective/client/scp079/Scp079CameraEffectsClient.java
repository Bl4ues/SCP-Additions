package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Low-light and feed-switch effects composited before the final curved CRT pass. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CameraEffectsClient {
    private static final long INTERFERENCE_NANOS = 280_000_000L;
    private static final long AUDIO_FADE_IN_NANOS = 110_000_000L;
    private static final long AUDIO_FADE_OUT_NANOS = 150_000_000L;
    private static Vec3 lastFeedPosition;
    private static long interferenceStartedAt;
    private static long interferenceUntil;

    private Scp079CameraEffectsClient() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Scp079PlayableClient.cameraMode()) {
            lastFeedPosition = null;
            interferenceStartedAt = 0L;
            interferenceUntil = 0L;
            return;
        }
        Vec3 current = Scp079PlayableClient.viewPosition();
        if (lastFeedPosition == null
                || current.distanceToSqr(lastFeedPosition) > 0.25D) {
            long now = System.nanoTime();
            interferenceStartedAt = now;
            interferenceUntil = now + INTERFERENCE_NANOS;
            lastFeedPosition = current;
        }
    }

    /**
     * Shared hand-off envelope for effects tied to the authored feed transition.
     * Keeping this here prevents the visual static and transition audio from
     * drifting onto independent timers when cameras are switched quickly.
     */
    public static float transitionEnvelope() {
        if (!Scp079PlayableClient.cameraMode() || interferenceStartedAt <= 0L) {
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

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!Scp079PlayableClient.cameraMode()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        BlockPos pos = BlockPos.containing(Scp079PlayableClient.viewPosition());
        if (minecraft.level.getMaxLocalRawBrightness(pos) < 5) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE);
            event.getGuiGraphics().fill(0, 0, width, height, 0x182B7187);
            RenderSystem.defaultBlendFunc();
            Scp079UiTheme.draw(event.getGuiGraphics(), minecraft.font,
                    "LOW-LIGHT ENHANCEMENT", 36, height - 62,
                    1.04F, 0xFF79DDF3);
        }

        long now = System.nanoTime();
        if (now < interferenceUntil) {
            long frame = now / 7_000_000L;
            for (int i = 0; i < 11; i++) {
                int y = Math.floorMod((int) (frame * 19L + i * 47L),
                        Math.max(1, height));
                int strip = 1 + Math.floorMod((int) (frame + i * 5L), 5);
                int offset = Math.floorMod(
                        (int) (frame * 11L + i * 31L), 73) - 36;
                int color = i % 3 == 0 ? 0x9AC6F4FF
                        : i % 3 == 1 ? 0x72182C38 : 0x584F96AC;
                event.getGuiGraphics().fill(Math.max(0, offset), y,
                        Math.min(width, width + offset),
                        Math.min(height, y + strip), color);
            }
        }
    }
}
