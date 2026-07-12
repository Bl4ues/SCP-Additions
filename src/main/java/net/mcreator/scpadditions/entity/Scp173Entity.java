package net.mcreator.scpadditions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.BlinkClient;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

public class Scp173Entity extends BlinkWatcherEntity {
    private static final EntityDataAccessor<Boolean> SCRAPING = SynchedEntityData.defineId(Scp173Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> MANUAL_YAW = SynchedEntityData.defineId(Scp173Entity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ACTIVATED = SynchedEntityData.defineId(Scp173Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ROUTINE_SPAWN = SynchedEntityData.defineId(Scp173Entity.class, EntityDataSerializers.BOOLEAN);

    private static final ResourceKey<DamageType> NECK_SNAP_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(ScpAdditionsMod.MODID, "scp_173_neck_snap"));
    private static final double OBSERVED_DOT_THRESHOLD = 0.08D;
    private static final double DIRECT_STEP_PER_TICK = 1.20D;
    private static final double BLINK_STEP_PER_TICK = 1.20D;
    private static final double STOP_DISTANCE = 0.72D;
    private static final double ATTACK_CONTACT_EXPAND = 0.08D;
    private static final double PATH_NODE_REACHED_DISTANCE_SQR = 0.55D * 0.55D;
    private static final double FROZEN_AIR_GRAVITY = 0.08D;
    private static final double FROZEN_WATER_GRAVITY = 0.045D;
    private static final double FROZEN_MAX_AIR_FALL_SPEED = -3.92D;
    private static final double FROZEN_MAX_WATER_SINK_SPEED = -0.32D;
    private static final double LINE_OF_SIGHT_STEP = 0.25D;
    private static final double VISIBILITY_EPSILON = 0.03D;
    private static final double IMMEDIATE_REACTION_RANGE = 48.0D;
    private static final double IMMEDIATE_REACTION_RANGE_SQR = IMMEDIATE_REACTION_RANGE * IMMEDIATE_REACTION_RANGE;
    private static final double DESPAWN_DISTANCE = 20.0D;
    private static final double DESPAWN_DISTANCE_SQR = DESPAWN_DISTANCE * DESPAWN_DISTANCE;
    private static final int ROUTINE_DESPAWN_UNSEEN_TICKS = 400;
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private static final int PATH_RECALCULATE_INTERVAL_TICKS = 8;
    private static final float NECK_SNAP_DAMAGE = 200.0F;

    private FrozenPose clientObservedVisualLock;
    private int lastSeenOrCloseTick;
    private int nextAttackTick;
    private int nextPathRecalculationTick;
    private double frozenFallSpeed;

    public Scp173Entity(EntityType<? extends Scp173Entity> type, Level level) {
        super(type, level);
        setMaxUpStep(1.05F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.20D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    public static void reactToBlinkState(ServerPlayer player, boolean closed) {
        if (player == null || player.level().isClientSide || !ScpAdditionsModulesConfig.get().scp173.enabled) return;
        AABB area = player.getBoundingBox().inflate(IMMEDIATE_REACTION_RANGE);
        for (Scp173Entity scp173 : player.serverLevel().getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(player) <= IMMEDIATE_REACTION_RANGE_SQR)) {
            Entity target = scp173.getTarget();
            if (target != null && target != player) continue;
            if (!scp173.isActivated() && !scp173.isObservedBy(player)) continue;
            scp173.setActivated(true);
            scp173.setTarget(player);
            scp173.reactImmediatelyToTarget(player);
        }
    }

    public void markRoutineSpawn() {
        entityData.set(ROUTINE_SPAWN, true);
        setActivated(false);
        setPersistenceRequired();
        lastSeenOrCloseTick = tickCount;
        setTarget(null);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SCRAPING, false);
        entityData.define(MANUAL_YAW, 0.0F);
        entityData.define(ACTIVATED, true);
        entityData.define(ROUTINE_SPAWN, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        setMaxUpStep(1.05F);
        if (level().isClientSide) {
            super.tick();
            if (!ScpAdditionsModulesConfig.get().scp173.enabled) {
                hardStopLocalMovement();
                return;
            }
            if (isClientObservedByLocalPlayer()) {
                freezeClientAtObservedPosition();
                return;
            }
            clientObservedVisualLock = null;
            applyClientManualRotation();
            if (!isScraping()) hardStopLocalMovement();
            return;
        }

        FrozenPose preTickPose = capturePose();
        if (!ScpAdditionsModulesConfig.get().scp173.enabled) {
            stopAndLock(preTickPose);
            return;
        }
        if (!isActivated()) {
            super.tick();
            LivingEntity observer = findObservingEntity();
            if (observer != null) {
                setActivated(true);
                setTarget(observer);
                if (observer instanceof Player) lastSeenOrCloseTick = tickCount;
            }
            restorePose(preTickPose);
            stopAndLock(preTickPose);
            handleRoutineDespawn();
            return;
        }

        LivingEntity observer = findObservingEntity();
        if (observer instanceof Player) lastSeenOrCloseTick = tickCount;
        if (observer != null) {
            restorePose(preTickPose);
            stopAndLock(preTickPose);
            handleRoutineDespawn();
            return;
        }

        super.tick();
        LivingEntity target = resolveTarget();
        if (target != null) {
            if (target instanceof Player && distanceToSqr(target) <= DESPAWN_DISTANCE_SQR) lastSeenOrCloseTick = tickCount;
            if (!trySnapAttack(target)) reactImmediatelyToTarget(target);
        } else {
            stopAndLock(preTickPose);
        }
        handleRoutineDespawn();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int increments, boolean teleport) {
        if (isClientObservedByLocalPlayer()) {
            freezeClientAtObservedPosition();
            return;
        }
        if (!isScraping()) {
            absMoveTo(x, y, z, entityData.get(MANUAL_YAW), 0.0F);
            hardStopLocalMovement();
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, increments, teleport);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        if (isClientObservedByLocalPlayer()) {
            freezeClientAtObservedPosition();
            return;
        }
        if (!isScraping()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.lerpMotion(x, y, z);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Activated", isActivated());
        tag.putBoolean("RoutineSpawn", isRoutineSpawn());
        tag.putInt("LastSeenOrCloseTick", lastSeenOrCloseTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setActivated(tag.getBoolean("Activated"));
        entityData.set(ROUTINE_SPAWN, tag.getBoolean("RoutineSpawn"));
        lastSeenOrCloseTick = tag.getInt("LastSeenOrCloseTick");
    }

    public boolean isScraping() { return entityData.get(SCRAPING); }
    public boolean isActivated() { return entityData.get(ACTIVATED); }
    public boolean isRoutineSpawn() { return entityData.get(ROUTINE_SPAWN); }
    public boolean isObservedBy(Player player) { return shouldFreezeFor(player); }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (level().isClientSide || !(entity instanceof LivingEntity target) || !canSnapTarget(target)) return false;
        return snapTargetNeck(target);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) level().playSound(null, getX(), getY(), getZ(), Scp173Sounds.STATUE_DEATH.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        super.die(source);
    }

    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return null; }
    @Override protected SoundEvent getDeathSound() { return null; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { }
    @Override protected boolean isSunSensitive() { return false; }

    private void setActivated(boolean value) { entityData.set(ACTIVATED, value); }

    private boolean shouldFreezeFor(LivingEntity observer) {
        if (!isValidObserver(observer)) return false;
        if (observer instanceof Player player && BlinkServerState.isBlinkClosed(player)) return false;
        return isObservedGeometry(observer);
    }

    private boolean isValidObserver(LivingEntity entity) {
        if (entity == null || entity == this || !entity.isAlive() || entity.isRemoved()) return false;
        if (entity instanceof Player player) return !player.isCreative() && !player.isSpectator();
        return Scp173TargetConfig.isConfiguredTarget(entity);
    }

    private LivingEntity resolveTarget() {
        LivingEntity current = getTarget();
        if (isValidTargetEntity(current)) return current;
        LivingEntity nearest = findNearestTargetEntity();
        if (nearest != null) setTarget(nearest);
        return nearest;
    }

    private LivingEntity findNearestTargetEntity() {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB area = getBoundingBox().inflate(IMMEDIATE_REACTION_RANGE);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && isValidTargetEntity(entity))) {
            double distance = distanceToSqr(entity);
            if (distance <= IMMEDIATE_REACTION_RANGE_SQR && distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private boolean isValidTargetEntity(LivingEntity entity) {
        if (entity == null || entity == this || !entity.isAlive() || entity.isRemoved()) return false;
        if (entity instanceof Player player) return isValidTargetPlayer(player);
        return Scp173TargetConfig.isConfiguredTarget(entity);
    }

    private boolean isValidTargetPlayer(Player player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private LivingEntity findObservingEntity() {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        AABB area = getBoundingBox().inflate(IMMEDIATE_REACTION_RANGE);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && isValidObserver(entity))) {
            if (shouldFreezeFor(entity)) {
                double distance = distanceToSqr(entity);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = entity;
                }
            }
        }
        return best;
    }

    private Player findObservingPlayer() {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : level().players()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (shouldFreezeFor(player)) {
                double distance = distanceToSqr(player);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = player;
                }
            }
        }
        return best;
    }

    private boolean hasClosePlayer() {
        for (Player player : level().players()) {
            if (!player.isCreative() && !player.isSpectator() && distanceToSqr(player) <= DESPAWN_DISTANCE_SQR) return true;
        }
        return false;
    }

    private void handleRoutineDespawn() {
        if (!isRoutineSpawn() || level().isClientSide) return;
        if (findObservingPlayer() != null || hasClosePlayer()) return;
        if (tickCount - lastSeenOrCloseTick >= ROUTINE_DESPAWN_UNSEEN_TICKS) discard();
    }

    private boolean isClientObservedByLocalPlayer() {
        if (!level().isClientSide) return false;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator() || BlinkClient.isBlinkClosedLocally()) return false;
        return isObservedGeometry(mc.player);
    }

