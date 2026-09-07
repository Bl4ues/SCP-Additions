package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.entity.PlayerCorpseEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp131AEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp131BEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Places SCP-079 recognition boxes on authored visual anatomy rather than the
 * generic vanilla eye point, which is inaccurate for non-humanoid GeckoLib
 * models and for the rotated persistent-player corpse renderer.
 */
@Mixin(Scp079PlayableVisualsV2.class)
public abstract class Scp079RecognitionAnchorMixin {
    private static final double SCP_131_VISUAL_CENTER_Y = 0.30D;
    private static final double SCP_939_HEAD_CENTER_Y = 0.88D;
    private static final double SCP_939_HEAD_FORWARD = 0.78D;
    // PlayerCorpseRenderer collapses the 1.501-block humanoid model by -90deg,
    // scales it to 0.9375, then translates -1.16 along the collapsed body axis.
    // The rendered head centre therefore lands only ~0.48 block from the entity
    // origin. The old 1.16 offset was measuring the translation itself and sent
    // the tracker well beyond the head.
    private static final double CORPSE_HEAD_FORWARD = 0.482D;
    private static final float[] CORPSE_YAW_OFFSETS = {
            -8.0F, 6.0F, -3.0F, 10.0F, -11.0F, 3.0F
    };
    private static final float SCP_131_RECOGNITION_WIDTH = 0.58F;
    private static final float SCP_939_HEAD_RECOGNITION_WIDTH = 0.60F;

    @Redirect(method = "captureRecognition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getX()D",
                    ordinal = 0),
            remap = false)
    private static double scpclassifieddirective$recognitionX(Entity entity) {
        return entity.getX() + scpclassifieddirective$forward(entity).x;
    }

    @Redirect(method = "captureRecognition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getEyeY()D",
                    ordinal = 0),
            remap = false)
    private static double scpclassifieddirective$recognitionY(Entity entity) {
        if (entity instanceof Scp131AEntity || entity instanceof Scp131BEntity) {
            return entity.getY() + SCP_131_VISUAL_CENTER_Y;
        }
        if (entity instanceof Scp939Entity) {
            return entity.getY() + SCP_939_HEAD_CENTER_Y;
        }
        return entity.getEyeY();
    }

    @Redirect(method = "captureRecognition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getZ()D",
                    ordinal = 0),
            remap = false)
    private static double scpclassifieddirective$recognitionZ(Entity entity) {
        return entity.getZ() + scpclassifieddirective$forward(entity).z;
    }

    /** Match the final collapsed model axis, including its small pose yaw. */
    @Redirect(method = "captureRecognition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1),
            remap = false)
    private static Vec3 scpclassifieddirective$corpseHeadPosition(Entity entity) {
        Vec3 origin = entity.position();
        if (!(entity instanceof PlayerCorpseEntity corpse)) return origin;
        int variant = Math.max(0, Math.min(CORPSE_YAW_OFFSETS.length - 1,
                corpse.poseVariant()));
        float renderedYaw = corpse.getYRot() - CORPSE_YAW_OFFSETS[variant];
        Vec3 forward = Vec3.directionFromRotation(0.0F, renderedYaw);
        double horizontal = Math.sqrt(forward.x * forward.x
                + forward.z * forward.z);
        if (horizontal < 1.0E-5D) return origin;
        return origin.add(forward.x / horizontal * CORPSE_HEAD_FORWARD,
                0.0D, forward.z / horizontal * CORPSE_HEAD_FORWARD);
    }

    @Redirect(method = "captureRecognition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getBbWidth()F",
                    ordinal = 0),
            remap = false)
    private static float scpclassifieddirective$recognitionWidth(Entity entity) {
        if (entity instanceof Scp131AEntity || entity instanceof Scp131BEntity) {
            return SCP_131_RECOGNITION_WIDTH;
        }
        if (entity instanceof Scp939Entity) {
            return SCP_939_HEAD_RECOGNITION_WIDTH;
        }
        return entity.getBbWidth();
    }

    private static Vec3 scpclassifieddirective$forward(Entity entity) {
        if (!(entity instanceof Scp939Entity)) return Vec3.ZERO;
        Vec3 look = entity.getLookAngle();
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontal < 1.0E-5D) return Vec3.ZERO;
        return new Vec3(look.x / horizontal * SCP_939_HEAD_FORWARD,
                0.0D, look.z / horizontal * SCP_939_HEAD_FORWARD);
    }
}
