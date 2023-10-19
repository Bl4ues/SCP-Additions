
package net.mcreator.scpadditions.gui;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.World;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.Minecraft;

import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;

@OnlyIn(Dist.CLIENT)
public class Scp914GuiGuiWindow extends ContainerScreen<Scp914GuiGui.GuiContainerMod> {
	private World world;
	private int x, y, z;
	private PlayerEntity entity;
	private final static HashMap guistate = Scp914GuiGui.guistate;

	public Scp914GuiGuiWindow(Scp914GuiGui.GuiContainerMod container, PlayerInventory inventory, ITextComponent text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.xSize = 0;
		this.ySize = 0;
	}

	@Override
	public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(ms);
		super.render(ms, mouseX, mouseY, partialTicks);
		this.renderHoveredTooltip(ms, mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack ms, float partialTicks, int gx, int gy) {
		RenderSystem.color4f(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeScreen();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	public void tick() {
		super.tick();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack ms, int mouseX, int mouseY) {
	}

	@Override
	public void onClose() {
		super.onClose();
		Minecraft.getInstance().keyboardListener.enableRepeatEvents(false);
	}

	@Override
	public void init(Minecraft minecraft, int width, int height) {
		super.init(minecraft, width, height);
		minecraft.keyboardListener.enableRepeatEvents(true);
		this.addButton(new Button(this.guiLeft + -90, this.guiTop + -4, 51, 20, new StringTextComponent("Rough"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiGui.ButtonPressedMessage(0, x, y, z));
				Scp914GuiGui.handleButtonAction(entity, 0, x, y, z);
			}
		}));
		this.addButton(new Button(this.guiLeft + 17, this.guiTop + -40, 46, 20, new StringTextComponent("Fine"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiGui.ButtonPressedMessage(1, x, y, z));
				Scp914GuiGui.handleButtonAction(entity, 1, x, y, z);
			}
		}));
		this.addButton(new Button(this.guiLeft + -63, this.guiTop + -40, 46, 20, new StringTextComponent("Coarse"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiGui.ButtonPressedMessage(2, x, y, z));
				Scp914GuiGui.handleButtonAction(entity, 2, x, y, z);
			}
		}));
		this.addButton(new Button(this.guiLeft + 35, this.guiTop + -4, 51, 20, new StringTextComponent("Very Fine"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiGui.ButtonPressedMessage(3, x, y, z));
				Scp914GuiGui.handleButtonAction(entity, 3, x, y, z);
			}
		}));
		this.addButton(new Button(this.guiLeft + -20, this.guiTop + -74, 40, 20, new StringTextComponent("1:1"), e -> {
			if (true) {
				ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiGui.ButtonPressedMessage(4, x, y, z));
				Scp914GuiGui.handleButtonAction(entity, 4, x, y, z);
			}
		}));
	}
}
