package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class InventoryPdaGeoModel extends GeoModel<InventoryPdaAnimatable> {
    private boolean thirdPerson;
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/gui/inventory_pda.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/entity/inventory_pda.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/gui/inventory_pda.animation.json");

    @Override
    public ResourceLocation getModelResource(InventoryPdaAnimatable animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(InventoryPdaAnimatable animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(InventoryPdaAnimatable animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(InventoryPdaAnimatable animatable,
            long instanceId, AnimationState<InventoryPdaAnimatable> state) {
        super.setCustomAnimations(animatable, instanceId, state);
        // The GUI is drawn as a full-bright dynamic surface over this opening.
        setHidden("screen_surface", true);
        // Keep the authored unpowered display below the live surface. Besides
        // being the correct third-person appearance, it prevents a missing or
        // not-yet-captured GUI frame from turning into a hole in the model.
        setHidden("screen_back", false);
    }

    void setThirdPerson(boolean thirdPerson) {
        this.thirdPerson = thirdPerson;
    }

    private void setHidden(String boneName, boolean hidden) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone != null) bone.setHidden(hidden);
    }
}
