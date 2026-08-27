package com.bl4ues.scpclassifieddirective.procedures;

import com.bl4ues.scpclassifieddirective.block.DecontaminationStructure;
import com.bl4ues.scpclassifieddirective.block.entity.DecontaminationBlockEntity;
import com.bl4ues.scpclassifieddirective.effect.EyeProtectionAccess;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityStructureBreakGuard;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModMobEffects;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModParticleTypes;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative sequence for the rebuilt GeckoLib checkpoint. */
public final class DecontaminationCheckpointController {
    /** GeckoLib animation gets a half-second visual lead before gas/audio. */
    public static final int ANIMATION_EFFECTS_DELAY_TICKS = 10;
    /** 2.75 seconds after gas/audio start: visible vapor begins to weaken. */
    public static final int SMOKE_FADE_START_TICK = 55;
    /** New vapor stops at 4.75 seconds; short-lived remnants die by 5.25 s. */
    public static final int SMOKE_EMISSION_END_TICK = 95;
    /** 5.25 seconds after gas/audio start: no vapor should remain. */
    public static final int SMOKE_COMPLETE_TICK = 105;
    /** Full authored GeckoLib animation is 6.5417 seconds. */
    public static final int SEQUENCE_END_TICK = 131;
    private static final int PARTICLE_INTERVAL_TICKS = 2;
    private static final int PARTICLES_PER_VENT = 2;
    private static final int EYE_SORE_DURATION_TICKS = 30 * 20;

    // Authored model coordinates. Vents 2/3 start behind their fan planes and
    // rise through the grilles; vent 1 starts just inside its lower face and
    // moves downward into the chamber.
    private static final double VENT_1_X = 0.0D;
    private static final double VENT_1_Y = 45.6D;
    private static final double VENT_1_Z = 48.025D;
    private static final double FLOOR_VENT_Y = -8.5D;
    private static final double VENT_2_Z = 26.1D;
    private static final double VENT_3_Z = 69.95D;

    private static final Set<CheckpointKey> LATCHED_UNTIL_EXIT =
            ConcurrentHashMap.newKeySet();

    private DecontaminationCheckpointController() {
    }

