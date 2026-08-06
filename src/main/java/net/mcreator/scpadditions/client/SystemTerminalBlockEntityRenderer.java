package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Shader-compatible placed renderer for the Facility Diagnostic Terminal. */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            "scp_additions", "textures/block/system_terminal_glowmask.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());

        /*
         * RenderType.eyes works for the item renderer but can be swallowed by
         * shader wrappers on placed block entities. The translucent-emissive
         * entity pass preserves the authored coloured glowmask, remains
         * full-bright and is accepted by those block-entity render paths.
         */
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    SystemTerminalBlockEntity animatable,
                    BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                RenderType emissive = RenderType.entityTranslucentEmissive(
                        GLOWMASK, true);
                getRenderer().reRender(bakedModel, poseStack, bufferSource,
                        animatable, emissive,
                        bufferSource.getBuffer(emissive), partialTick,
                        FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        1.0F, 1.0F, 1.0F, 1.0F);
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
