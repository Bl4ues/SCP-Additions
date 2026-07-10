package net.mcreator.scpadditions.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.network.TeslaTerminalButtonMessage;
import net.mcreator.scpadditions.world.inventory.TeslaTerminalMenu;

import com.mojang.blaze3d.systems.RenderSystem;

public class TeslaTerminalScreen extends AbstractContainerScreen<TeslaTerminalMenu> {
	private static final int TEX_W = 1410;
	private static final int TEX_H = 1080;
	private static final ResourceLocation SCREEN_ON = screen("1");
	private static final ResourceLocation SCREEN_STANDBY_DISABLE = screen("2");
	private static final ResourceLocation SCREEN_OFF = screen("3");
	private static final ResourceLocation SCREEN_STANDBY_ENABLE = screen("4");
	private static final ResourceLocation SCREEN_CREDENTIAL_PROMPT = screen("5");
	private static final ResourceLocation SCREEN_INVALID_CREDENTIALS = screen("6");
	private static final ResourceLocation SCREEN_AUTH_SUCCESS = screen("7");
	private static final ResourceLocation SCREEN_OVERRIDE_WARNING = screen("8");
	private static final ResourceLocation SCREEN_OVERRIDE_STANDBY = screen("9");
	private static final ResourceLocation SCREEN_OVERRIDE_ENGAGED = screen("10");
	private static final ResourceLocation SCREEN_ON_OVERRIDE = screen("11");

	private static final Rect OVERRIDE_TOGGLE = new Rect(1269, 637, 1393, 689);
	private static final Rect TESLA_TOGGLE = new Rect(1050, 1007, 1393, 1069);
	private static final Rect CREDENTIAL_OK = new Rect(345, 671, 688, 734);
	private static final Rect CREDENTIAL_CANCEL = new Rect(756, 671, 1101, 734);
	private static final Rect WARNING_ENGAGE = new Rect(383, 978, 764, 1027);
	private static final Rect WARNING_CANCEL = new Rect(820, 978, 1165, 1027);

	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private double guiScale = 1.0D;
	private VisualState visualState = VisualState.MAIN;
	private PendingAction pendingAction = PendingAction.NONE;
	private int visualTimer = 0;
	private boolean authenticated = false;
	private boolean initializedDisplayState = false;
	private boolean displayedTeslaGatesEnabled = true;
	private boolean displayedManualOverride = false;

	public TeslaTerminalScreen(TeslaTerminalMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = TEX_W;
		this.imageHeight = TEX_H;
	}

	private static ResourceLocation screen(String id) {
		return new ResourceLocation("scp_additions:textures/screens/" + id + ".png");
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		initializeDisplayState();
		updateLayout();
		this.renderBackground(guiGraphics);
		renderTerminal(guiGraphics);
	}

	private void renderTerminal(GuiGraphics guiGraphics) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(this.leftPos, this.topPos, 0);
		guiGraphics.pose().scale((float) guiScale, (float) guiScale, 1.0F);

