package net.mcreator.scpadditions.mixin.client;

import net.minecraft.world.level.GameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the legacy SCP-079 control gamerule available to the internal facility
 * logic while hiding it from the player-facing Edit Game Rules screen.
 *
 * The modern SCP-079 system derives control from auxiliary power, protocol
 * discovery and facility access. Exposing the mirrored compatibility gamerule
 * as a manual toggle would therefore be misleading and immediately overridden.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen$RuleList$1")
public abstract class EditGameRulesScreenMixin {
    @Inject(method = "visitBoolean", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$hideLegacyScp079ControlRule(
            GameRules.Key<GameRules.BooleanValue> key,
            GameRules.Type<GameRules.BooleanValue> type,
            CallbackInfo callback) {
        if (key == ScpAdditionsModGameRules.SCP079CONTROLON) {
            callback.cancel();
        }
    }
}
