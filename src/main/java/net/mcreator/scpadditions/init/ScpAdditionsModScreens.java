
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.scpadditions.init;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.gui.screens.MenuScreens;

import net.mcreator.scpadditions.client.gui.TeslaTerminalScreen;
import net.mcreator.scpadditions.client.gui.Scp914GuiScreen;
import net.mcreator.scpadditions.client.gui.Scp294GuiScreen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScpAdditionsModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(ScpAdditionsModMenus.TESLA_TERMINAL.get(), TeslaTerminalScreen::new);
			MenuScreens.register(ScpAdditionsModMenus.SCP_914_GUI.get(), Scp914GuiScreen::new);
			MenuScreens.register(ScpAdditionsModMenus.SCP_294_GUI.get(), Scp294GuiScreen::new);
		});
	}
}
