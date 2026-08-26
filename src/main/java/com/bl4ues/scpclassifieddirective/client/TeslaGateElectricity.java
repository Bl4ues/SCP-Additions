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
 *
 * Each bolt is generated as one coherent jagged polyline spanning the lethal
 * opening, then densely sampled with short-lived full-bright particles. This
 * keeps the effect crisp and continuous instead of turning the discharge into
 * a handful of unrelated glowing dots.
 */
public final class TeslaGateElectricity {
    private static final double ACTIVE_VIEW_RANGE = 18.0D;
    private static final double APPROACH_RANGE = 4.0D;
    private static final double FRAME_HALF_WIDTH = 1.08D;
    private static final double FRAME_MIN_Y = 0.28D;
    private static final double FRAME_MAX_Y = 3.48D;
    private static final double FRAME_HALF_DEPTH = 0.31D;
    private static final double NORMAL_SAMPLE_SPACING = 0.075D;
    private static final double OVERRIDE_SAMPLE_SPACING = 0.060D;
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
        float approach = approachFactor(player, pos);

        int arcs = 0;
        float jitter = 0.16F;
        float branchChance = 0.0F;
        boolean override = sequence == TeslaGateBlockEntity.Sequence.OVERRIDE;
        boolean overrideDischarged = override
                && elapsed >= TeslaGateBlockEntity.OVERRIDE_DISCHARGE_TICK;

        if (sequence == TeslaGateBlockEntity.Sequence.IDLE) {
            if (approach <= 0.0F) return;

            // Sparse warning crackles only once the player is within four
            // horizontal blocks of the controller block's center.
            float chance = 0.018F + approach * approach * 0.20F;
            if (random.nextFloat() < chance) arcs = 1;
            jitter = 0.08F + approach * 0.07F;
            branchChance = 0.05F + approach * 0.10F;
        } else if (sequence == TeslaGateBlockEntity.Sequence.NORMAL) {
            if (elapsed < TeslaGateBlockEntity.DISCHARGE_TICK) {
                float charge = Mth.clamp(elapsed
                        / (float) TeslaGateBlockEntity.DISCHARGE_TICK, 0.0F, 1.0F);
                if (random.nextFloat() < 0.10F + charge * 0.52F) {
                    arcs = 1 + (charge > 0.78F && random.nextFloat() < 0.35F ? 1 : 0);
                }
                jitter = 0.11F + charge * 0.09F;
                branchChance = 0.08F + charge * 0.16F;
            } else {
                float strength = normalDischargeStrength(elapsed);
                arcs = Math.min(NORMAL_MAX_ARCS,
                        Math.max(0, Math.round(strength * NORMAL_MAX_ARCS)));
                if (arcs == 0 && strength > 0.08F
                        && random.nextFloat() < strength) {
                    arcs = 1;
                }
                jitter = 0.18F + strength * 0.05F;
                branchChance = 0.18F + 0.20F * strength;
            }
        } else if (override) {
            if (!overrideDischarged) {
                float charge = Mth.clamp(elapsed
                        / (float) TeslaGateBlockEntity.OVERRIDE_DISCHARGE_TICK,
                        0.0F, 1.0F);
                if (random.nextFloat() < 0.08F + charge * 0.44F) {
                    arcs = 1 + (charge > 0.82F
                            && random.nextFloat() < 0.28F ? 1 : 0);
                }
                jitter = 0.11F + charge * 0.10F;
                branchChance = 0.08F + charge * 0.15F;
            } else {
                float strength = overrideStrength(elapsed);
                if (strength <= 0.0F) {
                    if (random.nextFloat() < 0.22F) arcs = 1;
                } else {
                    arcs = Math.min(OVERRIDE_MAX_ARCS,
                            Math.max(2, Math.round(1.0F
                                    + strength * (OVERRIDE_MAX_ARCS - 1))));
                }
                jitter = 0.25F + 0.08F * strength;
                branchChance = 0.38F + 0.30F * strength;
            }
        }

