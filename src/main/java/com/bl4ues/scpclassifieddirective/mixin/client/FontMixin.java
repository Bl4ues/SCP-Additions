package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.Font;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Removes the shadow pass from normal Minecraft text when the personal toggle is enabled. */
@Mixin(Font.class)
public abstract class FontMixin {
    @ModifyVariable(
  method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
  at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean scpClassifiedDirective$disableStringShadow(boolean dropShadow) {
        return ClientModulePreferences.disableTextDropShadows()
      ? false : dropShadow;
    }

    @ModifyVariable(
  method = "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I",
  at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean scpClassifiedDirective$disableFormattedShadow(boolean dropShadow) {
        return ClientModulePreferences.disableTextDropShadows()
      ? false : dropShadow;
    }
}
