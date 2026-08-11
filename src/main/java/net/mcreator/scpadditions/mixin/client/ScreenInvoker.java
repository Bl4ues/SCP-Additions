package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Mapping-safe access to Screen methods whose runtime names differ in jars. */
@Mixin(Screen.class)
public interface ScreenInvoker {
    @Invoker("init")
    void scpAdditions$invokeInit(Minecraft minecraft, int width, int height);
}
