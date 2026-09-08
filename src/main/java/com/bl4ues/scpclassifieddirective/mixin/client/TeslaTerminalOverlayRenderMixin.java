package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.TeslaTerminalBlockEntityRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Popup layers need deterministic depth rather than translucent-entity sorting.
 * Their authored transparent background still cuts away, while the visible UI
 * pixels stay above the emissive base CRT even under shader pipelines.
 */
@Mixin(value = TeslaTerminalBlockEntityRenderer.class, remap = false)
public abstract class TeslaTerminalOverlayRenderMixin {
    @Redirect(method = "renderQuad",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entityTranslucentEmissive(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private static RenderType scpclassifieddirective$stableTerminalLayer(
            ResourceLocation texture) {
        String path = texture.getPath();
        if (path.startsWith("textures/screens/") && path.endsWith(".png")) {
            String name = path.substring("textures/screens/".length(),
                    path.length() - 4);
            if (name.equals("5") || name.equals("6") || name.equals("7")
                    || name.equals("8") || name.equals("9")
                    || name.equals("10") || name.equals("12")) {
                return RenderType.entityCutoutNoCull(texture);
            }
        }
        return RenderType.entityTranslucentEmissive(texture);
    }
}
