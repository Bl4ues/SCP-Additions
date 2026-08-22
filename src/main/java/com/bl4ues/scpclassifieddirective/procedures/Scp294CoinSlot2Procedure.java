package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;

public class Scp294CoinSlot2Procedure {
	public static void execute(LevelAccessor world) {
		ScpClassifiedDirectiveModVariables.WorldVariables.get(world).coinslot = 0;
		ScpClassifiedDirectiveModVariables.WorldVariables.get(world).syncData(world);
	}
}
