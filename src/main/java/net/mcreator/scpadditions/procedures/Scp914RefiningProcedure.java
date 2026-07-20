package net.mcreator.scpadditions.procedures;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class Scp914RefiningProcedure {
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		execute(event, event.getEntity().level());
	}

	public static void execute(LevelAccessor world) {
		execute(null, world);
	}

	private static void execute(@Nullable Event event, LevelAccessor world) {
		ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
	}
}
