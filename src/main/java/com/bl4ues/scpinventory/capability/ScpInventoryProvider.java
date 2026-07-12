package com.bl4ues.scpinventory.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ScpInventoryProvider implements ICapabilitySerializable<CompoundTag> {
    private final IScpInventory backend = new ScpInventory();
    private final LazyOptional<IScpInventory> optional = LazyOptional.of(() -> backend);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability,
            @Nullable Direction side) {
        return capability == ScpInventoryCapability.INSTANCE
                ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        backend.deserializeNBT(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