    private boolean isObservedGeometry(LivingEntity observer) {
        Vec3 eye = observer.getEyePosition(1.0F);
        Vec3 look = observer.getViewVector(1.0F).normalize();
        AABB box = getBoundingBox();
        double minX = box.minX + VISIBILITY_EPSILON, midX = (box.minX + box.maxX) * 0.5D, maxX = box.maxX - VISIBILITY_EPSILON;
        double minY = box.minY + VISIBILITY_EPSILON, midY = (box.minY + box.maxY) * 0.5D, maxY = box.maxY - VISIBILITY_EPSILON;
        double minZ = box.minZ + VISIBILITY_EPSILON, midZ = (box.minZ + box.maxZ) * 0.5D, maxZ = box.maxZ - VISIBILITY_EPSILON;
        return isVisibleSample(eye, look, new Vec3(midX, midY, midZ))
                || isVisibleSample(eye, look, new Vec3(midX, maxY, midZ))
                || isVisibleSample(eye, look, new Vec3(midX, minY, midZ))
                || isVisibleSample(eye, look, new Vec3(minX, midY, midZ))
                || isVisibleSample(eye, look, new Vec3(maxX, midY, midZ))
                || isVisibleSample(eye, look, new Vec3(midX, midY, minZ))
                || isVisibleSample(eye, look, new Vec3(midX, midY, maxZ))
                || isVisibleSample(eye, look, new Vec3(minX, minY, minZ))
                || isVisibleSample(eye, look, new Vec3(minX, minY, maxZ))
                || isVisibleSample(eye, look, new Vec3(minX, maxY, minZ))
                || isVisibleSample(eye, look, new Vec3(minX, maxY, maxZ))
                || isVisibleSample(eye, look, new Vec3(maxX, minY, minZ))
                || isVisibleSample(eye, look, new Vec3(maxX, minY, maxZ))
                || isVisibleSample(eye, look, new Vec3(maxX, maxY, minZ))
                || isVisibleSample(eye, look, new Vec3(maxX, maxY, maxZ));
    }

