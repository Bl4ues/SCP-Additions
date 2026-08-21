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
 * Playback is calibrated against the authored gait cycle rather than the mob's
 * navigation speed modifier. Navigation speed is a MoveControl target and is not
 * the number of blocks actually travelled per tick after Minecraft movement
 * physics. Treating it as physical velocity made the 1.8-second walk play at a
 * fraction of its intended speed while the entity visibly crossed the floor.
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
     * The authored walk lasts 1.8 s (36 ticks) and visually covers roughly one
     * full quadrupedal stride. A reference near 0.032 block/tick therefore lets
     * one 1x cycle accompany about 1.15 blocks of travel. The 0.82 s run has a
     * much longer stride and uses ~0.095 block/tick, or about 1.56 blocks/cycle.
     * These are displacement references, not navigation modifiers.
     */
    @Unique
    private static final double SCPADDITIONS_WALK_REFERENCE_SPEED = 0.032D;
    @Unique
    private static final double SCPADDITIONS_RUN_REFERENCE_SPEED = 0.095D;
    @Unique
    private static final double SCPADDITIONS_MOVING_EPSILON_SQR = 0.00012D;

    @Inject(method = "registerControllers", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replace939AnimationControllers(
            AnimatableManager.ControllerRegistrar controllers,
            CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;

        AnimationController<Scp939Entity> locomotion =
                new AnimationController<>(entity, "locomotion", 7, state -> {
                    byte action = entity.getAction();
                    if (scpadditions$isCombatFullBodyAction(action)) {
                        return PlayState.STOP;
                    }

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

            // At the movement threshold the walk floor still corresponds to
            // roughly one authored stride over the travelled distance. It keeps
            // the paws alive during slow pathing without returning to the old
            // 0.90x treadmill behaviour.
            return Mth.clamp(movement / reference,
                    running ? 0.45D : 0.35D,
                    running ? 1.80D : 1.85D);
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
