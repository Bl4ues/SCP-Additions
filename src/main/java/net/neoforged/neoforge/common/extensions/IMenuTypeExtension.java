package net.neoforged.neoforge.common.extensions;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.mcreator.scpadditions.fabric.menu.LegacyMenuData;

public final class IMenuTypeExtension {
    private IMenuTypeExtension() {}

    @FunctionalInterface
    public interface Factory<T extends AbstractContainerMenu> {
        T create(int syncId, Inventory inventory, FriendlyByteBuf data);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> create(Factory<T> factory) {
        return new ExtendedScreenHandlerType<>(
                (syncId, inventory, data) -> factory.create(syncId, inventory, data.toBuffer()),
                LegacyMenuData.STREAM_CODEC);
    }
}
