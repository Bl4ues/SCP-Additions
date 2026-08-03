package net.mcreator.scpadditions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.facility.FacilityModule;
import net.mcreator.scpadditions.init.UnifiedReaderItems;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Lightweight facility-cleaning robot.
 *
 * <p>The Roomba deliberately avoids vanilla pathfinding. It travels in a
 * straight line, probes only the small area immediately ahead, and chooses the
 * clearest turn only when it meets a wall, ledge, step, or facility door. This
 * keeps large groups inexpensive while preserving the characteristic
 * bump-turn-continue movement of a robotic vacuum.</p>
 */
public final class RoombaEntity extends PathfinderMob implements GeoEntity {
    private static final double FORWARD_SPEED = 0.025D;
    private static final float TURN_MAX_SPEED_DEGREES = 1.75F;
    private static final float TURN_MIN_SPEED_DEGREES = 0.32F;
    private static final float TURN_ACCELERATION = 0.11F;
    private static final float TURN_BRAKE_FACTOR = 0.085F;
    private static final int TURN_CONTACT_PAUSE_TICKS = 4;
    private static final int TURN_SETTLE_TICKS = 3;
    private static final double IMMEDIATE_PROBE_DISTANCE = 0.42D;
    private static final double DOOR_PROBE_EXTRA = 0.38D;
    private static final double FLOOR_DROP_TOLERANCE = 0.18D;
    private static final double FLOOR_RISE_TOLERANCE = 0.06D;
    private static final double[] CLEARANCE_PROBES = {
            0.34D, 0.62D, 0.90D, 1.18D, 1.46D
    };
    private static final float[] LEFT_FIRST_TURNS = {
            90.0F, -90.0F, 135.0F, -135.0F, 180.0F
    };
    private static final float[] RIGHT_FIRST_TURNS = {
            -90.0F, 90.0F, -135.0F, 135.0F, 180.0F
    };

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private boolean turning;
    private float targetYaw;
    private float turnVelocity;
    private int turnPauseTicks;
    private int settleTicks;

    public RoombaEntity(EntityType<? extends RoombaEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setMaxUpStep(0.0F);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty. Roombas use a tiny deterministic probe instead
        // of pathfinding, target selection, random strolling, or look goals.
    }

    @Override
    public void tick() {
        setMaxUpStep(0.0F);
        super.tick();

        if (level().isClientSide) {
            return;
        }

        setPersistenceRequired();
        getNavigation().stop();

        if (!onGround()) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(0.0D, movement.y, 0.0D);
            return;
        }

        if (turning) {
            turnTowardTarget();
        } else if (settleTicks > 0) {
            stopHorizontalMotion();
            settleTicks--;
        } else if (horizontalCollision
                || routeBlocked(getYRot(), IMMEDIATE_PROBE_DISTANCE)) {
            beginBestTurn();
            turnTowardTarget();
        } else {
            driveForward();
        }

        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    private void driveForward() {
        Vec3 forward = forwardVector(getYRot());
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(forward.x * FORWARD_SPEED, movement.y,
                forward.z * FORWARD_SPEED);
        hasImpulse = true;
    }

    private void beginBestTurn() {
        float baseYaw = getYRot();
        float[] candidates = random.nextBoolean()
                ? LEFT_FIRST_TURNS : RIGHT_FIRST_TURNS;
        float chosenOffset = 180.0F;
        double bestClearance = -1.0D;

        for (float offset : candidates) {
            double clearance = clearanceAt(baseYaw + offset);
            if (clearance > bestClearance + 1.0E-4D
                    || Math.abs(clearance - bestClearance) <= 1.0E-4D
                    && random.nextBoolean()) {
                bestClearance = clearance;
                chosenOffset = offset;
            }
        }

        targetYaw = Mth.wrapDegrees(baseYaw + chosenOffset);
        turning = true;
        turnVelocity = 0.0F;
        turnPauseTicks = TURN_CONTACT_PAUSE_TICKS;
        settleTicks = 0;
        stopHorizontalMotion();
    }

