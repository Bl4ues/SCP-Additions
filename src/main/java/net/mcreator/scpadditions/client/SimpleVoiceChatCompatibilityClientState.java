package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.compat.ModCompatibilityConfig;
import net.mcreator.scpadditions.compat.SimpleVoiceChatPresence;
import net.mcreator.scpadditions.network.SimpleVoiceChatCompatibilityRequestPacket;

/** Cached server-owned Simple Voice Chat integration state for the config UI. */
public final class SimpleVoiceChatCompatibilityClientState {
    private static volatile boolean known;
    private static volatile boolean installed;
    private static volatile boolean enabled;
    private static volatile boolean canEdit;

    private SimpleVoiceChatCompatibilityClientState() {
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
            receive(SimpleVoiceChatPresence.installed(),
                    ModCompatibilityConfig.simpleVoiceChatEnabled(), true);
            return;
        }
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new SimpleVoiceChatCompatibilityRequestPacket(false, false));
    }

    public static void toggle() {
        if (!known || !installed || !canEdit) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            boolean next = !enabled;
            if (ModCompatibilityConfig.setSimpleVoiceChatEnabled(next)) {
                receive(installed, next, true);
            }
            return;
        }
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new SimpleVoiceChatCompatibilityRequestPacket(true, !enabled));
    }

    public static boolean known() { return known; }
    public static boolean installed() { return installed; }
    public static boolean enabled() { return enabled; }
    public static boolean canEdit() { return canEdit; }
}
