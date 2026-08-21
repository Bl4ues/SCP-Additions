package net.mcreator.scpadditions.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.client.Scp939PinPresentationClient;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives pinned players one deterministic horizontal render pose.
 *
 * Relying on vanilla's interpolated swimming amount made the result depend on
 * exactly which frame another client first saw Pose.SWIMMING. During that blend
 * the model could rotate through a misleading orientation and, combined with
 * the pin yaw, appear to have its feet where its head should be. A pin is not a
 * swimming transition, so render it explicitly like a body lying flat on a bed.
 */
@Mixin(PlayerRenderer.class)
public abstract class Scp939PinnedPlayerRotationMixin {
    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void scpadditions$renderPinnedPlayerFlat(
            AbstractClientPlayer player, PoseStack poseStack,
            float ageInTicks, float rotationYaw, float partialTick,
            CallbackInfo ci) {
        if (!Scp939PinPresentationClient.isPinnedVictim(player)) return;

        Scp939Entity predator =
                Scp939PinPresentationClient.findPinning939(player);
        float bodyYaw;
        if (predator != null) {
            // The victim lies opposite the 939's facing direction: head toward
            // the attacker, feet extending away from it.
            bodyYaw = Mth.rotLerp(partialTick, predator.yRotO,
                    predator.getYRot()) + 180.0F;
        } else {
            bodyYaw = rotationYaw;
        }

        // This is the same basic transform used by vanilla swimming, but it is
        // complete immediately instead of passing through swimAmount 0 -> 1.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(0.0D, -1.0D, 0.30D);
        ci.cancel();
    }
}
