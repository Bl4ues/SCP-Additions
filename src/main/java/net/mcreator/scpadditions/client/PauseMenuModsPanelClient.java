package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.util.MavenVersionStringHelper;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.MainMenuSounds;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Native Forge mod browser shared by the custom title and pause menus. */
public final class PauseMenuModsPanelClient {
    private static final int PANEL = 0xE20B0E12;
    private static final int PANEL_SOFT = 0xB812161C;
    private static final int ROW = 0x780B0E12;
    private static final int ROW_HOVER = 0xB5161B22;
    private static final int ROW_SELECTED = 0xC31A2028;
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9DA5AF;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int BORDER = 0x70414A56;
    private static final int TRACK = 0x563D4652;

    private static final int SORT_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 28;
    private static final int MOD_ROW_HEIGHT = 44;
    private static final int MOD_ROW_GAP = 4;
    private static final int LIST_ICON_SIZE = 28;
    private static final int DETAIL_LOGO_SIZE = 72;
    private static final int DETAIL_SCROLL_STEP = 26;
    private static final int CONTROL_COUNT = 4;
    private static final Set<String> INTERNAL_MOD_IDS = Set.of(
            "minecraft", "forge", "fml", "fmlcore", "fmlloader",
            "javafmllanguage", "lowcodelanguage", "mclanguage",
            "mixinextras");

    private static final Map<Screen, State> STATES =
            new WeakHashMap<>();

    private PauseMenuModsPanelClient() {
    }

    private enum SortMode {
        OFF,
        A_TO_Z,
        Z_TO_A
    }

