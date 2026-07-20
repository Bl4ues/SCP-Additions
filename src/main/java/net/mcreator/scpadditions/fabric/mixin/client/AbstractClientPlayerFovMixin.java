package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void scpAdditions$computeFovModifier(
            CallbackInfoReturnable<Float> callback) {
        float original = callback.getReturnValue();
        ComputeFovModifierEvent event = new ComputeFovModifierEvent(
                (AbstractClientPlayer) (Object) this, original);
        NeoForge.EVENT_BUS.post(event);
        callback.setReturnValue(event.getNewFovModifier());
    }
}
