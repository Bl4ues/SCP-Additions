package net.mcreator.scpadditions.handler;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.data.Scp294ActionExecutor;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp294DrinkHandler {
	private Scp294DrinkHandler() {
	}

	@SubscribeEvent
	public static void onDrinkFinished(LivingEntityUseItemEvent.Finish event) {
		ItemStack stack = event.getItem();
		if (!stack.hasTag() || !stack.getTag().contains("Scp294Drink", Tag.TAG_COMPOUND)) {
			return;
		}

		LivingEntity entity = event.getEntity();
		CompoundTag drinkTag = stack.getTag().getCompound("Scp294Drink");
		showActionbar(entity, drinkTag);
		applyConfiguredEffects(entity, drinkTag);
		executeDrinkActions(entity, drinkTag);
	}

	private static void showActionbar(LivingEntity entity, CompoundTag drinkTag) {
		if (!(entity instanceof Player player) || !drinkTag.contains("actionbar", Tag.TAG_STRING)) {
			return;
		}

		String message = drinkTag.getString("actionbar");
		if (!message.isBlank()) {
			player.displayClientMessage(Component.literal(message), true);
		}
	}

	private static void applyConfiguredEffects(LivingEntity entity, CompoundTag drinkTag) {
		if (!drinkTag.contains("effects", Tag.TAG_LIST)) {
			return;
		}

		ListTag effects = drinkTag.getList("effects", Tag.TAG_COMPOUND);
		for (int i = 0; i < effects.size(); i++) {
			CompoundTag effectTag = effects.getCompound(i);
			MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(new ResourceLocation(effectTag.getString("id")));
			if (effect == null) {
				continue;
			}

			entity.addEffect(new MobEffectInstance(
					effect,
					Math.max(1, effectTag.getInt("duration")),
					Math.max(0, effectTag.getInt("amplifier")),
					effectTag.getBoolean("ambient"),
					!effectTag.contains("visible", Tag.TAG_BYTE) || effectTag.getBoolean("visible"),
					!effectTag.contains("show_icon", Tag.TAG_BYTE) || effectTag.getBoolean("show_icon")));
		}
	}

	private static void executeDrinkActions(LivingEntity entity, CompoundTag drinkTag) {
		if (!drinkTag.contains("drink_actions", Tag.TAG_LIST)) {
			return;
		}
		ListTag actions = drinkTag.getList("drink_actions", Tag.TAG_COMPOUND);
		Scp294ActionExecutor.executeActions(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, actions);
	}
}