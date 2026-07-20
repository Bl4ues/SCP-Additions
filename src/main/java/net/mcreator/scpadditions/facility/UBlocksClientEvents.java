package net.mcreator.scpadditions.facility;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UBlocksClientEvents {
    // Approximates the original low-alpha white paint after it is blended over
    // the dark blue SL1 floor, while remaining stable in vanilla's cutout pass.
    private static final int FLOOR_DECAL_TINT = 0x6E7486;

    private UBlocksClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> UBlocksModule.cutoutBlocks().forEach(block ->
                ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout())));
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) ->
                        tintIndex == 0 ? FLOOR_DECAL_TINT : 0xFFFFFF,
                UBlocksModule.SL_1_FLOOR_DETAIL_SMALL.get(),
                UBlocksModule.SL_1_FLOOR_DETAIL_BIG.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) ->
                        tintIndex == 0 ? FLOOR_DECAL_TINT : 0xFFFFFF,
                UBlocksModule.SL_1_FLOOR_DETAIL_SMALL.get().asItem(),
                UBlocksModule.SL_1_FLOOR_DETAIL_BIG.get().asItem());
    }
}
