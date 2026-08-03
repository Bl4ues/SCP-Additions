package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.entity.RoombaEntity;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib resource binding for the facility Roomba. */
public final class RoombaModel extends GeoModel<RoombaEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "scp_additions", "geo/entity/roomba.geo.json");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            "scp_additions", "animations/entity/roomba.animation.json");
    private static final ResourceLocation[] TEXTURE_CANDIDATES = {
            new ResourceLocation("scp_additions",
                    "textures/entities/roomba.png"),
            new ResourceLocation("scp_additions",
                    "textures/entity/roomba.png"),
            new ResourceLocation("scp_additions",
                    "textures/roomba.png"),
            new ResourceLocation("scpinventory",
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
