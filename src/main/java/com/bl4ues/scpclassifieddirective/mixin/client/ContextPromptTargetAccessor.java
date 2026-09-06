package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the private ContextPromptClient target without generating ModifyArgs helpers. */
@Mixin(targets = "com.bl4ues.scpclassifieddirective.inventory.client.ContextPromptClient$ContextTarget")
public interface ContextPromptTargetAccessor {
    @Accessor("pos")
    BlockPos scpclassifieddirective$pos();

    @Accessor("interactionKey")
    String scpclassifieddirective$interactionKey();

    @Accessor("showAction")
    boolean scpclassifieddirective$showAction();

    @Accessor("showName")
    boolean scpclassifieddirective$showName();

    @Accessor("allowRightClick")
    boolean scpclassifieddirective$allowRightClick();
}
