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

    @Unique private static final double SCPADDITIONS_MOVE_DELTA_SQR = 0.0015D * 0.0015D;
    @Unique private static final int SCPADDITIONS_MOVE_HOLD_TICKS = 4;

    @Unique private float scpadditions$turnVelocity;
    @Unique private int scpadditions$movementHoldTicks;
    @Unique private int scpadditions$lastMovementSampleTick = Integer.MIN_VALUE;

    @Inject(method = "registerControllers", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replace939AnimationControllers(
            AnimatableManager.ControllerRegistrar controllers, CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;

        AnimationController<Scp939Entity> locomotion =
                new AnimationController<>(entity, "locomotion", 7, state -> {
                    byte action = entity.getAction();
                    if (scpadditions$isCombatFullBodyAction(action)) return PlayState.STOP;
                    if (action == Scp939Entity.ACTION_LISTEN) {
                        return state.setAndContinue(SCPADDITIONS_939_LISTEN);
                    }

                    Scp939AwarenessState awareness = entity.getAwarenessState();
                    boolean moving = scpadditions$isVisuallyMoving(entity,
                            state.isMoving());
                    boolean turning = Math.abs(Mth.wrapDegrees(
                            entity.getYRot() - entity.yRotO)) >= 0.45F;

                    if (moving) {
                        if (awareness == Scp939AwarenessState.CONFIRMED_HUNT
                                || awareness == Scp939AwarenessState.LOST_SEARCH) {
                            return state.setAndContinue(SCPADDITIONS_939_RUN);
                        }
                        return state.setAndContinue(SCPADDITIONS_939_WALK);
                    }
                    if (turning) return state.setAndContinue(SCPADDITIONS_939_WALK);
                    if (awareness == Scp939AwarenessState.SEARCH) {
                        return state.setAndContinue(SCPADDITIONS_939_SEARCH);
                    }
                    if (awareness == Scp939AwarenessState.HEARD_SOUND) {
                        return state.setAndContinue(SCPADDITIONS_939_LISTEN);
                    }
                    return state.setAndContinue(SCPADDITIONS_939_IDLE);
                });

        locomotion.setAnimationSpeedHandler(animatable -> {
            if (animatable.getAction() == Scp939Entity.ACTION_LISTEN) return 1.0D;
            Scp939AwarenessState awareness = animatable.getAwarenessState();
            boolean running = awareness == Scp939AwarenessState.CONFIRMED_HUNT
                    || awareness == Scp939AwarenessState.LOST_SEARCH;

            if (scpadditions$movementHoldTicks > 0
                    || animatable.getDeltaMovement().horizontalDistanceSqr()
                    > SCPADDITIONS_MOVE_DELTA_SQR) {
                return running ? 1.10D : 1.28D;
            }

            float turn = Math.abs(Mth.wrapDegrees(
                    animatable.getYRot() - animatable.yRotO));
            return turn >= 0.45F ? 0.72D : 1.0D;
        });
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
        if (scpadditions$lastMovementSampleTick != entity.tickCount) {
            scpadditions$lastMovementSampleTick = entity.tickCount;

            double dx = entity.getX() - entity.xo;
            double dz = entity.getZ() - entity.zo;
            double positionDeltaSqr = dx * dx + dz * dz;
            double velocitySqr = entity.getDeltaMovement().horizontalDistanceSqr();

            if (geckoMoving || positionDeltaSqr > SCPADDITIONS_MOVE_DELTA_SQR
                    || velocitySqr > SCPADDITIONS_MOVE_DELTA_SQR) {
                scpadditions$movementHoldTicks = SCPADDITIONS_MOVE_HOLD_TICKS;
            } else if (scpadditions$movementHoldTicks > 0) {
                scpadditions$movementHoldTicks--;
            }
        } else if (geckoMoving) {
            scpadditions$movementHoldTicks = SCPADDITIONS_MOVE_HOLD_TICKS;
        }

        return scpadditions$movementHoldTicks > 0;
    }

    @Inject(method = "smoothLocomotionRotation", at = @At("HEAD"), cancellable = true)
    private void scpadditions$smoothTurnAcceleration(CallbackInfo ci) {
        Scp939Entity entity = (Scp939Entity) (Object) this;
        float desired = entity.getYRot();
        float previous = entity.yRotO;
        float error = Mth.wrapDegrees(desired - previous);
        byte action = entity.getAction();
        float maxSpeed = action == Scp939Entity.ACTION_POUNCE ? 13.0F
                : action == Scp939Entity.ACTION_BITE ? 9.0F : 5.5F;
        float targetVelocity = Mth.clamp(error * 0.34F, -maxSpeed, maxSpeed);
        scpadditions$turnVelocity += (targetVelocity - scpadditions$turnVelocity) * 0.30F;
        if (Math.abs(error) < 0.12F) scpadditions$turnVelocity *= 0.60F;
        if (Math.abs(scpadditions$turnVelocity) > Math.abs(error)) scpadditions$turnVelocity = error;

        float yaw = previous + scpadditions$turnVelocity;
        entity.setYRot(yaw);
        entity.yBodyRot = Mth.rotLerp(0.24F, entity.yBodyRot, yaw);
        float headTarget = previous + Mth.clamp(error, -24.0F, 24.0F);
        entity.yHeadRot = Mth.rotLerp(0.38F, entity.yHeadRot, headTarget);
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
