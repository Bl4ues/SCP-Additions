package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.sounds.SoundManager;
import net.mcreator.scpadditions.client.CustomAdvancementToastClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Toast.Visibility.class)
public abstract class ToastVisibilityMixin {
    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$suppressCustomAdvancementTransitionSound(
            SoundManager soundManager, CallbackInfo ci) {
        if (CustomAdvancementToastClient
                .consumeVanillaTransitionSoundSuppression()) {
            ci.cancel();
        }
    }
}
