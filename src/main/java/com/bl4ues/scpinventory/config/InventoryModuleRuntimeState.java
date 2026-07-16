package com.bl4ues.scpinventory.config;

import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/**
 * Client view of the server-authoritative inventory module state.
 *
 * Integrated singleplayer can fall back to the local module configuration,
 * while dedicated-server clients use the value synchronized at login/reload.
 */
public final class InventoryModuleRuntimeState {
    private static volatile Boolean serverEnabled;

    private InventoryModuleRuntimeState() {
    }

    public static boolean isEnabledForClient() {
        Boolean synced = serverEnabled;
        return synced != null ? synced : ScpAdditionsModulesConfig.get().inventory.enabled;
    }

    public static void updateFromServer(boolean enabled) {
        serverEnabled = enabled;
    }

    public static void clearServerState() {
        serverEnabled = null;
    }
}
