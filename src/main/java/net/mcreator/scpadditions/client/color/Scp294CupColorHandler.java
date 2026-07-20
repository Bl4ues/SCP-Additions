package net.mcreator.scpadditions.client.color;

import com.bl4ues.scpadditions.compat.LegacyItemTags;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import net.minecraft.nbt.Tag;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp294CupColorHandler {
	private Scp294CupColorHandler() {
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> {
			if (tintIndex != 1) {
				return 0xFFFFFF;
			}
			if (LegacyItemTags.hasTag(stack) && LegacyItemTags.getTag(stack).contains("Scp294Drink", Tag.TAG_COMPOUND)) {
				return LegacyItemTags.getTag(stack).getCompound("Scp294Drink").getInt("cup_color") & 0xFFFFFF;
			}
			return 0xFFFFFF;
		}, ScpAdditionsModItems.CUP_OF_COFFEE.get());
	}
}