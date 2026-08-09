package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.MainMenuSounds;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Inline Settings navigation used by the custom pause screen. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class PauseMenuSettingsPanelClient {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int BUTTON_BASE = 0x780B0E12;
    private static final int BUTTON_HOVER = 0xB5161B22;
    private static final int TRACK = 0xFF414750;
    private static final int TRACK_FAINT = 0x66323A47;

    private static final int MIN_FOV = 30;
    private static final int MAX_FOV = 110;
    private static final int MAX_VISIBLE_ROWS = 7;

    private static final Map<CustomPauseMenuScreen, State> STATES =
            new WeakHashMap<>();
    private static boolean optionsReturnArmed;
    private static boolean reopenOnNextPause;

    private PauseMenuSettingsPanelClient() {
    }

    public static void toggle(CustomPauseMenuScreen screen) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        state.open = !state.open;
        if (state.open) ensureEntries(screen, state);
    }

    public static void close(CustomPauseMenuScreen screen) {
        State state = STATES.get(screen);
        if (state != null) state.open = false;
    }

    public static boolean shouldReplaceOptionsReturn(Screen incoming) {
        if (!optionsReturnArmed || incoming == null) return false;
        if (!"net.minecraft.client.gui.screens.OptionsScreen"
                .equals(incoming.getClass().getName())) {
            return false;
        }
        optionsReturnArmed = false;
        reopenOnNextPause = true;
        return true;
    }

    public static void restoreIfRequested(CustomPauseMenuScreen screen) {
        if (!reopenOnNextPause) return;
        reopenOnNextPause = false;
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        state.open = true;
        state.progress = 1.0F;
        ensureEntries(screen, state);
    }

    public static void render(CustomPauseMenuScreen screen,
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            long now, int baseX, int baseY, int width, int rowHeight,
            int gap) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        if (state.lastFrameAt == 0L) state.lastFrameAt = now;
        float delta = Math.min(0.10F,
                Math.max(0.0F, (now - state.lastFrameAt) / 1000.0F));
        state.lastFrameAt = now;

        float target = state.open ? 1.0F : 0.0F;
        state.progress = approach(state.progress, target, delta * 7.2F);
        if (!state.open && state.progress <= 0.01F) {
            state.hovered = Integer.MIN_VALUE;
            return;
        }

        ensureEntries(screen, state);
        Layout layout = layout(screen, state, baseX, baseY,
                width, rowHeight, gap);
        float eased = smootherStep(state.progress);
        int x = layout.x + Math.round((1.0F - eased) * -34.0F);
        float alpha = eased;

        int hovered = Integer.MIN_VALUE;
        boolean fovHover = layout.fovContains(mouseX, mouseY, x);
        if (fovHover) hovered = -1;
        drawFov(graphics, layout, x, alpha, fovHover);

        int maxOffset = Math.max(0, state.entries.size() - layout.visibleRows);
        state.scrollOffset = Mth.clamp(state.scrollOffset, 0, maxOffset);

        for (int row = 0; row < layout.visibleRows; row++) {
            int index = state.scrollOffset + row;
            if (index >= state.entries.size()) break;
            int y = layout.listY + row * (layout.rowHeight + layout.gap);
            boolean rowHover = layout.rowContains(mouseX, mouseY, x, y);
            if (rowHover) hovered = index;
            drawRow(graphics, state.entries.get(index),
                    x, y, layout.width, layout.rowHeight,
                    alpha, rowHover);
        }

        if (maxOffset > 0) {
            drawScrollbar(graphics, layout, x,
                    state.scrollOffset, maxOffset, alpha);
        }

        if (state.open && state.progress > 0.84F
                && hovered != state.hovered) {
            state.hovered = hovered;
            if (hovered != Integer.MIN_VALUE) playHover();
        } else if (hovered == Integer.MIN_VALUE) {
            state.hovered = Integer.MIN_VALUE;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CustomPauseMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.open || state.progress < 0.78F
                || event.getButton() != 0) return;

        Layout layout = currentLayout(screen, state);
        if (layout == null) return;

        if (layout.fovContains(event.getMouseX(), event.getMouseY(), layout.x)) {
            updateFov(layout, layout.x, event.getMouseX());
            state.draggingFov = true;
            playSelect();
            event.setCanceled(true);
            return;
        }

        int row = layout.rowAt(event.getMouseX(), event.getMouseY(), layout.x);
        if (row < 0) return;
        int index = state.scrollOffset + row;
        if (index < 0 || index >= state.entries.size()) return;
        Entry entry = state.entries.get(index);
        if (entry.source == null || !entry.source.active) return;

        playSelect();
        openEntry(screen, entry.source);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof CustomPauseMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.draggingFov) return;
        Layout layout = currentLayout(screen, state);
        if (layout == null) return;
        updateFov(layout, layout.x, event.getMouseX());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof CustomPauseMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.draggingFov) return;
        state.draggingFov = false;
        Minecraft.getInstance().options.save();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof CustomPauseMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.open || state.progress < 0.78F) return;
        Layout layout = currentLayout(screen, state);
        if (layout == null
                || !layout.panelContains(event.getMouseX(),
                event.getMouseY(), layout.x)) return;

        int maxOffset = Math.max(0, state.entries.size() - layout.visibleRows);
        if (maxOffset <= 0 || event.getScrollDelta() == 0.0D) return;
        int direction = event.getScrollDelta() > 0.0D ? -1 : 1;
        state.scrollOffset = Mth.clamp(state.scrollOffset + direction,
                0, maxOffset);
        event.setCanceled(true);
    }

    private static void ensureEntries(CustomPauseMenuScreen screen,
            State state) {
        if (state.entriesBuilt) return;
        state.entriesBuilt = true;
        try {
            Screen probe = createOptionsProbe(screen);
            if (probe == null) return;
            List<Entry> entries = new ArrayList<>();
            int sequence = 0;
            for (GuiEventListener listener : probe.children()) {
                if (!(listener instanceof AbstractButton button)) continue;
                String raw = button.getMessage().getString().trim();
                String key = translationKey(button.getMessage());
                if (raw.isBlank() || isDone(key, raw)) continue;
                Classification c = classify(key, raw, sequence++);
                entries.add(new Entry(c.label, c.order, button));
            }
            entries.sort(Comparator.comparingInt(Entry::order));
            state.entries.addAll(entries);
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not collect pause-menu Settings entries", exception);
        }
    }

    private static Screen createOptionsProbe(CustomPauseMenuScreen parent)
            throws ReflectiveOperationException {
        Minecraft minecraft = Minecraft.getInstance();
        Class<?> type = Class.forName(
                "net.minecraft.client.gui.screens.OptionsScreen");
        Object instance = null;
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 2
                    || !Screen.class.isAssignableFrom(parameters[0])
                    || !parameters[1].isInstance(minecraft.options)) continue;
            constructor.setAccessible(true);
            instance = constructor.newInstance(parent, minecraft.options);
            break;
        }
        if (!(instance instanceof Screen probe)) return null;
        Method init = Screen.class.getDeclaredMethod("init",
                Minecraft.class, int.class, int.class);
        init.setAccessible(true);
        init.invoke(probe, minecraft, parent.width, parent.height);
        return probe;
    }

    private static void openEntry(CustomPauseMenuScreen screen,
            AbstractButton source) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen before = minecraft.screen;
        source.onPress();
        if (minecraft.screen != null && minecraft.screen != before) {
            optionsReturnArmed = true;
        }
    }

    private static Classification classify(String key, String raw,
            int sequence) {
        String normalized = (key + " " + raw).toLowerCase(Locale.ROOT);
        if (contains(normalized, "video", "graphics"))
            return new Classification(0, "Graphics");
        if (contains(normalized, "sound", "audio"))
            return new Classification(10, "Audio");
        if (contains(normalized, "control", "keybind"))
            return new Classification(20, "Controls");
        if (contains(normalized, "resource", "pack"))
            return new Classification(30, "Resource Packs");
        if (contains(normalized, "online"))
            return new Classification(40, "Online");
        if (contains(normalized, "language"))
            return new Classification(50, "Language");
        if (contains(normalized, "accessib"))
            return new Classification(60, "Accessibility");
        if (contains(normalized, "skin"))
            return new Classification(70, "Skin Customization");
        if (contains(normalized, "telemetr"))
            return new Classification(80, "Telemetry");
        if (contains(normalized, "credit", "attribution"))
            return new Classification(90, "Credits & Attribution");
        return new Classification(1000 + sequence, stripEllipsis(raw));
    }

    private static boolean contains(String value, String... tokens) {
        for (String token : tokens) if (value.contains(token)) return true;
        return false;
    }

    private static boolean isDone(String key, String raw) {
        return "gui.done".equals(key) || raw.equalsIgnoreCase("Done");
    }

    private static String stripEllipsis(String raw) {
        String value = raw == null ? "" : raw.trim();
        while (value.endsWith("."))
            value = value.substring(0, value.length() - 1).trim();
        return value;
    }

    private static void drawFov(GuiGraphics graphics, Layout layout,
            int x, float alpha, boolean hovered) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int y = layout.y;
        graphics.fill(x, y, x + layout.width, y + layout.rowHeight,
                applyAlpha(hovered ? BUTTON_HOVER : BUTTON_BASE, alpha));
        graphics.fill(x, y, x + 4, y + layout.rowHeight,
                applyAlpha(ACCENT, alpha));

        int fov = minecraft.options.fov().get();
        graphics.drawString(font, ScpFonts.roboto("FOV"),
                x + 13, y + (layout.rowHeight - font.lineHeight) / 2,
                applyAlpha(hovered ? ACCENT_BRIGHT : TEXT, alpha), false);

        String value = Integer.toString(fov);
        int valueWidth = font.width(ScpFonts.roboto(value));
        graphics.drawString(font, ScpFonts.roboto(value),
                x + layout.width - 11 - valueWidth,
                y + (layout.rowHeight - font.lineHeight) / 2,
                applyAlpha(TEXT, alpha), false);

        int left = x + 72;
        int right = x + layout.width - 40;
        int trackY = y + layout.rowHeight / 2;
        graphics.fill(left, trackY - 1, right, trackY + 1,
                applyAlpha(TRACK_FAINT, alpha));
        float normalized = Mth.clamp((fov - MIN_FOV)
                / (float) (MAX_FOV - MIN_FOV), 0.0F, 1.0F);
        int head = Math.round(Mth.lerp(normalized, left, right));
        graphics.fill(left, trackY - 1, head, trackY + 1,
                applyAlpha(ACCENT, alpha));
        graphics.fill(head - 3, trackY - 3, head + 3, trackY + 3,
                applyAlpha(hovered ? ACCENT_BRIGHT : ACCENT, alpha));
    }

    private static void drawRow(GuiGraphics graphics, Entry entry,
            int x, int y, int width, int height,
            float alpha, boolean hovered) {
        Font font = Minecraft.getInstance().font;
        graphics.fill(x, y, x + width, y + height,
                applyAlpha(hovered ? BUTTON_HOVER : BUTTON_BASE, alpha));
        graphics.fill(x, y, x + 4, y + height,
                applyAlpha(ACCENT, alpha));
        graphics.drawString(font, ScpFonts.roboto(entry.label),
                x + 13, y + (height - font.lineHeight) / 2,
                applyAlpha(hovered ? ACCENT_BRIGHT : TEXT, alpha), false);
    }

    private static void drawScrollbar(GuiGraphics graphics, Layout layout,
            int x, int offset, int maxOffset, float alpha) {
        int trackX = x + layout.width - 5;
        graphics.fill(trackX, layout.listY, trackX + 2, layout.listBottom,
                applyAlpha(TRACK_FAINT, alpha));
        int total = Math.max(1, layout.visibleRows + maxOffset);
        int trackHeight = Math.max(8, layout.listBottom - layout.listY);
        int thumbHeight = Math.max(12,
                Math.round(trackHeight * layout.visibleRows / (float) total));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = layout.listY + Math.round(travel
                * (offset / (float) Math.max(1, maxOffset)));
        graphics.fill(trackX, thumbY, trackX + 2,
                thumbY + thumbHeight, applyAlpha(ACCENT, alpha));
    }

    private static void updateFov(Layout layout, int x, double mouseX) {
        int left = x + 72;
        int right = x + layout.width - 40;
        double normalized = Mth.clamp((mouseX - left)
                / Math.max(1.0D, right - left), 0.0D, 1.0D);
        int fov = Mth.clamp((int) Math.round(
                Mth.lerp(normalized, MIN_FOV, MAX_FOV)),
                MIN_FOV, MAX_FOV);
        Minecraft.getInstance().options.fov().set(fov);
    }

    private static Layout currentLayout(CustomPauseMenuScreen screen,
            State state) {
        int count = pauseButtonCount(screen);
        if (count <= 0) return null;
        int width = Mth.clamp(Math.round(screen.width * 0.28F), 225, 350);
        int rowHeight = Mth.clamp(Math.round(screen.height * 0.057F), 28, 38);
        int gap = Math.max(6, Math.round(screen.height * 0.011F));
        int total = count * rowHeight + Math.max(0, count - 1) * gap;
        int primaryX = Math.max(46, Math.round(screen.width * 0.105F));
        int primaryY = Math.max(48, (screen.height - total) / 2);
        return layout(screen, state, primaryX + width + 16,
                primaryY, width, rowHeight, gap);
    }

    private static int pauseButtonCount(CustomPauseMenuScreen screen) {
        int count = 0;
        for (GuiEventListener listener : screen.children()) {
            if (listener.getClass().getName().contains("PauseMenuButton")) count++;
        }
        return count;
    }

    private static Layout layout(CustomPauseMenuScreen screen, State state,
            int x, int y, int width, int rowHeight, int gap) {
        int available = Math.max(rowHeight,
                screen.height - y - Math.max(26,
                        Math.round(screen.height * 0.05F)));
        int visibleRows = Math.min(MAX_VISIBLE_ROWS,
                Math.max(3, (available - rowHeight - gap)
                        / Math.max(1, rowHeight + gap)));
        visibleRows = Math.min(visibleRows,
                Math.max(1, state.entries.size()));
        int listY = y + rowHeight + gap;
        int listBottom = listY + visibleRows * rowHeight
                + Math.max(0, visibleRows - 1) * gap;
        return new Layout(x, y, width, rowHeight, gap,
                visibleRows, listY, listBottom);
    }

    private static String translationKey(Component component) {
        if (component != null
                && component.getContents() instanceof TranslatableContents t)
            return t.getKey();
        return "";
    }

    private static void playHover() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(MainMenuSounds.HOVER.get(), 1.0F));
    }

    private static void playSelect() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(
                        ScpAdditionsModSounds.SELECT.get(), 1.0F, 0.35F));
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

    private record Entry(String label, int order, AbstractButton source) {
    }

    private record Classification(int order, String label) {
    }

    private record Layout(int x, int y, int width, int rowHeight, int gap,
            int visibleRows, int listY, int listBottom) {
        private boolean fovContains(double mouseX, double mouseY, int actualX) {
            return mouseX >= actualX && mouseX < actualX + width
                    && mouseY >= y && mouseY < y + rowHeight;
        }

        private boolean rowContains(double mouseX, double mouseY,
                int actualX, int rowY) {
            return mouseX >= actualX && mouseX < actualX + width
                    && mouseY >= rowY && mouseY < rowY + rowHeight;
        }

        private int rowAt(double mouseX, double mouseY, int actualX) {
            if (mouseX < actualX || mouseX >= actualX + width
                    || mouseY < listY || mouseY >= listBottom) return -1;
            int slot = (int) ((mouseY - listY) / (rowHeight + gap));
            if (slot < 0 || slot >= visibleRows) return -1;
            int rowY = listY + slot * (rowHeight + gap);
            return mouseY < rowY + rowHeight ? slot : -1;
        }

        private boolean panelContains(double mouseX, double mouseY, int actualX) {
            return mouseX >= actualX && mouseX < actualX + width
                    && mouseY >= y && mouseY < listBottom;
        }
    }

    private static final class State {
        private final List<Entry> entries = new ArrayList<>();
        private boolean entriesBuilt;
        private boolean open;
        private boolean draggingFov;
        private float progress;
        private long lastFrameAt;
        private int scrollOffset;
        private int hovered = Integer.MIN_VALUE;
    }
}
