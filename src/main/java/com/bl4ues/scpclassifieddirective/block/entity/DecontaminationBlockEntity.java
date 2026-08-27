package com.bl4ues.scpclassifieddirective.block.entity;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.procedures.DecontaminationCheckpointController;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/** Persistent GeckoLib/timing host for the rebuilt decontamination checkpoint. */
public final class DecontaminationBlockEntity extends BlockEntity
        implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DECONTAMINATION =
            RawAnimation.begin().thenPlay("decontamination");

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private boolean active;
    private long sequenceStartGameTime;

    public DecontaminationBlockEntity(BlockPos pos, BlockState state) {
        super(ScpClassifiedDirectiveModBlockEntities.DECONTAMINATION.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            DecontaminationBlockEntity blockEntity) {
        if (!blockEntity.active) return;
        DecontaminationCheckpointController.tickActiveSequence(level, pos, state,
                blockEntity, blockEntity.sequenceElapsedTicks());
    }

    public void beginSequence() {
        if (level == null || level.isClientSide) return;
        active = true;
        sequenceStartGameTime = level.getGameTime();
        sync();
    }

    public void clearSequence() {
        if (!active) return;
        active = false;
        sync();
    }

    public boolean isActive() {
        return active;
    }

    public long sequenceElapsedTicks() {
        return level == null ? 0L
                : Math.max(0L, level.getGameTime() - sequenceStartGameTime);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("DecontaminationActive", active);
        tag.putLong("DecontaminationStart", sequenceStartGameTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = tag.getBoolean("DecontaminationActive");
        sequenceStartGameTime = tag.getLong("DecontaminationStart");
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
    public void onDataPacket(Connection connection,
            ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) load(tag);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "decontamination", 0,
                state -> state.setAndContinue(active
                        ? DECONTAMINATION : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5.5D, 4.5D, 7.5D);
    }
}
