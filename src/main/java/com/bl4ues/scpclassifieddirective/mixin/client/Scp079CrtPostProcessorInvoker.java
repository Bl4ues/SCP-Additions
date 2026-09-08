package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CrtPostProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the final CRT pass after modal no-signal content is drawn. */
@Mixin(value = Scp079CrtPostProcessor.class, remap = false)
public interface Scp079CrtPostProcessorInvoker {
    @Invoker("apply")
    static void scpclassifieddirective$apply(Minecraft minecraft,
            GuiGraphics graphics, float cameraTint, boolean tintOnly) {
        throw new AssertionError();
    }
}
