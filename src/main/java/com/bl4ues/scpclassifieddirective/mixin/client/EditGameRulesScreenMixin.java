package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.world.level.GameRules;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides legacy compatibility gamerules that are now derived internally. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen$RuleList$1")
public abstract class EditGameRulesScreenMixin {
    @Inject(method = "visitBoolean", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$hideLegacyRules(
            GameRules.Key<GameRules.BooleanValue> key,
            GameRules.Type<GameRules.BooleanValue> type,
            CallbackInfo callback) {
        if (key == ScpClassifiedDirectiveModGameRules.SCP079CONTROLON
                || key == ScpClassifiedDirectiveModGameRules.DECONCHECKPOINT) {
            callback.cancel();
        }
    }
}
