package com.bl4ues.scpclassifieddirective.scp012;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Client-only render-layer registration for every SCP-012 animation stage. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp012ClientRenderEvents {
    private Scp012ClientRenderEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.CLOSED.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.OPENING_1.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.OPENING_2.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.OPENING_3.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.OPENING_4.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.OPEN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.CLOSING_4.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.CLOSING_3.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.CLOSING_2.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    Scp012Module.CLOSING_1.get(), RenderType.cutout());
        });
    }
}
