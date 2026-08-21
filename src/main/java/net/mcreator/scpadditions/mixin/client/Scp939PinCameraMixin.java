package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.client.Scp939ClientState;
import net.mcreator.scpadditions.client.Scp939PinPresentationClient;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forces the local maul camera down to a true floor-level viewpoint. */
@Mixin(Camera.class)
public abstract class Scp939PinCameraMixin {
    @Shadow
    private Vec3 position;

    @Shadow
    @Final
    private BlockPos.MutableBlockPos blockPosition;

    @Inject(method = "setup", at = @At("RETURN"))
    private void scpadditions$positionPinnedCamera(BlockGetter level,
            Entity cameraEntity, boolean detached, boolean mirror,
            float partialTick, CallbackInfo ci) {
        if (!(cameraEntity instanceof LocalPlayer player)
                || !Scp939ClientState.pinned()) {
            return;
        }

        Scp939Entity predator =
                Scp939PinPresentationClient.findPinning939(player);
        if (predator == null) return;

        Vec3 desired = Scp939PinPresentationClient.cameraPosition(predator);
        this.position = desired;
        this.blockPosition.set(Mth.floor(desired.x), Mth.floor(desired.y),
                Mth.floor(desired.z));
    }
}
