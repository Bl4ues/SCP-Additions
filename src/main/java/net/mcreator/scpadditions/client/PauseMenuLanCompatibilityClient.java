package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.mixin.client.ScreenInvoker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Hosts a real ShareToLanScreen behind the custom pause panel. Mods keep their
 * original callbacks and validation, while their injected widgets are laid out
 * and rendered as a continuation of SCP Additions' LAN presentation. Widgets
 * sharing a source Y coordinate are treated as one logical option row, which
 * keeps arbitrary LAN extensions usable without per-mod hardcoded coordinates.
 */
public final class PauseMenuLanCompatibilityClient {
    private static final int PANEL = 0xE20B0E12;
    private static final int ROW = 0xA2181D24;
    private static final int ROW_HOVER = 0xC6242B35;
    private static final int FIELD = 0xD110141A;
    private static final int BORDER = 0x70414A56;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9DA5AF;
    private static final int CONTENT_GAP = 8;
    private static final int BUTTON_HEIGHT = 30;

    private static final Map<CustomPauseMenuScreen, Backend> BACKENDS =
            new WeakHashMap<>();
    private static final ThreadLocal<Integer> PORT_OVERRIDE = new ThreadLocal<>();

    private PauseMenuLanCompatibilityClient() {
    }

    public static Integer portOverride() {
        return PORT_OVERRIDE.get();
    }

    public static void render(CustomPauseMenuScreen parent,
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Object nativeState = nativeState(parent);
        if (!isLanActive(nativeState)) {
            BACKENDS.remove(parent);
            return;
        }

        Backend backend = ensureBackend(parent, nativeState);
        if (backend == null) return;
        syncBackendFromCustom(backend, nativeState);
        if (backend.extras.isEmpty()) return;

        ExtraLayout layout = extraLayout(parent, nativeState, backend);
        if (layout == null) return;
        layoutExtras(backend, layout);

        graphics.fill(layout.x, layout.y, layout.x + layout.width,
                layout.y + layout.height, PANEL);
        graphics.fill(layout.x, layout.y, layout.x + layout.width,
                layout.y + 3, ACCENT);
        graphics.fill(layout.x, layout.y, layout.x + 2,
                layout.y + layout.height, ACCENT);
        graphics.fill(layout.x, layout.y + layout.height - 1,
                layout.x + layout.width, layout.y + layout.height, BORDER);

        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, ScpFonts.montserrat("LAN OPTIONS"),
                layout.x + 16, layout.y + 16, TEXT, false);
        graphics.fill(layout.x + 16, layout.y + 36,
                layout.x + layout.width - 16, layout.y + 37, BORDER);

