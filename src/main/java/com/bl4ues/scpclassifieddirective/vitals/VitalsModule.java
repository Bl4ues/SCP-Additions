package com.bl4ues.scpclassifieddirective.vitals;

import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;

/**
 * Centralized feature-gate helpers for the integrated health, stamina and
 * survival-horror movement systems.
 */
public final class VitalsModule {
    private VitalsModule() {
    }

    /** Controls client rendering only; stamina gameplay remains host-owned. */
    public static boolean hudEnabled() {
        return InventoryModuleRuntimeState.hudEnabledForClient();
    }

    /** Custom health bar is visible and vanilla hearts should be hidden. */
    public static boolean healthHudEnabled() {
        return hudEnabled()
                && InventoryModuleRuntimeState
                .customHealthEnabledForClient();
    }

    /** Server/client stamina drain, regeneration and sprint enforcement. */
    public static boolean staminaEnabled() {
        return ScpClassifiedDirectiveModulesConfig.get().vitals.staminaEnabled;
    }

    /** Slower walking and slightly faster committed sprinting. */
    public static boolean horrorMovementEnabled() {
        return ScpClassifiedDirectiveModulesConfig.get().vitals.horrorMovementEnabled;
    }

    /** Stamina bar visibility is independent from stamina gameplay. */
    public static boolean staminaHudEnabled() {
        return hudEnabled() && staminaEnabled();
    }

    public static boolean anyHudEnabled() {
        return healthHudEnabled() || staminaHudEnabled();
    }
}
