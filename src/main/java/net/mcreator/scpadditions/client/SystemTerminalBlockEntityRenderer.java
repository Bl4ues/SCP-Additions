package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Shader-compatible placed renderer for the Facility Diagnostic Terminal. */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/block/system_terminal_glowmask.png");

    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());

        /*
         * Use the authored glowmask as an ordinary resource texture. The old
         * dynamically registered copy rendered in item contexts, but vanished
         * from the placed block entity in shader/PBR render passes.
         */
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