        for (WidgetPlacement placement : backend.extras) {
            AbstractWidget widget = placement.widget;
            if (!widget.visible || placement.rowY < 0) continue;
            renderIntegratedWidget(graphics, font, placement,
                    mouseX, mouseY, partialTick);
        }
    }

    public static boolean mouseClicked(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        Object nativeState = nativeState(parent);
        if (!isLanActive(nativeState)) return false;
        Backend backend = ensureBackend(parent, nativeState);
        if (backend == null) return false;
        syncBackendFromCustom(backend, nativeState);

        ExtraLayout layout = extraLayout(parent, nativeState, backend);
        if (layout != null) layoutExtras(backend, layout);

        for (WidgetPlacement placement : backend.extras) {
            if (placement.widget.visible
                    && placement.widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        for (WidgetPlacement placement : backend.extras) {
            placement.widget.setFocused(false);
        }

        if (button == 0 && isNativeStartArea(nativeState, mouseX, mouseY)
                && backend.startButton != null) {
            return startThroughBackend(parent, nativeState, backend);
        }
        return false;
    }

    public static boolean mouseReleased(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        Backend backend = activeBackend(parent);
        if (backend == null) return false;
        for (WidgetPlacement placement : backend.extras) {
            if (placement.widget.visible
                    && placement.widget.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mouseDragged(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        Backend backend = activeBackend(parent);
        if (backend == null) return false;
        for (WidgetPlacement placement : backend.extras) {
            if (placement.widget.visible
                    && placement.widget.mouseDragged(mouseX, mouseY, button,
                    dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mouseScrolled(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, double delta) {
        Backend backend = activeBackend(parent);
        if (backend == null) return false;
        for (WidgetPlacement placement : backend.extras) {
            if (placement.widget.visible
                    && placement.widget.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return false;
    }

    public static boolean keyPressed(CustomPauseMenuScreen parent,
            int keyCode, int scanCode, int modifiers) {
        Backend backend = activeBackend(parent);
        if (backend == null) return false;
        for (WidgetPlacement placement : backend.extras) {
            if (placement.widget.visible
                    && placement.widget.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public static boolean keyReleased(CustomPauseMenuScreen parent,
            int keyCode, int scanCode, int modifiers) {
        Backend backend = activeBackend(parent);
        if (backend == null) return false;
        for (WidgetPlacement placement : backend.extras) {
            if (placement.widget.visible
                    && placement.widget.keyReleased(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    public static boolean charTyped(CustomPauseMenuScreen parent,
            char codePoint, int modifiers) {
        Backend backend = activeBackend(parent);
        if (backend == null) return false;
        for (WidgetPlacement placement : backend.extras) {
            if (placement.widget.visible
                    && placement.widget.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    private static Backend activeBackend(CustomPauseMenuScreen parent) {
        Object nativeState = nativeState(parent);
        return isLanActive(nativeState) ? BACKENDS.get(parent) : null;
    }

    private static Backend ensureBackend(CustomPauseMenuScreen parent,
            Object nativeState) {
        Backend existing = BACKENDS.get(parent);
        if (existing != null && existing.width == parent.width
                && existing.height == parent.height) {
            return existing;
        }
        try {
            ShareToLanScreen screen = new ShareToLanScreen(parent);
            ((ScreenInvoker) (Object) screen).scpAdditions$invokeInit(
                    Minecraft.getInstance(), parent.width, parent.height);
            Backend backend = inspectBackend(screen, parent.width, parent.height);
            importBackendToCustom(backend, nativeState);
            BACKENDS.put(parent, backend);
            return backend;
        } catch (Throwable throwable) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not initialize mod-compatible LAN controls", throwable);
            return null;
        }
    }

    private static Backend inspectBackend(ShareToLanScreen screen,
            int width, int height) {
        Button start = null;
        CycleButton<?> gameMode = null;
        CycleButton<?> commands = null;
        EditBox port = null;
        List<WidgetPlacement> extras = new ArrayList<>();
        Map<AbstractWidget, Boolean> vanilla = new IdentityHashMap<>();

        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractWidget widget)) continue;
            if (hasTranslationKey(widget.getMessage(), "lanServer.start")
                    && widget instanceof Button button) {
                start = button;
                vanilla.put(widget, true);
            } else if (hasTranslationKey(widget.getMessage(), "gui.cancel")) {
                vanilla.put(widget, true);
            } else if (hasTranslationKey(widget.getMessage(),
                    "selectWorld.gameMode") && widget instanceof CycleButton<?> cycle) {
                gameMode = cycle;
                vanilla.put(widget, true);
            } else if (hasTranslationKey(widget.getMessage(),
                    "selectWorld.allowCommands")
                    && widget instanceof CycleButton<?> cycle) {
                commands = cycle;
                vanilla.put(widget, true);
            } else if (hasTranslationKey(widget.getMessage(), "lanServer.port")
                    && widget instanceof EditBox editBox) {
                port = editBox;
                vanilla.put(widget, true);
            }
        }

        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractWidget widget)
                    || vanilla.containsKey(widget)) {
                continue;
            }
            extras.add(new WidgetPlacement(widget, widget.getX(), widget.getY(),
                    widget.getWidth(), widget.getHeight()));
        }
        extras.sort(Comparator.comparingInt((WidgetPlacement p) -> p.originalY)
                .thenComparingInt(p -> p.originalX));
        return new Backend(screen, width, height, start, gameMode, commands,
                port, extras);
    }

    private static void importBackendToCustom(Backend backend,
            Object nativeState) {
        if (nativeState == null) return;
        try {
            if (backend.gameMode != null
                    && backend.gameMode.getValue() instanceof GameType gameType) {
                writeField(nativeState, "lanGameType", gameType);
            }
            if (backend.commands != null
                    && backend.commands.getValue() instanceof Boolean commands) {
                writeField(nativeState, "lanCheats", commands);
            }
            if (backend.port != null) {
                Object customPort = readField(nativeState, "lanPort");
                if (customPort instanceof EditBox editBox
                        && !backend.port.getValue().isBlank()) {
                    editBox.setValue(backend.port.getValue());
                }
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not import LAN values from modded vanilla screen",
                    exception);
        }
    }

    private static void syncBackendFromCustom(Backend backend,
            Object nativeState) {
        if (nativeState == null) return;
        try {
            Object gameType = readField(nativeState, "lanGameType");
            syncCycle(backend.gameMode, gameType);
            Object cheats = readField(nativeState, "lanCheats");
            syncCycle(backend.commands, cheats);
            Object customPort = readField(nativeState, "lanPort");
            if (backend.port != null && customPort instanceof EditBox editBox
                    && !Objects.equals(backend.port.getValue(), editBox.getValue())) {
                backend.port.setValue(editBox.getValue());
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not synchronize custom LAN values", exception);
        }
    }

    private static void syncCycle(CycleButton<?> cycle, Object desired) {
        if (cycle == null || desired == null) return;
        for (int guard = 0; guard < 12
                && !Objects.equals(cycle.getValue(), desired); guard++) {
            cycle.onPress();
        }
    }

    private static boolean startThroughBackend(CustomPauseMenuScreen parent,
            Object nativeState, Backend backend) {
        int port = customPort(nativeState);
        if (port < 1024 || port > 65535) {
            setLanStatus(nativeState, "Port must be between 1024 and 65535.");
            return true;
        }
        if (!backend.startButton.active) {
            setLanStatus(nativeState,
                    "Check the additional LAN options before starting.");
            return true;
        }

        syncBackendFromCustom(backend, nativeState);
        PORT_OVERRIDE.set(port);
        try {
            backend.startButton.onPress();
            backend.screen.removed();
        } finally {
            PORT_OVERRIDE.remove();
            BACKENDS.remove(parent);
        }
        return true;
    }

    private static int customPort(Object nativeState) {
        try {
            Object value = readField(nativeState, "lanPort");
            if (value instanceof EditBox editBox) {
                return Integer.parseInt(editBox.getValue());
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static void setLanStatus(Object nativeState, String message) {
        try {
            writeField(nativeState, "lanStatus", message);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static ExtraLayout extraLayout(CustomPauseMenuScreen parent,
            Object nativeState, Backend backend) {
        if (backend.extras.isEmpty()) return null;

        Object nativeLayout = null;
        try {
            nativeLayout = readField(nativeState, "layout");
        } catch (ReflectiveOperationException ignored) {
        }
        int nativeX = parent.width / 2;
        int nativeY = Math.max(24, Math.round(parent.height * 0.235F));
        int nativeWidth = 360;
        int nativeHeight = Mth.clamp(Math.round(parent.height * 0.53F),
                220, 300);
        if (nativeLayout != null) {
            nativeX = intField(nativeLayout, "x", nativeX);
            nativeY = intField(nativeLayout, "y", nativeY);
            nativeWidth = intField(nativeLayout, "width", nativeWidth);
            nativeHeight = intField(nativeLayout, "height", nativeHeight);
        }

        int availableRight = parent.width - (nativeX + nativeWidth) - 10;
        int availableLeft = nativeX - 10;
        int preferred = Mth.clamp(Math.round(parent.width * 0.31F),
                380, 520);
        int panelWidth;
        int panelX;
        if (availableRight >= 330) {
            panelWidth = Math.min(preferred, availableRight);
            panelX = nativeX + nativeWidth;
        } else if (availableLeft >= 330) {
            panelWidth = Math.min(preferred, availableLeft);
            panelX = nativeX - panelWidth;
        } else {
            panelWidth = Math.min(Math.max(300, parent.width - 20), preferred);
            panelX = Math.max(10, parent.width - panelWidth - 10);
        }

        return new ExtraLayout(panelX, nativeY, panelWidth, nativeHeight);
    }

    private static void layoutExtras(Backend backend, ExtraLayout layout) {
        for (WidgetPlacement placement : backend.extras) placement.rowY = -1;

        List<WidgetPlacement> visible = backend.extras.stream()
                .filter(p -> p.widget.visible)
                .sorted(Comparator.comparingInt((WidgetPlacement p) -> p.originalY)
                        .thenComparingInt(p -> p.originalX))
                .toList();
        int contentX = layout.x + 16;
        int contentWidth = Math.max(120, layout.width - 32);
        int y = layout.y + 52;
        int maxBottom = layout.y + layout.height - 12;
        int index = 0;

        while (index < visible.size()) {
            int sourceY = visible.get(index).originalY;
            List<WidgetPlacement> group = new ArrayList<>();
            while (index < visible.size()
                    && Math.abs(visible.get(index).originalY - sourceY) <= 4) {
                group.add(visible.get(index++));
            }

            boolean hasField = group.stream().anyMatch(p -> p.widget instanceof EditBox);
            if (hasField) {
                for (WidgetPlacement placement : group) {
                    if (y + 39 > maxBottom) break;
                    placement.rowY = y;
                    placement.widget.setX(contentX);
                    placement.widget.setY(y + 13);
                    placement.widget.setWidth(contentWidth);
                    placement.widget.setHeight(24);
                    if (placement.widget instanceof EditBox editBox) {
                        editBox.setBordered(false);
                    }
                    y += 43;
                }
                continue;
            }

            int groupIndex = 0;
            while (groupIndex < group.size() && y + BUTTON_HEIGHT <= maxBottom) {
                int columns = Math.min(3, group.size() - groupIndex);
                int cellWidth = Math.max(64,
                        (contentWidth - CONTENT_GAP * (columns - 1)) / columns);
                for (int column = 0; column < columns; column++) {
                    WidgetPlacement placement = group.get(groupIndex + column);
                    int x = contentX + column * (cellWidth + CONTENT_GAP);
                    placement.rowY = y;
                    placement.widget.setX(x);
                    placement.widget.setY(y);
                    placement.widget.setWidth(cellWidth);
                    placement.widget.setHeight(BUTTON_HEIGHT);
                }
                groupIndex += columns;
                y += BUTTON_HEIGHT + CONTENT_GAP;
            }
        }
    }

    private static void renderIntegratedWidget(GuiGraphics graphics, Font font,
            WidgetPlacement placement, int mouseX, int mouseY,
            float partialTick) {
        AbstractWidget widget = placement.widget;
        int x = widget.getX();
        int y = widget.getY();
        int width = widget.getWidth();
        int height = widget.getHeight();

        if (widget instanceof EditBox editBox) {
            String label = widget.getMessage().getString();
            if (!label.isBlank()) {
                graphics.drawString(font, ScpFonts.titillium(label),
                        x, placement.rowY, MUTED, false);
            }
            boolean focused = editBox.isFocused();
            graphics.fill(x, y, x + width, y + height, FIELD);
            graphics.fill(x, y, x + (focused ? 4 : 3), y + height,
                    focused ? ACCENT_BRIGHT : ACCENT);
            graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
            editBox.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        if (widget instanceof AbstractButton) {
            boolean hovered = widget.active && widget.isMouseOver(mouseX, mouseY);
            graphics.fill(x, y, x + width, y + height,
                    hovered ? ROW_HOVER : ROW);
            graphics.fill(x, y, x + (hovered ? 5 : 3), y + height,
                    widget.active ? (hovered ? ACCENT_BRIGHT : ACCENT)
                            : 0xFF4D535C);
            graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
            Component message = widget.getMessage() == null
                    ? Component.empty() : ScpFonts.roboto(widget.getMessage());
            drawFittedCentered(graphics, font, message, x + 8, y,
                    width - 16, height, widget.active ? TEXT : MUTED);
            return;
        }

        widget.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawFittedCentered(GuiGraphics graphics, Font font,
            Component text, int x, int y, int width, int height, int color) {
        int measured = Math.max(1, font.width(text));
        float scale = Math.min(1.0F, width / (float) measured);
        int scaledWidth = Math.round(measured * scale);
        int scaledHeight = Math.round(font.lineHeight * scale);
        float drawX = x + (width - scaledWidth) / 2.0F;
        float drawY = y + (height - scaledHeight) / 2.0F + 1.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(drawX, drawY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static boolean isNativeStartArea(Object nativeState,
            double mouseX, double mouseY) {
        try {
            Object layout = readField(nativeState, "layout");
            if (layout == null) return false;
            int panelX = intField(layout, "x", 0);
            int panelY = intField(layout, "y", 0);
            int panelWidth = intField(layout, "width", 0);
            int width = Mth.clamp(panelWidth - 70, 230, 390);
            int left = panelX + (panelWidth - width) / 2;
            int gameModeY = panelY + 58;
            int portY = gameModeY + 80;
            int startY = portY + 48;
            return mouseX >= left && mouseX < left + width
                    && mouseY >= startY && mouseY < startY + 34;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Object nativeState(CustomPauseMenuScreen parent) {
        try {
            Field statesField = PauseMenuNativePanelsClient.class
                    .getDeclaredField("STATES");
            statesField.setAccessible(true);
            Object value = statesField.get(null);
            if (value instanceof Map<?, ?> states) return states.get(parent);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static boolean isLanActive(Object nativeState) {
        if (nativeState == null) return false;
        try {
            Object open = readField(nativeState, "open");
            Object mode = readField(nativeState, "mode");
            return Boolean.TRUE.equals(open)
                    && mode != null && "OPEN_TO_LAN".equals(mode.toString());
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean hasTranslationKey(Component component, String key) {
        if (component == null) return false;
        if (component.getContents() instanceof TranslatableContents translated) {
            if (key.equals(translated.getKey())) return true;
            for (Object argument : translated.getArgs()) {
                if (argument instanceof Component nested
                        && hasTranslationKey(nested, key)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (hasTranslationKey(sibling, key)) return true;
        }
        return false;
    }

    private static Object readField(Object target, String name)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void writeField(Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static int intField(Object target, String name, int fallback) {
        try {
            Object value = readField(target, name);
            return value instanceof Number number ? number.intValue() : fallback;
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private static final class Backend {
        private final ShareToLanScreen screen;
        private final int width;
        private final int height;
        private final Button startButton;
        private final CycleButton<?> gameMode;
        private final CycleButton<?> commands;
        private final EditBox port;
        private final List<WidgetPlacement> extras;

        private Backend(ShareToLanScreen screen, int width, int height,
                Button startButton, CycleButton<?> gameMode,
                CycleButton<?> commands, EditBox port,
                List<WidgetPlacement> extras) {
            this.screen = screen;
            this.width = width;
            this.height = height;
            this.startButton = startButton;
            this.gameMode = gameMode;
            this.commands = commands;
            this.port = port;
            this.extras = List.copyOf(extras);
        }
    }

    private static final class WidgetPlacement {
        private final AbstractWidget widget;
        private final int originalX;
        private final int originalY;
        @SuppressWarnings("unused")
        private final int originalWidth;
        @SuppressWarnings("unused")
        private final int originalHeight;
        private int rowY = -1;

        private WidgetPlacement(AbstractWidget widget, int originalX,
                int originalY, int width, int height) {
            this.widget = widget;
            this.originalX = originalX;
            this.originalY = originalY;
            this.originalWidth = width;
            this.originalHeight = height;
        }
    }

    private record ExtraLayout(int x, int y, int width, int height) {
    }
}
