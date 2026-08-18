package net.mcreator.scpadditions.mixin.client;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import net.mcreator.scpadditions.client.CustomAdvancementToastClient;
import net.mcreator.scpadditions.sound.AchievementSounds;
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

    @Unique private boolean scpAdditions$customSoundPlayed;
    @Unique private boolean scpAdditions$showSoundSuppressed;
    @Unique private boolean scpAdditions$hideSoundSuppressed;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$renderCustomAdvancementToast(
            GuiGraphics graphics, ToastComponent toastComponent,
            long age, CallbackInfoReturnable<Toast.Visibility> cir) {
        if (!ClientModulePreferences.customAdvancementToastsEnabled()) return;
        DisplayInfo display = advancement.getDisplay();
        if (display == null) return;

        if (!scpAdditions$customSoundPlayed) {
            scpAdditions$customSoundPlayed = true;
            toastComponent.getMinecraft().getSoundManager().play(
                    SimpleSoundInstance.forUI(
                            AchievementSounds.ACHIEVEMENT.get(), 1.0F));
        }

        Toast.Visibility visibility = CustomAdvancementToastClient.render(
                graphics, toastComponent, display, age);
        if (!scpAdditions$showSoundSuppressed) {
            scpAdditions$showSoundSuppressed = true;
            CustomAdvancementToastClient
                    .armVanillaTransitionSoundSuppression();
        } else if (visibility == Toast.Visibility.HIDE
                && !scpAdditions$hideSoundSuppressed) {
            scpAdditions$hideSoundSuppressed = true;
            CustomAdvancementToastClient
                    .armVanillaTransitionSoundSuppression();
        }
        cir.setReturnValue(visibility);
    }

    /**
     * Soft-implements Toast#width so Mixin's annotation processor remaps the
     * inherited default method name into production instead of merging a
     * development-only literal `width` method into AdvancementToast.
     */
    public int scpToast$width() {
        return ClientModulePreferences.customAdvancementToastsEnabled()
                ? CustomAdvancementToastClient.WIDTH : 160;
    }

    /** Same mapping-safe override strategy as scpToast$width(). */
    public int scpToast$height() {
        return ClientModulePreferences.customAdvancementToastsEnabled()
                ? CustomAdvancementToastClient.HEIGHT : 32;
    }
}
