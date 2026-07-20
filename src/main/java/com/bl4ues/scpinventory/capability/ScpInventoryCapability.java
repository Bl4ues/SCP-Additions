package com.bl4ues.scpinventory.capability;

import net.neoforged.neoforge.capabilities.Capability;
import net.neoforged.neoforge.capabilities.CapabilityManager;
import net.neoforged.neoforge.capabilities.CapabilityToken;

public class ScpInventoryCapability {

    public static final Capability<IScpInventory> INSTANCE =
            CapabilityManager.get(new CapabilityToken<>() {});
}

