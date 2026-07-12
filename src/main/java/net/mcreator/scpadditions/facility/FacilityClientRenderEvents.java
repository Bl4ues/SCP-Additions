package net.mcreator.scpadditions.facility;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client-only render-layer registration for migrated facility blocks. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FacilityClientRenderEvents {
    private FacilityClientRenderEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(
                    FacilityModule.TRASHBIN.get(), RenderType.translucent());

            ItemBlockRenderTypes.setRenderLayer(
                    MirroredDoorButtons.BUTTON_LOCKED.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    MirroredDoorButtons.BUTTON_CLOSED.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    MirroredDoorButtons.BUTTON_OPENING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    MirroredDoorButtons.BUTTON_OPEN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    MirroredDoorButtons.BUTTON_CLOSING.get(), RenderType.cutout());
        });
    }
}
