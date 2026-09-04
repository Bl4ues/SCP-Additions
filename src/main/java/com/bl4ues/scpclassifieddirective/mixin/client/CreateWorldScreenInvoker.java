package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Mapping-safe access to vanilla world-creation actions used by the New Game shell. */
@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenInvoker {
    @Invoker("onCreate")
    void scpclassifieddirective$invokeCreate();

    @Invoker("openExperimentsScreen")
    void scpclassifieddirective$invokeOpenExperimentsScreen(
            WorldDataConfiguration configuration);

    @Invoker("openDataPackSelectionScreen")
    void scpclassifieddirective$invokeOpenDataPackSelectionScreen(
            WorldDataConfiguration configuration);
}
