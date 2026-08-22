package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.LoadingErrorScreen;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives vanilla/default-font menu text the SCP: Classified Directive Roboto face while the
 * custom menu presentation is enabled. Explicitly authored fonts such as
 * Montserrat and Titillium are left untouched.
 */
@Mixin(Style.class)
public abstract class StyleFontMixin {
    @Inject(method = "getFont", at = @At("RETURN"), cancellable = true)
    private void scpClassifiedDirective$useRobotoInMenus(
            CallbackInfoReturnable<ResourceLocation> callback) {
        if (!ClientModulePreferences.customMainMenuEnabled()) return;
        if (Minecraft.getInstance().screen == null) return;

        // Forge can display this screen before mod resources have been loaded.
        // Keeping its vanilla font prevents missing-glyph boxes from hiding the
        // actual loading error that the player needs to read.
        if (Minecraft.getInstance().screen instanceof LoadingErrorScreen) return;

        ResourceLocation font = callback.getReturnValue();
        if (font != null
                && "minecraft".equals(font.getNamespace())
                && "default".equals(font.getPath())) {
            callback.setReturnValue(ScpFonts.ROBOTO);
        }
    }
}
