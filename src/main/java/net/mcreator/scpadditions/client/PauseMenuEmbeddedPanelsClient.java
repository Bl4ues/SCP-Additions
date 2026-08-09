package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hosts the real vanilla Achievements, Statistics and Open-to-LAN screens in a
 * clipped panel inside the custom pause screen. Keeping the actual Screen
 * instances preserves their behavior and mixin-based mod compatibility while
 * avoiding a full-screen visual jump.
 */
public final class PauseMenuEmbeddedPanelsClient {
    private static final int PANEL = 0xE20B0E12;
    private static final int ACCENT = 0xFFC99B18;
    private static final int BORDER = 0x70414A56;

    private static final Map<CustomPauseMenuScreen, State> STATES =
            new WeakHashMap<>();

    private PauseMenuEmbeddedPanelsClient() {
    }

    public enum Mode {
        ACHIEVEMENTS(
                "net.minecraft.client.gui.screens.advancements.AdvancementsScreen"),
        STATISTICS(
                "net.minecraft.client.gui.screens.achievement.StatsScreen"),
        OPEN_TO_LAN(
                "net.minecraft.client.gui.screens.ShareToLanScreen");

        private final String className;

        Mode(String className) {
            this.className = className;
        }
    }

    public static boolean toggle(CustomPauseMenuScreen parent, Mode mode) {
        State state = STATES.computeIfAbsent(parent, ignored -> new State());
        if (state.open && state.mode == mode) {
            close(parent);
            return true;
        }

        Screen embedded = createScreen(parent, mode);
        if (embedded == null) return false;

        state.mode = mode;
        state.screen = embedded;
        state.open = true;
        state.progress = Math.max(state.progress, 0.08F);
        state.initializedWidth = -1;
        state.initializedHeight = -1;
        return true;
    }

    public static void close(CustomPauseMenuScreen parent) {
        State state = STATES.get(parent);
        if (state != null) state.open = false;
    }

    public static boolean isOpen(CustomPauseMenuScreen parent) {
        State state = STATES.get(parent);
        return state != null && (state.open || state.progress > 0.02F);
    }

    public static void tick(CustomPauseMenuScreen parent) {
        State state = STATES.get(parent);
        if (state == null || !state.open || state.screen == null) return;
        try {
            state.screen.tick();
        } catch (Throwable throwable) {
            ScpAdditionsMod.LOGGER.warn(
                    "Embedded pause panel failed to tick; closing it",
                    throwable);
            state.open = false;
        }
    }

