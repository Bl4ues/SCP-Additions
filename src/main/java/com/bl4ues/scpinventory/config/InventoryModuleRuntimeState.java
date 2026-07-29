package com.bl4ues.scpinventory.config;

import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/** Client view of server-authoritative module settings needed by client-only systems. */
public final class InventoryModuleRuntimeState {
    private static volatile Boolean serverEnabled;
    private static volatile Boolean serverReduceScp012VisualEffects;
    private static volatile Boolean serverHungerDisabled;
    private static volatile Boolean serverReplacePlayerHurtSounds;
    private static volatile Boolean serverMuteNonPlayerHitSounds;
    private static volatile Boolean serverDisableVanillaMusic;
    private static volatile Boolean serverHideActiveEffectIndicators;

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

    public static boolean replacePlayerHurtSoundsForClient() {
        Boolean synced = serverReplacePlayerHurtSounds;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().audio.replacePlayerHurtSounds;
    }

    public static boolean muteNonPlayerHitSoundsForClient() {
        Boolean synced = serverMuteNonPlayerHitSounds;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().audio.muteNonPlayerHitSounds;
    }

    public static boolean disableVanillaMusicForClient() {
        Boolean synced = serverDisableVanillaMusic;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().audio.disableVanillaMusic;
    }

    public static boolean hideActiveEffectIndicatorsForClient() {
        Boolean synced = serverHideActiveEffectIndicators;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().hud
                .hideActiveEffectIndicators;
    }

    public static void updateFromServer(boolean enabled,
            boolean reduceScp012VisualEffects, boolean hungerDisabled,
            boolean replacePlayerHurtSounds,
            boolean muteNonPlayerHitSounds,
            boolean disableVanillaMusic,
            boolean hideActiveEffectIndicators) {
        serverEnabled = enabled;
        serverReduceScp012VisualEffects = reduceScp012VisualEffects;
        serverHungerDisabled = hungerDisabled;
        serverReplacePlayerHurtSounds = replacePlayerHurtSounds;
        serverMuteNonPlayerHitSounds = muteNonPlayerHitSounds;
        serverDisableVanillaMusic = disableVanillaMusic;
        serverHideActiveEffectIndicators = hideActiveEffectIndicators;
    }

    public static void clearServerState() {
        serverEnabled = null;
        serverReduceScp012VisualEffects = null;
        serverHungerDisabled = null;
        serverReplacePlayerHurtSounds = null;
        serverMuteNonPlayerHitSounds = null;
        serverDisableVanillaMusic = null;
        serverHideActiveEffectIndicators = null;
    }
}
