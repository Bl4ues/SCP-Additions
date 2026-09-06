package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.MineZeroScpCheckpoint;

/** Persistent selected pictogram for a Hazard Sign. */
public final class HazardSignBlockEntity extends BlockEntity {
    private static final String HAZARD_KEY = "Hazard";

    private String hazardId = "";

    public HazardSignBlockEntity(BlockPos pos, BlockState state) {
        super(HazardSignModule.BLOCK_ENTITY.get(), pos, state);
    }

    public String hazardId() {
        return hazardId;
    }

    public void setHazardId(String id) {
        String clean = ScpSignHazards.normalizeId(id);
        if (clean.equals(hazardId)) return;
        if (level instanceof ServerLevel serverLevel) {
            try {
                MineZeroScpCheckpoint.recordBlockBeforeChange(serverLevel,
                        worldPosition, getBlockState());
            } catch (RuntimeException exception) {
                ScpClassifiedDirectiveMod.LOGGER.error(
                        "Could not journal Hazard Sign {} before editing; applying the edit without a MineZero rollback entry",
                        worldPosition, exception);
            }
        }
        hazardId = clean;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(HAZARD_KEY, hazardId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hazardId = ScpSignHazards.normalizeId(tag.getString(HAZARD_KEY));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

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
}
