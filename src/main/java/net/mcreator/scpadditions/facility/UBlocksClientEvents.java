package net.mcreator.scpadditions.facility;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UBlocksClientEvents {
    private UBlocksClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            UBlocksModule.cutoutBlocks().forEach(block ->
                    ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout()));

            // The floor arrows use partially transparent pixels. They were
            // previously forced through cutout with the other decorations,
            // which discarded most of their texture and made them disappear.
            ItemBlockRenderTypes.setRenderLayer(
                    UBlocksModule.SL_1_FLOOR_DETAIL_SMALL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    UBlocksModule.SL_1_FLOOR_DETAIL_BIG.get(), RenderType.translucent());
        });
    }
}
