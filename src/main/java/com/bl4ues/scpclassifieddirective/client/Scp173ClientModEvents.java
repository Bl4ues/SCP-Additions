package com.bl4ues.scpclassifieddirective.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModParticleTypes;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp173ClientModEvents {
    private Scp173ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Screen-space view effects are composed below every HUD layer. The
        // blink vignette is rendered first so worn-item overlays such as the
        // Hazmat visor remain above it; future equipped-item overlays should
        // follow the same ordering rule. SCP-714 fatigue and SCP-012 influence
        // preserve their existing order relative to the visor.
        event.registerBelowAll("player_view_effects_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    BlinkClient.renderVignette(graphics, width, height);
                    HazmatVisorOverlay.render(graphics, width, height);
                    Scp714VignetteOverlay.render(graphics, width, height,
                            partialTick);
                    Scp012SubliminalOverlay.render(graphics, width, height,
                            partialTick);
                });
        event.registerAboveAll("scp_1176_honey_vignette_overlay",
                (gui, graphics, partialTick, width, height) ->
                        Scp1176HoneyVignette.render(graphics, width, height,
                                partialTick));
        event.registerAboveAll("blink_blackout_overlay",
                (gui, graphics, partialTick, width, height) ->
                        BlinkClient.renderBlackout(graphics, width, height));
        event.registerAboveAll("equipment_progress_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        EquipmentProgressOverlay.render(graphics, width, height,
                                partialTick);
                    }
                });
        event.registerAboveAll("blink_meter_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        BlinkClient.renderHud(graphics, width, height,
                                partialTick);
                    }
                });
        event.registerAboveAll("scp_131_notice_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        Scp131NoticeOverlay.render(graphics);
                    }
                });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Scp173Keybinds.BLINK);
        event.register(Scp131Keybinds.DISMISS);
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ScpClassifiedDirectiveModEntities.SCP_173.get(),
                Scp173Renderer::new);
        event.registerEntityRenderer(ScpClassifiedDirectiveModEntities.ROOMBA.get(),
                RoombaRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(
            RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ScpClassifiedDirectiveModParticleTypes.DECONTAMINATION_GAS.get(),
                DecontaminationGasParticle.Provider::new);
        event.registerSpriteSet(
                ScpClassifiedDirectiveModParticleTypes.SCP_106_CORROSION.get(),
                Scp106CorrosionParticle.Provider::new);
        event.registerSpriteSet(
                ScpClassifiedDirectiveModParticleTypes.SCP_106_PORTAL.get(),
                Scp106PortalParticle.Provider::new);
    }
}
