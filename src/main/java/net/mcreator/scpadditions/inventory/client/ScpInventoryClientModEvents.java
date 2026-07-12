package net.mcreator.scpadditions.inventory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.inventory.ClientScpInventoryFullOverlay;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ScpInventoryClientModEvents {
    private ScpInventoryClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("scp_world_prompts",
                (gui, graphics, partialTick, width, height) ->
                        ScpWorldPromptClient.render(graphics, width, height,
                                partialTick));
        event.registerAboveAll("scp_inventory_full",
                (gui, graphics, partialTick, width, height) ->
                        ClientScpInventoryFullOverlay.render(graphics));
    }
}
