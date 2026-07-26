package com.bl4ues.scpinventory.config;

import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/**
 * Client view of server-authoritative module settings needed by client-only
 * systems.
 *
 * Integrated singleplayer can fall back to the local module configuration,
 * while dedicated-server clients use the value synchronized at login/reload.
 */
public final class InventoryModuleRuntimeState {
    private static volatile Boolean serverEnabled;
    private static volatile Boolean serverReduceScp012VisualEffects;
    private static volatile Boolean serverHungerDisabled;

    private InventoryModuleRuntimeState() {
    }

    public static boolean isEnabledForClient() {
        Boolean synced = serverEnabled;
        return synced != null ? synced : ScpAdditionsModulesConfig.get().inventory.enabled;
    }

    public static boolean reduceScp012VisualEffectsForClient() {
        Boolean synced = serverReduceScp012VisualEffects;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().accessibility
                .reduceScp012VisualEffects;
    }

    public static boolean hungerDisabledForClient() {
        Boolean synced = serverHungerDisabled;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().hunger.disabled;
    }

    public static void updateFromServer(boolean enabled,
                                        boolean reduceScp012VisualEffects,
                                        boolean hungerDisabled) {
        serverEnabled = enabled;
        serverReduceScp012VisualEffects = reduceScp012VisualEffects;
        serverHungerDisabled = hungerDisabled;
    }

    public static void clearServerState() {
        serverEnabled = null;
        serverReduceScp012VisualEffects = null;
        serverHungerDisabled = null;
    }
}
