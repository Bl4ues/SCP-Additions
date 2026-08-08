package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.Font;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Removes the shadow pass from normal Minecraft text when the personal toggle is enabled. */
@Mixin(Font.class)
public abstract class FontMixin {
    @ModifyVariable(
  method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
  at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean scpAdditions$disableStringShadow(boolean dropShadow) {
        return ClientModulePreferences.disableTextDropShadows()
      ? false : dropShadow;
    }

    @ModifyVariable(
  method = "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
  at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean scpAdditions$disableFormattedShadow(boolean dropShadow) {
        return ClientModulePreferences.disableTextDropShadows()
      ? false : dropShadow;
    }
}
