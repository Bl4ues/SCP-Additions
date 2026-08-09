package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Native SCP Additions pause panels backed by the live client state. The
 * vanilla full-screen widgets are deliberately not embedded here: advancements
 * and statistics are read from their managers, while LAN publishing calls the
 * integrated server directly.
 */
public final class PauseMenuNativePanelsClient {
    private static final int PANEL = 0xE20B0E12;
    private static final int PANEL_SOFT = 0xB812161C;
    private static final int ROW = 0x6F0B0E12;
    private static final int ROW_HOVER = 0xA91A2028;
    private static final int LAN_ROW = 0xA2181D24;
    private static final int LAN_ROW_HOVER = 0xC6242B35;
    private static final int LAN_FIELD = 0xD110141A;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9DA5AF;
    private static final int BORDER = 0x70414A56;
    private static final int TRACK = 0x563D4652;
    private static final ResourceLocation SCP_ADDITIONS_LOGO = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/logo.png");

    private static final int ACHIEVEMENT_ROW_HEIGHT = 42;
    private static final int STAT_ROW_HEIGHT = 28;
    private static final float STAT_VALUE_SCALE = 1.18F;
    private static final float LAN_OPTION_TEXT_SCALE = 1.08F;
    private static final float LAN_START_TEXT_SCALE = 1.16F;
    private static final long STATS_REFRESH_MS = 500L;

    private static final Map<CustomPauseMenuScreen, State> STATES =
            new WeakHashMap<>();

    private PauseMenuNativePanelsClient() {
    }

    public enum Mode {
        ACHIEVEMENTS,
        STATISTICS,
        OPEN_TO_LAN
    }

    private enum StatGroup {
        GENERAL("GENERAL"),
        ITEMS("ITEMS"),
        MOBS("MOBS"),
        OTHER("OTHER");

        private final String label;

        StatGroup(String label) {
            this.label = label;
        }
    }

    public static boolean toggle(CustomPauseMenuScreen parent, Mode mode) {
        State state = STATES.computeIfAbsent(parent, ignored -> new State());
        if (state.open && state.mode == mode) {
            close(parent);
            return true;
        }

        state.mode = mode;
        state.open = true;
        state.progress = Math.max(state.progress, 0.08F);
        state.layout = null;
        if (mode == Mode.ACHIEVEMENTS) {
            rebuildAchievements(state);
        } else if (mode == Mode.STATISTICS) {
            requestStatistics(state);
            rebuildStatistics(state);
        } else {
            initializeLan(state);
        }
        return true;
    }

    public static void close(CustomPauseMenuScreen parent) {
        State state = STATES.get(parent);
        if (state != null) {
            state.open = false;
            if (state.lanPort != null) state.lanPort.setFocused(false);
        }
    }

    public static boolean isOpen(CustomPauseMenuScreen parent) {
        State state = STATES.get(parent);
        return state != null && (state.open || state.progress > 0.02F);
    }

    public static void tick(CustomPauseMenuScreen parent) {
        State state = STATES.get(parent);
        if (state == null || !state.open) return;
        if (state.mode == Mode.OPEN_TO_LAN && state.lanPort != null) {
            state.lanPort.tick();
        }
        if (state.mode == Mode.STATISTICS
                && Util.getMillis() - state.lastStatsRefresh >= STATS_REFRESH_MS) {
            rebuildStatistics(state);
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
            state.mode = null;
            state.layout = null;
            return;
        }
        if (state.mode == null) return;

        int availableWidth = Math.max(270, parent.width - baseX - 30);
        int panelWidth = Mth.clamp(availableWidth, 330, 620);
        int panelHeight = state.mode == Mode.OPEN_TO_LAN
                ? Mth.clamp(Math.round(parent.height * 0.53F), 220, 300)
                : Mth.clamp(parent.height - 64, 260, 470);
        int panelY = Math.max(24, (parent.height - panelHeight) / 2);
        float eased = smootherStep(state.progress);
        int panelX = baseX + Math.round((1.0F - eased) * -44.0F);

        graphics.fill(panelX, panelY, panelX + panelWidth,
                panelY + panelHeight, applyAlpha(PANEL, eased));
        graphics.fill(panelX, panelY, panelX + panelWidth,
                panelY + 3, applyAlpha(ACCENT, eased));
        graphics.fill(panelX, panelY, panelX + 2,
                panelY + panelHeight, applyAlpha(ACCENT, eased));
        graphics.fill(panelX, panelY + panelHeight - 1,
                panelX + panelWidth, panelY + panelHeight,
                applyAlpha(BORDER, eased));

        state.layout = new Layout(panelX, panelY, panelWidth, panelHeight);
        switch (state.mode) {
            case ACHIEVEMENTS -> renderAchievements(graphics, state,
                    mouseX, mouseY, eased);
            case STATISTICS -> renderStatistics(graphics, state,
                    mouseX, mouseY, eased, now);
            case OPEN_TO_LAN -> renderLan(graphics, state,
                    mouseX, mouseY, partialTick, eased);
        }
    }

