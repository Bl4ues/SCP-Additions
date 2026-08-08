package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import net.mcreator.scpadditions.client.FacilityChatLayout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps command completion and usage hints attached to the relocated input box. */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final private Screen screen;
    @Shadow @Final private EditBox input;
    @Shadow @Final private int suggestionLineLimit;
    @Shadow @Final @Mutable private boolean anchorToBottom;
    @Shadow @Final @Mutable private int fillColor;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void scpAdditions$useFacilitySuggestionStyle(CallbackInfo ci) {
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return;
        this.anchorToBottom = false;
        this.fillColor = 0xE6081022;
    }

    @ModifyConstant(method = "showSuggestions",
            constant = @Constant(intValue = 72))
    private int scpAdditions$moveSuggestionList(int original) {
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return original;
        return FacilityChatLayout.suggestionTop(this.input,
                this.screen.height, this.suggestionLineLimit);
    }

    @ModifyConstant(method = "renderUsage",
            constant = @Constant(intValue = 72))
    private int scpAdditions$moveUsageHints(int original) {
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return original;
        return FacilityChatLayout.suggestionTop(this.input,
                this.screen.height, this.suggestionLineLimit);
    }
}
