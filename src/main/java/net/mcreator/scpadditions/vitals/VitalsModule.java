package net.mcreator.scpadditions.vitals;

import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/**
 * Centralized feature-gate helpers for the integrated health and stamina
 * systems. Keeping rendering and gameplay checks here prevents the individual
 * handlers from drifting into different toggle semantics.
 */
public final class VitalsModule {
    private VitalsModule() {
    }

    /** Controls all custom vitals rendering, but not stamina gameplay. */
    public static boolean hudEnabled() {
        return ScpAdditionsModulesConfig.get().hud.enabled;
    }

    /** Custom health bar is visible and vanilla hearts should be hidden. */
    public static boolean healthHudEnabled() {
        return hudEnabled()
                && ScpAdditionsModulesConfig.get().vitals.customHealthEnabled;
    }

    /** Server/client stamina drain, regeneration and sprint enforcement. */
    public static boolean staminaEnabled() {
        return ScpAdditionsModulesConfig.get().vitals.staminaEnabled;
    }

    /** Stamina bar visibility is independent from stamina gameplay. */
    public static boolean staminaHudEnabled() {
        return hudEnabled() && staminaEnabled();
    }

    public static boolean anyHudEnabled() {
        return healthHudEnabled() || staminaHudEnabled();
    }
}
