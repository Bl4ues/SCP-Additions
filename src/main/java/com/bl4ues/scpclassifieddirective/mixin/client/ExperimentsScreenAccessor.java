package com.bl4ues.scpclassifieddirective.mixin.client;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.client.gui.screens.worldselection.ExperimentsScreen;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Access to vanilla experiment state while keeping its apply pipeline authoritative. */
@Mixin(ExperimentsScreen.class)
public interface ExperimentsScreenAccessor {
    @Accessor("packs")
    Object2BooleanMap<Pack> scpclassifieddirective$getPacks();

    @Invoker("onDone")
    void scpclassifieddirective$invokeOnDone();
}
