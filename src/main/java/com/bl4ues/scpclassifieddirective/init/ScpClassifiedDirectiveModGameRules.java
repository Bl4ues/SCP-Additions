
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.bl4ues.scpclassifieddirective.init;

import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.GameRules;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScpClassifiedDirectiveModGameRules {
	public static final GameRules.Key<GameRules.BooleanValue> TESLAGATEON = GameRules.register("teslaGateOn", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> TESLAGATEMANUALOVERRIDE = GameRules.register("teslaGateManualOverride", GameRules.Category.MISC, GameRules.BooleanValue.create(false));
	public static final GameRules.Key<GameRules.BooleanValue> SCP079CONTROLON = GameRules.register("scp079controlOn", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
	public static final GameRules.Key<GameRules.BooleanValue> DECONCHECKPOINT = GameRules.register("deconCheckpoint", GameRules.Category.MISC, GameRules.BooleanValue.create(false));
	public static final GameRules.Key<GameRules.BooleanValue> SCP_173_SPAWN = GameRules.register("173spawn", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SCP_106_SPAWN = GameRules.register("106spawn", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SCP_939_SPAWN = GameRules.register("939spawn", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> ROOMBA_SPAWN = GameRules.register("roombaSpawn", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
}
