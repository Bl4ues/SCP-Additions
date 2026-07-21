package net.mcreator.scpadditions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp106Entity extends PathfinderMob implements GeoEntity {
    public static final double MOVEMENT_SPEED = 0.18D;
    private static final double PURSUIT_SPEED_MODIFIER = 1.0D;
    private static final double PURSUIT_RANGE = 48.0D;
    private static final double PURSUIT_RANGE_SQR = PURSUIT_RANGE * PURSUIT_RANGE;
    private static final double PURSUIT_STOP_DISTANCE_SQR = 0.75D * 0.75D;
    private static final int PATH_REFRESH_INTERVAL = 5;
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public Scp106Entity(EntityType<? extends Scp106Entity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, PURSUIT_RANGE)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        pursueNearestPlayer();
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

        if (distanceToSqr(player) <= PURSUIT_STOP_DISTANCE_SQR) {
            getNavigation().stop();
            return;
        }

        if (getNavigation().isDone() || tickCount % PATH_REFRESH_INTERVAL == 0) {
            getNavigation().moveTo(player.getX(), player.getY(), player.getZ(), PURSUIT_SPEED_MODIFIER);
        }
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

    @Override
    public boolean doHurtTarget(Entity target) {
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIMATION : IDLE_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
