package net.mcreator.scpadditions.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.scpadditions.world.inventory.Scp294GuiMenu;
import net.mcreator.scpadditions.network.Scp294GuiButtonMessage;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class Scp294GuiScreen extends AbstractContainerScreen<Scp294GuiMenu> {
	private final static HashMap<String, Object> guistate = Scp294GuiMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	EditBox scp294input;
	Button button_enter;

	public Scp294GuiScreen(Scp294GuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	private static final ResourceLocation texture = new ResourceLocation("scp_additions:textures/screens/scp_294_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		scp294input.render(guiGraphics, mouseX, mouseY, partialTicks);
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
		if (scp294input.isFocused())
			return scp294input.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		scp294input.tick();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.scp_additions.scp_294_gui.label_scp294"), 68, 8, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.scp_additions.scp_294_gui.label_coin"), 139, 12, -12829636, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		scp294input = new EditBox(this.font, this.leftPos + 16, this.topPos + 26, 118, 18, Component.translatable("gui.scp_additions.scp_294_gui.scp294input")) {
			@Override
			public void insertText(String text) {
				super.insertText(text);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.scp_additions.scp_294_gui.scp294input").getString());
				else
					setSuggestion(null);
			}

			@Override
			public void moveCursorTo(int pos) {
				super.moveCursorTo(pos);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.scp_additions.scp_294_gui.scp294input").getString());
				else
					setSuggestion(null);
			}
		};
		scp294input.setSuggestion(Component.translatable("gui.scp_additions.scp_294_gui.scp294input").getString());
		scp294input.setMaxLength(32767);
		guistate.put("text:scp294input", scp294input);
		this.addWidget(this.scp294input);
		button_enter = Button.builder(Component.translatable("gui.scp_additions.scp_294_gui.button_enter"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp294GuiButtonMessage(0, x, y, z));
				Scp294GuiButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 108, this.topPos + 48, 51, 20).build();
		guistate.put("button:button_enter", button_enter);
		this.addRenderableWidget(button_enter);
	}
}
