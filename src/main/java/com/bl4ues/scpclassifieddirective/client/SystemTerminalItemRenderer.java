package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.SystemTerminalItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Double-sided item renderer for the SCiPNET terminal. */
public final class SystemTerminalItemRenderer
        extends GeoItemRenderer<SystemTerminalItem> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/block/system_terminal_glowmask.png");

    public SystemTerminalItemRenderer() {
        super(new SystemTerminalItemGeoModel());

        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    SystemTerminalItem animatable,
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
    public RenderType getRenderType(SystemTerminalItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
