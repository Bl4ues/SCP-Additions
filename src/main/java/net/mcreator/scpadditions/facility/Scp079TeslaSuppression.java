package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Local, temporary SCP-079 interference with individual Tesla Gates. */
public final class Scp079TeslaSuppression {
    private static final int SUPPRESSION_TICKS = 60;
    private static final int DEVICE_REUSE_TICKS = 100;
    private static final int FAILED_ROLL_RETRY_TICKS = 30;

    private static final double NORMAL_COST = 12.0D;
    private static final double OVERRIDE_COST = 18.0D;
    private static final float NORMAL_CHANCE = 0.42F;
    private static final float OVERRIDE_CHANCE = 0.28F;

    private static final Map<GateKey, GateState> STATES =
            new ConcurrentHashMap<>();

    private Scp079TeslaSuppression() {
    }

    /**
     * Returns true while the gate should skip its normal activation sequence.
     * This never changes the global Tesla Gate or manual-override gamerules.
     */
    public static boolean shouldSuppress(ServerLevel level, BlockPos gatePos,
            List<LivingEntity> occupants, boolean manualOverride) {
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
        if (state != null && (now < state.reuseAfter()
                || now < state.nextAttempt())) {
            return false;
        }

        // A player already inside the lethal volume must never receive an
        // unexplained safe passage merely because a pursuer entered with them.
        if (occupants.stream().anyMatch(ServerPlayer.class::isInstance)) {
            return false;
        }

        Mob pursuer = occupants.stream()
                .filter(Mob.class::isInstance)
                .map(Mob.class::cast)
                .filter(Scp079TeslaSuppression::isUsefulPursuer)
                .findFirst().orElse(null);
        if (pursuer == null) return false;

        double cost = manualOverride ? OVERRIDE_COST : NORMAL_COST;
        if (!Scp079ProcessingManager.canAfford(level, cost)) return false;

        ThreatAdjustment adjustment = adjustmentFor(pursuer);
        float chance = (manualOverride ? OVERRIDE_CHANCE : NORMAL_CHANCE)
                * adjustment.chanceMultiplier();
        if (level.getRandom().nextFloat() >= Math.min(0.75F, chance)) {
            STATES.put(key, new GateState(0L, 0L,
                    now + FAILED_ROLL_RETRY_TICKS));
            return false;
        }

        if (!Scp079ProcessingManager.trySpend(level, cost)) return false;
        long suppressedUntil = now + SUPPRESSION_TICKS;
        STATES.put(key, new GateState(suppressedUntil,
                suppressedUntil + DEVICE_REUSE_TICKS, suppressedUntil));
        emitInterference(level, gatePos);
        return true;
    }

    private static boolean isUsefulPursuer(Mob mob) {
        if (!mob.isAlive() || !(mob.getTarget() instanceof ServerPlayer player)) {
            return false;
        }
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private static ThreatAdjustment adjustmentFor(Mob mob) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (id == null) return ThreatAdjustment.DEFAULT;
        return switch (id.getPath()) {
            case "scp_173" -> new ThreatAdjustment(1.10F);
            case "scp_106" -> new ThreatAdjustment(1.20F);
            default -> ThreatAdjustment.DEFAULT;
        };
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

    private record GateState(long suppressedUntil, long reuseAfter,
                             long nextAttempt) {
        private boolean expired(long now) {
            return now >= suppressedUntil && now >= reuseAfter
                    && now >= nextAttempt;
        }
    }

    private record ThreatAdjustment(float chanceMultiplier) {
        private static final ThreatAdjustment DEFAULT =
                new ThreatAdjustment(1.0F);
    }
}
