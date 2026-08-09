package net.mcreator.scpadditions.mixin.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives vanilla/default-font menu text the SCP Additions Roboto face while the
 * custom menu presentation is enabled. Explicitly authored fonts such as
 * Montserrat and Titillium are left untouched.
 */
@Mixin(Style.class)
public abstract class StyleFontMixin {
    @Inject(method = "getFont", at = @At("RETURN"), cancellable = true)
    private void scpAdditions$useRobotoInMenus(
            CallbackInfoReturnable<ResourceLocation> callback) {
        if (!ClientModulePreferences.customMainMenuEnabled()) return;
        if (Minecraft.getInstance().screen == null) return;

        ResourceLocation font = callback.getReturnValue();
        if (font != null
                && "minecraft".equals(font.getNamespace())
                && "default".equals(font.getPath())) {
            callback.setReturnValue(ScpFonts.ROBOTO);
        }
    }
}
