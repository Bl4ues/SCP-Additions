package net.mcreator.scpadditions.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pinning owns the victim's position, not their view rotation. The previous
 * server teleport forced yaw/pitch every two ticks, which meant a detached
 * third-person camera was still dragged back toward SCP-939 even after the
 * client-only cinematic camera had been disabled there.
 *
 * First person is framed separately by the SCP-939 camera presentation code, so
 * preserving the player's live view rotation here makes third person genuinely
 * free without changing the authored first-person attack shot.
 */
@Mixin(value = Scp939Entity.class, remap = false)
public abstract class Scp939PinnedViewFreedomMixin {
    @Redirect(method = "tickPinned",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V",
                    remap = true),
            remap = false)
    private void scpadditions$preservePinnedViewRotation(
            ServerGamePacketListenerImpl connection,
            double x, double y, double z, float forcedYaw, float forcedPitch) {
        ServerPlayer player = connection.player;
        connection.teleport(x, y, z, player.getYRot(), player.getXRot());
    }
}
