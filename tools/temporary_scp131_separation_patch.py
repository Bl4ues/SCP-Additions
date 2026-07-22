from pathlib import Path


def replace(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}:\n{old[:400]}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# SCP-131: dismiss the complete following group instead of teleporting.
path = "src/main/java/net/mcreator/scpadditions/entity/AbstractScp131Entity.java"
replace(path,
'''import net.minecraft.server.level.ServerPlayer;
''',
'''import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
''')
replace(path,
'''    private static final double GROUP_TOGGLE_RANGE = 24.0D;
    private static final double GROUP_TOGGLE_RANGE_SQR = GROUP_TOGGLE_RANGE * GROUP_TOGGLE_RANGE;
''',
'''    private static final double GROUP_TOGGLE_RANGE = 24.0D;
    private static final double GROUP_TOGGLE_RANGE_SQR = GROUP_TOGGLE_RANGE * GROUP_TOGGLE_RANGE;
    private static final AABB WORLD_BOUNDS = new AABB(-30000000.0D,
            -2048.0D, -30000000.0D, 30000000.0D, 4096.0D,
            30000000.0D);
''')
replace(path,
'''        scheduleInitialAmbientNoise();
        maybePlayAmbientNoise();

        Scp173Entity scp173 = findNearestScp173();
''',
'''        scheduleInitialAmbientNoise();
        maybePlayAmbientNoise();

        if (isFollowing() && dismissGroupWhenOwnerIsTooFar()) {
            return;
        }

        Scp173Entity scp173 = findNearestScp173();
''')
replace(path,
'''        if (wasWatchingScp173) {
            wasWatchingScp173 = false;
            teleportToOwnerIfTooFar();
        }
''',
'''        if (wasWatchingScp173) {
            wasWatchingScp173 = false;
        }
''')
replace(path,
'''    public static boolean stopFollowersFor(Player player) {
        if (player == null || player.level().isClientSide) {
            return false;
        }
        boolean stoppedAny = false;
        AABB area = player.getBoundingBox().inflate(64.0D);
        for (AbstractScp131Entity scp131 : player.level().getEntitiesOfClass(AbstractScp131Entity.class, area,
                entity -> entity.isAlive() && entity.isFollowingPlayer(player))) {
            scp131.stopFollowing();
            stoppedAny = true;
        }
        return stoppedAny;
    }
''',
'''    public static boolean stopFollowersFor(Player player) {
        if (player == null || player.level().isClientSide) {
            return false;
        }
        boolean stoppedAny = false;
        MinecraftServer server = player.getServer();
        if (server == null) {
            for (AbstractScp131Entity scp131 : player.level().getEntitiesOfClass(
                    AbstractScp131Entity.class, WORLD_BOUNDS,
                    entity -> entity.isAlive()
                            && entity.isFollowingPlayer(player))) {
                scp131.stopFollowing();
                stoppedAny = true;
            }
            return stoppedAny;
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (AbstractScp131Entity scp131 : level.getEntitiesOfClass(
                    AbstractScp131Entity.class, WORLD_BOUNDS,
                    entity -> entity.isAlive()
                            && entity.isFollowingPlayer(player))) {
                scp131.stopFollowing();
                stoppedAny = true;
            }
        }
        return stoppedAny;
    }
''')
replace(path,
'''    private void teleportToOwnerIfTooFar() {
        if (followOwner == null) {
            return;
        }

        Player owner = level().getPlayerByUUID(followOwner);
        if (owner == null || owner.isRemoved() || !owner.isAlive() || distanceToSqr(owner) <= OWNER_RETURN_DISTANCE_SQR) {
            return;
        }

        teleportNearOwner(owner);
    }

    private void teleportNearOwner(Player owner) {
        getNavigation().stop();
        Vec3 behindOwner = owner.position().subtract(owner.getLookAngle().normalize().scale(1.15D));
        moveTo(behindOwner.x, owner.getY(), behindOwner.z, owner.getYRot(), 0.0F);
        scheduleNextOwnerRoam(8);
    }

''',
'''    private boolean dismissGroupWhenOwnerIsTooFar() {
        if (followOwner == null) {
            return false;
        }
        MinecraftServer server = level().getServer();
        ServerPlayer owner = server == null ? null
                : server.getPlayerList().getPlayer(followOwner);
        if (owner == null || owner.isRemoved() || !owner.isAlive()) {
            return false;
        }
        boolean differentLevel = owner.serverLevel() != level();
        if (!differentLevel
                && distanceToSqr(owner) <= OWNER_RETURN_DISTANCE_SQR) {
            return false;
        }

        boolean stopped = stopFollowersFor(owner);
        if (!stopped) {
            stopFollowing();
        }
        ScpEntityNetwork.showScp131Notice(owner, false);
        return true;
    }

''')
replace(path,
'''        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > OWNER_RETURN_DISTANCE_SQR) {
            teleportNearOwner(owner);
            return;
        }
''',
'''        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > OWNER_RETURN_DISTANCE_SQR) {
            dismissGroupWhenOwnerIsTooFar();
            return;
        }
''')


