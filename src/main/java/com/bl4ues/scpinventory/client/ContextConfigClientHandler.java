package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.client.gui.ContextConfigScreen;
import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import com.bl4ues.scpinventory.network.ContextConfigOpenPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ContextConfigClientHandler {
    private ContextConfigClientHandler() {
    }

    public static void open(ContextConfigOpenPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ContextConfigScreen(packet));
    }

    public static void reloadContextConfig() {
        // Keep the synchronized host snapshot authoritative on remote servers.
        ContextInteractionRegistry.reload();
        ContextPromptIcons.reload();
        ContextPromptClient.clear();
    }
}
