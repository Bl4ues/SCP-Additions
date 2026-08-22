package com.bl4ues.scpclassifieddirective.inventory.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ScpInventoryCapability {

    public static final Capability<IScpInventory> INSTANCE =
            CapabilityManager.get(new CapabilityToken<>() {});
}

