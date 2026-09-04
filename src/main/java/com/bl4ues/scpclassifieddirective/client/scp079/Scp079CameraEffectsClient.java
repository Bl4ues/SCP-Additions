package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Low-light and feed-switch effects placed over the world but below 079's HUD. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CameraEffectsClient {
    private static final long INTERFERENCE_NANOS = 280_000_000L;
    private static Vec3 lastFeedPosition;
    private static long interferenceUntil;

    private Scp079CameraEffectsClient() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Scp079PlayableClient.cameraMode()) {
            lastFeedPosition = null;
            return;
        }
        Vec3 current = Scp079PlayableClient.viewPosition();
        if (lastFeedPosition == null
                || current.distanceToSqr(lastFeedPosition) > 0.25D) {
            interferenceUntil = System.nanoTime() + INTERFERENCE_NANOS;
            lastFeedPosition = current;
        }
    }

    // CRT framebuffer distortion runs at normal priority first. This LOWEST
    // pass then adds camera-signal effects before vanilla/custom overlays render,
    // keeping labels, recognition boxes and controls sharp like the SL reference.
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
            event.getGuiGraphics().drawString(minecraft.font,
                    ScpFonts.roboto("LOW-LIGHT ENHANCEMENT"), 34, height - 34,
                    0xFF79DDF3, false);
        }

        long now = System.nanoTime();
        if (now < interferenceUntil) {
            long frame = now / 7_000_000L;
            for (int i = 0; i < 11; i++) {
                int y = Math.floorMod((int) (frame * 19L + i * 47L),
                        Math.max(1, height));
                int strip = 1 + Math.floorMod((int) (frame + i * 5L), 5);
                int offset = Math.floorMod((int) (frame * 11L + i * 31L), 73) - 36;
                int color = i % 3 == 0 ? 0x9AC6F4FF
                        : i % 3 == 1 ? 0x72182C38 : 0x584F96AC;
                event.getGuiGraphics().fill(Math.max(0, offset), y,
                        Math.min(width, width + offset), Math.min(height, y + strip),
                        color);
            }
        }
    }
}
