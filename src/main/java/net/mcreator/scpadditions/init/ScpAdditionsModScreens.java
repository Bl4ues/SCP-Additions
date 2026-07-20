
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.scpadditions.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;

import net.mcreator.scpadditions.client.gui.TeslaTerminalScreen;
import net.mcreator.scpadditions.client.gui.Scp914GuiScreen;
import net.mcreator.scpadditions.client.gui.Scp294GuiScreen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScpAdditionsModScreens {
	@SubscribeEvent
	public static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(ScpAdditionsModMenus.TESLA_TERMINAL.get(), TeslaTerminalScreen::new);
		event.register(ScpAdditionsModMenus.SCP_914_GUI.get(), Scp914GuiScreen::new);
		event.register(ScpAdditionsModMenus.SCP_294_GUI.get(), Scp294GuiScreen::new);
	}

	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_GATE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_RECHARGE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_ACTIVE.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_ACTIVE_2.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_ACTIVE_3.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_ACTIVE_4.get(), RenderType.cutout());
		});
	}
}
