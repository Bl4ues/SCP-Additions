package net.mcreator.scpadditions.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class NamedSpawnEggItem extends DeferredSpawnEggItem {
    private final Component displayName;
    private final Component subtitle;

    public NamedSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor,
            Item.Properties properties, String displayName, String subtitleTranslationKey) {
        super(type, backgroundColor, highlightColor, properties);
        this.displayName = Component.literal(displayName);
        this.subtitle = Component.translatable(subtitleTranslationKey);
    }

    @Override
    public Component getName(ItemStack stack) {
        return displayName;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(subtitle);
    }
}