    public static void toggle(Screen screen) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        state.open = !state.open;
        if (state.open) ensureMods(state);
    }

    public static void close(Screen screen) {
        State state = STATES.get(screen);
        if (state != null) state.open = false;
    }

    public static boolean isOpen(Screen screen) {
        State state = STATES.get(screen);
        return state != null && (state.open || state.progress > 0.02F);
    }

    public static void render(Screen screen,
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            long now, int baseX, int baseY, int menuWidth, int rowHeight,
            int gap) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        if (state.lastFrameAt == 0L) state.lastFrameAt = now;
        float delta = Math.min(0.10F,
                Math.max(0.0F, (now - state.lastFrameAt) / 1000.0F));
        state.lastFrameAt = now;

        float target = state.open ? 1.0F : 0.0F;
        state.progress = approach(state.progress, target, delta * 7.0F);
        if (!state.open && state.progress <= 0.01F) {
            state.layout = null;
            state.hoverKey = "";
            return;
        }

        ensureMods(state);
        float eased = smootherStep(state.progress);
        Layout base = layout(screen, baseX);
        int actualX = base.panelX
                + Math.round((1.0F - eased) * -44.0F);
        Layout layout = base.moveX(actualX - base.panelX);
        state.layout = layout;

        drawPanel(graphics, layout, eased);
        drawSortControls(graphics, state, layout, mouseX, mouseY, eased);
        drawModList(graphics, state, layout, mouseX, mouseY, eased);
        drawOpenFolder(graphics, layout, mouseX, mouseY, eased);
        drawDetails(graphics, state, layout, mouseX, mouseY, eased);
        updateHoverSound(state, layout, mouseX, mouseY);
    }

    public static boolean mouseClicked(Screen screen,
            double mouseX, double mouseY, int button) {
        State state = activeState(screen);
        if (state == null) return false;
        Layout layout = state.layout;
        if (!layout.contains(mouseX, mouseY)) return false;
        if (button != 0) return true;

        for (int index = 0; index < CONTROL_COUNT; index++) {
            if (!layout.sortContains(index, mouseX, mouseY)) continue;
            if (index == 3) {
                state.hideInternal = !state.hideInternal;
                state.listScroll = 0;
                sortMods(state);
                playSelect();
                return true;
            }
            SortMode mode = SortMode.values()[index];
            if (state.sortMode != mode) {
                state.sortMode = mode;
                state.listScroll = 0;
                sortMods(state);
                playSelect();
            }
            return true;
        }

        if (layout.folderContains(mouseX, mouseY)) {
            Util.getPlatform().openFile(FMLPaths.MODSDIR.get().toFile());
            playSelect();
            return true;
        }

        int row = layout.listRowAt(mouseX, mouseY);
        if (row >= 0) {
            int index = state.listScroll + row;
            if (index >= 0 && index < state.mods.size()) {
                state.selectedId = state.mods.get(index).info.getModId();
                state.detailScroll = 0;
                playSelect();
            }
            return true;
        }

        ModEntry selected = selected(state);
        if (selected != null && state.hasConfig
                && layout.settingsContains(mouseX, mouseY)) {
            openSettings(screen, selected);
            playSelect();
            return true;
        }
        return true;
    }

    public static boolean mouseScrolled(Screen screen,
            double mouseX, double mouseY, double delta) {
        State state = activeState(screen);
        if (state == null || delta == 0.0D) return false;
        Layout layout = state.layout;
        if (!layout.contains(mouseX, mouseY)) return false;

        int direction = delta > 0.0D ? -1 : 1;
        if (layout.listContains(mouseX, mouseY)) {
            int max = Math.max(0, state.mods.size() - layout.visibleRows);
            state.listScroll = Mth.clamp(state.listScroll + direction, 0, max);
            return true;
        }
        if (layout.detailScrollContains(mouseX, mouseY)) {
            state.detailScroll = Mth.clamp(state.detailScroll
                    + direction * DETAIL_SCROLL_STEP, 0,
                    state.detailMaxScroll);
            return true;
        }
        return true;
    }

    public static boolean keyPressed(Screen screen,
            int keyCode, int scanCode, int modifiers) {
        State state = activeState(screen);
        if (state == null) return false;
        if (keyCode == 256) {
            close(screen);
            return true;
        }
        return false;
    }

    private static State activeState(Screen screen) {
        State state = STATES.get(screen);
        return state == null || !state.open || state.progress < 0.78F
                || state.layout == null ? null : state;
    }

    private static void ensureMods(State state) {
        if (state.loaded) return;
        state.loaded = true;
        try {
            for (IModInfo info : ModList.get().getMods()) {
                String version = MavenVersionStringHelper.artifactVersionToString(
                        info.getVersion());
                state.originalMods.add(new ModEntry(info,
                        StringUtil.stripColor(info.getDisplayName()), version,
                        loadLogo(info), isInternal(info), hasConfig(info)));
            }
            sortMods(state);
        } catch (Throwable throwable) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not build custom Mods pause panel", throwable);
        }
    }

    private static void sortMods(State state) {
        state.mods.clear();
        for (ModEntry entry : state.originalMods) {
            if (!state.hideInternal || !entry.internal) state.mods.add(entry);
        }
        Comparator<ModEntry> comparator = Comparator.comparing(entry ->
                entry.name.toLowerCase(Locale.ROOT));
        if (state.sortMode == SortMode.A_TO_Z) {
            state.mods.sort(comparator);
        } else if (state.sortMode == SortMode.Z_TO_A) {
            state.mods.sort(comparator.reversed());
        }
        if (state.selectedId != null && state.mods.stream().noneMatch(entry ->
                state.selectedId.equals(entry.info.getModId()))) {
            state.selectedId = state.mods.isEmpty()
                    ? null : state.mods.get(0).info.getModId();
            state.detailScroll = 0;
        }
    }

    private static boolean isInternal(IModInfo info) {
        if (info == null) return false;
        String id = info.getModId().toLowerCase(Locale.ROOT);
        return INTERNAL_MOD_IDS.contains(id);
    }

    private static boolean hasConfig(IModInfo info) {
        try {
            return ConfigScreenHandler.getScreenFactoryFor(info).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static LogoData loadLogo(IModInfo info) {
        if (info == null || info.getLogoFile().isEmpty()) return LogoData.NONE;
        String logoFile = info.getLogoFile().get();
        try {
            var modFile = ModList.get().getModFileById(info.getModId());
            if (modFile == null) return LogoData.NONE;
            var path = modFile.getFile().findResource(logoFile);
            if (!Files.exists(path)) return LogoData.NONE;
            try (var input = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(input);
                int width = image.getWidth();
                int height = image.getHeight();
                TextureManager manager = Minecraft.getInstance()
                        .getTextureManager();
                ResourceLocation texture = manager.register(
                        "scp_additions_modlogo_" + safeId(info.getModId()),
                        new DynamicTexture(image));
                return new LogoData(texture, width, height);
            }
        } catch (IOException exception) {
            return LogoData.NONE;
        }
    }

    private static String safeId(String value) {
        return value == null ? "unknown"
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_");
    }

    private static void drawPanel(GuiGraphics graphics, Layout layout,
            float alpha) {
        graphics.fill(layout.panelX, layout.panelY,
                layout.panelRight, layout.panelBottom,
                applyAlpha(PANEL, alpha));
        graphics.fill(layout.panelX, layout.panelY,
                layout.panelRight, layout.panelY + 3,
                applyAlpha(ACCENT, alpha));
        graphics.fill(layout.panelX, layout.panelY,
                layout.panelX + 2, layout.panelBottom,
                applyAlpha(ACCENT, alpha));
        graphics.fill(layout.panelX, layout.panelBottom - 1,
                layout.panelRight, layout.panelBottom,
                applyAlpha(BORDER, alpha));

        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, ScpFonts.montserrat("MODS"),
                layout.panelX + 14, layout.panelY + 14,
                applyAlpha(TEXT, alpha), false);
    }

    private static void drawSortControls(GuiGraphics graphics, State state,
            Layout layout, int mouseX, int mouseY, float alpha) {
        String[] labels = {"Off", "A-Z", "Z-A", "Hide Internal"};
        for (int index = 0; index < labels.length; index++) {
            int x = layout.sortX(index);
            boolean hovered = layout.sortContains(index, mouseX, mouseY);
            boolean selected = index == 3
                    ? state.hideInternal : state.sortMode.ordinal() == index;
            graphics.fill(x, layout.sortY, x + layout.sortWidth,
                    layout.sortY + SORT_HEIGHT,
                    applyAlpha(hovered || selected ? ROW_HOVER : ROW, alpha));
            graphics.fill(x, layout.sortY, x + layout.sortWidth,
                    layout.sortY + 2,
                    applyAlpha(selected ? ACCENT : BORDER, alpha));
            Component text = ScpFonts.roboto(labels[index]);
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, text,
                    x + layout.sortWidth / 2,
                    layout.sortY + (SORT_HEIGHT - font.lineHeight) / 2,
                    applyAlpha(selected ? ACCENT_BRIGHT : TEXT, alpha));
        }
    }

    private static void drawModList(GuiGraphics graphics, State state,
            Layout layout, int mouseX, int mouseY, float alpha) {
        Font font = Minecraft.getInstance().font;
        int max = Math.max(0, state.mods.size() - layout.visibleRows);
        state.listScroll = Mth.clamp(state.listScroll, 0, max);

        graphics.enableScissor(layout.listX, layout.listY,
                layout.listRight, layout.listBottom);
        for (int row = 0; row < layout.visibleRows; row++) {
            int index = state.listScroll + row;
            if (index >= state.mods.size()) break;
            ModEntry entry = state.mods.get(index);
            int y = layout.listY + row * (MOD_ROW_HEIGHT + MOD_ROW_GAP);
            boolean hovered = mouseX >= layout.listX
                    && mouseX < layout.listRight
                    && mouseY >= y && mouseY < y + MOD_ROW_HEIGHT;
            boolean selected = entry.info.getModId().equals(state.selectedId);
            graphics.fill(layout.listX, y, layout.listRight,
                    y + MOD_ROW_HEIGHT,
                    applyAlpha(selected ? ROW_SELECTED
                            : hovered ? ROW_HOVER : ROW, alpha));
            graphics.fill(layout.listX, y, layout.listX + 3,
                    y + MOD_ROW_HEIGHT,
                    applyAlpha(selected ? ACCENT : BORDER, alpha));

            int iconX = layout.listX + 9;
            int iconY = y + (MOD_ROW_HEIGHT - LIST_ICON_SIZE) / 2;
            drawLogo(graphics, entry.logo, iconX, iconY,
                    LIST_ICON_SIZE, LIST_ICON_SIZE, alpha);

            int textX = layout.listX + 45;
            int textWidth = layout.listRight - textX - 18;
            String name = compactToWidth(font, entry.name, textWidth);
            graphics.drawString(font, ScpFonts.roboto(name),
                    textX, y + 9,
                    applyAlpha(selected ? ACCENT_BRIGHT : TEXT, alpha), false);
            String version = compactToWidth(font, entry.version, textWidth);
            graphics.drawString(font, ScpFonts.titillium(version),
                    textX, y + 25, applyAlpha(MUTED, alpha), false);
            if (entry.hasConfig) {
                int markerX = layout.listRight - 7;
                int markerY = y + (MOD_ROW_HEIGHT - 12) / 2;
                graphics.fill(markerX, markerY, markerX + 2, markerY + 12,
                        applyAlpha(ACCENT_BRIGHT, alpha * 0.78F));
            }
        }
        graphics.disableScissor();

        if (max > 0) {
            drawScrollbar(graphics, layout.listRight - 3,
                    layout.listY, layout.listBottom,
                    state.listScroll, max, layout.visibleRows, alpha);
        }
    }

    private static void drawOpenFolder(GuiGraphics graphics, Layout layout,
            int mouseX, int mouseY, float alpha) {
        boolean hovered = layout.folderContains(mouseX, mouseY);
        graphics.fill(layout.listX, layout.folderY,
                layout.listRight, layout.folderY + FOOTER_HEIGHT,
                applyAlpha(hovered ? ROW_HOVER : ROW, alpha));
        graphics.fill(layout.listX, layout.folderY,
                layout.listX + 3, layout.folderY + FOOTER_HEIGHT,
                applyAlpha(ACCENT, alpha));
        Font font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font,
                ScpFonts.roboto("Open mods folder"),
                layout.listX + layout.listWidth / 2,
                layout.folderY + (FOOTER_HEIGHT - font.lineHeight) / 2,
                applyAlpha(hovered ? ACCENT_BRIGHT : TEXT, alpha));
    }

    private static void drawDetails(GuiGraphics graphics, State state,
            Layout layout, int mouseX, int mouseY, float alpha) {
        ModEntry selected = selected(state);
        if (selected == null) {
            Font font = Minecraft.getInstance().font;
            Component message = ScpFonts.roboto("Select a mod to view details.");
            graphics.drawCenteredString(font, message,
                    layout.detailX + layout.detailWidth() / 2,
                    layout.detailY + layout.detailHeight() / 2,
                    applyAlpha(MUTED, alpha));
            state.hasConfig = false;
            state.detailMaxScroll = 0;
            state.detailScroll = 0;
            return;
        }

        Font font = Minecraft.getInstance().font;
        boolean hasLogo = selected.logo.present();
        int logoX = layout.detailX + 12;
        int logoY = layout.detailY + 8;
        if (hasLogo) {
            drawLogo(graphics, selected.logo, logoX, logoY,
                    DETAIL_LOGO_SIZE, DETAIL_LOGO_SIZE, alpha);
        }

        int headerTextX = hasLogo ? logoX + DETAIL_LOGO_SIZE + 14
                : layout.detailX + 12;
        int headerTextRight = layout.detailRight - 12;
        int headerWidth = Math.max(50, headerTextRight - headerTextX);
        graphics.drawString(font,
                ScpFonts.montserrat(compactToWidth(font, selected.name,
                        headerWidth)),
                headerTextX, layout.detailY + 13,
                applyAlpha(TEXT, alpha), false);
        drawScaledText(graphics, font,
                ScpFonts.roboto("Version " + selected.version),
                headerTextX, layout.detailY + 30, 1.18F,
                applyAlpha(ACCENT_BRIGHT, alpha));
        drawScaledText(graphics, font,
                ScpFonts.roboto("Mod ID: " + selected.info.getModId()),
                headerTextX, layout.detailY + 47, 1.14F,
                applyAlpha(MUTED, alpha));

        int headerBottom = layout.detailY + DETAIL_LOGO_SIZE + 16;
        graphics.fill(layout.detailX + 8, headerBottom,
                layout.detailRight - 8, headerBottom + 1,
                applyAlpha(BORDER, alpha));

        state.hasConfig = selected.hasConfig;
        int settingsHeight = state.hasConfig ? FOOTER_HEIGHT + 10 : 0;
        int scrollTop = headerBottom + 10;
        int scrollBottom = layout.detailBottom - 8 - settingsHeight;
        int contentWidth = Math.max(60,
                layout.detailWidth() - 28);
        DetailContent content = buildDetailContent(font, selected,
                contentWidth);
        int viewport = Math.max(1, scrollBottom - scrollTop);
        state.detailMaxScroll = Math.max(0, content.height - viewport);
        state.detailScroll = Mth.clamp(state.detailScroll, 0,
                state.detailMaxScroll);

        graphics.enableScissor(layout.detailX + 6, scrollTop,
                layout.detailRight - 6, scrollBottom);
        int contentX = layout.detailX + 14;
        int contentY = scrollTop - state.detailScroll;
        for (DetailLine line : content.lines) {
            graphics.drawString(font, line.text, contentX,
                    contentY + line.y, applyAlpha(line.color, alpha), false);
        }
        graphics.disableScissor();

        if (state.detailMaxScroll > 0) {
            drawPixelScrollbar(graphics, layout.detailRight - 5,
                    scrollTop, scrollBottom, state.detailScroll,
                    state.detailMaxScroll, content.height, viewport, alpha);
        }

        if (state.hasConfig) {
            int y = layout.detailBottom - FOOTER_HEIGHT - 4;
            boolean hovered = layout.settingsContains(mouseX, mouseY);
            graphics.fill(layout.detailX + 8, y,
                    layout.detailRight - 8, y + FOOTER_HEIGHT,
                    applyAlpha(hovered ? ROW_HOVER : ROW, alpha));
            graphics.fill(layout.detailX + 8, y,
                    layout.detailX + 11, y + FOOTER_HEIGHT,
                    applyAlpha(ACCENT, alpha));
            graphics.drawCenteredString(font,
                    ScpFonts.roboto("Open Settings"),
                    layout.detailX + layout.detailWidth() / 2,
                    y + (FOOTER_HEIGHT - font.lineHeight) / 2,
                    applyAlpha(hovered ? ACCENT_BRIGHT : TEXT, alpha));
        }
    }

    private static DetailContent buildDetailContent(Font font,
            ModEntry entry, int width) {
        List<DetailLine> lines = new ArrayList<>();
        int y = 0;
        y = addMetadata(lines, font, width, y, "STATE",
                ModList.get().getModContainerById(entry.info.getModId())
                        .map(ModContainer::getCurrentState)
                        .map(Object::toString).orElse("NONE"));
        String authors = metadata(entry.info, "authors");
        if (!authors.isBlank())
            y = addMetadata(lines, font, width, y, "AUTHORS", authors);
        String credits = metadata(entry.info, "credits");
        if (!credits.isBlank())
            y = addMetadata(lines, font, width, y, "CREDITS", credits);
        String url = metadata(entry.info, "displayURL");
        if (!url.isBlank())
            y = addMetadata(lines, font, width, y, "WEBSITE", url);
        String childMods = childMods(entry.info);
        if (!childMods.isBlank())
            y = addMetadata(lines, font, width, y, "CHILD MODS", childMods);
        String license = license(entry.info);
        if (!license.isBlank())
            y = addMetadata(lines, font, width, y, "LICENSE", license);

        y += 5;
        lines.add(new DetailLine(ScpFonts.titillium("DESCRIPTION").getVisualOrderText(),
                ACCENT_BRIGHT, y));
        y += font.lineHeight + 4;
        List<FormattedCharSequence> description = font.split(
                ScpFonts.roboto(entry.info.getDescription()), width);
        if (description.isEmpty()) {
            description = List.of(ScpFonts.roboto("No description provided.")
                    .getVisualOrderText());
        }
        for (FormattedCharSequence line : description) {
            lines.add(new DetailLine(line, TEXT, y));
            y += font.lineHeight + 2;
        }
        return new DetailContent(List.copyOf(lines), y + 4);
    }

    private static int addMetadata(List<DetailLine> lines, Font font,
            int width, int y, String label, String value) {
        lines.add(new DetailLine(ScpFonts.titillium(label).getVisualOrderText(),
                ACCENT_BRIGHT, y));
        y += font.lineHeight + 2;
        List<FormattedCharSequence> wrapped = font.split(
                ScpFonts.roboto(value), width);
        for (FormattedCharSequence line : wrapped) {
            lines.add(new DetailLine(line, MUTED, y));
            y += font.lineHeight + 1;
        }
        return y + 5;
    }

    private static String metadata(IModInfo info, String key) {
        Object value = info.getConfig().getConfigElement(key).orElse(null);
        if (value == null) return "";
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
        }
        return String.valueOf(value);
    }

    private static String childMods(IModInfo info) {
        if (info.getOwningFile() == null
                || info.getOwningFile().getMods().size() <= 1) return "";
        return info.getOwningFile().getMods().stream()
                .filter(child -> child != info)
                .map(IModInfo::getDisplayName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static String license(IModInfo info) {
        return info.getOwningFile() instanceof ModFileInfo file
                ? file.getLicense() : "";
    }

    private static void openSettings(Screen screen,
            ModEntry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            ConfigScreenHandler.getScreenFactoryFor(entry.info)
                    .map(factory -> factory.apply(minecraft, screen))
                    .ifPresent(minecraft::setScreen);
        } catch (Throwable throwable) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not open config screen for {}",
                    entry.info.getModId(), throwable);
        }
    }

    private static ModEntry selected(State state) {
        if (state.selectedId == null) return null;
        for (ModEntry entry : state.mods) {
            if (state.selectedId.equals(entry.info.getModId())) return entry;
        }
        for (ModEntry entry : state.originalMods) {
            if (state.selectedId.equals(entry.info.getModId())) return entry;
        }
        return null;
    }

    private static Layout layout(Screen screen, int baseX) {
        boolean titleMenu = screen instanceof CustomMainMenuScreen;
        int availableWidth = Math.max(330, screen.width - baseX - 28);
        int panelWidth = titleMenu
                ? Math.min(availableWidth, Mth.clamp(
                        Math.round(screen.width * 0.46F), 520, 720))
                : Math.min(820, availableWidth);
        int panelHeight = titleMenu
                ? Mth.clamp(Math.round(screen.height * 0.62F), 340, 540)
                : Mth.clamp(screen.height - 64, 300, 620);
        int panelY = titleMenu
                ? Mth.clamp(Math.round(screen.height * 0.18F), 48,
                        Math.max(48, screen.height - panelHeight - 24))
                : Math.max(24, (screen.height - panelHeight) / 2);
        int panelX = baseX;
        int panelRight = Math.min(screen.width - 12, panelX + panelWidth);
        panelWidth = panelRight - panelX;

        int listX = panelX + 14;
        int listWidth = Mth.clamp(Math.round(panelWidth * 0.36F), 190, 280);
        int listRight = Math.min(panelRight - 210, listX + listWidth);
        listWidth = Math.max(150, listRight - listX);
        listRight = listX + listWidth;

        int sortY = panelY + 38;
        int sortGap = 4;
        int sortWidth = Math.max(38,
                (listWidth - sortGap * (CONTROL_COUNT - 1)) / CONTROL_COUNT);
        int listY = sortY + SORT_HEIGHT + 10;
        int folderY = panelY + panelHeight - FOOTER_HEIGHT - 12;
        int listBottom = folderY - 10;
        int visibleRows = Math.max(1,
                (listBottom - listY + MOD_ROW_GAP)
                        / (MOD_ROW_HEIGHT + MOD_ROW_GAP));

        int detailX = listRight + 14;
        int detailRight = panelRight - 14;
        int detailY = sortY;
        int detailBottom = panelY + panelHeight - 12;
        return new Layout(panelX, panelY, panelRight,
                panelY + panelHeight, listX, listRight, listWidth,
                sortY, sortWidth, sortGap, listY, listBottom,
                folderY, visibleRows, detailX, detailRight,
                detailY, detailBottom);
    }

    private static void drawLogo(GuiGraphics graphics, LogoData logo,
            int x, int y, int maxWidth, int maxHeight, float alpha) {
        if (logo == null || !logo.present()) return;
        float scale = Math.min(maxWidth / (float) logo.width,
                maxHeight / (float) logo.height);
        int width = Math.max(1, Math.round(logo.width * scale));
        int height = Math.max(1, Math.round(logo.height * scale));
        int drawX = x + (maxWidth - width) / 2;
        int drawY = y + (maxHeight - height) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F,
                Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blit(logo.texture, drawX, drawY, width, height,
                0.0F, 0.0F, logo.width, logo.height,
                logo.width, logo.height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
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

    private static void drawPixelScrollbar(GuiGraphics graphics, int x,
            int top, int bottom, int offset, int maxOffset,
            int contentHeight, int viewportHeight, float alpha) {
        if (maxOffset <= 0 || bottom <= top) return;
        graphics.fill(x, top, x + 2, bottom, applyAlpha(TRACK, alpha));
        int height = bottom - top;
        int thumb = Math.max(12, Math.round(height
                * viewportHeight / (float) Math.max(viewportHeight,
                contentHeight)));
        int travel = Math.max(0, height - thumb);
        int y = top + Math.round(travel * offset / (float) maxOffset);
        graphics.fill(x, y, x + 2, y + thumb, applyAlpha(ACCENT, alpha));
    }

    private static void updateHoverSound(State state, Layout layout,
            int mouseX, int mouseY) {
        if (!state.open || state.progress < 0.84F) return;
        String key = hoverKey(state, layout, mouseX, mouseY);
        if (!key.equals(state.hoverKey)) {
            state.hoverKey = key;
            if (!key.isBlank()) playHover();
        }
    }

    private static String hoverKey(State state, Layout layout,
            int mouseX, int mouseY) {
        for (int index = 0; index < CONTROL_COUNT; index++) {
            if (layout.sortContains(index, mouseX, mouseY))
                return "sort:" + index;
        }
        if (layout.folderContains(mouseX, mouseY)) return "folder";
        int row = layout.listRowAt(mouseX, mouseY);
        if (row >= 0) return "row:" + (state.listScroll + row);
        if (state.hasConfig && layout.settingsContains(mouseX, mouseY))
            return "settings";
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

    private static void drawScaledText(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static String compactToWidth(Font font, String text,
            int maxWidth) {
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

    private record ModEntry(IModInfo info, String name,
            String version, LogoData logo, boolean internal,
            boolean hasConfig) {
    }

    private record LogoData(ResourceLocation texture, int width, int height) {
        private static final LogoData NONE = new LogoData(null, 0, 0);

        private boolean present() {
            return texture != null && width > 0 && height > 0;
        }
    }

    private record DetailLine(FormattedCharSequence text, int color, int y) {
    }

    private record DetailContent(List<DetailLine> lines, int height) {
    }

    private record Layout(int panelX, int panelY, int panelRight,
            int panelBottom, int listX, int listRight, int listWidth,
            int sortY, int sortWidth, int sortGap, int listY,
            int listBottom, int folderY, int visibleRows,
            int detailX, int detailRight, int detailY, int detailBottom) {
        private int detailWidth() {
            return detailRight - detailX;
        }

        private int detailHeight() {
            return detailBottom - detailY;
        }

        private int sortX(int index) {
            return listX + index * (sortWidth + sortGap);
        }

        private boolean contains(double x, double y) {
            return x >= panelX && x < panelRight
                    && y >= panelY && y < panelBottom;
        }

        private boolean sortContains(int index, double x, double y) {
            int left = sortX(index);
            return x >= left && x < left + sortWidth
                    && y >= sortY && y < sortY + SORT_HEIGHT;
        }

        private boolean listContains(double x, double y) {
            return x >= listX && x < listRight
                    && y >= listY && y < listBottom;
        }

        private int listRowAt(double x, double y) {
            if (!listContains(x, y)) return -1;
            int slot = (int) ((y - listY) / (MOD_ROW_HEIGHT + MOD_ROW_GAP));
            if (slot < 0 || slot >= visibleRows) return -1;
            int rowY = listY + slot * (MOD_ROW_HEIGHT + MOD_ROW_GAP);
            return y < rowY + MOD_ROW_HEIGHT ? slot : -1;
        }

        private boolean folderContains(double x, double y) {
            return x >= listX && x < listRight
                    && y >= folderY && y < folderY + FOOTER_HEIGHT;
        }

        private boolean detailScrollContains(double x, double y) {
            return x >= detailX && x < detailRight
                    && y >= detailY && y < detailBottom;
        }

        private boolean settingsContains(double x, double y) {
            int top = detailBottom - FOOTER_HEIGHT - 4;
            return x >= detailX + 8 && x < detailRight - 8
                    && y >= top && y < top + FOOTER_HEIGHT;
        }

        private Layout moveX(int delta) {
            return new Layout(panelX + delta, panelY,
                    panelRight + delta, panelBottom,
                    listX + delta, listRight + delta, listWidth,
                    sortY, sortWidth, sortGap, listY, listBottom,
                    folderY, visibleRows, detailX + delta,
                    detailRight + delta, detailY, detailBottom);
        }
    }

    private static final class State {
        private final List<ModEntry> originalMods = new ArrayList<>();
        private final List<ModEntry> mods = new ArrayList<>();
        private boolean loaded;
        private boolean open;
        private float progress;
        private long lastFrameAt;
        private SortMode sortMode = SortMode.OFF;
        private boolean hideInternal = true;
        private int listScroll;
        private int detailScroll;
        private int detailMaxScroll;
        private String selectedId;
        private boolean hasConfig;
        private String hoverKey = "";
        private Layout layout;
    }
}
