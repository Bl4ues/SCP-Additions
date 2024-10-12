package net.mcreator.scpadditions.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.scpadditions.world.inventory.TeslaTerminalMenu;
import net.mcreator.scpadditions.network.TeslaTerminalButtonMessage;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class TeslaTerminalScreen extends AbstractContainerScreen<TeslaTerminalMenu> {
	private final static HashMap<String, Object> guistate = TeslaTerminalMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	Button button_tesla_gate_on;
	Button button_tesla_gate_off;
	Button button_log_out;

	public TeslaTerminalScreen(TeslaTerminalMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 135;
		this.imageHeight = 85;
	}

	private static final ResourceLocation texture = new ResourceLocation("scp_additions:textures/screens/tesla_terminal.png");

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
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.scp_additions.tesla_terminal.label_tesla_gate_terminal"), 18, 6, -12829636, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_tesla_gate_on = Button.builder(Component.translatable("gui.scp_additions.tesla_terminal.button_tesla_gate_on"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(0, x, y, z));
				TeslaTerminalButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 22, this.topPos + 20, 92, 20).build();
		guistate.put("button:button_tesla_gate_on", button_tesla_gate_on);
		this.addRenderableWidget(button_tesla_gate_on);
		button_tesla_gate_off = Button.builder(Component.translatable("gui.scp_additions.tesla_terminal.button_tesla_gate_off"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(1, x, y, z));
				TeslaTerminalButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}).bounds(this.leftPos + 19, this.topPos + 41, 98, 20).build();
		guistate.put("button:button_tesla_gate_off", button_tesla_gate_off);
		this.addRenderableWidget(button_tesla_gate_off);
		button_log_out = Button.builder(Component.translatable("gui.scp_additions.tesla_terminal.button_log_out"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(2, x, y, z));
				TeslaTerminalButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}).bounds(this.leftPos + 38, this.topPos + 62, 61, 20).build();
		guistate.put("button:button_log_out", button_log_out);
		this.addRenderableWidget(button_log_out);
	}
}
