package net.mcreator.scpadditions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp106Entity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(Scp106Entity.class, EntityDataSerializers.BOOLEAN);

    public static final double MOVEMENT_SPEED = 0.29D;
    private static final double PURSUIT_SPEED_MODIFIER = 1.0D;
    private static final double PURSUIT_RANGE = 48.0D;
    private static final double PURSUIT_RANGE_SQR = PURSUIT_RANGE * PURSUIT_RANGE;
    private static final double ATTACK_START_DISTANCE_SQR = 2.35D * 2.35D;
    private static final double ATTACK_HIT_DISTANCE_SQR = 2.75D * 2.75D;
    private static final int PATH_REFRESH_INTERVAL = 5;
    private static final int ATTACK_HIT_TICK = 15;
    private static final int ATTACK_DURATION_TICKS = 34;
    private static final int WITHER_DURATION_TICKS = 5 * 20;
    private static final int TRAIL_PARTICLE_INTERVAL = 4;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int attackTicks;

    public Scp106Entity(EntityType<? extends Scp106Entity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
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
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            spawnCorrosionTrail();
            return;
        }

        if (isAttacking()) {
            tickAttack();
        } else {
            pursueNearestPlayer();
        }
    }

    private void pursueNearestPlayer() {
        Player player = findNearestPlayer();
        if (player == null) {
            setTarget(null);
            getNavigation().stop();
            return;
        }

        setTarget(player);
        getLookControl().setLookAt(player, 35.0F, 30.0F);

        if (distanceToSqr(player) <= ATTACK_START_DISTANCE_SQR && hasLineOfSight(player)) {
            startAttack();
            return;
        }

        if (getNavigation().isDone() || tickCount % PATH_REFRESH_INTERVAL == 0) {
            getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), PURSUIT_SPEED_MODIFIER);
        }
    }

    private void startAttack() {
        attackTicks = 0;
        entityData.set(ATTACKING, true);
        getNavigation().stop();
        stopHorizontalMovement();
    }

    private void tickAttack() {
        getNavigation().stop();
        stopHorizontalMovement();

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.isRemoved()) {
            finishAttack();
            return;
        }

        getLookControl().setLookAt(target, 60.0F, 45.0F);
        attackTicks++;

        if (attackTicks == ATTACK_HIT_TICK
                && distanceToSqr(target) <= ATTACK_HIT_DISTANCE_SQR
                && hasLineOfSight(target)) {
            doHurtTarget(target);
        }

        if (attackTicks >= ATTACK_DURATION_TICKS) {
            finishAttack();
        }
    }

    private void finishAttack() {
        attackTicks = 0;
        entityData.set(ATTACKING, false);
    }

    private void stopHorizontalMovement() {
        Vec3 movement = getDeltaMovement();
        setDeltaMovement(0.0D, movement.y, 0.0D);
    }

    private Player findNearestPlayer() {
        AABB area = getBoundingBox().inflate(PURSUIT_RANGE);
        Player nearest = null;
        double nearestDistanceSqr = PURSUIT_RANGE_SQR;

        for (Player player : level().getEntitiesOfClass(Player.class, area,
                candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            double distanceSqr = distanceToSqr(player);
            if (distanceSqr < nearestDistanceSqr) {
                nearest = player;
                nearestDistanceSqr = distanceSqr;
            }
        }
        return nearest;
    }

    private void spawnCorrosionTrail() {
        if (isAttacking() || tickCount % TRAIL_PARTICLE_INTERVAL != 0) {
            return;
        }

        Vec3 movement = getDeltaMovement();
        Vec3 horizontal = new Vec3(movement.x, 0.0D, movement.z);
        if (horizontal.lengthSqr() < 0.0004D) {
            return;
        }

        Vec3 behind = horizontal.normalize().scale(-0.42D);
        double particleX = getX() + behind.x + (random.nextDouble() - 0.5D) * 0.30D;
        double particleY = getY() + 0.08D;
        double particleZ = getZ() + behind.z + (random.nextDouble() - 0.5D) * 0.30D;
        level().addParticle(ParticleTypes.SQUID_INK,
                particleX, particleY, particleZ, 0.0D, 0.01D, 0.0D);
    }

    public boolean isAttacking() {
        return entityData.get(ATTACKING);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(
                    MobEffects.WITHER, WITHER_DURATION_TICKS, 0), this);
        }
        return hurt;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 2, state -> {
            if (isAttacking()) {
                return state.setAndContinue(ATTACK_ANIMATION);
            }
            return state.setAndContinue(state.isMoving() ? WALK_ANIMATION : IDLE_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
