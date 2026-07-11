package net.mcreator.scpadditions.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.function.Supplier;

public class NamedSpawnEggItem extends ForgeSpawnEggItem {
    private final Component displayName;

    public NamedSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor,
            Item.Properties properties, String displayName) {
        super(type, backgroundColor, highlightColor, properties);
        this.displayName = Component.literal(displayName);
    }

    @Override
    public Component getName(ItemStack stack) {
        return displayName;
    }
}
