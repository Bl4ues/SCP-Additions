package com.bl4ues.scpclassifieddirective.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpclassifieddirective.network.Scp939Network;
import com.bl4ues.scpclassifieddirective.scp939.Scp939AcousticBrain;
import com.bl4ues.scpclassifieddirective.scp939.Scp939AwarenessState;
import com.bl4ues.scpclassifieddirective.scp939.Scp939MimicryHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Sound-driven SCP-939 entity.
 *
 * Long-range pursuit is fed exclusively by Scp939AcousticBrain's last evidenced
 * sound position. The entity never assigns a vanilla target and never asks for
 * a player's current coordinates to navigate. Living players are inspected only
 * inside a short unobstructed heat/tactile radius for physical attacks.
 */
public class Scp939Entity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Byte> AWARENESS =
            SynchedEntityData.defineId(Scp939Entity.class,
                    EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ACTION =
            SynchedEntityData.defineId(Scp939Entity.class,
                    EntityDataSerializers.BYTE);

    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_BITE = 1;
    public static final byte ACTION_POUNCE = 2;
    public static final byte ACTION_PIN_LAND = 3;
    public static final byte ACTION_MAUL = 4;
    public static final byte ACTION_KICKED = 5;
    public static final byte ACTION_HURT = 6;
    public static final byte ACTION_DEATH = 7;
    public static final byte ACTION_MIMIC = 8;
    public static final byte ACTION_LISTEN = 9;

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_LISTEN =
            RawAnimation.begin().thenLoop("idle_listen");
    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN =
            RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SEARCH =
            RawAnimation.begin().thenLoop("search");
    private static final RawAnimation MIMIC_CALL =
            RawAnimation.begin().thenPlay("mimic_call");
    private static final RawAnimation ATTACK_BITE =
            RawAnimation.begin().thenPlay("attack_bite");
    private static final RawAnimation POUNCE_START =
            RawAnimation.begin().thenPlay("pounce_start");
    private static final RawAnimation POUNCE_LAND_PIN =
            RawAnimation.begin().thenPlay("pounce_land_pin");
    private static final RawAnimation POUNCE_MAUL =
            RawAnimation.begin().thenLoop("pounce_maul_loop");
    private static final RawAnimation POUNCE_KICKED =
            RawAnimation.begin().thenPlay("pounce_kicked_off");
    private static final RawAnimation HURT =
            RawAnimation.begin().thenPlay("hurt_stagger");
    private static final RawAnimation DEATH =
            RawAnimation.begin().thenPlay("death");

    private static final double IDLE_NAV_SPEED = 0.46D;
    private static final double INVESTIGATE_NAV_SPEED = 0.68D;
    private static final double SEARCH_NAV_SPEED = 0.52D;
    private static final double HUNT_NAV_SPEED = 1.20D;
    private static final double LOCAL_HEAT_RANGE = 8.5D;
    private static final double BITE_RANGE = 2.25D;
    private static final double POUNCE_MIN_RANGE = 3.4D;
    private static final double POUNCE_MAX_RANGE = 8.25D;
    private static final int BITE_DURATION_TICKS = 15;
    private static final int BITE_HIT_REMAINING_TICKS = 8;
    private static final int POUNCE_DURATION_TICKS = 27;
    private static final int POUNCE_WINDUP_TICKS = 7;
    private static final int POUNCE_COOLDOWN_TICKS = 20 * 9;
    private static final int PIN_LAND_TICKS = 17;
    public static final int STRUGGLE_WINDOW_TICKS = 36;
    private static final int SAFE_STRUGGLE_WINDOW_TICKS = 48;
    private static final int KETER_STRUGGLE_WINDOW_TICKS = 28;
    public static final int STRUGGLE_SUCCESS_REQUIRED = 3;
    public static final int STRUGGLE_FAILURE_LIMIT = 3;
    private static final int PIN_DAMAGE_INTERVAL_TICKS = 24;
    private static final int KICKED_TICKS = 33;
    private static final int HURT_TICKS = 16;
    private static final int MIMIC_TICKS = 50;
    private static final int DEATH_TICKS = 51;
    private static final int SEARCH_POINT_INTERVAL = 34;
    private static final int IDLE_POINT_MIN_TICKS = 55;
    private static final int IDLE_POINT_RANDOM_TICKS = 90;
    private static final int LISTEN_MIN_TICKS = 28;
    private static final int LISTEN_RANDOM_TICKS = 28;
    private static final int MIMIC_MIN_INTERVAL = 20 * 8;
    private static final int MIMIC_RANDOM_INTERVAL = 20 * 6;
    private static final int MIMIC_RETRY_MIN_TICKS = 20 * 2;
    private static final int MIMIC_RETRY_RANDOM_TICKS = 20 * 2;
    private static final int ROUTINE_QUIET_DESPAWN_TICKS = 20 * 30;
    private static final int ROUTINE_HARD_DESPAWN_TICKS = 20 * 60;

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private final Scp939AcousticBrain acousticBrain = new Scp939AcousticBrain();

    private int actionTicks;
    private int idleMoveTicks;
    private int searchMoveTicks;
    private int pounceCooldownTicks;
    private int nextMimicTicks;
    private int routineQuietTicks;
    private UUID biteTargetId;
    private UUID pinnedPlayerId;
    private int pinProgress;
    private int pinFailures;
    private int pinExpectedKey;
    private int pinWindowTicks;
    private int pinLandTicks;
    private boolean pounceLaunched;
    private Vec3 pounceTargetPosition;
    private boolean routineEncounter;
    private Vec3 encounterAnchor;
    private UUID encounterTriggerId;
    private UUID preferredMimicUuid;
    private Scp939AcousticBrain.Snapshot lastBrainSnapshot;

    public Scp939Entity(EntityType<? extends Scp939Entity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 18;
        idleMoveTicks = IDLE_POINT_MIN_TICKS + random.nextInt(
                IDLE_POINT_RANDOM_TICKS + 1);
        nextMimicTicks = MIMIC_MIN_INTERVAL + random.nextInt(
                MIMIC_RANDOM_INTERVAL + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 72.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.285D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D);
    }

    public static int struggleWindowTicks(Difficulty difficulty) {
        if (difficulty == null) return STRUGGLE_WINDOW_TICKS;
        return switch (difficulty) {
            case PEACEFUL, EASY -> SAFE_STRUGGLE_WINDOW_TICKS;
            case NORMAL -> STRUGGLE_WINDOW_TICKS;
            case HARD -> KETER_STRUGGLE_WINDOW_TICKS;
        };
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(AWARENESS,
                (byte) Scp939AwarenessState.IDLE.ordinal());
        entityData.define(ACTION, ACTION_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        smoothLocomotionRotation();
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (pounceCooldownTicks > 0) pounceCooldownTicks--;
        if (nextMimicTicks > 0) nextMimicTicks--;
        tickActionTimer();

        if (isDeadOrDying()) return;
        if (pinnedPlayerId != null) {
            tickPinned(serverLevel);
            return;
        }

        Vec3 remembered = acousticBrain.lastKnownPosition();
        boolean reached = remembered != null
                && position().distanceToSqr(remembered) <= 2.0D * 2.0D;
        lastBrainSnapshot = acousticBrain.tick(serverLevel,
                getEyePosition(), reached);
        setAwareness(lastBrainSnapshot.state());

        if (getAction() == ACTION_LISTEN
                && lastBrainSnapshot.state() != Scp939AwarenessState.IDLE) {
            clearAction();
        }
        if (getAction() == ACTION_MIMIC
                && lastBrainSnapshot.state()
                == Scp939AwarenessState.CONFIRMED_HUNT) {
            clearAction();
        }

        if (isMovementLockedByAction()) {
            getNavigation().stop();
            tickRoutineEncounter(serverLevel);
            return;
        }

        ServerPlayer localPrey = findLocalPrey(serverLevel, LOCAL_HEAT_RANGE);
        if (localPrey != null && tryStartCombat(localPrey,
                lastBrainSnapshot.state())) {
            tickRoutineEncounter(serverLevel);
            return;
        }

        driveAcousticNavigation(lastBrainSnapshot);
        maybeMimic(serverLevel, lastBrainSnapshot.state());
        tickRoutineEncounter(serverLevel);
    }

    private void driveAcousticNavigation(Scp939AcousticBrain.Snapshot snapshot) {
        Scp939AwarenessState state = snapshot.state();
        Vec3 known = snapshot.lastKnownPosition();
        switch (state) {
            case HEARD_SOUND -> {
                getNavigation().stop();
                if (getAction() == ACTION_NONE) {
                    setAction(ACTION_LISTEN,
                            LISTEN_MIN_TICKS + random.nextInt(
                                    LISTEN_RANDOM_TICKS + 1));
                }
                orientToward(known);
            }
            case INVESTIGATE -> moveToKnown(known, INVESTIGATE_NAV_SPEED);
            case CONFIRMED_HUNT, LOST_SEARCH -> moveToKnown(known,
                    HUNT_NAV_SPEED);
            case SEARCH -> tickSearchNavigation(known);
            case IDLE -> tickIdleNavigation();
        }
    }

    private void moveToKnown(Vec3 known, double speed) {
        if (known == null) return;
        if (getNavigation().isDone() || tickCount % 8 == 0) {
            getNavigation().moveTo(known.x, known.y, known.z, speed);
        }
    }

    private void tickSearchNavigation(Vec3 center) {
        if (--searchMoveTicks > 0 && !getNavigation().isDone()) return;
        searchMoveTicks = SEARCH_POINT_INTERVAL + random.nextInt(25);
        Vec3 origin = center == null ? position() : center;
        double radius = 3.0D + random.nextDouble() * 6.0D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double x = origin.x + Math.cos(angle) * radius;
        double z = origin.z + Math.sin(angle) * radius;
        double y = origin.y + random.nextInt(5) - 2;
        getNavigation().moveTo(x, y, z, SEARCH_NAV_SPEED);
    }

    private void tickIdleNavigation() {
        if (getAction() == ACTION_LISTEN) return;
        if (--idleMoveTicks > 0 && !getNavigation().isDone()) return;

        idleMoveTicks = IDLE_POINT_MIN_TICKS + random.nextInt(
                IDLE_POINT_RANDOM_TICKS + 1);
        if (random.nextFloat() < 0.36F) {
            setAction(ACTION_LISTEN,
                    LISTEN_MIN_TICKS + random.nextInt(
                            LISTEN_RANDOM_TICKS + 1));
            getNavigation().stop();
            return;
        }

        Vec3 center = encounterAnchor == null ? position() : encounterAnchor;
        double radius = 5.0D + random.nextDouble() * 8.0D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        getNavigation().moveTo(center.x + Math.cos(angle) * radius,
                center.y + random.nextInt(5) - 2,
                center.z + Math.sin(angle) * radius, IDLE_NAV_SPEED);
    }

    private boolean tryStartCombat(ServerPlayer player,
            Scp939AwarenessState awareness) {
        if (player == null || getAction() != ACTION_NONE) return false;
        double distance = distanceTo(player);
        if (distance <= BITE_RANGE) {
            startBite(player);
            return true;
        }
        if (awareness == Scp939AwarenessState.CONFIRMED_HUNT
                && pounceCooldownTicks <= 0
                && distance >= POUNCE_MIN_RANGE
                && distance <= POUNCE_MAX_RANGE) {
            startPounce(player);
            return true;
        }
        return false;
    }

    private void startBite(ServerPlayer target) {
        biteTargetId = target.getUUID();
        setAction(ACTION_BITE, BITE_DURATION_TICKS);
        orientToward(target.position());
    }

    private void startPounce(ServerPlayer target) {
        getNavigation().stop();
        pounceCooldownTicks = POUNCE_COOLDOWN_TICKS;
        pounceTargetPosition = target.position().add(0.0D,
                target.getBbHeight() * 0.30D, 0.0D);
        pounceLaunched = false;
        setDeltaMovement(Vec3.ZERO);
        setAction(ACTION_POUNCE, POUNCE_DURATION_TICKS);
        orientToward(pounceTargetPosition);
    }

    private void launchPounce() {
        Vec3 to = pounceTargetPosition;
        if (to == null) return;
        Vec3 delta = to.subtract(position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        if (horizontal.lengthSqr() < 0.0001D) return;
        pounceLaunched = true;
        Vec3 launch = horizontal.normalize().scale(
                Mth.clamp(horizontal.length() * 0.22D, 0.90D, 1.42D));
        setDeltaMovement(launch.x, 0.40D, launch.z);
        hasImpulse = true;
    }

    private void tickActionTimer() {
        byte action = getAction();
        if (action == ACTION_BITE) {
            if (actionTicks == BITE_HIT_REMAINING_TICKS) performBiteHit();
        } else if (action == ACTION_POUNCE) {
            if (!pounceLaunched) {
                orientToward(pounceTargetPosition);
                if (actionTicks <= POUNCE_DURATION_TICKS
                        - POUNCE_WINDUP_TICKS) {
                    launchPounce();
                }
            }
            if (pounceLaunched) tickPounceCollision();
        }

        if (actionTicks <= 0) return;
        actionTicks--;
        if (actionTicks > 0) return;
        if (action == ACTION_PIN_LAND || action == ACTION_MAUL
                || action == ACTION_DEATH) {
            return;
        }
        biteTargetId = null;
        pounceLaunched = false;
        pounceTargetPosition = null;
        clearAction();
    }

    private void performBiteHit() {
        if (!(level() instanceof ServerLevel serverLevel)
                || biteTargetId == null) return;
        ServerPlayer target = serverLevel.getServer().getPlayerList()
                .getPlayer(biteTargetId);
        if (!validPrey(target) || target.level() != level()
                || distanceTo(target) > BITE_RANGE + 0.55D
                || !hasPhysicalLine(target)) {
            return;
        }
        boolean hurt = target.hurt(damageSources().mobAttack(this), 10.0F);
        if (hurt) {
            Vec3 push = target.position().subtract(position());
            if (push.horizontalDistanceSqr() > 0.0001D) {
                Vec3 horizontal = new Vec3(push.x, 0.0D, push.z)
                        .normalize().scale(0.42D);
                target.push(horizontal.x, 0.12D, horizontal.z);
            }
            if (!target.isAlive()) preferredMimicUuid = target.getUUID();
        }
    }

    private void tickPounceCollision() {
        if (!pounceLaunched || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerPlayer prey = findLocalPrey(serverLevel, 1.70D);
        if (prey != null) beginPin(prey);
    }

    private void beginPin(ServerPlayer player) {
        pinnedPlayerId = player.getUUID();
        pinProgress = 0;
        pinFailures = 0;
        pinExpectedKey = random.nextBoolean() ? 1 : 0;
        pinWindowTicks = struggleWindowTicks(level().getDifficulty());
        pinLandTicks = PIN_LAND_TICKS;
        pounceLaunched = false;
        pounceTargetPosition = null;
        setAction(ACTION_PIN_LAND, PIN_LAND_TICKS);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        player.setForcedPose(Pose.SWIMMING);
        player.refreshDimensions();
        syncPin(player);
    }

    private void tickPinned(ServerLevel level) {
        ServerPlayer player = level.getServer().getPlayerList()
                .getPlayer(pinnedPlayerId);
        if (!validPrey(player) || player.level() != level) {
            releasePin(false);
            return;
        }

        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        player.setForcedPose(Pose.SWIMMING);
        Vec3 forward = getLookAngle();
        Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontal.lengthSqr() < 0.0001D) {
            horizontal = new Vec3(0, 0, 1);
        }
        horizontal = horizontal.normalize();

        // The prone player's eye is now under the front half of the 939 rather
        // than out in front of its paws. Looking upward keeps the maul framed
        // from the floor while the forced swimming pose remains visible to all
        // other clients as a pinned body.
        Vec3 pinPos = position().add(horizontal.scale(0.46D));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        if (tickCount % 2 == 0) {
            float faceYaw = getYRot() + 180.0F;
            player.connection.teleport(pinPos.x, getY() + 0.01D, pinPos.z,
                    faceYaw, -30.0F);
        }

        if (pinLandTicks > 0) {
            pinLandTicks--;
            if (pinLandTicks <= 0) {
                setAction(ACTION_MAUL, Integer.MAX_VALUE / 4);
            }
        }

        if (tickCount % PIN_DAMAGE_INTERVAL_TICKS == 0) {
            player.hurt(damageSources().mobAttack(this), 1.75F);
            if (!player.isAlive()) {
                preferredMimicUuid = player.getUUID();
                releasePin(false);
                return;
            }
        }

        if (--pinWindowTicks <= 0) {
            pinFailures++;
            if (pinFailures >= STRUGGLE_FAILURE_LIMIT) {
                finishMaul(player);
                return;
            }
            nextStrugglePrompt();
        }
    }

    public static void handleStruggleInput(ServerPlayer player, int input) {
        if (player == null) return;
        AABB search = player.getBoundingBox().inflate(4.0D);
        List<Scp939Entity> nearby = player.serverLevel().getEntitiesOfClass(
                Scp939Entity.class, search,
                entity -> entity.isAlive() && entity.isPinning(player));
        if (!nearby.isEmpty()) nearby.get(0).acceptStruggle(player, input);
    }

    private void acceptStruggle(ServerPlayer player, int input) {
        if (!isPinning(player)) return;
        if (input == pinExpectedKey) {
            pinProgress++;
            if (pinProgress >= STRUGGLE_SUCCESS_REQUIRED) {
                releasePin(true);
                return;
            }
        } else {
            pinFailures++;
            if (pinFailures >= STRUGGLE_FAILURE_LIMIT) {
                finishMaul(player);
                return;
            }
        }
        nextStrugglePrompt();
    }

    private void nextStrugglePrompt() {
        pinExpectedKey = pinExpectedKey == 0 ? 1 : 0;
        if (random.nextFloat() < 0.35F) {
            pinExpectedKey = random.nextBoolean() ? 1 : 0;
        }
        pinWindowTicks = struggleWindowTicks(level().getDifficulty());
        ServerPlayer player = pinnedPlayer();
        if (player != null) syncPin(player);
    }

    private void finishMaul(ServerPlayer player) {
        if (player != null && player.isAlive()) {
            player.hurt(damageSources().mobAttack(this), 40.0F);
            if (!player.isAlive()) preferredMimicUuid = player.getUUID();
        }
        releasePin(false);
    }

    private void releasePin(boolean kickedOff) {
        ServerPlayer player = pinnedPlayer();
        if (player != null) {
            player.setForcedPose(null);
            player.refreshDimensions();
            Scp939Network.sendPinState(player, false, 0, 0, 0, 0);
        }
        pinnedPlayerId = null;
        pinProgress = 0;
        pinFailures = 0;
        pinWindowTicks = 0;
        pinLandTicks = 0;
        if (isDeadOrDying()) return;
        if (kickedOff) {
            setAction(ACTION_KICKED, KICKED_TICKS);
            if (player != null) {
                Vec3 away = position().subtract(player.position());
                if (away.horizontalDistanceSqr() > 0.0001D) {
                    Vec3 push = new Vec3(away.x, 0.0D, away.z)
                            .normalize().scale(0.55D);
                    setDeltaMovement(push.x, 0.18D, push.z);
                    hasImpulse = true;
                }
            }
        } else {
            clearAction();
        }
    }

    private void syncPin(ServerPlayer player) {
        Scp939Network.sendPinState(player, true, pinProgress, pinFailures,
                pinExpectedKey, pinWindowTicks);
    }

    private boolean isPinning(ServerPlayer player) {
        return player != null && pinnedPlayerId != null
                && pinnedPlayerId.equals(player.getUUID());
    }

    private ServerPlayer pinnedPlayer() {
        if (!(level() instanceof ServerLevel serverLevel)
                || pinnedPlayerId == null) return null;
        return serverLevel.getServer().getPlayerList().getPlayer(pinnedPlayerId);
    }

    private ServerPlayer findLocalPrey(ServerLevel level, double range) {
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!validPrey(player)) continue;
            if (distanceToSqr(player) > range * range) continue;
            if (!hasPhysicalLine(player)) continue;
            candidates.add(player);
        }
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean validPrey(ServerPlayer player) {
        return player != null && player.isAlive()
                && !player.isCreative() && !player.isSpectator();
    }

    private boolean hasPhysicalLine(ServerPlayer player) {
        Vec3 from = getEyePosition();
        Vec3 to = player.position().add(0.0D,
                player.getBbHeight() * 0.58D, 0.0D);
        BlockHitResult hit = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(to) <= 0.35D;
    }

    private void maybeMimic(ServerLevel level, Scp939AwarenessState state) {
        if (nextMimicTicks > 0 || getAction() != ACTION_NONE) return;
        if (state != Scp939AwarenessState.IDLE
                && state != Scp939AwarenessState.SEARCH
                && state != Scp939AwarenessState.LOST_SEARCH) {
            nextMimicTicks = MIMIC_RETRY_MIN_TICKS;
            return;
        }

        if (Scp939MimicryHooks.request(level, getUUID(), position(),
                preferredMimicUuid)) {
            nextMimicTicks = MIMIC_MIN_INTERVAL + random.nextInt(
                    MIMIC_RANDOM_INTERVAL + 1);
            setAction(ACTION_MIMIC, MIMIC_TICKS);
        } else {
            nextMimicTicks = MIMIC_RETRY_MIN_TICKS + random.nextInt(
                    MIMIC_RETRY_RANDOM_TICKS + 1);
        }
    }

    public void beginNaturalEncounter(ServerPlayer trigger, Vec3 anchor) {
        routineEncounter = true;
        encounterTriggerId = trigger == null ? null : trigger.getUUID();
        encounterAnchor = anchor == null ? position() : anchor;
        preferredMimicUuid = chooseInitialMimic(trigger);
        routineQuietTicks = 0;
        setPersistenceRequired();
    }

    private UUID chooseInitialMimic(ServerPlayer trigger) {
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        List<ServerPlayer> others = new ArrayList<>();
        for (ServerPlayer player : serverLevel.getServer().getPlayerList()
                .getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            if (trigger != null && player.getUUID().equals(trigger.getUUID())) {
                continue;
            }
            others.add(player);
        }
        if (others.isEmpty()) return null;
        return others.get(random.nextInt(others.size())).getUUID();
    }

    private void tickRoutineEncounter(ServerLevel level) {
        if (!routineEncounter) return;
        boolean quiet = lastBrainSnapshot == null
                || lastBrainSnapshot.evidenceAgeTicks() > 20L * 8L;
        if (quiet) routineQuietTicks++;
        else routineQuietTicks = Math.max(0, routineQuietTicks - 4);

        ServerPlayer trigger = encounterTriggerId == null ? null
                : level.getServer().getPlayerList().getPlayer(encounterTriggerId);
        boolean triggerGone = trigger == null || trigger.level() != level
                || trigger.isSpectator()
                || encounterAnchor != null
                        && trigger.position().distanceToSqr(encounterAnchor)
                                > 48.0D * 48.0D;
        boolean anyoneNear = false;
        for (ServerPlayer player : level.players()) {
            if (!validPrey(player)) continue;
            if (distanceToSqr(player) <= 34.0D * 34.0D) {
                anyoneNear = true;
                break;
            }
        }

        if (routineQuietTicks >= ROUTINE_HARD_DESPAWN_TICKS
                || (routineQuietTicks >= ROUTINE_QUIET_DESPAWN_TICKS
                        && triggerGone && !anyoneNear)) {
            discard();
        }
    }

    private void orientToward(Vec3 position) {
        if (position == null) return;
        Vec3 delta = position.subtract(this.position());
        if (delta.horizontalDistanceSqr() <= 0.0001D) return;
        float targetYaw = (float) (Mth.atan2(delta.z, delta.x)
                * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = approachAngle(getYRot(), targetYaw, 9.0F);
        setYRot(yaw);
        yBodyRot = approachAngle(yBodyRot, yaw, 6.0F);
        yHeadRot = approachAngle(yHeadRot, targetYaw, 13.0F);
    }

    private void smoothLocomotionRotation() {
        byte action = getAction();
        float maximumTurn = action == ACTION_POUNCE ? 13.0F
                : action == ACTION_BITE ? 10.0F : 8.0F;
        float smoothedYaw = approachAngle(yRotO, getYRot(), maximumTurn);
        setYRot(smoothedYaw);
        yBodyRot = approachAngle(yBodyRot, smoothedYaw, 6.0F);
        yHeadRot = approachAngle(yHeadRot, smoothedYaw, 10.0F);
    }

    private static float approachAngle(float current, float target,
            float maximumChange) {
        float delta = Mth.wrapDegrees(target - current);
        return current + Mth.clamp(delta, -maximumChange, maximumChange);
    }

    private void setAwareness(Scp939AwarenessState state) {
        int ordinal = state == null ? 0 : state.ordinal();
        entityData.set(AWARENESS, (byte) Mth.clamp(ordinal, 0,
                Scp939AwarenessState.values().length - 1));
    }

    public Scp939AwarenessState getAwarenessState() {
        int ordinal = Byte.toUnsignedInt(entityData.get(AWARENESS));
        Scp939AwarenessState[] values = Scp939AwarenessState.values();
        return values[Math.min(ordinal, values.length - 1)];
    }

    private void setAction(byte action, int ticks) {
        entityData.set(ACTION, action);
        actionTicks = Math.max(0, ticks);
    }

    private void clearAction() {
        entityData.set(ACTION, ACTION_NONE);
        actionTicks = 0;
    }

    public byte getAction() {
        return entityData.get(ACTION);
    }

    private boolean isMovementLockedByAction() {
        byte action = getAction();
        return action == ACTION_POUNCE || action == ACTION_PIN_LAND
                || action == ACTION_MAUL || action == ACTION_KICKED
                || action == ACTION_HURT || action == ACTION_LISTEN;
    }

    private boolean isFullBodyAnimationAction(byte action) {
        return action == ACTION_POUNCE || action == ACTION_PIN_LAND
                || action == ACTION_MAUL || action == ACTION_KICKED
                || action == ACTION_HURT || action == ACTION_DEATH
                || action == ACTION_LISTEN;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!hurt) return false;

        Entity attacker = source.getEntity();
        if (pinnedPlayerId != null && attacker instanceof Player player
                && !player.getUUID().equals(pinnedPlayerId)) {
            releasePin(true);
        } else if (isAlive() && getAction() == ACTION_NONE) {
            setAction(ACTION_HURT, HURT_TICKS);
            getNavigation().stop();
        }
        return true;
    }

    @Override
    public void die(DamageSource cause) {
        if (isDeadOrDying()) return;
        releasePin(false);
        setAction(ACTION_DEATH, DEATH_TICKS);
        getNavigation().stop();
        super.die(cause);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DEATH_TICKS && !level().isClientSide) {
            remove(RemovalReason.KILLED);
        }
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
        tag.putBoolean("RoutineEncounter", routineEncounter);
        if (encounterAnchor != null) {
            tag.putDouble("EncounterAnchorX", encounterAnchor.x);
            tag.putDouble("EncounterAnchorY", encounterAnchor.y);
            tag.putDouble("EncounterAnchorZ", encounterAnchor.z);
        }
        if (encounterTriggerId != null) {
            tag.putUUID("EncounterTrigger", encounterTriggerId);
        }
        if (preferredMimicUuid != null) {
            tag.putUUID("PreferredMimic", preferredMimicUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        routineEncounter = tag.getBoolean("RoutineEncounter");
        if (tag.contains("EncounterAnchorX")) {
            encounterAnchor = new Vec3(tag.getDouble("EncounterAnchorX"),
                    tag.getDouble("EncounterAnchorY"),
                    tag.getDouble("EncounterAnchorZ"));
        }
        encounterTriggerId = tag.hasUUID("EncounterTrigger")
                ? tag.getUUID("EncounterTrigger") : null;
        preferredMimicUuid = tag.hasUUID("PreferredMimic")
                ? tag.getUUID("PreferredMimic") : null;
        acousticBrain.reset();
        clearAction();
        pinnedPlayerId = null;
        pounceLaunched = false;
        pounceTargetPosition = null;
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Scp939Entity> locomotion =
                new AnimationController<>(this, "locomotion", 3, state -> {
                    byte action = getAction();
                    if (isFullBodyAnimationAction(action)) {
                        return PlayState.STOP;
                    }

                    Scp939AwarenessState awareness = getAwarenessState();
                    boolean moving = getDeltaMovement()
                            .horizontalDistanceSqr() > 0.00008D;
                    if (moving) {
                        if (awareness == Scp939AwarenessState.CONFIRMED_HUNT
                                || awareness
                                == Scp939AwarenessState.LOST_SEARCH) {
                            return state.setAndContinue(RUN);
                        }
                        return state.setAndContinue(WALK);
                    }
                    if (awareness == Scp939AwarenessState.SEARCH) {
                        return state.setAndContinue(SEARCH);
                    }
                    if (awareness == Scp939AwarenessState.HEARD_SOUND) {
                        return state.setAndContinue(IDLE_LISTEN);
                    }
                    return state.setAndContinue(IDLE);
                });
        locomotion.setAnimationSpeedHandler(entity -> {
            double movement = Math.sqrt(entity.getDeltaMovement()
                    .horizontalDistanceSqr());
            if (movement < 0.001D) return 1.0D;
            Scp939AwarenessState awareness = entity.getAwarenessState();
            boolean running = awareness == Scp939AwarenessState.CONFIRMED_HUNT
                    || awareness == Scp939AwarenessState.LOST_SEARCH;
            double reference = running ? 0.21D : 0.09D;
            return Mth.clamp(movement / reference, 0.90D, 1.90D);
        });
        controllers.add(locomotion);

        // Bite and mimic clips only replace the upper-body bones they author.
        // Because locomotion stays alive underneath them, the paws and gait no
        // longer freeze just because the 939 opens its mouth or vocalizes.
        AnimationController<Scp939Entity> actionController =
                new AnimationController<>(this, "action", 1, state -> {
                    return switch (getAction()) {
                        case ACTION_BITE -> state.setAndContinue(ATTACK_BITE);
                        case ACTION_POUNCE -> state.setAndContinue(POUNCE_START);
                        case ACTION_PIN_LAND ->
                                state.setAndContinue(POUNCE_LAND_PIN);
                        case ACTION_MAUL -> state.setAndContinue(POUNCE_MAUL);
                        case ACTION_KICKED ->
                                state.setAndContinue(POUNCE_KICKED);
                        case ACTION_HURT -> state.setAndContinue(HURT);
                        case ACTION_DEATH -> state.setAndContinue(DEATH);
                        case ACTION_MIMIC -> state.setAndContinue(MIMIC_CALL);
                        case ACTION_LISTEN -> state.setAndContinue(IDLE_LISTEN);
                        default -> PlayState.STOP;
                    };
                });
        controllers.add(actionController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
