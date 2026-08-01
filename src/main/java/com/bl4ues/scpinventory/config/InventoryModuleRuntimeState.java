package com.bl4ues.scpinventory.config;

import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/**
 * Client view of host-authoritative gameplay rules plus local presentation
 * preferences. Host packets never overwrite fields that belong to one player.
 */
public final class InventoryModuleRuntimeState {
    private static volatile Boolean serverEnabled;
    private static volatile Boolean serverRequireEquippedWeaponToAttack;
    private static volatile Boolean serverHungerDisabled;

    private static volatile Boolean localHudEnabled;
    private static volatile Boolean localCustomHealthEnabled;
    private static volatile Boolean localCustomHotbar;
    private static volatile Boolean localReduceScp012VisualEffects;
    private static volatile Boolean localEnterSoundEnabled;
    private static volatile Boolean localSaveGameSoundEnabled;
    private static volatile Boolean localReplacePlayerHurtSounds;
    private static volatile Boolean localUseVoiceProfileB;
    private static volatile Boolean localMuteNonPlayerHitSounds;
    private static volatile Boolean localDisableVanillaMusic;
    private static volatile Boolean localHideActiveEffectIndicators;
    private static volatile Boolean localHideEmptyHand;
    private static volatile Boolean localDisableExperienceBar;
    private static volatile Boolean localCustomOxygenBar;
    private static volatile Boolean localActionBarsRoboto;
    private static volatile Boolean localCustomCrosshairEnabled;
    private static volatile Boolean localInGameCrosshairEnabled;
    private static volatile Float localCrosshairRed;
    private static volatile Float localCrosshairGreen;
    private static volatile Float localCrosshairBlue;
    private static volatile Float localCrosshairAlpha;

    private InventoryModuleRuntimeState() {
    }

    public static boolean isEnabledForClient() {
        Boolean synced = serverEnabled;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().inventory.enabled;
    }

    public static boolean requireEquippedWeaponToAttackForClient() {
        Boolean synced = serverRequireEquippedWeaponToAttack;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().inventory
                .requireEquippedWeaponToAttack;
    }

    public static boolean hudEnabledForClient() {
        Boolean local = localHudEnabled;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().hud.enabled;
    }

    public static boolean customHealthEnabledForClient() {
        Boolean local = localCustomHealthEnabled;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().vitals.customHealthEnabled;
    }

    public static boolean customHotbarForClient() {
        Boolean local = localCustomHotbar;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().inventory.customHotbar;
    }

    public static boolean reduceScp012VisualEffectsForClient() {
        Boolean local = localReduceScp012VisualEffects;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().accessibility
                .reduceScp012VisualEffects;
    }

    public static boolean hungerDisabledForClient() {
        Boolean synced = serverHungerDisabled;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().hunger.disabled;
    }

    public static boolean enterSoundEnabledForClient() {
        Boolean local = localEnterSoundEnabled;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().audio.enterSoundEnabled;
    }

    public static boolean saveGameSoundEnabledForClient() {
        Boolean local = localSaveGameSoundEnabled;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().audio.saveGameSoundEnabled;
    }

    public static boolean replacePlayerHurtSoundsForClient() {
        Boolean local = localReplacePlayerHurtSounds;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().audio.replacePlayerHurtSounds;
    }

    public static boolean useVoiceProfileBForClient() {
        Boolean local = localUseVoiceProfileB;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().audio.useVoiceProfileB;
    }

    public static boolean muteNonPlayerHitSoundsForClient() {
        Boolean local = localMuteNonPlayerHitSounds;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().audio.muteNonPlayerHitSounds;
    }

    public static boolean disableVanillaMusicForClient() {
        Boolean local = localDisableVanillaMusic;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().audio.disableVanillaMusic;
    }

    public static boolean hideActiveEffectIndicatorsForClient() {
        Boolean local = localHideActiveEffectIndicators;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().hud
                .hideActiveEffectIndicators;
    }

    public static boolean hideEmptyHandForClient() {
        Boolean local = localHideEmptyHand;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().hud.hideEmptyHand;
    }

    public static boolean disableExperienceBarForClient() {
        Boolean local = localDisableExperienceBar;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().hud.disableExperienceBar;
    }

