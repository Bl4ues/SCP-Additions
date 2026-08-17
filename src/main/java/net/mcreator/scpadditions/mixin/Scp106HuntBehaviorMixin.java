package net.mcreator.scpadditions.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.event.Scp106TargetingEvents;
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
    @Unique private static final double scpAdditions$verticalPhaseThreshold = 1.35D;
    @Unique private static final double scpAdditions$verticalPhaseSpeed = 0.10D;
    @Unique private static final int scpAdditions$huntingState = 0;
    @Unique private static final int scpAdditions$phaseTravelState = 3;
    @Unique private static final int scpAdditions$vanishingState = 4;
    @Unique private static final int scpAdditions$ambushRetryTicks = 8;
    @Unique private static final Method scpAdditions$beginPhaseTravel =
            scpAdditions$method("beginPhaseTravel");

    @Unique private int scpAdditions$vanishTicksAtHead;

    @Inject(method = "resolveHuntedPlayer", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$chooseReachablePlayer(
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
                || vanishForDespawn || scpAdditions$beginPhaseTravel == null) {
            return;
        }

        boolean retryScheduled = stateTicks == scpAdditions$ambushRetryTicks
                && scpAdditions$vanishTicksAtHead <= 1;
        if (!retryScheduled) return;

        // Both emergence searches already failed for this target position. A
        // canopy, cliff, or other unsuitable surface should not freeze the hunt
        // for repeated retries; phase travel is the terrain-independent fallback.
        self.setInvisible(false);
        scpAdditions$beginPhaseTravel(self);
    }

    @Inject(method = "tickPhaseTravel", at = @At("TAIL"))
    private void scpAdditions$followVerticalTargetDuringPhase(CallbackInfo callback) {
        Scp106Entity self = (Scp106Entity) (Object) this;
        LivingEntity rawTarget = self.getTarget();
        if (!(rawTarget instanceof Player player)
                || !scpAdditions$isValid(self, player)) {
            return;
        }

        double deltaY = player.getY() - self.getY();
        if (Math.abs(deltaY) <= scpAdditions$verticalPhaseThreshold) return;

        // Vanilla phase travel can decide it has safely exited merely because it
        // is on solid ground with line of sight. If the target is still on a
        // different vertical level, re-enter phase travel so the next tick keeps
        // traversing the structure instead of returning to ordinary pathfinding.
        if (self.getEncounterState() == scpAdditions$huntingState) {
            scpAdditions$beginPhaseTravel(self);
            return;
        }
        if (self.getEncounterState() != scpAdditions$phaseTravelState) return;

        Vec3 movement = self.getDeltaMovement();
        self.setDeltaMovement(movement.x,
                Mth.clamp(deltaY, -scpAdditions$verticalPhaseSpeed,
                        scpAdditions$verticalPhaseSpeed),
                movement.z);
    }

    @Unique
    private static void scpAdditions$beginPhaseTravel(Scp106Entity self) {
        if (scpAdditions$beginPhaseTravel == null) return;
        try {
            scpAdditions$beginPhaseTravel.invoke(self);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not recover SCP-106 through phase travel", exception);
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
