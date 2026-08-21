package net.mcreator.scpadditions.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.client.Scp939PinPresentationClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Completes the vanilla swimming/prone rotation for a player pinned by SCP-939.
 *
 * Forced Pose.SWIMMING is synchronized to other clients immediately, while the
 * vanilla swim animation amount may still be partway through its interpolation.
 * Applying only the missing fraction here, after PlayerRenderer has established
 * its normal yaw and swim transform, keeps head/feet orientation stable without
 * double-rotating players that vanilla already rendered fully prone.
 */
@Mixin(PlayerRenderer.class)
public abstract class Scp939PinnedPlayerRotationMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void scpadditions$finishPinnedProneRotation(
            AbstractClientPlayer player, PoseStack poseStack,
            float ageInTicks, float rotationYaw, float partialTick,
            CallbackInfo ci) {
        if (!Scp939PinPresentationClient.isPinnedVictim(player)) return;

        float swim = Mth.clamp(player.getSwimAmount(partialTick), 0.0F, 1.0F);
        float missing = 1.0F - swim;
        if (missing <= 0.001F) return;

        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * missing));
        poseStack.translate(0.0D, -1.0D * missing, 0.30D * missing);
    }
}
