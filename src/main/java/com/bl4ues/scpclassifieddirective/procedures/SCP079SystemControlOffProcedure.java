package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.facility.Scp079ProcessingManager;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;

public class SCP079SystemControlOffProcedure {
	public static void execute(LevelAccessor world) {
		world.getLevelData().getGameRules().getRule(ScpClassifiedDirectiveModGameRules.SCP079CONTROLON).set(false, world.getServer());
		Scp079ProcessingManager.onControlDisabled(world);
	}
}
