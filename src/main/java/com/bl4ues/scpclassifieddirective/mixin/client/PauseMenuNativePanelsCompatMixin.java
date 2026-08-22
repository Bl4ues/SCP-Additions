package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.stats.Stat;
import com.bl4ues.scpclassifieddirective.client.PauseMenuNativePanelsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restores statistics data access after production reobfuscation. */
@Mixin(value = PauseMenuNativePanelsClient.class, remap = false)
public abstract class PauseMenuNativePanelsCompatMixin {
    @Inject(method = "invokeNoArg", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$readStatWithoutMappedReflection(
            Object target, String methodName,
            CallbackInfoReturnable<Object> callback) {
        if (!(target instanceof Stat<?> stat)) return;
        if ("getType".equals(methodName)) {
            callback.setReturnValue(stat.getType());
        } else if ("getValue".equals(methodName)) {
            callback.setReturnValue(stat.getValue());
        }
    }

    @Inject(method = "formatStat", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$formatStatWithoutMappedReflection(
            Object target, int value,
            CallbackInfoReturnable<String> callback) {
        if (target instanceof Stat<?> stat) {
            callback.setReturnValue(stat.format(value));
        }
    }
}