		if (isOverlayState()) {
			guiGraphics.blit(mainTexture(), 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
			guiGraphics.blit(overlayTexture(), 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
		} else {
			guiGraphics.blit(currentTexture(), 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
		}
		renderPermissionText(guiGraphics);

		guiGraphics.pose().popPose();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private void renderPermissionText(GuiGraphics guiGraphics) {
		String text = authenticated ? "GRANTED" : "DENIED";
		int color = authenticated ? 0x608952 : 0xAC384A;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(1278, 63, 0);
		guiGraphics.pose().scale(3.0F, 3.0F, 1.0F);
		guiGraphics.drawString(this.font, Component.literal(text), 0, 0, color, false);
		guiGraphics.pose().popPose();
	}

	private ResourceLocation currentTexture() {
		return switch (visualState) {
			case STANDBY_DISABLE -> SCREEN_STANDBY_DISABLE;
			case STANDBY_ENABLE -> SCREEN_STANDBY_ENABLE;
			case MAIN, CREDENTIAL_PROMPT, INVALID_CREDENTIALS, AUTH_SUCCESS, OVERRIDE_WARNING, OVERRIDE_STANDBY, OVERRIDE_ENGAGED -> mainTexture();
		};
	}

	private ResourceLocation mainTexture() {
		if (displayedManualOverride) {
			return SCREEN_ON_OVERRIDE;
		}
		return displayedTeslaGatesEnabled ? SCREEN_ON : SCREEN_OFF;
	}

	private ResourceLocation overlayTexture() {
		return switch (visualState) {
			case CREDENTIAL_PROMPT -> SCREEN_CREDENTIAL_PROMPT;
			case INVALID_CREDENTIALS -> SCREEN_INVALID_CREDENTIALS;
			case AUTH_SUCCESS -> SCREEN_AUTH_SUCCESS;
			case OVERRIDE_WARNING -> SCREEN_OVERRIDE_WARNING;
			case OVERRIDE_STANDBY -> SCREEN_OVERRIDE_STANDBY;
			case OVERRIDE_ENGAGED -> SCREEN_OVERRIDE_ENGAGED;
			default -> mainTexture();
		};
	}

	private boolean isOverlayState() {
		return visualState == VisualState.CREDENTIAL_PROMPT || visualState == VisualState.INVALID_CREDENTIALS || visualState == VisualState.AUTH_SUCCESS || visualState == VisualState.OVERRIDE_WARNING || visualState == VisualState.OVERRIDE_STANDBY
				|| visualState == VisualState.OVERRIDE_ENGAGED;
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

		if (visualState == VisualState.MAIN) {
			if (TESLA_TOGGLE.contains(tx, ty)) {
				pendingAction = displayedTeslaGatesEnabled ? PendingAction.DISABLE_GATES : PendingAction.ENABLE_GATES;
				if (authenticated) {
					beginAuthorizedAction(pendingAction);
				} else {
					visualState = VisualState.CREDENTIAL_PROMPT;
				}
				return true;
			}
			if (OVERRIDE_TOGGLE.contains(tx, ty)) {
				pendingAction = displayedManualOverride ? PendingAction.OVERRIDE_OFF : PendingAction.OVERRIDE_ON;
				if (authenticated) {
					beginAuthorizedAction(pendingAction);
				} else {
					visualState = VisualState.CREDENTIAL_PROMPT;
				}
				return true;
			}
			return true;
		}

		if (visualState == VisualState.CREDENTIAL_PROMPT) {
			if (CREDENTIAL_OK.contains(tx, ty)) {
				if (hasCredentialsItem()) {
					authenticated = true;
					setTimedState(VisualState.AUTH_SUCCESS, 55);
				} else {
					visualState = VisualState.INVALID_CREDENTIALS;
				}
				return true;
			}
			if (CREDENTIAL_CANCEL.contains(tx, ty)) {
				resetToMain();
				return true;
			}
		}

		if (visualState == VisualState.INVALID_CREDENTIALS) {
			if (CREDENTIAL_OK.contains(tx, ty) || CREDENTIAL_CANCEL.contains(tx, ty)) {
				resetToMain();
				return true;
			}
		}

		if (visualState == VisualState.OVERRIDE_WARNING) {
			if (WARNING_ENGAGE.contains(tx, ty)) {
				setTimedState(VisualState.OVERRIDE_STANDBY, 110);
				return true;
			}
			if (WARNING_CANCEL.contains(tx, ty)) {
				resetToMain();
				return true;
			}
		}

		return true;
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (visualTimer > 0) {
			visualTimer--;
			if (visualTimer <= 0) {
				onTimedStateFinished();
			}
		}
	}

	private void onTimedStateFinished() {
		if (visualState == VisualState.AUTH_SUCCESS) {
			beginAuthorizedAction(pendingAction);
			return;
		}

		if (visualState == VisualState.STANDBY_DISABLE) {
			applyAndSendAction(PendingAction.DISABLE_GATES);
			resetToMain();
			return;
		}

		if (visualState == VisualState.STANDBY_ENABLE) {
			applyAndSendAction(PendingAction.ENABLE_GATES);
			resetToMain();
			return;
		}

		if (visualState == VisualState.OVERRIDE_STANDBY) {
			applyAndSendAction(pendingAction);
			if (pendingAction == PendingAction.OVERRIDE_ON) {
				setTimedState(VisualState.OVERRIDE_ENGAGED, 60);
			} else {
				resetToMain();
			}
			return;
		}

		if (visualState == VisualState.OVERRIDE_ENGAGED) {
			resetToMain();
		}
	}

	private void beginAuthorizedAction(PendingAction action) {
		if (action == PendingAction.DISABLE_GATES) {
			setTimedState(VisualState.STANDBY_DISABLE, 125);
		} else if (action == PendingAction.ENABLE_GATES) {
			setTimedState(VisualState.STANDBY_ENABLE, 125);
		} else if (action == PendingAction.OVERRIDE_ON) {
			visualState = VisualState.OVERRIDE_WARNING;
			visualTimer = 0;
		} else if (action == PendingAction.OVERRIDE_OFF) {
			setTimedState(VisualState.OVERRIDE_STANDBY, 110);
		} else {
			resetToMain();
		}
	}

	private void applyAndSendAction(PendingAction action) {
		applyLocalAction(action);
		sendAction(action);
	}

	private void applyLocalAction(PendingAction action) {
		if (action == PendingAction.ENABLE_GATES) {
			displayedTeslaGatesEnabled = true;
		} else if (action == PendingAction.DISABLE_GATES) {
			displayedTeslaGatesEnabled = false;
			displayedManualOverride = false;
		} else if (action == PendingAction.OVERRIDE_ON) {
			displayedTeslaGatesEnabled = true;
			displayedManualOverride = true;
		} else if (action == PendingAction.OVERRIDE_OFF) {
			displayedManualOverride = false;
		}
	}

	private void sendAction(PendingAction action) {
		if (action == PendingAction.ENABLE_GATES) {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(0, x, y, z));
		} else if (action == PendingAction.DISABLE_GATES) {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(1, x, y, z));
		} else if (action == PendingAction.OVERRIDE_ON) {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(3, x, y, z));
		} else if (action == PendingAction.OVERRIDE_OFF) {
			ScpAdditionsMod.PACKET_HANDLER.sendToServer(new TeslaTerminalButtonMessage(4, x, y, z));
		}
	}

