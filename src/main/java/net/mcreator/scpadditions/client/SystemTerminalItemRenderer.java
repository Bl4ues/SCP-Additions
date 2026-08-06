package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.item.SystemTerminalItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Double-sided item renderer for the SCiPNET terminal. */
public final class SystemTerminalItemRenderer
        extends GeoItemRenderer<SystemTerminalItem> {
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            "scp_additions", "textures/block/system_terminal_glowmask.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public SystemTerminalItemRenderer() {
        super(new SystemTerminalItemGeoModel());

        /* Keep the item from invoking AutoGlowingTexture on the base texture
         * shared with the placed block. The authored glowmask is already a
         * complete coloured emissive texture, so render it directly. */
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
