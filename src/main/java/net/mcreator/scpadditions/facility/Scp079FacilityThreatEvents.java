package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contextual SCP-079 facility control driven by a shared processing budget.
 *
 * There is deliberately no facility-wide action cooldown. Different useful
 * devices may be manipulated in quick succession while processing remains,
 * whereas each individual door receives a short reuse limit to prevent a
 * mechanical open/close loop. SCP-012 owns its separate contest logic.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079FacilityThreatEvents {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int UNPROVOKED_INTERVAL_TICKS = 100;
    private static final int FLEE_DOOR_RADIUS = 7;
    private static final int PURSUER_DOOR_RADIUS = 6;
    private static final int PURSUER_SEARCH_RADIUS = 14;
    private static final int DOOR_REUSE_TICKS = 100;
    private static final int LOCKED_DOOR_REUSE_TICKS = 140;

    private static final double OPEN_FOR_THREAT_COST = 8.0D;
    private static final double CLOSE_AHEAD_COST = 8.0D;
    private static final double DENY_ACCESS_COST = 12.0D;
    private static final double UNPROVOKED_COST = 6.0D;
    private static final float UNPROVOKED_CLOSE_CHANCE = 0.03F;

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
        if ((gameTime + player.getId()) % CHECK_INTERVAL_TICKS != 0L
                || !level.getGameRules().getBoolean(
                ScpAdditionsModGameRules.SCP079CONTROLON)) {
            return;
        }

        float availablePower = Scp079ProcessingManager.getPower(level);
        if (availablePower < UNPROVOKED_COST) return;
        cleanCooldowns(level, gameTime);

        List<Mob> pursuers = level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(PURSUER_SEARCH_RADIUS),
                mob -> mob.isAlive() && mob.getTarget() == player);
        Mob pursuer = pursuers.stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);

        if (pursuer != null) {
            evaluatePursuit(level, player, pursuer, gameTime,
                    availablePower);
            return;
        }

        if ((gameTime + player.getId()) % UNPROVOKED_INTERVAL_TICKS != 0L
                || availablePower < 60.0F) {
            return;
        }

        AheadDoors ahead = findDoorsAhead(level, player, null, gameTime,
                false);
        if (ahead.open() != null
                && level.getRandom().nextFloat() < UNPROVOKED_CLOSE_CHANCE) {
            execute(level, new Action(ActionType.CLOSE,
                    ahead.open(), 40.0D, UNPROVOKED_COST, 0), gameTime,
                    player, null);
        }
    }

    private static void evaluatePursuit(ServerLevel level, ServerPlayer player,
            Mob pursuer, long gameTime, float availablePower) {
        ThreatProfile profile = ThreatProfile.forMob(pursuer);
        AheadDoors ahead = findDoorsAhead(level, player, pursuer, gameTime,
                true);
        DoorMatch threatDoor = profile.canOpenDoors()
                ? findClosedDoorForThreat(level, player, pursuer, gameTime)
                : null;

        RandomSource random = level.getRandom();
        List<Action> candidates = new ArrayList<>(3);

        if (ahead.closed() != null && profile.canDenyAccess()
                && HeavyDoorControlPanelAccess.hasDeniableInterface(level,
                ahead.closed().pos())
                && pursuer.distanceTo(player) >= profile.minimumLockDistance()
                && random.nextFloat() < profile.denyChance()) {
            double commitment = Math.max(0.0D,
                    8.0D - Math.sqrt(player.distanceToSqr(
                            Vec3.atCenterOf(ahead.closed().pos()))));
            candidates.add(new Action(ActionType.DENY, ahead.closed(),
                    78.0D + commitment, DENY_ACCESS_COST,
                    profile.lockDurationTicks()));
        }

        if (ahead.open() != null
                && random.nextFloat() < profile.closeAheadChance()) {
            double commitment = Math.max(0.0D,
                    7.0D - Math.sqrt(player.distanceToSqr(
                            Vec3.atCenterOf(ahead.open().pos()))));
            candidates.add(new Action(ActionType.CLOSE, ahead.open(),
                    72.0D + commitment, CLOSE_AHEAD_COST, 0));
        }

        if (threatDoor != null
                && random.nextFloat() < profile.openForThreatChance()) {
            double proximity = Math.max(0.0D,
                    7.0D - Math.sqrt(pursuer.distanceToSqr(
                            Vec3.atCenterOf(threatDoor.pos()))));
            candidates.add(new Action(ActionType.OPEN, threatDoor,
                    70.0D + proximity * 1.6D,
                    OPEN_FOR_THREAT_COST, 0));
        }

        if (candidates.isEmpty()) return;
        candidates.removeIf(action -> !Scp079ProcessingManager.canAfford(
                level, action.cost()));
        if (candidates.isEmpty()) return;

        Action selected = candidates.stream()
                .max(Comparator.comparingDouble(action -> adjustedUtility(
                        action, availablePower)))
                .orElse(null);
        if (selected == null
                || adjustedUtility(selected, availablePower) < 52.0D) {
            return;
        }
        execute(level, selected, gameTime, player, pursuer);
    }

    private static double adjustedUtility(Action action, float availablePower) {
        double utility = action.utility();
        if (availablePower < 30.0F) utility -= action.cost() * 0.9D;
        if (availablePower < 15.0F) utility -= action.cost() * 1.2D;
        return utility;
    }

    private static boolean execute(ServerLevel level, Action action,
            long gameTime, ServerPlayer player, Mob pursuer) {
        if (!Scp079ProcessingManager.trySpend(level, action.cost())) {
            return false;
        }

        boolean success = switch (action.type()) {
            case OPEN -> forceOpen(level, action.door());
            case CLOSE -> forceClosed(level, action.door());
            case DENY -> denyAccess(level, action.door(),
                    action.durationTicks());
        };
        if (!success) {
            Scp079ProcessingManager.refund(level, action.cost());
            Scp079DecisionLog.record(level,
                    Scp079DecisionLog.DecisionType.ABORTED_ACTION,
                    Scp079DecisionLog.DecisionOutcome.ABORTED,
                    action.door().pos(), 0.0D,
                    "door state changed before execution · processing refunded");
            return false;
        }

        int reuse = action.type() == ActionType.DENY
                ? LOCKED_DOOR_REUSE_TICKS : DOOR_REUSE_TICKS;
        DOOR_COOLDOWNS.put(new DoorKey(level.dimension(),
                action.door().pos().asLong()), gameTime + reuse);
        Scp079DecisionLog.record(level, decisionType(action.type()),
                Scp079DecisionLog.DecisionOutcome.EXECUTED,
                action.door().pos(), action.cost(),
                decisionContext(action, player, pursuer));
        return true;
    }

    private static Scp079DecisionLog.DecisionType decisionType(
            ActionType type) {
        return switch (type) {
            case OPEN -> Scp079DecisionLog.DecisionType.OPEN_DOOR;
            case CLOSE -> Scp079DecisionLog.DecisionType.CLOSE_DOOR;
            case DENY -> Scp079DecisionLog.DecisionType.DENY_ACCESS;
        };
    }

    private static String decisionContext(Action action,
            ServerPlayer player, Mob pursuer) {
        String playerName = player == null ? "player"
                : player.getGameProfile().getName();
        String threat = pursuer == null ? ""
                : pursuer.getDisplayName().getString();
        return switch (action.type()) {
            case OPEN -> "for " + threat + " pursuing " + playerName;
            case CLOSE -> pursuer == null
                    ? "unprovoked near " + playerName
                    : "ahead of " + playerName + " fleeing " + threat;
            case DENY -> "against " + playerName + " fleeing " + threat
                    + " · " + action.durationTicks() / 20.0D + "s";
        };
    }

    private static AheadDoors findDoorsAhead(ServerLevel level,
            ServerPlayer player, Mob pursuer, long gameTime,
            boolean requirePursuerBehind) {
        if (requirePursuerBehind && !player.isSprinting()) {
            return AheadDoors.EMPTY;
        }

        Vec3 travel = horizontal(player.getDeltaMovement());
        if (travel.lengthSqr() < 0.0025D) {
            if (!requirePursuerBehind) return AheadDoors.EMPTY;
            travel = horizontal(player.getLookAngle());
        }
        if (travel.lengthSqr() < 0.0001D) return AheadDoors.EMPTY;
        Vec3 direction = travel.normalize();

        if (requirePursuerBehind && pursuer != null) {
            Vec3 fromThreatToPlayer = horizontal(
                    player.position().subtract(pursuer.position()));
            if (fromThreatToPlayer.lengthSqr() < 0.0001D
                    || fromThreatToPlayer.normalize().dot(direction) <= 0.20D) {
                return AheadDoors.EMPTY;
            }
        }

        DoorMatch open = null;
        DoorMatch closed = null;
        double openDistance = Double.MAX_VALUE;
        double closedDistance = Double.MAX_VALUE;
        Set<Long> visited = new HashSet<>();

        for (int step = 1; step <= FLEE_DOOR_RADIUS; step++) {
            Vec3 sample = player.position().add(direction.scale(step));
            BlockPos center = BlockPos.containing(sample);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -2; dy <= 3; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!visited.add(pos.asLong())
                                || onCooldown(level, pos, gameTime)) {
                            continue;
                        }
                        DoorMatch match = matchDoor(level, pos);
                        if (match == null
                                || !HeavyDoorControlPanelAccess
                                .hasControllableInterface(level, pos)) {
                            continue;
                        }

                        Vec3 toDoor = horizontal(Vec3.atCenterOf(pos)
                                .subtract(player.position()));
                        double distance = toDoor.lengthSqr();
                        if (distance < 0.25D
                                || distance > FLEE_DOOR_RADIUS
                                * FLEE_DOOR_RADIUS
                                || toDoor.normalize().dot(direction) < 0.45D) {
                            continue;
                        }

                        if (match.stage() == DoorStage.OPEN
                                && distance < openDistance) {
                            open = match;
                            openDistance = distance;
                        } else if (match.stage() == DoorStage.CLOSED
                                && distance < closedDistance) {
                            closed = match;
                            closedDistance = distance;
                        }
                    }
                }
            }
        }
        return new AheadDoors(open, closed);
    }

    private static DoorMatch findClosedDoorForThreat(ServerLevel level,
            ServerPlayer player, Mob pursuer, long gameTime) {
        Vec3 route = horizontal(player.position().subtract(pursuer.position()));
        if (route.lengthSqr() < 0.25D) return null;
        Vec3 direction = route.normalize();
        int steps = Math.min(PURSUER_DOOR_RADIUS,
                Math.max(1, (int) Math.ceil(Math.sqrt(route.lengthSqr()))));

        DoorMatch best = null;
        double bestDistance = Double.MAX_VALUE;
        Set<Long> visited = new HashSet<>();
        for (int step = 1; step <= steps; step++) {
            BlockPos center = BlockPos.containing(
                    pursuer.position().add(direction.scale(step)));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -2; dy <= 3; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!visited.add(pos.asLong())
                                || onCooldown(level, pos, gameTime)) {
                            continue;
                        }
                        DoorMatch match = matchDoor(level, pos);
                        if (match == null || match.stage() != DoorStage.CLOSED
                                || !HeavyDoorControlPanelAccess
                                .hasControllableInterface(level, pos)
                                || !oppositeSides(match.state(), pos,
                                pursuer.position(), player.position())) {
                            continue;
                        }
                        double distance = pursuer.distanceToSqr(
                                Vec3.atCenterOf(pos));
                        if (distance < bestDistance) {
                            best = match;
                            bestDistance = distance;
                        }
                    }
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

    private static boolean denyAccess(ServerLevel level, DoorMatch match,
            int durationTicks) {
        DoorMatch fresh = matchDoor(level, match.pos());
        if (fresh == null || fresh.stage() != DoorStage.CLOSED) return false;
        int changed = HeavyDoorControlPanelAccess
                .temporarilyDenyConnectedControls(level, match.pos(),
                        durationTicks);
        if (changed <= 0) return false;
        emitOverrideParticles(level, match.pos());
        return true;
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
        if (controlCount <= 0) return false;

        Direction facing = current.getValue(HorizontalDirectionalBlock.FACING);
        Block target = fresh.family().opening().get(0).get();
        level.playSound(null, match.pos(), fresh.family().openingSound().get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(match.pos(), target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing),
                Block.UPDATE_ALL);
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
        if (controlCount <= 0) return false;

        if (doorPowered(level, match.pos())) {
            HeavyDoorControlPanelAccess.openConnectedControls(level, match.pos());
            return false;
        }

        Direction facing = current.getValue(HorizontalDirectionalBlock.FACING);
        Block target = fresh.family().closing().get(0).get();
        level.playSound(null, match.pos(), fresh.family().closingSound().get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(match.pos(), target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing),
                Block.UPDATE_ALL);
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

    private static void cleanCooldowns(ServerLevel level, long gameTime) {
        if (gameTime % 600L != 0L) return;
        DOOR_COOLDOWNS.entrySet().removeIf(entry ->
                entry.getKey().dimension().equals(level.dimension())
                        && entry.getValue() <= gameTime);
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

    private enum ActionType {
        OPEN,
        CLOSE,
        DENY
    }

    private record Action(ActionType type, DoorMatch door, double utility,
                           double cost, int durationTicks) {
    }

    private record AheadDoors(DoorMatch open, DoorMatch closed) {
        private static final AheadDoors EMPTY = new AheadDoors(null, null);
    }

    private record DoorMatch(BlockPos pos, BlockState state,
            FacilityModule.DoorFamily family, DoorStage stage) {
    }

    private record DoorKey(ResourceKey<Level> dimension, long pos) {
    }

    private record ThreatProfile(boolean canOpenDoors,
                                 boolean canDenyAccess,
                                 float openForThreatChance,
                                 float closeAheadChance,
                                 float denyChance,
                                 int lockDurationTicks,
                                 double minimumLockDistance) {
        private static final ThreatProfile DEFAULT = new ThreatProfile(
                true, true, 0.30F, 0.20F, 0.10F, 60, 5.0D);

        private static ThreatProfile forMob(Mob mob) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(
                    mob.getType());
            if (id == null) return DEFAULT;
            return switch (id.getPath()) {
                case "scp_173" -> new ThreatProfile(true, true,
                        0.36F, 0.22F, 0.06F, 40, 8.0D);
                case "scp_106" -> new ThreatProfile(false, true,
                        0.0F, 0.24F, 0.04F, 35, 10.0D);
                default -> DEFAULT;
            };
        }
    }
}
