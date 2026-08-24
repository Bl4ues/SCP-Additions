package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the elevator-sequence fallback crosshair independent of GUI scale. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class ResponsiveElevatorCrosshairOverlay {
    private static final ResourceLocation CROSSHAIR_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final int SIZE = 15;

    private ResponsiveElevatorCrosshairOverlay() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void render(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())
                || Scp939ClientState.pinned()
                || InventoryModuleRuntimeState.customCrosshairEnabledForClient()) {
            return;
        }
        float opacity = ElevatorArrivalOverlay.crosshairOpacity();
        if (opacity >= 0.999F) return;

        event.setCanceled(true);
        if (opacity <= 0.001F) return;

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        ResponsiveUiScale.renderHud(graphics,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                (width, height) -> {
                    int x = (width - SIZE) / 2;
                    int y = (height - SIZE) / 2;
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
                    graphics.blit(CROSSHAIR_TEXTURE, x, y, 0, 0, SIZE, SIZE);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                });
    }
}
