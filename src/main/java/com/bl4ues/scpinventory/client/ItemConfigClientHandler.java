package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.client.gui.ItemConfigScreen;
import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import com.bl4ues.scpinventory.network.ItemConfigOpenPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ItemConfigClientHandler {
    private ItemConfigClientHandler() {
    }

    public static void open(ItemConfigOpenPacket packet) {
        Minecraft.getInstance().setScreen(new ItemConfigScreen(packet));
    }

    public static void reloadItemConfig() {
        // Keep the synchronized host snapshot authoritative on remote servers.
        ScpInventoryConfig.reload();
    }
}
