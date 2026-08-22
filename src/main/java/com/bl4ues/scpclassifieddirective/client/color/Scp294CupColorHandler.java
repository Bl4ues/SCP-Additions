package com.bl4ues.scpclassifieddirective.client.color;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.nbt.Tag;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp294CupColorHandler {
	private Scp294CupColorHandler() {
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> {
			if (tintIndex != 1) {
				return 0xFFFFFF;
			}
			if (stack.hasTag() && stack.getTag().contains("Scp294Drink", Tag.TAG_COMPOUND)) {
				return stack.getTag().getCompound("Scp294Drink").getInt("cup_color") & 0xFFFFFF;
			}
			return 0xFFFFFF;
		}, ScpClassifiedDirectiveModItems.CUP_OF_COFFEE.get());
	}
}