package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.client.gui.ItemConfigScreen;
import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import com.bl4ues.scpinventory.network.ItemConfigOpenPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ItemConfigClientHandler {
    private ItemConfigClientHandler() {
    }

    public static void open(ItemConfigOpenPacket packet) {
        Minecraft.getInstance().setScreen(new ItemConfigScreen(packet));
    }

    public static void reloadItemConfig() {
        ScpInventoryConfig.reloadFromDisk();
    }
}
