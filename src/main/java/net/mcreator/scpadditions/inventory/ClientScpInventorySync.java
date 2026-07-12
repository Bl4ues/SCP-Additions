package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

final class ClientScpInventorySync {
    private ClientScpInventorySync() {
    }

    static void apply(CompoundTag data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || data == null) {
            return;
        }
        minecraft.player.getCapability(ScpInventoryCapability.INSTANCE)
                .ifPresent(inventory -> inventory.deserializeNBT(data));
    }
}