    private boolean isVisibleSample(Vec3 eye, Vec3 look, Vec3 point) {
        Vec3 toPoint = point.subtract(eye);
        double distance = toPoint.length();
        if (distance <= 0.001D) return true;
        double dot = look.dot(toPoint.scale(1.0D / distance));
        return dot >= OBSERVED_DOT_THRESHOLD && hasVisualLineOfSightThroughTransparentBlocks(eye, point);
    }

    private boolean hasVisualLineOfSightThroughTransparentBlocks(Vec3 start, Vec3 end) {
        double distance = start.distanceTo(end);
        int steps = Math.max(1, (int) Math.ceil(distance / LINE_OF_SIGHT_STEP));
        for (int i = 1; i < steps; i++) {
            Vec3 point = start.lerp(end, i / (double) steps);
            BlockPos pos = BlockPos.containing(point);
            BlockState state = level().getBlockState(pos);
            if (!state.isAir() && state.canOcclude()) return false;
        }
        return true;
    }

    private void freezeClientAtObservedPosition() {
        if (clientObservedVisualLock == null) clientObservedVisualLock = capturePose();
        restorePose(clientObservedVisualLock);
        setManualYaw(clientObservedVisualLock.yRot());
        applyFrozenVerticalPhysics();
        clientObservedVisualLock = capturePose();
        hardStopLocalMovement();
    }

    private void reactImmediatelyToTarget(LivingEntity target) {
        FrozenPose preActionPose = capturePose();
        if (findObservingEntity() != null) {
            restorePose(preActionPose);
            stopAndLock(preActionPose);
        } else chaseImmediately(target, preActionPose);
    }

