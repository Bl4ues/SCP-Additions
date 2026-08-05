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
    public RenderType getRenderType(SystemTerminalBlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        // Match the known-good Elevator Floor Station render path. The boolean
        // variant preserves the base texture while the eyes layer handles only
        // the authored _e glowmask.
        return RenderType.entityTranslucent(texture, true);
    }
}
