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
         * PlayerRenderer's sleeping-style basis is longitudinally reversed from
         * ordinary standing yaw. The previous 90-yaw basis left the victim's
         * feet pointing at the 939; adding the half turn places the head at the
         * predator and lets the legs extend outward, while keeping the player
         * face-up like a body lying on a bed.
         */
        float sleepingDirectionRotation = 270.0F - predatorYaw;
        poseStack.mulPose(Axis.YP.rotationDegrees(sleepingDirectionRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        ci.cancel();
    }
}
