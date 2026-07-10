package net.mcreator.scpadditions.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.Scp294GuiButtonMessage;
import net.mcreator.scpadditions.world.inventory.Scp294GuiMenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

public class Scp294GuiScreen extends AbstractContainerScreen<Scp294GuiMenu> {
	private static final int TEX_W = 600;
	private static final int TEX_H = 476;
	private static final ResourceLocation BACKGROUND = new ResourceLocation("scp_additions:textures/screens/scp_294_gui.png");
	private static final ResourceLocation COIN_OFF = new ResourceLocation("scp_additions:textures/screens/scp_294_coin_off.png");
	private static final ResourceLocation COIN_ON = new ResourceLocation("scp_additions:textures/screens/scp_294_coin_on.png");
	private static final ResourceLocation SCREEN_OVERLAY = new ResourceLocation("scp_additions:textures/screens/scp_294_screen_overlay.png");
	private static final double[][] INPUT_POLY = {{84, 285}, {399, 286}, {408, 50}, {73, 52}};
	private static final double[][] COIN_POLY = {{464, 388}, {564, 390}, {579, 227}, {472, 228}};
	private static final double[][] ENTER_POLY = {{110, 442}, {376, 440}, {364, 330}, {115, 330}};

	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private String order = "";
	private boolean inputFocused = true;
	private double guiScale = 1.0D;

	public Scp294GuiScreen(Scp294GuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = TEX_W;
		this.imageHeight = TEX_H;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		updateLayout();
		this.renderBackground(guiGraphics);
		renderPanel(guiGraphics);
		renderHoverTooltip(guiGraphics, mouseX, mouseY);
	}

	private void renderPanel(GuiGraphics guiGraphics) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(this.leftPos, this.topPos, 0);
		guiGraphics.pose().scale((float) guiScale, (float) guiScale, 1.0F);

		RenderSystem.setShaderColor(0.95F, 0.95F, 0.95F, 1.0F);
		guiGraphics.blit(BACKGROUND, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

		RenderSystem.setShaderColor(1, 1, 1, 1);
		guiGraphics.blit(hasCoinInserted() ? COIN_ON : COIN_OFF, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.55F);
		guiGraphics.blit(SCREEN_OVERLAY, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		renderOrderText(guiGraphics);

		guiGraphics.pose().popPose();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private void renderOrderText(GuiGraphics guiGraphics) {
		String visible = order.isBlank() && !inputFocused ? "ENTER YOUR ORDER" : order;
		if (visible.length() > 26) {
			visible = visible.substring(Math.max(0, visible.length() - 26));
		}
		int color = inputFocused ? 0x07111B : 0x1B2A35;
		String cursor = inputFocused && (System.currentTimeMillis() / 400L) % 2L == 0L ? "_" : "";
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(112, 178, 0);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(0.15F));
		guiGraphics.pose().scale(1.9F, 1.9F, 1.0F);
		guiGraphics.drawString(this.font, visible + cursor, 0, 0, color, false);
		guiGraphics.pose().popPose();
	}

	private void renderHoverTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		double tx = textureX(mouseX);
		double ty = textureY(mouseY);
		Component tooltip = null;
		if (contains(COIN_POLY, tx, ty)) {
			tooltip = Component.literal("Coin");
		} else if (contains(INPUT_POLY, tx, ty)) {
			tooltip = Component.literal("Input");
		} else if (contains(ENTER_POLY, tx, ty)) {
			tooltip = Component.literal("Enter");
		}
		if (tooltip != null) {
			guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return true;
		}
		double tx = textureX(mouseX);
		double ty = textureY(mouseY);
		if (contains(COIN_POLY, tx, ty)) {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp294GuiButtonMessage(1, x, y, z, ""));
			inputFocused = true;
			return true;
		}
		if (contains(INPUT_POLY, tx, ty)) {
			inputFocused = true;
			return true;
		}
		if (contains(ENTER_POLY, tx, ty)) {
			sendDrinkRequestAndClose();
			return true;
		}
		inputFocused = true;
		return true;
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		inputFocused = true;
		if (isAllowedInputCharacter(codePoint) && order.length() < 80) {
			order += codePoint;
			return true;
		}
		return true;
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		if (key == 257 || key == 335) {
			sendDrinkRequestAndClose();
			return true;
		}
		if (key == 259 && !order.isEmpty()) {
			inputFocused = true;
			order = order.substring(0, order.length() - 1);
			return true;
		}
		if (isPaste(key, c)) {
			inputFocused = true;
			String clipboard = this.minecraft.keyboardHandler.getClipboard();
			for (char ch : clipboard.toCharArray()) {
				if (isAllowedInputCharacter(ch) && order.length() < 80) {
					order += ch;
				}
			}
			return true;
		}
		return true;
	}

	private boolean isPaste(int key, int modifiers) {
		return key == 86 && hasControlDown();
	}

	private static boolean isAllowedInputCharacter(char character) {
		return character >= 32 && character != 127;
	}

	private void sendDrinkRequestAndClose() {
		ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp294GuiButtonMessage(0, x, y, z, order));
		this.minecraft.player.closeContainer();
	}

	private boolean hasCoinInserted() {
		Slot slot = this.menu.get().get(0);
		return slot != null && slot.hasItem();
	}

	private void updateLayout() {
		this.guiScale = Math.min(1.0D, Math.min((this.width - 20.0D) / TEX_W, (this.height - 20.0D) / TEX_H));
		this.leftPos = (int) Math.round((this.width - TEX_W * guiScale) / 2.0D);
		this.topPos = (int) Math.round((this.height - TEX_H * guiScale) / 2.0D);
	}

	private double textureX(double mouseX) {
		return (mouseX - this.leftPos) / guiScale;
	}

	private double textureY(double mouseY) {
		return (mouseY - this.topPos) / guiScale;
	}

	private static boolean contains(double[][] polygon, double x, double y) {
		boolean inside = false;
		for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
			double xi = polygon[i][0], yi = polygon[i][1];
			double xj = polygon[j][0], yj = polygon[j][1];
			boolean intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / ((yj - yi) == 0 ? 0.00001D : (yj - yi)) + xi);
			if (intersect) inside = !inside;
		}
		return inside;
	}
}