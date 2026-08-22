package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.MineZeroScpCheckpoint;

import java.util.ArrayList;
import java.util.List;

/** Persistent controller data for the editable multiblock SCP sign. */
public final class ScpSignSupportBlockEntity extends BlockEntity {
    private static final String SCP_NUMBER_KEY = "ScpNumber";
    private static final String CONTAINMENT_KEY = "ContainmentClass";
    private static final String CUSTOM_CONTAINMENT_KEY = "CustomContainmentClass";
    private static final String CLEARANCE_KEY = "ClearanceLevel";
    private static final String ANOMALY_KEY = "AnomalyType";
    private static final String CUSTOM_ANOMALY_KEY = "CustomAnomalyType";
    private static final String HAZARDS_KEY = "Hazards";
    private static final String TEMPLATE_KEY = "TemplateId";

    private ScpSignData data = ScpSignData.DEFAULT;

    public ScpSignSupportBlockEntity(BlockPos pos, BlockState state) {
        super(FacilityModule.SCP_SIGN_BLOCK_ENTITY.get(), pos, state);
    }

    public ScpSignData data() {
        return data;
    }

    public void setData(ScpSignData updated) {
        if (level instanceof ServerLevel serverLevel) {
            // MineZero rollback bookkeeping is auxiliary to the sign edit. A
            // compatibility snapshot failure must never turn pressing Done into
            // a server/client crash; the edit remains authoritative even if that
            // one mutation cannot be journaled for a future rewind.
            try {
                MineZeroScpCheckpoint.recordBlockBeforeChange(serverLevel,
                        worldPosition, getBlockState());
            } catch (RuntimeException exception) {
                ScpClassifiedDirectiveMod.LOGGER.error(
                        "Could not journal SCP sign {} before editing; applying the edit without a MineZero rollback entry",
                        worldPosition, exception);
            }
        }
        data = updated == null ? ScpSignData.DEFAULT : updated;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(SCP_NUMBER_KEY, data.scpNumber());
        tag.putString(CONTAINMENT_KEY, data.containmentClass().name());
        tag.putString(CUSTOM_CONTAINMENT_KEY, data.customContainmentClass());
        tag.putInt(CLEARANCE_KEY, data.clearanceLevel());
        tag.putString(ANOMALY_KEY, data.anomalyType().name());
        tag.putString(CUSTOM_ANOMALY_KEY, data.customAnomalyType());
        tag.putString(TEMPLATE_KEY, data.templateId());
        ListTag hazards = new ListTag();
        for (String hazard : data.hazards()) {
            hazards.add(StringTag.valueOf(hazard));
        }
        tag.put(HAZARDS_KEY, hazards);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        List<String> hazards = new ArrayList<>();
        ListTag list = tag.getList(HAZARDS_KEY, Tag.TAG_STRING);
        for (int index = 0; index < Math.min(ScpSignData.HAZARD_SLOTS,
                list.size()); index++) {
            hazards.add(list.getString(index));
        }
        data = new ScpSignData(
                tag.getString(SCP_NUMBER_KEY),
                ScpSignData.ContainmentClass.parse(
                        tag.getString(CONTAINMENT_KEY)),
                tag.getString(CUSTOM_CONTAINMENT_KEY),
                tag.contains(CLEARANCE_KEY, Tag.TAG_INT)
                        ? tag.getInt(CLEARANCE_KEY) : 1,
                ScpSignData.AnomalyType.parse(tag.getString(ANOMALY_KEY)),
                tag.getString(CUSTOM_ANOMALY_KEY), hazards,
                tag.contains(TEMPLATE_KEY, Tag.TAG_STRING)
                        ? tag.getString(TEMPLATE_KEY)
                        : ScpSignTemplates.INFORMATION);
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

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.5D, 2.5D, 1.5D);
    }
}
