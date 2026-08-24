package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.Scp1176BlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class Scp1176ItemGeoModel extends GeoModel<Scp1176BlockItem> {
    static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/block/scp1176.geo.json");
    static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/block/scp1176.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/scp1176.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp1176BlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Scp1176BlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp1176BlockItem animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(Scp1176BlockItem animatable,
            long instanceId, AnimationState<Scp1176BlockItem> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        showSolidGeometry();
    }

    void showSolidGeometry() {
        setBoneVisible("bb_main", false);
        setBoneVisible("sarc", true);
        setBoneVisible("1176", true);
        setBoneVisible("lid", true);
        setBoneVisible("2", true);
        setBoneVisible("faucet", true);
        setBoneVisible("bone", true);
    }

    void showHoneyGeometry() {
        setBoneVisible("bb_main", true);
        setBoneVisible("sarc", false);
        setBoneVisible("1176", false);
        setBoneVisible("lid", false);
        setBoneVisible("2", false);
        setBoneVisible("faucet", false);
        setBoneVisible("bone", false);
    }

    private void setBoneVisible(String name, boolean visible) {
        CoreGeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) bone.setHidden(!visible);
    }
}
