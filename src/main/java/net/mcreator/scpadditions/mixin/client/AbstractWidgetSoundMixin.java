package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the vanilla button click with SCP Additions selection feedback. */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetSoundMixin {
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$replaceButtonClickSound(
            SoundManager soundManager, CallbackInfo callback) {
        if (!ClientModulePreferences.customMainMenuEnabled()) return;

        AbstractWidget widget = (AbstractWidget) (Object) this;
        if (!(widget instanceof AbstractButton)) return;

        // Loading/error screens may exist after mod construction failed, before
        // deferred registries finished binding. In that state, keep vanilla's
        // click instead of dereferencing an absent RegistryObject and masking
        // the original loading error with a second crash.
        if (!ScpAdditionsModSounds.SELECT.isPresent()) return;

        soundManager.play(SimpleSoundInstance.forUI(
                ScpAdditionsModSounds.SELECT.get(), 1.0F, 0.35F));
        callback.cancel();
    }
}