    public static boolean customOxygenBarForClient() {
        Boolean local = localCustomOxygenBar;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().hud.customOxygenBar;
    }

    public static boolean actionBarsRobotoForClient() {
        Boolean local = localActionBarsRoboto;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().hud.actionBarsRoboto;
    }

    public static boolean customCrosshairEnabledForClient() {
        Boolean local = localCustomCrosshairEnabled;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().crosshair.enabled;
    }

    public static boolean inGameCrosshairEnabledForClient() {
        Boolean local = localInGameCrosshairEnabled;
        return local != null ? local
                : ScpAdditionsModulesConfig.get().crosshair.inGameEnabled;
    }

    public static float crosshairRedForClient() {
        Float local = localCrosshairRed;
        return clampUnit(local != null ? local
                : (float) ScpAdditionsModulesConfig.get().crosshair.red);
    }

    public static float crosshairGreenForClient() {
        Float local = localCrosshairGreen;
        return clampUnit(local != null ? local
                : (float) ScpAdditionsModulesConfig.get().crosshair.green);
    }

    public static float crosshairBlueForClient() {
        Float local = localCrosshairBlue;
        return clampUnit(local != null ? local
                : (float) ScpAdditionsModulesConfig.get().crosshair.blue);
    }

    public static float crosshairAlphaForClient() {
        Float local = localCrosshairAlpha;
        return clampUnit(local != null ? local
                : (float) ScpAdditionsModulesConfig.get().crosshair.alpha);
    }

    public static void updateLocalPreferences(boolean hudEnabled,
            boolean customHealthEnabled, boolean customHotbar,
            boolean reduceScp012VisualEffects,
            boolean enterSoundEnabled, boolean saveGameSoundEnabled,
            boolean replacePlayerHurtSounds, boolean useVoiceProfileB,
            boolean muteNonPlayerHitSounds, boolean disableVanillaMusic,
            boolean hideActiveEffectIndicators, boolean hideEmptyHand,
            boolean disableExperienceBar, boolean customOxygenBar,
            boolean actionBarsRoboto,
            boolean customCrosshairEnabled,
            boolean inGameCrosshairEnabled,
            float crosshairRed, float crosshairGreen,
            float crosshairBlue, float crosshairAlpha) {
        localHudEnabled = hudEnabled;
        localCustomHealthEnabled = customHealthEnabled;
        localCustomHotbar = customHotbar;
        localReduceScp012VisualEffects = reduceScp012VisualEffects;
        localEnterSoundEnabled = enterSoundEnabled;
        localSaveGameSoundEnabled = saveGameSoundEnabled;
        localReplacePlayerHurtSounds = replacePlayerHurtSounds;
        localUseVoiceProfileB = useVoiceProfileB;
        localMuteNonPlayerHitSounds = muteNonPlayerHitSounds;
        localDisableVanillaMusic = disableVanillaMusic;
        localHideActiveEffectIndicators = hideActiveEffectIndicators;
        localHideEmptyHand = hideEmptyHand;
        localDisableExperienceBar = disableExperienceBar;
        localCustomOxygenBar = customOxygenBar;
        localActionBarsRoboto = actionBarsRoboto;
        localCustomCrosshairEnabled = customCrosshairEnabled;
        localInGameCrosshairEnabled = inGameCrosshairEnabled;
        localCrosshairRed = clampUnit(crosshairRed);
        localCrosshairGreen = clampUnit(crosshairGreen);
        localCrosshairBlue = clampUnit(crosshairBlue);
        localCrosshairAlpha = clampUnit(crosshairAlpha);
    }

    /**
     * Only simulation-changing rules are accepted from the host. The remaining
     * packet fields are retained for protocol compatibility but deliberately
     * ignored so one player's visual and audio choices remain personal.
     */
    public static void updateFromServer(boolean enabled,
            boolean requireEquippedWeaponToAttack,
            boolean customHotbar,
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
        serverRequireEquippedWeaponToAttack = requireEquippedWeaponToAttack;
        serverHungerDisabled = hungerDisabled;
    }

    public static void clearServerState() {
        serverEnabled = null;
        serverRequireEquippedWeaponToAttack = null;
        serverHungerDisabled = null;
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
