package net.mcreator.scpadditions.world;

import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import net.minecraft.world.GameRules;

import net.mcreator.scpadditions.ScpAdditionsModElements;

import java.lang.reflect.Method;

@ScpAdditionsModElements.ModElement.Tag
public class Scp079controlOnGameRule extends ScpAdditionsModElements.ModElement {
	public static final GameRules.RuleKey<GameRules.BooleanValue> gamerule = GameRules.register("scp079controlOn", GameRules.Category.PLAYER,
			create(false));

	public Scp079controlOnGameRule(ScpAdditionsModElements instance) {
		super(instance, 86);
	}

	public static GameRules.RuleType<GameRules.BooleanValue> create(boolean defaultValue) {
		try {
			Method createGameruleMethod = ObfuscationReflectionHelper.findMethod(GameRules.BooleanValue.class, "func_223568_b", boolean.class);
			createGameruleMethod.setAccessible(true);
			return (GameRules.RuleType<GameRules.BooleanValue>) createGameruleMethod.invoke(null, defaultValue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
