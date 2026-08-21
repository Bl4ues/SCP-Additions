package net.mcreator.scpadditions.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.scp939.Scp939AcousticBrain;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import net.mcreator.scpadditions.scp939.Scp939MimicryHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** SCP-939 predator driven by acoustic evidence instead of visual targeting. */
public class Scp939Entity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Byte> AWARENESS = SynchedEntityData.defineId(Scp939Entity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ACTION = SynchedEntityData.defineId(Scp939Entity.class, EntityDataSerializers.BYTE);

    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_BITE = 1;
    public static final byte ACTION_POUNCE = 2;
    public static final byte ACTION_HURT = 3;
    public static final byte ACTION_DEATH = 4;
    public static final byte ACTION_MIMIC = 5;
    public static final byte ACTION_LISTEN = 6;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_LISTEN = RawAnimation.begin().thenLoop("idle_listen");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SEARCH = RawAnimation.begin().thenLoop("search");
    private static final RawAnimation MIMIC = RawAnimation.begin().thenPlay("mimic_call");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("attack_bite");
    private static final RawAnimation POUNCE = RawAnimation.begin().thenPlay("pounce_start");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("hurt_stagger");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("death");

    private static final int DEATH_TICKS = 51;
    private static final int BITE_TICKS = 15;
    private static final int POUNCE_TICKS = 20;
    private static final int HURT_TICKS = 16;
    private static final int LISTEN_TICKS = 36;
    private static final int MIMIC_TICKS = 50;
    private static final double LOCAL_HEAT_RANGE = 8.5D;
    private static final double BITE_RANGE = 2.25D;
    private static final double POUNCE_MIN = 3.4D;
    private static final double POUNCE_MAX = 8.25D;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Scp939AcousticBrain acousticBrain = new Scp939AcousticBrain();
    private Scp939AcousticBrain.Snapshot snapshot;
    private int actionTicks;
    private int pounceCooldown;
    private int nextMimic = 20 * 20;
    private int idleTicks = 50;
    private int searchTicks;
    private UUID biteTarget;
    private boolean routineEncounter;
    private Vec3 encounterAnchor;
    private UUID encounterTrigger;
    private UUID preferredMimic;
    private int quietTicks;

    public Scp939Entity(EntityType<? extends Scp939Entity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 18;
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(AWARENESS, (byte) Scp939AwarenessState.IDLE.ordinal());
        entityData.define(ACTION, ACTION_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        if (pounceCooldown > 0) pounceCooldown--;
        if (nextMimic > 0) nextMimic--;
        tickAction(server);
        if (isDeadOrDying()) return;

        Vec3 remembered = acousticBrain.lastKnownPosition();
        boolean reached = remembered != null && distanceToSqr(remembered) <= 4.0D;
        snapshot = acousticBrain.tick(server, getEyePosition(), reached);
        setAwareness(snapshot.state());

        if (getAction() == ACTION_LISTEN && snapshot.state() != Scp939AwarenessState.IDLE) clearAction();
        if (getAction() == ACTION_MIMIC && snapshot.state() == Scp939AwarenessState.CONFIRMED_HUNT) clearAction();

        if (getAction() == ACTION_NONE) {
            ServerPlayer prey = findLocalPrey(server, LOCAL_HEAT_RANGE);
            if (prey != null && tryCombat(prey)) {
                tickRoutineEncounter(server);
                return;
            }
            driveNavigation(snapshot);
            maybeMimic(server);
        } else if (getAction() != ACTION_POUNCE) {
            getNavigation().stop();
        }
        tickRoutineEncounter(server);
    }

    private void tickAction(ServerLevel server) {
        if (actionTicks <= 0) return;
        byte action = getAction();
        if (action == ACTION_BITE && actionTicks == 8) performBite(server);
        if (action == ACTION_POUNCE) {
            ServerPlayer prey = findLocalPrey(server, 1.5D);
            if (prey != null) {
                prey.hurt(damageSources().mobAttack(this), 14.0F);
                Vec3 push = prey.position().subtract(position());
                if (push.horizontalDistanceSqr() > 0.001D) {
                    Vec3 h = new Vec3(push.x, 0, push.z).normalize().scale(0.6D);
                    prey.push(h.x, 0.18D, h.z);
                }
                actionTicks = 1;
            }
        }
        if (--actionTicks <= 0 && action != ACTION_DEATH) {
            biteTarget = null;
            clearAction();
        }
    }

    private boolean tryCombat(ServerPlayer prey) {
        double distance = distanceTo(prey);
        if (distance <= BITE_RANGE) {
            biteTarget = prey.getUUID();
            setAction(ACTION_BITE, BITE_TICKS);
            orientToward(prey.position());
            return true;
        }
        if (snapshot != null && snapshot.state() == Scp939AwarenessState.CONFIRMED_HUNT
                && pounceCooldown <= 0 && distance >= POUNCE_MIN && distance <= POUNCE_MAX) {
            Vec3 delta = prey.position().subtract(position());
            Vec3 horizontal = new Vec3(delta.x, 0, delta.z);
            if (horizontal.lengthSqr() > 0.001D) {
                Vec3 launch = horizontal.normalize().scale(Mth.clamp(horizontal.length() * 0.17D, 0.72D, 1.10D));
                setDeltaMovement(launch.x, 0.30D, launch.z);
                hasImpulse = true;
                pounceCooldown = 20 * 9;
                setAction(ACTION_POUNCE, POUNCE_TICKS);
                orientToward(prey.position());
                return true;
            }
        }
        return false;
    }

    private void performBite(ServerLevel server) {
        if (biteTarget == null) return;
        ServerPlayer prey = server.getServer().getPlayerList().getPlayer(biteTarget);
        if (!validPrey(prey) || prey.level() != level() || distanceTo(prey) > BITE_RANGE + 0.6D || !hasPhysicalLine(prey)) return;
        prey.hurt(damageSources().mobAttack(this), 10.0F);
        if (!prey.isAlive()) preferredMimic = prey.getUUID();
    }

    private void driveNavigation(Scp939AcousticBrain.Snapshot state) {
        Vec3 known = state.lastKnownPosition();
        switch (state.state()) {
            case HEARD_SOUND -> {
                getNavigation().stop();
                setAction(ACTION_LISTEN, LISTEN_TICKS);
                orientToward(known);
            }
            case INVESTIGATE -> moveKnown(known, 1.05D);
            case CONFIRMED_HUNT, LOST_SEARCH -> moveKnown(known, 1.52D);
            case SEARCH -> searchAround(known);
            case IDLE -> wander();
        }
    }

    private void moveKnown(Vec3 known, double speed) {
        if (known != null && (getNavigation().isDone() || tickCount % 8 == 0))
            getNavigation().moveTo(known.x, known.y, known.z, speed);
    }

    private void searchAround(Vec3 center) {
        if (--searchTicks > 0 && !getNavigation().isDone()) return;
        searchTicks = 34 + random.nextInt(25);
        Vec3 origin = center == null ? position() : center;
        double radius = 3.0D + random.nextDouble() * 6.0D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        getNavigation().moveTo(origin.x + Math.cos(angle) * radius,
                origin.y + random.nextInt(5) - 2,
                origin.z + Math.sin(angle) * radius, 0.72D);
    }

    private void wander() {
        if (--idleTicks > 0 && !getNavigation().isDone()) return;
        idleTicks = 55 + random.nextInt(91);
        if (random.nextFloat() < 0.36F) {
            setAction(ACTION_LISTEN, LISTEN_TICKS);
            return;
        }
        Vec3 center = encounterAnchor == null ? position() : encounterAnchor;
        double radius = 5.0D + random.nextDouble() * 8.0D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        getNavigation().moveTo(center.x + Math.cos(angle) * radius,
                center.y + random.nextInt(5) - 2,
                center.z + Math.sin(angle) * radius, 0.62D);
    }

    private void maybeMimic(ServerLevel server) {
        if (nextMimic > 0 || snapshot == null) return;
        nextMimic = 20 * (15 + random.nextInt(21));
        Scp939AwarenessState state = snapshot.state();
        if (state != Scp939AwarenessState.IDLE && state != Scp939AwarenessState.SEARCH
                && state != Scp939AwarenessState.LOST_SEARCH) return;
        if (Scp939MimicryHooks.request(server, getUUID(), position(), preferredMimic))
            setAction(ACTION_MIMIC, MIMIC_TICKS);
    }

    private ServerPlayer findLocalPrey(ServerLevel level, double range) {
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (validPrey(player) && distanceToSqr(player) <= range * range && hasPhysicalLine(player))
                candidates.add(player);
        }
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean validPrey(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private boolean hasPhysicalLine(ServerPlayer player) {
        Vec3 to = player.position().add(0, player.getBbHeight() * 0.58D, 0);
        BlockHitResult hit = level().clip(new ClipContext(getEyePosition(), to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) <= 0.35D;
    }

    private void orientToward(Vec3 point) {
        if (point == null) return;
        Vec3 delta = point.subtract(position());
        if (delta.horizontalDistanceSqr() < 0.001D) return;
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    public void beginNaturalEncounter(ServerPlayer trigger, Vec3 anchor) {
        routineEncounter = true;
        encounterTrigger = trigger == null ? null : trigger.getUUID();
        encounterAnchor = anchor == null ? position() : anchor;
        quietTicks = 0;
    }

    private void tickRoutineEncounter(ServerLevel level) {
        if (!routineEncounter) return;
        boolean quiet = snapshot == null || snapshot.evidenceAgeTicks() > 160;
        quietTicks = quiet ? quietTicks + 1 : Math.max(0, quietTicks - 4);
        ServerPlayer trigger = encounterTrigger == null ? null : level.getServer().getPlayerList().getPlayer(encounterTrigger);
        boolean triggerGone = trigger == null || trigger.level() != level || trigger.isSpectator()
                || encounterAnchor != null && trigger.position().distanceToSqr(encounterAnchor) > 48 * 48;
        if (quietTicks >= 20 * 60 || quietTicks >= 20 * 30 && triggerGone) discard();
    }

    private void setAwareness(Scp939AwarenessState state) {
        entityData.set(AWARENESS, (byte) (state == null ? 0 : state.ordinal()));
    }

    public Scp939AwarenessState getAwarenessState() {
        int i = Byte.toUnsignedInt(entityData.get(AWARENESS));
        return Scp939AwarenessState.values()[Math.min(i, Scp939AwarenessState.values().length - 1)];
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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && isAlive() && getAction() == ACTION_NONE) {
            setAction(ACTION_HURT, HURT_TICKS);
            getNavigation().stop();
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        if (isDeadOrDying()) return;
        setAction(ACTION_DEATH, DEATH_TICKS);
        getNavigation().stop();
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DEATH_TICKS && !level().isClientSide) remove(RemovalReason.KILLED);
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public boolean requiresCustomPersistence() { return true; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("RoutineEncounter", routineEncounter);
        if (encounterAnchor != null) {
            tag.putDouble("EncounterAnchorX", encounterAnchor.x);
            tag.putDouble("EncounterAnchorY", encounterAnchor.y);
            tag.putDouble("EncounterAnchorZ", encounterAnchor.z);
        }
        if (encounterTrigger != null) tag.putUUID("EncounterTrigger", encounterTrigger);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        routineEncounter = tag.getBoolean("RoutineEncounter");
        if (tag.contains("EncounterAnchorX")) encounterAnchor = new Vec3(tag.getDouble("EncounterAnchorX"), tag.getDouble("EncounterAnchorY"), tag.getDouble("EncounterAnchorZ"));
        encounterTrigger = tag.hasUUID("EncounterTrigger") ? tag.getUUID("EncounterTrigger") : null;
        acousticBrain.reset();
        clearAction();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            byte action = getAction();
            if (action == ACTION_BITE) return state.setAndContinue(BITE);
            if (action == ACTION_POUNCE) return state.setAndContinue(POUNCE);
            if (action == ACTION_HURT) return state.setAndContinue(HURT);
            if (action == ACTION_DEATH) return state.setAndContinue(DEATH);
            if (action == ACTION_MIMIC) return state.setAndContinue(MIMIC);
            if (action == ACTION_LISTEN) return state.setAndContinue(IDLE_LISTEN);
            Scp939AwarenessState awareness = getAwarenessState();
            if (state.isMoving()) return state.setAndContinue(
                    awareness == Scp939AwarenessState.CONFIRMED_HUNT || awareness == Scp939AwarenessState.LOST_SEARCH ? RUN : WALK);
            if (awareness == Scp939AwarenessState.SEARCH) return state.setAndContinue(SEARCH);
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
