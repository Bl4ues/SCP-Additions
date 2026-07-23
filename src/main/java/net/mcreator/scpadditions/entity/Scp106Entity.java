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
import net.mcreator.scpadditions.roamer.Scp106EmergenceLocator;
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
    private static final double AMBUSH_DISTANCE = 16.0D;
    private static final double AMBUSH_DISTANCE_SQR =
            AMBUSH_DISTANCE * AMBUSH_DISTANCE;
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
    private static final int AMBUSH_DISTANCE_TICKS = 20;
    private static final int AMBUSH_COOLDOWN_TICKS = 8 * 20;
    private static final int TARGET_LOST_DESPAWN_TICKS = 10 * 20;

    private static final RawAnimation IDLE_ANIMATION =
            RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION =
            RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION =
            RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation PHASE_GROUND_ANIMATION =
            RawAnimation.begin().thenPlay("phase_ground");
    private static final RawAnimation EMERGE_GROUND_ANIMATION =
            RawAnimation.begin().thenPlay("emerge_ground");
    private static final RawAnimation EMERGE_WALL_ANIMATION =
            RawAnimation.begin().thenPlay("emerge_wall");

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    private int attackTicks;
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
    private byte clientPreviousState = -1;

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
        entityData.define(ENCOUNTER_STATE, HUNTING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Scp106EncounterState", getEncounterState());
        tag.putInt("Scp106StateTicks", stateTicks);
        tag.putInt("Scp106InterestTicks", interestTicksRemaining);
        tag.putInt("Scp106AmbushCooldown", ambushCooldownTicks);
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
        farDistanceTicks = Math.max(0,
                tag.getInt("Scp106FarDistanceTicks"));
        vanishForDespawn = tag.getBoolean("Scp106VanishForDespawn");
        huntedPlayerId = tag.hasUUID("Scp106HuntedPlayer")
                ? tag.getUUID("Scp106HuntedPlayer") : null;
        entityData.set(ATTACKING, false);
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

        if (level().isClientSide) {
            tickClientVisuals();
            return;
        }

        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }

        if (ambushCooldownTicks > 0) ambushCooldownTicks--;

        switch (getEncounterState()) {
            case EMERGING_GROUND, EMERGING_WALL -> tickEmergence();
            case PHASE_TRAVEL -> tickPhaseTravel();
            case VANISHING -> tickVanish();
            default -> tickHunt();
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

    private void tickClientVisuals() {
        byte state = getEncounterState();
        if (state != clientPreviousState) {
            if (state == EMERGING_GROUND || state == VANISHING) {
                spawnPortalParticle(false);
            } else if (state == EMERGING_WALL) {
                spawnPortalParticle(true);
            }
            clientPreviousState = state;
        }

        if (state == HUNTING) {
            spawnCorrosionTrail();
        }
    }

    private void spawnPortalParticle(boolean wall) {
        Vec3 normal;
        double x = getX();
        double y;
        double z = getZ();
        if (wall) {
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            Vec3 outward =
                    new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
            normal = outward;
            x -= outward.x * 0.46D;
            y = getY() + 1.0D;
            z -= outward.z * 0.46D;
        } else {
            normal = new Vec3(0.0D, 1.0D, 0.0D);
            y = getY() + 0.018D;
        }

        level().addParticle(
                ScpAdditionsModParticleTypes.SCP_106_PORTAL.get(),
                x, y, z, normal.x, normal.y, normal.z);
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
            if (++noTargetTicks >= TARGET_LOST_DESPAWN_TICKS) {
                beginVanish(true);
            }
            return;
        }
        noTargetTicks = 0;

        setTarget(player);
        getLookControl().setLookAt(player, 35.0F, 20.0F);

        if (isAttacking()) {
            tickAttack(player);
            return;
        }

        boolean hasClearSight = hasLineOfSight(player);
        if (isWithinMeleeGap(player, ATTACK_START_GAP) && hasClearSight) {
            startAttack();
            return;
        }

        updateAmbushPressure(player);
        if (farDistanceTicks >= AMBUSH_DISTANCE_TICKS
                && ambushCooldownTicks <= 0) {
            beginVanish(false);
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

    private void pursueDirectly(Player player) {
        getNavigation().stop();
        getMoveControl().setWantedPosition(
                player.getX(), player.getY(), player.getZ(),
                PURSUIT_SPEED_MODIFIER);
    }

    private void startAttack() {
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
        setEncounterState(PHASE_TRAVEL);
        phaseEntryGraceTicks = PHASE_ENTRY_GRACE_TICKS;
        phaseExitClearTicks = 0;
        blockedSightTicks = 0;
        stuckTicks = 0;
        getNavigation().stop();
        setNoGravity(false);
        noPhysics = true;
    }

    private void tickPhaseTravel() {
        if (!tickInterest()) return;

        Player player = resolveHuntedPlayer();
        if (player == null) {
            if (++noTargetTicks >= TARGET_LOST_DESPAWN_TICKS) {
                beginVanish(true);
            }
            return;
        }
        noTargetTicks = 0;
        setTarget(player);
        getLookControl().setLookAt(player, 24.0F, 16.0F);

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
        Placement placement = null;
        if (player != null && level() instanceof ServerLevel serverLevel) {
            placement = Scp106EmergenceLocator.findAmbush(
                    serverLevel, player, random);
            if (placement == null) {
                placement = Scp106EmergenceLocator.findInitial(
                        serverLevel, player, random);
            }
        }

        if (placement == null) {
            startEmergence(Emergence.GROUND);
            ambushCooldownTicks = AMBUSH_COOLDOWN_TICKS;
            farDistanceTicks = 0;
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
            Player hunted =
                    serverLevel.getPlayerByUUID(huntedPlayerId);
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
                    BlockState state =
                            level().getBlockState(mutable);
                    VoxelShape shape =
                            state.getCollisionShape(level(), mutable);
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

    private boolean isWithinMeleeGap(
            Entity target, double maximumGap) {
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
        if (!onGround()
                || tickCount % TRAIL_PARTICLE_INTERVAL != 0) {
            return;
        }

        Vec3 movement = getDeltaMovement();
        Vec3 horizontal =
                new Vec3(movement.x, 0.0D, movement.z);
        if (horizontal.lengthSqr() < 0.0004D) return;

        Vec3 behind = horizontal.normalize().scale(-0.28D);
        Vec3 sideways =
                new Vec3(-horizontal.z, 0.0D, horizontal.x)
                        .normalize()
                        .scale((random.nextDouble() - 0.5D) * 0.34D);
        double particleX = getX() + behind.x + sideways.x;
        double particleY = getY() + 0.018D;
        double particleZ = getZ() + behind.z + sideways.z;

        level().addParticle(
                ScpAdditionsModParticleTypes.SCP_106_CORROSION.get(),
                particleX, particleY, particleZ,
                0.0D, 0.0D, 0.0D);
    }

    public boolean isAttacking() {
        return entityData.get(ATTACKING);
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
                    0,
                    false,
                    false,
                    true), this);
            if (interestTicksRemaining > 0) {
                interestTicksRemaining =
                        Math.min(180 * 20,
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
    public boolean removeWhenFarAway(
            double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Scp106Entity> movementController =
                new AnimationController<>(
                        this, "movement", 2, state -> {
                    byte encounterState = getEncounterState();
                    if (encounterState == EMERGING_GROUND) {
                        return state.setAndContinue(
                                EMERGE_GROUND_ANIMATION);
                    }
                    if (encounterState == EMERGING_WALL) {
                        return state.setAndContinue(
                                EMERGE_WALL_ANIMATION);
                    }
                    if (encounterState == VANISHING) {
                        return state.setAndContinue(
                                PHASE_GROUND_ANIMATION);
                    }
                    if (isAttacking()) {
                        return state.setAndContinue(
                                ATTACK_ANIMATION);
                    }
                    return state.setAndContinue(
                            state.isMoving()
                                    ? WALK_ANIMATION
                                    : IDLE_ANIMATION);
                });
        movementController.setAnimationSpeedHandler(entity -> {
            byte state = entity.getEncounterState();
            if (state == EMERGING_GROUND
                    || state == EMERGING_WALL
                    || state == VANISHING
                    || entity.isAttacking()) {
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
