package com.bl4ues.scpclassifieddirective.block.entity;

import com.bl4ues.scpclassifieddirective.block.DecontaminationStructure;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.procedures.DecontaminationCheckpointController;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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
        if (level.isClientSide) return;

        // Legacy transient states remain registered for world compatibility,
        // but the rebuilt checkpoint keeps its live state in this BlockEntity.
        if (!blockEntity.active
                && state.is(ScpClassifiedDirectiveModBlocks.DECON_CLOSED.get())) {
            DecontaminationCheckpointController.beginClosed(level,
                    pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        if (!blockEntity.active
                && state.is(ScpClassifiedDirectiveModBlocks.DECON_OPEN_RELOAD.get())) {
            DecontaminationCheckpointController.finishReload(level,
                    pos.getX(), pos.getY(), pos.getZ());
            return;
        }

        long scanPhase = level.getGameTime()
                + pos.getX() * 31L + pos.getY() * 17L + pos.getZ() * 13L;

        if (blockEntity.active) {
            // MineZero rewinds world/BlockEntity state independently from
            // transient neighbor notifications. Reasserting the two relay
            // edges every five ticks makes restored doors converge to the
            // restored sequence immediately, whether they should close or open.
            if (scanPhase % 5L == 0L
                    && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                DecontaminationStructure.nudgeOwnedDoors(level, pos,
                        state.getValue(HorizontalDirectionalBlock.FACING));
            }
            DecontaminationCheckpointController.tickActiveSequence(level, pos,
                    state, blockEntity, blockEntity.sequenceElapsedTicks());
            return;
        }

        // Stagger idle scans between checkpoints instead of scheduling transient
        // block ticks. This survives MineZero/world-time restores cleanly.
        if (scanPhase % 5L == 0L) {
            DecontaminationCheckpointController.scanOpen(level,
                    pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public boolean beginSequence() {
        if (level == null || level.isClientSide || active) return false;
        active = true;
        sequenceStartGameTime = level.getGameTime();
        sync();
        return true;
    }

    public boolean clearSequence() {
        if (!active) return false;
        active = false;
        sync();
        return true;
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
