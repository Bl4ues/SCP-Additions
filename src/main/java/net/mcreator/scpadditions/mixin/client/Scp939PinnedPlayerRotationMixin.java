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
 * Renders an SCP-939 maul victim as a person lying face-up on the floor.
 *
 * Rotating the standing model -90 degrees around X produced a front-flip pose:
 * the player ended face-down and the longitudinal direction was easy to invert.
 * Vanilla sleeping already solves the exact geometric problem we need, so this
 * uses the same Y -> Z(90) -> Y(270) basis as a player lying in a bed, with a
 * synthetic facing direction whose head points back toward the attacking 939.
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

        // The 939 faces outward from itself toward the pinned player. The
        // victim's head must point the other way, back toward the 939, while
        // their feet extend away from it.
        float headFacingYaw = predator != null
                ? Mth.rotLerp(partialTick, predator.yRotO,
                        predator.getYRot()) + 180.0F
                : rotationYaw;

        // Equivalent orientation basis to LivingEntityRenderer's sleeping
        // branch. For Minecraft yaw, the sleeping direction rotation is
        // 90 - facingYaw. Z+90 lays the model on its back rather than pitching
        // it face-first into the floor.
        float sleepingDirectionRotation = 90.0F - headFacingYaw;
        poseStack.mulPose(Axis.YP.rotationDegrees(sleepingDirectionRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));

        // Do not add the old -1Y/+Z corrective translation here. Vanilla's
        // player render pipeline positions a sleeping body around this basis;
        // Scp939PinPresentationClient supplies the small floor offset globally.
        ci.cancel();
    }
}
