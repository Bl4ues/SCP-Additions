package com.bl4ues.scpclassifieddirective.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.PlayerCorpseRenderer;
import com.bl4ues.scpclassifieddirective.client.Scp106Renderer;
import com.bl4ues.scpclassifieddirective.client.Scp131ARenderer;
import com.bl4ues.scpclassifieddirective.client.Scp131BRenderer;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScpClassifiedDirectiveModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ScpClassifiedDirectiveModEntities.SCP_106.get(), Scp106Renderer::new);
		event.registerEntityRenderer(ScpClassifiedDirectiveModEntities.SCP_131_A.get(), Scp131ARenderer::new);
		event.registerEntityRenderer(ScpClassifiedDirectiveModEntities.SCP_131_B.get(), Scp131BRenderer::new);
		event.registerEntityRenderer(ScpClassifiedDirectiveModEntities.PLAYER_CORPSE.get(), PlayerCorpseRenderer::new);
	}
}
