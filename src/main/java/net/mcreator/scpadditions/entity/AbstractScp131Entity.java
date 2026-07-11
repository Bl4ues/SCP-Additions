package net.mcreator.scpadditions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.network.ScpEntityNetwork;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractScp131Entity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(AbstractScp131Entity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
    private static final String OWNER_TAG = "FollowOwner";
    private static final double SCP_173_SCAN_RANGE = 15.0D;
    private static final double SCP_173_SCAN_RANGE_SQR = SCP_173_SCAN_RANGE * SCP_173_SCAN_RANGE;
    private static final double SCP_173_WATCH_DISTANCE = 1.2D;
    private static final double SCP_173_WATCH_DISTANCE_SQR = SCP_173_WATCH_DISTANCE * SCP_173_WATCH_DISTANCE;
    private static final double SCP_173_FAR_WATCH_DISTANCE = 1.8D;
    private static final double SCP_173_FAR_WATCH_DISTANCE_SQR = SCP_173_FAR_WATCH_DISTANCE * SCP_173_FAR_WATCH_DISTANCE;
    private static final double OWNER_FOLLOW_DISTANCE = 1.55D;
    private static final double OWNER_FOLLOW_DISTANCE_SQR = OWNER_FOLLOW_DISTANCE * OWNER_FOLLOW_DISTANCE;
    private static final double OWNER_CLOSE_DISTANCE = 0.75D;
    private static final double OWNER_CLOSE_DISTANCE_SQR = OWNER_CLOSE_DISTANCE * OWNER_CLOSE_DISTANCE;
    private static final double OWNER_ROAM_MIN_DISTANCE = 0.9D;
    private static final double OWNER_ROAM_MAX_DISTANCE = 1.65D;
    private static final int OWNER_ROAM_INTERVAL_TICKS = 28;
    private static final int OWNER_ROAM_RANDOM_TICKS = 22;
    private static final double OWNER_RETURN_DISTANCE = 32.0D;
    private static final double OWNER_RETURN_DISTANCE_SQR = OWNER_RETURN_DISTANCE * OWNER_RETURN_DISTANCE;
    private static final double COMPANION_SCAN_RANGE = 16.0D;
    private static final double COMPANION_SCAN_RANGE_SQR = COMPANION_SCAN_RANGE * COMPANION_SCAN_RANGE;
    private static final double COMPANION_FOLLOW_DISTANCE = 2.25D;
    private static final double COMPANION_FOLLOW_DISTANCE_SQR = COMPANION_FOLLOW_DISTANCE * COMPANION_FOLLOW_DISTANCE;
    private static final double IDLE_A_SCAN_RANGE = 16.0D;
    private static final double IDLE_A_SCAN_RANGE_SQR = IDLE_A_SCAN_RANGE * IDLE_A_SCAN_RANGE;
    private static final double IDLE_A_FOLLOW_DISTANCE = 2.6D;
    private static final double IDLE_A_FOLLOW_DISTANCE_SQR = IDLE_A_FOLLOW_DISTANCE * IDLE_A_FOLLOW_DISTANCE;
    private static final double IDLE_A_CLOSE_DISTANCE = 1.35D;
    private static final double IDLE_A_CLOSE_DISTANCE_SQR = IDLE_A_CLOSE_DISTANCE * IDLE_A_CLOSE_DISTANCE;
    private static final int IDLE_A_ROAM_INTERVAL_TICKS = 34;
    private static final double GROUP_TOGGLE_RANGE = 24.0D;
    private static final double GROUP_TOGGLE_RANGE_SQR = GROUP_TOGGLE_RANGE * GROUP_TOGGLE_RANGE;
    private static final int AMBIENT_NOISE_MIN_TICKS = 360;
    private static final int AMBIENT_NOISE_RANDOM_TICKS = 620;
    private static final double RANDOM_ROAM_SPEED = 0.36D;
    private static final double IDLE_A_FOLLOW_SPEED = 0.46D;
    private static final double FOLLOW_CLOSE_SPEED = 0.30D;
    private static final double FOLLOW_CATCH_UP_SPEED = 0.90D;
    private static final double COMPANION_CLOSE_SPEED = 0.28D;
    private static final double COMPANION_CATCH_UP_SPEED = 0.82D;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private UUID followOwner;
    private boolean wasWatchingScp173;
    private int nextOwnerRoamTick;
    private int nextAmbientNoiseTick;

    protected AbstractScp131Entity(EntityType<? extends AbstractScp131Entity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    public abstract String scpName();

    public abstract String textureName();

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FOLLOWING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(7, new RandomStrollGoal(this, RANDOM_ROAM_SPEED));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        setPersistenceRequired();
        scheduleInitialAmbientNoise();
        maybePlayAmbientNoise();

        Scp173Entity scp173 = findNearestScp173();
        if (scp173 != null) {
            wasWatchingScp173 = true;
            runToAndWatch(scp173);
            return;
        }

        if (wasWatchingScp173) {
            wasWatchingScp173 = false;
            teleportToOwnerIfTooFar();
        }

        if (isFollowing()) {
            followOwner();
        } else if (!followNearbyScp131Leader()) {
            followIdleScp131A();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            boolean startedFollowing = startFollowGroup(player, this);
            if (!startedFollowing) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                ScpEntityNetwork.showScp131Notice(serverPlayer, true);
            }
            playVoice(1.0F);
            scheduleAmbientNoise(220 + random.nextInt(320));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
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
        tag.putBoolean("Following", isFollowing());
        if (followOwner != null) {
            tag.putUUID(OWNER_TAG, followOwner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFollowing(tag.getBoolean("Following"));
        followOwner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        if (followOwner == null) {
            setFollowing(false);
        }
    }

    public boolean isFollowing() {
        return entityData.get(FOLLOWING);
    }

    public UUID getFollowOwner() {
        return followOwner;
    }

    public boolean isFollowingPlayer(Player player) {
        return player != null && isFollowing() && player.getUUID().equals(followOwner);
    }

    public void startFollowing(Player player) {
        if (player == null) {
            return;
        }
        followOwner = player.getUUID();
        setFollowing(true);
        setPersistenceRequired();
        scheduleNextOwnerRoam(0);
    }

    public void stopFollowing() {
        setFollowing(false);
        followOwner = null;
        getNavigation().stop();
    }

    public static boolean stopFollowersFor(Player player) {
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    private static boolean startFollowGroup(Player player, AbstractScp131Entity trigger) {
        List<AbstractScp131Entity> group = trigger.findNearbyGroup();
        if (group.stream().anyMatch(scp131 -> scp131.isFollowingPlayer(player))) {
            return false;
        }

        for (AbstractScp131Entity scp131 : group) {
            scp131.startFollowing(player);
        }
        return true;
    }

    private List<AbstractScp131Entity> findNearbyGroup() {
        List<AbstractScp131Entity> group = new ArrayList<>();
        AABB area = getBoundingBox().inflate(GROUP_TOGGLE_RANGE);
        for (AbstractScp131Entity scp131 : level().getEntitiesOfClass(AbstractScp131Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(this) <= GROUP_TOGGLE_RANGE_SQR)) {
            group.add(scp131);
        }
        if (!group.contains(this)) {
            group.add(this);
        }
        return group;
    }

    private void setFollowing(boolean following) {
        entityData.set(FOLLOWING, following);
    }

    private Scp173Entity findNearestScp173() {
        AABB area = getBoundingBox().inflate(SCP_173_SCAN_RANGE);
        Scp173Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Scp173Entity scp173 : level().getEntitiesOfClass(Scp173Entity.class, area,
                entity -> entity.isAlive() && entity.distanceToSqr(this) <= SCP_173_SCAN_RANGE_SQR)) {
            double distance = distanceToSqr(scp173);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = scp173;
            }
        }
        return best;
    }

    private void runToAndWatch(Scp173Entity scp173) {
        lookHardAt(scp173);
        double distanceSqr = distanceToSqr(scp173);
        boolean hasLineOfSight = hasLineOfSight(scp173);
        if (!hasLineOfSight || distanceSqr > SCP_173_FAR_WATCH_DISTANCE_SQR || distanceSqr < SCP_173_WATCH_DISTANCE_SQR) {
            Vec3 spot = watchSpotNear(scp173);
            getNavigation().moveTo(spot.x, spot.y, spot.z, 1.45D);
        } else {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
        }
        lookHardAt(scp173);
    }

    private Vec3 watchSpotNear(Scp173Entity scp173) {
        Vec3 away = position().subtract(scp173.position());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 direction = horizontal.normalize();
        return scp173.position().add(direction.scale(SCP_173_WATCH_DISTANCE));
    }

    private void teleportToOwnerIfTooFar() {
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

    private void followOwner() {
        if (followOwner == null) {
            setFollowing(false);
            getNavigation().stop();
            return;
        }

        Player owner = level().getPlayerByUUID(followOwner);
        if (owner == null || owner.isRemoved() || !owner.isAlive()) {
            return;
        }

        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > OWNER_RETURN_DISTANCE_SQR) {
            teleportNearOwner(owner);
            return;
        }

        boolean tooFar = distanceSqr > OWNER_FOLLOW_DISTANCE_SQR;
        boolean tooClose = distanceSqr < OWNER_CLOSE_DISTANCE_SQR;
        if (tooFar || tooClose || getNavigation().isDone() || tickCount >= nextOwnerRoamTick) {
            Vec3 spot = ownerRoamSpot(owner);
            getNavigation().moveTo(spot.x, spot.y, spot.z, tooFar ? FOLLOW_CATCH_UP_SPEED : FOLLOW_CLOSE_SPEED);
            scheduleNextOwnerRoam(OWNER_ROAM_INTERVAL_TICKS + random.nextInt(OWNER_ROAM_RANDOM_TICKS + 1));
        }

        getLookControl().setLookAt(owner, 35.0F, 25.0F);
    }

    private boolean followNearbyScp131Leader() {
        AbstractScp131Entity leader = findNearbyScp131Leader();
        if (leader == null) {
            return false;
        }

        double distanceSqr = distanceToSqr(leader);
        if (distanceSqr > COMPANION_FOLLOW_DISTANCE_SQR || getNavigation().isDone() || tickCount >= nextOwnerRoamTick) {
            Vec3 spot = companionRoamSpot(leader);
            getNavigation().moveTo(spot.x, spot.y, spot.z,
                    distanceSqr > COMPANION_FOLLOW_DISTANCE_SQR ? COMPANION_CATCH_UP_SPEED : COMPANION_CLOSE_SPEED);
            scheduleNextOwnerRoam(OWNER_ROAM_INTERVAL_TICKS + random.nextInt(OWNER_ROAM_RANDOM_TICKS + 1));
        }
        getLookControl().setLookAt(leader, 35.0F, 25.0F);
        return true;
    }

    private AbstractScp131Entity findNearbyScp131Leader() {
        AABB area = getBoundingBox().inflate(COMPANION_SCAN_RANGE);
        AbstractScp131Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (AbstractScp131Entity other : level().getEntitiesOfClass(AbstractScp131Entity.class, area,
                entity -> entity != this && entity.isAlive() && entity.isFollowing() && entity.getFollowOwner() != null)) {
            double distance = distanceToSqr(other);
            if (distance <= COMPANION_SCAN_RANGE_SQR && distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    private boolean followIdleScp131A() {
        if (!(this instanceof Scp131BEntity)) {
            return false;
        }

        Scp131AEntity scp131A = findNearbyIdleScp131A();
        if (scp131A == null) {
            return false;
        }

        double distanceSqr = distanceToSqr(scp131A);
        if (distanceSqr <= IDLE_A_CLOSE_DISTANCE_SQR) {
            return true;
        }

        if (distanceSqr > IDLE_A_FOLLOW_DISTANCE_SQR || getNavigation().isDone() || tickCount >= nextOwnerRoamTick) {
            Vec3 spot = idleAFollowSpot(scp131A);
            getNavigation().moveTo(spot.x, spot.y, spot.z, IDLE_A_FOLLOW_SPEED);
            scheduleNextOwnerRoam(IDLE_A_ROAM_INTERVAL_TICKS + random.nextInt(OWNER_ROAM_RANDOM_TICKS + 1));
        }
        return true;
    }

    private Scp131AEntity findNearbyIdleScp131A() {
        AABB area = getBoundingBox().inflate(IDLE_A_SCAN_RANGE);
        Scp131AEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Scp131AEntity other : level().getEntitiesOfClass(Scp131AEntity.class, area,
                entity -> entity != this && entity.isAlive() && !entity.isFollowing())) {
            double distance = distanceToSqr(other);
            if (distance <= IDLE_A_SCAN_RANGE_SQR && distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    private Vec3 idleAFollowSpot(Scp131AEntity scp131A) {
        long seed = Math.abs(getUUID().getLeastSignificantBits() % 2048L);
        double angle = ((seed / 2048.0D) * Math.PI * 2.0D) + (tickCount * 0.02D)
                + ((random.nextDouble() - 0.5D) * 0.35D);
        double radius = IDLE_A_CLOSE_DISTANCE + random.nextDouble() * 0.45D;
        return scp131A.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }

    private Vec3 companionRoamSpot(AbstractScp131Entity leader) {
        long seed = Math.abs(getUUID().getLeastSignificantBits() % 2048L);
        double angle = (tickCount * 0.04D) + ((seed / 2048.0D) * Math.PI * 2.0D)
                + ((random.nextDouble() - 0.5D) * 0.45D);
        double radius = 0.85D + random.nextDouble() * 1.05D;
        return leader.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }

    private Vec3 ownerRoamSpot(Player owner) {
        long seed = Math.abs(getUUID().getLeastSignificantBits() % 2048L);
        double baseAngle = (tickCount * 0.055D) + ((seed / 2048.0D) * Math.PI * 2.0D);
        double jitter = (random.nextDouble() - 0.5D) * 0.65D;
        double angle = baseAngle + jitter;
        double radius = OWNER_ROAM_MIN_DISTANCE
                + random.nextDouble() * (OWNER_ROAM_MAX_DISTANCE - OWNER_ROAM_MIN_DISTANCE);
        return owner.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }

    private void scheduleInitialAmbientNoise() {
        if (nextAmbientNoiseTick <= 0) {
            scheduleAmbientNoise(AMBIENT_NOISE_MIN_TICKS + random.nextInt(AMBIENT_NOISE_RANDOM_TICKS + 1));
        }
    }

    private void maybePlayAmbientNoise() {
        if (tickCount < nextAmbientNoiseTick) {
            return;
        }
        playVoice(1.0F);
        scheduleAmbientNoise(AMBIENT_NOISE_MIN_TICKS + random.nextInt(AMBIENT_NOISE_RANDOM_TICKS + 1));
    }

    private void scheduleAmbientNoise(int delay) {
        nextAmbientNoiseTick = tickCount + Math.max(1, delay);
    }

    private void playVoice(float volume) {
        level().playSound(null, getX(), getY() + 0.35D, getZ(), Scp131Sounds.EYE_POD_VOICE.get(),
                SoundSource.NEUTRAL, volume, 0.86F + (random.nextFloat() * 0.30F));
    }

    private void scheduleNextOwnerRoam(int delay) {
        nextOwnerRoamTick = tickCount + Math.max(0, delay);
    }

    private void lookHardAt(LivingEntity target) {
        getLookControl().setLookAt(target, 90.0F, 90.0F);
        Vec3 toTarget = target.getEyePosition().subtract(getEyePosition());
        double horizontal = Math.sqrt((toTarget.x * toTarget.x) + (toTarget.z * toTarget.z));
        if (horizontal <= 0.0001D) {
            return;
        }

        float yaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(toTarget.y, horizontal) * Mth.RAD_TO_DEG));
        setYRot(yaw);
        yRotO = yaw;
        yBodyRot = yaw;
        yBodyRotO = yaw;
        yHeadRot = yaw;
        yHeadRotO = yaw;
        setXRot(pitch);
        xRotO = pitch;
    }
}
