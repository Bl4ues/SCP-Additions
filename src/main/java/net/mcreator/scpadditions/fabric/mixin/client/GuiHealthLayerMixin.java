package net.mcreator.scpadditions.fabric.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class GuiHealthLayerMixin {
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$beforePlayerHealth(
            GuiGraphics graphics,
            CallbackInfo callback) {
        RenderGuiLayerEvent.Pre event = new RenderGuiLayerEvent.Pre(
                VanillaGuiLayers.PLAYER_HEALTH);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.cancel();
    }
}
