
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.bl4ues.scpclassifieddirective.init;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;

import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import com.bl4ues.scpclassifieddirective.client.gui.Scp294GuiScreen;
import com.bl4ues.scpclassifieddirective.client.gui.PlayerCorpseScreen;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScpClassifiedDirectiveModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(ScpClassifiedDirectiveModMenus.TESLA_TERMINAL.get(), TeslaTerminalScreen::new);
			MenuScreens.register(ScpClassifiedDirectiveModMenus.SCP_294_GUI.get(), Scp294GuiScreen::new);
			MenuScreens.register(ScpClassifiedDirectiveModMenus.PLAYER_CORPSE.get(), PlayerCorpseScreen::new);

			ItemBlockRenderTypes.setRenderLayer(ScpClassifiedDirectiveModBlocks.TESLA_GATE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ScpClassifiedDirectiveModBlocks.TESLA_RECHARGE.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_2.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_3.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_4.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(Scp714Items.SCP_714_PLACED.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(CoreRoomElevatorModule.FLOOR.get(), RenderType.translucent());
		});
	}
}
