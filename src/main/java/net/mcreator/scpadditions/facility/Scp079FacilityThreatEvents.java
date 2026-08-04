package net.mcreator.scpadditions.facility;

import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contextual SCP-079 facility control driven by a shared processing budget.
 *
 * Tactical actions are deterministic once their physical and strategic
 * conditions are satisfied. Randomness remains only for harmlessly rare,
 * unprovoked harassment, where certainty would be repetitive rather than
 * intelligent. Different useful devices may be manipulated in the same
 * evaluation while processing remains, but each tactical lane and each door
 * still has an anti-spam limit.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079FacilityThreatEvents {
    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final int UNPROVOKED_INTERVAL_TICKS = 100;
    private static final int FLEE_DOOR_RADIUS = 12;
    private static final int PURSUER_DOOR_RADIUS = 10;
    private static final int PURSUER_SEARCH_RADIUS = 24;
    private static final int SCP_106_PURSUER_SEARCH_RADIUS = 36;
    private static final int DOOR_REUSE_TICKS = 60;
    private static final int CLOSE_FOLLOWUP_REUSE_TICKS = 24;
    private static final int LOCKED_DOOR_REUSE_TICKS = 100;
    private static final int RECENT_TRAVEL_TICKS = 30;
    private static final int SCP_131_SEPARATION_RADIUS = 16;
    private static final int SCP_131_DOOR_RADIUS = 7;
    private static final int SCP_131_INITIAL_LOCK_TICKS = 40;
    private static final int SCP_131_MAX_LOCK_TICKS = 160;
    private static final int MAX_TACTICAL_ACTIONS = 2;

    private static final double MOVEMENT_THRESHOLD_SQR = 0.0004D;
    private static final double OPEN_FOR_THREAT_COST = 8.0D;
    private static final double CLOSE_AHEAD_COST = 8.0D;
    private static final double DENY_ACCESS_COST = 12.0D;
    private static final double UNPROVOKED_COST = 6.0D;
    private static final double SCP_131_SEPARATION_COST =
            CLOSE_AHEAD_COST + DENY_ACCESS_COST;
    private static final float UNPROVOKED_CLOSE_CHANCE = 0.03F;
    private static final float UNPROVOKED_MINIMUM_POWER = 75.0F;

    private static final Map<DoorKey, Long> DOOR_COOLDOWNS =
            new ConcurrentHashMap<>();
    private static final Map<UUID, TravelMemory> RECENT_TRAVEL =
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
        rememberTravel(player, gameTime);
        if ((gameTime + player.getId()) % CHECK_INTERVAL_TICKS != 0L
                || !Scp079FacilityAccessManager.hasFacilityAccess(level)) {
            return;
        }

        float availablePower = Scp079ProcessingManager.getPower(level);
        if (availablePower < UNPROVOKED_COST) return;
        cleanCooldowns(level, gameTime);

        if (availablePower >= SCP_131_SEPARATION_COST
                && trySeparateScp131(level, player, gameTime)) {
            return;
        }

        Mob pursuer = findBestPursuer(level, player);
        if (pursuer != null) {
            evaluatePursuit(level, player, pursuer, gameTime,
                    availablePower);
            return;
        }

        evaluateUnprovokedPressure(level, player, gameTime, availablePower);
    }

    private static Mob findBestPursuer(ServerLevel level,
            ServerPlayer player) {
        List<Mob> pursuers = new ArrayList<>(level.getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(PURSUER_SEARCH_RADIUS),
                mob -> mob.isAlive() && mob.getTarget() == player));
        for (Scp106Entity scp106 : level.getEntitiesOfClass(
                Scp106Entity.class,
                player.getBoundingBox().inflate(
                        SCP_106_PURSUER_SEARCH_RADIUS),
                entity -> entity.isAlive()
                        && (entity.getTarget() == player
                        || entity.isHuntingPlayer(player)))) {
            if (!pursuers.contains(scp106)) pursuers.add(scp106);
        }

        return pursuers.stream()
                .max(Comparator.comparingDouble(pursuer ->
                        threatSelectionScore(player, pursuer)))
                .orElse(null);
    }

    private static double threatSelectionScore(ServerPlayer player,
            Mob pursuer) {
        ThreatProfile profile = ThreatProfile.forMob(pursuer);
        return profile.threatPriority()
                - Math.sqrt(player.distanceToSqr(pursuer)) * 1.5D;
    }

    private static void evaluateUnprovokedPressure(ServerLevel level,
            ServerPlayer player, long gameTime, float availablePower) {
        if ((gameTime + player.getId()) % UNPROVOKED_INTERVAL_TICKS != 0L
                || availablePower < UNPROVOKED_MINIMUM_POWER) {
            return;
        }

        AheadDoors ahead = findDoorsAhead(level, player, null, gameTime,
                false);
        if (ahead.open() != null
                && level.getRandom().nextFloat() < UNPROVOKED_CLOSE_CHANCE) {
            execute(level, new Action(ActionType.CLOSE,
                    ahead.open(), 40.0D, UNPROVOKED_COST, 0, 0), gameTime,
                    player, null);
        }
    }

    private static boolean trySeparateScp131(ServerLevel level,
            ServerPlayer player, long gameTime) {
        List<AbstractScp131Entity> followers = level.getEntitiesOfClass(
                AbstractScp131Entity.class,
                player.getBoundingBox().inflate(SCP_131_SEPARATION_RADIUS),
                entity -> entity.isAlive()
                        && entity.isFollowingPlayer(player));
        if (followers.isEmpty()) return false;

        Vec3 travel = observedTravel(player);
        if (travel.lengthSqr() < MOVEMENT_THRESHOLD_SQR) return false;
        Vec3 direction = travel.normalize();

        Scp173Entity scp173 = findThreateningScp173Ahead(level, player,
                followers, direction);
        if (scp173 == null) return false;

        SeparationOpportunity opportunity = findScp131SeparationDoor(level,
                player, followers, scp173, direction, gameTime);
        if (opportunity == null
                || !Scp079ProcessingManager.trySpend(level,
                SCP_131_SEPARATION_COST)) {
            return false;
        }

        if (!forceClosed(level, opportunity.door())) {
            Scp079ProcessingManager.refund(level,
                    SCP_131_SEPARATION_COST);
            Scp079DecisionLog.record(level,
                    Scp079DecisionLog.DecisionType.ABORTED_ACTION,
                    Scp079DecisionLog.DecisionOutcome.ABORTED,
                    opportunity.door().pos(), 0.0D,
                    "SCP-131 separation door changed · processing refunded");
            return false;
        }

        int denied = HeavyDoorControlPanelAccess
                .temporarilyDenyConnectedControls(level,
                opportunity.door().pos(), SCP_131_INITIAL_LOCK_TICKS);
        if (denied <= 0) {
            Scp079ProcessingManager.refund(level, DENY_ACCESS_COST);
            DOOR_COOLDOWNS.put(new DoorKey(level.dimension(),
                    opportunity.door().pos().asLong()),
                    gameTime + DOOR_REUSE_TICKS);
            Scp079DecisionLog.record(level,
                    Scp079DecisionLog.DecisionType.CLOSE_DOOR,
                    Scp079DecisionLog.DecisionOutcome.EXECUTED,
                    opportunity.door().pos(), CLOSE_AHEAD_COST,
                    "attempted SCP-131 separation but could not lock controls");
            return true;
        }

        boolean dismissed = AbstractScp131Entity.stopFollowersFor(player);
        if (dismissed) {
            ScpEntityNetwork.showScp131Notice(player, false);
        }
        DOOR_COOLDOWNS.put(new DoorKey(level.dimension(),
                opportunity.door().pos().asLong()),
                gameTime + LOCKED_DOOR_REUSE_TICKS);
        Scp079SustainedDoorLocks.begin(level, opportunity.door().pos(),
                player.getUUID(), opportunity.follower().getUUID(),
                scp173.getUUID(),
                Scp079SustainedDoorLocks.LockReason.SCP_131_SEPARATION,
                SCP_131_MAX_LOCK_TICKS);
        Scp079DecisionLog.record(level,
                Scp079DecisionLog.DecisionType.SEPARATE_SCP_131,
                Scp079DecisionLog.DecisionOutcome.EXECUTED,
                opportunity.door().pos(), SCP_131_SEPARATION_COST,
                "closed SCP-173 ahead away from "
                        + player.getGameProfile().getName()
                        + " · lock upkeep 1.5 AP/s");
        Scp079FacilityAccessManager.awardFirstInterference(player);
        return true;
    }

    private static Scp173Entity findThreateningScp173Ahead(ServerLevel level,
            ServerPlayer player, List<AbstractScp131Entity> followers,
            Vec3 direction) {
        Scp173Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Scp173Entity scp173 : level.getEntitiesOfClass(
                Scp173Entity.class,
                player.getBoundingBox().inflate(SCP_131_SEPARATION_RADIUS + 2),
                entity -> entity.isAlive() && entity.getTarget() == player)) {
            Vec3 toScp173 = horizontal(
                    scp173.position().subtract(player.position()));
            if (toScp173.lengthSqr() < 1.0D
                    || toScp173.normalize().dot(direction) < 0.35D) {
                continue;
            }
            boolean watchedByFollower = followers.stream().anyMatch(follower ->
                    follower.distanceToSqr(scp173) <= 15.0D * 15.0D
                            && follower.hasLineOfSight(scp173));
            if (!watchedByFollower) continue;
            double distance = player.distanceToSqr(scp173);
            if (distance < bestDistance) {
                best = scp173;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static SeparationOpportunity findScp131SeparationDoor(
            ServerLevel level, ServerPlayer player,
            List<AbstractScp131Entity> followers, Scp173Entity scp173,
            Vec3 travelDirection, long gameTime) {
        SeparationOpportunity best = null;
        double bestDistance = Double.MAX_VALUE;
        Set<Long> visited = new HashSet<>();

        for (AbstractScp131Entity follower : followers) {
            Vec3 route = horizontal(
                    follower.position().subtract(player.position()));
            if (route.lengthSqr() < 2.25D
                    || route.normalize().dot(travelDirection) > -0.25D) {
                continue;
            }
            int steps = Math.min(SCP_131_DOOR_RADIUS,
                    Math.max(1, (int) Math.ceil(Math.sqrt(
                    route.lengthSqr()))));
            Vec3 direction = route.normalize();
            for (int step = 1; step <= steps; step++) {
                BlockPos center = BlockPos.containing(
                        player.position().add(direction.scale(step)));
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
                                    || match.stage() != DoorStage.OPEN
                                    || !HeavyDoorControlPanelAccess
                                    .hasDeniableInterface(level, pos)
                                    || !separatesFollowerAndScp173(match.state(),
                                    pos, player.position(), follower.position(),
                                    scp173.position())) {
                                continue;
                            }
                            double distance = player.distanceToSqr(
                                    Vec3.atCenterOf(pos));
                            if (distance < bestDistance) {
                                best = new SeparationOpportunity(match,
                                        follower);
                                bestDistance = distance;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean separatesFollowerAndScp173(BlockState doorState,
            BlockPos doorPos, Vec3 playerPosition, Vec3 followerPosition,
            Vec3 scp173Position) {
        if (!doorState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false;
        }
        Direction facing = doorState.getValue(HorizontalDirectionalBlock.FACING);
        Vec3 center = Vec3.atCenterOf(doorPos);
        double playerSide = signedDoorSide(center, facing, playerPosition);
        double followerSide = signedDoorSide(center, facing, followerPosition);
        double scp173Side = signedDoorSide(center, facing, scp173Position);
        return Math.abs(playerSide) >= 0.70D
                && Math.abs(followerSide) >= 0.70D
                && Math.abs(scp173Side) >= 0.35D
                && playerSide * followerSide < 0.0D
                && playerSide * scp173Side > 0.0D;
    }

    private static double signedDoorSide(Vec3 center, Direction facing,
            Vec3 position) {
        return (position.x - center.x) * facing.getStepX()
                + (position.z - center.z) * facing.getStepZ();
    }

    private static void evaluatePursuit(ServerLevel level, ServerPlayer player,
            Mob pursuer, long gameTime, float availablePower) {
        ThreatProfile profile = ThreatProfile.forMob(pursuer);
        AheadDoors ahead = findDoorsAhead(level, player, pursuer, gameTime,
                true);
        DoorMatch threatDoor = profile.canOpenDoors()
                ? findClosedDoorForThreat(level, player, pursuer, gameTime)
                : null;

        List<Action> candidates = new ArrayList<>(3);
        double pursuerDistance = pursuer.distanceTo(player);
        double threatPressure = Math.max(0.0D, 12.0D - pursuerDistance);

        if (ahead.closed() != null && profile.canDenyAccess()
                && HeavyDoorControlPanelAccess.hasDeniableInterface(level,
                ahead.closed().pos())
                && pursuerDistance >= profile.minimumLockDistance()) {
            double doorDistance = Math.sqrt(player.distanceToSqr(
                    Vec3.atCenterOf(ahead.closed().pos())));
            double commitment = Math.max(0.0D,
                    FLEE_DOOR_RADIUS - doorDistance);
            candidates.add(new Action(ActionType.DENY, ahead.closed(),
                    78.0D + commitment * 2.25D + threatPressure * 0.5D
                            + profile.denyBias(),
                    DENY_ACCESS_COST, profile.lockDurationTicks(),
                    profile.maximumLockDurationTicks()));
        }

        if (ahead.open() != null) {
            double doorDistance = Math.sqrt(player.distanceToSqr(
                    Vec3.atCenterOf(ahead.open().pos())));
            double commitment = Math.max(0.0D,
                    FLEE_DOOR_RADIUS - doorDistance);
            boolean canFollowWithLock = profile.canDenyAccess()
                    && HeavyDoorControlPanelAccess.hasDeniableInterface(level,
                    ahead.open().pos());
            candidates.add(new Action(ActionType.CLOSE, ahead.open(),
                    74.0D + commitment * 2.25D + threatPressure
                            + profile.closeBias(), CLOSE_AHEAD_COST,
                    canFollowWithLock ? profile.lockDurationTicks() : 0,
                    canFollowWithLock
                            ? profile.maximumLockDurationTicks() : 0));
        }

        if (threatDoor != null) {
            double doorDistance = Math.sqrt(pursuer.distanceToSqr(
                    Vec3.atCenterOf(threatDoor.pos())));
            double proximity = Math.max(0.0D,
                    PURSUER_DOOR_RADIUS - doorDistance);
            candidates.add(new Action(ActionType.OPEN, threatDoor,
                    76.0D + proximity * 2.5D + profile.openBias(),
                    OPEN_FOR_THREAT_COST, 0, 0));
        }

        executeTacticalPlan(level, candidates, gameTime, player, pursuer,
                availablePower);
    }

    private static void executeTacticalPlan(ServerLevel level,
            List<Action> candidates, long gameTime, ServerPlayer player,
            Mob pursuer, float availablePower) {
        if (candidates.isEmpty()) return;

        candidates.sort(Comparator.comparingDouble(
                (Action action) -> adjustedUtility(action, availablePower))
                .reversed());

        EnumSet<TacticalLane> usedLanes = EnumSet.noneOf(TacticalLane.class);
        int executed = 0;
        for (Action action : candidates) {
            TacticalLane lane = action.type().lane();
            if (usedLanes.contains(lane)
                    || !Scp079ProcessingManager.canAfford(level,
                    action.cost())) {
                continue;
            }
            if (execute(level, action, gameTime, player, pursuer)) {
                usedLanes.add(lane);
                executed++;
                if (executed >= MAX_TACTICAL_ACTIONS) return;
            }
        }
    }

    private static double adjustedUtility(Action action, float availablePower) {
        double utility = action.utility();
        if (availablePower < 30.0F) utility -= action.cost() * 0.75D;
        if (availablePower < 18.0F) utility -= action.cost() * 0.75D;
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

        boolean combinedLock = action.type() == ActionType.CLOSE
                && action.durationTicks() > 0 && player != null
                && pursuer != null
                && Scp079ProcessingManager.trySpend(level, DENY_ACCESS_COST);
        if (combinedLock) {
            int changed = HeavyDoorControlPanelAccess
                    .temporarilyDenyConnectedControls(level,
                    action.door().pos(), action.durationTicks());
            if (changed <= 0) {
                Scp079ProcessingManager.refund(level, DENY_ACCESS_COST);
                combinedLock = false;
            } else {
                Scp079SustainedDoorLocks.begin(level, action.door().pos(),
                        player.getUUID(), pursuer.getUUID(),
                        pursuer.getUUID(),
                        Scp079SustainedDoorLocks.LockReason.PURSUIT,
                        action.maximumDurationTicks());
            }
        }

        int reuse = combinedLock || action.type() == ActionType.DENY
                ? LOCKED_DOOR_REUSE_TICKS
                : action.type() == ActionType.CLOSE
                ? CLOSE_FOLLOWUP_REUSE_TICKS : DOOR_REUSE_TICKS;
        DOOR_COOLDOWNS.put(new DoorKey(level.dimension(),
                action.door().pos().asLong()), gameTime + reuse);
        if (action.type() == ActionType.DENY && player != null
                && pursuer != null
                && action.maximumDurationTicks() > action.durationTicks()) {
            Scp079SustainedDoorLocks.begin(level, action.door().pos(),
                    player.getUUID(), pursuer.getUUID(), pursuer.getUUID(),
                    Scp079SustainedDoorLocks.LockReason.PURSUIT,
                    action.maximumDurationTicks());
        }

        Scp079DecisionLog.DecisionType type = combinedLock
                ? Scp079DecisionLog.DecisionType.DENY_ACCESS
                : decisionType(action.type());
        double totalCost = action.cost()
                + (combinedLock ? DENY_ACCESS_COST : 0.0D);
        String context = combinedLock
                ? combinedCloseContext(action, player, pursuer)
                : decisionContext(action, player, pursuer);
        Scp079DecisionLog.record(level, type,
                Scp079DecisionLog.DecisionOutcome.EXECUTED,
                action.door().pos(), totalCost, context);
        if (player != null) {
            Scp079FacilityAccessManager.awardFirstInterference(player);
        }
        return true;
    }

    private static String combinedCloseContext(Action action,
            ServerPlayer player, Mob pursuer) {
        return "closed and denied the escape route ahead of "
                + player.getGameProfile().getName() + " fleeing "
                + pursuer.getDisplayName().getString() + " · sustained up to "
                + action.maximumDurationTicks() / 20.0D + "s";
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
            case OPEN -> "opened the pursuit route for " + threat
                    + " against " + playerName;
            case CLOSE -> pursuer == null
                    ? "unprovoked pressure near " + playerName
                    : "closed the escape route ahead of " + playerName
                    + " fleeing " + threat;
            case DENY -> "denied the escape route ahead of " + playerName
                    + " fleeing " + threat + " · sustained up to "
                    + action.maximumDurationTicks() / 20.0D + "s";
        };
    }

    private static AheadDoors findDoorsAhead(ServerLevel level,
            ServerPlayer player, Mob pursuer, long gameTime,
            boolean requirePursuerBehind) {
        Vec3 travel = recentTravel(player, gameTime);
        if (travel.lengthSqr() < MOVEMENT_THRESHOLD_SQR) {
            return AheadDoors.EMPTY;
        }
        Vec3 direction = travel.normalize();

        if (requirePursuerBehind && pursuer != null) {
            Vec3 fromThreatToPlayer = horizontal(
                    player.position().subtract(pursuer.position()));
            double minimumAlignment = pursuer instanceof Scp106Entity
                    ? -0.55D : -0.15D;
            if (fromThreatToPlayer.lengthSqr() < 0.0001D
                    || fromThreatToPlayer.normalize().dot(direction)
                    <= minimumAlignment) {
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
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 3; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
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
                                || toDoor.normalize().dot(direction) < 0.18D) {
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
        double threatSide = signedDoorSide(center, facing, threatPosition);
        double playerSide = signedDoorSide(center, facing, playerPosition);
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
            HeavyDoorControlPanelAccess.openConnectedControls(level,
                    match.pos());
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
            if (family.opening().stream().anyMatch(
                    entry -> entry.get() == block)) {
                return new DoorMatch(pos, state, family, DoorStage.OPENING);
            }
            if (family.closing().stream().anyMatch(
                    entry -> entry.get() == block)) {
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

    private static Vec3 observedTravel(ServerPlayer player) {
        Vec3 displacement = new Vec3(player.getX() - player.xo, 0.0D,
                player.getZ() - player.zo);
        Vec3 velocity = horizontal(player.getDeltaMovement());
        if (displacement.lengthSqr() >= MOVEMENT_THRESHOLD_SQR) {
            if (velocity.lengthSqr() >= MOVEMENT_THRESHOLD_SQR
                    && displacement.dot(velocity) > 0.0D) {
                return displacement.scale(0.75D).add(velocity.scale(0.25D));
            }
            return displacement;
        }
        return velocity.lengthSqr() >= MOVEMENT_THRESHOLD_SQR
                ? velocity : Vec3.ZERO;
    }

    private static void rememberTravel(ServerPlayer player, long gameTime) {
        RECENT_TRAVEL.computeIfAbsent(player.getUUID(),
                ignored -> new TravelMemory()).observe(player, gameTime);
    }

    private static Vec3 recentTravel(ServerPlayer player, long gameTime) {
        TravelMemory memory = RECENT_TRAVEL.get(player.getUUID());
        Vec3 fallback = observedTravel(player);
        return memory == null ? fallback : memory.resolve(gameTime, fallback);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RECENT_TRAVEL.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RECENT_TRAVEL.clear();
        DOOR_COOLDOWNS.clear();
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

    private enum TacticalLane {
        THREAT_ROUTE,
        PLAYER_ESCAPE
    }

    private enum ActionType {
        OPEN(TacticalLane.THREAT_ROUTE),
        CLOSE(TacticalLane.PLAYER_ESCAPE),
        DENY(TacticalLane.PLAYER_ESCAPE);

        private final TacticalLane lane;

        ActionType(TacticalLane lane) {
            this.lane = lane;
        }

        private TacticalLane lane() {
            return lane;
        }
    }

    private record Action(ActionType type, DoorMatch door, double utility,
                          double cost, int durationTicks,
                          int maximumDurationTicks) {
    }

    private record AheadDoors(DoorMatch open, DoorMatch closed) {
        private static final AheadDoors EMPTY = new AheadDoors(null, null);
    }

    private record DoorMatch(BlockPos pos, BlockState state,
            FacilityModule.DoorFamily family, DoorStage stage) {
    }

    private record SeparationOpportunity(DoorMatch door,
            AbstractScp131Entity follower) {
    }

    private record DoorKey(ResourceKey<Level> dimension, long pos) {
    }

    private static final class TravelMemory {
        private Vec3 previousPosition;
        private Vec3 smoothedTravel = Vec3.ZERO;
        private long lastMovingTick = Long.MIN_VALUE;

        private void observe(ServerPlayer player, long gameTime) {
            Vec3 position = player.position();
            if (previousPosition != null) {
                Vec3 displacement = horizontal(
                        position.subtract(previousPosition));
                double distanceSqr = displacement.lengthSqr();
                if (distanceSqr >= MOVEMENT_THRESHOLD_SQR
                        && distanceSqr <= 4.0D) {
                    smoothedTravel = smoothedTravel.lengthSqr()
                            < MOVEMENT_THRESHOLD_SQR
                            ? displacement
                            : smoothedTravel.scale(0.45D)
                            .add(displacement.scale(0.55D));
                    lastMovingTick = gameTime;
                }
            }
            previousPosition = position;
        }

        private Vec3 resolve(long gameTime, Vec3 fallback) {
            if (gameTime - lastMovingTick <= RECENT_TRAVEL_TICKS
                    && smoothedTravel.lengthSqr()
                    >= MOVEMENT_THRESHOLD_SQR) {
                return smoothedTravel;
            }
            return fallback;
        }
    }

    private record ThreatProfile(boolean canOpenDoors,
                                 boolean canDenyAccess,
                                 double openBias,
                                 double closeBias,
                                 double denyBias,
                                 int lockDurationTicks,
                                 int maximumLockDurationTicks,
                                 double minimumLockDistance,
                                 double threatPriority) {
        private static final ThreatProfile DEFAULT = new ThreatProfile(
                true, true, 0.0D, 0.0D, 0.0D,
                60, 160, 5.0D, 50.0D);

        private static ThreatProfile forMob(Mob mob) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(
                    mob.getType());
            if (id == null) return DEFAULT;
            return switch (id.getPath()) {
                case "scp_173" -> new ThreatProfile(true, true,
                        12.0D, 5.0D, -2.0D,
                        40, 100, 8.0D, 100.0D);
                case "scp_106" -> new ThreatProfile(false, true,
                        0.0D, 12.0D, 14.0D,
                        35, 100, 2.5D, 95.0D);
                default -> DEFAULT;
            };
        }
    }
}
