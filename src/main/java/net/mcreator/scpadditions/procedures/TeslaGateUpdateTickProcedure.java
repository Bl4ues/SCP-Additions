package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.FacilityStructureBreakGuard;
import net.mcreator.scpadditions.facility.Scp079TeslaSuppression;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.List;

public class TeslaGateUpdateTickProcedure {
    public static void execute(LevelAccessor world, double x, double y,
            double z) {
        BlockPos gatePos = BlockPos.containing(x, y, z);
        if (FacilityStructureBreakGuard.isBeingMined(world, gatePos)) {
            return;
        }

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
        ResourceLocation activationSound = new ResourceLocation(
                "scp_additions", manualOverride
                ? "overcharge" : "teslaactivate");
        float activationVolume = manualOverride ? 2.0F : 1.0F;

        AABB volume = TeslaGateVolume.at(x, y, z);
        List<LivingEntity> occupants = world.getEntitiesOfClass(
                LivingEntity.class, volume,
                entity -> TeslaGateVolume.intersects(entity, volume));
        if (occupants.isEmpty()) {
            return;
        }

        if (world instanceof ServerLevel server
                && Scp079TeslaSuppression.shouldSuppress(server, gatePos,
                occupants, manualOverride)) {
            return;
        }

        if (world instanceof Level level) {
            if (!level.isClientSide()) {
                level.playSound(null, gatePos,
                        ForgeRegistries.SOUND_EVENTS.getValue(activationSound),
                        SoundSource.HOSTILE, activationVolume,
                        manualOverride ? 1.25F : 1.0F);
            } else {
                level.playLocalSound(x, y, z,
                        ForgeRegistries.SOUND_EVENTS.getValue(activationSound),
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
