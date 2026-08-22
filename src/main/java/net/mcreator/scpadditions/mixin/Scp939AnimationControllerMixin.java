package net.mcreator.scpadditions.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.UUID;

/**
 * Keeps SCP-939 locomotion tied to actual distance travelled and gives a
 * confirmed acoustic pursuit the responsiveness expected from a large predator.
 *
 * <p>The chase helpers deliberately do not turn SCP-939 into a sighted mob. Its
 * long-range destination still comes from the acoustic brain. The only live
 * player sampling below happens after the prey has already entered the short
 * physical-detection range and triggered a pounce, where it is used to stop the
 * seven-tick wind-up from aiming at an obsolete position.</p>
 */
@Mixin(value = Scp939Entity.class, remap = false)
public abstract class Scp939AnimationControllerMixin {
    @Unique private static final RawAnimation SCPADDITIONS_939_IDLE = RawAnimation.begin().thenLoop("idle");
    @Unique private static final RawAnimation SCPADDITIONS_939_LISTEN = RawAnimation.begin().thenLoop("idle_listen");
    @Unique private static final RawAnimation SCPADDITIONS_939_WALK = RawAnimation.begin().thenLoop("walk");
    @Unique private static final RawAnimation SCPADDITIONS_939_RUN = RawAnimation.begin().thenLoop("run");
    @Unique private static final RawAnimation SCPADDITIONS_939_SEARCH = RawAnimation.begin().thenLoop("search");
    @Unique private static final RawAnimation SCPADDITIONS_939_MIMIC = RawAnimation.begin().thenPlay("mimic_call");
    @Unique private static final RawAnimation SCPADDITIONS_939_BITE = RawAnimation.begin().thenPlay("attack_bite");
    @Unique private static final RawAnimation SCPADDITIONS_939_POUNCE = RawAnimation.begin().thenPlay("pounce_start");
    @Unique private static final RawAnimation SCPADDITIONS_939_PIN = RawAnimation.begin().thenPlay("pounce_land_pin");
    @Unique private static final RawAnimation SCPADDITIONS_939_MAUL = RawAnimation.begin().thenLoop("pounce_maul_loop");
    @Unique private static final RawAnimation SCPADDITIONS_939_KICKED = RawAnimation.begin().thenPlay("pounce_kicked_off");
    @Unique private static final RawAnimation SCPADDITIONS_939_HURT = RawAnimation.begin().thenPlay("hurt_stagger");
    @Unique private static final RawAnimation SCPADDITIONS_939_DEATH = RawAnimation.begin().thenPlay("death");

    @Unique private static final double SCPADDITIONS_MOVE_DELTA = 0.0015D;
    @Unique private static final int SCPADDITIONS_MOVE_HOLD_TICKS = 2;
    // At 1x playback the 1.8 s walk covers roughly 3.24 blocks and the
    // 0.82 s run roughly 3.44 blocks. These are stride calibration values,
    // not movement-speed limits.
    @Unique private static final double SCPADDITIONS_WALK_REFERENCE = 0.090D;
    @Unique private static final double SCPADDITIONS_RUN_REFERENCE = 0.210D;
    @Unique private static final double SCPADDITIONS_CONFIRMED_HUNT_SPEED = 1.62D;
    @Unique private static final double SCPADDITIONS_LOST_HUNT_SPEED = 1.40D;
    @Unique private static final double SCPADDITIONS_MAX_POUNCE_LEAD = 2.40D;

    @Shadow private boolean pounceLaunched;
    @Shadow private Vec3 pounceTargetPosition;

    @Unique private int scpadditions$movementHoldTicks;
    @Unique private int scpadditions$lastMovementSampleTick = Integer.MIN_VALUE;
    @Unique private int scpadditions$lastPlaybackSampleTick = Integer.MIN_VALUE;
    @Unique private double scpadditions$smoothedDistance;
    @Unique private double scpadditions$smoothedPlayback = 1.0D;
    @Unique private UUID scpadditions$pounceTargetId;

