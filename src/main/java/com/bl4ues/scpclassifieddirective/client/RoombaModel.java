package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.entity.RoombaEntity;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib resource binding for the facility Roomba. */
public final class RoombaModel extends GeoModel<RoombaEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "scp_classified_directive", "geo/entity/roomba.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            "scp_classified_directive", "animations/entity/roomba.animation.json");
    private static final ResourceLocation[] TEXTURE_CANDIDATES = {
            new ResourceLocation("scp_classified_directive",
                    "textures/entities/roomba.png"),
            new ResourceLocation("scp_classified_directive",
                    "textures/entity/roomba.png"),
            new ResourceLocation("scp_classified_directive",
                    "textures/roomba.png"),
            new ResourceLocation("scp_classified_directive",
                    "textures/entities/roomba.png")
    };
    private static final ResourceLocation FALLBACK_TEXTURE =
            new ResourceLocation("minecraft",
                    "textures/block/gray_concrete.png");

    @Override
    public ResourceLocation getModelResource(RoombaEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RoombaEntity animatable) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            for (ResourceLocation candidate : TEXTURE_CANDIDATES) {
                if (minecraft.getResourceManager().getResource(candidate)
                        .isPresent()) {
                    return candidate;
                }
            }
        }
        return FALLBACK_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RoombaEntity animatable) {
        return ANIMATION;
    }
}
