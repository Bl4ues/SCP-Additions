package com.bl4ues.scpclassifieddirective.vitals.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.AdvancementToastHudCoordination;
import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import com.bl4ues.scpclassifieddirective.client.Scp939ClientState;

/** Client MOD-bus registration for the custom gameplay overlays. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientVitalsModEvents {
    private ClientVitalsModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("player_vitals_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        PlayerVitalsOverlay.render(graphics,
                                                virtualWidth, virtualHeight,
                                                partialTick));
                    }
                });
        event.registerAboveAll("scp_079_energy_debug",
                (gui, graphics, partialTick, width, height) -> {
                    if (Scp939ClientState.pinned()) return;
                    ResponsiveUiScale.renderHud(graphics, width, height,
                            (virtualWidth, virtualHeight) ->
                                    renderBelowAchievementToast(graphics,
                                            () -> Scp079EnergyOverlay.render(
                                                    graphics, virtualWidth,
                                                    virtualHeight, partialTick)));
                });
        event.registerAboveAll("scp_spawn_timers_debug",
                (gui, graphics, partialTick, width, height) -> {
                    if (Scp939ClientState.pinned()) return;
                    ResponsiveUiScale.renderHud(graphics, width, height,
                            (virtualWidth, virtualHeight) ->
                                    renderBelowAchievementToast(graphics,
                                            () -> ScpSpawnTimersOverlay.render(
                                                    graphics, virtualWidth,
                                                    virtualHeight, partialTick)));
                });
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
