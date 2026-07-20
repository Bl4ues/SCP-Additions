package net.mcreator.scpadditions.facility;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contextual proof of concept for SCP-079 facility control.
 *
 * SCP-079 can manipulate only the three redstone heavy-door families and only
 * through a connected functional button, keycard reader, or existing legacy
 * Facility Pulse Node. Locked buttons, ordinary redstone and bare doors do not
 * grant access.
 */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.GAME)
public final class Scp079FacilityThreatEvents {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int FLEE_DOOR_RADIUS = 7;
    private static final int PURSUER_DOOR_RADIUS = 6;
    private static final int PURSUER_SEARCH_RADIUS = 14;
    private static final int DOOR_COOLDOWN_TICKS = 600;
    private static final int MIN_GLOBAL_COOLDOWN_TICKS = 160;
    private static final int GLOBAL_COOLDOWN_VARIANCE_TICKS = 140;
    private static final int MIN_EVALUATION_COOLDOWN_TICKS = 100;
    private static final int EVALUATION_COOLDOWN_VARIANCE_TICKS = 100;

    private static final float CLOSE_AHEAD_CHANCE = 0.20F;
    private static final float UNPROVOKED_CLOSE_CHANCE = 0.03F;
    private static final float OPEN_FOR_THREAT_CHANCE = 0.30F;

    private static final Map<ResourceKey<Level>, Long> NEXT_ACTION_TIME =
            new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Long> NEXT_EVALUATION_TIME =
            new ConcurrentHashMap<>();
    private static final Map<DoorKey, Long> DOOR_COOLDOWNS =
            new ConcurrentHashMap<>();

    private Scp079FacilityThreatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || !player.isAlive()
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        if ((gameTime + player.getId()) % CHECK_INTERVAL_TICKS != 0L) {
            return;
        }
        if (!level.getGameRules().getBoolean(ScpAdditionsModGameRules.SCP079CONTROLON)) {
            return;
        }
        if (NEXT_ACTION_TIME.getOrDefault(level.dimension(), 0L) > gameTime) {
            return;
        }
        if (NEXT_EVALUATION_TIME.getOrDefault(level.dimension(), 0L) > gameTime) {
            return;
        }

        RandomSource random = level.getRandom();
        NEXT_EVALUATION_TIME.put(level.dimension(), gameTime
                + MIN_EVALUATION_COOLDOWN_TICKS
                + random.nextInt(EVALUATION_COOLDOWN_VARIANCE_TICKS + 1));

        if (gameTime % 600L == 0L) {
            DOOR_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
        }