    public static boolean mouseClicked(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        State state = activeState(parent);
        if (state == null || !state.layout.contains(mouseX, mouseY)) return false;
        if (button != 0) return true;
        switch (state.mode) {
            case ACHIEVEMENTS -> handleAchievementClick(state, mouseX, mouseY);
            case STATISTICS -> handleStatisticsClick(state, mouseX, mouseY);
            case OPEN_TO_LAN -> handleLanClick(state, mouseX, mouseY);
        }
        return true;
    }

    public static boolean mouseReleased(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        State state = activeState(parent);
        return state != null && state.layout.contains(mouseX, mouseY);
    }

    public static boolean mouseDragged(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        State state = activeState(parent);
        return state != null && state.layout.contains(mouseX, mouseY);
    }

    public static boolean mouseScrolled(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, double delta) {
        State state = activeState(parent);
        if (state == null || !state.layout.contains(mouseX, mouseY)
                || delta == 0.0D) return false;
        int direction = delta > 0.0D ? -1 : 1;

        if (state.mode == Mode.ACHIEVEMENTS) {
            AchievementLayout layout = achievementLayout(state.layout);
            if (mouseX < layout.contentX) {
                int max = Math.max(0, state.achievementCategories.size()
                        - layout.visibleCategories);
                state.achievementCategoryScroll = Mth.clamp(
                        state.achievementCategoryScroll + direction, 0, max);
            } else {
                List<AdvancementRow> rows = selectedAchievementRows(state);
                int max = Math.max(0, rows.size() - layout.visibleRows);
                state.achievementScroll = Mth.clamp(
                        state.achievementScroll + direction, 0, max);
            }
        } else if (state.mode == Mode.STATISTICS) {
            StatisticsLayout layout = statisticsLayout(state.layout);
            List<StatRow> rows = statsForGroup(state);
            int max = Math.max(0, rows.size() - layout.visibleRows);
            state.statsScroll = Mth.clamp(state.statsScroll + direction,
                    0, max);
        }
        return true;
    }

    public static boolean keyPressed(CustomPauseMenuScreen parent,
            int keyCode, int scanCode, int modifiers) {
        State state = activeState(parent);
        if (state == null) return false;
        if (keyCode == 256) {
            close(parent);
            return true;
        }
        return state.mode == Mode.OPEN_TO_LAN && state.lanPort != null
                && state.lanPort.keyPressed(keyCode, scanCode, modifiers);
    }

