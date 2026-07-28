package net.mcreator.scpadditions.facility;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.FacilitySignBlockEntityRenderer;
import net.mcreator.scpadditions.client.ScpSignSupportBlockEntityRenderer;
import net.mcreator.scpadditions.client.WetFloorBlockEntityRenderer;

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
                    FacilityModule.TRASHBIN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    FacilityModule.EMERGENCY_BUTTON.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    FacilityModule.CORE_ROOM_SIGN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    FacilityModule.DOOR_SIGN.get(), RenderType.cutout());

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

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                FacilityModule.FACILITY_SIGN_BLOCK_ENTITY.get(),
                FacilitySignBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                FacilityModule.WET_FLOOR_BLOCK_ENTITY.get(),
                WetFloorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                FacilityModule.SCP_SIGN_BLOCK_ENTITY.get(),
                ScpSignSupportBlockEntityRenderer::new);
    }
}
