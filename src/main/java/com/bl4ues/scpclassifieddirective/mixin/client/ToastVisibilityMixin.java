package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.sounds.SoundManager;
import com.bl4ues.scpclassifieddirective.client.CustomAdvancementToastClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Toast.Visibility.class)
public abstract class ToastVisibilityMixin {
    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$suppressCustomAdvancementTransitionSound(
            SoundManager soundManager, CallbackInfo ci) {
        if (CustomAdvancementToastClient
                .consumeVanillaTransitionSoundSuppression()) {
            ci.cancel();
        }
    }
}
