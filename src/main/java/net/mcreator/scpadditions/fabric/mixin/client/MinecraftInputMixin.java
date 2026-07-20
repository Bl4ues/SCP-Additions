package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftInputMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$beforeUseKey(CallbackInfo callback) {
        InputEvent.InteractionKeyMappingTriggered event =
                new InputEvent.InteractionKeyMappingTriggered(true);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.cancel();
    }
}
