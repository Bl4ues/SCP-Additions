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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Compact Settings flyout used only by the custom title screen. It probes the
 * real vanilla OptionsScreen so Forge/mod-injected option buttons are preserved
 * automatically, while the in-game pause/options flow remains untouched.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MainMenuSettingsPanelClient {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int BUTTON_BASE = 0x7A0B0E12;
    private static final int BUTTON_HOVER = 0xB5161B22;
    private static final int TRACK = 0xFF414750;
    private static final int TRACK_FAINT = 0x80303944;

    private static final int MAX_VISIBLE_ROWS = 6;
    private static final int MIN_FOV = 30;
    private static final int MAX_FOV = 110;

    private static final Map<CustomMainMenuScreen, State> STATES =
            new WeakHashMap<>();

    private static boolean optionsReturnArmed;
    private static boolean reopenSettingsOnNextMenu;

    private MainMenuSettingsPanelClient() {
    }

    public static void attach(CustomMainMenuScreen screen) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        renameOptionsButton(screen);
        if (reopenSettingsOnNextMenu) {
            reopenSettingsOnNextMenu = false;
            state.open = true;
            state.progress = 1.0F;
            ensureEntries(screen, state);
        }
    }

    public static void render(CustomMainMenuScreen screen,
            GuiGraphics graphics, int mouseX, int mouseY) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        renameOptionsButton(screen);

        long now = Util.getMillis();
        float delta = state.lastFrameAt == 0L ? 0.0F
                : Math.min(0.10F, Math.max(0.0F,
                (now - state.lastFrameAt) / 1000.0F));
        state.lastFrameAt = now;

        float target = state.open ? 1.0F : 0.0F;
        state.progress = approach(state.progress, target, delta * 7.0F);
        if (state.progress <= 0.01F && !state.open) {
            state.hoveredToken = Integer.MIN_VALUE;
            return;
        }

        ensureEntries(screen, state);
        Layout layout = layout(screen, state);
        float eased = smootherStep(state.progress);
        int slide = Math.round((1.0F - eased) * 32.0F);
        int panelX = layout.x - slide;
        float alpha = eased;

        int hovered = Integer.MIN_VALUE;
        if (layout.fovContains(mouseX, mouseY, panelX)) hovered = -1;

        drawFovSlider(graphics, screen, layout, panelX, alpha,
                hovered == -1);

        int maxOffset = Math.max(0,
                state.entries.size() - layout.visibleRows);
        state.scrollOffset = Mth.clamp(state.scrollOffset, 0, maxOffset);

        for (int row = 0; row < layout.visibleRows; row++) {
            int index = state.scrollOffset + row;
            if (index >= state.entries.size()) break;
            int rowY = layout.listY + row * (layout.rowHeight + layout.gap);
            boolean isHovered = layout.rowContains(mouseX, mouseY,
                    panelX, rowY);
            if (isHovered) hovered = index;
            drawRow(graphics, state.entries.get(index), panelX, rowY,
                    layout.width, layout.rowHeight, alpha, isHovered);
        }

        if (maxOffset > 0) {
            drawScrollbar(graphics, layout, panelX,
                    state.scrollOffset, maxOffset, alpha);
        }

        if (state.open && state.progress > 0.85F
                && hovered != state.hoveredToken) {
            state.hoveredToken = hovered;
            if (hovered != Integer.MIN_VALUE) playHover();
        } else if (hovered == Integer.MIN_VALUE) {
            state.hoveredToken = Integer.MIN_VALUE;
        }
    }

    public static boolean shouldReplaceOptionsReturn(Screen incoming) {
        if (!optionsReturnArmed || incoming == null) return false;
        if (!"net.minecraft.client.gui.screens.OptionsScreen"
                .equals(incoming.getClass().getName())) {
            return false;
        }
        optionsReturnArmed = false;
        reopenSettingsOnNextMenu = true;
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CustomMainMenuScreen screen)) return;
        if (!ClientModulePreferences.customMainMenuEnabled()) return;
        if (event.getButton() != 0) return;

        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        renameOptionsButton(screen);

        AbstractButton settingsButton = findNamedButton(screen, "Settings");
        if (settingsButton != null
                && settingsButton.isMouseOver(event.getMouseX(), event.getMouseY())) {
            state.open = !state.open;
            if (state.open) {
                ensureEntries(screen, state);
                closeExtras(screen);
            }
            playSelect();
            event.setCanceled(true);
            return;
        }

        AbstractButton extrasButton = findNamedButton(screen, "Extras");
        if (state.open && extrasButton != null
                && extrasButton.isMouseOver(event.getMouseX(), event.getMouseY())) {
            state.open = false;
            return;
        }

        if (!state.open || state.progress < 0.78F) return;
        Layout layout = layout(screen, state);
        int panelX = layout.x;

        if (layout.fovContains(event.getMouseX(), event.getMouseY(), panelX)) {
            updateFov(screen, layout, panelX, event.getMouseX());
            state.draggingFov = true;
            playSelect();
            event.setCanceled(true);
            return;
        }

        int row = layout.rowAt(event.getMouseX(), event.getMouseY(), panelX);
        if (row < 0) return;
        int index = state.scrollOffset + row;
        if (index < 0 || index >= state.entries.size()) return;

        Entry entry = state.entries.get(index);
        if (entry.source == null || !entry.source.active) return;
        playSelect();
        beginTransition(screen, entry.source);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!(event.getScreen() instanceof CustomMainMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.draggingFov) return;

        Layout layout = layout(screen, state);
        updateFov(screen, layout, layout.x, event.getMouseX());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof CustomMainMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.draggingFov) return;
        state.draggingFov = false;
        Minecraft.getInstance().options.save();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof CustomMainMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.open || state.progress < 0.78F) return;

        Layout layout = layout(screen, state);
        if (!layout.panelContains(event.getMouseX(), event.getMouseY(), layout.x)) {
            return;
        }

        int maxOffset = Math.max(0,
                state.entries.size() - layout.visibleRows);
        if (maxOffset <= 0 || event.getScrollDelta() == 0.0D) return;

        int direction = event.getScrollDelta() > 0.0D ? -1 : 1;
        state.scrollOffset = Mth.clamp(state.scrollOffset + direction,
                0, maxOffset);
        event.setCanceled(true);
    }

    private static void ensureEntries(CustomMainMenuScreen screen, State state) {
        if (state.entriesBuilt) return;
        state.entriesBuilt = true;

        try {
            Screen probe = createOptionsProbe(screen);
            if (probe == null) return;

            List<Entry> discovered = new ArrayList<>();
            int sequence = 0;
            for (GuiEventListener listener : probe.children()) {
                if (!(listener instanceof AbstractButton button)) continue;
                String raw = button.getMessage().getString().trim();
                String key = translationKey(button.getMessage());
                if (raw.isBlank() || isDoneButton(key, raw)) continue;

                Classification classification = classify(key, raw, sequence++);
                discovered.add(new Entry(classification.label,
                        classification.order, button));
            }

            discovered.sort(Comparator.comparingInt(Entry::order));
            state.entries.addAll(discovered);
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not collect the title-screen Settings options",
                    exception);
        }
    }

    private static Screen createOptionsProbe(CustomMainMenuScreen parent)
            throws ReflectiveOperationException {
        Minecraft minecraft = Minecraft.getInstance();
        Class<?> type = Class.forName(
                "net.minecraft.client.gui.screens.OptionsScreen");

        Object instance = null;
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 2
                    || !Screen.class.isAssignableFrom(parameters[0])
                    || !parameters[1].isInstance(minecraft.options)) {
                continue;
            }
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

    private static Classification classify(String key, String raw,
            int sequence) {
        String normalized = (key + " " + raw).toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "video", "graphics")) {
            return new Classification(0, "Graphics");
        }
        if (containsAny(normalized, "sound", "audio")) {
            return new Classification(10, "Audio");
        }
        if (containsAny(normalized, "control", "keybind")) {
            return new Classification(20, "Controls");
        }
        if (containsAny(normalized, "resource", "pack")) {
            return new Classification(30, "Resource Packs");
        }
        if (containsAny(normalized, "online")) {
            return new Classification(40, "Online");
        }
        if (containsAny(normalized, "language")) {
            return new Classification(50, "Language");
        }
        if (containsAny(normalized, "accessib")) {
            return new Classification(60, "Accessibility");
        }
        if (containsAny(normalized, "skin")) {
            return new Classification(70, "Skin Customization");
        }
        if (containsAny(normalized, "telemetr")) {
            return new Classification(80, "Telemetry");
        }
        if (containsAny(normalized, "credit", "attribution")) {
            return new Classification(90, "Credits & Attribution");
        }
        if (containsAny(normalized, "chat")) {
            return new Classification(100, "Chat");
        }
        return new Classification(1000 + sequence, stripEllipsis(raw));
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private static boolean isDoneButton(String key, String raw) {
        String normalized = (key + " " + raw).toLowerCase(Locale.ROOT);
        return normalized.contains("gui.done")
                || normalized.equals(" done")
                || raw.equalsIgnoreCase("Done");
    }

    private static String stripEllipsis(String text) {
        String value = text == null ? "" : text.trim();
        while (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    private static void renameOptionsButton(CustomMainMenuScreen screen) {
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractButton button)) continue;
            String key = translationKey(button.getMessage());
            String text = button.getMessage().getString();
            if ("menu.options".equals(key)
                    || "Options...".equals(text)
                    || "Options".equals(text)) {
                button.setMessage(ScpFonts.roboto("Settings"));
                return;
            }
        }
    }

    private static AbstractButton findNamedButton(CustomMainMenuScreen screen,
            String name) {
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractButton button
                    && button.visible && button.active
                    && name.equals(button.getMessage().getString())) {
                return button;
            }
        }
        return null;
    }

    private static void closeExtras(CustomMainMenuScreen screen) {
        try {
            Field field = CustomMainMenuScreen.class
                    .getDeclaredField("extrasOpen");
            field.setAccessible(true);
            field.setBoolean(screen, false);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.debug(
                    "Could not close the Extras flyout while opening Settings",
                    exception);
        }
    }

    private static void beginTransition(CustomMainMenuScreen screen,
            AbstractButton source) {
        try {
            Method method = CustomMainMenuScreen.class.getDeclaredMethod(
                    "beginScreenTransition", Runnable.class);
            method.setAccessible(true);
            Runnable action = () -> {
                Minecraft minecraft = Minecraft.getInstance();
                Screen before = minecraft.screen;
                source.onPress();
                if (minecraft.screen != null && minecraft.screen != before) {
                    optionsReturnArmed = true;
                }
            };
            method.invoke(screen, action);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not run the Settings menu transition", exception);
            source.onPress();
        }
    }

    private static void updateFov(CustomMainMenuScreen screen, Layout layout,
            int panelX, double mouseX) {
        Minecraft minecraft = Minecraft.getInstance();
        int trackLeft = panelX + 78;
        int trackRight = panelX + layout.width - 18;
        double normalized = Mth.clamp(
                (mouseX - trackLeft) / Math.max(1.0D, trackRight - trackLeft),
                0.0D, 1.0D);
        int fov = Mth.clamp((int) Math.round(
                Mth.lerp(normalized, MIN_FOV, MAX_FOV)),
                MIN_FOV, MAX_FOV);
        minecraft.options.fov().set(fov);
    }

    private static void drawFovSlider(GuiGraphics graphics,
            CustomMainMenuScreen screen, Layout layout, int x,
            float alpha, boolean hovered) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int y = layout.y;
        int background = applyAlpha(hovered ? BUTTON_HOVER : BUTTON_BASE, alpha);
        graphics.fill(x, y, x + layout.width, y + layout.rowHeight, background);
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

        int trackLeft = x + 78;
        int trackRight = x + layout.width - 42;
        int trackY = y + layout.rowHeight / 2;
        graphics.fill(trackLeft, trackY - 1, trackRight, trackY + 1,
                applyAlpha(TRACK_FAINT, alpha));

        float normalized = Mth.clamp((fov - MIN_FOV)
                / (float) (MAX_FOV - MIN_FOV), 0.0F, 1.0F);
        int head = Math.round(Mth.lerp(normalized, trackLeft, trackRight));
        graphics.fill(trackLeft, trackY - 1, head, trackY + 1,
                applyAlpha(ACCENT, alpha));
        graphics.fill(head - 3, trackY - 3, head + 3, trackY + 3,
                applyAlpha(hovered ? ACCENT_BRIGHT : ACCENT, alpha));
    }

    private static void drawRow(GuiGraphics graphics, Entry entry,
            int x, int y, int width, int height, float alpha,
            boolean hovered) {
        Font font = Minecraft.getInstance().font;
        graphics.fill(x, y, x + width, y + height,
                applyAlpha(hovered ? BUTTON_HOVER : BUTTON_BASE, alpha));
        graphics.fill(x, y, x + 4, y + height,
                applyAlpha(ACCENT, alpha));

        Component text = ScpFonts.roboto(entry.label);
        int textY = y + (height - font.lineHeight) / 2;
        graphics.drawString(font, text, x + 13, textY,
                applyAlpha(hovered ? ACCENT_BRIGHT : TEXT, alpha), false);
    }

    private static void drawScrollbar(GuiGraphics graphics, Layout layout,
            int x, int offset, int maxOffset, float alpha) {
        int trackX = x + layout.width - 5;
        int top = layout.listY;
        int bottom = layout.listBottom;
        int trackHeight = Math.max(8, bottom - top);
        graphics.fill(trackX, top, trackX + 2, bottom,
                applyAlpha(TRACK_FAINT, alpha));

        int total = Math.max(1, layout.visibleRows + maxOffset);
        float visibleFraction = layout.visibleRows / (float) total;
        int thumbHeight = Math.max(12,
                Math.round(trackHeight * visibleFraction));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = top + Math.round(travel
                * (offset / (float) Math.max(1, maxOffset)));
        graphics.fill(trackX, thumbY, trackX + 2,
                thumbY + thumbHeight, applyAlpha(ACCENT, alpha));
    }

    private static Layout layout(CustomMainMenuScreen screen, State state) {
        int primaryLeft = Math.max(42, Math.round(screen.width * 0.073F));
        int primaryWidth = Mth.clamp(Math.round(screen.width * 0.265F),
                220, 330);
        int x = primaryLeft + primaryWidth + 16;
        int y = Math.round(screen.height * 0.385F);
        int width = Mth.clamp(Math.round(screen.width * 0.205F),
                168, 235);
        int rowHeight = Mth.clamp(Math.round(screen.height * 0.052F),
                26, 32);
        int gap = 6;

        int available = Math.max(rowHeight,
                screen.height - y - Math.max(32,
                        Math.round(screen.height * 0.055F)));
        int visibleRows = Math.min(MAX_VISIBLE_ROWS,
                Math.max(3, (available - rowHeight - gap)
                        / Math.max(1, rowHeight + gap)));
        visibleRows = Math.min(visibleRows,
                Math.max(1, state.entries.size()));

        int listY = y + rowHeight + gap;
        int listBottom = listY
                + visibleRows * rowHeight
                + Math.max(0, visibleRows - 1) * gap;
        return new Layout(x, y, width, rowHeight, gap,
                visibleRows, listY, listBottom);
    }

    private static String translationKey(Component component) {
        if (component != null
                && component.getContents() instanceof TranslatableContents t) {
            return t.getKey();
        }
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
                    || mouseY < listY || mouseY >= listBottom) {
                return -1;
            }
            int slot = (int) ((mouseY - listY) / (rowHeight + gap));
            if (slot < 0 || slot >= visibleRows) return -1;
            int rowY = listY + slot * (rowHeight + gap);
            return mouseY < rowY + rowHeight ? slot : -1;
        }

        private boolean panelContains(double mouseX, double mouseY,
                int actualX) {
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
        private int hoveredToken = Integer.MIN_VALUE;
    }
}
