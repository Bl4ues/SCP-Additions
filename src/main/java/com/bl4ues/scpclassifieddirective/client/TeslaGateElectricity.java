package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.block.entity.TeslaGateBlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side procedural electrical arcs for the replacement Tesla Gate.
 * No animated electricity textures are used: each visible bolt is a cheap
 * polyline assembled from short-lived full-bright particles.
 */
public final class TeslaGateElectricity {
    private static final double ACTIVE_VIEW_RANGE = 18.0D;
    private static final double APPROACH_RANGE = 5.0D;
    private static final double FRAME_HALF_WIDTH = 1.08D;
    private static final double FRAME_MIN_Y = 0.28D;
    private static final double FRAME_MAX_Y = 3.48D;
    private static final int NORMAL_MAX_ARCS = 3;
    private static final int OVERRIDE_MAX_ARCS = 5;

    private TeslaGateElectricity() {
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
            TeslaGateBlockEntity gate) {
        if (!gate.isPowered()) return;

        Player player = level.getNearestPlayer(pos.getX() + 0.5D,
                pos.getY() + 1.7D, pos.getZ() + 0.5D,
                ACTIVE_VIEW_RANGE, false);
        if (player == null) return;

        RandomSource random = level.random;
        TeslaGateBlockEntity.Sequence sequence = gate.getSequence();
        long elapsed = gate.sequenceElapsedTicks();
        float approach = approachFactor(player, pos, state);

        int arcs = 0;
        float jitter = 0.16F;
        float branchChance = 0.0F;

        if (sequence == TeslaGateBlockEntity.Sequence.IDLE) {
            if (approach <= 0.0F) return;
            // A powered gate starts to crackle as something approaches the
            // sensor, without changing the authoritative trigger threshold.
            float chance = 0.025F + approach * approach * 0.28F;
            if (random.nextFloat() < chance) arcs = 1;
            jitter = 0.09F + approach * 0.09F;
            branchChance = 0.08F + approach * 0.12F;
        } else if (sequence == TeslaGateBlockEntity.Sequence.NORMAL) {
            if (elapsed < TeslaGateBlockEntity.DISCHARGE_TICK) {
                float charge = Mth.clamp(elapsed
                        / (float) TeslaGateBlockEntity.DISCHARGE_TICK, 0.0F, 1.0F);
                if (random.nextFloat() < 0.18F + charge * 0.62F) {
                    arcs = 1 + (charge > 0.72F && random.nextBoolean() ? 1 : 0);
                }
                jitter = 0.13F + charge * 0.11F;
                branchChance = 0.14F + charge * 0.18F;
            } else {
                float strength = normalDischargeStrength(elapsed);
                arcs = Math.min(NORMAL_MAX_ARCS,
                        Math.max(0, Math.round(strength * NORMAL_MAX_ARCS)));
                if (arcs == 0 && strength > 0.08F && random.nextFloat() < strength) arcs = 1;
                jitter = 0.22F;
                branchChance = 0.28F * strength;
            }
        } else if (sequence == TeslaGateBlockEntity.Sequence.OVERRIDE) {
            float strength = overrideStrength(elapsed);
            arcs = Math.min(OVERRIDE_MAX_ARCS,
                    Math.max(1, Math.round(1.0F + strength * (OVERRIDE_MAX_ARCS - 1))));
            // Override should look substantially less civilized than a normal
            // discharge, but still has a hard particle budget per gate/tick.
            jitter = 0.28F + 0.08F * strength;
            branchChance = 0.42F + 0.28F * strength;
        }

        for (int i = 0; i < arcs; i++) {
            spawnFrameArc(level, pos, state, random, jitter, branchChance,
                    sequence == TeslaGateBlockEntity.Sequence.OVERRIDE);
        }
    }

    private static float approachFactor(Player player, BlockPos pos,
            BlockState state) {
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        Vec3 center = Vec3.atBottomCenterOf(pos).add(0.0D, 1.7D, 0.0D);
        Vec3 delta = player.position().subtract(center);

        // Sensor is three blocks deep and about 2.2 blocks wide. Distance is
        // measured outside that volume, so approach=1 when entering it.
        double forward = Math.abs(delta.x * facing.getStepX()
                + delta.z * facing.getStepZ());
        Direction right = facing.getClockWise();
        double lateral = Math.abs(delta.x * right.getStepX()
                + delta.z * right.getStepZ());
        double outsideForward = Math.max(0.0D, forward - 1.5D);
        double outsideLateral = Math.max(0.0D, lateral - FRAME_HALF_WIDTH);
        double distance = Math.sqrt(outsideForward * outsideForward
                + outsideLateral * outsideLateral);
        return Mth.clamp((float) (1.0D - distance / APPROACH_RANGE), 0.0F, 1.0F);
    }

