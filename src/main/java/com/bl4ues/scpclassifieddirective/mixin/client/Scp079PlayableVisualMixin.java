package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Replaces first-pass SCP-079 presentation/input while preserving gameplay state. */
@Mixin(Scp079PlayableClient.class)
public abstract class Scp079PlayableVisualMixin {
    @Inject(method = "onClientTickStart", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$inventoryRouting(
            TickEvent.ClientTickEvent event, CallbackInfo ci) {
        Scp079PlayableVisuals.handleInventoryKey(event);
        ci.cancel();
    }

    @Inject(method = "renderRecognition", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$screenRecognition(
            RenderLevelStageEvent event, CallbackInfo ci) {
        Scp079PlayableVisuals.captureRecognition(event);
        ci.cancel();
    }

    @Inject(method = "renderCameraHud", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$cameraHud(
            GuiGraphics graphics, CallbackInfo ci) {
        Scp079PlayableVisuals.renderCameraHud(graphics);
        ci.cancel();
    }

    @Inject(method = "renderLocalHud", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$localHud(
            GuiGraphics graphics, CallbackInfo ci) {
        Scp079PlayableVisuals.renderLocalHud(graphics);
        ci.cancel();
    }

    /** InputEvent now routes facility clicks through projected world prompts. */
    @Inject(method = "consumeCameraActions", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$disableLegacyAimClicks(
            Minecraft minecraft, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Keep the established orbit angles, but restore the intended distance rule:
     * the front of SCP-079 gets the wide shot and its back gets the close shot.
     */
    @ModifyArgs(method = "updateLocalCamera",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;lerp(DDD)D"),
            remap = false)
    private static void scpclassifieddirective$swapOrbitDistances(Args args) {
        double start = args.get(1);
        double end = args.get(2);
        args.set(1, end);
        args.set(2, start);
    }
}
