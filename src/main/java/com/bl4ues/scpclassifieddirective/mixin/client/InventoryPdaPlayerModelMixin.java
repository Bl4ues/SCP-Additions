package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.pda.InventoryPdaThirdPersonClient;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the PDA grip after vanilla has finished calculating player limbs. */
@Mixin(PlayerModel.class)
public abstract class InventoryPdaPlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL"))
    private void scpClassifiedDirective$posePdaArms(LivingEntity entity,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, CallbackInfo callback) {
        if (entity instanceof Player player) {
            InventoryPdaThirdPersonClient.applyArmPose(
                    (PlayerModel<?>) (Object) this, player);
        }
    }
}
