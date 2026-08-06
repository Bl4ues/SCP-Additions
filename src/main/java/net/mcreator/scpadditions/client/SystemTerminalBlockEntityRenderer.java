package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Placed renderer matching the terminal item's known-good glow path. */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());

        // The item renderer already renders this exact model, texture and
        // authored _e mask correctly with and without shaders. Keep the placed
        // block on the same GeckoLib path instead of maintaining a divergent
        // custom re-render pass that can interfere with the base texture.
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public boolean shouldRenderOffScreen(
            SystemTerminalBlockEntity blockEntity) {
        // The model extends outside its host block, so vanilla's default
        // block-entity bounds would cull it while parts are still on screen.
        return true;
    }

    @Override
    public RenderType getRenderType(SystemTerminalBlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        // Match SystemTerminalItemRenderer exactly. Its base texture and glow
        // are confirmed to survive both vanilla rendering and shader packs.
        return RenderType.entityTranslucent(texture);
    }
}
