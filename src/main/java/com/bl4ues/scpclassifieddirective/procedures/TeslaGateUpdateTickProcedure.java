package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityStructureBreakGuard;
import com.bl4ues.scpclassifieddirective.facility.Scp079TeslaSuppression;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;

import java.util.List;

public class TeslaGateUpdateTickProcedure {
    public static boolean execute(LevelAccessor world, double x, double y,
            double z) {
        BlockPos gatePos = BlockPos.containing(x, y, z);
        if (!Scp079FacilityAccessManager.isAuxiliaryPowerOnline(world)) {
            return false;
        }
        if (FacilityStructureBreakGuard.isBeingMined(world, gatePos)) {
            return false;
        }

        boolean manualOverride = world.getLevelData().getGameRules()
                .getBoolean(ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean teslaGateOn = world.getLevelData().getGameRules()
                .getBoolean(ScpClassifiedDirectiveModGameRules.TESLAGATEON);
        if (manualOverride && !teslaGateOn
                && world instanceof Level level && !level.isClientSide()) {
            world.getLevelData().getGameRules()
                    .getRule(ScpClassifiedDirectiveModGameRules.TESLAGATEON)
                    .set(true, level.getServer());
            teslaGateOn = true;
        }
        if (!teslaGateOn && !manualOverride) {
            return false;
        }

        int activationDelay = manualOverride ? 1 : 5;
        ResourceLocation activationSound = new ResourceLocation(
                "scp_classified_directive", manualOverride
                ? "overcharge" : "teslaactivate");
        float activationVolume = manualOverride ? 2.0F : 1.0F;

        AABB detectionVolume = TeslaGateVolume.at(x, y, z);
        List<LivingEntity> occupants = world.getEntitiesOfClass(
                LivingEntity.class,
                TeslaGateVolume.motionCandidates(detectionVolume),
                entity -> TeslaGateVolume.intersectsOrCrossed(entity,
                        detectionVolume));
        if (occupants.isEmpty()) {
            return false;
        }

        AABB lethalVolume = TeslaGateVolume.lethalArcAt(world, gatePos);
        List<LivingEntity> lethalOccupants = occupants.stream()
                .filter(entity -> TeslaGateVolume.intersectsOrCrossed(entity,
                        lethalVolume))
                .toList();
        if (world instanceof ServerLevel server
                && Scp079TeslaSuppression.shouldSuppress(server, gatePos,
                occupants, lethalOccupants, manualOverride)) {
            return false;
        }

        if (world instanceof ServerLevel server
                && occupants.stream().anyMatch(ServerPlayer.class::isInstance)) {
            Scp079FacilityAccessManager.recordActivity(server,
                    Scp079FacilityAccessManager.Activity.TESLA_TRAVERSAL);
        }
        if (world instanceof Level level && !level.isClientSide()) {
            level.playSound(null, gatePos,
                    ForgeRegistries.SOUND_EVENTS.getValue(activationSound),
                    SoundSource.HOSTILE, activationVolume,
                    manualOverride ? 1.25F : 1.0F);
        }
        ScpClassifiedDirectiveMod.queueServerWork(activationDelay,
                () -> TeslaGateTransitionHelper.transitionIfCurrent(
                        world, x, y, z,
                        ScpClassifiedDirectiveModBlocks.TESLA_GATE,
                        ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE));
        return true;
    }
}