        for (int i = 0; i < arcs; i++) {
            spawnFrameArc(level, pos, state, random, jitter, branchChance,
                    overrideDischarged);
        }
    }

    private static float approachFactor(Player player, BlockPos pos) {
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double dx = player.getX() - centerX;
        double dz = player.getZ() - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        return Mth.clamp((float) (1.0D - distance / APPROACH_RANGE),
                0.0F, 1.0F);
    }

    private static float normalDischargeStrength(long elapsed) {
        if (elapsed <= 45L) return 1.0F;
        if (elapsed >= 66L) return 0.0F;
        return 1.0F - (elapsed - 45L) / 21.0F;
    }

    private static float overrideStrength(long elapsed) {
        // Manual override discharge was moved one second later to match the
        // uploaded audio transient. Preserve the same post-shot envelope by
        // shifting the strong and fade windows forward by the same 20 ticks.
        if (elapsed <= 100L) return 1.0F;
        if (elapsed >= 120L) return 0.0F;
        return 1.0F - (elapsed - 100L) / 20.0F;
    }

    private static void spawnFrameArc(Level level, BlockPos pos,
            BlockState state, RandomSource random, float jitter,
            float branchChance, boolean override) {
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        Direction right = facing.getClockWise();
        Vec3 center = Vec3.atLowerCornerOf(pos).add(0.5D, 0.0D, 0.5D);

        // Both ends stay attached to opposite sides of the physical gate. Their
        // independent heights make each discharge cross the opening differently.
        double startY = randomFrameY(random);
        double endY = randomFrameY(random);
        double depthA = randomDepth(random, FRAME_HALF_DEPTH);
        double depthB = randomDepth(random, FRAME_HALF_DEPTH);
        boolean flip = random.nextBoolean();
        double startSide = flip ? -FRAME_HALF_WIDTH : FRAME_HALF_WIDTH;
        double endSide = -startSide;

        Vec3 start = basisPoint(center, right, facing,
                startSide, startY, depthA);
        Vec3 end = basisPoint(center, right, facing,
                endSide, endY, depthB);
        int nodes = override ? 9 : 7;
        Vec3[] points = jaggedPolyline(start, end, right, facing, random,
                nodes, jitter);
        emitPolyline(level, points,
                override ? OVERRIDE_SAMPLE_SPACING : NORMAL_SAMPLE_SPACING);

        if (random.nextFloat() < branchChance) {
            spawnBranch(level, pos, center, right, facing, points, random,
                    jitter, override);
        }
        if (override && random.nextFloat() < branchChance * 0.58F) {
            spawnBranch(level, pos, center, right, facing, points, random,
                    jitter * 0.88F, true);
        }
    }

    private static void spawnBranch(Level level, BlockPos pos, Vec3 center,
            Direction right, Direction facing, Vec3[] source,
            RandomSource random, float jitter, boolean override) {
        int branchIndex = 1 + random.nextInt(Math.max(1, source.length - 2));
        Vec3 branchStart = source[branchIndex];

        // Branches terminate on the frame instead of stopping in mid-air. The
        // chosen rail is whichever side is reached by a short secondary fork.
        double localSide = localCoordinate(branchStart, center, right);
        double targetSide;
        if (Math.abs(localSide) > FRAME_HALF_WIDTH * 0.50D) {
            targetSide = Math.copySign(FRAME_HALF_WIDTH, localSide);
        } else {
            targetSide = random.nextBoolean()
                    ? -FRAME_HALF_WIDTH : FRAME_HALF_WIDTH;
        }
        double targetY = Mth.clamp(branchStart.y
                        + (random.nextDouble() - 0.5D)
                        * (override ? 1.25D : 0.78D),
                pos.getY() + FRAME_MIN_Y,
                pos.getY() + FRAME_MAX_Y);
        double targetDepth = randomDepth(random,
                override ? FRAME_HALF_DEPTH * 1.35D : FRAME_HALF_DEPTH);
        Vec3 branchEnd = basisPoint(center, right, facing, targetSide,
                targetY - pos.getY(), targetDepth);

        Vec3[] branch = jaggedPolyline(branchStart, branchEnd, right, facing,
                random, override ? 6 : 5, jitter * 0.66F);
        emitPolyline(level, branch,
                override ? OVERRIDE_SAMPLE_SPACING : NORMAL_SAMPLE_SPACING);
    }

    private static double randomFrameY(RandomSource random) {
        return Mth.lerp(random.nextDouble(), FRAME_MIN_Y, FRAME_MAX_Y);
    }

    private static double randomDepth(RandomSource random, double halfDepth) {
        return (random.nextDouble() * 2.0D - 1.0D) * halfDepth;
    }

    private static double localCoordinate(Vec3 point, Vec3 center,
            Direction axis) {
        return (point.x - center.x) * axis.getStepX()
                + (point.z - center.z) * axis.getStepZ();
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
            double envelope = Math.sin(Math.PI * t);
            double lateral = (random.nextDouble() - 0.5D)
                    * jitter * 2.0D * envelope;
            double vertical = (random.nextDouble() - 0.5D)
                    * jitter * 2.15D * envelope;
            double depth = (random.nextDouble() - 0.5D)
                    * jitter * 1.35D * envelope;
            points[i] = base.add(right.getStepX() * lateral,
                    vertical,
                    right.getStepZ() * lateral)
                    .add(facing.getStepX() * depth, 0.0D,
                            facing.getStepZ() * depth);
        }
        return points;
    }

    private static void emitPolyline(Level level, Vec3[] points,
            double spacing) {
        for (int i = 0; i < points.length - 1; i++) {
            Vec3 a = points[i];
            Vec3 b = points[i + 1];
            double length = a.distanceTo(b);
            int samples = Math.max(2, Mth.ceil(length / spacing));
            int first = i == 0 ? 0 : 1;
            for (int sample = first; sample <= samples; sample++) {
                double t = sample / (double) samples;
                Vec3 p = a.lerp(b, t);
                level.addParticle(
                        ScpClassifiedDirectiveModParticleTypes.TESLA_ARC.get(),
                        p.x, p.y, p.z, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