    public static void scanOpen(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!state.is(ScpClassifiedDirectiveModBlocks.DECON_OPEN.get())
                || FacilityStructureBreakGuard.isBeingMined(level, pos)
                || !(level.getBlockEntity(pos)
                instanceof DecontaminationBlockEntity blockEntity)
                || blockEntity.isActive()) {
            return;
        }

        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());
        List<ServerPlayer> players = playersInside(level, pos, state);
        if (LATCHED_UNTIL_EXIT.contains(key)) {
            if (players.isEmpty()) LATCHED_UNTIL_EXIT.remove(key);
            return;
        }
        if (players.isEmpty()) return;

        LATCHED_UNTIL_EXIT.add(key);
        if (!blockEntity.beginSequence()) return;
        beginDoorClosure(level, pos, state);
    }

    /** Compatibility entry point for old transient DECON_CLOSED world states. */
    public static void beginClosed(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos)
                instanceof DecontaminationBlockEntity blockEntity)) return;
        if (!blockEntity.beginSequence()) return;

        CheckpointKey key = new CheckpointKey(level.dimension(), pos.immutable());
        LATCHED_UNTIL_EXIT.add(key);
        beginDoorClosure(level, pos, state);
    }

    private static void beginDoorClosure(ServerLevel level, BlockPos pos,
            BlockState state) {
        DecontaminationStructure.nudgeOwnedDoors(level, pos, facing(state));
    }

    public static void tickActiveSequence(Level world, BlockPos pos,
            BlockState state, DecontaminationBlockEntity blockEntity,
            long activeElapsedTicks) {
        if (!(world instanceof ServerLevel level) || !blockEntity.isActive()) return;
        if (FacilityStructureBreakGuard.isBeingMined(level, pos)) return;

        Direction facing = facing(state);

        // Do not guess the heavy-door animation duration. Wait until both owned
        // BLACK_DOOR blocks have actually reached their fully-closed endpoint.
        if (!blockEntity.hasAnimationStarted()) {
            if (ownedDoorsFullyClosed(level, pos, facing)) {
                blockEntity.startAnimation();
            }
            return;
        }

        long animationElapsed = blockEntity.animationElapsedTicks();

        // The model begins moving as soon as the doors finish closing. Audio,
        // player effects and vapor deliberately enter half a second later.
        if (!blockEntity.hasEffectsStarted()
                && animationElapsed >= ANIMATION_EFFECTS_DELAY_TICKS) {
            if (blockEntity.startEffects()) {
                beginTimedEffects(level, pos, state);
            }
        }

        if (blockEntity.hasEffectsStarted()) {
            long processElapsed = blockEntity.sequenceElapsedTicks();

            if (processElapsed < SMOKE_EMISSION_END_TICK
                    && processElapsed % PARTICLE_INTERVAL_TICKS == 0L) {
                emitVentSmoke(level, pos, state, processElapsed);
            }

            // Door release remains 4.75 seconds after gas/audio start, not
            // after the player merely entered the checkpoint.
            if (processElapsed >= DecontaminationStructure.DOOR_RELEASE_TICK
                    && (processElapsed == DecontaminationStructure.DOOR_RELEASE_TICK
                    || processElapsed % 5L == 0L)) {
                DecontaminationStructure.nudgeOwnedDoors(level, pos, facing);
            }
        }

        if (animationElapsed < SEQUENCE_END_TICK) return;

        blockEntity.clearSequence();
        DecontaminationStructure.nudgeOwnedDoors(level, pos, facing);

        // Old worlds may still contain one of the transient controller blocks.
        // Normalize it only after the authored animation has completed.
        if (!state.is(ScpClassifiedDirectiveModBlocks.DECON_OPEN.get())) {
            level.setBlock(pos, copyCommonState(
                    ScpClassifiedDirectiveModBlocks.DECON_OPEN.get()
                            .defaultBlockState(), state), 3);
        }
    }

    private static boolean ownedDoorsFullyClosed(ServerLevel level,
            BlockPos controllerPos, Direction facing) {
        return level.getBlockState(DecontaminationStructure.entranceDoorPosition(
                        controllerPos, facing)).getBlock()
                == FacilityModule.BLACK_DOOR.closed().get()
                && level.getBlockState(DecontaminationStructure.exitDoorPosition(
                        controllerPos, facing)).getBlock()
                == FacilityModule.BLACK_DOOR.closed().get();
    }

    private static void beginTimedEffects(ServerLevel level, BlockPos pos,
            BlockState state) {
        Direction facing = facing(state);
        Vec3 soundPosition = DecontaminationStructure.chamberBox(pos, facing)
                .getCenter();
        level.playSound(null, soundPosition.x, soundPosition.y, soundPosition.z,
                ScpClassifiedDirectiveModSounds.DECONTAMINATION.get(),
                SoundSource.BLOCKS, 1.15F, 1.0F);

        for (ServerPlayer player : playersInside(level, pos, state)) {
            decontaminate(level, player);
        }
    }

    /** Compatibility hook retained for legacy scheduled ticks. */
    public static void finishClosed(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!DecontaminationStructure.isController(state)) return;
        if (level.getBlockEntity(pos)
                instanceof DecontaminationBlockEntity blockEntity) {
            blockEntity.clearSequence();
        }
        level.setBlock(pos, copyCommonState(
                ScpClassifiedDirectiveModBlocks.DECON_OPEN.get()
                        .defaultBlockState(), state), 3);
        DecontaminationStructure.nudgeOwnedDoors(level, pos, facing(state));
    }

    /** Compatibility hook retained for legacy DECON_OPEN_RELOAD states. */
    public static void finishReload(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (!state.is(ScpClassifiedDirectiveModBlocks.DECON_OPEN_RELOAD.get())) return;
        level.setBlock(pos, copyCommonState(
                ScpClassifiedDirectiveModBlocks.DECON_OPEN.get()
                        .defaultBlockState(), state), 3);
        DecontaminationStructure.nudgeOwnedDoors(level, pos, facing(state));
    }

    public static void forget(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        LATCHED_UNTIL_EXIT.remove(new CheckpointKey(
                level.dimension(), pos.immutable()));
    }

    /** Clears runtime-only latches after a MineZero/world restore. */
    public static void clearRuntimeState(ServerLevel level) {
        if (level == null) return;
        ResourceKey<Level> dimension = level.dimension();
        LATCHED_UNTIL_EXIT.removeIf(key -> key.dimension().equals(dimension));
    }

    // Kept private with this exact signature because the MineZero checkpoint
    // integration injects at this method's HEAD.
    private static void decontaminate(ServerLevel level, ServerPlayer player) {
        MobEffectInstance lubricatedEye = player.getEffect(
                ScpClassifiedDirectiveModMobEffects.LUBRICATED_EYE.get());
        player.removeAllEffects();
        if (lubricatedEye != null) {
            player.addEffect(new MobEffectInstance(lubricatedEye));
        }
        EyeProtectionAccess.applyExternalEyeSore(player, EYE_SORE_DURATION_TICKS);

        if (level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.DECONCHECKPOINT)) {
            player.setRespawnPosition(player.level().dimension(),
                    player.blockPosition(), player.getYRot(), true, false);
        }

        Advancement advancement = player.server.getAdvancements().getAdvancement(
                new ResourceLocation("scp_classified_directive",
                        "decon_achievement"));
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancements()
                .getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    private static List<ServerPlayer> playersInside(ServerLevel level,
            BlockPos pos, BlockState state) {
        AABB authored = DecontaminationStructure.chamberBox(pos, facing(state));
        // Keep the horizontal trigger exactly on the chamber interior, but make
        // vertical presence tolerant enough that jumping never counts as an
        // exit/re-entry. Using the player's whole bounding box also avoids the
        // old one-tick gap caused by testing only its centre point.
        AABB presence = new AABB(authored.minX, authored.minY - 0.25D,
                authored.minZ, authored.maxX, authored.maxY + 1.25D,
                authored.maxZ);
        return level.getEntitiesOfClass(ServerPlayer.class, presence,
                player -> player.isAlive() && !player.isRemoved()
                        && !player.isSpectator()
                        && presence.intersects(player.getBoundingBox()));
    }

    private static void emitVentSmoke(ServerLevel level, BlockPos pos,
            BlockState state, long elapsedTicks) {
        float strength = smokeStrength(elapsedTicks);
        if (strength <= 0.0F) return;
        Direction facing = facing(state);

        emitVent(level, pos, facing, strength,
                VENT_1_X, VENT_1_Y, VENT_1_Z,
                7.4D, 7.1D, false);
        emitVent(level, pos, facing, strength,
                0.0D, FLOOR_VENT_Y, VENT_2_Z,
                8.0D, 8.0D, true);
        emitVent(level, pos, facing, strength,
                0.0D, FLOOR_VENT_Y, VENT_3_Z,
                8.0D, 8.0D, true);
    }

    private static void emitVent(ServerLevel level, BlockPos controllerPos,
            Direction facing, float strength, double centerX, double modelY,
            double centerZ, double spreadX, double spreadZ, boolean upward) {
        for (int particle = 0; particle < PARTICLES_PER_VENT; particle++) {
            if (level.random.nextFloat() > strength) continue;

            double modelX = centerX
                    + (level.random.nextDouble() - 0.5D) * spreadX * 2.0D;
            double modelZ = centerZ
                    + (level.random.nextDouble() - 0.5D) * spreadZ * 2.0D;
            Vec3 origin = DecontaminationStructure.modelPointToWorld(
                    controllerPos, facing, modelX, modelY, modelZ);

            double localX = (level.random.nextDouble() - 0.5D) * 0.012D;
            double localZ = (level.random.nextDouble() - 0.5D) * 0.012D;
            double velocityY = upward
                    ? 0.085D + level.random.nextDouble() * 0.030D
                    : -(0.045D + level.random.nextDouble() * 0.020D);
            Vec3 velocity = rotateLocalVector(facing,
                    localX, velocityY, localZ);

            level.sendParticles(
                    ScpClassifiedDirectiveModParticleTypes.DECONTAMINATION_GAS.get(),
                    origin.x, origin.y, origin.z, 0,
                    velocity.x, velocity.y, velocity.z, 1.0D);
        }
    }

    private static float smokeStrength(long elapsedTicks) {
        if (elapsedTicks < SMOKE_FADE_START_TICK) return 1.0F;
        if (elapsedTicks >= SMOKE_EMISSION_END_TICK) return 0.0F;
        return Mth.clamp((SMOKE_EMISSION_END_TICK - elapsedTicks)
                / (float) (SMOKE_EMISSION_END_TICK - SMOKE_FADE_START_TICK),
                0.0F, 1.0F);
    }

    private static Vec3 rotateLocalVector(Direction facing,
            double localX, double y, double localZ) {
        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();
        return new Vec3(
                right.getStepX() * localX + forward.getStepX() * localZ,
                y,
                right.getStepZ() * localX + forward.getStepZ() * localZ);
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
    }

    private static BlockState copyCommonState(BlockState target,
            BlockState source) {
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
