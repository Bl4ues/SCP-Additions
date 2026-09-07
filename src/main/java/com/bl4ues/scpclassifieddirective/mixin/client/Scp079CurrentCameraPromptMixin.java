package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Prevents a feed from offering to switch to the camera already being used. */
@Mixin(value = Scp079PlayableVisualsV2.class, remap = false)
public abstract class Scp079CurrentCameraPromptMixin {
    private static final double SELF_CAMERA_RADIUS_SQR = 0.75D * 0.75D;

    @Shadow @Final private static List<?> PROMPTS;

    @Inject(method = "refreshPrompts", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$removeCurrentCameraPrompt(
            Minecraft minecraft, CallbackInfo ci) {
        if (!Scp079PlayableClient.cameraMode() || minecraft.level == null) return;
        Vec3 currentEye = Scp079PlayableClient.viewPosition();
        PROMPTS.removeIf(raw -> {
            if (!(raw instanceof Scp079InteractionPromptAccessor accessor)) {
                return false;
            }
            BlockPos pos = accessor.scpclassifieddirective$pos();
            BlockState state = minecraft.level.getBlockState(pos);
            if (!state.is(CeilingCameraModule.BLOCK.get())
                    && !state.is(SurveillanceCameraPlaceholderModule.BLOCK.get())) {
                return false;
            }
            return Vec3.atCenterOf(pos).distanceToSqr(currentEye)
                    <= SELF_CAMERA_RADIUS_SQR;
        });
    }
}
