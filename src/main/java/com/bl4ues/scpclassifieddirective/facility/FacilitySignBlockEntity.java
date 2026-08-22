package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class FacilitySignBlockEntity extends BlockEntity {
    private static final String ENTRIES_KEY = "Entries";
    private static final String NUMBER_KEY = "Number";
    private static final String TEXT_KEY = "Text";

    private List<FacilitySignData.Entry> entries = FacilitySignData.normalize(
            FacilitySignBlock.SignType.CORE_ROOM, List.of());

    public FacilitySignBlockEntity(BlockPos pos, BlockState state) {
        super(FacilityModule.FACILITY_SIGN_BLOCK_ENTITY.get(), pos, state);
        entries = FacilitySignData.normalize(type(), List.of());
    }

    public FacilitySignBlock.SignType type() {
        return getBlockState().getBlock() instanceof FacilitySignBlock sign
                ? sign.type() : FacilitySignBlock.SignType.CORE_ROOM;
    }

    public List<FacilitySignData.Entry> entries() {
        return List.copyOf(entries);
    }

    public void setEntries(List<FacilitySignData.Entry> updated) {
        entries = FacilitySignData.normalize(type(), updated);
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (FacilitySignData.Entry entry : entries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(NUMBER_KEY, entry.number());
            entryTag.putString(TEXT_KEY, entry.text());
            list.add(entryTag);
        }
        tag.put(ENTRIES_KEY, list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        List<FacilitySignData.Entry> loaded = new ArrayList<>();
        ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(FacilitySignData.ENTRY_COUNT, list.size()); i++) {
            CompoundTag entryTag = list.getCompound(i);
            loaded.add(new FacilitySignData.Entry(
                    entryTag.getString(NUMBER_KEY), entryTag.getString(TEXT_KEY)));
        }
        entries = FacilitySignData.normalize(type(), loaded);
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
        return new AABB(worldPosition).inflate(2.0D);
    }
}