    @Inject(method = "registerControllers", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replace939AnimationControllers(
            AnimatableManager.ControllerRegistrar controllers, CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;

        AnimationController<Scp939Entity> locomotion =
                new AnimationController<>(entity, "locomotion", 5, state -> {
                    byte action = entity.getAction();
                    if (scpadditions$isCombatFullBodyAction(action)) return PlayState.STOP;
                    if (action == Scp939Entity.ACTION_LISTEN) {
                        return state.setAndContinue(SCPADDITIONS_939_LISTEN);
                    }

                    Scp939AwarenessState awareness = entity.getAwarenessState();
                    boolean moving = scpadditions$isVisuallyMoving(entity,
                            state.isMoving());
                    if (moving) {
                        if (awareness == Scp939AwarenessState.CONFIRMED_HUNT
                                || awareness == Scp939AwarenessState.LOST_SEARCH) {
                            return state.setAndContinue(SCPADDITIONS_939_RUN);
                        }
                        return state.setAndContinue(SCPADDITIONS_939_WALK);
                    }
                    if (awareness == Scp939AwarenessState.SEARCH) {
                        return state.setAndContinue(SCPADDITIONS_939_SEARCH);
                    }
                    if (awareness == Scp939AwarenessState.HEARD_SOUND) {
                        return state.setAndContinue(SCPADDITIONS_939_LISTEN);
                    }
                    return state.setAndContinue(SCPADDITIONS_939_IDLE);
                });

        locomotion.setAnimationSpeedHandler(animatable ->
                scpadditions$locomotionPlayback(animatable));
        controllers.add(locomotion);

        AnimationController<Scp939Entity> actionController =
                new AnimationController<>(entity, "action", 2, state -> switch (entity.getAction()) {
                    case Scp939Entity.ACTION_BITE -> state.setAndContinue(SCPADDITIONS_939_BITE);
                    case Scp939Entity.ACTION_POUNCE -> state.setAndContinue(SCPADDITIONS_939_POUNCE);
                    case Scp939Entity.ACTION_PIN_LAND -> state.setAndContinue(SCPADDITIONS_939_PIN);
                    case Scp939Entity.ACTION_MAUL -> state.setAndContinue(SCPADDITIONS_939_MAUL);
                    case Scp939Entity.ACTION_KICKED -> state.setAndContinue(SCPADDITIONS_939_KICKED);
                    case Scp939Entity.ACTION_HURT -> state.setAndContinue(SCPADDITIONS_939_HURT);
                    case Scp939Entity.ACTION_DEATH -> state.setAndContinue(SCPADDITIONS_939_DEATH);
                    case Scp939Entity.ACTION_MIMIC -> state.setAndContinue(SCPADDITIONS_939_MIMIC);
                    default -> PlayState.STOP;
                });
        controllers.add(actionController);
        ci.cancel();
    }

    @Unique
    private boolean scpadditions$isVisuallyMoving(Scp939Entity entity,
            boolean geckoMoving) {
        scpadditions$sampleMovement(entity);
        if (geckoMoving || scpadditions$smoothedDistance > SCPADDITIONS_MOVE_DELTA) {
            scpadditions$movementHoldTicks = SCPADDITIONS_MOVE_HOLD_TICKS;
        }
        return scpadditions$movementHoldTicks > 0;
    }

    @Unique
    private void scpadditions$sampleMovement(Scp939Entity entity) {
        if (scpadditions$lastMovementSampleTick == entity.tickCount) return;
        scpadditions$lastMovementSampleTick = entity.tickCount;

        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        double distance = Math.sqrt(dx * dx + dz * dz);
        // Teleports, pin placement and the actual pounce impulse do not belong to
        // the ordinary locomotion gait clock.
        if (!Double.isFinite(distance) || distance > 0.75D) distance = 0.0D;

        // Fast enough to preserve stance timing, slow enough to discard the
        // one-tick velocity chatter produced by path node changes.
        double response = distance > scpadditions$smoothedDistance ? 0.68D : 0.54D;
        scpadditions$smoothedDistance +=
                (distance - scpadditions$smoothedDistance) * response;
        if (distance < SCPADDITIONS_MOVE_DELTA * 0.5D) {
            scpadditions$smoothedDistance *= 0.62D;
        }

        if (distance > SCPADDITIONS_MOVE_DELTA) {
            scpadditions$movementHoldTicks = SCPADDITIONS_MOVE_HOLD_TICKS;
        } else if (scpadditions$movementHoldTicks > 0) {
            scpadditions$movementHoldTicks--;
        }
    }

    @Unique
    private double scpadditions$locomotionPlayback(Scp939Entity entity) {
        if (entity.getAction() == Scp939Entity.ACTION_LISTEN) return 1.0D;
        scpadditions$sampleMovement(entity);

        if (scpadditions$lastPlaybackSampleTick != entity.tickCount) {
            scpadditions$lastPlaybackSampleTick = entity.tickCount;
            Scp939AwarenessState awareness = entity.getAwarenessState();
            boolean running = awareness == Scp939AwarenessState.CONFIRMED_HUNT
                    || awareness == Scp939AwarenessState.LOST_SEARCH;

            double reference = running
                    ? SCPADDITIONS_RUN_REFERENCE : SCPADDITIONS_WALK_REFERENCE;
            double target;
            if (scpadditions$movementHoldTicks <= 0
                    && scpadditions$smoothedDistance < SCPADDITIONS_MOVE_DELTA) {
                target = 1.0D;
            } else {
                target = Mth.clamp(scpadditions$smoothedDistance / reference,
                        running ? 0.55D : 0.42D,
                        running ? 2.60D : 2.45D);
            }

            // A small blend prevents visible single-tick spikes, but intentionally
            // catches up quickly so stance paws do not skate while the body has
            // already accelerated.
            scpadditions$smoothedPlayback +=
                    (target - scpadditions$smoothedPlayback) * 0.58D;
        }
        return scpadditions$smoothedPlayback;
    }

    /**
     * A confirmed hunt receives much denser replanning than investigate/search.
     * The destination is still only the acoustic brain's evidenced/predicted
     * position; this simply stops the navigator from chasing an eight-tick-old
     * node while a loud player is already somewhere else.
     */
    @Inject(method = "moveToKnown", at = @At("HEAD"), cancellable = true,
            remap = false)
    private void scpadditions$drivePredatoryChase(Vec3 known, double ignoredSpeed,
            CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;
        Scp939AwarenessState awareness = entity.getAwarenessState();
        boolean confirmed = awareness == Scp939AwarenessState.CONFIRMED_HUNT;
        boolean lost = awareness == Scp939AwarenessState.LOST_SEARCH;
        if (!confirmed && !lost) return;

        ci.cancel();
        if (known == null) return;
        int interval = confirmed ? 2 : 3;
        double speed = confirmed ? SCPADDITIONS_CONFIRMED_HUNT_SPEED
                : SCPADDITIONS_LOST_HUNT_SPEED;
        if (entity.getNavigation().isDone() || entity.tickCount % interval == 0) {
            entity.getNavigation().moveTo(known.x, known.y, known.z, speed);
        }
    }

    /** Remember the physically detected prey only for the pounce wind-up. */
    @Inject(method = "startPounce", at = @At("TAIL"), remap = false)
    private void scpadditions$rememberPouncePrey(ServerPlayer target,
            CallbackInfo ci) {
        scpadditions$pounceTargetId = target == null ? null : target.getUUID();
    }

    /**
     * Refresh the launch solution while SCP-939 coils to jump. Without this the
     * seven-tick wind-up aims at the place where a running target used to be,
     * which is why the predator could repeatedly leap beside its prey.
     */
    @Inject(method = "tickActionTimer", at = @At("HEAD"), remap = false)
    private void scpadditions$leadMovingPounceTarget(CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;
        if (entity.getAction() != Scp939Entity.ACTION_POUNCE
                || pounceLaunched || scpadditions$pounceTargetId == null
                || !(entity.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer prey = level.getServer().getPlayerList()
                .getPlayer(scpadditions$pounceTargetId);
        if (prey == null || !prey.isAlive() || prey.isCreative()
                || prey.isSpectator() || prey.level() != level) {
            return;
        }

        Vec3 base = prey.position().add(0.0D,
                prey.getBbHeight() * 0.30D, 0.0D);
        Vec3 delta = base.subtract(entity.position());
        double distance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double launchSpeed = Mth.clamp(distance * 0.22D, 0.90D, 1.42D);
        double leadTicks = Mth.clamp(distance / Math.max(0.01D, launchSpeed)
                + 0.75D, 1.5D, 6.0D);

        Vec3 velocity = prey.getDeltaMovement();
        Vec3 lead = new Vec3(velocity.x * leadTicks, 0.0D,
                velocity.z * leadTicks);
        double leadDistance = lead.horizontalDistance();
        if (leadDistance > SCPADDITIONS_MAX_POUNCE_LEAD) {
            lead = lead.scale(SCPADDITIONS_MAX_POUNCE_LEAD / leadDistance);
        }
        pounceTargetPosition = base.add(lead);
    }

    /**
     * Let the body turn decisively during a hunt without reintroducing the old
     * angular-velocity accumulator that caused visible oscillation. The turn is
     * a pure bounded interpolation each tick, so a path correction cannot leave
     * stored rotational momentum behind.
     */
    @Inject(method = "smoothLocomotionRotation", at = @At("HEAD"),
            cancellable = true, remap = false)
    private void scpadditions$predatoryTurnRate(CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;
        float desired = entity.getYRot();
        float previous = entity.yRotO;
        float error = Mth.wrapDegrees(desired - previous);
        byte action = entity.getAction();
        Scp939AwarenessState awareness = entity.getAwarenessState();

        float maximumTurn = action == Scp939Entity.ACTION_POUNCE ? 24.0F
                : action == Scp939Entity.ACTION_BITE ? 18.0F
                : awareness == Scp939AwarenessState.CONFIRMED_HUNT ? 20.0F
                : awareness == Scp939AwarenessState.LOST_SEARCH ? 15.0F
                : 9.0F;
        float yaw = previous + Mth.clamp(error, -maximumTurn, maximumTurn);
        entity.setYRot(yaw);
        entity.yBodyRot = Mth.rotLerp(0.58F, entity.yBodyRot, yaw);
        entity.yHeadRot = Mth.rotLerp(0.70F, entity.yHeadRot, yaw);
        ci.cancel();
    }

    @Unique
    private static boolean scpadditions$isCombatFullBodyAction(byte action) {
        return action == Scp939Entity.ACTION_POUNCE
                || action == Scp939Entity.ACTION_PIN_LAND
                || action == Scp939Entity.ACTION_MAUL
                || action == Scp939Entity.ACTION_KICKED
                || action == Scp939Entity.ACTION_HURT
                || action == Scp939Entity.ACTION_DEATH;
    }
}
