package net.mcreator.scpadditions.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Scp939Entity.class, remap = false)
public abstract class Scp939PinnedViewFreedomMixin {
    // The flat player model extends forward from its entity origin. Keeping that
    // origin in front of SCP-939 therefore pushed most of the victim outside the
    // animal's footprint. A small rearward offset centers the torso under the
    // predator while the victim's head still points toward the 939's head.
    @Unique private static final double SCPADDITIONS_PIN_FORWARD = -0.18D;
    @Unique private double scpadditions$pinGroundY = Double.NaN;

    @Inject(method = "beginPin", at = @At("TAIL"), remap = false)
    private void scpadditions$capturePinFloor(ServerPlayer player, CallbackInfo ci) {
        Scp939Entity predator = (Scp939Entity) (Object) this;
        double playerGround = scpadditions$findGroundY(player, 4.0D);
        scpadditions$pinGroundY = Double.isFinite(playerGround)
                ? playerGround : player.getY();
        scpadditions$groundPredator(predator, 4.0D);
    }

    @Inject(method = "tickPinned", at = @At("HEAD"), remap = false)
    private void scpadditions$keepMaulOnFloor(
            net.minecraft.server.level.ServerLevel level, CallbackInfo ci) {
        scpadditions$groundPredator((Scp939Entity) (Object) this, 2.75D);
    }

    @Inject(method = "releasePin", at = @At("TAIL"), remap = false)
    private void scpadditions$forgetPinFloor(boolean kickedOff, CallbackInfo ci) {
        scpadditions$pinGroundY = Double.NaN;
    }

    @Redirect(method = "tickPinned",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V",
                    remap = true),
            remap = false)
    private void scpadditions$groundPinnedVictim(ServerGamePacketListenerImpl connection,
            double x, double y, double z, float forcedYaw, float forcedPitch) {
        ServerPlayer player = connection.player;
        Scp939Entity predator = (Scp939Entity) (Object) this;
        Vec3 radial = new Vec3(x - predator.getX(), 0.0D, z - predator.getZ());
        if (radial.horizontalDistanceSqr() < 1.0E-5D) {
            Vec3 look = predator.getLookAngle();
            radial = new Vec3(look.x, 0.0D, look.z);
        }
        if (radial.horizontalDistanceSqr() < 1.0E-5D) {
            radial = new Vec3(0.0D, 0.0D, 1.0D);
        }
        radial = radial.normalize();
        Vec3 pinPosition = predator.position().add(
                radial.scale(SCPADDITIONS_PIN_FORWARD));
        double groundedY = Double.isFinite(scpadditions$pinGroundY)
                ? scpadditions$pinGroundY : player.getY();
        connection.teleport(pinPosition.x, groundedY, pinPosition.z,
                player.getYRot(), player.getXRot());
    }

    @Unique
    private static void scpadditions$groundPredator(Scp939Entity predator,
            double depth) {
        if (predator == null) return;
        double ground = scpadditions$findGroundY(predator, depth);
        if (!Double.isFinite(ground)) return;
        double above = predator.getY() - ground;
        if (above < -0.15D || above > depth) return;
        predator.setPos(predator.getX(), ground + 0.001D, predator.getZ());
        predator.setDeltaMovement(Vec3.ZERO);
        predator.fallDistance = 0.0F;
    }

    @Unique
    private static double scpadditions$findGroundY(Entity entity, double depth) {
        if (entity == null) return Double.NaN;
        Vec3 start = entity.position().add(0.0D, 0.65D, 0.0D);
        Vec3 end = entity.position().add(0.0D, -Math.max(1.0D, depth), 0.0D);
        BlockHitResult hit = entity.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().y : Double.NaN;
    }
}
