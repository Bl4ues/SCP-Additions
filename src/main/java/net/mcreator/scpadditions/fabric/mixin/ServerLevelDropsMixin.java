package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.mcreator.scpadditions.fabric.FabricLivingDropsCapture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
abstract class ServerLevelDropsMixin {
    @Inject(
            method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$captureDeathDrop(
            Entity entity,
            CallbackInfoReturnable<Boolean> callback) {
        if (FabricLivingDropsCapture.capture(
                (ServerLevel) (Object) this, entity)) {
            callback.setReturnValue(true);
        }
    }
}
