package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.item.SystemTerminalItem;
import software.bernie.geckolib.model.GeoModel;

/** Item resource binding for the SCiPNET terminal. */
public final class SystemTerminalItemGeoModel
        extends GeoModel<SystemTerminalItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "scp_classified_directive", "geo/block/system_terminal.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "scp_classified_directive", "textures/block/system_terminal.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            "scp_classified_directive",
            "animations/block/system_terminal.animation.json");

    @Override
    public ResourceLocation getModelResource(SystemTerminalItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SystemTerminalItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(
            SystemTerminalItem animatable) {
        return ANIMATION;
    }
}
