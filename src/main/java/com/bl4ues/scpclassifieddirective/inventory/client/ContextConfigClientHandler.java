package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.client.gui.ContextAnchorEditorScreen;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import com.bl4ues.scpclassifieddirective.inventory.network.ContextConfigOpenPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ContextConfigClientHandler {
    private ContextConfigClientHandler() {
    }

    public static void open(ContextConfigOpenPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ContextAnchorEditorScreen(packet));
    }

    public static void reloadContextConfig() {
        // Keep the synchronized host snapshot authoritative on remote servers.
        ContextInteractionRegistry.reload();
        ContextPromptIcons.reload();
        ContextPromptClient.clear();
    }
}
