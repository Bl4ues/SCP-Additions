package net.mcreator.scpadditions.init;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;

public final class ScpAdditionsModGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> TESLAGATEON =
            GameRuleRegistry.register("teslaGateOn", GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanValue> TESLAGATEMANUALOVERRIDE =
            GameRuleRegistry.register("teslaGateManualOverride", GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(false));
    public static final GameRules.Key<GameRules.BooleanValue> SCP079CONTROLON =
            GameRuleRegistry.register("scp079controlOn", GameRules.Category.PLAYER,
                    GameRuleFactory.createBooleanRule(false));
    public static final GameRules.Key<GameRules.BooleanValue> DECONCHECKPOINT =
            GameRuleRegistry.register("deconCheckpoint", GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(false));

    private ScpAdditionsModGameRules() {}

    public static void bootstrap() {}
}
