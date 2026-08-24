package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import com.bl4ues.scpclassifieddirective.client.AdvancementToastHudCoordination;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import com.bl4ues.scpclassifieddirective.client.CustomAdvancementToastClient;
import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import com.bl4ues.scpclassifieddirective.sound.AchievementSounds;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementToast.class)
@Implements(@Interface(iface = Toast.class, prefix = "scpToast$",
        remap = Interface.Remap.ONLY_PREFIXED))
public abstract class AdvancementToastMixin {
    @Shadow @Final private Advancement advancement;

    @Unique private boolean scpClassifiedDirective$customSoundPlayed;
    @Unique private boolean scpClassifiedDirective$showSoundSuppressed;
    @Unique private boolean scpClassifiedDirective$hideSoundSuppressed;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$renderCustomAdvancementToast(
            GuiGraphics graphics, ToastComponent toastComponent,
            long age, CallbackInfoReturnable<Toast.Visibility> cir) {
        if (!ClientModulePreferences.customAdvancementToastsEnabled()) return;
        DisplayInfo display = advancement.getDisplay();
        if (display == null) return;

        if (!scpClassifiedDirective$customSoundPlayed) {
            scpClassifiedDirective$customSoundPlayed = true;
            toastComponent.getMinecraft().getSoundManager().play(
                    SimpleSoundInstance.forUI(
                            AchievementSounds.ACHIEVEMENT.get(), 1.0F));
        }

        AdvancementToastHudCoordination.markRendered();
        ResponsiveUiScale.Context context = ResponsiveUiScale.current();
        ResponsiveUiScale.push(graphics, context);
        Toast.Visibility visibility;
        try {
            visibility = CustomAdvancementToastClient.render(
                    graphics, toastComponent, display, age);
        } finally {
            ResponsiveUiScale.pop(graphics);
        }
        if (!scpClassifiedDirective$showSoundSuppressed) {
            scpClassifiedDirective$showSoundSuppressed = true;
            CustomAdvancementToastClient
                    .armVanillaTransitionSoundSuppression();
        } else if (visibility == Toast.Visibility.HIDE
                && !scpClassifiedDirective$hideSoundSuppressed) {
            scpClassifiedDirective$hideSoundSuppressed = true;
            CustomAdvancementToastClient
                    .armVanillaTransitionSoundSuppression();
        }
        cir.setReturnValue(visibility);
    }

    /**
     * ToastComponent uses this width to anchor the card against the right edge.
     * Match it to the visual transform so GUI scale cannot move or crop the card.
     */
    public int scpToast$width() {
        if (!ClientModulePreferences.customAdvancementToastsEnabled()) return 160;
        return Math.max(1, Mth.ceil(CustomAdvancementToastClient.WIDTH
                * ResponsiveUiScale.current().scale()));
    }

    /** Same responsive slot sizing as scpToast$width(). */
    public int scpToast$height() {
        if (!ClientModulePreferences.customAdvancementToastsEnabled()) return 32;
        return Math.max(1, Mth.ceil(CustomAdvancementToastClient.HEIGHT
                * ResponsiveUiScale.current().scale()));
    }
}
