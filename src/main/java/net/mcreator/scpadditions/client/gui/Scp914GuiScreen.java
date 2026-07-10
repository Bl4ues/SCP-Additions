package net.mcreator.scpadditions.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.network.Scp914GuiButtonMessage;
import net.mcreator.scpadditions.world.inventory.Scp914GuiMenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

public class Scp914GuiScreen extends AbstractContainerScreen<Scp914GuiMenu> {
	private static final int TEX_W = 1126;
	private static final int TEX_H = 876;
	private static final int DIAL_PIVOT_X = 567;
	private static final int DIAL_PIVOT_Y = 486;
	private static final double MIN_DIAL_ANGLE = -87.0D;
	private static final double MAX_DIAL_ANGLE = 89.2D;
	private static final double SNAP_THRESHOLD = 12.0D;
	private static final ResourceLocation BACKGROUND = new ResourceLocation("scp_additions:textures/screens/scp_914_gui.png");
	private static final ResourceLocation DIAL = new ResourceLocation("scp_additions:textures/screens/scp_914_dial.png");
	private static final ResourceLocation DIAL_HOVER = new ResourceLocation("scp_additions:textures/screens/scp_914_dial_hover.png");

	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private double guiScale = 1.0D;
	private DialSetting selected = DialSetting.ONE_TO_ONE;
	private boolean dragging = false;
	private boolean initializedSetting = false;
	private double dragAngle = 0.0D;

	public Scp914GuiScreen(Scp914GuiMenu container, Inventory inventory, Component text) {
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
		if (!initializedSetting) {
			selected = currentSettingFromBlock();
			dragAngle = selected.angle();
			initializedSetting = true;
		}
		this.renderBackground(guiGraphics);
		renderPanel(guiGraphics, mouseX, mouseY);
		renderHoverTooltip(guiGraphics, mouseX, mouseY);
	}

	private void renderPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(this.leftPos, this.topPos, 0);
		guiGraphics.pose().scale((float) guiScale, (float) guiScale, 1.0F);

		RenderSystem.setShaderColor(0.50F, 0.50F, 0.50F, 1.0F);
		guiGraphics.blit(BACKGROUND, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

		RenderSystem.setShaderColor(0.58F, 0.58F, 0.58F, 1.0F);
		renderDial(guiGraphics, dragging ? dragAngle : selected.angle(), dragging || isMouseNearDial(mouseX, mouseY));

		RenderSystem.setShaderColor(1, 1, 1, 1);
		guiGraphics.pose().popPose();
		RenderSystem.disableBlend();
	}