# Allow active visual denials to be extended without restoring the panel first.
path = "src/main/java/net/mcreator/scpadditions/facility/HeavyDoorControlPanelAccess.java"
replace(path,
'''        int changed = 0;
        for (BlockPos buttonPos : controls.buttons()) {
            if (denyButton(level, buttonPos, durationTicks)) changed++;
        }
        for (BlockPos readerPos : controls.readers()) {
            if (denyReader(level, readerPos, durationTicks)) changed++;
        }
        return changed;
    }
''',
'''        int changed = 0;
        for (BlockPos buttonPos : controls.buttons()) {
            if (extendActiveDenial(level, buttonPos, durationTicks)
                    || denyButton(level, buttonPos, durationTicks)) {
                changed++;
            }
        }
        for (BlockPos readerPos : controls.readers()) {
            if (extendActiveDenial(level, readerPos, durationTicks)
                    || denyReader(level, readerPos, durationTicks)) {
                changed++;
            }
        }
        return changed;
    }

    private static boolean extendActiveDenial(ServerLevel level,
            BlockPos pos, int durationTicks) {
        PanelKey key = new PanelKey(level.dimension(), pos.asLong());
        DenialState previous = ACTIVE_DENIALS.get(key);
        if (previous == null || level.getBlockState(pos).getBlock()
                != previous.deniedBlock()) {
            return false;
        }
        long generation = DENIAL_GENERATION.incrementAndGet();
        DenialState extended = new DenialState(previous.originalState(),
                previous.deniedBlock(), generation);
        ACTIVE_DENIALS.put(key, extended);
        scheduleRestore(level, pos, key, extended,
                Math.max(1, durationTicks));
        return true;
    }
''')
replace(path,
'''                Block block = level.getBlockState(candidate).getBlock();
                if (isFunctionalButton(block)) {
                    buttons.add(candidate.immutable());
                } else if (readerBasePath(block) != null) {
                    readers.add(candidate.immutable());
                } else if (isLegacyNode(block)) {
                    legacyNodes.add(candidate.immutable());
                }
''',
'''                Block block = level.getBlockState(candidate).getBlock();
                DenialState active = ACTIVE_DENIALS.get(new PanelKey(
                        level.dimension(), candidate.asLong()));
                if (isFunctionalButton(block)
                        || isActiveDeniedButton(block, active)) {
                    buttons.add(candidate.immutable());
                } else if (readerBasePath(block) != null
                        || isActiveDeniedReader(block, active)) {
                    readers.add(candidate.immutable());
                } else if (isLegacyNode(block)) {
                    legacyNodes.add(candidate.immutable());
                }
''')
replace(path,
'''    private static boolean isFunctionalButton(Block block) {
''',
'''    private static boolean isActiveDeniedButton(Block block,
            DenialState active) {
        return active != null && block == active.deniedBlock()
                && deniedButton(active.originalState().getBlock()) != null;
    }

    private static boolean isActiveDeniedReader(Block block,
            DenialState active) {
        return active != null && block == active.deniedBlock()
                && readerBasePath(active.originalState().getBlock()) != null;
    }

    private static boolean isFunctionalButton(Block block) {
''')


# SCP-079 facility strategy.
path = "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityThreatEvents.java"
replace(path,
'''import net.minecraft.world.entity.Mob;
''',
'''import net.minecraft.world.entity.Mob;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import net.mcreator.scpadditions.entity.Scp173Entity;
''')
replace(path,
'''    private static final int LOCKED_DOOR_REUSE_TICKS = 140;

    private static final double OPEN_FOR_THREAT_COST = 8.0D;
''',
'''    private static final int LOCKED_DOOR_REUSE_TICKS = 140;
    private static final int SCP_131_SEPARATION_RADIUS = 16;
    private static final int SCP_131_DOOR_RADIUS = 7;
    private static final int SCP_131_INITIAL_LOCK_TICKS = 40;
    private static final int SCP_131_MAX_LOCK_TICKS = 160;

    private static final double OPEN_FOR_THREAT_COST = 8.0D;
''')
replace(path,
'''    private static final double UNPROVOKED_COST = 6.0D;
''',
'''    private static final double UNPROVOKED_COST = 6.0D;
    private static final double SCP_131_SEPARATION_COST =
            CLOSE_AHEAD_COST + DENY_ACCESS_COST;
''')
replace(path,
'''        List<Mob> pursuers = level.getEntitiesOfClass(Mob.class,
''',
'''        if (availablePower >= SCP_131_SEPARATION_COST
                && trySeparateScp131(level, player, gameTime)) {
            return;
        }

        List<Mob> pursuers = level.getEntitiesOfClass(Mob.class,
''')
replace(path,
'''            execute(level, new Action(ActionType.CLOSE,
                    ahead.open(), 40.0D, UNPROVOKED_COST, 0), gameTime,
                    player, null);
''',
'''            execute(level, new Action(ActionType.CLOSE,
                    ahead.open(), 40.0D, UNPROVOKED_COST, 0, 0), gameTime,
                    player, null);
''')

