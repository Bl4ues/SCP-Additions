package com.bl4ues.scpinventory.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * Legacy serialization wrapper retained for source and save migration code.
 * NeoForge 1.21.1 stores the inventory through ScpInventoryCapability's data
 * attachment, so this class is no longer attached directly to entities.
 */
@Deprecated(forRemoval = false)
public final class ScpInventoryProvider {
    private final IScpInventory backend = new ScpInventory();

    public IScpInventory backend() {
        return backend;
    }

    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    public void deserializeNBT(CompoundTag nbt) {
        backend.deserializeNBT(nbt);
    }
}
