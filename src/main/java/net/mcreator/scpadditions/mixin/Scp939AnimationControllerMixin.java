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
 * The previous controller forced walk/run playback to at least 0.90x even when
 * navigation had slowed almost to a stop. That guarantees visible paw skating:
 * the authored stance continues moving while the entity barely advances. This
 * replacement derives playback from actual horizontal velocity and keeps idle,
 * walk, run, search and listening on one controller so GeckoLib can transition
 * between them instead of snapping between separate controllers.
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

    // Nominal physical speeds: MOVEMENT_SPEED 0.285 * navigation modifier.
    // IDLE 0.46 -> 0.1311 block/tick; HUNT 1.20 -> 0.342 block/tick.
    @Unique
    private static final double SCPADDITIONS_WALK_REFERENCE_SPEED = 0.131D;
    @Unique
    private static final double SCPADDITIONS_RUN_REFERENCE_SPEED = 0.342D;
    @Unique
    private static final double SCPADDITIONS_MOVING_EPSILON_SQR = 0.00012D;

    @Inject(method = "registerControllers", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replace939AnimationControllers(
            AnimatableManager.ControllerRegistrar controllers,
            CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;

        AnimationController<Scp939Entity> locomotion =
                new AnimationController<>(entity, "locomotion", 9, state -> {
                    byte action = entity.getAction();
                    if (scpadditions$isCombatFullBodyAction(action)) {
                        return PlayState.STOP;
                    }

                    // Listening belongs here rather than on the action controller.
                    // This gives walk -> listen and listen -> walk the same native
                    // transition as every other locomotion state.
                    if (action == Scp939Entity.ACTION_LISTEN) {
                        return state.setAndContinue(SCPADDITIONS_939_LISTEN);
                    }

                    Scp939AwarenessState awareness = entity.getAwarenessState();
                    boolean moving = entity.getDeltaMovement()
                            .horizontalDistanceSqr()
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

            double movement = Math.sqrt(animatable.getDeltaMovement()
                    .horizontalDistanceSqr());
            if (movement * movement <= SCPADDITIONS_MOVING_EPSILON_SQR) {
                return 1.0D;
            }

            Scp939AwarenessState awareness = animatable.getAwarenessState();
            boolean running = awareness == Scp939AwarenessState.CONFIRMED_HUNT
                    || awareness == Scp939AwarenessState.LOST_SEARCH;
            double reference = running
                    ? SCPADDITIONS_RUN_REFERENCE_SPEED
                    : SCPADDITIONS_WALK_REFERENCE_SPEED;

            // A low floor is intentional. If collision/pathing reduces physical
            // speed, the feet must slow with the body rather than treadmill at
            // almost full animation speed.
            return Mth.clamp(movement / reference,
                    running ? 0.28D : 0.12D,
                    running ? 1.75D : 2.00D);
        });
        controllers.add(locomotion);

        // Bite and mimic only author upper-body bones and therefore layer over
        // locomotion. Pounce/maul/hurt/death still replace the full body quickly.
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
    private static boolean scpadditions$isCombatFullBodyAction(byte action) {
        return action == Scp939Entity.ACTION_POUNCE
                || action == Scp939Entity.ACTION_PIN_LAND
                || action == Scp939Entity.ACTION_MAUL
                || action == Scp939Entity.ACTION_KICKED
                || action == Scp939Entity.ACTION_HURT
                || action == Scp939Entity.ACTION_DEATH;
    }
}
