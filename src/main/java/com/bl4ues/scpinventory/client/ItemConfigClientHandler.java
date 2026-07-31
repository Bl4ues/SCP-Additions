package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.client.gui.ItemRuleEditorScreen;
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
        Minecraft.getInstance().setScreen(new ItemRuleEditorScreen(packet));
    }

    public static void reloadItemConfig() {
        // Keep the synchronized host snapshot authoritative on remote servers.
        ScpInventoryConfig.reload();
    }
}
