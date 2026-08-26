package com.bl4ues.scpclassifieddirective.block.entity;

import com.bl4ues.scpclassifieddirective.client.TeslaGateAudioClient;
import com.bl4ues.scpclassifieddirective.client.TeslaGateElectricity;
import com.bl4ues.scpclassifieddirective.facility.FacilityStructureBreakGuard;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079TeslaSuppression;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import com.bl4ues.scpclassifieddirective.procedures.TeslaGatePulseHelper;
import com.bl4ues.scpclassifieddirective.procedures.TeslaGateVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GeckoLib host and authoritative timing controller for the replacement Tesla Gate.
 * Audio, damage and client electricity all derive from the same sequence clock.
 */
public final class TeslaGateBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int DISCHARGE_TICK = 25; // normal discharge at 1.25 s
    public static final int OVERRIDE_DISCHARGE_TICK = DISCHARGE_TICK + 20; // manual override +1 s
    public static final int NORMAL_SOUND_START_TICK = 10; // +0.75 s = normal discharge tick
    public static final int NORMAL_SEQUENCE_TICKS = 80;
    public static final int OVERRIDE_SEQUENCE_TICKS = 130;

    public enum Sequence {
        IDLE(0), NORMAL(1), OVERRIDE(2);

        private final int id;

        Sequence(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        private static Sequence byId(int id) {
            return switch (id) {
                case 1 -> NORMAL;
                case 2 -> OVERRIDE;
                default -> IDLE;
            };
        }
    }

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private Sequence sequence = Sequence.IDLE;
    private long sequenceStartGameTime;
    private boolean powered;

    public TeslaGateBlockEntity(BlockPos pos, BlockState state) {
        super(ScpClassifiedDirectiveModBlockEntities.TESLA_GATE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            TeslaGateBlockEntity gate) {
        if (!(level instanceof ServerLevel server)) return;

        boolean manualOverride = level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean gateRule = level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.TESLAGATEON);
        boolean enabled = Scp079FacilityAccessManager.isAuxiliaryPowerOnline(level)
                && (gateRule || manualOverride);
        gate.setPowered(enabled);

        if (!enabled || FacilityStructureBreakGuard.isBeingMined(level, pos)) {
            if (gate.sequence != Sequence.IDLE) gate.setSequence(Sequence.IDLE);
            return;
        }

        long elapsed = gate.sequenceElapsedTicks();
        if (gate.sequence == Sequence.NORMAL) {
            if (elapsed == NORMAL_SOUND_START_TICK) {
                level.playSound(null, pos, ScpClassifiedDirectiveModSounds.TESLA_DISCHARGE.get(),
                        SoundSource.HOSTILE, 1.55F, 1.0F);
            }
            if (elapsed == DISCHARGE_TICK) TeslaGatePulseHelper.damageAt(level, pos);
            if (elapsed >= NORMAL_SEQUENCE_TICKS) gate.setSequence(Sequence.IDLE);
            return;
        }
        if (gate.sequence == Sequence.OVERRIDE) {
            if (elapsed == OVERRIDE_DISCHARGE_TICK) {
                TeslaGatePulseHelper.damageAt(level, pos);
            }
            if (elapsed >= OVERRIDE_SEQUENCE_TICKS) gate.setSequence(Sequence.IDLE);
            return;
        }

        AABB sensor = TeslaGateVolume.sensorAt(level, pos);
        List<LivingEntity> occupants = level.getEntitiesOfClass(
                LivingEntity.class, TeslaGateVolume.motionCandidates(sensor),
                entity -> TeslaGateVolume.intersectsOrCrossed(entity, sensor));
        if (occupants.isEmpty()) return;

        AABB lethal = TeslaGateVolume.lethalArcAt(level, pos);
        List<LivingEntity> lethalOccupants = occupants.stream()
                .filter(entity -> TeslaGateVolume.intersectsOrCrossed(entity, lethal))
                .toList();
        if (Scp079TeslaSuppression.shouldSuppress(server, pos, occupants,
                lethalOccupants, manualOverride)) return;

        if (occupants.stream().anyMatch(ServerPlayer.class::isInstance)) {
            Scp079FacilityAccessManager.recordActivity(server,
                    Scp079FacilityAccessManager.Activity.TESLA_TRAVERSAL);
        }
        gate.beginSequence(manualOverride ? Sequence.OVERRIDE : Sequence.NORMAL);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
            TeslaGateBlockEntity blockEntity) {
        TeslaGateAudioClient.ensureLoop(level, pos, blockEntity);
        TeslaGateElectricity.clientTick(level, pos, state, blockEntity);
    }

    private void beginSequence(Sequence next) {
        if (level == null || level.isClientSide || next == Sequence.IDLE) return;
        setSequence(next);
        level.playSound(null, worldPosition, ScpClassifiedDirectiveModSounds.TESLA_ALARM.get(),
                SoundSource.HOSTILE, next == Sequence.OVERRIDE ? 1.8F : 1.25F, 1.0F);
        if (next == Sequence.OVERRIDE) {
            level.playSound(null, worldPosition,
                    ScpClassifiedDirectiveModSounds.TESLA_OVERRIDE_DISCHARGE.get(),
                    SoundSource.HOSTILE, 2.1F, 1.0F);
        }
    }

    private void setSequence(Sequence next) {
        if (level == null) return;
        sequence = next;
        sequenceStartGameTime = level.getGameTime();
        sync();
    }

    private void setPowered(boolean value) {
        if (powered == value) return;
        powered = value;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public Sequence getSequence() {
        return sequence;
    }

    public long sequenceElapsedTicks() {
        return level == null ? 0L : Math.max(0L, level.getGameTime() - sequenceStartGameTime);
    }

    public boolean isPowered() {
        return powered;
    }

    /** 0 immediately after the zap, then smoothly returns to 1 through cooldown. */
    public float getRecoveryFactor() {
        if (sequence == Sequence.IDLE) return powered ? 1.0F : 0.0F;
        long elapsed = sequenceElapsedTicks();
        int dischargeTick = sequence == Sequence.OVERRIDE
                ? OVERRIDE_DISCHARGE_TICK : DISCHARGE_TICK;
        if (elapsed < dischargeTick) return 1.0F;
        int end = sequence == Sequence.OVERRIDE
                ? OVERRIDE_SEQUENCE_TICKS : NORMAL_SEQUENCE_TICKS;
        return Math.min(1.0F, Math.max(0.04F,
                (elapsed - dischargeTick)
                        / (float) Math.max(1, end - dischargeTick)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("TeslaSequence", sequence.id());
        tag.putLong("TeslaSequenceStart", sequenceStartGameTime);
        tag.putBoolean("TeslaPowered", powered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        sequence = Sequence.byId(tag.getInt("TeslaSequence"));
        sequenceStartGameTime = tag.getLong("TeslaSequenceStart");
        powered = tag.getBoolean("TeslaPowered");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) load(tag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The replacement gate is static; electrical motion is particle-driven.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(4.5D, 4.5D, 3.0D);
    }
}
