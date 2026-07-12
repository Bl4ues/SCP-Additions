package net.mcreator.scpadditions.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.Scp131ARenderer;
import net.mcreator.scpadditions.client.Scp131BRenderer;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScpAdditionsModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ScpAdditionsModEntities.SCP_131_A.get(), Scp131ARenderer::new);
		event.registerEntityRenderer(ScpAdditionsModEntities.SCP_131_B.get(), Scp131BRenderer::new);
	}
}
