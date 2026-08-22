package com.bl4ues.scpclassifieddirective.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.event.Scp106TargetingEvents;
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

    @Unique private static final double scpClassifiedDirective$targetRange = 128.0D;
    @Unique private static final double scpClassifiedDirective$switchMargin = 1.5D;
    @Unique private static final double scpClassifiedDirective$verticalPhaseThreshold = 1.35D;
    @Unique private static final double scpClassifiedDirective$verticalPhaseSpeed = 0.10D;
    @Unique private static final int scpClassifiedDirective$huntingState = 0;
    @Unique private static final int scpClassifiedDirective$phaseTravelState = 3;
    @Unique private static final int scpClassifiedDirective$vanishingState = 4;
    @Unique private static final int scpClassifiedDirective$ambushRetryTicks = 8;
    @Unique private static final Method scpClassifiedDirective$beginPhaseTravel =
            scpClassifiedDirective$method("beginPhaseTravel");

    @Unique private int scpClassifiedDirective$vanishTicksAtHead;

    @Inject(method = "resolveHuntedPlayer", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$chooseReachablePlayer(
            CallbackInfoReturnable<Player> callback) {
        Scp106Entity self = (Scp106Entity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;

        Player retaliation = Scp106TargetingEvents.preferredTarget(self);
        if (retaliation != null) {
            huntedPlayerId = retaliation.getUUID();
            if (self.getTarget() != retaliation) self.setTarget(retaliation);
            callback.setReturnValue(retaliation);
            return;
        }

        Player locked = huntedPlayerId == null ? null
                : level.getPlayerByUUID(huntedPlayerId);
        if (!scpClassifiedDirective$isValid(self, locked)) locked = null;

        LivingEntity rawTarget = self.getTarget();
        Player current = rawTarget instanceof Player player
                && scpClassifiedDirective$isValid(self, player) ? player : locked;

        Player nearest = null;
        Player nearestVisible = null;
        double nearestDistance = Double.MAX_VALUE;
        double nearestVisibleDistance = Double.MAX_VALUE;
        for (Player candidate : level.players()) {
            if (!scpClassifiedDirective$isValid(self, candidate)) continue;
            double distance = self.distanceTo(candidate);
            if (distance > scpClassifiedDirective$targetRange) continue;
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
                    + scpClassifiedDirective$switchMargin < currentDistance) {
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
    private void scpClassifiedDirective$captureVanishCountdown(CallbackInfo callback) {
        scpClassifiedDirective$vanishTicksAtHead = stateTicks;
    }

    @Inject(method = "tickVanish", at = @At("TAIL"))
    private void scpClassifiedDirective$recoverFailedRelocation(CallbackInfo callback) {
        Scp106Entity self = (Scp106Entity) (Object) this;
        if (self.getEncounterState() != scpClassifiedDirective$vanishingState
                || vanishForDespawn || scpClassifiedDirective$beginPhaseTravel == null) {
            return;
        }

        boolean retryScheduled = stateTicks == scpClassifiedDirective$ambushRetryTicks
                && scpClassifiedDirective$vanishTicksAtHead <= 1;
        if (!retryScheduled) return;

        // Both emergence searches already failed for this target position. A
        // canopy, cliff, or other unsuitable surface should not freeze the hunt
        // for repeated retries; phase travel is the terrain-independent fallback.
        self.setInvisible(false);
        scpClassifiedDirective$beginPhaseTravel(self);
    }

    @Inject(method = "tickPhaseTravel", at = @At("TAIL"))
    private void scpClassifiedDirective$followVerticalTargetDuringPhase(CallbackInfo callback) {
        Scp106Entity self = (Scp106Entity) (Object) this;
        LivingEntity rawTarget = self.getTarget();
        if (!(rawTarget instanceof Player player)
                || !scpClassifiedDirective$isValid(self, player)) {
            return;
        }

        double deltaY = player.getY() - self.getY();
        if (Math.abs(deltaY) <= scpClassifiedDirective$verticalPhaseThreshold) return;

        // Vanilla phase travel can decide it has safely exited merely because it
        // is on solid ground with line of sight. If the target is still on a
        // different vertical level, re-enter phase travel so the next tick keeps
        // traversing the structure instead of returning to ordinary pathfinding.
        if (self.getEncounterState() == scpClassifiedDirective$huntingState) {
            scpClassifiedDirective$beginPhaseTravel(self);
            return;
        }
        if (self.getEncounterState() != scpClassifiedDirective$phaseTravelState) return;

        Vec3 movement = self.getDeltaMovement();
        self.setDeltaMovement(movement.x,
                Mth.clamp(deltaY, -scpClassifiedDirective$verticalPhaseSpeed,
                        scpClassifiedDirective$verticalPhaseSpeed),
                movement.z);
    }

    @Unique
    private static void scpClassifiedDirective$beginPhaseTravel(Scp106Entity self) {
        if (scpClassifiedDirective$beginPhaseTravel == null) return;
        try {
            scpClassifiedDirective$beginPhaseTravel.invoke(self);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not recover SCP-106 through phase travel", exception);
        }
    }

    @Unique
    private static boolean scpClassifiedDirective$isValid(Scp106Entity self,
            Player player) {
        return player != null && player.isAlive() && !player.isRemoved()
                && !player.isCreative() && !player.isSpectator()
                && player.level() == self.level();
    }

    @Unique
    private static Method scpClassifiedDirective$method(String name) {
        try {
            Method method = Scp106Entity.class.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
