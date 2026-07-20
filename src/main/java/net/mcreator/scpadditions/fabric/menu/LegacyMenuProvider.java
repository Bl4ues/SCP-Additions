package net.mcreator.scpadditions.fabric.menu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Objects;
import java.util.function.Supplier;

public final class LegacyMenuProvider implements ExtendedScreenHandlerFactory<LegacyMenuData> {
    @FunctionalInterface
    public interface Factory {
        AbstractContainerMenu create(int id, Inventory inventory, Player player, LegacyMenuData data);
    }

    private final Component title;
    private final Factory factory;
    private final Supplier<LegacyMenuData> openingData;

    public LegacyMenuProvider(Component title, Factory factory, Supplier<LegacyMenuData> openingData) {
        this.title = Objects.requireNonNull(title);
        this.factory = Objects.requireNonNull(factory);
        this.openingData = Objects.requireNonNull(openingData);
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return factory.create(id, inventory, player, openingData.get());
    }

    @Override
    public LegacyMenuData getScreenOpeningData(ServerPlayer player) {
        return openingData.get();
    }
}
