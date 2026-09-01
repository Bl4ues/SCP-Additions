package com.bl4ues.scpclassifieddirective.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the pickup outline renderer temporarily route outline geometry to its own mask. */
@Mixin(LevelRenderer.class)
public interface LevelRendererEntityTargetAccessor {
    @Accessor("entityTarget")
    RenderTarget scpclassifieddirective$getEntityTarget();

    @Accessor("entityTarget")
    void scpclassifieddirective$setEntityTarget(RenderTarget target);
}
