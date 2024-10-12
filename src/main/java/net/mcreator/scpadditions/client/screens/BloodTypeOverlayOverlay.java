
package net.mcreator.scpadditions.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.Minecraft;

import net.mcreator.scpadditions.procedures.BloodTypeOverlayDisplayOverlayIngameProcedure;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class BloodTypeOverlayOverlay {
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void eventHandler(ScreenEvent.Render.Post event) {
		if (event.getScreen() instanceof InventoryScreen) {
			int w = event.getScreen().width;
			int h = event.getScreen().height;
			Level world = null;
			double x = 0;
			double y = 0;
			double z = 0;
			Player entity = Minecraft.getInstance().player;
			if (entity != null) {
				world = entity.level();
				x = entity.getX();
				y = entity.getY();
				z = entity.getZ();
			}
			if (BloodTypeOverlayDisplayOverlayIngameProcedure.execute(entity)) {
				if (BloodTypeOverlayDisplayOverlayIngameProcedure.execute(entity))
					event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("gui.scp_additions.blood_type_overlay.label_blood_type_o"), w / 2 + -208, h / 2 + 105, -65536, false);
			}
		}
	}
}
