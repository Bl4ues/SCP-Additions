package net.mcreator.scpadditions.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModEntities;
import net.mcreator.scpadditions.init.ScpAdditionsModParticleTypes;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp173ClientModEvents {
    private Scp173ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("blink_vignette_overlay",
                (gui, graphics, partialTick, width, height) -> BlinkClient.renderVignette(graphics, width, height));
        event.registerAboveAll("blink_blackout_overlay",
                (gui, graphics, partialTick, width, height) -> BlinkClient.renderBlackout(graphics, width, height));
        event.registerAboveAll("blink_meter_overlay",
                (gui, graphics, partialTick, width, height) -> BlinkClient.renderHud(graphics, width, height, partialTick));
        event.registerAboveAll("scp_131_notice_overlay",
                (gui, graphics, partialTick, width, height) -> Scp131NoticeOverlay.render(graphics));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Scp173Keybinds.BLINK);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ScpAdditionsModEntities.SCP_173.get(), Scp173Renderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ScpAdditionsModParticleTypes.DECONTAMINATION_GAS.get(),
                DecontaminationGasParticle.Provider::new);
    }
}
