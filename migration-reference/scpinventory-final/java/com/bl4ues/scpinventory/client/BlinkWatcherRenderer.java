package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.entity.BlinkWatcherEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BlinkWatcherRenderer extends MobRenderer<BlinkWatcherEntity, ZombieModel<BlinkWatcherEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/zombie/zombie.png");

    public BlinkWatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BlinkWatcherEntity entity) {
        return TEXTURE;
    }
}
