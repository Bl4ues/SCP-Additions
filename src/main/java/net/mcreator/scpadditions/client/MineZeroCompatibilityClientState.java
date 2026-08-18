package net.mcreator.scpadditions.client;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.MineZeroCompatibilityRequestPacket;

/** Cached server-owned status used by the Mod Compatibilities screen. */
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
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new MineZeroCompatibilityRequestPacket(false, false));
    }

    public static void toggle() {
        if (!known || !installed || !canEdit) return;
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new MineZeroCompatibilityRequestPacket(true, !enabled));
    }

    public static boolean known() { return known; }
    public static boolean installed() { return installed; }
    public static boolean enabled() { return enabled; }
    public static boolean canEdit() { return canEdit; }
}
