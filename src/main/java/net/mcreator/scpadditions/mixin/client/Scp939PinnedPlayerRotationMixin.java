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

/** Renders an SCP-939 maul victim face-up with their head toward the attacker. */
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
        float predatorYaw = predator != null
                ? Mth.rotLerp(partialTick, predator.yRotO, predator.getYRot())
                : rotationYaw;

        /*
         * The flat sleeping basis reverses the apparent longitudinal axis after
         * the Z/Y rotations below. In this basis 90 - predatorYaw is the version
         * that puts the victim's head under the 939's head/chest and their feet
         * away from it. The opposite half-turn visibly puts the legs at the
         * predator, which is exactly the failure this transform must avoid.
         */
        float sleepingDirectionRotation = 90.0F - predatorYaw;
        poseStack.mulPose(Axis.YP.rotationDegrees(sleepingDirectionRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        ci.cancel();
    }
}
