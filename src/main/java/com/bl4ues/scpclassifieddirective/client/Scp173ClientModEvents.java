package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModParticleTypes;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079Keybinds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp173ClientModEvents {
    private Scp173ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("player_view_effects_overlay",
                (gui, graphics, partialTick, width, height) ->
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) -> {
                                    BlinkClient.renderVignette(graphics,
                                            virtualWidth, virtualHeight);
                                    HazmatVisorOverlay.render(graphics,
                                            virtualWidth, virtualHeight);
                                    Scp714VignetteOverlay.render(graphics,
                                            virtualWidth, virtualHeight,
                                            partialTick);
                                    Scp012SubliminalOverlay.render(graphics,
                                            virtualWidth, virtualHeight,
                                            partialTick);
                                }));
        event.registerAboveAll("scp_1176_honey_vignette_overlay",
                (gui, graphics, partialTick, width, height) ->
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        Scp1176HoneyVignette.render(graphics,
                                                virtualWidth, virtualHeight,
                                                partialTick)));
        event.registerAboveAll("blink_blackout_overlay",
                (gui, graphics, partialTick, width, height) ->
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        BlinkClient.renderBlackout(graphics,
                                                virtualWidth, virtualHeight)));
        event.registerAboveAll("equipment_progress_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        EquipmentProgressOverlay.render(graphics,
                                                virtualWidth, virtualHeight,
                                                partialTick));
                    }
                });
        event.registerAboveAll("blink_meter_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        BlinkClient.renderHud(graphics,
                                                virtualWidth, virtualHeight,
                                                partialTick));
                    }
                });
        event.registerAboveAll("scp_131_notice_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        Scp131NoticeOverlay.render(graphics));
                    }
                });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Scp173Keybinds.BLINK);
        event.register(Scp131Keybinds.DISMISS);
        event.register(Scp079Keybinds.USE_SPEAKER);
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
        event.registerSpriteSet(
                ScpClassifiedDirectiveModParticleTypes.DAMAGE_SPLATTER.get(),
                DamageSplatterParticle.Provider::new);
        event.registerSpriteSet(
                ScpClassifiedDirectiveModParticleTypes.TESLA_ARC.get(),
                TeslaArcParticle.Provider::new);
    }
}
