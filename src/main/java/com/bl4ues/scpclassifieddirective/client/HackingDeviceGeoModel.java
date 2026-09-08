package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class HackingDeviceGeoModel extends GeoModel<HackingDeviceItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/item/hacking_device.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/item/hacking_device.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/item/hacking_device.animation.json");

    @Override
    public ResourceLocation getModelResource(HackingDeviceItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HackingDeviceItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HackingDeviceItem animatable) {
        return ANIMATION;
    }
}
