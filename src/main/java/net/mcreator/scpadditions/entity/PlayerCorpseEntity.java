package net.mcreator.scpadditions.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.compat.MineZeroDeathCoordinator;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-owned physical remnant of a dead player.
 *
 * <p>The entity is intentionally inert but remains pickable so future SCP,
 * recovery, and body-interaction systems can target it without having to
 * retrofit a purely visual corpse later.</p>
 */
public final class PlayerCorpseEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> LOGICAL_DEATH =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.BOOLEAN);

    public PlayerCorpseEntity(EntityType<? extends PlayerCorpseEntity> type,
            Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoAi(true);
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
        // A corpse does not pathfind, look around, retaliate, or wander.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER_ID, Optional.empty());
        entityData.define(OWNER_NAME, "");
        entityData.define(LOGICAL_DEATH, false);
    }

    public void initializeFrom(ServerPlayer player, boolean logicalDeath) {
        if (player == null) return;
        entityData.set(OWNER_ID, Optional.of(player.getUUID()));
        entityData.set(OWNER_NAME, player.getGameProfile().getName());
        entityData.set(LOGICAL_DEATH, logicalDeath);
        moveTo(player.getX(), player.getY(), player.getZ(),
                player.yBodyRot, 0.0F);
        setYBodyRot(player.yBodyRot);
        setYHeadRot(player.yBodyRot);
        yBodyRotO = player.yBodyRot;
        yHeadRotO = player.yBodyRot;
        setDeltaMovement(Vec3.ZERO);
        hasImpulse = true;
    }

    public UUID ownerId() {
        return entityData.get(OWNER_ID).orElse(null);
    }

    public String ownerName() {
        return entityData.get(OWNER_NAME);
    }

    public boolean logicalDeath() {
        return entityData.get(LOGICAL_DEATH);
    }

    @Override
    public void tick() {
        setNoAi(true);
        setMaxUpStep(0.0F);
        super.tick();

        Vec3 movement = getDeltaMovement();
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
        } else {
            // Let gravity settle a body that died in mid-air without allowing
            // horizontal AI or collision impulses to drag it away.
            setDeltaMovement(0.0D, movement.y, 0.0D);
        }
        getNavigation().stop();
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());

        if (!level().isClientSide && logicalDeath()
                && level() instanceof ServerLevel serverLevel) {
            UUID id = ownerId();
            ServerPlayer owner = id == null ? null
                    : serverLevel.getServer().getPlayerList().getPlayer(id);
            // MineZero rewinds remove bodies created by the discarded timeline.
            if (owner == null || !MineZeroDeathCoordinator.isLogicallyDead(owner)) {
                discard();
            }
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID id = ownerId();
        if (id != null) tag.putUUID("Owner", id);
        tag.putString("OwnerName", ownerName());
        tag.putBoolean("LogicalDeath", logicalDeath());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER_ID, tag.hasUUID("Owner")
                ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        entityData.set(LOGICAL_DEATH, tag.getBoolean("LogicalDeath"));
        setNoAi(true);
        setPersistenceRequired();
    }
}
