package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * SCP: Classified Directive presentation for the vanilla Add/Edit Server and
 * Direct Connection screens opened by the custom multiplayer panel.
 *
 * <p>The real vanilla screens and their widgets remain authoritative. We only
 * reposition and redraw those same listeners, preserving validation, callbacks,
 * resource-pack policy and mod-added controls.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MultiplayerConnectionScreenClient {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9FA6AD;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int PANEL = 0xC20B0E12;
    private static final int BASE = 0xD00B0E12;
    private static final int HOVER = 0xF0161B22;
    private static final int DISABLED = 0xB011151A;
    private static final int BORDER = 0x80444C57;

    private static final Set<Screen> ARMED = Collections.newSetFromMap(
            new WeakHashMap<>());
    private static final Map<Screen, State> STATES = new WeakHashMap<>();

    private MultiplayerConnectionScreenClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOpening(ScreenEvent.Opening event) {
        Screen incoming = event.getNewScreen();
        if (!(incoming instanceof EditServerScreen)
                && !(incoming instanceof DirectJoinServerScreen)) return;
        if (!(event.getCurrentScreen() instanceof CustomMainMenuScreen menu)) {
            return;
        }
        ConfigCenterVisuals.prepare(menu);
        ARMED.add(incoming);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        State existing = STATES.get(screen);
        if (existing != null) {
            existing.capture(event.getListenersList());
            return;
        }
        if (!ARMED.remove(screen)) return;
        State state = new State(screen);
        state.capture(event.getListenersList());
        STATES.put(screen, state);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Pre event) {
        State state = STATES.get(event.getScreen());
        if (state == null) return;
        state.render(event.getGuiGraphics(), event.getMouseX(),
                event.getMouseY(), event.getPartialTick());
        event.setCanceled(true);
    }

    private static final class State {
        private final Screen screen;
        private final List<EditBox> fields = new ArrayList<>();
        private final List<AbstractWidget> extras = new ArrayList<>();
        private Button primary;
        private Button cancel;

        private State(Screen screen) {
            this.screen = screen;
        }

        private void capture(List<? extends GuiEventListener> listeners) {
            fields.clear();
            extras.clear();
            primary = null;
            cancel = null;

            List<Button> buttons = new ArrayList<>();
            for (GuiEventListener listener : listeners) {
                if (listener instanceof EditBox box) {
                    box.setBordered(false);
                    fields.add(box);
                } else if (listener instanceof Button button) {
                    buttons.add(button);
                } else if (listener instanceof AbstractWidget widget) {
                    extras.add(widget);
                }
            }

            String cancelText = Component.translatable("gui.cancel")
                    .getString();
            for (Button button : buttons) {
                if (cancel == null
                        && cancelText.equals(button.getMessage().getString())) {
                    cancel = button;
                } else if (primary == null) {
                    primary = button;
                } else {
                    extras.add(button);
                }
            }
            if (cancel == null && buttons.size() >= 2) {
                cancel = buttons.get(buttons.size() - 1);
                if (primary == cancel) primary = buttons.get(0);
            }
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            ConfigCenterVisuals.renderBackdrop(screen, graphics, mouseX, mouseY);
            Layout layout = layout();
            float alpha = ConfigCenterVisuals.contentAlpha();
            position(layout);

            graphics.fill(layout.x, layout.y,
                    layout.x + layout.width, layout.y + layout.height,
                    fade(PANEL, alpha));
            graphics.fill(layout.x, layout.y,
                    layout.x + 4, layout.y + layout.height,
                    fade(ACCENT, alpha));
            graphics.fill(layout.x + 20, layout.y + 62,
                    layout.x + layout.width - 20, layout.y + 63,
                    fade(BORDER, alpha));

            drawScaled(graphics, ScpFonts.montserrat(title()),
                    layout.x + 22, layout.y + 18, 1.30F,
                    fade(TEXT, alpha));
            drawScaled(graphics, ScpFonts.titillium(subtitle()),
                    layout.x + 22, layout.y + 40, 0.98F,
                    fade(MUTED, alpha));

            for (int i = 0; i < fields.size(); i++) {
                EditBox box = fields.get(i);
                label(graphics, fieldLabel(i), box.getX(), box.getY() - 16,
                        alpha);
                drawEditSurface(graphics, box, alpha);
                renderEditBox(graphics, box, mouseX, mouseY, partialTick);
            }

            for (AbstractWidget widget : extras) {
                drawControl(graphics, widget, mouseX, mouseY, alpha);
            }

            int separatorY = layout.y + layout.height - 57;
            graphics.fill(layout.x + 20, separatorY,
                    layout.x + layout.width - 20, separatorY + 1,
                    fade(BORDER, alpha));
            if (cancel != null) drawControl(graphics, cancel, mouseX, mouseY, alpha);
            if (primary != null) drawControl(graphics, primary, mouseX, mouseY, alpha);
        }

        private void position(Layout layout) {
            int innerX = layout.x + 24;
            int innerWidth = layout.width - 48;
            int y = layout.y + 92;
            for (EditBox box : fields) {
                box.setX(innerX);
                box.setY(y);
                box.setWidth(innerWidth);
                box.setHeight(32);
                box.visible = true;
                y += 62;
            }
            for (AbstractWidget widget : extras) {
                widget.setX(innerX);
                widget.setY(y);
                widget.setWidth(innerWidth);
                widget.setHeight(32);
                widget.visible = true;
                y += 40;
            }

            int footerY = layout.y + layout.height - 44;
            int buttonWidth = Math.max(130,
                    Math.min(190, (innerWidth - 12) / 2));
            if (cancel != null) {
                cancel.setX(innerX);
                cancel.setY(footerY);
                cancel.setWidth(buttonWidth);
                cancel.setHeight(32);
                cancel.visible = true;
            }
            if (primary != null) {
                primary.setWidth(buttonWidth);
                primary.setHeight(32);
                primary.setX(layout.x + layout.width - 24 - buttonWidth);
                primary.setY(footerY);
                primary.visible = true;
            }
        }

        private String title() {
            if (screen instanceof DirectJoinServerScreen) {
                return "DIRECT CONNECTION";
            }
            if (fields.size() >= 2 && fields.get(1).getValue().isBlank()) {
                return "ADD SERVER";
            }
            return "EDIT SERVER";
        }

        private String subtitle() {
            if (screen instanceof DirectJoinServerScreen) {
                return "Connect directly without adding the server to your list";
            }
            if (fields.size() >= 2 && fields.get(1).getValue().isBlank()) {
                return "Add a server to the multiplayer directory";
            }
            return "Update this server entry";
        }

        private String fieldLabel(int index) {
            if (screen instanceof EditServerScreen && fields.size() >= 2) {
                return index == 0 ? "SERVER NAME" : "SERVER ADDRESS";
            }
            return "SERVER ADDRESS";
        }

        private Layout layout() {
            int width = Mth.clamp(Math.round(screen.width * 0.44F),
                    390, 620);
            width = Math.min(width, Math.max(300, screen.width - 32));
            int extraHeight = extras.size() * 40;
            int fieldHeight = fields.size() * 62;
            int height = Mth.clamp(156 + fieldHeight + extraHeight,
                    230, Math.max(230, screen.height - 44));
            int x = ConfigCenterVisuals.contentLeft(screen.width, width);
            int y = Math.max(22, (screen.height - height) / 2);
            return new Layout(x, y, width, height);
        }
    }

    private static void renderEditBox(GuiGraphics graphics, EditBox box,
            int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int insetX = 10;
        int insetY = Math.max(0, (box.getHeight() - font.lineHeight) / 2);
        graphics.pose().pushPose();
        graphics.pose().translate(insetX, insetY, 0.0F);
        box.render(graphics, mouseX - insetX, mouseY - insetY, partialTick);
        graphics.pose().popPose();
    }

    private static void drawEditSurface(GuiGraphics graphics, EditBox box,
            float alpha) {
        int x = box.getX();
        int y = box.getY();
        int width = box.getWidth();
        int height = box.getHeight();
        graphics.fill(x, y, x + width, y + height, fade(BASE, alpha));
        int border = box.isFocused() ? ACCENT : BORDER;
        graphics.fill(x, y, x + width, y + 1, fade(border, alpha));
        graphics.fill(x, y + height - 1, x + width, y + height,
                fade(border, alpha));
        graphics.fill(x, y, x + 1, y + height, fade(border, alpha));
        graphics.fill(x + width - 1, y, x + width, y + height,
                fade(border, alpha));
    }

    private static void drawControl(GuiGraphics graphics,
            AbstractWidget widget, int mouseX, int mouseY, float alpha) {
        boolean hovered = widget.active && widget.isMouseOver(mouseX, mouseY);
        int background = !widget.active ? DISABLED : hovered ? HOVER : BASE;
        graphics.fill(widget.getX(), widget.getY(),
                widget.getX() + widget.getWidth(),
                widget.getY() + widget.getHeight(), fade(background, alpha));
        graphics.fill(widget.getX(), widget.getY(), widget.getX() + 4,
                widget.getY() + widget.getHeight(),
                fade(widget.active ? ACCENT : 0xFF555B64, alpha));
        Font font = Minecraft.getInstance().font;
        Component message = ScpFonts.roboto(widget.getMessage());
        int color = !widget.active ? MUTED
                : hovered ? ACCENT_BRIGHT : TEXT;
        graphics.drawString(font, message, widget.getX() + 14,
                widget.getY() + Math.max(1,
                        (widget.getHeight() - font.lineHeight) / 2),
                fade(color, alpha), false);
    }

    private static void label(GuiGraphics graphics, String text,
            int x, int y, float alpha) {
        graphics.drawString(Minecraft.getInstance().font,
                ScpFonts.montserrat(text), x, y,
                fade(MUTED, alpha), false);
    }

    private static void drawScaled(GuiGraphics graphics, Component text,
            float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, text,
                0, 0, color, false);
        graphics.pose().popPose();
    }

    private static int fade(int color, float alpha) {
        int source = (color >>> 24) & 0xFF;
        int out = Mth.clamp(Math.round(source
                * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return (out << 24) | (color & 0x00FFFFFF);
    }

    private record Layout(int x, int y, int width, int height) {
    }
}
