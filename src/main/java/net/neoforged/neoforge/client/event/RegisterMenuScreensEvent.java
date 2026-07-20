package net.neoforged.neoforge.client.event;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.Event;
public final class RegisterMenuScreensEvent extends Event {
    public <M extends AbstractContainerMenu, U extends AbstractContainerScreen<M> & MenuAccess<M>>
    void register(MenuType<? extends M> type, MenuScreens.ScreenConstructor<M,U> factory) {
        MenuScreens.register(type, factory);
    }
}