    private void stopAndLock(FrozenPose pose) {
        restorePose(pose);
        setManualYaw(pose.yRot());
        applyFrozenVerticalPhysics();
        freezeCompletely();
    }

    private void freezeCompletely() {
        entityData.set(SCRAPING, false);
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);
        hardStopLocalMovement();
        xxa = yya = zza = 0.0F;
        hasImpulse = true;
    }

    private void applyFrozenVerticalPhysics() {
        if (isInWater()) frozenFallSpeed = Math.max(frozenFallSpeed - FROZEN_WATER_GRAVITY, FROZEN_MAX_WATER_SINK_SPEED);
        else if (!onGround()) frozenFallSpeed = Math.max(frozenFallSpeed - FROZEN_AIR_GRAVITY, FROZEN_MAX_AIR_FALL_SPEED);
        else { frozenFallSpeed = 0.0D; return; }
        move(MoverType.SELF, new Vec3(0.0D, frozenFallSpeed, 0.0D));
        if (onGround() && frozenFallSpeed < 0.0D) frozenFallSpeed = 0.0D;
    }

    private void applyHeavyWaterSinking() {
        if (isInWater()) move(MoverType.SELF, new Vec3(0.0D, -0.08D, 0.0D));
    }

    private void chaseImmediately(LivingEntity target, FrozenPose preTickPose) {
        Vec3 toTarget = target.position().subtract(position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        double distance = horizontal.length();
        if (distance <= STOP_DISTANCE) {
            stopAndLock(preTickPose);
            trySnapAttack(target);
            return;
        }
        entityData.set(SCRAPING, true);
        faceTarget(target);
        boolean blinkClosed = target instanceof Player player && BlinkServerState.isBlinkClosed(player);
        double maxStep = blinkClosed ? BLINK_STEP_PER_TICK : DIRECT_STEP_PER_TICK;
        Vec3 step = chooseChaseStep(target, horizontal, distance, maxStep);
        if (step.lengthSqr() > 0.000001D) snapMove(step); else getNavigation().moveTo(target, 1.25D);
        applyHeavyWaterSinking();
        hardStopLocalMovement();
        trySnapAttack(target);
    }

    private Vec3 chooseChaseStep(LivingEntity target, Vec3 directHorizontal, double distance, double maxStep) {
        double stepDistance = Math.min(maxStep, distance - STOP_DISTANCE);
        if (stepDistance <= 0.001D || directHorizontal.lengthSqr() <= 0.000001D) return Vec3.ZERO;
        Vec3 directStep = directHorizontal.scale(1.0D / distance).scale(stepDistance);
        if (canMoveBy(directStep)) { getNavigation().stop(); return directStep; }
        Vec3 pathStep = pathStepToward(target, stepDistance);
        if (pathStep.lengthSqr() > 0.000001D && canMoveBy(pathStep)) return pathStep;
        return bestFallbackStep(target, directHorizontal, stepDistance);
    }

    private Vec3 pathStepToward(LivingEntity target, double stepDistance) {
        if (tickCount >= nextPathRecalculationTick || getNavigation().getPath() == null || getNavigation().isDone()) {
            Path path = getNavigation().createPath(target, 0);
            if (path != null) getNavigation().moveTo(path, 1.25D);
            nextPathRecalculationTick = tickCount + PATH_RECALCULATE_INTERVAL_TICKS;
        }
        Path path = getNavigation().getPath();
        if (path == null || path.isDone()) return Vec3.ZERO;
        Vec3 next = path.getNextEntityPos(this);
        Vec3 horizontal = new Vec3(next.x - getX(), 0.0D, next.z - getZ());
        if (horizontal.lengthSqr() <= PATH_NODE_REACHED_DISTANCE_SQR) {
            path.advance();
            if (path.isDone()) return Vec3.ZERO;
            next = path.getNextEntityPos(this);
            horizontal = new Vec3(next.x - getX(), 0.0D, next.z - getZ());
        }
        double length = horizontal.length();
        return length <= 0.001D ? Vec3.ZERO : horizontal.scale(1.0D / length).scale(Math.min(stepDistance, length));
    }

    private Vec3 bestFallbackStep(LivingEntity target, Vec3 directHorizontal, double stepDistance) {
        double length = directHorizontal.length();
        if (length <= 0.001D) return Vec3.ZERO;
        Vec3 base = directHorizontal.scale(1.0D / length);
        double[] angles = {35.0D, -35.0D, 70.0D, -70.0D, 110.0D, -110.0D, 160.0D, -160.0D};
        Vec3 best = Vec3.ZERO;
        double bestScore = Double.MAX_VALUE;
        for (double angle : angles) {
            Vec3 candidate = rotateHorizontal(base, angle).scale(stepDistance * 0.75D);
            if (!canMoveBy(candidate)) continue;
            double score = position().add(candidate).distanceToSqr(target.position());
            if (score < bestScore) { bestScore = score; best = candidate; }
        }
        return best;
    }

    private Vec3 rotateHorizontal(Vec3 vector, double degrees) {
        double radians = Math.toRadians(degrees), cos = Math.cos(radians), sin = Math.sin(radians);
        return new Vec3(vector.x * cos - vector.z * sin, 0.0D, vector.x * sin + vector.z * cos);
    }

    private boolean canMoveBy(Vec3 step) {
        return step != null && step.lengthSqr() > 0.000001D && level().noCollision(this, getBoundingBox().move(step));
    }

    private void snapMove(Vec3 step) {
        if (level().noCollision(this, getBoundingBox().move(step))) setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        else move(MoverType.SELF, step);
    }

    private boolean trySnapAttack(LivingEntity target) { return canSnapTarget(target) && snapTargetNeck(target); }
    private boolean isInSnapRange(LivingEntity target) {
        return target != null && hasVerticalAttackOverlap(target)
                && getBoundingBox().inflate(ATTACK_CONTACT_EXPAND, 0.12D, ATTACK_CONTACT_EXPAND).intersects(target.getBoundingBox());
    }
    private boolean hasVerticalAttackOverlap(LivingEntity target) {
        return getBoundingBox().maxY >= target.getBoundingBox().minY && getBoundingBox().minY <= target.getBoundingBox().maxY;
    }
    private boolean canSnapTarget(LivingEntity target) {
        return isActivated() && isValidTargetEntity(target) && tickCount >= nextAttackTick && isInSnapRange(target) && findObservingEntity() == null;
    }
    private boolean snapTargetNeck(LivingEntity target) {
        nextAttackTick = tickCount + ATTACK_COOLDOWN_TICKS;
        target.invulnerableTime = 0;
        boolean damaged = target.hurt(neckSnapDamageSource(), NECK_SNAP_DAMAGE);
        if (damaged) playNeckSnapSound();
        return damaged;
    }
    private DamageSource neckSnapDamageSource() {
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(NECK_SNAP_DAMAGE_TYPE), this);
    }

    private void faceTarget(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(position());
        if (toTarget.x * toTarget.x + toTarget.z * toTarget.z > 0.000001D) setManualYaw(yawTo(toTarget));
    }
    private float yawTo(Vec3 vector) { return (float) (Mth.atan2(vector.z, vector.x) * Mth.RAD_TO_DEG) - 90.0F; }
    private void setManualYaw(float yaw) { yaw = Mth.wrapDegrees(yaw); entityData.set(MANUAL_YAW, yaw); applyYaw(yaw); }
    private void applyClientManualRotation() {
        if (isScraping()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                Vec3 toPlayer = mc.player.position().subtract(position());
                if (toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z > 0.000001D) { applyYaw(yawTo(toPlayer)); return; }
            }
        }
        applyYaw(entityData.get(MANUAL_YAW));
    }
    private void applyYaw(float yaw) {
        yaw = Mth.wrapDegrees(yaw);
        setYRot(yaw); yRotO = yaw; yBodyRot = yaw; yBodyRotO = yaw; yHeadRot = yaw; yHeadRotO = yaw;
        setXRot(0.0F); xRotO = 0.0F;
    }
    private void hardStopLocalMovement() {
        setDeltaMovement(Vec3.ZERO); xxa = yya = zza = 0.0F; setSpeed(0.0F);
        xo = getX(); yo = getY(); zo = getZ(); hasImpulse = true;
    }
    private void playNeckSnapSound() {
        level().playSound(null, getX(), getY(), getZ(), Scp173Sounds.NECK_SNAP.get(), SoundSource.HOSTILE, 1.0F,
                0.96F + getRandom().nextFloat() * 0.08F);
    }
    private FrozenPose capturePose() { return new FrozenPose(getX(), getY(), getZ(), getXRot(), getYRot()); }
    private void restorePose(FrozenPose pose) {
        if (pose == null) return;
        absMoveTo(pose.x(), pose.y(), pose.z(), pose.yRot(), 0.0F);
        applyYaw(pose.yRot());
        hardStopLocalMovement();
    }
    private record FrozenPose(double x, double y, double z, float xRot, float yRot) { }
}
