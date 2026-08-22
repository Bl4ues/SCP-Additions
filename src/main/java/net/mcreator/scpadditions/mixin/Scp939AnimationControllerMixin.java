package net.mcreator.scpadditions.mixin;

import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * Keeps SCP-939 locomotion tied to actual distance travelled instead of a
 * second, independent animation clock.
 *
 * <p>For a quadruped this matters more than it does for a biped: when playback
 * is slower than translation every stance paw visibly skates backwards, and
 * when a heavily filtered playback speed catches up late the whole animal looks
 * as if it is trembling through gear changes. The authored walk/run cycles are
 * therefore scaled from world-space displacement with only a small amount of
 * filtering for pathfinding/network noise.</p>
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

    @Unique private int scpadditions$movementHoldTicks;
    @Unique private int scpadditions$lastMovementSampleTick = Integer.MIN_VALUE;
    @Unique private int scpadditions$lastPlaybackSampleTick = Integer.MIN_VALUE;
    @Unique private double scpadditions$smoothedDistance;
    @Unique private double scpadditions$smoothedPlayback = 1.0D;

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

            // A small blend prevents audible/visible single-tick spikes, but it
            // intentionally catches up quickly so feet do not skate while the
            // body has already accelerated.
            scpadditions$smoothedPlayback +=
                    (target - scpadditions$smoothedPlayback) * 0.58D;
        }
        return scpadditions$smoothedPlayback;
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
