package net.mcreator.scpadditions.fabric.mixin.client;

import net.fabricmc.fabric.api.client.screen.v1.ScreensAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Screen.class)
abstract class ScreenAccessorMixin implements ScreensAccessor {
    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry>
            T addRenderableWidget(T widget);

    @Shadow
    protected abstract void removeWidget(GuiEventListener listener);

    @Override
    public void fabric$add(GuiEventListener listener) {
        if (!(listener instanceof AbstractWidget widget)) {
            throw new IllegalArgumentException(
                    "SCP Additions can only add renderable screen widgets through the compatibility bridge");
        }
        addRenderableWidget(widget);
    }

    @Override
    public void fabric$remove(GuiEventListener listener) {
        removeWidget(listener);
    }
}
