package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
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
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerBelowAll(id("player_view_effects_overlay"),
                (graphics, deltaTracker) -> {
                    int width = graphics.guiWidth();
                    int height = graphics.guiHeight();
                    float partialTick = deltaTracker
                            .getGameTimeDeltaPartialTick(false);
                    HazmatVisorOverlay.render(graphics, width, height);
                    Scp714VignetteOverlay.render(graphics, width, height,
                            partialTick);
                    Scp012SubliminalOverlay.render(graphics, width, height,
                            partialTick);
                });
        event.registerAboveAll(id("blink_vignette_overlay"),
                (graphics, deltaTracker) -> {
                    int width = graphics.guiWidth();
                    int height = graphics.guiHeight();
                    float partialTick = deltaTracker
                            .getGameTimeDeltaPartialTick(false);
                    BlinkClient.renderVignette(graphics, width, height);
                    Scp1176HoneyVignette.render(graphics, width, height,
                            partialTick);
                });
        event.registerAboveAll(id("blink_blackout_overlay"),
                (graphics, deltaTracker) -> BlinkClient.renderBlackout(
                        graphics, graphics.guiWidth(), graphics.guiHeight()));
        event.registerAboveAll(id("equipment_progress_overlay"),
                (graphics, deltaTracker) -> EquipmentProgressOverlay.render(
                        graphics, graphics.guiWidth(), graphics.guiHeight(),
                        deltaTracker.getGameTimeDeltaPartialTick(false)));
        event.registerAboveAll(id("blink_meter_overlay"),
                (graphics, deltaTracker) -> BlinkClient.renderHud(
                        graphics, graphics.guiWidth(), graphics.guiHeight(),
                        deltaTracker.getGameTimeDeltaPartialTick(false)));
        event.registerAboveAll(id("scp_131_notice_overlay"),
                (graphics, deltaTracker) ->
                        Scp131NoticeOverlay.render(graphics));
    }

    private static net.minecraft.resources.ResourceLocation id(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                ScpAdditionsMod.MODID, path);
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
