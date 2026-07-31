package net.mcreator.scpadditions.facility.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.mcreator.scpadditions.network.ScpEntityNetwork;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** One server-authoritative moving carriage shared by every stop in a column. */
public final class CoreRoomElevatorCarriageEntity extends Entity
        implements GeoEntity {
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(CoreRoomElevatorCarriageEntity.class,
                    EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FACING =
            SynchedEntityData.defineId(CoreRoomElevatorCarriageEntity.class,
                    EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<BlockPos> CONTROLLER =
            SynchedEntityData.defineId(CoreRoomElevatorCarriageEntity.class,
                    EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> CURRENT_FLOOR =
            SynchedEntityData.defineId(CoreRoomElevatorCarriageEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_FLOOR =
            SynchedEntityData.defineId(CoreRoomElevatorCarriageEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLOOR_COUNT =
            SynchedEntityData.defineId(CoreRoomElevatorCarriageEntity.class,
                    EntityDataSerializers.INT);

    private static final int DOOR_TICKS = 15;
    private static final int MECHANICAL_PAUSE_TICKS = 4;
    private static final int LEVELING_TICKS = 5;
    public static final int TRAVEL_TICKS = 8 * 20;
    private static final int ARRIVAL_SOUND_LEAD_TICKS = 30;
    private static final int ARRIVAL_SOUND_MOVING_TICK =
            TRAVEL_TICKS + LEVELING_TICKS - ARRIVAL_SOUND_LEAD_TICKS;
    private static final double FLOOR_EPSILON = 0.035D;
    private static final int DOOR_COLLISION_THRESHOLD = DOOR_TICKS / 2;
    private static final double BUTTON_HIT_RADIUS_SQR = 0.20D * 0.20D;
    private static final double FLOOR_TOP = 0.0D;
    private static final double COLLISION_EPSILON = 1.0E-4D;

    private static final RawAnimation CLOSED_ANIMATION = RawAnimation.begin()
            .thenLoop(ElevatorAssets.CARRIAGE_CLOSED);
    private static final RawAnimation OPENING_ANIMATION = RawAnimation.begin()
            .thenPlay(ElevatorAssets.CARRIAGE_OPENING);
    private static final RawAnimation OPEN_ANIMATION = RawAnimation.begin()
            .thenLoop(ElevatorAssets.CARRIAGE_OPEN);
    private static final RawAnimation CLOSING_ANIMATION = RawAnimation.begin()
            .thenPlay(ElevatorAssets.CARRIAGE_CLOSING);

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private int[] floorHeights = new int[0];
    private int phaseTicks;
    private int queuedTarget = -1;
    private double motionStartY;
    private double motionEndY;
    private boolean motionReady;
    private double previousServerY;
    private int clientLerpSteps;
    private double clientLerpX;
    private double clientLerpY;
    private double clientLerpZ;
    private float clientLerpYRot;
    private float clientLerpXRot;
    private final Map<Integer, Vec3> previousEntityPositions = new HashMap<>();
    private final Map<Integer, Double> cabinStepDistance = new HashMap<>();

    public CoreRoomElevatorCarriageEntity(
            EntityType<? extends CoreRoomElevatorCarriageEntity> type,
            Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(PHASE,
                (byte) ElevatorFoundation.Phase.IDLE_OPEN.ordinal());
        entityData.define(FACING, (byte) Direction.NORTH.get2DDataValue());
        entityData.define(CONTROLLER, BlockPos.ZERO);
        entityData.define(CURRENT_FLOOR, 0);
        entityData.define(TARGET_FLOOR, -1);
        entityData.define(FLOOR_COUNT, 0);
    }

    public void applyLayout(CoreRoomElevatorManager.ColumnLayout layout) {
        floorHeights = layout.floorHeights();
        entityData.set(FACING, (byte) layout.facing().get2DDataValue());
        entityData.set(FLOOR_COUNT, floorHeights.length);
        entityData.set(CONTROLLER, layout.pulley());
        setYRot(layout.facing().toYRot());
        yRotO = getYRot();
        if (floorHeights.length == 0) return;
        int nearest = nearestFloorIndex(getY());
        int current = currentFloorIndex();
        if (current < 0 || current >= floorHeights.length) {
            entityData.set(CURRENT_FLOOR, nearest);
        }
        if (phase() == ElevatorFoundation.Phase.IDLE_OPEN
                && Math.abs(getY() - floorHeights[currentFloorIndex()])
                > FLOOR_EPSILON) {
            setPos(getX(), floorHeights[nearest], getZ());
            entityData.set(CURRENT_FLOOR, nearest);
        }
    }

    public boolean requestFromStation(int stationIndex,
            ElevatorFoundation.TravelDirection direction,
            ServerPlayer player) {
        if (stationIndex < 0 || stationIndex >= floorHeights.length) {
            return false;
        }
        int destination;
        if (!isAtFloorIndex(stationIndex)) {
            destination = stationIndex;
        } else {
            destination = stationIndex + direction.step();
            if (destination < 0 || destination >= floorHeights.length) {
                player.sendSystemMessage(net.minecraft.network.chat.Component
                        .translatable(direction == ElevatorFoundation.TravelDirection.UP
                                ? "message.scp_additions.elevator_no_floor_above"
                                : "message.scp_additions.elevator_no_floor_below"));
                return false;
            }
        }
        queueDestination(destination);
        return true;
    }

    public boolean handleContextInteraction(ServerPlayer player,
            String actionKey) {
        ElevatorFoundation.TravelDirection direction = actionKey != null
                && actionKey.endsWith("up")
                ? ElevatorFoundation.TravelDirection.UP
                : ElevatorFoundation.TravelDirection.DOWN;
        Vec3 button = contextAnchor(
                direction == ElevatorFoundation.TravelDirection.UP);
        playElevatorSoundAt(CoreRoomElevatorModule.ELEVATOR_BUTTON_PRESS.get(),
                button, 1.0F);
        if (phase() != ElevatorFoundation.Phase.IDLE_OPEN) return false;
        int current = nearestFloorIndex(getY());
        int destination = current + direction.step();
        if (destination < 0 || destination >= floorHeights.length) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable(direction == ElevatorFoundation.TravelDirection.UP
                            ? "message.scp_additions.elevator_no_floor_above"
                            : "message.scp_additions.elevator_no_floor_below"));
            return false;
        }
        queueDestination(destination);
        playElevatorSoundAt(CoreRoomElevatorModule.ELEVATOR_BUTTON_ACCEPT.get(),
                button, 1.0F);
        return true;
    }

    private void queueDestination(int destination) {
        if (destination < 0 || destination >= floorHeights.length) return;
        if (phase() == ElevatorFoundation.Phase.IDLE_OPEN) {
            queuedTarget = -1;
            entityData.set(TARGET_FLOOR, destination);
            playElevatorSound(CoreRoomElevatorModule.ELEVATOR_DOOR_CLOSE.get(),
                    1.0F);
            setPhase(ElevatorFoundation.Phase.DOOR_CLOSING);
        } else {
            queuedTarget = destination;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot,
            float xRot, int steps, boolean teleport) {
        if (!level().isClientSide || teleport) {
            super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
            return;
        }
        clientLerpX = x;
        clientLerpY = y;
        clientLerpZ = z;
        clientLerpYRot = yRot;
        clientLerpXRot = xRot;
        clientLerpSteps = Math.max(1, steps);
    }

    private void tickClientInterpolation() {
        if (clientLerpSteps <= 0) return;
        double fraction = 1.0D / clientLerpSteps;
        setPos(Mth.lerp(fraction, getX(), clientLerpX),
                Mth.lerp(fraction, getY(), clientLerpY),
                Mth.lerp(fraction, getZ(), clientLerpZ));
        setYRot(Mth.rotLerp((float) fraction, getYRot(),
                clientLerpYRot));
        setXRot(Mth.lerp((float) fraction, getXRot(),
                clientLerpXRot));
        clientLerpSteps--;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) {
            double oldY = getY();
            tickClientInterpolation();
            double clientDeltaY = getY() - oldY;
            previousServerY = oldY;
            if (Math.abs(clientDeltaY) > 1.0D) {
                clientDeltaY = 0.0D;
            }
            resolveNearbyEntities(clientDeltaY);
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (tickCount % 40 == 0
                && !CoreRoomElevatorManager.isColumnStillValid(serverLevel,
                this)) {
            discard();
            return;
        }

        previousServerY = getY();
        phaseTicks++;
        switch (phase()) {
            case IDLE_OPEN -> tickIdle();
            case DOOR_CLOSING -> {
                if (phaseTicks >= DOOR_TICKS) {
                    playElevatorSound(CoreRoomElevatorModule.STATION_CLOSE.get(),
                            1.0F);
                    setPhase(ElevatorFoundation.Phase.STATION_CLOSING);
                }
            }
            case STATION_CLOSING -> {
                if (phaseTicks >= DOOR_TICKS) {
                    setPhase(ElevatorFoundation.Phase.READY_TO_MOVE);
                }
            }
            case READY_TO_MOVE -> {
                if (phaseTicks >= MECHANICAL_PAUSE_TICKS) {
                    beginMotion();
                }
            }
            case MOVING -> tickMotion();
            case LEVELING -> {
                if (phaseTicks >= LEVELING_TICKS) {
                    playElevatorSound(
                            CoreRoomElevatorModule.ELEVATOR_DOOR_OPEN.get(),
                            1.0F);
                    setPhase(ElevatorFoundation.Phase.DOOR_OPENING);
                }
            }
            case DOOR_OPENING -> {
                if (phaseTicks >= DOOR_TICKS) {
                    setPhase(ElevatorFoundation.Phase.IDLE_OPEN);
                    entityData.set(TARGET_FLOOR, -1);
                    if (queuedTarget == currentFloorIndex()) queuedTarget = -1;
                }
            }
            case FAULT -> {
            }
        }
        resolveNearbyEntities(getY() - previousServerY);
    }

    private void tickIdle() {
        if (queuedTarget >= 0 && queuedTarget != currentFloorIndex()) {
            int next = queuedTarget;
            queuedTarget = -1;
            entityData.set(TARGET_FLOOR, next);
            playElevatorSound(CoreRoomElevatorModule.ELEVATOR_DOOR_CLOSE.get(),
                    1.0F);
            setPhase(ElevatorFoundation.Phase.DOOR_CLOSING);
        }
    }

    private void beginMotion() {
        int target = targetFloorIndex();
        if (target < 0 || target >= floorHeights.length) {
            setPhase(ElevatorFoundation.Phase.FAULT);
            return;
        }
        motionStartY = getY();
        motionEndY = floorHeights[target];
        motionReady = true;
        setPhase(ElevatorFoundation.Phase.MOVING);
    }

    private boolean resumeMotionAfterLoad() {
        int target = targetFloorIndex();
        if (floorHeights.length == 0 || target < 0
                || target >= floorHeights.length) {
            recoverAtNearestFloor();
            return false;
        }
        if (!motionReady) {
            motionStartY = getY();
            motionEndY = floorHeights[target];
            phaseTicks = 0;
            motionReady = true;
        }
        return true;
    }

    private void recoverAtNearestFloor() {
        if (floorHeights.length == 0) {
            setPhase(ElevatorFoundation.Phase.FAULT);
            return;
        }
        int nearest = nearestFloorIndex(getY());
        setPos(getX(), floorHeights[nearest], getZ());
        entityData.set(CURRENT_FLOOR, nearest);
        entityData.set(TARGET_FLOOR, -1);
        queuedTarget = -1;
        motionReady = false;
        setPhase(ElevatorFoundation.Phase.IDLE_OPEN);
    }

    private void tickMotion() {
        if (!motionReady && !resumeMotionAfterLoad()) return;
        double normalized = Mth.clamp(phaseTicks / (double) TRAVEL_TICKS,
                0.0D, 1.0D);
        double progress = soundSyncedProgress(normalized);
        if (phaseTicks == ARRIVAL_SOUND_MOVING_TICK
                && level() instanceof ServerLevel serverLevel) {
            triggerArrivalCue(serverLevel);
        }
        setPos(getX(), Mth.lerp(progress, motionStartY, motionEndY), getZ());
        if (phaseTicks >= TRAVEL_TICKS) {
            int target = targetFloorIndex();
            setPos(getX(), floorHeights[target], getZ());
            entityData.set(CURRENT_FLOOR, target);
            motionReady = false;
            setPhase(ElevatorFoundation.Phase.LEVELING);
        }
    }

    /**
     * Uses the full eight-second movement phase, beginning when the carriage
     * first moves and the movement sound starts, then eases into the stop at
     * the destination as the shortened audio ends.
     */
    private static double soundSyncedProgress(double time) {
        double clamped = Mth.clamp(time, 0.0D, 1.0D);
        return clamped * clamped * clamped
                * (clamped * (clamped * 6.0D - 15.0D) + 10.0D);
    }

    private void triggerArrivalCue(ServerLevel serverLevel) {
        int floor = targetFloorIndex();
        if (floor < 0 || floor >= floorHeights.length) return;
        BlockPos stationPos = new BlockPos(columnX(),
                floorHeights[floor], columnZ());
        if (!(serverLevel.getBlockEntity(stationPos)
                instanceof CoreRoomElevatorModule.StationBlockEntity station)) {
            return;
        }
        ElevatorArrivalDisplayData data = station.arrivalDisplay();
        if (!data.enabled() || data.sectorLabel().isBlank()) return;
        AABB passengers = cabinInteriorBox().inflate(0.18D, 0.12D,
                0.18D);
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.isAlive() || player.isSpectator()
                    || !passengers.intersects(player.getBoundingBox())) {
                continue;
            }
            ScpEntityNetwork.showElevatorArrival(player, data,
                    ARRIVAL_SOUND_LEAD_TICKS);
        }
    }

    private void playElevatorSound(net.minecraft.sounds.SoundEvent sound,
            float volume) {
        playElevatorSoundAt(sound,
                new Vec3(getX(), getY() + 1.0D, getZ()), volume);
    }

    private void playElevatorSoundAt(net.minecraft.sounds.SoundEvent sound,
            Vec3 position, float volume) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.playSound(null, position.x, position.y, position.z, sound,
                net.minecraft.sounds.SoundSource.BLOCKS, volume, 1.0F);
    }

    private void resolveNearbyEntities(double deltaY) {
        AABB outer = cabinOuterBox().inflate(0.45D, 1.50D, 0.45D);
        List<Entity> nearby = level().getEntities(this, outer,
                entity -> entity.isAlive() && !entity.noPhysics
                        && !(entity instanceof CoreRoomElevatorCarriageEntity)
                        && !(entity instanceof Player player
                        && player.isSpectator()));
        Set<Integer> present = new HashSet<>();
        for (Entity entity : nearby) {
            present.add(entity.getId());
            Vec3 previous = previousEntityPositions.getOrDefault(
                    entity.getId(), entity.position());
            boolean inside = cabinInteriorBox().intersects(entity.getBoundingBox());
            boolean standing = isStandingOnFloor(entity, previousServerY);
            if ((inside || standing) && Math.abs(deltaY) > 1.0E-7D) {
                if (level().isClientSide) {
                    entity.setPos(entity.getX(), entity.getY() + deltaY,
                            entity.getZ());
                } else {
                    entity.move(MoverType.SHULKER,
                            new Vec3(0.0D, deltaY, 0.0D));
                }
                entity.fallDistance = 0.0F;
                if (standing) stabilizeGroundedEntity(entity);
            }
            resolveSweptFloorCollision(entity, previous);
            resolveShellCollision(entity);
            playCabinFootstep(entity, previous);
            previousEntityPositions.put(entity.getId(), entity.position());
        }
        previousEntityPositions.keySet().removeIf(id -> !present.contains(id));
        cabinStepDistance.keySet().removeIf(id -> !present.contains(id));
    }

    private void resolveSweptFloorCollision(Entity entity, Vec3 previous) {
        AABB floor = shellBoxes().get(0);
        AABB box = entity.getBoundingBox();
        Vec3 motion = entity.getDeltaMovement();
        boolean horizontalOverlap = box.maxX > floor.minX
                && box.minX < floor.maxX && box.maxZ > floor.minZ
                && box.minZ < floor.maxZ;
        boolean descending = motion.y <= 0.0D;
        boolean crossedFloor = descending
                && previous.y >= floor.maxY - 0.10D
                && box.minY < floor.maxY;
        boolean recoverBelowFloor = descending
                && previous.y >= floor.maxY - 0.35D
                && box.minY < floor.maxY
                && box.minY > floor.minY - 0.45D;
        if (!horizontalOverlap || (!crossedFloor && !recoverBelowFloor)) return;
        placeEntityOnFloor(entity, floor);
    }

    private static void placeEntityOnFloor(Entity entity, AABB floor) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > 0.0D) return;
        AABB box = entity.getBoundingBox();
        entity.move(MoverType.SHULKER, new Vec3(0.0D,
                floor.maxY - box.minY + COLLISION_EPSILON, 0.0D));
        stabilizeGroundedEntity(entity);
    }

    private static void stabilizeGroundedEntity(Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > 0.0D) return;
        entity.setDeltaMovement(motion.x, 0.0D, motion.z);
        entity.setOnGround(true);
        entity.fallDistance = 0.0F;
    }

    private void playCabinFootstep(Entity entity, Vec3 previous) {
        if (!(entity instanceof Player) || !isStandingOnFloor(entity, getY())) {
            cabinStepDistance.remove(entity.getId());
            return;
        }
        double dx = entity.getX() - previous.x;
        double dz = entity.getZ() - previous.z;
        double travelled = Math.sqrt(dx * dx + dz * dz);
        if (travelled <= 1.0E-4D || travelled > 1.25D) return;
        double accumulated = cabinStepDistance.getOrDefault(entity.getId(), 0.0D)
                + travelled;
        if (accumulated >= 0.58D && level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, entity.blockPosition(),
                    SoundType.STONE.getStepSound(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.32F,
                    0.96F + serverLevel.getRandom().nextFloat() * 0.08F);
            accumulated %= 0.58D;
        }
        cabinStepDistance.put(entity.getId(), accumulated);
    }

    private boolean isStandingOnFloor(Entity entity, double oldFloorY) {
        AABB box = entity.getBoundingBox();
        double floorTop = oldFloorY + FLOOR_TOP;
        return entity.getDeltaMovement().y <= 0.02D
                && box.maxX > getX() - 0.74D
                && box.minX < getX() + 0.74D
                && box.maxZ > getZ() - 0.74D
                && box.minZ < getZ() + 0.74D
                && box.minY >= floorTop - 0.08D
                && box.minY <= floorTop + 0.12D;
    }

    private void resolveShellCollision(Entity entity) {
        List<AABB> shells = shellBoxes();
        if (shells.size() < 5) return;
        resolveFloorCollision(entity, shells.get(0));
        resolveCeilingCollision(entity, shells.get(1));
        for (int index = 2; index < shells.size(); index++) {
            resolveHorizontalCollision(entity, shells.get(index));
        }
    }

    private void resolveFloorCollision(Entity entity, AABB floor) {
        AABB box = entity.getBoundingBox();
        if (entity.getDeltaMovement().y > 0.0D || !box.intersects(floor)) {
            return;
        }
        if (box.getCenter().y >= floor.getCenter().y) {
            placeEntityOnFloor(entity, floor);
        }
    }

    private void resolveCeilingCollision(Entity entity, AABB ceiling) {
        AABB box = entity.getBoundingBox();
        if (!box.intersects(ceiling)
                || box.getCenter().y > ceiling.getCenter().y) return;
        entity.move(MoverType.SHULKER, new Vec3(0.0D,
                ceiling.minY - box.maxY - COLLISION_EPSILON, 0.0D));
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y > 0.0D) {
            entity.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
        entity.setOnGround(false);
    }

    private void resolveHorizontalCollision(Entity entity, AABB shell) {
        AABB box = entity.getBoundingBox();
        if (!box.intersects(shell)) return;
        double west = box.maxX - shell.minX;
        double east = shell.maxX - box.minX;
        double north = box.maxZ - shell.minZ;
        double south = shell.maxZ - box.minZ;
        double smallest = west;
        Vec3 push = new Vec3(-west - COLLISION_EPSILON, 0.0D, 0.0D);
        if (east < smallest) {
            smallest = east;
            push = new Vec3(east + COLLISION_EPSILON, 0.0D, 0.0D);
        }
        if (north < smallest) {
            smallest = north;
            push = new Vec3(0.0D, 0.0D,
                    -north - COLLISION_EPSILON);
        }
        if (south < smallest) {
            push = new Vec3(0.0D, 0.0D,
                    south + COLLISION_EPSILON);
        }
        Vec3 motion = entity.getDeltaMovement();
        entity.move(MoverType.SHULKER, push);
        double motionX = motion.x;
        double motionZ = motion.z;
        if (push.x != 0.0D && motionX * push.x < 0.0D) motionX = 0.0D;
        if (push.z != 0.0D && motionZ * push.z < 0.0D) motionZ = 0.0D;
        entity.setDeltaMovement(motionX, motion.y, motionZ);
    }

    private List<AABB> shellBoxes() {
        List<AABB> local = new ArrayList<>();
        local.add(new AABB(-0.8125D, -0.203125D, -0.8125D,
                0.8125D, FLOOR_TOP, 0.8125D));
        local.add(new AABB(-0.8125D, 2.5625D, -0.8125D,
                0.8125D, 3.3125D, 0.8125D));
        local.add(new AABB(0.71875D, 0.0D, -0.8125D,
                0.84375D, 3.08D, 0.8125D));
        local.add(new AABB(-0.8125D, 0.0D, -0.84375D,
                0.8125D, 3.08D, -0.71875D));
        local.add(new AABB(-0.8125D, 0.0D, 0.71875D,
                0.8125D, 3.08D, 0.84375D));
        if (isDoorCollisionSolid()) {
            local.add(new AABB(-0.84375D, 0.0D, -0.71875D,
                    -0.71875D, 2.35D, 0.71875D));
        }
        List<AABB> world = new ArrayList<>();
        for (AABB box : local) {
            AABB modelAligned = CoreRoomElevatorGeometry.rotateAabb(box,
                    Direction.EAST, 0.0D, 0.0D);
            AABB facingAligned = CoreRoomElevatorGeometry.rotateAabb(
                    modelAligned, facing(), 0.0D, 0.0D);
            world.add(facingAligned.move(getX(), getY(), getZ()));
        }
        return world;
    }

    public boolean isDoorCollisionSolid() {
        return switch (phase()) {
            case IDLE_OPEN -> false;
            case DOOR_OPENING -> phaseTicks < DOOR_COLLISION_THRESHOLD;
            case DOOR_CLOSING -> phaseTicks >= DOOR_COLLISION_THRESHOLD;
            default -> true;
        };
    }

    private AABB cabinOuterBox() {
        return new AABB(getX() - 0.86D, getY() - 0.22D,
                getZ() - 0.86D, getX() + 0.86D, getY() + 3.34D,
                getZ() + 0.86D);
    }

    private AABB cabinInteriorBox() {
        return new AABB(getX() - 0.72D, getY() + FLOOR_TOP - 0.04D,
                getZ() - 0.72D, getX() + 0.72D, getY() + 3.05D,
                getZ() + 0.72D);
    }

    public Vec3 contextAnchor(boolean up) {
        double modelX = -10.95508D / 16.0D;
        double modelY = (up ? 21.25D : 19.25D) / 16.0D;
        double modelZ = -11.00251D / 16.0D;
        Vec3 modelAligned = CoreRoomElevatorGeometry.rotateLocalVector(
                Direction.EAST, modelX, modelY, modelZ);
        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing(), modelAligned.x, modelAligned.y, modelAligned.z);
        return position().add(facingRotated);
    }

    public Vec3 cableAttachment(boolean front) {
        return cableAttachment(front, 1.0F);
    }

    public Vec3 cableAttachment(boolean front, float partialTick) {
        double modelY = 53.0D / 16.0D;
        double modelZ = (front ? -7.0D : 7.0D) / 16.0D;
        return getPosition(partialTick).add(
                CoreRoomElevatorGeometry.rotateLocalVector(facing(),
                        0.0D, modelY, modelZ));
    }

    public Vec3 cableOrigin(boolean front, float partialTick) {
        BlockPos pulley = controllerPos();
        double modelX = (front ? -7.0D : 7.0D) / 16.0D;
        double modelY = 15.0D / 16.0D;
        Vec3 rootRotated = new Vec3(0.0D, modelY, modelX);
        Vec3 offset = CoreRoomElevatorGeometry.rotateLocalVector(facing(),
                rootRotated.x, rootRotated.y, rootRotated.z);
        return Vec3.atLowerCornerOf(pulley).add(0.5D, 0.0D, 0.5D)
                .add(offset);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hit,
            InteractionHand hand) {
        Vec3 upAnchor = contextAnchor(true).subtract(position());
        Vec3 downAnchor = contextAnchor(false).subtract(position());
        double upDistance = hit.distanceToSqr(upAnchor);
        double downDistance = hit.distanceToSqr(downAnchor);
        if (Math.min(upDistance, downDistance) > BUTTON_HIT_RADIUS_SQR) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        boolean up = upDistance <= downDistance;
        Vec3 selectedButton = up ? contextAnchor(true)
                : contextAnchor(false);
        Vec3 inward = position().subtract(selectedButton)
                .multiply(1.0D, 0.0D, 1.0D);
        if (inward.lengthSqr() >= 1.0E-6D
                && player.getEyePosition().subtract(selectedButton)
                .dot(inward.normalize()) <= 0.02D) {
            return InteractionResult.PASS;
        }
        return handleContextInteraction(serverPlayer,
                up ? "elevator_carriage_up" : "elevator_carriage_down")
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    public boolean matchesColumn(int x, int z) {
        return columnX() == x && columnZ() == z;
    }

    public int columnX() {
        BlockPos controller = controllerPos();
        return controller.equals(BlockPos.ZERO)
                ? Mth.floor(getX()) : controller.getX();
    }

    public int columnZ() {
        BlockPos controller = controllerPos();
        return controller.equals(BlockPos.ZERO)
                ? Mth.floor(getZ()) : controller.getZ();
    }

    public BlockPos controllerPos() {
        return entityData.get(CONTROLLER);
    }

    public int[] floorHeights() {
        return Arrays.copyOf(floorHeights, floorHeights.length);
    }

    public Direction facing() {
        return Direction.from2DDataValue(entityData.get(FACING));
    }

    public boolean canTravel(ElevatorFoundation.TravelDirection direction) {
        if (direction == ElevatorFoundation.TravelDirection.NONE
                || phase() != ElevatorFoundation.Phase.IDLE_OPEN) return false;
        int destination = currentFloorIndex() + direction.step();
        return destination >= 0 && destination < entityData.get(FLOOR_COUNT);
    }

    public ElevatorFoundation.Phase phase() {
        int index = entityData.get(PHASE);
        return index >= 0 && index < ElevatorFoundation.Phase.values().length
                ? ElevatorFoundation.Phase.values()[index]
                : ElevatorFoundation.Phase.FAULT;
    }

    private void setPhase(ElevatorFoundation.Phase phase) {
        entityData.set(PHASE, (byte) phase.ordinal());
        phaseTicks = 0;
    }

    public int currentFloorIndex() {
        return entityData.get(CURRENT_FLOOR);
    }

    public int targetFloorIndex() {
        return entityData.get(TARGET_FLOOR);
    }

    public boolean isAtFloorHeight(int floorY) {
        return Math.abs(getY() - floorY) <= FLOOR_EPSILON;
    }

    private boolean isAtFloorIndex(int index) {
        return index >= 0 && index < floorHeights.length
                && isAtFloorHeight(floorHeights[index]);
    }

    private int nearestFloorIndex(double y) {
        if (floorHeights.length == 0) return 0;
        int nearest = 0;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < floorHeights.length; i++) {
            double candidate = Math.abs(y - floorHeights[i]);
            if (candidate < distance) {
                distance = candidate;
                nearest = i;
            }
        }
        return nearest;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        floorHeights = tag.getIntArray("Floors");
        entityData.set(CONTROLLER, BlockPos.of(tag.getLong("Controller")));
        entityData.set(FACING, tag.getByte("Facing"));
        entityData.set(PHASE, tag.getByte("Phase"));
        entityData.set(CURRENT_FLOOR, tag.getInt("CurrentFloor"));
        entityData.set(TARGET_FLOOR, tag.getInt("TargetFloor"));
        entityData.set(FLOOR_COUNT, floorHeights.length);
        queuedTarget = tag.contains("QueuedTarget")
                ? tag.getInt("QueuedTarget") : -1;
        phaseTicks = tag.getInt("PhaseTicks");
        motionStartY = tag.contains("MotionStartY")
                ? tag.getDouble("MotionStartY") : getY();
        motionEndY = tag.contains("MotionEndY")
                ? tag.getDouble("MotionEndY") : getY();
        motionReady = tag.getBoolean("MotionReady");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putIntArray("Floors", floorHeights);
        tag.putLong("Controller", controllerPos().asLong());
        tag.putByte("Facing", entityData.get(FACING));
        tag.putByte("Phase", entityData.get(PHASE));
        tag.putInt("CurrentFloor", currentFloorIndex());
        tag.putInt("TargetFloor", targetFloorIndex());
        tag.putInt("QueuedTarget", queuedTarget);
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putDouble("MotionStartY", motionStartY);
        tag.putDouble("MotionEndY", motionEndY);
        tag.putBoolean("MotionReady", motionReady);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "doors", 0, state ->
                state.setAndContinue(switch (phase()) {
                    case IDLE_OPEN -> OPEN_ANIMATION;
                    case DOOR_OPENING -> OPENING_ANIMATION;
                    case DOOR_CLOSING -> CLOSING_ANIMATION;
                    default -> CLOSED_ANIMATION;
                })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
