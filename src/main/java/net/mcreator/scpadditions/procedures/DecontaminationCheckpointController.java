package net.mcreator.scpadditions.procedures;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DecontaminationCheckpointController {
    private static final int CLOSE_DELAY_TICKS = 3;
    private static final int PROCESSING_TICKS = 100;
    private static final int PARTICLE_INTERVAL_TICKS = 5;
    private static final int PARTICLE_BURSTS = PROCESSING_TICKS / PARTICLE_INTERVAL_TICKS + 1;
    private static final int PARTICLES_PER_VENT = 3;
    private static final int GAS_PARTICLES_PER_VENT = 6;

    // Exact usable grille rectangles from models/custom/deconclosed.json.
    // The model's unrotated coordinates are used by the NORTH blockstate.
    private static final double VENT_MIN_X = 11.1D;
    private static final double VENT_MAX_X = 21.0D;
    private static final double VENT_SURFACE_Y = -15.25D;
    private static final double CHAMBER_MODEL_CENTER_Z = 8.0D;
    private static final double[][] VENT_Z_RANGES = {
            {-9.7D, -0.4D},
            {16.3D, 25.6D}
    };

    private static final Set<CheckpointKey> LATCHED_UNTIL_EXIT = ConcurrentHashMap.newKeySet();
    private static final Set<CheckpointKey> PROCESSING = ConcurrentHashMap.newKeySet();

    private DecontaminationCheckpointController() {
    }

    public static void scanOpen(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!state.is(ScpAdditionsModBlocks.DECON_OPEN.get())) return;

        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());
        List<ServerPlayer> players = playersInside(level, pos, state);
        if (LATCHED_UNTIL_EXIT.contains(key)) {
            if (players.isEmpty()) LATCHED_UNTIL_EXIT.remove(key);
            return;
        }
        if (players.isEmpty()) return;

        LATCHED_UNTIL_EXIT.add(key);
        ScpAdditionsMod.queueServerWork(CLOSE_DELAY_TICKS, () -> {
            BlockState current = level.getBlockState(pos);
            if (!current.is(ScpAdditionsModBlocks.DECON_OPEN.get())) return;
            if (playersInside(level, pos, current).isEmpty()) {
                LATCHED_UNTIL_EXIT.remove(key);
                return;
            }

            level.playSound(null, BlockPos.containing(chamberCenter(pos, current)), ScpAdditionsModSounds.DOORCLOSING.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, copyCommonState(
                    ScpAdditionsModBlocks.DECON_CLOSED.get().defaultBlockState(), current), 3);
        });
    }

    public static void beginClosed(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!state.is(ScpAdditionsModBlocks.DECON_CLOSED.get())) return;

        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());
        if (!PROCESSING.add(key)) return;

        List<ServerPlayer> players = playersInside(level, pos, state);
        if (!players.isEmpty()) {
            level.playSound(null, BlockPos.containing(chamberCenter(pos, state)), ScpAdditionsModSounds.DECONTAMINATION.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            for (ServerPlayer player : players) {
                decontaminate(level, player);
            }
            for (int burst = 0; burst < PARTICLE_BURSTS; burst++) {
                int delay = burst * PARTICLE_INTERVAL_TICKS;
                ScpAdditionsMod.queueServerWork(delay, () -> {
                    BlockState current = level.getBlockState(pos);
                    if (current.is(ScpAdditionsModBlocks.DECON_CLOSED.get())) {
                        emitFloorVentParticles(level, pos, current);
                    }
                });
            }
        }

        ScpAdditionsMod.queueServerWork(PROCESSING_TICKS, () -> {
            try {
                BlockState current = level.getBlockState(pos);
                if (!current.is(ScpAdditionsModBlocks.DECON_CLOSED.get())) return;

                level.playSound(null, BlockPos.containing(chamberCenter(pos, current)), ScpAdditionsModSounds.DOOROPEN.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                level.setBlock(pos, copyCommonState(
                        ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get().defaultBlockState(), current), 3);
            } finally {
                PROCESSING.remove(key);
            }
        });
    }

    public static void finishReload(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!state.is(ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get())) return;
        level.setBlock(pos, copyCommonState(
                ScpAdditionsModBlocks.DECON_OPEN.get().defaultBlockState(), state), 3);
    }

    private static void decontaminate(ServerLevel level, ServerPlayer player) {
        player.removeAllEffects();

        if (level.getGameRules().getBoolean(ScpAdditionsModGameRules.DECONCHECKPOINT)) {
            player.setRespawnPosition(player.level().dimension(), player.blockPosition(),
                    player.getYRot(), true, false);
        }

        Advancement advancement = player.server.getAdvancements().getAdvancement(
                new ResourceLocation("scp_additions", "decon_achievement"));
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    private static List<ServerPlayer> playersInside(ServerLevel level, BlockPos pos, BlockState state) {
        AABB chamber = chamberBox(pos, facing(state));
        return level.getEntitiesOfClass(ServerPlayer.class, chamber,
                player -> player.isAlive() && !player.isRemoved() && !player.isSpectator()
                        && chamber.contains(player.getBoundingBox().getCenter()));
    }

    /**
     * Interior bounds are derived from the model/collision coordinates:
     * SOUTH x=-13..13, z=-13..29, y=-16..19, rotated per facing.
     * A small 0.08-block inset prevents a player touching the exterior frame
     * from triggering the checkpoint.
     */
    private static AABB chamberBox(BlockPos pos, Direction facing) {
        double margin = 0.08D;
        double minY = pos.getY() - 1.0D + margin;
        double maxY = pos.getY() + (19.0D / 16.0D) - margin;

        return switch (facing) {
            case NORTH -> new AABB(
                    pos.getX() + (3.0D / 16.0D) + margin, minY,
                    pos.getZ() - (13.0D / 16.0D) + margin,
                    pos.getX() + (29.0D / 16.0D) - margin, maxY,
                    pos.getZ() + (29.0D / 16.0D) - margin);
            case EAST -> new AABB(
                    pos.getX() - (13.0D / 16.0D) + margin, minY,
                    pos.getZ() + (3.0D / 16.0D) + margin,
                    pos.getX() + (29.0D / 16.0D) - margin, maxY,
                    pos.getZ() + (29.0D / 16.0D) - margin);
            case WEST -> new AABB(
                    pos.getX() - (13.0D / 16.0D) + margin, minY,
                    pos.getZ() - (13.0D / 16.0D) + margin,
                    pos.getX() + (29.0D / 16.0D) - margin, maxY,
                    pos.getZ() + (13.0D / 16.0D) - margin);
            default -> new AABB(
                    pos.getX() - (13.0D / 16.0D) + margin, minY,
                    pos.getZ() - (13.0D / 16.0D) + margin,
                    pos.getX() + (13.0D / 16.0D) - margin, maxY,
                    pos.getZ() + (29.0D / 16.0D) - margin);
        };
    }

    private static void emitFloorVentParticles(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = facing(state);
        for (double[] zRange : VENT_Z_RANGES) {
            for (int particle = 0; particle < PARTICLES_PER_VENT; particle++) {
                double modelX = Mth.lerp(level.random.nextDouble(), VENT_MIN_X, VENT_MAX_X);
                double modelZ = Mth.lerp(level.random.nextDouble(), zRange[0], zRange[1]);
                Vec3 origin = modelPointToWorld(pos, facing, modelX, VENT_SURFACE_Y, modelZ);

                double localVelocityX = (level.random.nextDouble() - 0.5D) * 0.018D;
                double localVelocityZ = (level.random.nextDouble() - 0.5D) * 0.018D;
                double velocityY = 0.055D + level.random.nextDouble() * 0.025D;
                Vec3 velocity = rotateModelVector(facing, localVelocityX, velocityY, localVelocityZ);

                // A zero-count particle packet uses the offsets as one particle's
                // velocity, which gives the vapor a consistent upward bias.
                level.sendParticles(ParticleTypes.CLOUD,
                        origin.x, origin.y, origin.z,
                        0, velocity.x, velocity.y, velocity.z, 1.0D);
            }
        }

        emitRisingVentGas(level, pos, facing);
    }

    private static void emitRisingVentGas(ServerLevel level, BlockPos pos, Direction facing) {
        for (double[] zRange : VENT_Z_RANGES) {
            for (int particle = 0; particle < GAS_PARTICLES_PER_VENT; particle++) {
                double modelX = Mth.lerp(level.random.nextDouble(), VENT_MIN_X, VENT_MAX_X);
                double modelZ = Mth.lerp(level.random.nextDouble(), zRange[0], zRange[1]);
                double modelY = VENT_SURFACE_Y + level.random.nextDouble() * 0.8D;
                Vec3 origin = modelPointToWorld(pos, facing, modelX, modelY, modelZ);

                // These clouds still originate at the grilles, but rise much
                // faster and drift inward. The two plumes meet in the chamber
                // instead of accumulating as two opaque piles on the floor.
                double inwardDirection = Math.signum(CHAMBER_MODEL_CENTER_Z - modelZ);
                double localVelocityX = (level.random.nextDouble() - 0.5D) * 0.075D;
                double localVelocityZ = inwardDirection * (0.050D + level.random.nextDouble() * 0.030D)
                        + (level.random.nextDouble() - 0.5D) * 0.018D;
                double velocityY = 0.105D + level.random.nextDouble() * 0.050D;
                Vec3 velocity = rotateModelVector(
                        facing, localVelocityX, velocityY, localVelocityZ);

                level.sendParticles(ParticleTypes.CLOUD,
                        origin.x, origin.y, origin.z,
                        0, velocity.x, velocity.y, velocity.z, 1.0D);
            }
        }
    }

    private static Vec3 modelPointToWorld(BlockPos pos, Direction facing,
            double modelX, double modelY, double modelZ) {
        double rotatedX;
        double rotatedZ;
        switch (facing) {
            case EAST -> {
                rotatedX = 16.0D - modelZ;
                rotatedZ = modelX;
            }
            case SOUTH -> {
                rotatedX = 16.0D - modelX;
                rotatedZ = 16.0D - modelZ;
            }
            case WEST -> {
                rotatedX = modelZ;
                rotatedZ = 16.0D - modelX;
            }
            default -> {
                rotatedX = modelX;
                rotatedZ = modelZ;
            }
        }
        return new Vec3(
                pos.getX() + rotatedX / 16.0D,
                pos.getY() + modelY / 16.0D,
                pos.getZ() + rotatedZ / 16.0D);
    }

    private static Vec3 rotateModelVector(Direction facing, double x, double y, double z) {
        return switch (facing) {
            case EAST -> new Vec3(-z, y, x);
            case SOUTH -> new Vec3(-x, y, -z);
            case WEST -> new Vec3(z, y, -x);
            default -> new Vec3(x, y, z);
        };
    }

    private static Vec3 chamberCenter(BlockPos pos, BlockState state) {
        return chamberBox(pos, facing(state)).getCenter();
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.SOUTH;
    }

    private static BlockState copyCommonState(BlockState target, BlockState source) {
        if (target.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && source.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            target = target.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    source.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        if (target.hasProperty(BlockStateProperties.WATERLOGGED)
                && source.hasProperty(BlockStateProperties.WATERLOGGED)) {
            target = target.setValue(BlockStateProperties.WATERLOGGED,
                    source.getValue(BlockStateProperties.WATERLOGGED));
        }
        return target;
    }

    private record CheckpointKey(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