    private static float normalDischargeStrength(long elapsed) {
        // Normal sample stays at full energy until 1.75 s after its t=10 start,
        // then fades through 2.80 s. Absolute sequence ticks: 25..45..66.
        if (elapsed <= 45L) return 1.0F;
        if (elapsed >= 66L) return 0.0F;
        return 1.0F - (elapsed - 45L) / 21.0F;
    }

    private static float overrideStrength(long elapsed) {
        // Override sample is forceful through 4 s, then drops rapidly by 5 s.
        if (elapsed <= 80L) return 1.0F;
        if (elapsed >= 100L) return 0.0F;
        return 1.0F - (elapsed - 80L) / 20.0F;
    }

    private static void spawnFrameArc(Level level, BlockPos pos, BlockState state,
            RandomSource random, float jitter, float branchChance,
            boolean override) {
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        Direction right = facing.getClockWise();
        Vec3 center = Vec3.atLowerCornerOf(pos).add(0.5D, 0.0D, 0.5D);

        double startY = Mth.lerp(random.nextDouble(), FRAME_MIN_Y, FRAME_MAX_Y);
        double endY = Mth.lerp(random.nextDouble(), FRAME_MIN_Y, FRAME_MAX_Y);
        double depthA = (random.nextDouble() - 0.5D) * 0.62D;
        double depthB = (random.nextDouble() - 0.5D) * 0.62D;
        boolean flip = random.nextBoolean();
        double startSide = flip ? -FRAME_HALF_WIDTH : FRAME_HALF_WIDTH;
        double endSide = -startSide;

        Vec3 start = basisPoint(center, right, facing, startSide, startY, depthA);
        Vec3 end = basisPoint(center, right, facing, endSide, endY, depthB);
        int nodes = override ? 7 : 6;
        Vec3[] points = jaggedPolyline(start, end, right, facing, random,
                nodes, jitter);
        emitPolyline(level, points, override ? 2 : 1);

        if (random.nextFloat() < branchChance) {
            int branchIndex = 1 + random.nextInt(Math.max(1, points.length - 2));
            Vec3 branchStart = points[branchIndex];
            double side = (random.nextBoolean() ? -1.0D : 1.0D)
                    * (0.22D + random.nextDouble() * 0.62D);
            double branchY = Mth.clamp(branchStart.y + (random.nextDouble() - 0.5D)
                    * (override ? 1.45D : 0.85D),
                    pos.getY() + FRAME_MIN_Y, pos.getY() + FRAME_MAX_Y);
            double branchDepth = (random.nextDouble() - 0.5D)
                    * (override ? 1.05D : 0.65D);
            Vec3 branchEnd = branchStart
                    .add(right.getStepX() * side, 0.0D, right.getStepZ() * side)
                    .add(facing.getStepX() * branchDepth, branchY - branchStart.y,
                            facing.getStepZ() * branchDepth);
            emitPolyline(level, jaggedPolyline(branchStart, branchEnd, right,
                    facing, random, override ? 5 : 4, jitter * 0.72F), 1);
        }
    }

    private static Vec3 basisPoint(Vec3 center, Direction right,
            Direction facing, double side, double y, double depth) {
        return center.add(right.getStepX() * side,
                y,
                right.getStepZ() * side)
                .add(facing.getStepX() * depth, 0.0D,
                        facing.getStepZ() * depth);
    }

    private static Vec3[] jaggedPolyline(Vec3 start, Vec3 end,
            Direction right, Direction facing, RandomSource random,
            int nodes, float jitter) {
        Vec3[] points = new Vec3[nodes];
        points[0] = start;
        points[nodes - 1] = end;
        for (int i = 1; i < nodes - 1; i++) {
            double t = i / (double) (nodes - 1);
            Vec3 base = start.lerp(end, t);
            double lateral = (random.nextDouble() - 0.5D) * jitter * 2.0D;
            double vertical = (random.nextDouble() - 0.5D) * jitter * 2.25D;
            double depth = (random.nextDouble() - 0.5D) * jitter * 1.45D;
            points[i] = base.add(right.getStepX() * lateral,
                    vertical,
                    right.getStepZ() * lateral)
                    .add(facing.getStepX() * depth, 0.0D,
                            facing.getStepZ() * depth);
        }
        return points;
    }

    private static void emitPolyline(Level level, Vec3[] points,
            int samplesPerSegment) {
        for (int i = 0; i < points.length - 1; i++) {
            Vec3 a = points[i];
            Vec3 b = points[i + 1];
            int samples = Math.max(1, samplesPerSegment);
            for (int sample = 0; sample <= samples; sample++) {
                double t = sample / (double) samples;
                Vec3 p = a.lerp(b, t);
                Vec3 motion = b.subtract(a).normalize().scale(0.02D);
                level.addParticle(ScpClassifiedDirectiveModParticleTypes.TESLA_ARC.get(),
                        p.x, p.y, p.z, motion.x, motion.y, motion.z);
            }
        }
    }
}
