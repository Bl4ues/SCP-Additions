package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Shader-safe placed renderer for the Facility Diagnostic Terminal. */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation(
            "scp_additions", "textures/block/system_terminal.png");

    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());

        /*
         * GeckoLib's _glowmask is not a ready-to-render emissive texture. Its
         * AutoGlowingTexture loader combines the mask with the base colours and
         * removes those pixels from the ordinary pass. Keep that processing,
         * but render the generated texture through vanilla's eyes pass so
         * Oculus/shader wrappers cannot discard GeckoLib's custom render type.
         */
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            protected RenderType getRenderType(
                    SystemTerminalBlockEntity animatable) {
                ResourceLocation generatedGlow =
                        AutoGlowingTexture.getEmissiveResource(BASE_TEXTURE);
                return RenderType.eyes(generatedGlow);
            }
        });
    }

    @Override
    public boolean shouldRenderOffScreen(
            SystemTerminalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public RenderType getRenderType(SystemTerminalBlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture, true);
    }
}