insert_before = '''    private static void evaluatePursuit(ServerLevel level, ServerPlayer player,
'''
separation_methods = '''    private static boolean trySeparateScp131(ServerLevel level,
            ServerPlayer player, long gameTime) {
        List<AbstractScp131Entity> followers = level.getEntitiesOfClass(
                AbstractScp131Entity.class,
                player.getBoundingBox().inflate(SCP_131_SEPARATION_RADIUS),
                entity -> entity.isAlive()
                        && entity.isFollowingPlayer(player));
        if (followers.isEmpty()) return false;

        Vec3 travel = horizontal(player.getDeltaMovement());
        if (travel.lengthSqr() < 0.0025D) {
            travel = horizontal(player.getLookAngle());
        }
        if (travel.lengthSqr() < 0.0001D) return false;
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
                player.getUUID(), scp173.getUUID(),
                Scp079SustainedDoorLocks.LockReason.SCP_131_SEPARATION,
                SCP_131_MAX_LOCK_TICKS);
        Scp079DecisionLog.record(level,
                Scp079DecisionLog.DecisionType.SEPARATE_SCP_131,
                Scp079DecisionLog.DecisionOutcome.EXECUTED,
                opportunity.door().pos(), SCP_131_SEPARATION_COST,
                "closed SCP-173 ahead away from "
                        + player.getGameProfile().getName()
                        + " · lock upkeep 1.5 AP/s");
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
                    || route.normalize().dot(travelDirection) < 0.25D) {
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
                && followerSide * scp173Side > 0.0D;
    }

    private static double signedDoorSide(Vec3 center, Direction facing,
            Vec3 position) {
        return (position.x - center.x) * facing.getStepX()
                + (position.z - center.z) * facing.getStepZ();
    }

'''
replace(path, insert_before, separation_methods + insert_before)

replace(path,
'''            candidates.add(new Action(ActionType.DENY, ahead.closed(),
                    78.0D + commitment, DENY_ACCESS_COST,
                    profile.lockDurationTicks()));
''',
'''            candidates.add(new Action(ActionType.DENY, ahead.closed(),
                    78.0D + commitment, DENY_ACCESS_COST,
                    profile.lockDurationTicks(),
                    profile.maximumLockDurationTicks()));
''')
replace(path,
'''            candidates.add(new Action(ActionType.CLOSE, ahead.open(),
                    72.0D + commitment, CLOSE_AHEAD_COST, 0));
''',
'''            candidates.add(new Action(ActionType.CLOSE, ahead.open(),
                    72.0D + commitment, CLOSE_AHEAD_COST, 0, 0));
''')
replace(path,
'''            candidates.add(new Action(ActionType.OPEN, threatDoor,
                    70.0D + proximity * 1.6D,
                    OPEN_FOR_THREAT_COST, 0));
''',
'''            candidates.add(new Action(ActionType.OPEN, threatDoor,
                    70.0D + proximity * 1.6D,
                    OPEN_FOR_THREAT_COST, 0, 0));
''')
replace(path,
'''        DOOR_COOLDOWNS.put(new DoorKey(level.dimension(),
                action.door().pos().asLong()), gameTime + reuse);
        Scp079DecisionLog.record(level, decisionType(action.type()),
''',
'''        DOOR_COOLDOWNS.put(new DoorKey(level.dimension(),
                action.door().pos().asLong()), gameTime + reuse);
        if (action.type() == ActionType.DENY && player != null
                && pursuer != null
                && action.maximumDurationTicks() > action.durationTicks()) {
            Scp079SustainedDoorLocks.begin(level, action.door().pos(),
                    player.getUUID(), pursuer.getUUID(),
                    Scp079SustainedDoorLocks.LockReason.PURSUIT,
                    action.maximumDurationTicks());
        }
        Scp079DecisionLog.record(level, decisionType(action.type()),
''')
replace(path,
'''            case DENY -> "against " + playerName + " fleeing " + threat
                    + " · " + action.durationTicks() / 20.0D + "s";
''',
'''            case DENY -> "against " + playerName + " fleeing " + threat
                    + " · sustained up to "
                    + action.maximumDurationTicks() / 20.0D + "s";
''')
replace(path,
'''    private record Action(ActionType type, DoorMatch door, double utility,
                           double cost, int durationTicks) {
''',
'''    private record Action(ActionType type, DoorMatch door, double utility,
                           double cost, int durationTicks,
                           int maximumDurationTicks) {
''')
replace(path,
'''    private record DoorMatch(BlockPos pos, BlockState state,
            FacilityModule.DoorFamily family, DoorStage stage) {
    }

    private record DoorKey(ResourceKey<Level> dimension, long pos) {
''',
'''    private record DoorMatch(BlockPos pos, BlockState state,
            FacilityModule.DoorFamily family, DoorStage stage) {
    }

    private record SeparationOpportunity(DoorMatch door,
            AbstractScp131Entity follower) {
    }

    private record DoorKey(ResourceKey<Level> dimension, long pos) {
''')
replace(path,
'''                                  int lockDurationTicks,
                                  double minimumLockDistance) {
        private static final ThreatProfile DEFAULT = new ThreatProfile(
                true, true, 0.30F, 0.20F, 0.10F, 60, 5.0D);
''',
'''                                  int lockDurationTicks,
                                  int maximumLockDurationTicks,
                                  double minimumLockDistance) {
        private static final ThreatProfile DEFAULT = new ThreatProfile(
                true, true, 0.30F, 0.20F, 0.10F, 60, 160, 5.0D);
''')
replace(path,
'''                case "scp_173" -> new ThreatProfile(true, true,
                        0.36F, 0.22F, 0.06F, 40, 8.0D);
                case "scp_106" -> new ThreatProfile(false, true,
                        0.0F, 0.24F, 0.04F, 35, 10.0D);
''',
'''                case "scp_173" -> new ThreatProfile(true, true,
                        0.36F, 0.22F, 0.06F, 40, 100, 8.0D);
                case "scp_106" -> new ThreatProfile(false, true,
                        0.0F, 0.24F, 0.04F, 35, 80, 10.0D);
''')

