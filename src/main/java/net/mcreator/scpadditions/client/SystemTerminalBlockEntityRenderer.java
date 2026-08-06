package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import net.mcreator.scpadditions.client.render.ShaderCompatibleGlowLayer;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Double-sided translucent renderer with a shader-compatible glowmask. */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "scp_additions", "textures/block/system_terminal.png");

    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());
        addRenderLayer(new ShaderCompatibleGlowLayer<>(this, TEXTURE));
    }

    @Override
    public boolean shouldRenderOffScreen(
            SystemTerminalBlockEntity blockEntity) {
        // The authored model extends well beyond its host block. Vanilla's
        // one-block block-entity bounds can therefore cull the complete model
        // after the first render/chunk rebuild even while it is still visible.
        return true;
    }

    @Override
    public RenderType getRenderType(SystemTerminalBlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture, true);
    }
}
