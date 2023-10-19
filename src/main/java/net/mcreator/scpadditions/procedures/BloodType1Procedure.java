package net.mcreator.scpadditions.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraft.entity.Entity;

import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.HashMap;

public class BloodType1Procedure {
	@Mod.EventBusSubscriber
	private static class GlobalTrigger {
		@SubscribeEvent
		public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
			Entity entity = event.getPlayer();
			Map<String, Object> dependencies = new HashMap<>();
			dependencies.put("x", entity.getPosX());
			dependencies.put("y", entity.getPosY());
			dependencies.put("z", entity.getPosZ());
			dependencies.put("world", entity.world);
			dependencies.put("entity", entity);
			dependencies.put("endconquered", event.isEndConquered());
			dependencies.put("event", event);
			executeProcedure(dependencies);
		}
	}

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure BloodType1!");
			return;
		}
		Entity entity = (Entity) dependencies.get("entity");
		if (Math.random() < 0.2) {
			{
				boolean _setval = (true);
				entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.ABpos = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		} else {
			if (Math.random() < 0.2) {
				{
					boolean _setval = (true);
					entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Bneg = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else {
				if (Math.random() < 0.25) {
					{
						boolean _setval = (true);
						entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ABpos = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				} else {
					if (Math.random() < 0.3) {
						{
							boolean _setval = (true);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Aneg = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else {
						if (Math.random() < 0.4) {
							{
								boolean _setval = (true);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.Bpos = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
						} else {
							if (Math.random() < 0.4) {
								{
									boolean _setval = (true);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.Oneg = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
							} else {
								if (Math.random() < 0.4) {
									{
										boolean _setval = (true);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.Apos = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
								} else {
									{
										boolean _setval = (true);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.Opos = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
