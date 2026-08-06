package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Shader-compatible placed renderer for the Facility Diagnostic Terminal. */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation(
            "scp_additions", "textures/block/system_terminal.png");
    private static final ResourceLocation GENERATED_GLOW_TEXTURE =
            new ResourceLocation("scp_additions",
                    "textures/block/system_terminal_glowmask.png");

    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());

        /*
         * Let GeckoLib generate its emissive texture from the authored
         * _glowmask using its public 4.4.9 API. The returned custom render type
         * is intentionally discarded; only its registered generated texture is
         * reused through vanilla's shader-compatible eyes pass.
         */
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            protected RenderType getRenderType(
                    SystemTerminalBlockEntity animatable) {
                AutoGlowingTexture.getRenderType(BASE_TEXTURE);
                return RenderType.eyes(GENERATED_GLOW_TEXTURE);
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
        return RenderType.entityTranslucent(texture);
    }
}
