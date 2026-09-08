package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accessor used to filter private SCP-079 world prompt targets by authored room. */
@Mixin(targets = "com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2$WorldTarget", remap = false)
public interface Scp079WorldTargetAccessor {
    @Accessor("kind")
    Object scpclassifieddirective$kind();

    @Accessor("pos")
    BlockPos scpclassifieddirective$pos();
}
