package net.mcreator.scpadditions.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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

/**
 * Keeps a pinned victim physically grounded while leaving their view rotation
 * free for third-person inspection.
 *
 * tickPinned originally rebuilt the victim position from the 939 every two
 * ticks, including the predator's current Y. A pounce can enter pin while the
 * 939 is still airborne, so that made the player climb with it. The overlapping
 * player collision then became a moving support surface and both entities could
 * slowly rise together. Capture the floor once when the pin begins and keep the
 * tiny swimming hitbox outside the 939's own hitbox instead.
 */
@Mixin(value = Scp939Entity.class, remap = false)
public abstract class Scp939PinnedViewFreedomMixin {
    @Unique
    private static final double SCPADDITIONS_PIN_FORWARD = 1.02D;
    @Unique
    private double scpadditions$pinGroundY = Double.NaN;

    @Inject(method = "beginPin", at = @At("TAIL"), remap = false)
    private void scpadditions$capturePinFloor(ServerPlayer player,
            CallbackInfo ci) {
        scpadditions$pinGroundY = scpadditions$findGroundY(player);
    }

    @Inject(method = "releasePin", at = @At("TAIL"), remap = false)
    private void scpadditions$forgetPinFloor(boolean kickedOff,
            CallbackInfo ci) {
        scpadditions$pinGroundY = Double.NaN;
    }

    @Redirect(method = "tickPinned",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V",
                    remap = true),
            remap = false)
    private void scpadditions$groundPinnedVictim(
            ServerGamePacketListenerImpl connection,
            double x, double y, double z, float forcedYaw, float forcedPitch) {
        ServerPlayer player = connection.player;
        Scp939Entity predator = (Scp939Entity) (Object) this;

        // The original x/z arguments already encode the predator's forward
        // direction. Reuse that direction, but move the player's 0.6-wide
        // swimming hitbox just beyond the 939's 1.2-wide collision box. The
        // horizontal player model still extends back underneath the predator.
        Vec3 radial = new Vec3(x - predator.getX(), 0.0D,
                z - predator.getZ());
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
    private static double scpadditions$findGroundY(ServerPlayer player) {
        if (player == null) return 0.0D;
        Vec3 start = player.position().add(0.0D, 0.30D, 0.0D);
        Vec3 end = player.position().add(0.0D, -3.25D, 0.0D);
        BlockHitResult hit = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation().y + 0.01D;
        }
        return player.getY();
    }
}
