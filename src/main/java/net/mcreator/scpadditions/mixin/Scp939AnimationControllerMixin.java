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
 * Keeps SCP-939 locomotion visually tied to real displacement.
 *
 * Client-side deltaMovement is not a reliable measure of how far a remote mob
 * was actually interpolated this tick. Using it made the model enter walk while
 * its playback rate was calculated from a much smaller stale velocity, producing
 * the exact "frozen paws sliding over the floor" look. The controller now uses
 * measured X/Z displacement between entity ticks for both state selection and
 * playback speed.
 */
@Mixin(value = Scp939Entity.class, remap = false)
public abstract class Scp939AnimationControllerMixin {
    @Unique
    private static final RawAnimation SCPADDITIONS_939_IDLE =
            RawAnimation.begin().thenLoop("idle");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_LISTEN =
            RawAnimation.begin().thenLoop("idle_listen");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_WALK =
            RawAnimation.begin().thenLoop("walk");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_RUN =
            RawAnimation.begin().thenLoop("run");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_SEARCH =
            RawAnimation.begin().thenLoop("search");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_MIMIC =
            RawAnimation.begin().thenPlay("mimic_call");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_BITE =
            RawAnimation.begin().thenPlay("attack_bite");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_POUNCE =
            RawAnimation.begin().thenPlay("pounce_start");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_PIN =
            RawAnimation.begin().thenPlay("pounce_land_pin");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_MAUL =
            RawAnimation.begin().thenLoop("pounce_maul_loop");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_KICKED =
            RawAnimation.begin().thenPlay("pounce_kicked_off");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_HURT =
            RawAnimation.begin().thenPlay("hurt_stagger");
    @Unique
    private static final RawAnimation SCPADDITIONS_939_DEATH =
            RawAnimation.begin().thenPlay("death");

    /*
     * Authored stride calibration. The walk is 1.8 s and the run 0.82 s. These
     * references are physical displacement per tick, not MoveControl targets.
     * A modest floor keeps very slow path corrections visibly alive; below the
     * movement threshold we use idle/turn presentation instead of pretending a
     * nearly stationary body is walking.
     */
    @Unique
    private static final double SCPADDITIONS_WALK_REFERENCE_SPEED = 0.030D;
    @Unique
    private static final double SCPADDITIONS_RUN_REFERENCE_SPEED = 0.082D;
    @Unique
    private static final double SCPADDITIONS_MOVING_EPSILON_SQR = 0.0035D * 0.0035D;

    @Inject(method = "registerControllers", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replace939AnimationControllers(
            AnimatableManager.ControllerRegistrar controllers,
            CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;

        AnimationController<Scp939Entity> locomotion =
                new AnimationController<>(entity, "locomotion", 6, state -> {
                    byte action = entity.getAction();
                    if (scpadditions$isCombatFullBodyAction(action)) {
                        return PlayState.STOP;
                    }

                    if (action == Scp939Entity.ACTION_LISTEN) {
                        return state.setAndContinue(SCPADDITIONS_939_LISTEN);
                    }

                    Scp939AwarenessState awareness = entity.getAwarenessState();
                    boolean moving = scpadditions$horizontalDisplacementSqr(entity)
                            > SCPADDITIONS_MOVING_EPSILON_SQR;
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

        locomotion.setAnimationSpeedHandler(animatable -> {
            byte action = animatable.getAction();
            if (action == Scp939Entity.ACTION_LISTEN) return 1.0D;

            double movement = Math.sqrt(
                    scpadditions$horizontalDisplacementSqr(animatable));
            if (movement * movement <= SCPADDITIONS_MOVING_EPSILON_SQR) {
                return 1.0D;
            }

            Scp939AwarenessState awareness = animatable.getAwarenessState();
            boolean running = awareness == Scp939AwarenessState.CONFIRMED_HUNT
                    || awareness == Scp939AwarenessState.LOST_SEARCH;
            double reference = running
                    ? SCPADDITIONS_RUN_REFERENCE_SPEED
                    : SCPADDITIONS_WALK_REFERENCE_SPEED;

            return Mth.clamp(movement / reference,
                    running ? 0.70D : 0.58D,
                    running ? 1.85D : 1.95D);
        });
        controllers.add(locomotion);

        AnimationController<Scp939Entity> actionController =
                new AnimationController<>(entity, "action", 2, state -> {
                    return switch (entity.getAction()) {
                        case Scp939Entity.ACTION_BITE ->
                                state.setAndContinue(SCPADDITIONS_939_BITE);
                        case Scp939Entity.ACTION_POUNCE ->
                                state.setAndContinue(SCPADDITIONS_939_POUNCE);
                        case Scp939Entity.ACTION_PIN_LAND ->
                                state.setAndContinue(SCPADDITIONS_939_PIN);
                        case Scp939Entity.ACTION_MAUL ->
                                state.setAndContinue(SCPADDITIONS_939_MAUL);
                        case Scp939Entity.ACTION_KICKED ->
                                state.setAndContinue(SCPADDITIONS_939_KICKED);
                        case Scp939Entity.ACTION_HURT ->
                                state.setAndContinue(SCPADDITIONS_939_HURT);
                        case Scp939Entity.ACTION_DEATH ->
                                state.setAndContinue(SCPADDITIONS_939_DEATH);
                        case Scp939Entity.ACTION_MIMIC ->
                                state.setAndContinue(SCPADDITIONS_939_MIMIC);
                        default -> PlayState.STOP;
                    };
                });
        controllers.add(actionController);
        ci.cancel();
    }

    @Unique
    private static double scpadditions$horizontalDisplacementSqr(
            Scp939Entity entity) {
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        return dx * dx + dz * dz;
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
