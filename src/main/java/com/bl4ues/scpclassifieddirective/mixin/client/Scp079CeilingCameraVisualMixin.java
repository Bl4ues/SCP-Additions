package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets the 079 feed HUD discover ceiling cameras anywhere wall cameras count. */
@Mixin(value = Scp079PlayableVisualsV2.class, remap = false)
public abstract class Scp079CeilingCameraVisualMixin {
    @Redirect(method = "scanRoom",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private static boolean scpclassifieddirective$includeCeilingCamera(
            BlockState state, Block expected) {
        return state.is(expected) || state.is(CeilingCameraModule.BLOCK.get());
    }
}