	private void setTimedState(VisualState state, int timer) {
		this.visualState = state;
		this.visualTimer = timer;
	}

	private void resetToMain() {
		this.visualState = VisualState.MAIN;
		this.pendingAction = PendingAction.NONE;
		this.visualTimer = 0;
	}

	private void initializeDisplayState() {
		if (initializedDisplayState) {
			return;
		}
		displayedTeslaGatesEnabled = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
		displayedManualOverride = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
		initializedDisplayState = true;
	}

	private boolean hasCredentialsItem() {
		return entity != null && entity.getInventory().contains(new ItemStack(ScpAdditionsModItems.SECURITY_CREDENTIALS.get()));
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

	private enum VisualState {
		MAIN,
		CREDENTIAL_PROMPT,
		INVALID_CREDENTIALS,
		AUTH_SUCCESS,
		STANDBY_DISABLE,
		STANDBY_ENABLE,
		OVERRIDE_WARNING,
		OVERRIDE_STANDBY,
		OVERRIDE_ENGAGED
	}

	private enum PendingAction {
		NONE,
		ENABLE_GATES,
		DISABLE_GATES,
		OVERRIDE_ON,
		OVERRIDE_OFF
	}

	private record Rect(double x1, double y1, double x2, double y2) {
		private boolean contains(double x, double y) {
			return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) && y >= Math.min(y1, y2) && y <= Math.max(y1, y2);
		}
	}
}
