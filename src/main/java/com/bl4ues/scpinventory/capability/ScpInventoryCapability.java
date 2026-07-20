package com.bl4ues.scpinventory.capability;

import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;

public class ScpInventoryCapability {

    public static final Capability<IScpInventory> INSTANCE =
            CapabilityManager.get(new CapabilityToken<>() {});
}

