package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.MineZeroCompatibility;
import com.bl4ues.scpclassifieddirective.compat.ModCompatibilityConfig;
import com.bl4ues.scpclassifieddirective.network.MineZeroCompatibilityRequestPacket;

/** Cached server-owned status used by the Mod Integrations screen. */
public final class MineZeroCompatibilityClientState {
    private static volatile boolean known;
    private static volatile boolean installed;
    private static volatile boolean enabled;
    private static volatile boolean canEdit;

    private MineZeroCompatibilityClientState() {
    }

    public static void receive(boolean isInstalled, boolean isEnabled,
            boolean editable) {
        known = true;
        installed = isInstalled;
        enabled = isEnabled;
        canEdit = editable;
    }

    public static void query() {
        known = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            // From the title screen this is the future integrated-server host's
            // local config, so it is safe and useful to edit before world entry.
            receive(ModList.get().isLoaded(MineZeroCompatibility.MOD_ID),
                    ModCompatibilityConfig.mineZeroEnabled(), true);
            return;
        }
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new MineZeroCompatibilityRequestPacket(false, false));
    }

    public static void toggle() {
        if (!known || !installed || !canEdit) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            boolean next = !enabled;
            if (ModCompatibilityConfig.setMineZeroEnabled(next)) {
                receive(installed, next, true);
            }
            return;
        }
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new MineZeroCompatibilityRequestPacket(true, !enabled));
    }

    public static boolean known() { return known; }
    public static boolean installed() { return installed; }
    public static boolean enabled() { return enabled; }
    public static boolean canEdit() { return canEdit; }
}
