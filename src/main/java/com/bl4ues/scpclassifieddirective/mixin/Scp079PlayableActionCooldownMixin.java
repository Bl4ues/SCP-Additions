package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative debounce for player-triggered SCP-079 device actions. */
@Mixin(Scp079PlayableManager.class)
public abstract class Scp079PlayableActionCooldownMixin {
    private static final int ACTION_COOLDOWN_TICKS = 16;
    private static final Map<ActionKey, Long> SCPCLASSIFIEDDIRECTIVE$COOLDOWNS =
            new ConcurrentHashMap<>();

    @Inject(method = "performAction", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpclassifieddirective$rejectRepeatedAction(
            ServerPlayer player, Scp079PlayableManager.ManualAction action,
            BlockPos aimedPos, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || action == null || aimedPos == null) return;
        long now = player.serverLevel().getGameTime();
        ActionKey key = new ActionKey(player.getUUID(), action, aimedPos.asLong());
        Long until = SCPCLASSIFIEDDIRECTIVE$COOLDOWNS.get(key);
        if (until == null) return;
        if (now >= until) {
            SCPCLASSIFIEDDIRECTIVE$COOLDOWNS.remove(key, until);
            return;
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "performAction", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$rememberSuccessfulAction(
            ServerPlayer player, Scp079PlayableManager.ManualAction action,
            BlockPos aimedPos, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || action == null || aimedPos == null
                || !cir.getReturnValueZ()) return;
        long now = player.serverLevel().getGameTime();
        SCPCLASSIFIEDDIRECTIVE$COOLDOWNS.put(
                new ActionKey(player.getUUID(), action, aimedPos.asLong()),
                now + ACTION_COOLDOWN_TICKS);
    }

    private record ActionKey(UUID playerId,
            Scp079PlayableManager.ManualAction action, long target) {
    }
}
