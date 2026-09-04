package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces vanilla widget clicks with SCP: Classified Directive selection feedback. */
@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetSoundMixin {
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$replaceButtonClickSound(
            SoundManager soundManager, CallbackInfo callback) {
        if (!ClientModulePreferences.customMainMenuEnabled()) return;

        AbstractWidget widget = (AbstractWidget) (Object) this;
        // Text fields use mouse clicks for caret/focus placement, not as actions.
        if (widget instanceof EditBox) return;

        // Loading/error screens may exist after mod construction failed, before
        // deferred registries finished binding. In that state, keep vanilla's
        // click instead of dereferencing an absent RegistryObject and masking
        // the original loading error with a second crash.
        if (!ScpClassifiedDirectiveModSounds.SELECT.isPresent()) return;

        soundManager.play(SimpleSoundInstance.forUI(
                ScpClassifiedDirectiveModSounds.SELECT.get(), 1.0F, 0.35F));
        callback.cancel();
    }
}
