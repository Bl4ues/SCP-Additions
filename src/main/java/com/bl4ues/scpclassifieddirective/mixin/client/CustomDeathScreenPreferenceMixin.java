package com.bl4ues.scpclassifieddirective.mixin.client;

import com.google.gson.JsonObject;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import com.bl4ues.scpclassifieddirective.client.CustomDeathScreenPreferences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends client module preferences without making the server own this toggle. */
@Mixin(value = ClientModulePreferences.class, remap = false)
public abstract class CustomDeathScreenPreferenceMixin {
    @Inject(method = "captureAndSave", at = @At("TAIL"))
    private static void scpClassifiedDirective$captureDeathScreen(JsonObject modules,
            CallbackInfo callback) {
        CustomDeathScreenPreferences.captureAndSave(modules);
    }

    @Inject(method = "applyTo", at = @At("TAIL"))
    private static void scpClassifiedDirective$applyDeathScreen(JsonObject modules,
            CallbackInfo callback) {
        CustomDeathScreenPreferences.applyTo(modules);
    }

    @Inject(method = "resetDefaults", at = @At("TAIL"))
    private static void scpClassifiedDirective$resetDeathScreen(JsonObject modules,
            CallbackInfo callback) {
        CustomDeathScreenPreferences.reset(modules);
    }

    @Inject(method = "isClientPreference", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$scopeDeathScreen(String group, String key,
            CallbackInfoReturnable<Boolean> callback) {
        if ("ui".equals(group) && "custom_death_screen".equals(key)) {
            callback.setReturnValue(true);
        }
    }
}
