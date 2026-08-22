package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;

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
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).Scp914refining = false;
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).syncData(world);
	}
}
