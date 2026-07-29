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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private static final int DOOR_TICKS = 15;
    private static final int MECHANICAL_PAUSE_TICKS = 4;
    private static final int LEVELING_TICKS = 5;
    private static final double MAX_SPEED = 0.115D;
    private static final double ACCELERATION = 0.0075D;
    private static final double FLOOR_EPSILON = 0.035D;

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
    private ElevatorFoundation.MotionPlan motionPlan;
    private double previousServerY;

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
    }

    public void applyLayout(CoreRoomElevatorManager.ColumnLayout layout) {
        floorHeights = layout.floorHeights();
        entityData.set(FACING, (byte) layout.facing().get2DDataValue());
        entityData.set(CONTROLLER, layout.pulley());
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
        return true;
    }

    private void queueDestination(int destination) {
        if (destination < 0 || destination >= floorHeights.length) return;
        if (phase() == ElevatorFoundation.Phase.IDLE_OPEN) {
            queuedTarget = -1;
            entityData.set(TARGET_FLOOR, destination);
            setPhase(ElevatorFoundation.Phase.DOOR_CLOSING);
        } else {
            queuedTarget = destination;
        }
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) return;
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
            setPhase(ElevatorFoundation.Phase.DOOR_CLOSING);
        }
    }

    private void beginMotion() {
        int target = targetFloorIndex();
        if (target < 0 || target >= floorHeights.length) {
            setPhase(ElevatorFoundation.Phase.FAULT);
            return;
        }
        motionPlan = ElevatorFoundation.MotionPlan.create(getY(),
                floorHeights[target], MAX_SPEED, ACCELERATION);
        setPhase(ElevatorFoundation.Phase.MOVING);
    }

    private void tickMotion() {
        if (motionPlan == null) {
            setPhase(ElevatorFoundation.Phase.FAULT);
            return;
        }
        ElevatorFoundation.MotionSample sample = motionPlan.sample(phaseTicks);
        setPos(getX(), sample.positionY(), getZ());
        if (sample.complete()) {
            int target = targetFloorIndex();
            setPos(getX(), floorHeights[target], getZ());
            entityData.set(CURRENT_FLOOR, target);
            motionPlan = null;
            setPhase(ElevatorFoundation.Phase.LEVELING);
        }
    }

    private void resolveNearbyEntities(double deltaY) {
        AABB outer = cabinOuterBox().inflate(0.35D, 0.35D, 0.35D);
        List<Entity> nearby = level().getEntities(this, outer,
                entity -> entity.isAlive() && !entity.noPhysics
                        && !(entity instanceof CoreRoomElevatorCarriageEntity)
                        && !(entity instanceof Player player
                        && player.isSpectator()));
        for (Entity entity : nearby) {
            boolean inside = cabinInteriorBox().intersects(entity.getBoundingBox());
            boolean standing = isStandingOnFloor(entity, previousServerY);
            if ((inside || standing) && Math.abs(deltaY) > 1.0E-7D) {
                entity.move(MoverType.SHULKER,
                        new Vec3(0.0D, deltaY, 0.0D));
                entity.fallDistance = 0.0F;
                if (standing) entity.setOnGround(true);
            }
            resolveShellCollision(entity);
        }
    }

    private boolean isStandingOnFloor(Entity entity, double oldFloorY) {
        AABB box = entity.getBoundingBox();
        return box.maxX > getX() - 0.74D && box.minX < getX() + 0.74D
                && box.maxZ > getZ() - 0.74D && box.minZ < getZ() + 0.74D
                && box.minY >= oldFloorY - 0.12D
                && box.minY <= oldFloorY + 0.34D;
    }

    private void resolveShellCollision(Entity entity) {
        for (AABB shell : shellBoxes()) {
            AABB entityBox = entity.getBoundingBox();
            if (!entityBox.intersects(shell)) continue;
            double pushDown = entityBox.maxY - shell.minY;
            double pushUp = shell.maxY - entityBox.minY;
            double pushWest = entityBox.maxX - shell.minX;
            double pushEast = shell.maxX - entityBox.minX;
            double pushNorth = entityBox.maxZ - shell.minZ;
            double pushSouth = shell.maxZ - entityBox.minZ;

            double smallest = pushDown;
            Vec3 push = new Vec3(0.0D, -pushDown, 0.0D);
            if (pushUp < smallest) {
                smallest = pushUp;
                push = new Vec3(0.0D, pushUp, 0.0D);
            }
            if (pushWest < smallest) {
                smallest = pushWest;
                push = new Vec3(-pushWest, 0.0D, 0.0D);
            }
            if (pushEast < smallest) {
                smallest = pushEast;
                push = new Vec3(pushEast, 0.0D, 0.0D);
            }
            if (pushNorth < smallest) {
                smallest = pushNorth;
                push = new Vec3(0.0D, 0.0D, -pushNorth);
            }
            if (pushSouth < smallest) {
                push = new Vec3(0.0D, 0.0D, pushSouth);
            }
            entity.move(MoverType.SHULKER, push);
            if (push.y > 0.0D) {
                entity.setOnGround(true);
                entity.fallDistance = 0.0F;
            }
        }
    }

    private List<AABB> shellBoxes() {
        List<AABB> local = new ArrayList<>();
        local.add(new AABB(-0.82D, -0.20D, -0.82D,
                0.82D, 0.0D, 0.82D));
        local.add(new AABB(-0.82D, 3.06D, -0.82D,
                0.82D, 3.32D, 0.82D));
        local.add(new AABB(-0.84D, 0.0D, -0.82D,
                -0.72D, 3.08D, 0.82D));
        local.add(new AABB(0.72D, 0.0D, -0.82D,
                0.84D, 3.08D, 0.82D));
        local.add(new AABB(-0.82D, 0.0D, 0.72D,
                0.82D, 3.08D, 0.84D));
        if (phase() != ElevatorFoundation.Phase.IDLE_OPEN
                && phase() != ElevatorFoundation.Phase.DOOR_OPENING) {
            local.add(new AABB(-0.72D, 0.0D, -0.84D,
                    0.72D, 2.35D, -0.72D));
        }
        List<AABB> world = new ArrayList<>();
        for (AABB box : local) {
            AABB rotated = CoreRoomElevatorGeometry.rotateAabb(box, facing(),
                    0.0D, 0.0D);
            world.add(rotated.move(getX(), getY(), getZ()));
        }
        return world;
    }

    private AABB cabinOuterBox() {
        return new AABB(getX() - 0.86D, getY() - 0.22D,
                getZ() - 0.86D, getX() + 0.86D, getY() + 3.34D,
                getZ() + 0.86D);
    }

    private AABB cabinInteriorBox() {
        return new AABB(getX() - 0.72D, getY() - 0.05D,
                getZ() - 0.72D, getX() + 0.72D, getY() + 3.05D,
                getZ() + 0.72D);
    }

    public Vec3 contextAnchor(boolean up) {
        double modelX = -10.95508D / 16.0D;
        double modelY = (up ? 21.25D : 19.25D) / 16.0D;
        double modelZ = 11.00251D / 16.0D;
        // The authored cabin root carries a -90 degree Y rotation.
        Vec3 rootRotated = new Vec3(-modelZ, modelY, modelX);
        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing(), rootRotated.x, rootRotated.y, rootRotated.z);
        return position().add(facingRotated);
    }

    public Vec3 cableAttachment(boolean front) {
        double modelX = (front ? -7.0D : 7.0D) / 16.0D;
        double modelY = 53.0D / 16.0D;
        Vec3 rootRotated = new Vec3(0.0D, modelY, modelX);
        return position().add(CoreRoomElevatorGeometry.rotateLocalVector(
                facing(), rootRotated.x, rootRotated.y, rootRotated.z));
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
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        boolean up = hit.y >= 1.25D;
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
        queuedTarget = tag.getInt("QueuedTarget");
        phaseTicks = tag.getInt("PhaseTicks");
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