    public static boolean keyReleased(CustomPauseMenuScreen parent,
            int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public static boolean charTyped(CustomPauseMenuScreen parent,
            char codePoint, int modifiers) {
        State state = activeState(parent);
        return state != null && state.mode == Mode.OPEN_TO_LAN
                && state.lanPort != null
                && state.lanPort.charTyped(codePoint, modifiers);
    }

    private static State activeState(CustomPauseMenuScreen parent) {
        State state = STATES.get(parent);
        return state == null || !state.open || state.progress < 0.78F
                || state.mode == null || state.layout == null ? null : state;
    }

    private static void rebuildAchievements(State state) {
        state.achievementCategories.clear();
        state.achievementScroll = 0;
        state.achievementCategoryScroll = 0;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() == null) return;
            Object manager = minecraft.getConnection().getAdvancements();
            Object list = invokeNoArg(manager, "getAdvancements");
            Object all = invokeNoArg(list, "getAllAdvancements");
            Map<?, ?> progress = findMapField(manager, "AdvancementProgress");

            Map<Object, List<AdvancementRow>> byRoot = new IdentityHashMap<>();
            List<Object> rootOrder = new ArrayList<>();
            for (Object advancement : iterable(all)) {
                Object display = displayOf(advancement);
                if (display == null) continue;
                Object root = rootOf(advancement);
                if (!byRoot.containsKey(root)) {
                    byRoot.put(root, new ArrayList<>());
                    rootOrder.add(root);
                }
                boolean done = progressDone(progress == null
                        ? null : progress.get(advancement));
                boolean hidden = booleanValue(invokeNoArg(display, "isHidden"));
                if (hidden && !done) continue;
                Component title = component(invokeNoArg(display, "getTitle"),
                        humanize(idOf(advancement)));
                Component description = component(
                        invokeNoArg(display, "getDescription"), "");
                ItemStack icon = itemStack(invokeNoArg(display, "getIcon"));
                byRoot.get(root).add(new AdvancementRow(title, description,
                        icon, done));
            }

            for (Object root : rootOrder) {
                List<AdvancementRow> rows = byRoot.get(root);
                if (rows == null || rows.isEmpty()) continue;
                rows.sort(Comparator.comparing(row ->
                        row.title.getString().toLowerCase(Locale.ROOT)));
                Object rootDisplay = displayOf(root);
                Component title = rootDisplay == null
                        ? Component.literal(humanize(idOf(root)))
                        : component(invokeNoArg(rootDisplay, "getTitle"),
                        humanize(idOf(root)));
                ItemStack icon = rootDisplay == null ? ItemStack.EMPTY
                        : itemStack(invokeNoArg(rootDisplay, "getIcon"));
                boolean useModLogo = (ScpAdditionsMod.MODID + ":scp_additions_ach")
                        .equals(idOf(root));
                int completed = (int) rows.stream().filter(row -> row.done).count();
                state.achievementCategories.add(new AdvancementCategory(
                        title, icon, useModLogo, completed, rows.size(),
                        List.copyOf(rows)));
            }
            state.achievementCategories.sort(Comparator.comparing(category ->
                    category.title.getString().toLowerCase(Locale.ROOT)));
            if (state.selectedAchievementCategory
                    >= state.achievementCategories.size()) {
                state.selectedAchievementCategory = 0;
            }
        } catch (Throwable throwable) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not build custom Achievements panel", throwable);
        }
    }

    private static void renderAchievements(GuiGraphics graphics, State state,
            int mouseX, int mouseY, float alpha) {
        Font font = Minecraft.getInstance().font;
        Layout panel = state.layout;
        AchievementLayout layout = achievementLayout(panel);
        drawHeader(graphics, panel, "ACHIEVEMENTS", alpha);
        graphics.fill(layout.sidebarX, layout.sidebarY,
                layout.sidebarX + layout.sidebarWidth, layout.sidebarBottom,
                applyAlpha(PANEL_SOFT, alpha));

        int categoryMax = Math.max(0, state.achievementCategories.size()
                - layout.visibleCategories);
        state.achievementCategoryScroll = Mth.clamp(
                state.achievementCategoryScroll, 0, categoryMax);
        for (int row = 0; row < layout.visibleCategories; row++) {
            int index = state.achievementCategoryScroll + row;
            if (index >= state.achievementCategories.size()) break;
            AdvancementCategory category = state.achievementCategories.get(index);
            int y = layout.sidebarY + row * 30;
            boolean selected = index == state.selectedAchievementCategory;
            boolean hovered = mouseX >= layout.sidebarX
                    && mouseX < layout.sidebarX + layout.sidebarWidth
                    && mouseY >= y && mouseY < y + 28;
            graphics.fill(layout.sidebarX, y,
                    layout.sidebarX + layout.sidebarWidth, y + 28,
                    applyAlpha(selected || hovered ? ROW_HOVER : ROW, alpha));
            if (selected) graphics.fill(layout.sidebarX, y,
                    layout.sidebarX + 3, y + 28, applyAlpha(ACCENT, alpha));
            if (category.useModLogo) {
                renderScpAdditionsLogo(graphics, layout.sidebarX + 7, y + 6, alpha);
            } else if (!category.icon.isEmpty()) {
                graphics.renderItem(category.icon, layout.sidebarX + 7, y + 6);
            }
            String title = compactToWidth(font, category.title.getString(),
                    layout.sidebarWidth - 50);
            graphics.drawString(font, ScpFonts.roboto(title),
                    layout.sidebarX + 29, y + 7,
                    applyAlpha(selected ? ACCENT_BRIGHT : TEXT, alpha), false);
            String count = category.completed + "/" + category.total;
            Component countText = ScpFonts.titillium(count);
            graphics.drawString(font, countText,
                    layout.sidebarX + layout.sidebarWidth - 7
                            - font.width(countText), y + 17,
                    applyAlpha(MUTED, alpha), false);
        }
        if (categoryMax > 0) drawScrollbar(graphics,
                layout.sidebarX + layout.sidebarWidth - 3,
                layout.sidebarY, layout.sidebarBottom,
                state.achievementCategoryScroll, categoryMax,
                layout.visibleCategories, alpha);

        AdvancementCategory selected = selectedCategory(state);
        List<AdvancementRow> rows = selectedAchievementRows(state);
        if (selected != null) {
            graphics.drawString(font,
                    ScpFonts.montserrat(selected.title.getString()),
                    layout.contentX, layout.contentY - 22,
                    applyAlpha(TEXT, alpha), false);
            Component progress = ScpFonts.titillium(selected.completed + " / "
                    + selected.total + " COMPLETED");
            graphics.drawString(font, progress,
                    layout.contentRight - font.width(progress),
                    layout.contentY - 21,
                    applyAlpha(ACCENT_BRIGHT, alpha), false);
        }

        int rowMax = Math.max(0, rows.size() - layout.visibleRows);
        state.achievementScroll = Mth.clamp(state.achievementScroll, 0, rowMax);
        for (int row = 0; row < layout.visibleRows; row++) {
            int index = state.achievementScroll + row;
            if (index >= rows.size()) break;
            AdvancementRow entry = rows.get(index);
            int y = layout.contentY + row * (ACHIEVEMENT_ROW_HEIGHT + 5);
            boolean hovered = mouseX >= layout.contentX
                    && mouseX < layout.contentRight
                    && mouseY >= y && mouseY < y + ACHIEVEMENT_ROW_HEIGHT;
            graphics.fill(layout.contentX, y, layout.contentRight,
                    y + ACHIEVEMENT_ROW_HEIGHT,
                    applyAlpha(hovered ? ROW_HOVER : ROW, alpha));
            graphics.fill(layout.contentX, y, layout.contentX + 3,
                    y + ACHIEVEMENT_ROW_HEIGHT,
                    applyAlpha(entry.done ? ACCENT : BORDER, alpha));
            if (!entry.icon.isEmpty()) graphics.renderItem(entry.icon,
                    layout.contentX + 9, y + 13);
            int textX = layout.contentX + 34;
            String title = compactToWidth(font, entry.title.getString(),
                    layout.contentRight - textX - 78);
            graphics.drawString(font, ScpFonts.roboto(title), textX, y + 7,
                    applyAlpha(entry.done ? ACCENT_BRIGHT : TEXT, alpha), false);
            String description = compactToWidth(font,
                    entry.description.getString(),
                    layout.contentRight - textX - 12);
            graphics.drawString(font, ScpFonts.roboto(description),
                    textX, y + 23, applyAlpha(MUTED, alpha), false);
            Component status = ScpFonts.titillium(entry.done ? "DONE" : "OPEN");
            graphics.drawString(font, status,
                    layout.contentRight - 10 - font.width(status), y + 7,
                    applyAlpha(entry.done ? ACCENT_BRIGHT : MUTED, alpha), false);
        }
        if (rowMax > 0) drawScrollbar(graphics, layout.contentRight - 3,
                layout.contentY, layout.contentBottom, state.achievementScroll,
                rowMax, layout.visibleRows, alpha);
        if (state.achievementCategories.isEmpty()) drawCenteredMessage(graphics,
                panel, "No achievements are available yet.", alpha);
    }

    private static void handleAchievementClick(State state,
            double mouseX, double mouseY) {
        AchievementLayout layout = achievementLayout(state.layout);
        if (mouseX < layout.sidebarX
                || mouseX >= layout.sidebarX + layout.sidebarWidth
                || mouseY < layout.sidebarY || mouseY >= layout.sidebarBottom)
            return;
        int row = (int) ((mouseY - layout.sidebarY) / 30.0D);
        int index = state.achievementCategoryScroll + row;
        if (index >= 0 && index < state.achievementCategories.size()) {
            state.selectedAchievementCategory = index;
            state.achievementScroll = 0;
        }
    }

    private static AchievementLayout achievementLayout(Layout panel) {
        int top = panel.y + 66;
        int bottom = panel.y + panel.height - 15;
        int sidebarX = panel.x + 14;
        int sidebarWidth = Mth.clamp(Math.round(panel.width * 0.29F), 118, 168);
        int contentX = sidebarX + sidebarWidth + 14;
        int contentRight = panel.x + panel.width - 14;
        return new AchievementLayout(sidebarX, top, sidebarWidth, bottom,
                contentX, top, contentRight, bottom,
                Math.max(1, (bottom - top) / 30),
                Math.max(1, (bottom - top) / (ACHIEVEMENT_ROW_HEIGHT + 5)));
    }

    private static AdvancementCategory selectedCategory(State state) {
        if (state.achievementCategories.isEmpty()) return null;
        return state.achievementCategories.get(Mth.clamp(
                state.selectedAchievementCategory, 0,
                state.achievementCategories.size() - 1));
    }

    private static List<AdvancementRow> selectedAchievementRows(State state) {
        AdvancementCategory category = selectedCategory(state);
        return category == null ? List.of() : category.rows;
    }

    private static void requestStatistics(State state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;
        try {
            minecraft.getConnection().send(new ServerboundClientCommandPacket(
                    ServerboundClientCommandPacket.Action.REQUEST_STATS));
            state.statsRequestedAt = Util.getMillis();
            state.statsRequested = true;
        } catch (Throwable throwable) {
            ScpAdditionsMod.LOGGER.warn("Could not request player statistics",
                    throwable);
        }
    }

    private static void rebuildStatistics(State state) {
        state.lastStatsRefresh = Util.getMillis();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Map<?, ?> values = findMapField(minecraft.player.getStats(), "Stat");
        if (values == null) return;
        List<StatRow> rows = new ArrayList<>();
        for (Map.Entry<?, ?> mapEntry : values.entrySet()) {
            Object stat = mapEntry.getKey();
            int value = number(mapEntry.getValue());
            if (stat == null || value == 0) continue;
            Object type = invokeNoArg(stat, "getType");
            Object statValue = invokeNoArg(stat, "getValue");
            rows.add(new StatRow(statGroup(type), statLabel(type, statValue),
                    formatStat(stat, value)));
        }
        rows.sort(Comparator.comparing(row ->
                row.label.getString().toLowerCase(Locale.ROOT)));
        state.statsRows.clear();
        state.statsRows.addAll(rows);
    }

    private static void renderStatistics(GuiGraphics graphics, State state,
            int mouseX, int mouseY, float alpha, long now) {
        Font font = Minecraft.getInstance().font;
        Layout panel = state.layout;
        StatisticsLayout layout = statisticsLayout(panel);
        drawHeader(graphics, panel, "STATISTICS", alpha);
        int tabWidth = Math.max(48,
                (layout.right - layout.left - 6) / StatGroup.values().length);
        for (int i = 0; i < StatGroup.values().length; i++) {
            int x = layout.left + i * tabWidth;
            int right = i == StatGroup.values().length - 1
                    ? layout.right : x + tabWidth - 2;
            boolean selected = state.statsGroup == i;
            boolean hovered = mouseX >= x && mouseX < right
                    && mouseY >= layout.tabsY && mouseY < layout.tabsBottom;
            graphics.fill(x, layout.tabsY, right, layout.tabsBottom,
                    applyAlpha(selected || hovered ? ROW_HOVER : ROW, alpha));
            if (selected) graphics.fill(x, layout.tabsBottom - 2,
                    right, layout.tabsBottom, applyAlpha(ACCENT, alpha));
            graphics.drawCenteredString(font,
                    ScpFonts.titillium(StatGroup.values()[i].label),
                    x + (right - x) / 2, layout.tabsY + 7,
                    applyAlpha(selected ? ACCENT_BRIGHT : TEXT, alpha));
        }

        List<StatRow> rows = statsForGroup(state);
        int max = Math.max(0, rows.size() - layout.visibleRows);
        state.statsScroll = Mth.clamp(state.statsScroll, 0, max);
        if (rows.isEmpty()) {
            String message = state.statsRequested
                    && now - state.statsRequestedAt < 1400L
                    ? "Retrieving statistics..."
                    : "No recorded statistics in this category.";
            drawCenteredMessage(graphics,
                    new Layout(layout.left, layout.listY,
                            layout.right - layout.left,
                            layout.listBottom - layout.listY), message, alpha);
            return;
        }
        for (int row = 0; row < layout.visibleRows; row++) {
            int index = state.statsScroll + row;
            if (index >= rows.size()) break;
            StatRow entry = rows.get(index);
            int y = layout.listY + row * (STAT_ROW_HEIGHT + 3);
            boolean hovered = mouseX >= layout.left && mouseX < layout.right
                    && mouseY >= y && mouseY < y + STAT_ROW_HEIGHT;
            graphics.fill(layout.left, y, layout.right, y + STAT_ROW_HEIGHT,
                    applyAlpha(hovered ? ROW_HOVER : ROW, alpha));
            graphics.fill(layout.left, y, layout.left + 3,
                    y + STAT_ROW_HEIGHT, applyAlpha(ACCENT, alpha));
            String label = compactToWidth(font, entry.label.getString(),
                    layout.right - layout.left - 105);
            graphics.drawString(font, ScpFonts.roboto(label),
                    layout.left + 12, y + 8, applyAlpha(TEXT, alpha), false);
            Component value = ScpFonts.roboto(entry.value);
            float valueWidth = font.width(value) * STAT_VALUE_SCALE;
            drawScaledString(graphics, font, value,
                    layout.right - 12 - valueWidth, y + 7,
                    STAT_VALUE_SCALE, applyAlpha(ACCENT_BRIGHT, alpha));
        }
        if (max > 0) drawScrollbar(graphics, layout.right - 3,
                layout.listY, layout.listBottom, state.statsScroll,
                max, layout.visibleRows, alpha);
    }

    private static void handleStatisticsClick(State state,
            double mouseX, double mouseY) {
        StatisticsLayout layout = statisticsLayout(state.layout);
        if (mouseY < layout.tabsY || mouseY >= layout.tabsBottom
                || mouseX < layout.left || mouseX >= layout.right) return;
        int tabWidth = Math.max(48,
                (layout.right - layout.left - 6) / StatGroup.values().length);
        state.statsGroup = Mth.clamp(
                (int) ((mouseX - layout.left) / tabWidth),
                0, StatGroup.values().length - 1);
        state.statsScroll = 0;
    }

    private static StatisticsLayout statisticsLayout(Layout panel) {
        int left = panel.x + 16;
        int right = panel.x + panel.width - 16;
        int tabsY = panel.y + 48;
        int tabsBottom = tabsY + 28;
        int listY = tabsBottom + 12;
        int listBottom = panel.y + panel.height - 16;
        return new StatisticsLayout(left, right, tabsY, tabsBottom,
                listY, listBottom, Math.max(1,
                (listBottom - listY) / (STAT_ROW_HEIGHT + 3)));
    }

    private static List<StatRow> statsForGroup(State state) {
        StatGroup wanted = StatGroup.values()[Mth.clamp(state.statsGroup, 0,
                StatGroup.values().length - 1)];
        return state.statsRows.stream().filter(row -> row.group == wanted).toList();
    }

    private static StatGroup statGroup(Object type) {
        if (type == Stats.CUSTOM) return StatGroup.GENERAL;
        if (type == Stats.BLOCK_MINED || type == Stats.ITEM_CRAFTED
                || type == Stats.ITEM_USED || type == Stats.ITEM_BROKEN
                || type == Stats.ITEM_PICKED_UP || type == Stats.ITEM_DROPPED)
            return StatGroup.ITEMS;
        if (type == Stats.ENTITY_KILLED || type == Stats.ENTITY_KILLED_BY)
            return StatGroup.MOBS;
        return StatGroup.OTHER;
    }

    private static Component statLabel(Object type, Object value) {
        if (type == Stats.CUSTOM && value instanceof ResourceLocation id) {
            String key = "stat." + id.getNamespace() + "."
                    + id.getPath().replace('/', '.');
            return I18n.exists(key) ? Component.translatable(key)
                    : Component.literal(humanize(id.toString()));
        }
        Component valueName = displayName(value);
        if (type == Stats.BLOCK_MINED) return prefixed("Mined", valueName);
        if (type == Stats.ITEM_CRAFTED) return prefixed("Crafted", valueName);
        if (type == Stats.ITEM_USED) return prefixed("Used", valueName);
        if (type == Stats.ITEM_BROKEN) return prefixed("Broken", valueName);
        if (type == Stats.ITEM_PICKED_UP) return prefixed("Picked Up", valueName);
        if (type == Stats.ITEM_DROPPED) return prefixed("Dropped", valueName);
        if (type == Stats.ENTITY_KILLED) return prefixed("Killed", valueName);
        if (type == Stats.ENTITY_KILLED_BY) return prefixed("Killed by", valueName);
        return Component.literal("Statistic: ").append(valueName.copy());
    }

    private static Component displayName(Object value) {
        if (value instanceof Item item) return item.getDescription();
        if (value instanceof Block block) return block.getName();
        if (value instanceof EntityType<?> entityType) return entityType.getDescription();
        if (value instanceof ResourceLocation id)
            return Component.literal(humanize(id.toString()));
        return Component.literal(humanize(String.valueOf(value)));
    }

    private static Component prefixed(String prefix, Component value) {
        return Component.literal(prefix + " ").append(value.copy());
    }

    private static String formatStat(Object stat, int value) {
        try {
            for (Method method : stat.getClass().getMethods()) {
                if (!"format".equals(method.getName())
                        || method.getParameterCount() != 1
                        || method.getParameterTypes()[0] != int.class) continue;
                Object formatted = method.invoke(stat, value);
                if (formatted != null) return String.valueOf(formatted);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return Integer.toString(value);
    }

    private static void initializeLan(State state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (state.lanGameType == null) {
            state.lanGameType = minecraft.getSingleplayerServer() == null
                    ? GameType.SURVIVAL
                    : minecraft.getSingleplayerServer().getDefaultGameType();
        }
        if (state.lanPort == null) {
            state.lanPort = new EditBox(minecraft.font, 0, 0, 100, 20,
                    Component.literal("Port"));
            state.lanPort.setBordered(false);
            state.lanPort.setMaxLength(5);
            state.lanPort.setFilter(value -> value.isEmpty()
                    || value.chars().allMatch(Character::isDigit));
            state.lanPort.setFormatter((value, cursor) ->
                    ScpFonts.roboto(value).getVisualOrderText());
            state.lanPort.setValue(Integer.toString(availablePort()));
        }
        state.lanStatus = "";
    }

    private static void renderLan(GuiGraphics graphics, State state,
            int mouseX, int mouseY, float partialTick, float alpha) {
        Font font = Minecraft.getInstance().font;
        Layout panel = state.layout;
        LanLayout layout = lanLayout(panel);
        drawHeader(graphics, panel, "OPEN TO LAN", alpha);
        drawLanOption(graphics, layout.left, layout.gameModeY,
                layout.width, "GAME MODE", gameTypeName(state.lanGameType),
                layout.gameModeContains(mouseX, mouseY), alpha);
        drawLanOption(graphics, layout.left, layout.cheatsY,
                layout.width, "ALLOW CHEATS", state.lanCheats ? "ON" : "OFF",
                layout.cheatsContains(mouseX, mouseY), alpha);

        boolean portHover = layout.portContains(mouseX, mouseY);
        graphics.fill(layout.left, layout.portY,
                layout.left + layout.width, layout.portY + 32,
                applyAlpha(portHover ? LAN_ROW_HOVER : LAN_ROW, alpha));
        graphics.fill(layout.left, layout.portY, layout.left + 3,
                layout.portY + 32, applyAlpha(ACCENT, alpha));
        drawScaledString(graphics, font, ScpFonts.roboto("PORT"),
                layout.left + 12, layout.portY + 10,
                LAN_OPTION_TEXT_SCALE, applyAlpha(TEXT, alpha));
        int fieldWidth = 108;
        int fieldX = layout.left + layout.width - fieldWidth - 10;
        graphics.fill(fieldX, layout.portY + 5, fieldX + fieldWidth,
                layout.portY + 27, applyAlpha(LAN_FIELD, alpha));
        if (state.lanPort != null) {
            int textWidth = font.width(ScpFonts.roboto(state.lanPort.getValue()));
            int editWidth = Mth.clamp(textWidth + 4, 4, fieldWidth - 12);
            state.lanPort.setX(fieldX + (fieldWidth - textWidth) / 2);
            state.lanPort.setY(layout.portY + 12);
            state.lanPort.setWidth(editWidth);
            state.lanPort.render(graphics, mouseX, mouseY, partialTick);
        }

        boolean startHover = layout.startContains(mouseX, mouseY);
        graphics.fill(layout.left, layout.startY,
                layout.left + layout.width, layout.startY + 34,
                applyAlpha(startHover ? LAN_ROW_HOVER : LAN_ROW, alpha));
        graphics.fill(layout.left, layout.startY, layout.left + 4,
                layout.startY + 34, applyAlpha(ACCENT, alpha));
        drawScaledCenteredString(graphics, font,
                ScpFonts.roboto("START LAN WORLD"),
                layout.left + layout.width / 2.0F, layout.startY + 11,
                LAN_START_TEXT_SCALE,
                applyAlpha(startHover ? ACCENT_BRIGHT : TEXT, alpha));
        if (!state.lanStatus.isBlank()) {
            graphics.drawCenteredString(font, ScpFonts.titillium(state.lanStatus),
                    panel.x + panel.width / 2, layout.startY + 45,
                    applyAlpha(MUTED, alpha));
        }
    }

    private static void drawLanOption(GuiGraphics graphics, int x, int y,
            int width, String label, String value, boolean hovered,
            float alpha) {
        Font font = Minecraft.getInstance().font;
        graphics.fill(x, y, x + width, y + 32,
                applyAlpha(hovered ? LAN_ROW_HOVER : LAN_ROW, alpha));
        graphics.fill(x, y, x + 3, y + 32, applyAlpha(ACCENT, alpha));
        Component labelText = ScpFonts.roboto(label);
        drawScaledString(graphics, font, labelText, x + 12, y + 10,
                LAN_OPTION_TEXT_SCALE, applyAlpha(TEXT, alpha));
        Component valueText = ScpFonts.roboto(value);
        float valueWidth = font.width(valueText) * LAN_OPTION_TEXT_SCALE;
        drawScaledString(graphics, font, valueText,
                x + width - 12 - valueWidth, y + 10,
                LAN_OPTION_TEXT_SCALE,
                applyAlpha(hovered ? ACCENT_BRIGHT : TEXT, alpha));
    }

    private static void handleLanClick(State state,
            double mouseX, double mouseY) {
        LanLayout layout = lanLayout(state.layout);
        if (layout.gameModeContains(mouseX, mouseY)) {
            state.lanGameType = nextGameType(state.lanGameType);
            return;
        }
        if (layout.cheatsContains(mouseX, mouseY)) {
            state.lanCheats = !state.lanCheats;
            return;
        }
        if (state.lanPort != null) {
            boolean editClicked = state.lanPort.mouseClicked(mouseX, mouseY, 0);
            if (!editClicked && layout.portContains(mouseX, mouseY)) {
                state.lanPort.setFocused(true);
                return;
            }
        }
        if (layout.startContains(mouseX, mouseY)) publishLan(state);
    }

    private static void publishLan(State state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() == null) {
            state.lanStatus = "No integrated server is available.";
            return;
        }
        int port;
        try {
            port = Integer.parseInt(state.lanPort == null
                    ? "" : state.lanPort.getValue());
        } catch (NumberFormatException exception) {
            state.lanStatus = "Enter a valid port.";
            return;
        }
        if (port < 1024 || port > 65535) {
            state.lanStatus = "Port must be between 1024 and 65535.";
            return;
        }
        try {
            if (!minecraft.getSingleplayerServer().publishServer(
                    state.lanGameType, state.lanCheats, port)) {
                state.lanStatus = "Could not open the world to LAN.";
                return;
            }
            minecraft.gui.getChat().addMessage(Component.literal(
                    "Local game hosted on port " + port + "."));
            minecraft.setScreen(null);
        } catch (Throwable throwable) {
            state.lanStatus = "Could not open the world to LAN.";
            ScpAdditionsMod.LOGGER.warn("Could not publish LAN world", throwable);
        }
    }

    private static LanLayout lanLayout(Layout panel) {
        int width = Mth.clamp(panel.width - 70, 230, 390);
        int left = panel.x + (panel.width - width) / 2;
        int gameModeY = panel.y + 58;
        int cheatsY = gameModeY + 40;
        int portY = cheatsY + 40;
        return new LanLayout(left, width, gameModeY, cheatsY,
                portY, portY + 48);
    }

    private static GameType nextGameType(GameType current) {
        GameType[] order = {GameType.SURVIVAL, GameType.CREATIVE,
                GameType.ADVENTURE, GameType.SPECTATOR};
        for (int i = 0; i < order.length; i++) {
            if (order[i] == current) return order[(i + 1) % order.length];
        }
        return GameType.SURVIVAL;
    }

    private static String gameTypeName(GameType type) {
        if (type == null) return "SURVIVAL";
        return type.getName().toUpperCase(Locale.ROOT);
    }

    private static int availablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception ignored) {
            return 25565;
        }
    }

    private static void drawHeader(GuiGraphics graphics, Layout panel,
            String title, float alpha) {
        graphics.drawString(Minecraft.getInstance().font,
                ScpFonts.montserrat(title), panel.x + 16, panel.y + 17,
                applyAlpha(TEXT, alpha), false);
    }

    private static void drawCenteredMessage(GuiGraphics graphics,
            Layout area, String message, float alpha) {
        Font font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, ScpFonts.roboto(message),
                area.x + area.width / 2,
                area.y + Math.max(0, (area.height - font.lineHeight) / 2),
                applyAlpha(MUTED, alpha));
    }

    private static void drawScaledString(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawScaledCenteredString(GuiGraphics graphics, Font font,
            Component text, float centerX, float y, float scale, int color) {
        float width = font.width(text) * scale;
        drawScaledString(graphics, font, text, centerX - width / 2.0F,
                y, scale, color);
    }

    private static void drawScrollbar(GuiGraphics graphics, int x,
            int top, int bottom, int offset, int maxOffset,
            int visibleRows, float alpha) {
        if (maxOffset <= 0 || bottom <= top) return;
        graphics.fill(x, top, x + 2, bottom, applyAlpha(TRACK, alpha));
        int totalRows = visibleRows + maxOffset;
        int height = bottom - top;
        int thumb = Math.max(12,
                Math.round(height * visibleRows / (float) totalRows));
        int travel = Math.max(0, height - thumb);
        int y = top + Math.round(travel * offset / (float) maxOffset);
        graphics.fill(x, y, x + 2, y + thumb, applyAlpha(ACCENT, alpha));
    }

    private static Map<?, ?> findMapField(Object owner, String genericHint) {
        if (owner == null) return null;
        Class<?> type = owner.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(owner);
                    if (!(value instanceof Map<?, ?> map)) continue;
                    String generic = field.getGenericType().getTypeName();
                    if (generic.contains(genericHint)
                            || mapMatches(map, genericHint)) return map;
                } catch (ReflectiveOperationException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static boolean mapMatches(Map<?, ?> map, String hint) {
        if (map.isEmpty()) return false;
        Map.Entry<?, ?> first = map.entrySet().iterator().next();
        return className(first.getKey()).contains(hint)
                || className(first.getValue()).contains(hint);
    }

    private static String className(Object value) {
        return value == null ? "" : value.getClass().getName();
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) return null;
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            try {
                Method method = type.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static Iterable<?> iterable(Object value) {
        if (value instanceof Iterable<?> iterable) return iterable;
        if (value instanceof Collection<?> collection) return collection;
        if (value instanceof Object[] array) return List.of(array);
        return List.of();
    }

    private static Object displayOf(Object advancement) {
        Object value = invokeNoArg(advancement, "getDisplay");
        return value instanceof Optional<?> optional
                ? optional.orElse(null) : value;
    }

    private static Object rootOf(Object advancement) {
        Object current = advancement;
        for (int guard = 0; guard < 128; guard++) {
            Object parent = invokeNoArg(current, "getParent");
            if (parent == null || parent == current) break;
            current = parent;
        }
        return current;
    }

    private static String idOf(Object value) {
        Object id = invokeNoArg(value, "getId");
        return id == null ? String.valueOf(value) : String.valueOf(id);
    }

    private static boolean progressDone(Object progress) {
        return booleanValue(invokeNoArg(progress, "isDone"));
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Component component(Object value, String fallback) {
        return value instanceof Component component
                ? component : Component.literal(fallback);
    }

    private static ItemStack itemStack(Object value) {
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static String humanize(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        String raw = value;
        int separator = raw.indexOf(':');
        if (separator >= 0 && separator + 1 < raw.length())
            raw = raw.substring(separator + 1);
        raw = raw.replace('/', ' ').replace('_', ' ').replace('.', ' ');
        StringBuilder result = new StringBuilder(raw.length());
        boolean upper = true;
        for (char c : raw.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                upper = true;
            } else {
                result.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return result.toString().trim();
    }

    private static String compactToWidth(Font font, String text, int maxWidth) {
        if (text == null) return "";
        if (font.width(ScpFonts.roboto(text)) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = font.width(ScpFonts.roboto(suffix));
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = result.toString() + text.charAt(i);
            if (font.width(ScpFonts.roboto(candidate)) + suffixWidth > maxWidth)
                break;
            result.append(text.charAt(i));
        }
        return result.append(suffix).toString();
    }

    private static void renderScpAdditionsLogo(GuiGraphics graphics,
            int x, int y, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager().getResource(SCP_ADDITIONS_LOGO).isEmpty()) {
            return;
        }
        int width = 16;
        int height = 14;
        int drawY = y + 1;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F,
                Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blit(SCP_ADDITIONS_LOGO, x, drawY, width, height,
                0.0F, 0.0F, 960, 832, 960, 832);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
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
        private boolean open;
        private float progress;
        private long lastFrameAt;
        private Layout layout;
        private final List<AdvancementCategory> achievementCategories =
                new ArrayList<>();
        private int selectedAchievementCategory;
        private int achievementCategoryScroll;
        private int achievementScroll;
        private final List<StatRow> statsRows = new ArrayList<>();
        private int statsGroup;
        private int statsScroll;
        private boolean statsRequested;
        private long statsRequestedAt;
        private long lastStatsRefresh;
        private GameType lanGameType;
        private boolean lanCheats;
        private EditBox lanPort;
        private String lanStatus = "";
    }

    private record AdvancementCategory(Component title, ItemStack icon,
            boolean useModLogo, int completed, int total,
            List<AdvancementRow> rows) {
    }

    private record AdvancementRow(Component title, Component description,
            ItemStack icon, boolean done) {
    }

    private record StatRow(StatGroup group, Component label, String value) {
    }

    private record Layout(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }

    private record AchievementLayout(int sidebarX, int sidebarY,
            int sidebarWidth, int sidebarBottom, int contentX, int contentY,
            int contentRight, int contentBottom, int visibleCategories,
            int visibleRows) {
    }

    private record StatisticsLayout(int left, int right, int tabsY,
            int tabsBottom, int listY, int listBottom, int visibleRows) {
    }

    private record LanLayout(int left, int width, int gameModeY,
            int cheatsY, int portY, int startY) {
        private boolean gameModeContains(double x, double y) {
            return contains(left, gameModeY, width, 32, x, y);
        }

        private boolean cheatsContains(double x, double y) {
            return contains(left, cheatsY, width, 32, x, y);
        }

        private boolean portContains(double x, double y) {
            return contains(left, portY, width, 32, x, y);
        }

        private boolean startContains(double x, double y) {
            return contains(left, startY, width, 34, x, y);
        }

        private static boolean contains(int x, int y, int width, int height,
                double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }
}