    private void turnTowardTarget() {
        stopHorizontalMotion();
        if (turnPauseTicks > 0) {
            turnPauseTicks--;
            return;
        }

        float difference = Mth.wrapDegrees(targetYaw - getYRot());
        float absoluteDifference = Math.abs(difference);
        if (absoluteDifference <= TURN_MIN_SPEED_DEGREES) {
            setYRot(targetYaw);
            turning = false;
            turnVelocity = 0.0F;
            settleTicks = TURN_SETTLE_TICKS;
            return;
        }

        float brakingSpeed = Mth.clamp(
                absoluteDifference * TURN_BRAKE_FACTOR,
                TURN_MIN_SPEED_DEGREES, TURN_MAX_SPEED_DEGREES);
        turnVelocity = Math.min(brakingSpeed,
                turnVelocity + TURN_ACCELERATION);
        float step = Math.min(absoluteDifference, turnVelocity);
        setYRot(Mth.wrapDegrees(getYRot()
                + Math.copySign(step, difference)));
    }

    private void stopHorizontalMotion() {
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(0.0D, movement.y, 0.0D);
        hasImpulse = true;
    }

    private double clearanceAt(float yaw) {
        double clearance = 0.0D;
        for (double distance : CLEARANCE_PROBES) {
            if (routeBlocked(yaw, distance)) {
                break;
            }
            clearance = distance;
        }
        return clearance;
    }

    private boolean routeBlocked(float yaw, double distance) {
        Vec3 forward = forwardVector(yaw);
        AABB probe = getBoundingBox()
                .move(forward.x * distance, 0.0D, forward.z * distance)
                .inflate(-0.025D, 0.0D, -0.025D);

        if (hasBlockCollision(probe)) {
            return true;
        }
        if (hasFacilityDoorAhead(forward, distance + DOOR_PROBE_EXTRA)) {
            return true;
        }

        double floorX = getX() + forward.x * (distance + 0.18D);
        double floorZ = getZ() + forward.z * (distance + 0.18D);
        return !hasLevelFloor(floorX, floorZ);
    }

    private boolean hasBlockCollision(AABB probe) {
        for (VoxelShape ignored : level().getBlockCollisions(this, probe)) {
            return true;
        }
        return false;
    }

    private boolean hasFacilityDoorAhead(Vec3 forward, double distance) {
        double feetY = getBoundingBox().minY + 0.05D;
        for (double sample = 0.12D; sample <= distance; sample += 0.18D) {
            double x = getX() + forward.x * sample;
            double z = getZ() + forward.z * sample;
            for (int vertical = 0; vertical <= 1; vertical++) {
                BlockPos pos = BlockPos.containing(x, feetY + vertical, z);
                if (FacilityModule.isFacilityDoor(level().getBlockState(pos))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasLevelFloor(double x, double z) {
        double feetY = getBoundingBox().minY;
        BlockPos below = BlockPos.containing(x, feetY - 0.06D, z);
        BlockState state = level().getBlockState(below);
        VoxelShape shape = state.getCollisionShape(level(), below);
        if (shape.isEmpty()) {
            return false;
        }

        double top = below.getY() + shape.max(Direction.Axis.Y);
        double difference = top - feetY;
        return difference >= -FLOOR_DROP_TOLERANCE
                && difference <= FLOOR_RISE_TOLERANCE;
    }

    private static Vec3 forwardVector(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0.0D,
                Math.cos(radians));
    }

    @Override
    protected InteractionResult mobInteract(Player player,
                                             InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(UnifiedReaderItems.SCREWDRIVER.get())) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier,
                                   DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // Players and mobs pass through without displacing the cleaner.
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("RoombaTurning", turning);
        tag.putFloat("RoombaTargetYaw", targetYaw);
        tag.putFloat("RoombaTurnVelocity", turnVelocity);
        tag.putInt("RoombaTurnPause", turnPauseTicks);
        tag.putInt("RoombaSettleTicks", settleTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        turning = tag.getBoolean("RoombaTurning");
        targetYaw = tag.contains("RoombaTargetYaw")
                ? tag.getFloat("RoombaTargetYaw") : getYRot();
        turnVelocity = tag.contains("RoombaTurnVelocity")
                ? tag.getFloat("RoombaTurnVelocity") : 0.0F;
        turnPauseTicks = tag.getInt("RoombaTurnPause");
        settleTicks = tag.getInt("RoombaSettleTicks");
        setPersistenceRequired();
        setMaxUpStep(0.0F);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        // Static model: movement comes from the entity's physical rotation.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
