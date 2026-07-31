package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
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
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Replaces Minecraft's crosshair with the configurable SCP: Unity-style texture. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CustomCrosshairOverlay {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/gui/crosshair.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int RENDER_SIZE = 14;

    private CustomCrosshairOverlay() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())
                || !InventoryModuleRuntimeState.customCrosshairEnabledForClient()) {
            return;
        }

        event.setCanceled(true);
        if (!InventoryModuleRuntimeState.inGameCrosshairEnabledForClient()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        float sequenceOpacity = ElevatorArrivalOverlay.crosshairOpacity();
        float alpha = InventoryModuleRuntimeState.crosshairAlphaForClient()
                * sequenceOpacity;
        if (alpha <= 0.001F) return;

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int x = (width - RENDER_SIZE) / 2;
        int y = (height - RENDER_SIZE) / 2;
        GuiGraphics graphics = event.getGuiGraphics();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(
                InventoryModuleRuntimeState.crosshairRedForClient(),
                InventoryModuleRuntimeState.crosshairGreenForClient(),
                InventoryModuleRuntimeState.crosshairBlueForClient(), alpha);
        graphics.blit(TEXTURE, x, y, RENDER_SIZE, RENDER_SIZE,
                0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
