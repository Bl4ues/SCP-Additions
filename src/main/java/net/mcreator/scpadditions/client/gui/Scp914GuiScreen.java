package net.mcreator.scpadditions.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.scpadditions.world.inventory.Scp914GuiMenu;
import net.mcreator.scpadditions.network.Scp914GuiButtonMessage;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class Scp914GuiScreen extends AbstractContainerScreen<Scp914GuiMenu> {
	private final static HashMap<String, Object> guistate = Scp914GuiMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	Button button_rough;
	Button button_fine;
	Button button_coarse;
	Button button_very_fine;
	Button button_11;

	public Scp914GuiScreen(Scp914GuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_rough = Button.builder(Component.translatable("gui.scp_additions.scp_914_gui.button_rough"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiButtonMessage(0, x, y, z));
				Scp914GuiButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + -91, this.topPos + -4, 51, 20).build();
		guistate.put("button:button_rough", button_rough);
		this.addRenderableWidget(button_rough);
		button_fine = Button.builder(Component.translatable("gui.scp_additions.scp_914_gui.button_fine"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiButtonMessage(1, x, y, z));
				Scp914GuiButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}).bounds(this.leftPos + 17, this.topPos + -40, 46, 20).build();
		guistate.put("button:button_fine", button_fine);
		this.addRenderableWidget(button_fine);
		button_coarse = Button.builder(Component.translatable("gui.scp_additions.scp_914_gui.button_coarse"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiButtonMessage(2, x, y, z));
				Scp914GuiButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}).bounds(this.leftPos + -64, this.topPos + -40, 46, 20).build();
		guistate.put("button:button_coarse", button_coarse);
		this.addRenderableWidget(button_coarse);
		button_very_fine = Button.builder(Component.translatable("gui.scp_additions.scp_914_gui.button_very_fine"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiButtonMessage(3, x, y, z));
				Scp914GuiButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}).bounds(this.leftPos + 35, this.topPos + -4, 51, 20).build();
		guistate.put("button:button_very_fine", button_very_fine);
		this.addRenderableWidget(button_very_fine);
		button_11 = Button.builder(Component.translatable("gui.scp_additions.scp_914_gui.button_11"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiButtonMessage(4, x, y, z));
				Scp914GuiButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}).bounds(this.leftPos + -21, this.topPos + -74, 40, 20).build();
		guistate.put("button:button_11", button_11);
		this.addRenderableWidget(button_11);
	}
}
