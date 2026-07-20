package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModEntities;
import net.mcreator.scpadditions.init.ScpAdditionsModParticleTypes;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp173ClientModEvents {
    private Scp173ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // View effects are composed in a fixed order below every HUD layer. The
        // SCP-714 fatigue closes over the world and any worn visor, while health,
        // hotbar, crosshair, warnings and progress bars remain readable.
        event.registerBelowAll("player_view_effects_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    HazmatVisorOverlay.render(graphics, width, height);
                    Scp714VignetteOverlay.render(graphics, width, height,
                            partialTick);
                    Scp012SubliminalOverlay.render(graphics, width, height,
                            partialTick);
                });
        event.registerAboveAll("blink_vignette_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    BlinkClient.renderVignette(graphics, width, height);
                    Scp1176HoneyVignette.render(graphics, width, height,
                            partialTick);
                });
        event.registerAboveAll("blink_blackout_overlay",
                (gui, graphics, partialTick, width, height) ->
                        BlinkClient.renderBlackout(graphics, width, height));
        event.registerAboveAll("equipment_progress_overlay",
                (gui, graphics, partialTick, width, height) ->
                        EquipmentProgressOverlay.render(graphics, width, height,
                                partialTick));
        event.registerAboveAll("blink_meter_overlay",
                (gui, graphics, partialTick, width, height) ->
                        BlinkClient.renderHud(graphics, width, height,
                                partialTick));
        event.registerAboveAll("scp_131_notice_overlay",
                (gui, graphics, partialTick, width, height) ->
                        Scp131NoticeOverlay.render(graphics));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Scp173Keybinds.BLINK);
        event.register(Scp131Keybinds.DISMISS);
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ScpAdditionsModEntities.SCP_173.get(),
                Scp173Renderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(
            RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                ScpAdditionsModParticleTypes.DECONTAMINATION_GAS.get(),
                DecontaminationGasParticle.Provider::new);
    }
}
