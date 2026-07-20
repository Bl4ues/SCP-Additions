package net.mcreator.scpadditions.init;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.Scp131ARenderer;
import net.mcreator.scpadditions.client.Scp131BRenderer;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScpAdditionsModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ScpAdditionsModEntities.SCP_131_A.get(), Scp131ARenderer::new);
		event.registerEntityRenderer(ScpAdditionsModEntities.SCP_131_B.get(), Scp131BRenderer::new);
	}
}
