package net.mcreator.scpadditions.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
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
