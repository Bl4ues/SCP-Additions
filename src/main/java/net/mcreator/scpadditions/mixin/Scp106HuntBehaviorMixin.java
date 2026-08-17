package net.mcreator.scpadditions.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.UUID;

/** Multiplayer target arbitration and relocation recovery for SCP-106. */
@Mixin(value = Scp106Entity.class, remap = false)
public abstract class Scp106HuntBehaviorMixin {
    @Shadow private UUID huntedPlayerId;
    @Shadow private int stateTicks;
    @Shadow private boolean vanishForDespawn;

    @Unique private static final double scpAdditions$targetRange = 128.0D;
    @Unique private static final double scpAdditions$switchMargin = 1.5D;
    @Unique private static final int scpAdditions$vanishingState = 4;
    @Unique private static final int scpAdditions$ambushRetryTicks = 8;
    @Unique private static final Method scpAdditions$beginPhaseTravel =
            scpAdditions$method("beginPhaseTravel");

    @Unique private int scpAdditions$vanishTicksAtHead;
    @Unique private int scpAdditions$failedRelocations;

    @Inject(method = "resolveHuntedPlayer", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$chooseReachablePlayer(
            CallbackInfoReturnable<Player> callback) {
        Scp106Entity self = (Scp106Entity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;

        Player locked = huntedPlayerId == null ? null
                : level.getPlayerByUUID(huntedPlayerId);
        if (!scpAdditions$isValid(self, locked)) locked = null;

        LivingEntity rawTarget = self.getTarget();
        Player current = rawTarget instanceof Player player
                && scpAdditions$isValid(self, player) ? player : locked;

        Player nearest = null;
        Player nearestVisible = null;
        double nearestDistance = Double.MAX_VALUE;
        double nearestVisibleDistance = Double.MAX_VALUE;
        for (Player candidate : level.players()) {
            if (!scpAdditions$isValid(self, candidate)) continue;
            double distance = self.distanceTo(candidate);
            if (distance > scpAdditions$targetRange) continue;
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
            if (self.hasLineOfSight(candidate)
                    && distance < nearestVisibleDistance) {
                nearestVisible = candidate;
                nearestVisibleDistance = distance;
            }
        }

        Player chosen = current != null ? current : nearest;
        if (nearestVisible != null && chosen != nearestVisible) {
            boolean currentVisible = chosen != null && self.hasLineOfSight(chosen);
            double currentDistance = chosen == null
                    ? Double.MAX_VALUE : self.distanceTo(chosen);
            if (!currentVisible || nearestVisibleDistance
                    + scpAdditions$switchMargin < currentDistance) {
                chosen = nearestVisible;
            }
        }

        if (chosen == null) {
            huntedPlayerId = null;
            callback.setReturnValue(null);
            return;
        }

        huntedPlayerId = chosen.getUUID();
        if (self.getTarget() != chosen) self.setTarget(chosen);
        callback.setReturnValue(chosen);
    }

    @Inject(method = "tickVanish", at = @At("HEAD"))
    private void scpAdditions$captureVanishCountdown(CallbackInfo callback) {
        scpAdditions$vanishTicksAtHead = stateTicks;
    }

    @Inject(method = "tickVanish", at = @At("TAIL"))
    private void scpAdditions$recoverFailedRelocation(CallbackInfo callback) {
        Scp106Entity self = (Scp106Entity) (Object) this;
        if (self.getEncounterState() != scpAdditions$vanishingState
                || vanishForDespawn) {
            scpAdditions$failedRelocations = 0;
            return;
        }

        boolean retryScheduled = stateTicks == scpAdditions$ambushRetryTicks
                && scpAdditions$vanishTicksAtHead <= 1;
        if (!retryScheduled) return;

        scpAdditions$failedRelocations++;
        if (scpAdditions$failedRelocations < 3
                || scpAdditions$beginPhaseTravel == null) {
            return;
        }

        scpAdditions$failedRelocations = 0;
        self.setInvisible(false);
        try {
            scpAdditions$beginPhaseTravel.invoke(self);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not recover SCP-106 from a failed relocation",
                    exception);
        }
    }

    @Unique
    private static boolean scpAdditions$isValid(Scp106Entity self,
            Player player) {
        return player != null && player.isAlive() && !player.isRemoved()
                && !player.isCreative() && !player.isSpectator()
                && player.level() == self.level();
    }

    @Unique
    private static Method scpAdditions$method(String name) {
        try {
            Method method = Scp106Entity.class.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
