package net.fabricmc.fabric.api.client.screen.v1;
import net.minecraft.client.gui.components.events.GuiEventListener;
public interface ScreensAccessor {
    void fabric$add(GuiEventListener listener);
    void fabric$remove(GuiEventListener listener);
}
