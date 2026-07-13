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
    private static final int PARTICLE_BURSTS = 12;
    private static final int PARTICLE_INTERVAL_TICKS = 5;

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
                        emitFixedNozzleParticles(level, pos, current);
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

    private static void emitFixedNozzleParticles(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = facing(state);
        AABB chamber = chamberBox(pos, facing);
        double y = chamber.maxY - 0.18D;
        double[] fractions = {0.22D, 0.50D, 0.78D};

        if (facing.getAxis() == Direction.Axis.Z) {
            for (double fraction : fractions) {
                double z = Mth.lerp(fraction, chamber.minZ, chamber.maxZ);
                emit(level, chamber.minX + 0.03D, y, z);
                emit(level, chamber.maxX - 0.03D, y, z);
            }
        } else {
            for (double fraction : fractions) {
                double x = Mth.lerp(fraction, chamber.minX, chamber.maxX);
                emit(level, x, y, chamber.minZ + 0.03D);
                emit(level, x, y, chamber.maxZ - 0.03D);
            }
        }
    }

    private static void emit(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.CLOUD, x, y, z,
                6, 0.055D, 0.045D, 0.055D, 0.025D);
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
