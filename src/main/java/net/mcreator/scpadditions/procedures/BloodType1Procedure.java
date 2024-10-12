package net.mcreator.scpadditions.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraft.world.entity.Entity;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class BloodType1Procedure {
	@SubscribeEvent
	public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (Math.random() < 0.2) {
			{
				boolean _setval = true;
				entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.ABpos = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		} else {
			if (Math.random() < 0.2) {
				{
					boolean _setval = true;
					entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Bneg = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else {
				if (Math.random() < 0.25) {
					{
						boolean _setval = true;
						entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.ABpos = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				} else {
					if (Math.random() < 0.3) {
						{
							boolean _setval = true;
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Aneg = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else {
						if (Math.random() < 0.4) {
							{
								boolean _setval = true;
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.Bpos = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
						} else {
							if (Math.random() < 0.4) {
								{
									boolean _setval = true;
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.Oneg = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
							} else {
								if (Math.random() < 0.4) {
									{
										boolean _setval = true;
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.Apos = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
								} else {
									{
										boolean _setval = true;
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
