package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.MouseHandler;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseScrollInputMixin {
    @Inject(
            method = "onScroll(JDD)V",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$mouseScroll(
            long window,
            double horizontal,
            double vertical,
            CallbackInfo callback) {
        InputEvent.MouseScrollingEvent event =
                new InputEvent.MouseScrollingEvent(horizontal, vertical);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.cancel();
    }
}
