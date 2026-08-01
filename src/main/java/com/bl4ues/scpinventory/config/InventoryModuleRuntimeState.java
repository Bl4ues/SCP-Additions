package com.bl4ues.scpinventory.config;

import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/** Client view of server-authoritative module settings needed by client-only systems. */
public final class InventoryModuleRuntimeState {
    private static volatile Boolean serverEnabled;
    private static volatile Boolean serverReduceScp012VisualEffects;
    private static volatile Boolean serverHungerDisabled;
    private static volatile Boolean serverReplacePlayerHurtSounds;
    private static volatile Boolean serverUseVoiceProfileB;
    private static volatile Boolean serverMuteNonPlayerHitSounds;
    private static volatile Boolean serverDisableVanillaMusic;
    private static volatile Boolean serverHideActiveEffectIndicators;
    private static volatile Boolean serverHideEmptyHand;
    private static volatile Boolean serverDisableExperienceBar;
    private static volatile Boolean serverCustomOxygenBar;
    private static volatile Boolean serverCustomCrosshairEnabled;
    private static volatile Boolean serverInGameCrosshairEnabled;
    private static volatile Float serverCrosshairRed;
    private static volatile Float serverCrosshairGreen;
    private static volatile Float serverCrosshairBlue;
    private static volatile Float serverCrosshairAlpha;

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

    public static boolean useVoiceProfileBForClient() {
        Boolean synced = serverUseVoiceProfileB;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().audio.useVoiceProfileB;
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

    public static boolean hideEmptyHandForClient() {
        Boolean synced = serverHideEmptyHand;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().hud.hideEmptyHand;
    }

    public static boolean disableExperienceBarForClient() {
        Boolean synced = serverDisableExperienceBar;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().hud.disableExperienceBar;
    }

    public static boolean customOxygenBarForClient() {
        Boolean synced = serverCustomOxygenBar;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().hud.customOxygenBar;
    }

    public static boolean customCrosshairEnabledForClient() {
        Boolean synced = serverCustomCrosshairEnabled;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().crosshair.enabled;
    }

    public static boolean inGameCrosshairEnabledForClient() {
        Boolean synced = serverInGameCrosshairEnabled;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().crosshair.inGameEnabled;
    }

    public static float crosshairRedForClient() {
        Float synced = serverCrosshairRed;
        return clampUnit(synced != null ? synced
                : (float) ScpAdditionsModulesConfig.get().crosshair.red);
    }

    public static float crosshairGreenForClient() {
        Float synced = serverCrosshairGreen;
        return clampUnit(synced != null ? synced
                : (float) ScpAdditionsModulesConfig.get().crosshair.green);
    }

    public static float crosshairBlueForClient() {
        Float synced = serverCrosshairBlue;
        return clampUnit(synced != null ? synced
                : (float) ScpAdditionsModulesConfig.get().crosshair.blue);
    }

    public static float crosshairAlphaForClient() {
        Float synced = serverCrosshairAlpha;
        return clampUnit(synced != null ? synced
                : (float) ScpAdditionsModulesConfig.get().crosshair.alpha);
    }

    public static void updateFromServer(boolean enabled,
            boolean reduceScp012VisualEffects, boolean hungerDisabled,
            boolean replacePlayerHurtSounds,
            boolean useVoiceProfileB,
            boolean muteNonPlayerHitSounds,
            boolean disableVanillaMusic,
            boolean hideActiveEffectIndicators,
            boolean hideEmptyHand,
            boolean disableExperienceBar,
            boolean customOxygenBar,
            boolean customCrosshairEnabled,
            boolean inGameCrosshairEnabled,
            float crosshairRed, float crosshairGreen,
            float crosshairBlue, float crosshairAlpha) {
        serverEnabled = enabled;
        serverReduceScp012VisualEffects = reduceScp012VisualEffects;
        serverHungerDisabled = hungerDisabled;
        serverReplacePlayerHurtSounds = replacePlayerHurtSounds;
        serverUseVoiceProfileB = useVoiceProfileB;
        serverMuteNonPlayerHitSounds = muteNonPlayerHitSounds;
        serverDisableVanillaMusic = disableVanillaMusic;
        serverHideActiveEffectIndicators = hideActiveEffectIndicators;
        serverHideEmptyHand = hideEmptyHand;
        serverDisableExperienceBar = disableExperienceBar;
        serverCustomOxygenBar = customOxygenBar;
        serverCustomCrosshairEnabled = customCrosshairEnabled;
        serverInGameCrosshairEnabled = inGameCrosshairEnabled;
        serverCrosshairRed = clampUnit(crosshairRed);
        serverCrosshairGreen = clampUnit(crosshairGreen);
        serverCrosshairBlue = clampUnit(crosshairBlue);
        serverCrosshairAlpha = clampUnit(crosshairAlpha);
    }

    public static void clearServerState() {
        serverEnabled = null;
        serverReduceScp012VisualEffects = null;
        serverHungerDisabled = null;
        serverReplacePlayerHurtSounds = null;
        serverUseVoiceProfileB = null;
        serverMuteNonPlayerHitSounds = null;
        serverDisableVanillaMusic = null;
        serverHideActiveEffectIndicators = null;
        serverHideEmptyHand = null;
        serverDisableExperienceBar = null;
        serverCustomOxygenBar = null;
        serverCustomCrosshairEnabled = null;
        serverInGameCrosshairEnabled = null;
        serverCrosshairRed = null;
        serverCrosshairGreen = null;
        serverCrosshairBlue = null;
        serverCrosshairAlpha = null;
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
