package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.FacilitySignBlockEntityRenderer;
import com.bl4ues.scpclassifieddirective.client.Scp914UsageNoticeBlockEntityRenderer;
import com.bl4ues.scpclassifieddirective.client.ScpSignSupportBlockEntityRenderer;
import com.bl4ues.scpclassifieddirective.client.WetFloorBlockEntityRenderer;
import com.bl4ues.scpclassifieddirective.client.SystemTerminalBlockEntityRenderer;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

/** Client-only render-layer registration for migrated facility blocks. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
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
                    FacilityModule.ARCHIVISTS_TABLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(
                    FacilityModule.SCP_914_USAGE_NOTICE.get(),
                    RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    ScpClassifiedDirectiveModBlocks.SCP_079_AUXILIARY_POWER.get(),
                    RenderType.cutout());

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
        event.registerBlockEntityRenderer(
                FacilityModule.SCP_914_NOTICE_BLOCK_ENTITY.get(),
                Scp914UsageNoticeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(
                ScpClassifiedDirectiveModBlockEntities.SCP_079_SYSTEM_CONTROL.get(),
                SystemTerminalBlockEntityRenderer::new);
    }
}
