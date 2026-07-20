package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
abstract class ScreenDragMixin {
    @Inject(
            method = "mouseDragged(DDIDD)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            CallbackInfoReturnable<Boolean> callback) {
        ScreenEvent.MouseDragged.Pre event = new ScreenEvent.MouseDragged.Pre(
                (Screen) (Object) this,
                mouseX, mouseY, button, dragX, dragY);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.setReturnValue(true);
    }
}
