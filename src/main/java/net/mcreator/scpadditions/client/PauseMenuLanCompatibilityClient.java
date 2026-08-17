package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Hosts a real ShareToLanScreen behind the custom pause panel. Mods may keep
 * injecting their normal LAN widgets and start callbacks; only their visual
 * placement is adapted to the SCP Additions presentation.
 */
public final class PauseMenuLanCompatibilityClient {
    private static final int PANEL = 0xE20B0E12;
    private static final int BORDER = 0x70414A56;
    private static final int ACCENT = 0xFFC99B18;
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9DA5AF;

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
        graphics.drawString(Minecraft.getInstance().font,
                ScpFonts.montserrat("MOD OPTIONS"), layout.x + 14,
                layout.y + 14, TEXT, false);

        for (WidgetPlacement placement : backend.extras) {
            AbstractWidget widget = placement.widget;
            if (!widget.visible) continue;
            if (widget instanceof EditBox) {
                String label = widget.getMessage().getString();
                if (!label.isBlank()) {
                    graphics.drawString(Minecraft.getInstance().font,
                            ScpFonts.titillium(label), widget.getX(),
                            widget.getY() - 10, MUTED, false);
                }
            }
            widget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    public static boolean mouseClicked(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        Object nativeState = nativeState(parent);
        if (!isLanActive(nativeState)) return false;
        Backend backend = ensureBackend(parent, nativeState);
        if (backend == null) return false;
        syncBackendFromCustom(backend, nativeState);

        for (WidgetPlacement placement : backend.extras) {
            if (placement.widget.mouseClicked(mouseX, mouseY, button)) {
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
            if (placement.widget.mouseReleased(mouseX, mouseY, button)) {
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
            if (placement.widget.mouseDragged(mouseX, mouseY, button,
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
            if (placement.widget.mouseScrolled(mouseX, mouseY, delta)) {
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
            if (placement.widget.keyPressed(keyCode, scanCode, modifiers)) {
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
            if (placement.widget.keyReleased(keyCode, scanCode, modifiers)) {
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
            if (placement.widget.charTyped(codePoint, modifiers)) {
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
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        for (WidgetPlacement placement : backend.extras) {
            minX = Math.min(minX, placement.originalX);
            maxX = Math.max(maxX, placement.originalX + placement.width);
            minY = Math.min(minY, placement.originalY);
        }
        int panelWidth = Mth.clamp(maxX - minX + 24, 220,
                Math.max(220, parent.width - 20));
        int panelY = 10;
        int panelHeight = Math.max(120, parent.height - 20);

        Object layout = null;
        try {
            layout = readField(nativeState, "layout");
        } catch (ReflectiveOperationException ignored) {
        }
        int nativeX = parent.width / 2;
        int nativeWidth = 0;
        if (layout != null) {
            nativeX = intField(layout, "x", nativeX);
            nativeWidth = intField(layout, "width", 0);
        }

        int right = nativeX + nativeWidth + 12;
        int left = nativeX - panelWidth - 12;
        int panelX;
        if (right + panelWidth <= parent.width - 8) panelX = right;
        else if (left >= 8) panelX = left;
        else panelX = Mth.clamp(parent.width - panelWidth - 8,
                8, Math.max(8, parent.width - panelWidth - 8));

        return new ExtraLayout(panelX, panelY, panelWidth, panelHeight,
                minX, minY);
    }

    private static void layoutExtras(Backend backend, ExtraLayout layout) {
        int dx = layout.x + 12 - layout.sourceMinX;
        int dy = layout.y + 30 - layout.sourceMinY;
        for (WidgetPlacement placement : backend.extras) {
            placement.widget.setX(placement.originalX + dx);
            placement.widget.setY(placement.originalY + dy);
        }
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
        private final int width;
        private final int height;

        private WidgetPlacement(AbstractWidget widget, int originalX,
                int originalY, int width, int height) {
            this.widget = widget;
            this.originalX = originalX;
            this.originalY = originalY;
            this.width = width;
            this.height = height;
        }
    }

    private record ExtraLayout(int x, int y, int width, int height,
                               int sourceMinX, int sourceMinY) {
    }
}
