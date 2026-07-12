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
        event.enqueueWork(() -> UBlocksModule.cutoutBlocks().forEach(block ->
                ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout())));
    }
}
