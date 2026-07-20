package net.neoforged.neoforge.items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public final class ItemHandlerHelper {
    private ItemHandlerHelper() {}
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy) && !copy.isEmpty()) player.drop(copy, false);
    }
}
