package net.mcreator.scpadditions.fabric;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
public final class FabricItemAbilities {
    private FabricItemAbilities() {}
    public static boolean isPickaxe(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES) || stack.has(DataComponents.TOOL);
    }
}
