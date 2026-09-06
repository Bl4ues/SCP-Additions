package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Uses the physical host listener only while SCP-079 is actually at its host. */
@Mixin(SoundEngine.class)
public abstract class Scp079LocalHostAudioMixin {
    @Redirect(method = "updateSource",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 scpclassifieddirective$localHostListenerPosition(Camera camera) {
        if (!useLocalHostListener()) return camera.getPosition();
        return Vec3.atCenterOf(Scp079PlayableClient.hostPos());
    }

    @Redirect(method = "updateSource",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getLookVector()Lorg/joml/Vector3f;"))
    private Vector3f scpclassifieddirective$localHostListenerLook(Camera camera) {
        if (!useLocalHostListener()) return camera.getLookVector();
        Direction facing = hostFacing();
        return new Vector3f(facing.getStepX(), 0.0F, facing.getStepZ());
    }

    @Redirect(method = "updateSource",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getUpVector()Lorg/joml/Vector3f;"))
    private Vector3f scpclassifieddirective$localHostListenerUp(Camera camera) {
        if (!useLocalHostListener()) return camera.getUpVector();
        return new Vector3f(0.0F, 1.0F, 0.0F);
    }

    private static boolean useLocalHostListener() {
        Minecraft minecraft = Minecraft.getInstance();
        return Scp079PlayableClient.active()
                && !Scp079PlayableClient.cameraMode()
                && minecraft.level != null
                && minecraft.level.dimension().location().equals(
                        Scp079PlayableClient.hostDimension());
    }

    private static Direction hostFacing() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return Direction.NORTH;
        BlockState state = minecraft.level.getBlockState(
                Scp079PlayableClient.hostPos());
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }
}
