package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Mapping-safe access to the vanilla create action used by the New Game shell. */
@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenInvoker {
    @Invoker("onCreate")
    void scpclassifieddirective$invokeCreate();
}
