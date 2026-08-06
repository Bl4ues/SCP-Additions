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

/** Placed renderer with an explicit shader-compatible emissive pass. */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            "scp_additions", "textures/block/system_terminal_e.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());

        // GeckoLib's automatic glow layer is suppressed by some shader wrappers
        // for block entities. Use the exact explicit eyes pass already proven by
        // the Elevator Floor Station so the authored _e mask remains bloom-ready.
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    SystemTerminalBlockEntity animatable,
                    BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                RenderType emissive = RenderType.eyes(GLOWMASK);
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
