package net.mcreator.scpadditions.mixin.client;

import net.minecraft.world.level.GameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides legacy compatibility gamerules that are now derived internally. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen$RuleList$1")
public abstract class EditGameRulesScreenMixin {
    @Inject(method = "visitBoolean", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$hideLegacyRules(
            GameRules.Key<GameRules.BooleanValue> key,
            GameRules.Type<GameRules.BooleanValue> type,
            CallbackInfo callback) {
        if (key == ScpAdditionsModGameRules.SCP079CONTROLON
                || key == ScpAdditionsModGameRules.DECONCHECKPOINT) {
            callback.cancel();
        }
    }
}
