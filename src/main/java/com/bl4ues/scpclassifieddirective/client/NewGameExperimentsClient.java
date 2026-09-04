package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.mixin.client.ExperimentsScreenAccessor;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.ExperimentsScreen;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Custom presentation over vanilla's real ExperimentsScreen. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NewGameExperimentsClient {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9FA6AD;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int PANEL = 0xC20B0E12;
    private static final int ROW = 0xA80B0E12;
    private static final int BORDER = 0x70444C57;
    private static final int ROW_HEIGHT = 58;
    private static final int GAP = 7;

    private static final Map<ExperimentsScreen, State> STATES = new WeakHashMap<>();
    private static boolean armed;

    private NewGameExperimentsClient() {
    }

    static void arm() {
        armed = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ExperimentsScreen screen)) return;
        State existing = STATES.get(screen);
        if (existing != null) {
            existing.captureVanilla(event.getListenersList());
            existing.reattach(event);
            existing.hideVanilla();
            return;
        }
        if (!armed) return;
        armed = false;
        State state = new State(screen, event.getListenersList());
        STATES.put(screen, state);
        state.build(event);
        state.hideVanilla();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof ExperimentsScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null) return;
        state.captureVanilla(screen.children());
        state.hideVanilla();
        state.render(event.getGuiGraphics(), event.getMouseX(),
                event.getMouseY(), event.getPartialTick());
        event.setCanceled(true);
    }

    private static final class State {
        private final ExperimentsScreen screen;
        private final Object2BooleanMap<Pack> packs;
        private final Set<GuiEventListener> vanilla =
                java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<Row> rows = new ArrayList<>();
        private final List<AbstractWidget> custom = new ArrayList<>();
        private NewGameWidgets.ActionButton back;
        private NewGameWidgets.ActionButton done;
        private int scroll;
        private int contentHeight;

        private State(ExperimentsScreen screen,
                List<GuiEventListener> initialListeners) {
            this.screen = screen;
            this.packs = ((ExperimentsScreenAccessor) screen)
                    .scpclassifieddirective$getPacks();
            vanilla.addAll(initialListeners);
        }

        private void build(ScreenEvent.Init.Post event) {
            rows.clear();
            List<Pack> ordered = new ArrayList<>(packs.keySet());
            ordered.sort(Comparator.comparing(pack -> pack.getTitle().getString(),
                    String.CASE_INSENSITIVE_ORDER));
            for (Pack pack : ordered) {
                NewGameWidgets.Toggle toggle = add(event,
                        new NewGameWidgets.Toggle(0, 0, 240, 34,
                                pack.getTitle().getString(),
                                packs.getBoolean(pack),
                                enabled -> packs.put(pack, enabled)));
                rows.add(new Row(pack, toggle));
            }
            back = add(event, new NewGameWidgets.ActionButton(
                    0, 0, 150, 34, ScpFonts.roboto("Back"), screen::onClose));
            done = add(event, new NewGameWidgets.ActionButton(
                    0, 0, 190, 34, ScpFonts.roboto("Apply Experiments"),
                    () -> ((ExperimentsScreenAccessor) screen)
                            .scpclassifieddirective$invokeOnDone()));
        }

        private <T extends AbstractWidget> T add(ScreenEvent.Init.Post event, T widget) {
            custom.add(widget);
            event.addListener(widget);
            return widget;
        }

        private void reattach(ScreenEvent.Init.Post event) {
            for (AbstractWidget widget : custom) {
                if (!event.getListenersList().contains(widget)) {
                    event.addListener(widget);
                }
                widget.visible = true;
            }
        }

        private void captureVanilla(List<? extends GuiEventListener> listeners) {
            for (GuiEventListener listener : listeners) {
                if (!custom.contains(listener)) vanilla.add(listener);
            }
        }

        private void hideVanilla() {
            for (GuiEventListener listener : vanilla) {
                if (listener instanceof AbstractWidget widget) widget.visible = false;
            }
            for (AbstractWidget widget : custom) widget.visible = true;
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            ConfigCenterVisuals.renderBackdrop(screen, graphics, mouseX, mouseY);
            Layout layout = layout();
            float alpha = ConfigCenterVisuals.contentAlpha();
            graphics.fill(layout.x, layout.top, layout.x + layout.width,
                    layout.bottom, fade(PANEL, alpha));
            graphics.fill(layout.x, layout.top, layout.x + 4,
                    layout.bottom, fade(ACCENT, alpha));
            drawScaled(graphics, ScpFonts.montserrat("EXPERIMENTS"),
                    layout.x + 20, layout.top + 17, 1.28F, fade(TEXT, alpha));
            drawScaled(graphics, ScpFonts.titillium(
                            "Experimental world features may affect compatibility."),
                    layout.x + 20, layout.top + 39, 0.98F, fade(MUTED, alpha));
            graphics.fill(layout.x + 20, layout.listTop - 10,
                    layout.x + layout.width - 20, layout.listTop - 9,
                    fade(BORDER, alpha));

            position(layout);
            graphics.enableScissor(layout.x + 8, layout.listTop,
                    layout.x + layout.width - 8, layout.listBottom);
            for (Row row : rows) {
                if (row.y + ROW_HEIGHT <= layout.listTop
                        || row.y >= layout.listBottom) continue;
                boolean hovered = mouseX >= layout.x + 18
                        && mouseX < layout.x + layout.width - 18
                        && mouseY >= row.y && mouseY < row.y + ROW_HEIGHT;
                graphics.fill(layout.x + 18, row.y,
                        layout.x + layout.width - 18, row.y + ROW_HEIGHT,
                        fade(hovered ? 0xD5161B22 : ROW, alpha));
                graphics.fill(layout.x + 18, row.y,
                        layout.x + 21, row.y + ROW_HEIGHT, fade(ACCENT, alpha));
                graphics.drawString(Minecraft.getInstance().font,
                        ScpFonts.titillium(fit(row.pack.getDescription().getString(),
                                layout.width - 330)),
                        layout.x + 32, row.y + 34,
                        fade(MUTED, alpha), false);
                row.toggle.render(graphics, mouseX, mouseY, partialTick);
            }
            graphics.disableScissor();
            drawScrollbar(graphics, layout, alpha);

            int footerY = layout.bottom + 12;
            back.setX(layout.x);
            back.setY(footerY);
            done.setX(layout.x + layout.width - done.getWidth());
            done.setY(footerY);
            back.render(graphics, mouseX, mouseY, partialTick);
            done.render(graphics, mouseX, mouseY, partialTick);
        }

        private void position(Layout layout) {
            int y = layout.listTop - scroll;
            int toggleW = Mth.clamp(Math.round(layout.width * 0.34F), 190, 270);
            for (Row row : rows) {
                row.y = y;
                row.toggle.setX(layout.x + layout.width - toggleW - 24);
                row.toggle.setY(y + 8);
                row.toggle.setWidth(toggleW);
                row.toggle.setHeight(34);
                row.toggle.visible = y + ROW_HEIGHT > layout.listTop
                        && y < layout.listBottom;
                y += ROW_HEIGHT + GAP;
            }
            contentHeight = Math.max(0, y + scroll - layout.listTop);
            int max = Math.max(0, contentHeight
                    - (layout.listBottom - layout.listTop));
            scroll = Mth.clamp(scroll, 0, max);
        }

        private void drawScrollbar(GuiGraphics graphics, Layout layout,
                float alpha) {
            int viewport = layout.listBottom - layout.listTop;
            int max = Math.max(0, contentHeight - viewport);
            if (max <= 0) return;
            int x = layout.x + layout.width - 8;
            graphics.fill(x, layout.listTop, x + 2, layout.listBottom,
                    fade(0x553A424D, alpha));
            int thumb = Math.max(18,
                    Math.round(viewport * viewport / (float) contentHeight));
            int travel = viewport - thumb;
            int y = layout.listTop + Math.round(travel * (scroll / (float) max));
            graphics.fill(x, y, x + 2, y + thumb, fade(ACCENT, alpha));
        }

        private Layout layout() {
            int width = Mth.clamp(Math.round(screen.width * 0.54F),
                    Math.min(450, Math.max(300, screen.width - 30)), 780);
            width = Math.min(width, Math.max(280, screen.width - 28));
            int x = ConfigCenterVisuals.contentLeft(screen.width, width);
            int top = Math.max(24, Math.round(screen.height * 0.055F));
            int bottom = Math.max(top + 220, screen.height - 62);
            return new Layout(x, width, top, bottom,
                    top + 72, bottom - 16);
        }

        private static String fit(String value, int pixels) {
            var font = Minecraft.getInstance().font;
            if (pixels <= 40) return "";
            if (font.width(ScpFonts.titillium(value)) <= pixels) return value;
            String suffix = "...";
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                String next = out.toString() + value.charAt(i);
                if (font.width(ScpFonts.titillium(next + suffix)) > pixels) break;
                out.append(value.charAt(i));
            }
            return out + suffix;
        }
    }

    private static final class Row {
        private final Pack pack;
        private final NewGameWidgets.Toggle toggle;
        private int y;

        private Row(Pack pack, NewGameWidgets.Toggle toggle) {
            this.pack = pack;
            this.toggle = toggle;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof ExperimentsScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || event.getScrollDelta() == 0.0D) return;
        Layout layout = state.layout();
        if (event.getMouseX() < layout.x
                || event.getMouseX() >= layout.x + layout.width
                || event.getMouseY() < layout.listTop
                || event.getMouseY() >= layout.listBottom) return;
        int max = Math.max(0, state.contentHeight
                - (layout.listBottom - layout.listTop));
        state.scroll = Mth.clamp(state.scroll
                + (event.getScrollDelta() > 0.0D ? -44 : 44), 0, max);
        event.setCanceled(true);
    }

    private static void drawScaled(GuiGraphics graphics,
            net.minecraft.network.chat.Component text, float x, float y,
            float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, text,
                0, 0, color, false);
        graphics.pose().popPose();
    }

    private static int fade(int color, float alpha) {
        int source = (color >>> 24) & 0xFF;
        int out = Mth.clamp(Math.round(source * alpha), 0, 255);
        return (out << 24) | (color & 0x00FFFFFF);
    }

    private record Layout(int x, int width, int top, int bottom,
            int listTop, int listBottom) {
    }
}