	private void renderDial(GuiGraphics guiGraphics, double angle, boolean hover) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(DIAL_PIVOT_X, DIAL_PIVOT_Y, 0);
		guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees((float) angle));
		guiGraphics.pose().translate(-DIAL_PIVOT_X, -DIAL_PIVOT_Y, 0);
		guiGraphics.blit(hover ? DIAL_HOVER : DIAL, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
		guiGraphics.pose().popPose();
	}

	private void renderHoverTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		double tx = textureX(mouseX);
		double ty = textureY(mouseY);
		DialSetting nearest = nearestSetting(tx, ty);
		if (nearest != null && distance(tx, ty, nearest.x, nearest.y) <= 70.0D) {
			guiGraphics.renderTooltip(this.font, Component.literal(nearest.label), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return true;
		}
		double tx = textureX(mouseX);
		double ty = textureY(mouseY);
		DialSetting nearest = nearestSetting(tx, ty);
		if (nearest != null && distance(tx, ty, nearest.x, nearest.y) <= 75.0D) {
			selectSetting(nearest, true);
			return true;
		}
		if (isMouseNearDial(mouseX, mouseY)) {
			dragging = true;
			updateDraggedDial(angleFor(tx, ty));
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (dragging && button == 0) {
			updateDraggedDial(angleFor(textureX(mouseX), textureY(mouseY)));
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging && button == 0) {
			dragging = false;
			selectSetting(nearestSettingByAngle(dragAngle), true);
			return true;
		}
		return true;
	}

	private void updateDraggedDial(double rawAngle) {
		double clamped = clampDialAngle(normalizeAngle(rawAngle));
		DialSetting snapped = snappedSetting(clamped);
		if (snapped != null) {
			dragAngle = snapped.angle();
			selectSetting(snapped, true);
		} else {
			dragAngle = clamped;
		}
	}

	private void selectSetting(DialSetting setting, boolean sendPacket) {
		boolean changed = selected != setting;
		selected = setting;
		dragAngle = setting.angle();
		if (sendPacket && changed) {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new Scp914GuiButtonMessage(setting.buttonId, x, y, z));
		}
	}

	private DialSetting currentSettingFromBlock() {
		Block block = world.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).getBlock();
		if (block == ScpAdditionsModBlocks.SCP_914DIAL_ROUGH.get()) return DialSetting.ROUGH;
		if (block == ScpAdditionsModBlocks.SCP_914DIAL_COARSE.get()) return DialSetting.COARSE;
		if (block == ScpAdditionsModBlocks.SCP_914DIAL_FINE.get()) return DialSetting.FINE;
		if (block == ScpAdditionsModBlocks.SCP_914DIAL_VERY_FINE.get()) return DialSetting.VERY_FINE;
		return DialSetting.ONE_TO_ONE;
	}

	private boolean isMouseNearDial(double mouseX, double mouseY) {
		double tx = textureX(mouseX);
		double ty = textureY(mouseY);
		double angle = dragging ? dragAngle : selected.angle();
		double tipX = DIAL_PIVOT_X + Math.sin(Math.toRadians(angle)) * 180.0D;
		double tipY = DIAL_PIVOT_Y - Math.cos(Math.toRadians(angle)) * 180.0D;
		return distance(tx, ty, DIAL_PIVOT_X, DIAL_PIVOT_Y) <= 85.0D || distance(tx, ty, tipX, tipY) <= 80.0D || distanceToSegment(tx, ty, DIAL_PIVOT_X, DIAL_PIVOT_Y, tipX, tipY) <= 35.0D;
	}

	private DialSetting nearestSetting(double tx, double ty) {
		DialSetting best = null;
		double bestDistance = Double.MAX_VALUE;
		for (DialSetting setting : DialSetting.values()) {
			double dist = distance(tx, ty, setting.x, setting.y);
			if (dist < bestDistance) {
				bestDistance = dist;
				best = setting;
			}
		}
		return best;
	}

	private DialSetting nearestSettingByAngle(double angle) {
		angle = clampDialAngle(normalizeAngle(angle));
		DialSetting best = DialSetting.ONE_TO_ONE;
		double bestDiff = Double.MAX_VALUE;
		for (DialSetting setting : DialSetting.values()) {
			double diff = Math.abs(normalizeAngle(angle - setting.angle()));
			if (diff < bestDiff) {
				bestDiff = diff;
				best = setting;
			}
		}
		return best;
	}

	private DialSetting snappedSetting(double angle) {
		angle = clampDialAngle(normalizeAngle(angle));
		for (DialSetting setting : DialSetting.values()) {
			if (Math.abs(angle - setting.angle()) <= SNAP_THRESHOLD) {
				return setting;
			}
		}
		return null;
	}

	private static double clampDialAngle(double angle) {
		if (angle < MIN_DIAL_ANGLE) return MIN_DIAL_ANGLE;
		if (angle > MAX_DIAL_ANGLE) return MAX_DIAL_ANGLE;
		return angle;
	}

	private static double normalizeAngle(double angle) {
		while (angle > 180.0D) angle -= 360.0D;
		while (angle < -180.0D) angle += 360.0D;
		return angle;
	}

	private static double angleFor(double tx, double ty) {
		return Math.toDegrees(Math.atan2(tx - DIAL_PIVOT_X, -(ty - DIAL_PIVOT_Y)));
	}

	private static double distance(double ax, double ay, double bx, double by) {
		double dx = ax - bx;
		double dy = ay - by;
		return Math.sqrt(dx * dx + dy * dy);
	}

	private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
		double dx = bx - ax;
		double dy = by - ay;
		if (dx == 0.0D && dy == 0.0D) {
			return distance(px, py, ax, ay);
		}
		double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
		t = Math.max(0.0D, Math.min(1.0D, t));
		return distance(px, py, ax + t * dx, ay + t * dy);
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

	private enum DialSetting {
		ROUGH(0, "Rough", 185, 466),
		COARSE(2, "Coarse", 338, 234),
		ONE_TO_ONE(4, "1:1", 573, 151),
		FINE(1, "Fine", 806, 241),
		VERY_FINE(3, "Very Fine", 951, 480);

		private final int buttonId;
		private final String label;
		private final double x;
		private final double y;

		DialSetting(int buttonId, String label, double x, double y) {
			this.buttonId = buttonId;
			this.label = label;
			this.x = x;
			this.y = y;
		}

		private double angle() {
			return angleFor(x, y);
		}
	}
}
