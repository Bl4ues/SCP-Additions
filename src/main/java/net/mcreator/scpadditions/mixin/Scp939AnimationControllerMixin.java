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
 * Keeps the authored SCP-939 gait synchronized with the entity's real motion.
 *
 * <p>The important distinction here is that locomotion selection and playback
 * speed are driven by a filtered world-space displacement sampled once per game
 * tick. Feeding raw per-frame/network deltas into GeckoLib made the gait visibly
 * stutter, while a fixed animation speed made planted paws skate whenever the
 * navigator changed speed.</p>
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
    @Unique private static final int SCPADDITIONS_MOVE_HOLD_TICKS = 3;
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
                new AnimationController<>(entity, "locomotion", 8, state -> {
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
        // Entity teleports/pounce corrections must not spike the gait clock.
        if (!Double.isFinite(distance) || distance > 0.65D) distance = 0.0D;

        double response = distance > scpadditions$smoothedDistance ? 0.28D : 0.18D;
        scpadditions$smoothedDistance +=
                (distance - scpadditions$smoothedDistance) * response;
        if (distance < SCPADDITIONS_MOVE_DELTA * 0.5D) {
            scpadditions$smoothedDistance *= 0.88D;
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
                        running ? 0.45D : 0.38D,
                        running ? 1.60D : 1.70D);
            }

            // Playback follows the body over several ticks rather than snapping
            // to every tiny pathfinding/network speed correction.
            scpadditions$smoothedPlayback +=
                    (target - scpadditions$smoothedPlayback) * 0.22D;
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
