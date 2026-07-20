package net.mcreator.scpadditions.facility;

import net.neoforged.fml.common.EventBusSubscriber;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client-only render-layer registration for migrated facility blocks. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FacilityClientRenderEvents {
    private FacilityClientRenderEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockRenderLayerMap.INSTANCE.putBlock(
                    FacilityModule.TRASHBIN.get(), RenderType.translucent());

            BlockRenderLayerMap.INSTANCE.putBlock(
                    MirroredDoorButtons.BUTTON_LOCKED.get(), RenderType.cutout());
            BlockRenderLayerMap.INSTANCE.putBlock(
                    MirroredDoorButtons.BUTTON_CLOSED.get(), RenderType.cutout());
            BlockRenderLayerMap.INSTANCE.putBlock(
                    MirroredDoorButtons.BUTTON_OPENING.get(), RenderType.cutout());
            BlockRenderLayerMap.INSTANCE.putBlock(
                    MirroredDoorButtons.BUTTON_OPEN.get(), RenderType.cutout());
            BlockRenderLayerMap.INSTANCE.putBlock(
                    MirroredDoorButtons.BUTTON_CLOSING.get(), RenderType.cutout());
        });
    }
}
