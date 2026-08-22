package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.DocumentHolderBlockEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib resource binding for the placed Document Holder. */
public final class DocumentHolderGeoModel
        extends GeoModel<DocumentHolderBlockEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/block/document_holder.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/block/document_holder.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/document_holder.animation.json");

    @Override
    public ResourceLocation getModelResource(
            DocumentHolderBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(
            DocumentHolderBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(
            DocumentHolderBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(DocumentHolderBlockEntity animatable,
            long instanceId,
            AnimationState<DocumentHolderBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        prepareOpaquePass();
    }

    /**
     * The holder body and stored document are depth-writing geometry. The
     * glass door must not share that same translucent draw ordering or the
     * document can disappear when the camera angle changes.
     */
    public void prepareOpaquePass() {
        setHidden("bone", false);
        setHidden("document", false);
        setHidden("door", true);
    }

    public void prepareGlassPass() {
        setHidden("bone", true);
        setHidden("document", true);
        setHidden("door", false);
    }

    private void setHidden(String name, boolean hidden) {
        CoreGeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) bone.setHidden(hidden);
    }
}
