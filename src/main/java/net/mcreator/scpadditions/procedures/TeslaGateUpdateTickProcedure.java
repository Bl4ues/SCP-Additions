package net.mcreator.scpadditions.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

public class TeslaGateUpdateTickProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        boolean manualOverride = world.getLevelData().getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean teslaGateOn = world.getLevelData().getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
        if (manualOverride && !teslaGateOn
                && world instanceof Level level && !level.isClientSide()) {
            world.getLevelData().getGameRules()
                    .getRule(ScpAdditionsModGameRules.TESLAGATEON)
                    .set(true, level.getServer());
            teslaGateOn = true;
        }
        if (!teslaGateOn && !manualOverride) {
            return;
        }

        int activationDelay = manualOverride ? 1 : 5;
        ResourceLocation activationSound = ResourceLocation.fromNamespaceAndPath(
                "scp_additions", manualOverride ? "overcharge" : "teslaactivate");
        float activationVolume = manualOverride ? 2.0F : 1.0F;

        AABB volume = TeslaGateVolume.at(x, y, z);
        if (world.getEntitiesOfClass(LivingEntity.class, volume,
                entity -> TeslaGateVolume.intersects(entity, volume)).isEmpty()) {
            return;
        }

        if (world instanceof Level level) {
            if (!level.isClientSide()) {
                level.playSound(null, BlockPos.containing(x, y, z),
                        BuiltInRegistries.SOUND_EVENT.get(activationSound),
                        SoundSource.HOSTILE, activationVolume,
                        manualOverride ? 1.25F : 1.0F);
            } else {
                level.playLocalSound(x, y, z,
                        BuiltInRegistries.SOUND_EVENT.get(activationSound),
                        SoundSource.HOSTILE, activationVolume,
                        manualOverride ? 1.25F : 1.0F, false);
            }
        }
        ScpAdditionsMod.queueServerWork(activationDelay,
                () -> TeslaGateTransitionHelper.transitionIfCurrent(
                        world, x, y, z,
                        ScpAdditionsModBlocks.TESLA_GATE,
                        ScpAdditionsModBlocks.TESLA_ACTIVE));
    }
}
