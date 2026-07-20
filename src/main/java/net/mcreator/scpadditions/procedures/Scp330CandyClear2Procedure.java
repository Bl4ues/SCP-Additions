package net.mcreator.scpadditions.procedures;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class Scp330CandyClear2Procedure {
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		net.mcreator.scpadditions.fabric.FabricPersistentData.get(entity).putBoolean("candy0", true);
		net.mcreator.scpadditions.fabric.FabricPersistentData.get(entity).putBoolean("candy1", false);
		net.mcreator.scpadditions.fabric.FabricPersistentData.get(entity).putBoolean("candy2", false);
	}
}
