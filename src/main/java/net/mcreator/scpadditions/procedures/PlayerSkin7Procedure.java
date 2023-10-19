package net.mcreator.scpadditions.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;

import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.HashMap;

public class PlayerSkin7Procedure {
	@Mod.EventBusSubscriber
	private static class GlobalTrigger {
		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void KleidersRenderEvent(RenderLivingEvent event) {
			Entity entity = event.getEntity();
			World world = entity.world;
			double i = entity.getPosX();
			double j = entity.getPosY();
			double k = entity.getPosZ();
			Map<String, Object> dependencies = new HashMap<>();
			dependencies.put("x", i);
			dependencies.put("y", j);
			dependencies.put("z", k);
			dependencies.put("world", world);
			dependencies.put("entity", entity);
			dependencies.put("event", event);
			executeProcedure(dependencies);
		}
	}

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure PlayerSkin7!");
			return;
		}
		Entity entity = (Entity) dependencies.get("entity");

		// Enter the FTL code here
		Object _obj = dependencies.get("event");
		RenderLivingEvent _evt = (RenderLivingEvent) _obj;
		if ((entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new ScpAdditionsModVariables.PlayerVariables())).PlayerOn1to1_7) {
			if (_evt.getRenderer() instanceof PlayerRenderer) {
				if (_evt instanceof RenderLivingEvent.Pre) {
					//  _evt.setCanceled(true); 
				}
				new com.kleiders.kleidersplayerrenderer.ClassicPlayerRenderer(_evt.getRenderer().getRenderManager(),
						new ResourceLocation("scp_additions:textures/entities/skin7.png")).render((AbstractClientPlayerEntity) _evt.getEntity(),
								_evt.getEntity().rotationYaw, _evt.getPartialRenderTick(), _evt.getMatrixStack(), _evt.getBuffers(), _evt.getLight());
			}
		}
	}
}
