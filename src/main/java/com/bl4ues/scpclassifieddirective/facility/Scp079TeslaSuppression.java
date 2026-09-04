package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Local, temporary SCP-079 interference with individual Tesla Gates. */
public final class Scp079TeslaSuppression {
    private static final int SUPPRESSION_TICKS = 60;
    private static final int DEVICE_REUSE_TICKS = 100;

    private static final double NORMAL_COST = 12.0D;
    private static final double OVERRIDE_COST = 18.0D;

    private static final Map<GateKey, GateState> STATES =
            new ConcurrentHashMap<>();

    private Scp079TeslaSuppression() {
    }

    /**
     * Returns true while the gate should skip its normal activation sequence.
     * This never changes the global Tesla Gate or manual-override gamerules.
     * Once a useful pursuit enters the broad gate sensor, SCP-079 suppresses it
     * deterministically whenever enough processing power is available. Only a
     * player already intersecting the visible lethal arc blocks that passage.
     */
    public static boolean shouldSuppress(ServerLevel level, BlockPos gatePos,
            List<LivingEntity> detectionOccupants,
            List<LivingEntity> lethalOccupants, boolean manualOverride) {
        GateKey key = key(level, gatePos);
        if (!Scp079ProcessingManager.isActive(level)) {
            STATES.remove(key);
            return false;
        }

        long now = level.getGameTime();
        GateState state = STATES.get(key);
        if (state != null && state.expired(now)) {
            STATES.remove(key, state);
            state = null;
        }
        if (state != null && now < state.suppressedUntil()) {
            return true;
        }
        if (state != null && now < state.reuseAfter()) {
            return false;
        }

        // A player already inside the actual arc must never receive unexplained
        // safe passage merely because a pursuer entered the broader sensor.
        if (lethalOccupants.stream()
                .anyMatch(ServerPlayer.class::isInstance)) {
            return false;
        }

        Mob pursuer = detectionOccupants.stream()
                .filter(Mob.class::isInstance)
                .map(Mob.class::cast)
                .filter(Scp079TeslaSuppression::isUsefulPursuer)
                .findFirst().orElse(null);
        if (pursuer == null) return false;

        double cost = manualOverride ? OVERRIDE_COST : NORMAL_COST;
        if (!Scp079ProcessingManager.trySpend(level, cost)) return false;

        long suppressedUntil = now + SUPPRESSION_TICKS;
        STATES.put(key, new GateState(suppressedUntil,
                suppressedUntil + DEVICE_REUSE_TICKS));
        emitInterference(level, gatePos);

        ServerPlayer target = pursuer.getTarget() instanceof ServerPlayer player
                ? player : null;
        String targetName = target == null ? "player"
                : target.getGameProfile().getName();
        String mode = manualOverride ? "Emergency Override"
                : "normal circuit";
        Scp079DecisionLog.record(level,
                Scp079DecisionLog.DecisionType.TESLA_SUPPRESSION,
                Scp079DecisionLog.DecisionOutcome.EXECUTED, gatePos, cost,
                "guaranteed safe passage for "
                        + pursuer.getDisplayName().getString()
                        + " pursuing " + targetName + " · " + mode
                        + " · " + SUPPRESSION_TICKS / 20.0D + "s");
        if (target != null) {
            Scp079FacilityAccessManager.awardFirstInterference(target);
        }
        return true;
    }

    /** Player-controlled SCP-079 can deliberately suppress the aimed gate. */
    public static boolean tryPlayerSuppress(ServerLevel level,
            BlockPos gatePos) {
        if (level == null || gatePos == null
                || !Scp079PlayableManager.hasController(level.getServer())
                || !Scp079ProcessingManager.isActive(level)) return false;
        GateKey key = key(level, gatePos);
        long now = level.getGameTime();
        GateState state = STATES.get(key);
        if (state != null && state.expired(now)) {
            STATES.remove(key, state);
            state = null;
        }
        if (state != null && now < state.suppressedUntil()) return true;
        if (state != null && now < state.reuseAfter()) return false;
        if (!Scp079PlayerPower.trySpend(level, NORMAL_COST)) return false;

        long suppressedUntil = now + SUPPRESSION_TICKS;
        STATES.put(key, new GateState(suppressedUntil,
                suppressedUntil + DEVICE_REUSE_TICKS));
        emitInterference(level, gatePos);
        Scp079DecisionLog.record(level,
                Scp079DecisionLog.DecisionType.TESLA_SUPPRESSION,
                Scp079DecisionLog.DecisionOutcome.EXECUTED, gatePos,
                Scp079ProcessingManager.adjustedActionCost(level, NORMAL_COST),
                "manual SCP-079 camera suppression · "
                        + SUPPRESSION_TICKS / 20.0D + "s");
        return true;
    }

    private static boolean isUsefulPursuer(Mob mob) {
        if (!mob.isAlive() || !(mob.getTarget() instanceof ServerPlayer player)) {
            return false;
        }
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private static void emitInterference(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT,
                SoundSource.HOSTILE, 0.85F, 0.72F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                10, 0.50D, 0.65D, 0.50D, 0.035D);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                3, 0.40D, 0.35D, 0.40D, 0.015D);
    }

    private static GateKey key(ServerLevel level, BlockPos pos) {
        return new GateKey(level.dimension(), pos.asLong());
    }

    private record GateKey(ResourceKey<Level> dimension, long pos) {
    }

    private record GateState(long suppressedUntil, long reuseAfter) {
        private boolean expired(long now) {
            return now >= suppressedUntil && now >= reuseAfter;
        }
    }
}