    public static void render(CustomPauseMenuScreen parent,
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            long now, int baseX, int baseY, int menuWidth, int rowHeight,
            int gap) {
        State state = STATES.computeIfAbsent(parent, ignored -> new State());
        if (state.lastFrameAt == 0L) state.lastFrameAt = now;
        float delta = Math.min(0.10F,
                Math.max(0.0F, (now - state.lastFrameAt) / 1000.0F));
        state.lastFrameAt = now;

        float target = state.open ? 1.0F : 0.0F;
        state.progress = approach(state.progress, target, delta * 7.0F);
        if (!state.open && state.progress <= 0.01F) {
            state.screen = null;
            state.mode = null;
            return;
        }
        if (state.screen == null) return;

        int availableWidth = Math.max(260, parent.width - baseX - 30);
        int panelWidth = Mth.clamp(availableWidth, 300, 620);
        int panelHeight = Mth.clamp(parent.height - 64, 220, 470);
        int panelY = Math.max(24, (parent.height - panelHeight) / 2);

        float eased = smootherStep(state.progress);
        int panelX = baseX + Math.round((1.0F - eased) * -44.0F);
        int alpha = Math.round(255.0F * eased);

        graphics.fill(panelX, panelY, panelX + panelWidth,
                panelY + panelHeight, applyAlpha(PANEL, eased));
        graphics.fill(panelX, panelY, panelX + panelWidth,
                panelY + 3, applyAlpha(ACCENT, eased));
        graphics.fill(panelX, panelY + panelHeight - 1,
                panelX + panelWidth, panelY + panelHeight,
                applyAlpha(BORDER, eased));

        int inset = 3;
        int contentX = panelX + inset;
        int contentY = panelY + 3;
        int contentWidth = Math.max(1, panelWidth - inset * 2);
        int contentHeight = Math.max(1, panelHeight - 6);
        state.layout = new Layout(contentX, contentY,
                contentWidth, contentHeight);

        ensureInitialized(state, parent, contentWidth, contentHeight);

        int localMouseX = mouseX - contentX;
        int localMouseY = mouseY - contentY;
        graphics.enableScissor(contentX, contentY,
                contentX + contentWidth, contentY + contentHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(contentX, contentY, 0.0F);
        try {
            state.screen.render(graphics, localMouseX, localMouseY,
                    partialTick);
        } catch (Throwable throwable) {
            ScpAdditionsMod.LOGGER.warn(
                    "Embedded pause panel failed to render; closing it",
                    throwable);
            state.open = false;
        }
        graphics.pose().popPose();
        graphics.disableScissor();

        // A tiny exterior border remains authored by SCP Additions even when
        // the contained screen is replaced or restyled by another mod.
        graphics.fill(panelX, panelY, panelX + 2,
                panelY + panelHeight, applyAlpha(ACCENT, eased));
    }

    public static boolean mouseClicked(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        State state = activeState(parent);
        if (state == null) return false;
        Layout layout = state.layout;
        if (!layout.contains(mouseX, mouseY)) return false;
        return state.screen.mouseClicked(mouseX - layout.x,
                mouseY - layout.y, button);
    }

    public static boolean mouseReleased(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        State state = activeState(parent);
        if (state == null) return false;
        Layout layout = state.layout;
        if (!layout.contains(mouseX, mouseY)) return false;
        return state.screen.mouseReleased(mouseX - layout.x,
                mouseY - layout.y, button);
    }

    public static boolean mouseDragged(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        State state = activeState(parent);
        if (state == null) return false;
        Layout layout = state.layout;
        if (!layout.contains(mouseX, mouseY)) return false;
        return state.screen.mouseDragged(mouseX - layout.x,
                mouseY - layout.y, button, dragX, dragY);
    }

    public static boolean mouseScrolled(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, double delta) {
        State state = activeState(parent);
        if (state == null) return false;
        Layout layout = state.layout;
        if (!layout.contains(mouseX, mouseY)) return false;
        return state.screen.mouseScrolled(mouseX - layout.x,
                mouseY - layout.y, delta);
    }

    public static boolean keyPressed(CustomPauseMenuScreen parent,
            int keyCode, int scanCode, int modifiers) {
        State state = activeState(parent);
        if (state == null) return false;
        if (keyCode == 256) {
            close(parent);
            return true;
        }
        return state.screen.keyPressed(keyCode, scanCode, modifiers);
    }

    public static boolean keyReleased(CustomPauseMenuScreen parent,
            int keyCode, int scanCode, int modifiers) {
        State state = activeState(parent);
        return state != null
                && state.screen.keyReleased(keyCode, scanCode, modifiers);
    }

    public static boolean charTyped(CustomPauseMenuScreen parent,
            char codePoint, int modifiers) {
        State state = activeState(parent);
        return state != null && state.screen.charTyped(codePoint, modifiers);
    }

    private static State activeState(CustomPauseMenuScreen parent) {
        State state = STATES.get(parent);
        if (state == null || !state.open || state.progress < 0.78F
                || state.screen == null || state.layout == null) {
            return null;
        }
        return state;
    }

    private static Screen createScreen(CustomPauseMenuScreen parent,
            Mode mode) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            List<Object> candidates = new ArrayList<>();
            candidates.add(parent);
            candidates.add(minecraft);
            candidates.add(minecraft.options);
            if (minecraft.player != null) {
                candidates.add(minecraft.player);
                Object stats = invokeNoArg(minecraft.player, "getStats");
                if (stats != null) candidates.add(stats);
            }
            if (minecraft.getConnection() != null) {
                candidates.add(minecraft.getConnection());
                Object advancements = invokeNoArg(
                        minecraft.getConnection(), "getAdvancements");
                if (advancements != null) candidates.add(advancements);
            }

            Class<?> type = Class.forName(mode.className);
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                Object[] args = resolveArguments(constructor.getParameterTypes(),
                        candidates);
                if (args == null) continue;
                constructor.setAccessible(true);
                Object instance = constructor.newInstance(args);
                if (instance instanceof Screen screen) return screen;
            }
        } catch (Throwable throwable) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not build embedded pause panel {}", mode,
                    throwable);
        }
        return null;
    }

    private static Object[] resolveArguments(Class<?>[] parameterTypes,
            List<Object> candidates) {
        Object[] args = new Object[parameterTypes.length];
        boolean[] used = new boolean[candidates.size()];
        for (int parameter = 0; parameter < parameterTypes.length; parameter++) {
            Class<?> expected = parameterTypes[parameter];
            int found = -1;
            for (int candidate = 0; candidate < candidates.size(); candidate++) {
                Object value = candidates.get(candidate);
                if (!used[candidate] && value != null
                        && expected.isInstance(value)) {
                    found = candidate;
                    break;
                }
            }
            if (found < 0) return null;
            used[found] = true;
            args[parameter] = candidates.get(found);
        }
        return args;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void ensureInitialized(State state,
            CustomPauseMenuScreen parent, int width, int height) {
        if (state.screen == null
                || state.initializedWidth == width
                && state.initializedHeight == height) {
            return;
        }
        try {
            Method init = Screen.class.getDeclaredMethod("init",
                    Minecraft.class, int.class, int.class);
            init.setAccessible(true);
            init.invoke(state.screen, Minecraft.getInstance(), width, height);
            state.initializedWidth = width;
            state.initializedHeight = height;
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not initialize embedded pause panel", exception);
            state.open = false;
        }
    }

    private static int applyAlpha(int color, float alpha) {
        int sourceAlpha = color >>> 24;
        int finalAlpha = Mth.clamp(Math.round(sourceAlpha
                * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return (finalAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float approach(float current, float target, float amount) {
        if (current < target) return Math.min(target, current + amount);
        if (current > target) return Math.max(target, current - amount);
        return current;
    }

    private static final class State {
        private Mode mode;
        private Screen screen;
        private boolean open;
        private float progress;
        private long lastFrameAt;
        private int initializedWidth = -1;
        private int initializedHeight = -1;
        private Layout layout;
    }

    private record Layout(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }
}
