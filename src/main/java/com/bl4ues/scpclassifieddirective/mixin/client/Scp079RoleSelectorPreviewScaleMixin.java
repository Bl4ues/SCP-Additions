package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.ScpRoleSelectorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keeps the SCP-079 card image close without clipping the authored computer. */
@Mixin(ScpRoleSelectorScreen.class)
public abstract class Scp079RoleSelectorPreviewScaleMixin {
    @ModifyConstant(method = "<clinit>",
            constant = @Constant(floatValue = 2.28F), require = 1)
    private static float scpClassifiedDirective$rebalancePreview(float original) {
        return 1.30F;
    }
}
