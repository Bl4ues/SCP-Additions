package net.mcreator.scpadditions.vitals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.AdvancementToastHudCoordination;

/** Client MOD-bus registration for the custom gameplay overlays. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientVitalsModEvents {
    private ClientVitalsModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("player_vitals_overlay",
                (gui, graphics, partialTick, width, height) ->
                        PlayerVitalsOverlay.render(graphics, width, height, partialTick));
        event.registerAboveAll("scp_079_energy_debug",
                (gui, graphics, partialTick, width, height) ->
                        renderBelowAchievementToast(graphics,
                                () -> Scp079EnergyOverlay.render(graphics,
                                        width, height, partialTick)));
        event.registerAboveAll("scp_spawn_timers_debug",
                (gui, graphics, partialTick, width, height) ->
                        renderBelowAchievementToast(graphics,
                                () -> ScpSpawnTimersOverlay.render(graphics,
                                        width, height, partialTick)));
    }

    private static void renderBelowAchievementToast(GuiGraphics graphics,
            Runnable renderer) {
        int clearance = AdvancementToastHudCoordination.topRightClearance();
        if (clearance <= 0) {
            renderer.run();
            return;
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0.0F, clearance, 0.0F);
            renderer.run();
        } finally {
            graphics.pose().popPose();
        }
    }
}
