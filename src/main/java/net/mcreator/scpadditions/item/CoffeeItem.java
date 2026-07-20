package net.mcreator.scpadditions.item;

import com.bl4ues.scpadditions.compat.LegacyItemTags;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.util.List;

public class CoffeeItem extends Item {
	public CoffeeItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON).food((new FoodProperties.Builder()).nutrition(0).saturationModifier(0.0f).alwaysEdible().build()));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack itemstack, LivingEntity user) {
		return 32;
	}

	@Override
	public Component getName(ItemStack stack) {
		if (LegacyItemTags.hasTag(stack) && LegacyItemTags.getTag(stack).contains("Scp294Drink", Tag.TAG_COMPOUND)) {
			String id = LegacyItemTags.getTag(stack).getCompound("Scp294Drink").getString("id");
			if (!id.isBlank()) {
				String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
				while (path.startsWith("cup_of_")) {
					path = path.substring("cup_of_".length());
				}
				return Component.literal("Cup of " + toTitleCase(path.replace('_', ' ')));
			}
		}
		return Component.translatable(this.getDescriptionId(stack));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (LegacyItemTags.hasTag(stack) && LegacyItemTags.getTag(stack).contains("Scp294Drink", Tag.TAG_COMPOUND)) {
			CompoundTag drinkTag = LegacyItemTags.getTag(stack).getCompound("Scp294Drink");
			if (drinkTag.contains("drinkable", Tag.TAG_BYTE) && !drinkTag.getBoolean("drinkable")) {
				if (!world.isClientSide()) {
					String message = drinkTag.getString("refuse_message");
					if (!message.isBlank()) {
						player.displayClientMessage(Component.literal(message), true);
					}
				}
				return InteractionResultHolder.fail(stack);
			}
		}
		return ItemUtils.startUsingInstantly(world, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		super.finishUsingItem(itemstack, world, entity);
		ItemStack emptyCup = new ItemStack(ScpAdditionsModItems.EMPTY_CUP.get());

		if (entity instanceof Player player && !player.getAbilities().instabuild) {
			if (itemstack.isEmpty()) {
				return emptyCup;
			}
			if (!player.getInventory().add(emptyCup)) {
				player.drop(emptyCup, false);
			}
		}

		return itemstack;
	}

	private static String toTitleCase(String value) {
		StringBuilder result = new StringBuilder();
		for (String word : value.split(" ")) {
			if (word.isBlank()) {
				continue;
			}
			if (!result.isEmpty()) {
				result.append(' ');
			}
			result.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1) {
				result.append(word.substring(1));
			}
		}
		return result.toString();
	}
}