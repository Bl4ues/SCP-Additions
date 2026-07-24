package net.mcreator.scpadditions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.init.ScpAdditionsModParticleTypes;
import net.mcreator.scpadditions.roamer.Scp106CorrosionFieldManager;
import net.mcreator.scpadditions.roamer.Scp106EmergenceLocator;
import net.mcreator.scpadditions.roamer.Scp106PhasePortalTracker;
import net.mcreator.scpadditions.roamer.Scp106SpawnSuppression;
import net.mcreator.scpadditions.roamer.Scp106EmergenceLocator.Emergence;
import net.mcreator.scpadditions.roamer.Scp106EmergenceLocator.Placement;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class Scp106Entity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(Scp106Entity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RANGED_ATTACKING =
            SynchedEntityData.defineId(Scp106Entity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> ENCOUNTER_STATE =
            SynchedEntityData.defineId(Scp106Entity.class,
                    EntityDataSerializers.BYTE);

    private static final byte HUNTING = 0;
    private static final byte EMERGING_GROUND = 1;
    private static final byte EMERGING_WALL = 2;
    private static final byte PHASE_TRAVEL = 3;
    private static final byte VANISHING = 4;

    public static final double MOVEMENT_SPEED = 0.245D;
    private static final double PURSUIT_SPEED_MODIFIER = 1.0D;
    private static final double ATTACK_PURSUIT_SPEED_MODIFIER = 0.84D;
    private static final double PURSUIT_RANGE = 128.0D;
    private static final double PURSUIT_RANGE_SQR =
            PURSUIT_RANGE * PURSUIT_RANGE;
    private static final double DIRECT_PURSUIT_VERTICAL_LIMIT = 1.35D;
    private static final double ATTACK_START_GAP = 0.16D;
    private static final double ATTACK_HIT_GAP = 0.38D;
    private static final double WALK_ANIMATION_SPEED = 1.38D;
    private static final double PHASE_MOVEMENT_SPEED =
            MOVEMENT_SPEED * 0.5D;
    private static final double AMBUSH_DISTANCE = 8.0D;
    private static final double AMBUSH_DISTANCE_SQR =
            AMBUSH_DISTANCE * AMBUSH_DISTANCE;
    private static final double AMBUSH_HARD_DISTANCE = 20.0D;
    private static final double AMBUSH_HARD_DISTANCE_SQR =
            AMBUSH_HARD_DISTANCE * AMBUSH_HARD_DISTANCE;
    private static final double RANGED_MIN_DISTANCE_SQR = 6.0D * 6.0D;
    private static final double RANGED_MAX_DISTANCE_SQR = 12.0D * 12.0D;
    private static final int PATH_REFRESH_INTERVAL = 10;
    private static final int ATTACK_HIT_TICK = 15;
    private static final int ATTACK_DURATION_TICKS = 34;
    private static final int WITHER_DURATION_TICKS = 5 * 20;
    private static final int TRAIL_PARTICLE_INTERVAL = 5;
    private static final int EMERGE_GROUND_TICKS = 54;
    private static final int EMERGE_WALL_TICKS = 55;
    private static final int VANISH_TICKS = 49;
    private static final int BLOCKED_PHASE_DELAY_TICKS = 16;
    private static final int STUCK_PHASE_DELAY_TICKS = 30;
    private static final int PHASE_ENTRY_GRACE_TICKS = 12;
    private static final int PHASE_EXIT_CLEAR_TICKS = 6;
    private static final int AMBUSH_DISTANCE_TICKS = 12;
    private static final int AMBUSH_RETRY_TICKS = 8;
    private static final int AMBUSH_COOLDOWN_TICKS = 8 * 20;
    private static final int RANGED_PREPARE_TICKS = 8;
    private static final int RANGED_AIM_LOCK_TICK = 38;
    private static final int RANGED_RELEASE_TICK = 42;
    private static final int RANGED_ATTACK_DURATION_TICKS = 69;
    private static final int RANGED_SEGMENTS = 15;
    private static final double RANGED_SEGMENT_SPACING = 0.65D;
    private static final int RANGED_COOLDOWN_TICKS = 8 * 20;
    private static final int TESLA_SUPPRESSION_TICKS = 10 * 60 * 20;

    private static final RawAnimation IDLE_ANIMATION =
            RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION =
            RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION =
            RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation RANGED_ATTACK_ANIMATION =
            RawAnimation.begin().thenPlay("ranged_attack");
    private static final RawAnimation PHASE_GROUND_ANIMATION =
            RawAnimation.begin().thenPlay("phase_ground");
    private static final RawAnimation EMERGE_GROUND_ANIMATION =
            RawAnimation.begin().thenPlay("emerge_ground");
    private static final RawAnimation EMERGE_WALL_ANIMATION =
            RawAnimation.begin().thenPlay("emerge_wall");

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    private int attackTicks;
    private int rangedAttackTicks;
    private int rangedCooldownTicks;
    private int rangedOpportunityTicks;
    private int stateTicks;
    private int interestTicksRemaining = -1;
    private int farDistanceTicks;
    private int blockedSightTicks;
    private int stuckTicks;
    private int noTargetTicks;
    private int phaseEntryGraceTicks;
    private int phaseExitClearTicks;
    private int ambushCooldownTicks;
    private UUID huntedPlayerId;
    private boolean vanishForDespawn;
    private Vec3 rangedLockedDirection = Vec3.ZERO;
    private boolean rangedHit;

    public Scp106Entity(EntityType<? extends Scp106Entity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        applyCurrentMovementSpeed();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, PURSUIT_RANGE)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACKING, false);
        entityData.define(RANGED_ATTACKING, false);
        entityData.define(ENCOUNTER_STATE, HUNTING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Scp106EncounterState", getEncounterState());
        tag.putInt("Scp106StateTicks", stateTicks);
        tag.putInt("Scp106InterestTicks", interestTicksRemaining);
        tag.putInt("Scp106AmbushCooldown", ambushCooldownTicks);
        tag.putInt("Scp106RangedCooldown", rangedCooldownTicks);
        tag.putInt("Scp106FarDistanceTicks", farDistanceTicks);
        tag.putBoolean("Scp106VanishForDespawn", vanishForDespawn);
        if (huntedPlayerId != null) {
            tag.putUUID("Scp106HuntedPlayer", huntedPlayerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        applyCurrentMovementSpeed();
        byte savedState = tag.getByte("Scp106EncounterState");
        setEncounterState((byte) Mth.clamp(savedState, HUNTING, VANISHING));
        stateTicks = Math.max(0, tag.getInt("Scp106StateTicks"));
        interestTicksRemaining = tag.contains("Scp106InterestTicks")
                ? tag.getInt("Scp106InterestTicks") : -1;
        ambushCooldownTicks = Math.max(0,
                tag.getInt("Scp106AmbushCooldown"));
        rangedCooldownTicks = Math.max(0,
                tag.getInt("Scp106RangedCooldown"));
        farDistanceTicks = Math.max(0,
                tag.getInt("Scp106FarDistanceTicks"));
        vanishForDespawn = tag.getBoolean("Scp106VanishForDespawn");
        huntedPlayerId = tag.hasUUID("Scp106HuntedPlayer")
                ? tag.getUUID("Scp106HuntedPlayer") : null;
        entityData.set(ATTACKING, false);
        entityData.set(RANGED_ATTACKING, false);
    }

    private void applyCurrentMovementSpeed() {
        AttributeInstance movementSpeed =
                getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(MOVEMENT_SPEED);
        }
    }

    public void beginNaturalEncounter(ServerPlayer target,
            Emergence emergence) {
        if (target == null || level().isClientSide) return;
        huntedPlayerId = target.getUUID();
        setTarget(target);
        interestTicksRemaining = rollInterestTicks();
        ambushCooldownTicks = 0;
        rangedCooldownTicks = 0;
        startEmergence(emergence);
    }

    private int rollInterestTicks() {
        float roll = random.nextFloat();
        if (roll < 0.25F) {
            return 18 * 20 + random.nextInt(12 * 20);
        }
        if (roll < 0.82F) {
            return 45 * 20 + random.nextInt(76 * 20);
        }
        return 121 * 20 + random.nextInt(60 * 20);
    }

    @Override
    public void tick() {
        preparePhysicsForCurrentState();
        super.tick();

        if (level().isClientSide) return;

        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (shouldIdleForCreativeTarget()) {
            idleForCreativeTarget();
            return;
        }

        if (ambushCooldownTicks > 0) ambushCooldownTicks--;
        if (rangedCooldownTicks > 0) rangedCooldownTicks--;

        switch (getEncounterState()) {
            case EMERGING_GROUND, EMERGING_WALL -> tickEmergence();
            case PHASE_TRAVEL -> tickPhaseTravel();
            case VANISHING -> tickVanish();
            default -> tickHunt();
        }

        if (getEncounterState() == HUNTING && !isRangedAttacking()) {
            spawnCorrosionTrail();
        }
    }

    private void preparePhysicsForCurrentState() {
        byte state = getEncounterState();
        if (state == EMERGING_GROUND || state == EMERGING_WALL
                || state == VANISHING) {
            noPhysics = true;
            setNoGravity(true);
            stopHorizontalMovement();
            return;
        }

        if (state == PHASE_TRAVEL) {
            getNavigation().stop();
            setSpeed(0.0F);
            boolean insideSolid = overlapsSolidBlock();
            boolean enteringSolid = phaseEntryGraceTicks > 0;
            noPhysics = insideSolid || enteringSolid;
            setNoGravity(insideSolid);
            return;
        }

        noPhysics = false;
        setNoGravity(false);
    }

    private void tickEmergence() {
        getNavigation().stop();
        stopHorizontalMovement();
        if (stateTicks > 0) stateTicks--;
        if (stateTicks > 0) return;

        setEncounterState(HUNTING);
        noPhysics = false;
        setNoGravity(false);
        farDistanceTicks = 0;
        blockedSightTicks = 0;
        stuckTicks = 0;
        phaseExitClearTicks = 0;
    }

    private void tickHunt() {
        if (!tickInterest()) return;

        Player player = resolveHuntedPlayer();
        if (player == null) {
            setTarget(null);
            getNavigation().stop();
            beginVanish(true);
            return;
        }
        noTargetTicks = 0;

        setTarget(player);
        getLookControl().setLookAt(player, 35.0F, 20.0F);
        updateAmbushPressure(player);

        if (isRangedAttacking()) {
            tickRangedAttack(player);
            return;
        }

        if (distanceToSqr(player) >= AMBUSH_HARD_DISTANCE_SQR) {
            beginVanish(false);
            return;
        }

        if (isRangedOpportunity(player)) {
            rangedOpportunityTicks++;
        } else {
            rangedOpportunityTicks = 0;
        }
        if (rangedOpportunityTicks >= RANGED_PREPARE_TICKS) {
            startRangedAttack(player);
            return;
        }

        if (shouldAmbush(player)) {
            beginVanish(false);
            return;
        }

        if (isAttacking()) {
            tickAttack(player);
            return;
        }

        boolean hasClearSight = hasLineOfSight(player);
        if (isWithinMeleeGap(player, ATTACK_START_GAP) && hasClearSight) {
            startAttack();
            return;
        }

        if (!hasClearSight) {
            blockedSightTicks++;
        } else {
            blockedSightTicks = 0;
        }

        double movementSqr =
                getDeltaMovement().horizontalDistanceSqr();
        if (distanceToSqr(player) > 16.0D
                && movementSqr < 0.0004D) {
            stuckTicks++;
        } else {
            stuckTicks = Math.max(0, stuckTicks - 2);
        }

        if (blockedSightTicks >= BLOCKED_PHASE_DELAY_TICKS
                || stuckTicks >= STUCK_PHASE_DELAY_TICKS) {
            beginPhaseTravel();
            return;
        }

        if (hasClearSight
                && Math.abs(player.getY() - getY())
                <= DIRECT_PURSUIT_VERTICAL_LIMIT) {
            pursueDirectly(player);
            return;
        }

        if (getNavigation().isDone()
                || tickCount % PATH_REFRESH_INTERVAL == 0) {
            getNavigation().moveTo(
                    player.getX(), player.getY(), player.getZ(),
                    PURSUIT_SPEED_MODIFIER);
        }
    }

    private boolean tickInterest() {
        if (interestTicksRemaining < 0) {
            interestTicksRemaining = rollInterestTicks();
        }
        if (interestTicksRemaining > 0) interestTicksRemaining--;
        if (interestTicksRemaining > 0) return true;
        beginVanish(true);
        return false;
    }

    private void updateAmbushPressure(Player player) {
        if (distanceToSqr(player) >= AMBUSH_DISTANCE_SQR) {
            farDistanceTicks++;
        } else {
            farDistanceTicks = Math.max(0, farDistanceTicks - 3);
        }
    }

    private boolean shouldAmbush(Player player) {
        double distanceSqr = distanceToSqr(player);
        return distanceSqr >= AMBUSH_HARD_DISTANCE_SQR
                || (farDistanceTicks >= AMBUSH_DISTANCE_TICKS
                && ambushCooldownTicks <= 0);
    }

    private void pursueDirectly(Player player) {
        getNavigation().stop();
        getMoveControl().setWantedPosition(
                player.getX(), player.getY(), player.getZ(),
                PURSUIT_SPEED_MODIFIER);
    }

    private void startAttack() {
        cancelRangedAttack();
        attackTicks = 0;
        entityData.set(ATTACKING, true);
    }

    private void tickAttack(LivingEntity target) {
        getLookControl().setLookAt(target, 45.0F, 25.0F);
        pursueDuringAttack(target);
        attackTicks++;

        if (attackTicks == ATTACK_HIT_TICK
                && isWithinMeleeGap(target, ATTACK_HIT_GAP)) {
            doHurtTarget(target);
        }

        if (attackTicks >= ATTACK_DURATION_TICKS) {
            attackTicks = 0;
            entityData.set(ATTACKING, false);
        }
    }

    private void pursueDuringAttack(LivingEntity target) {
        if (hasLineOfSight(target)
                && Math.abs(target.getY() - getY())
                <= DIRECT_PURSUIT_VERTICAL_LIMIT) {
            getNavigation().stop();
            getMoveControl().setWantedPosition(
                    target.getX(), target.getY(), target.getZ(),
                    ATTACK_PURSUIT_SPEED_MODIFIER);
            return;
        }

        if (getNavigation().isDone() || tickCount % 5 == 0) {
            getNavigation().moveTo(
                    target.getX(), target.getY(), target.getZ(),
                    ATTACK_PURSUIT_SPEED_MODIFIER);
        }
    }

    private void beginPhaseTravel() {
        entityData.set(ATTACKING, false);
        attackTicks = 0;
        cancelRangedAttack();
        setEncounterState(PHASE_TRAVEL);
        phaseEntryGraceTicks = PHASE_ENTRY_GRACE_TICKS;
        phaseExitClearTicks = 0;
        blockedSightTicks = 0;
        stuckTicks = 0;
        getNavigation().stop();
        setNoGravity(false);
        noPhysics = true;
        Scp106PhasePortalTracker.begin(this);
    }

    private void tickPhaseTravel() {
        if (!tickInterest()) return;

        Player player = resolveHuntedPlayer();
        if (player == null) {
            beginVanish(true);
            return;
        }
        noTargetTicks = 0;
        setTarget(player);
        getLookControl().setLookAt(player, 24.0F, 16.0F);

        updateAmbushPressure(player);
        if (shouldAmbush(player)) {
            beginVanish(false);
            return;
        }

        boolean insideSolid = overlapsSolidBlock();
        if (phaseEntryGraceTicks > 0) phaseEntryGraceTicks--;

        Vec3 toTarget = player.position().subtract(position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() > 0.0001D) {
            horizontal = horizontal.normalize()
                    .scale(PHASE_MOVEMENT_SPEED);
        }

        Vec3 currentMovement = getDeltaMovement();
        double vertical = currentMovement.y;
        if (insideSolid) {
            vertical = Mth.clamp(toTarget.y, -0.10D, 0.10D);
        }
        setDeltaMovement(horizontal.x, vertical, horizontal.z);

        boolean safelyOut = !insideSolid
                && phaseEntryGraceTicks <= 0
                && onGround()
                && hasLineOfSight(player);
        if (safelyOut) {
            phaseExitClearTicks++;
        } else {
            phaseExitClearTicks = 0;
        }

        if (phaseExitClearTicks >= PHASE_EXIT_CLEAR_TICKS) {
            setEncounterState(HUNTING);
            noPhysics = false;
            setNoGravity(false);
            blockedSightTicks = 0;
            stuckTicks = 0;
            phaseExitClearTicks = 0;
        }
    }

    private void beginVanish(boolean despawnAfterward) {
        if (getEncounterState() == VANISHING) return;
        vanishForDespawn = despawnAfterward;
        stateTicks = VANISH_TICKS;
        setEncounterState(VANISHING);
        entityData.set(ATTACKING, false);
        attackTicks = 0;
        cancelRangedAttack();
        getNavigation().stop();
        stopHorizontalMovement();
        noPhysics = true;
        setNoGravity(true);
    }

    private void tickVanish() {
        getNavigation().stop();
        stopHorizontalMovement();
        if (stateTicks > 0) stateTicks--;
        if (stateTicks > 0) return;

        if (vanishForDespawn) {
            discard();
            return;
        }

        Player player = resolveHuntedPlayer();
        if (player == null) {
            discard();
            return;
        }
        noTargetTicks = 0;

        Placement placement = null;
        if (level() instanceof ServerLevel serverLevel) {
            placement = Scp106EmergenceLocator.findAmbush(
                    serverLevel, player, random);
            if (placement == null) {
                placement = Scp106EmergenceLocator.findInitial(
                        serverLevel, player, random);
            }
        }

        if (placement == null) {
            stateTicks = AMBUSH_RETRY_TICKS;
            return;
        }

        moveTo(placement.position().x, placement.position().y,
                placement.position().z, placement.yaw(), 0.0F);
        setYBodyRot(placement.yaw());
        setYHeadRot(placement.yaw());
        startEmergence(placement.emergence());
        ambushCooldownTicks = AMBUSH_COOLDOWN_TICKS;
        farDistanceTicks = 0;
    }

    private void startEmergence(Emergence emergence) {
        vanishForDespawn = false;
        entityData.set(ATTACKING, false);
        attackTicks = 0;
        cancelRangedAttack();
        if (emergence == Emergence.WALL) {
            setEncounterState(EMERGING_WALL);
            stateTicks = EMERGE_WALL_TICKS;
        } else {
            setEncounterState(EMERGING_GROUND);
            stateTicks = EMERGE_GROUND_TICKS;
        }
        noPhysics = true;
        setNoGravity(true);
        getNavigation().stop();
        stopHorizontalMovement();
    }

    private Player resolveHuntedPlayer() {
        if (level() instanceof ServerLevel serverLevel
                && huntedPlayerId != null) {
            Player hunted = serverLevel.getPlayerByUUID(huntedPlayerId);
            if (isValidHuntTarget(hunted)) return hunted;
        }

        LivingEntity current = getTarget();
        if (current instanceof Player player
                && isValidHuntTarget(player)) {
            huntedPlayerId = player.getUUID();
            return player;
        }

        Player nearest = findNearestPlayer();
        if (nearest != null) huntedPlayerId = nearest.getUUID();
        return nearest;
    }

    private boolean isValidHuntTarget(Player player) {
        return player != null
                && player.isAlive()
                && !player.isRemoved()
                && !player.isCreative()
                && !player.isSpectator()
                && player.level() == level();
    }

    private Player findNearestPlayer() {
        AABB area = getBoundingBox().inflate(PURSUIT_RANGE);
        Player nearest = null;
        double nearestDistanceSqr = PURSUIT_RANGE_SQR;

        for (Player player : level().getEntitiesOfClass(
                Player.class, area, this::isValidHuntTarget)) {
            double distanceSqr = distanceToSqr(player);
            if (distanceSqr < nearestDistanceSqr) {
                nearest = player;
                nearestDistanceSqr = distanceSqr;
            }
        }
        return nearest;
    }

    private boolean shouldIdleForCreativeTarget() {
        if (getEncounterState() == EMERGING_GROUND
                || getEncounterState() == EMERGING_WALL
                || (getEncounterState() == VANISHING && vanishForDespawn)) {
            return false;
        }

        Player hunted = rawHuntedPlayer();
        if (hunted == null || !hunted.isAlive() || !hunted.isCreative()
                || hunted.level() != level()) {
            return false;
        }

        Player replacement = findNearestPlayer();
        if (replacement != null) {
            huntedPlayerId = replacement.getUUID();
            setTarget(replacement);
            return false;
        }
        return true;
    }

    private Player rawHuntedPlayer() {
        if (level() instanceof ServerLevel serverLevel
                && huntedPlayerId != null) {
            return serverLevel.getPlayerByUUID(huntedPlayerId);
        }
        return getTarget() instanceof Player player ? player : null;
    }

    private void idleForCreativeTarget() {
        setEncounterState(HUNTING);
        setTarget(null);
        entityData.set(ATTACKING, false);
        attackTicks = 0;
        cancelRangedAttack();
        getNavigation().stop();
        stopHorizontalMovement();
        noPhysics = false;
        setNoGravity(false);
        farDistanceTicks = 0;
        blockedSightTicks = 0;
        stuckTicks = 0;
    }

    private boolean isRangedOpportunity(Player player) {
        if (player == null || rangedCooldownTicks > 0 || !onGround()
                || !hasLineOfSight(player)) {
            return false;
        }
        double distanceSqr = distanceToSqr(player);
        return distanceSqr >= RANGED_MIN_DISTANCE_SQR
                && distanceSqr <= RANGED_MAX_DISTANCE_SQR
                && Math.abs(player.getY() - getY()) <= 1.75D;
    }

    private void startRangedAttack(Player target) {
        entityData.set(ATTACKING, false);
        attackTicks = 0;
        entityData.set(RANGED_ATTACKING, true);
        rangedAttackTicks = 0;
        rangedOpportunityTicks = 0;
        rangedHit = false;
        rangedLockedDirection = horizontalDirectionTo(target);
        getNavigation().stop();
        stopHorizontalMovement();
    }

    private void tickRangedAttack(Player target) {
        getNavigation().stop();
        stopHorizontalMovement();
        rangedAttackTicks++;

        if (rangedAttackTicks <= RANGED_AIM_LOCK_TICK) {
            rangedLockedDirection = horizontalDirectionTo(target);
            faceDirection(rangedLockedDirection);
            getLookControl().setLookAt(target, 50.0F, 25.0F);
        } else {
            faceDirection(rangedLockedDirection);
        }

        int segment = rangedAttackTicks - RANGED_RELEASE_TICK;
        if (segment >= 0 && segment < RANGED_SEGMENTS) {
            spawnRangedSegment(segment);
        }

        if (rangedAttackTicks >= RANGED_ATTACK_DURATION_TICKS) {
            entityData.set(RANGED_ATTACKING, false);
            rangedAttackTicks = 0;
            rangedCooldownTicks = RANGED_COOLDOWN_TICKS;
            farDistanceTicks = 0;
            ambushCooldownTicks = Math.max(ambushCooldownTicks, 3 * 20);
        }
    }

    private Vec3 horizontalDirectionTo(Entity target) {
        Vec3 direction = target.position().subtract(position());
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        return direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
    }

    private void faceDirection(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) return;
        float yaw = (float) (Mth.atan2(direction.z, direction.x)
                * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void spawnRangedSegment(int segment) {
        if (!(level() instanceof ServerLevel serverLevel)
                || rangedLockedDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        double distance = 0.75D + segment * RANGED_SEGMENT_SPACING;
        Vec3 point = position().add(rangedLockedDirection.scale(distance));
        double surfaceY = findCorrosionSurfaceY(point.x, point.y, point.z);
        Vec3 puddle = new Vec3(point.x, surfaceY + 0.025D, point.z);
        double sizeScale = 1.25D - segment * (0.35D / (RANGED_SEGMENTS - 1));

        serverLevel.sendParticles(
                ScpAdditionsModParticleTypes.SCP_106_CORROSION.get(),
                puddle.x, puddle.y, puddle.z,
                0, sizeScale, 0.0D, 0.0D, 1.0D);
        Scp106CorrosionFieldManager.addRanged(serverLevel, puddle);

        if (rangedHit) return;
        AABB hitbox = new AABB(puddle.x - 0.68D, puddle.y - 0.15D,
                puddle.z - 0.68D, puddle.x + 0.68D,
                puddle.y + 1.15D, puddle.z + 0.68D);
        for (Player player : serverLevel.getEntitiesOfClass(Player.class,
                hitbox, this::isValidHuntTarget)) {
            if (player.hurt(damageSources().mobAttack(this), 8.0F)) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER,
                        5 * 20, 0, false, false, true), this);
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        5 * 20, 0, false, false, true), this);
                rangedHit = true;
                break;
            }
        }
    }

    private double findCorrosionSurfaceY(double x, double referenceY,
            double z) {
        double best = referenceY;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int centerY = Mth.floor(referenceY);
        for (int y = centerY + 1; y >= centerY - 3; y--) {
            mutable.set(Mth.floor(x), y, Mth.floor(z));
            BlockState state = level().getBlockState(mutable);
            VoxelShape shape = state.getCollisionShape(level(), mutable);
            for (AABB local : shape.toAabbs()) {
                AABB world = local.move(mutable.getX(), mutable.getY(),
                        mutable.getZ());
                if (x < world.minX || x > world.maxX
                        || z < world.minZ || z > world.maxZ) {
                    continue;
                }
                double distance = Math.abs(world.maxY - referenceY);
                if (world.maxY <= referenceY + 1.25D
                        && distance < bestDistance) {
                    best = world.maxY;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private void cancelRangedAttack() {
        entityData.set(RANGED_ATTACKING, false);
        rangedAttackTicks = 0;
        rangedOpportunityTicks = 0;
        rangedLockedDirection = Vec3.ZERO;
        rangedHit = false;
    }

    private boolean overlapsSolidBlock() {
        AABB box = getBoundingBox().deflate(0.06D);
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX - 1.0E-7D);
        int maxY = Mth.floor(box.maxY - 1.0E-7D);
        int maxZ = Mth.floor(box.maxZ - 1.0E-7D);
        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    BlockState state = level().getBlockState(mutable);
                    VoxelShape shape = state.getCollisionShape(level(), mutable);
                    if (shape.isEmpty()) continue;
                    for (AABB shapeBox : shape.toAabbs()) {
                        if (shapeBox.move(x, y, z).intersects(box)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isWithinMeleeGap(Entity target, double maximumGap) {
        AABB selfBox = getBoundingBox();
        AABB targetBox = target.getBoundingBox();

        double xGap = Math.max(0.0D,
                Math.max(targetBox.minX - selfBox.maxX,
                        selfBox.minX - targetBox.maxX));
        double zGap = Math.max(0.0D,
                Math.max(targetBox.minZ - selfBox.maxZ,
                        selfBox.minZ - targetBox.maxZ));
        boolean verticallyAligned =
                selfBox.maxY + 0.35D >= targetBox.minY
                && targetBox.maxY + 0.35D >= selfBox.minY;

        return verticallyAligned
                && xGap * xGap + zGap * zGap
                <= maximumGap * maximumGap;
    }

    private void stopHorizontalMovement() {
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(0.0D, movement.y, 0.0D);
        setSpeed(0.0F);
    }

    private void spawnCorrosionTrail() {
        if (!(level() instanceof ServerLevel serverLevel) || !onGround()
                || tickCount % TRAIL_PARTICLE_INTERVAL != 0) {
            return;
        }

        Vec3 movement = getDeltaMovement();
        Vec3 horizontal = new Vec3(movement.x, 0.0D, movement.z);
        if (horizontal.lengthSqr() < 0.0004D) return;

        Vec3 behind = horizontal.normalize().scale(-0.28D);
        Vec3 sideways = new Vec3(-horizontal.z, 0.0D, horizontal.x)
                .normalize()
                .scale((random.nextDouble() - 0.5D) * 0.34D);
        Vec3 puddle = new Vec3(getX() + behind.x + sideways.x,
                getY() + 0.025D, getZ() + behind.z + sideways.z);

        serverLevel.sendParticles(
                ScpAdditionsModParticleTypes.SCP_106_CORROSION.get(),
                puddle.x, puddle.y, puddle.z,
                0, 1.0D, 0.0D, 0.0D, 1.0D);
        Scp106CorrosionFieldManager.addTrail(serverLevel, puddle);
    }

    public boolean isAttacking() {
        return entityData.get(ATTACKING);
    }

    public boolean isRangedAttacking() {
        return entityData.get(RANGED_ATTACKING);
    }

    public byte getEncounterState() {
        return entityData.get(ENCOUNTER_STATE);
    }

    private void setEncounterState(byte state) {
        entityData.set(ENCOUNTER_STATE, state);
    }

    public boolean allowsHeadTracking() {
        byte state = getEncounterState();
        return state == HUNTING || state == PHASE_TRAVEL;
    }

    public void onTeslaGateHit() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (getEncounterState() == VANISHING && vanishForDespawn) return;
        Scp106SpawnSuppression.suppress(serverLevel.getServer(),
                TESLA_SUPPRESSION_TICKS);
        beginVanish(true);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        if (livingTarget instanceof Player player
                && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        boolean hurt = livingTarget.hurt(
                damageSources().mobAttack(this), 5.0F);
        if (hurt) {
            livingTarget.addEffect(new MobEffectInstance(
                    MobEffects.WITHER,
                    WITHER_DURATION_TICKS,
                    0, false, false, true), this);
            if (interestTicksRemaining > 0) {
                interestTicksRemaining = Math.min(180 * 20,
                        interestTicksRemaining + 10 * 20);
            }
        }
        return hurt;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Scp106Entity> movementController =
                new AnimationController<>(this, "movement", 2, state -> {
                    byte encounterState = getEncounterState();
                    if (encounterState == EMERGING_GROUND) {
                        return state.setAndContinue(EMERGE_GROUND_ANIMATION);
                    }
                    if (encounterState == EMERGING_WALL) {
                        return state.setAndContinue(EMERGE_WALL_ANIMATION);
                    }
                    if (encounterState == VANISHING) {
                        return state.setAndContinue(PHASE_GROUND_ANIMATION);
                    }
                    if (isRangedAttacking()) {
                        return state.setAndContinue(RANGED_ATTACK_ANIMATION);
                    }
                    if (isAttacking()) {
                        return state.setAndContinue(ATTACK_ANIMATION);
                    }
                    return state.setAndContinue(
                            state.isMoving() ? WALK_ANIMATION : IDLE_ANIMATION);
                });
        movementController.setAnimationSpeedHandler(entity -> {
            byte state = entity.getEncounterState();
            if (state == EMERGING_GROUND
                    || state == EMERGING_WALL
                    || state == VANISHING
                    || entity.isAttacking()
                    || entity.isRangedAttacking()) {
                return 1.0D;
            }
            if (state == PHASE_TRAVEL) return 0.75D;
            return entity.getDeltaMovement()
                    .horizontalDistanceSqr() > 0.0004D
                    ? WALK_ANIMATION_SPEED
                    : 1.0D;
        });
        controllers.add(movementController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