# Decision feed and protocol.
path = "src/main/java/net/mcreator/scpadditions/facility/Scp079DecisionLog.java"
replace(path,
'''        ABANDON_SCP_012_CONTEST,
        ABORTED_ACTION
''',
'''        ABANDON_SCP_012_CONTEST,
        SEPARATE_SCP_131,
        ABORTED_ACTION
''')

path = "src/main/java/net/mcreator/scpadditions/vitals/client/Scp079EnergyOverlay.java"
replace(path,
'''            case ABANDON_SCP_012_CONTEST ->
                    "ABANDONED SCP-012 CONTEST";
            case ABORTED_ACTION -> "ABORTED ACTION";
''',
'''            case ABANDON_SCP_012_CONTEST ->
                    "ABANDONED SCP-012 CONTEST";
            case SEPARATE_SCP_131 -> "SEPARATED SCP-131";
            case ABORTED_ACTION -> "ABORTED ACTION";
''')

path = "src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java"
replace(path,
'''    private static final String PROTOCOL_VERSION = "12";
''',
'''    private static final String PROTOCOL_VERSION = "13";
''')

# Player-facing changelog.
path = "CHANGELOG.md"
replace(path,
'''## SCP-079

- Added a processing-power system that limits how often SCP-079 can interfere with the facility and forces it to choose its actions more carefully;
- SCP-079 now reacts differently depending on the threat chasing the player, using doors, temporary access denial, and nearby Tesla Gates when useful;
- Improved its SCP-012 trap behavior so repeated interference becomes increasingly difficult and less worthwhile;
- Added optional Debug Tools displays for SCP-079's power, its recent decisions, and roamer spawn timers.
''',
'''## SCP-079

- Added a processing-power system that limits how often SCP-079 can interfere with the facility and forces it to choose its actions more carefully;
- SCP-079 now reacts differently depending on the threat chasing the player, using doors, temporary access denial, and nearby Tesla Gates when useful;
- Improved its SCP-012 trap behavior so repeated interference becomes increasingly difficult and less worthwhile;
- SCP-079 can now close and lock an open door to separate following SCP-131 instances when SCP-173 is waiting ahead;
- SCP-079 can spend processing power continuously to keep a useful door locked for longer, releasing it when the strategy is no longer useful or it cannot afford the upkeep;
- Added optional Debug Tools displays for SCP-079's power, its recent decisions, and roamer spawn timers.
''')
replace(path,
'''## SCP-173

- Reduced SCP-173's rendered height to approximately two blocks.
''',
'''## SCP-173

- Reduced SCP-173's rendered height to approximately two blocks.

## SCP-131

- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally.
''')
