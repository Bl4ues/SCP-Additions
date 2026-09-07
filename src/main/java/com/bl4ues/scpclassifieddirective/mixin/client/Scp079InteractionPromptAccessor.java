package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor for filtering prompts that point back at the currently used camera. */
@Mixin(targets = "com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2$InteractionPrompt", remap = false)
public interface Scp079InteractionPromptAccessor {
    @Accessor("pos")
    BlockPos scpclassifieddirective$pos();
}
