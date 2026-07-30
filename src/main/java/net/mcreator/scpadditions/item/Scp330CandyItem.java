package net.mcreator.scpadditions.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** The three rebuilt SCP-330 candies share food behavior and grant no buffs. */
public final class Scp330CandyItem extends Item {
    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(6)
            .saturationMod(0.65F)
            .alwaysEat()
            .build();

    public Scp330CandyItem() {
        super(new Item.Properties().stacksTo(64).food(FOOD));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 16;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level,
            LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide()) {
            entity.heal(1.0F);
            level.playSound(null, entity.blockPosition(),
                    ScpAdditionsModSounds.CANDYEAT.get(), SoundSource.PLAYERS,
                    0.8F, 0.95F + level.random.nextFloat() * 0.1F);
        }
        return result;
    }
}
