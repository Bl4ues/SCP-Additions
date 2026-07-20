package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.entity.Scp131BEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp131BRenderer extends GeoEntityRenderer<Scp131BEntity> {
    private static final float VISUAL_SCALE = 2.3F;
    private static final ResourceLocation TEXTURE_RESOURCE = ResourceLocation.fromNamespaceAndPath("scpinventory", "textures/entities/scp131b.png");

    public Scp131BRenderer(EntityRendererProvider.Context context) {
        super(context, new Scp131Model<>(TEXTURE_RESOURCE));
        this.shadowRadius = 0.24F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(Scp131BEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void render(Scp131BEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(VISUAL_SCALE, VISUAL_SCALE, VISUAL_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
