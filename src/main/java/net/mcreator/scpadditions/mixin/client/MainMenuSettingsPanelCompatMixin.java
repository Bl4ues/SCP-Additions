package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.mcreator.scpadditions.client.CustomMainMenuScreen;
import net.mcreator.scpadditions.client.MainMenuSettingsPanelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Builds the title-menu Options probe without mapped-name reflection. */
@Mixin(value = MainMenuSettingsPanelClient.class, remap = false)
public abstract class MainMenuSettingsPanelCompatMixin {
    @Inject(method = "createOptionsProbe", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$createProductionSafeProbe(
            CustomMainMenuScreen parent,
            CallbackInfoReturnable<Screen> callback) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen probe = new OptionsScreen(parent, minecraft.options);
        ((ScreenInvoker) (Object) probe).scpAdditions$invokeInit(
                minecraft, parent.width, parent.height);
        callback.setReturnValue(probe);
    }
}
