package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import com.bl4ues.scpclassifieddirective.client.FacilityChatLayout;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079ChatLayout;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps command completion and usage hints attached to relocated chat inputs. */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final private Screen screen;
    @Shadow @Final private EditBox input;
    @Shadow @Final private int suggestionLineLimit;
    @Shadow @Final @Mutable private boolean anchorToBottom;
    @Shadow @Final @Mutable private int fillColor;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void scpClassifiedDirective$useFacilitySuggestionStyle(CallbackInfo ci) {
        if (Scp079PlayableClient.active()) {
            // ChatScreen's suggestion list is designed to grow upward when this
            // flag is true. The previous false value treated our custom y as a
            // top anchor and produced the upside-down detached list seen in-game.
            this.anchorToBottom = true;
            this.fillColor = 0xED061018;
            return;
        }
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return;
        this.anchorToBottom = false;
        this.fillColor = 0xE6081022;
    }

    /** Replaces vanilla's bottom margin so the upward list ends at our input. */
    @ModifyConstant(method = "showSuggestions",
            constant = @Constant(intValue = 12), require = 0)
    private int scpClassifiedDirective$moveBottomAnchoredSuggestions(
            int original) {
        if (!Scp079PlayableClient.active()) return original;
        return Math.max(0,
                this.screen.height - Scp079ChatLayout.suggestionAnchor(this.input));
    }

    @ModifyConstant(method = "showSuggestions",
            constant = @Constant(intValue = 72))
    private int scpClassifiedDirective$moveSuggestionList(int original) {
        if (Scp079PlayableClient.active()) {
            return Scp079ChatLayout.suggestionTop(this.input,
                    this.screen.height, this.suggestionLineLimit);
        }
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return original;
        return FacilityChatLayout.suggestionTop(this.input,
                this.screen.height, this.suggestionLineLimit);
    }

    @ModifyConstant(method = "renderUsage",
            constant = @Constant(intValue = 72))
    private int scpClassifiedDirective$moveUsageHints(int original) {
        if (Scp079PlayableClient.active()) {
            return Scp079ChatLayout.suggestionTop(this.input,
                    this.screen.height, this.suggestionLineLimit);
        }
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return original;
        return FacilityChatLayout.suggestionTop(this.input,
                this.screen.height, this.suggestionLineLimit);
    }
}
