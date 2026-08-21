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

        /*
         * The sleeping basis below reverses the model's longitudinal direction
         * compared with ordinary Minecraft facing. Feeding predatorYaw + 180
         * therefore put the victim's feet at the 939. Use the predator's own
         * outward yaw here: after the sleeping transform the model's head points
         * back toward the attacker and its feet extend away from it.
         */
        float sleepingFacingYaw = predator != null
                ? Mth.rotLerp(partialTick, predator.yRotO, predator.getYRot())
                : rotationYaw;

        float sleepingDirectionRotation = 90.0F - sleepingFacingYaw;
        poseStack.mulPose(Axis.YP.rotationDegrees(sleepingDirectionRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        ci.cancel();
    }
}
