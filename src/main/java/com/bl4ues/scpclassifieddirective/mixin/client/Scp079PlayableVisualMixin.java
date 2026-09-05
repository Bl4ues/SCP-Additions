package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces first-pass SCP-079 presentation/input while preserving gameplay state. */
@Mixin(Scp079PlayableClient.class)
public abstract class Scp079PlayableVisualMixin {
    @Inject(method = "onClientTickStart", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$inventoryRouting(
            TickEvent.ClientTickEvent event, CallbackInfo ci) {
        Scp079PlayableVisualsV2.handleInventoryKey(event);
        ci.cancel();
    }

    @Inject(method = "renderRecognition", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$screenRecognition(
            RenderLevelStageEvent event, CallbackInfo ci) {
        Scp079PlayableVisualsV2.captureRecognition(event);
        ci.cancel();
    }

    @Inject(method = "renderCameraHud", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$cameraHud(
            GuiGraphics graphics, CallbackInfo ci) {
        Scp079PlayableVisualsV2.renderCameraHud(graphics);
        ci.cancel();
    }

    @Inject(method = "renderLocalHud", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$localHud(
            GuiGraphics graphics, CallbackInfo ci) {
        Scp079PlayableVisualsV2.renderLocalHud(graphics);
        ci.cancel();
    }

    @Inject(method = "consumeCameraActions", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$disableLegacyAimClicks(
            Minecraft minecraft, CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "updateLocalCamera",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;lerp(DDD)D"),
            remap = false)
    private static double scpclassifieddirective$swapOrbitDistances(
            double delta, double start, double end) {
        return Mth.lerp(delta, end, start);
    }
}
