package net.mcreator.scpadditions.client.gui;

import net.minecraft.world.item.ItemStack;
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
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
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
	Button button_manual_override;
	Button button_log_out;

	public TeslaTerminalScreen(TeslaTerminalMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 220;
		this.imageHeight = 150;
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
		guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xE0202428);
		guiGraphics.fill(this.leftPos + 6, this.topPos + 18, this.leftPos + this.imageWidth - 6, this.topPos + 20, 0xFF3A4048);
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
		if (button_tesla_gate_on != null) {
			button_tesla_gate_on.active = hasCredentials();
		}
		if (button_tesla_gate_off != null) {
			button_tesla_gate_off.active = hasCredentials();
		}
		if (button_manual_override != null) {
			button_manual_override.active = hasCredentials();
			button_manual_override.setMessage(Component.literal(isManualOverride() ? "Disengage Override" : "Engage Override"));
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.literal("MONITORING STATION"), 10, 7, 0xD6DCE2, false);
		guiGraphics.drawString(this.font, Component.literal("Elevated Permissions: " + (hasCredentials() ? "GRANTED" : "DENIED")), 10, 28, hasCredentials() ? 0x77DD77 : 0xFF7777, false);
		guiGraphics.drawString(this.font, Component.literal("Tesla Gates: " + (areTeslaGatesEnabled() ? "ENABLED" : "DISABLED")), 10, 43, areTeslaGatesEnabled() ? 0x77DD77 : 0xFFAA66, false);
		guiGraphics.drawString(this.font, Component.literal("Manual Override: " + (isManualOverride() ? "ENGAGED" : "STANDBY")), 10, 58, isManualOverride() ? 0xFF7777 : 0x9AA4AA, false);
		if (!hasCredentials()) {
			guiGraphics.drawString(this.font, Component.literal("Insert Security Credentials to configure gates."), 10, 75, 0xFFCC77, false);
		}
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_tesla_gate_on = Button.builder(Component.literal("Enable Tesla Gates"), e -> {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(0, x, y, z));
		}).bounds(this.leftPos + 14, this.topPos + 91, 92, 20).build();
		guistate.put("button:button_tesla_gate_on", button_tesla_gate_on);
		this.addRenderableWidget(button_tesla_gate_on);

		button_tesla_gate_off = Button.builder(Component.literal("Disable Tesla Gates"), e -> {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(1, x, y, z));
		}).bounds(this.leftPos + 114, this.topPos + 91, 92, 20).build();
		guistate.put("button:button_tesla_gate_off", button_tesla_gate_off);
		this.addRenderableWidget(button_tesla_gate_off);

		button_manual_override = Button.builder(Component.literal("Engage Override"), e -> {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(3, x, y, z));
		}).bounds(this.leftPos + 14, this.topPos + 115, 126, 20).build();
		guistate.put("button:button_manual_override", button_manual_override);
		this.addRenderableWidget(button_manual_override);

		button_log_out = Button.builder(Component.literal("Log Out"), e -> {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(2, x, y, z));
			this.minecraft.player.closeContainer();
		}).bounds(this.leftPos + 146, this.topPos + 115, 60, 20).build();
		guistate.put("button:button_log_out", button_log_out);
		this.addRenderableWidget(button_log_out);
	}

	private boolean hasCredentials() {
		return entity != null && entity.getInventory().contains(new ItemStack(ScpAdditionsModItems.SECURITY_CREDENTIALS.get()));
	}

	private boolean areTeslaGatesEnabled() {
		return world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
	}

	private boolean isManualOverride() {
		return world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
	}
}