        List<Mob> pursuers = level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(PURSUER_SEARCH_RADIUS),
                mob -> mob.isAlive() && mob.getTarget() == player);

        if (!pursuers.isEmpty()) {
            DoorMatch doorAhead = findOpenDoorAhead(level, player, pursuers,
                    gameTime, true);
            if (doorAhead != null && random.nextFloat() < CLOSE_AHEAD_CHANCE
                    && forceClosed(level, doorAhead)) {
                recordAction(level, doorAhead.pos(), gameTime, random);
                return;
            }

            DoorMatch doorForThreat = findClosedDoorForThreat(level, player,
                    pursuers, gameTime);
            if (doorForThreat != null
                    && random.nextFloat() < OPEN_FOR_THREAT_CHANCE
                    && forceOpen(level, doorForThreat)) {
                recordAction(level, doorForThreat.pos(), gameTime, random);
            }
            return;
        }

        // Low-probability CB-style harassment even when nothing is chasing the
        // player. Movement toward an open door is required, so standing near or
        // merely looking at one does not repeatedly roll the chance.
        DoorMatch unprovokedDoor = findOpenDoorAhead(level, player, List.of(),
                gameTime, false);
        if (unprovokedDoor != null
                && random.nextFloat() < UNPROVOKED_CLOSE_CHANCE
                && forceClosed(level, unprovokedDoor)) {
            recordAction(level, unprovokedDoor.pos(), gameTime, random);
        }
    }

    private static DoorMatch findOpenDoorAhead(ServerLevel level,
            ServerPlayer player, List<Mob> pursuers, long gameTime,
            boolean requirePursuerBehind) {
        if (requirePursuerBehind && !player.isSprinting()) {
            return null;
        }

        Vec3 travel = horizontal(player.getDeltaMovement());
        if (travel.lengthSqr() < 0.0025D) {
            if (!requirePursuerBehind) {
                return null;
            }
            travel = horizontal(player.getLookAngle());
        }
        if (travel.lengthSqr() < 0.0001D) {
            return null;
        }
        Vec3 travelDirection = travel.normalize();

        if (requirePursuerBehind) {
            boolean threatBehind = pursuers.stream().anyMatch(mob -> {
                Vec3 fromThreatToPlayer = horizontal(
                        player.position().subtract(mob.position()));
                return fromThreatToPlayer.lengthSqr() > 0.0001D
                        && fromThreatToPlayer.normalize().dot(travelDirection) > 0.20D;
            });
            if (!threatBehind) {
                return null;
            }
        }

        DoorMatch best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos center = player.blockPosition();

        for (BlockPos mutable : BlockPos.betweenClosed(
                center.offset(-FLEE_DOOR_RADIUS, -2, -FLEE_DOOR_RADIUS),
                center.offset(FLEE_DOOR_RADIUS, 3, FLEE_DOOR_RADIUS))) {
            BlockPos pos = mutable.immutable();
            DoorMatch match = matchDoor(level, pos);
            if (match == null || match.stage() != DoorStage.OPEN
                    || onCooldown(level, pos, gameTime)
                    || !HeavyDoorControlPanelAccess.hasControllableInterface(level, pos)) {
                continue;
            }

            Vec3 horizontalToDoor = horizontal(
                    Vec3.atCenterOf(pos).subtract(player.position()));
            double distance = horizontalToDoor.lengthSqr();
            if (distance < 0.25D
                    || distance > FLEE_DOOR_RADIUS * FLEE_DOOR_RADIUS
                    || horizontalToDoor.normalize().dot(travelDirection) < 0.45D) {
                continue;
            }

            if (distance < bestDistance) {
                best = match;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static DoorMatch findClosedDoorForThreat(ServerLevel level,
            ServerPlayer player, List<Mob> pursuers, long gameTime) {
        DoorMatch best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Mob pursuer : pursuers) {
            BlockPos center = pursuer.blockPosition();
            for (BlockPos mutable : BlockPos.betweenClosed(
                    center.offset(-PURSUER_DOOR_RADIUS, -2, -PURSUER_DOOR_RADIUS),
                    center.offset(PURSUER_DOOR_RADIUS, 3, PURSUER_DOOR_RADIUS))) {
                BlockPos pos = mutable.immutable();
                DoorMatch match = matchDoor(level, pos);
                if (match == null || match.stage() != DoorStage.CLOSED
                        || onCooldown(level, pos, gameTime)
                        || !HeavyDoorControlPanelAccess.hasControllableInterface(level, pos)
                        || !oppositeSides(match.state(), pos,
                                pursuer.position(), player.position())) {
                    continue;
                }

                double distance = pursuer.distanceToSqr(Vec3.atCenterOf(pos));
                if (distance < bestDistance) {
                    best = match;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static boolean oppositeSides(BlockState doorState, BlockPos doorPos,
            Vec3 threatPosition, Vec3 playerPosition) {
        if (!doorState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false;
        }

        Direction facing = doorState.getValue(HorizontalDirectionalBlock.FACING);
        Vec3 center = Vec3.atCenterOf(doorPos);
        double threatSide = (threatPosition.x - center.x) * facing.getStepX()
                + (threatPosition.z - center.z) * facing.getStepZ();
        double playerSide = (playerPosition.x - center.x) * facing.getStepX()
                + (playerPosition.z - center.z) * facing.getStepZ();
        return threatSide * playerSide < -0.35D;
    }

    private static boolean forceOpen(ServerLevel level, DoorMatch match) {
        BlockState current = level.getBlockState(match.pos());
        DoorMatch fresh = matchDoor(level, match.pos());
        if (fresh == null || fresh.stage() != DoorStage.CLOSED
                || !current.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false;
        }

        int controlCount = HeavyDoorControlPanelAccess.openConnectedControls(
                level, match.pos());
        if (controlCount <= 0) {
            return false;
        }

        Direction facing = current.getValue(HorizontalDirectionalBlock.FACING);
        Block target = fresh.family().opening().get(0).get();
        level.playSound(null, match.pos(), fresh.family().openingSound().get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(match.pos(), target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);
        emitOverrideParticles(level, match.pos());
        return true;
    }

    private static boolean forceClosed(ServerLevel level, DoorMatch match) {
        BlockState current = level.getBlockState(match.pos());
        DoorMatch fresh = matchDoor(level, match.pos());
        if (fresh == null || fresh.stage() != DoorStage.OPEN
                || !current.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false;
        }

        int controlCount = HeavyDoorControlPanelAccess.closeConnectedControls(
                level, match.pos());
        if (controlCount <= 0) {
            return false;
        }

        // Do not visually close the connected panel while an unrelated lever or
        // other ordinary redstone source is still forcing the door open.
        if (doorPowered(level, match.pos())) {
            HeavyDoorControlPanelAccess.openConnectedControls(level, match.pos());
            return false;
        }

        Direction facing = current.getValue(HorizontalDirectionalBlock.FACING);
        Block target = fresh.family().closing().get(0).get();
        level.playSound(null, match.pos(), fresh.family().closingSound().get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(match.pos(), target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);
        emitOverrideParticles(level, match.pos());
        return true;
    }

    private static boolean doorPowered(ServerLevel level, BlockPos doorPos) {
        return level.hasNeighborSignal(doorPos)
                || level.hasNeighborSignal(doorPos.above())
                || level.hasNeighborSignal(doorPos.above(2));
    }

    private static DoorMatch matchDoor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        FacilityModule.DoorFamily[] families = {
                FacilityModule.DEFAULT_DOOR,
                FacilityModule.YELLOW_DOOR,
                FacilityModule.BLACK_DOOR
        };

        for (FacilityModule.DoorFamily family : families) {
            if (block == family.closed().get()) {
                return new DoorMatch(pos, state, family, DoorStage.CLOSED);
            }
            if (block == family.open().get()) {
                return new DoorMatch(pos, state, family, DoorStage.OPEN);
            }
            if (family.opening().stream().anyMatch(entry -> entry.get() == block)) {
                return new DoorMatch(pos, state, family, DoorStage.OPENING);
            }
            if (family.closing().stream().anyMatch(entry -> entry.get() == block)) {
                return new DoorMatch(pos, state, family, DoorStage.CLOSING);
            }
        }
        return null;
    }

    private static boolean onCooldown(ServerLevel level, BlockPos pos,
            long gameTime) {
        return DOOR_COOLDOWNS.getOrDefault(
                new DoorKey(level.dimension(), pos.asLong()), 0L) > gameTime;
    }

    private static void recordAction(ServerLevel level, BlockPos pos,
            long gameTime, RandomSource random) {
        NEXT_ACTION_TIME.put(level.dimension(), gameTime
                + MIN_GLOBAL_COOLDOWN_TICKS
                + random.nextInt(GLOBAL_COOLDOWN_VARIANCE_TICKS + 1));
        DOOR_COOLDOWNS.put(new DoorKey(level.dimension(), pos.asLong()),
                gameTime + DOOR_COOLDOWN_TICKS);
    }

    private static void emitOverrideParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                8, 0.45D, 0.55D, 0.45D, 0.03D);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                2, 0.35D, 0.30D, 0.35D, 0.01D);
    }

    private static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0D, vector.z);
    }

    private enum DoorStage {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    private record DoorMatch(BlockPos pos, BlockState state,
            FacilityModule.DoorFamily family, DoorStage stage) {
    }

    private record DoorKey(ResourceKey<Level> dimension, long pos) {
    }
}
