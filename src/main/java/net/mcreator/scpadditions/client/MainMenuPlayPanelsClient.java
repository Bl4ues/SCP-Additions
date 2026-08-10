package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;

/**
 * Singleplayer and multiplayer flyouts for the SCP Additions title screen.
 * The rows are custom, but world/server mutations deliberately hand off to the
 * normal Minecraft screens and storage classes so the underlying behavior is
 * still vanilla.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MainMenuPlayPanelsClient {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int SUBTEXT = 0xFFADB3BC;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int BUTTON_BASE = 0xB20B0E12;
    private static final int BUTTON_HOVER = 0xDD161B22;
    private static final int POPUP = 0xF20B0E12;
    private static final int SCROLL_TRACK = 0x80414750;
    private static final int ROW_HEIGHT = 50;
    private static final int GAP = 6;
    private static final int HEADER_HEIGHT = 30;
    private static final ResourceLocation UNKNOWN_SERVER =
            new ResourceLocation("textures/misc/unknown_server.png");
    private static final SimpleDateFormat WORLD_DATE =
            new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault());

    private static final Map<CustomMainMenuScreen, State> STATES =
            new WeakHashMap<>();

    private MainMenuPlayPanelsClient() {
    }

    public static void attach(CustomMainMenuScreen screen) {
        STATES.computeIfAbsent(screen, ignored -> new State());
    }

    public static void toggleSingleplayer(CustomMainMenuScreen screen) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        boolean closing = state.open && state.mode == Mode.SINGLEPLAYER;
        state.mode = Mode.SINGLEPLAYER;
        state.open = !closing;
        state.contextIndex = -1;
        state.scrollOffset = 0;
        if (state.open) {
            closeOtherPanels(screen);
            ensureWorlds(state);
            playSelect();
        }
    }

    public static void toggleMultiplayer(CustomMainMenuScreen screen) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        boolean closing = state.open && state.mode == Mode.MULTIPLAYER;
        state.mode = Mode.MULTIPLAYER;
        state.open = !closing;
        state.contextIndex = -1;
        state.scrollOffset = 0;
        if (state.open) {
            closeOtherPanels(screen);
            ensureServers(state);
            playSelect();
        }
    }

    public static void close(CustomMainMenuScreen screen) {
        State state = STATES.get(screen);
        if (state != null) {
            state.open = false;
            state.contextIndex = -1;
        }
    }

    public static void render(CustomMainMenuScreen screen, GuiGraphics graphics,
            int mouseX, int mouseY) {
        State state = STATES.computeIfAbsent(screen, ignored -> new State());
        pollWorlds(state);

        long now = Util.getMillis();
        float delta = state.lastFrameAt == 0L ? 0.0F
                : Math.min(0.10F, Math.max(0.0F,
                (now - state.lastFrameAt) / 1000.0F));
        state.lastFrameAt = now;
        state.progress = approach(state.progress, state.open ? 1.0F : 0.0F,
                delta * 7.2F);
        if (!state.open && state.progress <= 0.01F) return;

        if (state.mode == Mode.SINGLEPLAYER) ensureWorlds(state);
        else ensureServers(state);

        List<Row> rows = rows(state);
        Layout layout = layout(screen, rows.size());
        int maxOffset = Math.max(0, rows.size() - layout.visibleRows);
        state.scrollOffset = Mth.clamp(state.scrollOffset, 0, maxOffset);

        float eased = smootherStep(state.progress);
        int panelX = layout.x - Math.round((1.0F - eased) * 34.0F);
        int alpha = Math.round(255.0F * eased);
        Font font = Minecraft.getInstance().font;

        drawHeader(graphics, state, layout, panelX, alpha, mouseX, mouseY);

        int listBottom = layout.listY
                + layout.visibleRows * (ROW_HEIGHT + GAP) - GAP;
        graphics.enableScissor(panelX, layout.listY,
                panelX + layout.width, listBottom);
        int hoveredToken = Integer.MIN_VALUE;
        for (int visible = 0; visible < layout.visibleRows; visible++) {
            int index = state.scrollOffset + visible;
            if (index >= rows.size()) break;
            int rowY = layout.listY + visible * (ROW_HEIGHT + GAP);
            Row row = rows.get(index);
            boolean hovered = contains(mouseX, mouseY,
                    panelX, rowY, layout.width, ROW_HEIGHT);
            boolean optionsHovered = row.hasOptions()
                    && contains(mouseX, mouseY,
                    panelX + layout.width - 38, rowY + 7, 30, 36);
            if (hovered) hoveredToken = index;
            drawRow(graphics, font, state, row, panelX, rowY,
                    layout.width, alpha, hovered, optionsHovered);
        }
        graphics.disableScissor();

        if (maxOffset > 0) {
            int trackX = panelX + layout.width + 4;
            int trackH = Math.max(20, listBottom - layout.listY);
            graphics.fill(trackX, layout.listY, trackX + 2,
                    layout.listY + trackH, withAlpha(SCROLL_TRACK, alpha));
            int thumbH = Math.max(18,
                    Math.round(trackH * (layout.visibleRows / (float) rows.size())));
            int travel = Math.max(0, trackH - thumbH);
            int thumbY = layout.listY + (maxOffset == 0 ? 0
                    : Math.round(travel * state.scrollOffset / (float) maxOffset));
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH,
                    withAlpha(ACCENT, alpha));
        }

        drawContextMenu(graphics, font, state, rows, layout,
                panelX, alpha, mouseX, mouseY);

        if (state.open && state.progress > 0.86F
                && hoveredToken != state.hoveredToken) {
            state.hoveredToken = hoveredToken;
            if (hoveredToken != Integer.MIN_VALUE) playHover();
        } else if (hoveredToken == Integer.MIN_VALUE) {
            state.hoveredToken = Integer.MIN_VALUE;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof CustomMainMenuScreen screen)) return;
        if (event.getButton() != 0) return;
        State state = STATES.computeIfAbsent(screen, ignored -> new State());

        AbstractButton settings = findNamedButton(screen, "Settings");
        AbstractButton extras = findNamedButton(screen, "Extras");
        AbstractButton mods = findNamedButton(screen, "Mods");
        AbstractButton quit = findNamedButton(screen, "Quit Game");
        if (isOver(settings, event) || isOver(extras, event)
                || isOver(mods, event) || isOver(quit, event)) {
            close(screen);
            return;
        }

        if (!state.open || state.progress < 0.78F) return;
        pollWorlds(state);
        List<Row> rows = rows(state);
        Layout layout = layout(screen, rows.size());
        int panelX = layout.x;
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        if (state.mode == Mode.MULTIPLAYER
                && contains(mouseX, mouseY,
                panelX + layout.width - 86, layout.y + 4, 78, 22)) {
            refreshServers(state);
            playSelect();
            event.setCanceled(true);
            return;
        }

        if (handleContextClick(screen, state, rows, layout,
                panelX, mouseX, mouseY)) {
            event.setCanceled(true);
            return;
        }

        int visible = rowAt(layout, panelX, mouseX, mouseY);
        if (visible < 0) return;
        int index = state.scrollOffset + visible;
        if (index < 0 || index >= rows.size()) return;
        Row row = rows.get(index);
        int rowY = layout.listY + visible * (ROW_HEIGHT + GAP);

        if (row.hasOptions() && contains(mouseX, mouseY,
                panelX + layout.width - 38, rowY + 7, 30, 36)) {
            state.contextIndex = state.contextIndex == index ? -1 : index;
            playSelect();
            event.setCanceled(true);
            return;
        }

        state.contextIndex = -1;
        activateRow(screen, state, row);
        playSelect();
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof CustomMainMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || !state.open || state.progress < 0.78F) return;
        List<Row> rows = rows(state);
        Layout layout = layout(screen, rows.size());
        if (!contains(event.getMouseX(), event.getMouseY(), layout.x,
                layout.y, layout.width + 8,
                layout.height())) return;
        int maxOffset = Math.max(0, rows.size() - layout.visibleRows);
        if (maxOffset <= 0 || event.getScrollDelta() == 0.0D) return;
        state.scrollOffset = Mth.clamp(state.scrollOffset
                + (event.getScrollDelta() > 0 ? -1 : 1), 0, maxOffset);
        state.contextIndex = -1;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof CustomMainMenuScreen screen)) return;
        State state = STATES.get(screen);
        if (state != null && state.serverPinger != null) {
            state.serverPinger.tick();
        }
    }

    private static void activateRow(CustomMainMenuScreen screen,
            State state, Row row) {
        switch (row.kind) {
            case CONTINUE, WORLD -> joinWorld(screen, (LevelSummary) row.value);
            case CREATE_WORLD -> CreateWorldScreen.openFresh(
                    Minecraft.getInstance(), screen);
            case ADD_SERVER -> addServer(screen, state);
            case DIRECT_CONNECT -> directConnect(screen, state);
            case SERVER -> connectServer(screen, (ServerData) row.value);
        }
    }

    private static boolean handleContextClick(CustomMainMenuScreen screen,
            State state, List<Row> rows, Layout layout, int panelX,
            double mouseX, double mouseY) {
        if (state.contextIndex < 0 || state.contextIndex >= rows.size()) {
            return false;
        }
        Row row = rows.get(state.contextIndex);
        if (!row.hasOptions()) {
            state.contextIndex = -1;
            return false;
        }
        int visible = state.contextIndex - state.scrollOffset;
        if (visible < 0 || visible >= layout.visibleRows) {
            state.contextIndex = -1;
            return false;
        }
        int rowY = layout.listY + visible * (ROW_HEIGHT + GAP);
        String[] options = row.kind == RowKind.WORLD
                ? new String[]{"Edit", "Re-create", "Delete"}
                : new String[]{"Edit", "Delete"};
        int popupW = 108;
        int optionH = 26;
        int popupX = panelX + layout.width - popupW - 6;
        int popupY = rowY + ROW_HEIGHT + 2;
        int totalH = options.length * optionH;
        int listBottom = layout.listY
                + layout.visibleRows * (ROW_HEIGHT + GAP) - GAP;
        if (popupY + totalH > listBottom) popupY = rowY - totalH - 2;

        for (int i = 0; i < options.length; i++) {
            if (!contains(mouseX, mouseY, popupX,
                    popupY + i * optionH, popupW, optionH)) continue;
            state.contextIndex = -1;
            playSelect();
            if (row.kind == RowKind.WORLD) {
                LevelSummary world = (LevelSummary) row.value;
                if (i == 0) editWorld(screen, state, world);
                else if (i == 1) recreateWorld(screen, state, world);
                else deleteWorld(screen, state, world);
            } else {
                ServerData server = (ServerData) row.value;
                if (i == 0) editServer(screen, state, server);
                else deleteServer(screen, state, server);
            }
            return true;
        }
        return false;
    }

    private static void joinWorld(CustomMainMenuScreen screen, LevelSummary world) {
        Minecraft minecraft = Minecraft.getInstance();
        if (world == null || world.isLocked()
                || world.requiresManualConversion()) return;
        if (minecraft.getLevelSource().levelExists(world.getLevelId())) {
            minecraft.createWorldOpenFlows().loadLevel(screen, world.getLevelId());
        }
    }

    private static void editWorld(CustomMainMenuScreen screen,
            State state, LevelSummary world) {
        Minecraft minecraft = Minecraft.getInstance();
        try {
            var access = minecraft.getLevelSource()
                    .validateAndCreateAccess(world.getLevelId());
            minecraft.setScreen(new EditWorldScreen(changed -> {
                try {
                    access.close();
                } catch (IOException ignored) {
                }
                refreshWorlds(state);
                minecraft.setScreen(screen);
            }, access));
        } catch (Exception ignored) {
            refreshWorlds(state);
            minecraft.setScreen(screen);
        }
    }

    private static void recreateWorld(CustomMainMenuScreen screen,
            State state, LevelSummary world) {
        Minecraft minecraft = Minecraft.getInstance();
        try (var access = minecraft.getLevelSource()
                .validateAndCreateAccess(world.getLevelId())) {
            Pair<LevelSettings, WorldCreationContext> data =
                    minecraft.createWorldOpenFlows().recreateWorldData(access);
            Path dataPacks = CreateWorldScreen.createTempDataPackDirFromExistingWorld(
                    access.getLevelPath(LevelResource.DATAPACK_DIR), minecraft);
            Screen recreated = CreateWorldScreen.createFromExisting(
                    minecraft, screen, data.getFirst(), data.getSecond(), dataPacks);
            if (data.getSecond().options().isOldCustomizedWorld()) {
                minecraft.setScreen(new ConfirmScreen(confirmed ->
                        minecraft.setScreen(confirmed ? recreated : screen),
                        Component.translatable("selectWorld.recreate.customized.title"),
                        Component.translatable("selectWorld.recreate.customized.text"),
                        CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
            } else {
                minecraft.setScreen(recreated);
            }
        } catch (Exception ignored) {
            refreshWorlds(state);
            minecraft.setScreen(screen);
        }
    }

    private static void deleteWorld(CustomMainMenuScreen screen,
            State state, LevelSummary world) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                try (var access = minecraft.getLevelSource()
                        .createAccess(world.getLevelId())) {
                    access.deleteLevel();
                } catch (Exception ignored) {
                }
                refreshWorlds(state);
            }
            minecraft.setScreen(screen);
        }, Component.translatable("selectWorld.deleteQuestion"),
                Component.translatable("selectWorld.deleteWarning",
                        world.getLevelName()),
                Component.translatable("selectWorld.deleteButton"),
                CommonComponents.GUI_CANCEL));
    }

    private static void addServer(CustomMainMenuScreen screen, State state) {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData editing = new ServerData(
                I18n.get("selectServer.defaultName"), "", false);
        minecraft.setScreen(new EditServerScreen(screen, accepted -> {
            if (accepted) {
                ServerData hidden = state.serverList.unhide(editing.ip);
                if (hidden != null) hidden.copyNameIconFrom(editing);
                else state.serverList.add(editing, false);
                state.serverList.save();
            }
            refreshServers(state);
            minecraft.setScreen(screen);
        }, editing));
    }

    private static void directConnect(CustomMainMenuScreen screen, State state) {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData editing = new ServerData(
                I18n.get("selectServer.defaultName"), "", false);
        minecraft.setScreen(new DirectJoinServerScreen(screen, accepted -> {
            if (!accepted) {
                minecraft.setScreen(screen);
                return;
            }
            ServerData saved = state.serverList.get(editing.ip);
            if (saved == null) {
                state.serverList.add(editing, true);
                state.serverList.save();
                saved = editing;
            }
            connectServer(screen, saved);
        }, editing));
    }

    private static void editServer(CustomMainMenuScreen screen,
            State state, ServerData original) {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData editing = new ServerData(original.name, original.ip, false);
        editing.copyFrom(original);
        minecraft.setScreen(new EditServerScreen(screen, accepted -> {
            if (accepted) {
                original.name = editing.name;
                original.ip = editing.ip;
                original.copyFrom(editing);
                state.serverList.save();
            }
            refreshServers(state);
            minecraft.setScreen(screen);
        }, editing));
    }

    private static void deleteServer(CustomMainMenuScreen screen,
            State state, ServerData server) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                state.serverList.remove(server);
                state.serverList.save();
            }
            refreshServers(state);
            minecraft.setScreen(screen);
        }, Component.translatable("selectServer.deleteQuestion"),
                Component.translatable("selectServer.deleteWarning", server.name),
                Component.translatable("selectServer.deleteButton"),
                CommonComponents.GUI_CANCEL));
    }

    private static void connectServer(CustomMainMenuScreen screen,
            ServerData server) {
        ConnectScreen.startConnecting(screen, Minecraft.getInstance(),
                ServerAddress.parseString(server.ip), server, false);
    }

    private static void ensureWorlds(State state) {
        if (state.worldFuture != null || state.worldsReady) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            var candidates = minecraft.getLevelSource().findLevelCandidates();
            if (candidates.isEmpty()) {
                state.worlds = List.of();
                state.worldsReady = true;
                return;
            }
            state.worldFuture = minecraft.getLevelSource()
                    .loadLevelSummaries(candidates);
        } catch (Exception ignored) {
            state.worlds = List.of();
            state.worldsReady = true;
        }
    }

    private static void pollWorlds(State state) {
        if (state.worldFuture == null || !state.worldFuture.isDone()) return;
        try {
            List<LevelSummary> loaded = new ArrayList<>(state.worldFuture.join());
            loaded.sort(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed());
            state.worlds = List.copyOf(loaded);
        } catch (CompletionException ignored) {
            state.worlds = List.of();
        }
        state.worldFuture = null;
        state.worldsReady = true;
    }

    private static void refreshWorlds(State state) {
        closeIcons(state.worldIcons);
        state.worlds = List.of();
        state.worldsReady = false;
        state.worldFuture = null;
        state.scrollOffset = 0;
        state.contextIndex = -1;
        ensureWorlds(state);
    }

    private static void ensureServers(State state) {
        if (state.serverList != null) return;
        refreshServers(state);
    }

    private static void refreshServers(State state) {
        if (state.serverPinger != null) state.serverPinger.removeAll();
        closeIcons(state.serverIcons);
        state.dirtyServerIcons.clear();
        state.serverList = new ServerList(Minecraft.getInstance());
        state.serverList.load();
        state.servers = new ArrayList<>();
        for (int i = 0; i < state.serverList.size(); i++) {
            state.servers.add(state.serverList.get(i));
        }
        state.serverPinger = new ServerStatusPinger();
        state.scrollOffset = 0;
        state.contextIndex = -1;
        for (ServerData server : state.servers) ping(state, server);
    }

    private static void ping(State state, ServerData server) {
        server.pinged = true;
        try {
            state.serverPinger.pingServer(server, () -> {
                ServerList.saveSingleServer(server);
                state.dirtyServerIcons.add(server.ip);
            });
        } catch (UnknownHostException ignored) {
            server.motd = Component.translatable("multiplayer.status.cannot_connect");
            server.status = CommonComponents.EMPTY;
            server.ping = -1L;
        }
    }

    private static List<Row> rows(State state) {
        List<Row> rows = new ArrayList<>();
        if (state.mode == Mode.SINGLEPLAYER) {
            if (state.worldsReady && !state.worlds.isEmpty()) {
                rows.add(new Row(RowKind.CONTINUE, state.worlds.get(0)));
            }
            rows.add(new Row(RowKind.CREATE_WORLD, null));
            for (LevelSummary world : state.worlds) {
                rows.add(new Row(RowKind.WORLD, world));
            }
        } else {
            rows.add(new Row(RowKind.ADD_SERVER, null));
            rows.add(new Row(RowKind.DIRECT_CONNECT, null));
            for (ServerData server : state.servers) {
                rows.add(new Row(RowKind.SERVER, server));
            }
        }
        return rows;
    }

    private static void drawHeader(GuiGraphics graphics, State state,
            Layout layout, int panelX, int alpha, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        String title = state.mode == Mode.SINGLEPLAYER
                ? "SINGLEPLAYER" : "MULTIPLAYER";
        drawScaled(graphics, font, ScpFonts.montserrat(title),
                panelX + 4, layout.y + 8, 1.06F,
                withAlpha(TEXT, alpha));
        graphics.fill(panelX, layout.y + HEADER_HEIGHT - 2,
                panelX + layout.width, layout.y + HEADER_HEIGHT,
                withAlpha(ACCENT, alpha));
        if (state.mode == Mode.MULTIPLAYER) {
            int x = panelX + layout.width - 86;
            int y = layout.y + 4;
            boolean hovered = contains(mouseX, mouseY, x, y, 78, 22);
            graphics.fill(x, y, x + 78, y + 22,
                    withAlpha(hovered ? BUTTON_HOVER : BUTTON_BASE, alpha));
            graphics.fill(x, y, x + 3, y + 22,
                    withAlpha(ACCENT, alpha));
            graphics.drawCenteredString(font, ScpFonts.roboto("Refresh"),
                    x + 40, y + 7, withAlpha(TEXT, alpha));
        }
    }

    private static void drawRow(GuiGraphics graphics, Font font, State state,
            Row row, int x, int y, int width, int alpha,
            boolean hovered, boolean optionsHovered) {
        graphics.fill(x, y, x + width, y + ROW_HEIGHT,
                withAlpha(hovered ? BUTTON_HOVER : BUTTON_BASE, alpha));
        graphics.fill(x, y, x + 4, y + ROW_HEIGHT,
                withAlpha(hovered ? ACCENT_BRIGHT : ACCENT, alpha));

        int textX = x + 14;
        if (row.kind == RowKind.WORLD) {
            LevelSummary world = (LevelSummary) row.value;
            ResourceLocation icon = worldIcon(state, world);
            drawIcon(graphics, icon, x + 10, y + 7, 36);
            textX = x + 54;
            drawScaled(graphics, font, ScpFonts.roboto(world.getLevelName()),
                    textX, y + 8, 1.08F, withAlpha(TEXT, alpha));
            drawScaled(graphics, font,
                    ScpFonts.titillium(WORLD_DATE.format(
                            new Date(world.getLastPlayed()))),
                    textX, y + 29, 0.88F, withAlpha(SUBTEXT, alpha));
            drawOptions(graphics, font, x, y, width, alpha, optionsHovered);
            return;
        }
        if (row.kind == RowKind.SERVER) {
            ServerData server = (ServerData) row.value;
            ResourceLocation icon = serverIcon(state, server);
            drawIcon(graphics, icon, x + 10, y + 7, 36);
            textX = x + 54;
            drawScaled(graphics, font, ScpFonts.roboto(server.name),
                    textX, y + 5, 1.03F, withAlpha(TEXT, alpha));
            String motd = server.motd == null ? "Pinging..."
                    : compact(font, server.motd.getString(),
                    Math.max(40, width - 150));
            drawScaled(graphics, font, ScpFonts.titillium(motd),
                    textX, y + 22, 0.82F, withAlpha(SUBTEXT, alpha));
            String players = server.players == null ? "-- / --"
                    : server.players.online() + " / " + server.players.max();
            drawScaled(graphics, font, ScpFonts.titillium(players),
                    x + width - 104, y + 35, 0.78F,
                    withAlpha(ACCENT_BRIGHT, alpha));
            drawOptions(graphics, font, x, y, width, alpha, optionsHovered);
            return;
        }

        String title;
        String subtitle;
        if (row.kind == RowKind.CONTINUE) {
            LevelSummary world = (LevelSummary) row.value;
            title = "Continue";
            subtitle = world.getLevelName();
        } else if (row.kind == RowKind.CREATE_WORLD) {
            title = "Create New World";
            subtitle = "Start a new local world";
        } else if (row.kind == RowKind.ADD_SERVER) {
            title = "Add Server";
            subtitle = "Save a server to this list";
        } else {
            title = "Direct Connection";
            subtitle = "Connect without adding a visible entry";
        }
        drawScaled(graphics, font, ScpFonts.roboto(title),
                textX, y + 8, 1.08F, withAlpha(TEXT, alpha));
        drawScaled(graphics, font, ScpFonts.titillium(subtitle),
                textX, y + 29, 0.86F, withAlpha(SUBTEXT, alpha));
    }

    private static void drawOptions(GuiGraphics graphics, Font font,
            int x, int y, int width, int alpha, boolean hovered) {
        int bx = x + width - 38;
        graphics.fill(bx, y + 7, bx + 30, y + 43,
                withAlpha(hovered ? BUTTON_HOVER : 0x66161B22, alpha));
        graphics.drawCenteredString(font, "...", bx + 15, y + 20,
                withAlpha(TEXT, alpha));
    }

    private static void drawContextMenu(GuiGraphics graphics, Font font,
            State state, List<Row> rows, Layout layout, int panelX,
            int alpha, int mouseX, int mouseY) {
        if (state.contextIndex < 0 || state.contextIndex >= rows.size()) return;
        Row row = rows.get(state.contextIndex);
        if (!row.hasOptions()) return;
        int visible = state.contextIndex - state.scrollOffset;
        if (visible < 0 || visible >= layout.visibleRows) return;
        int rowY = layout.listY + visible * (ROW_HEIGHT + GAP);
        String[] options = row.kind == RowKind.WORLD
                ? new String[]{"Edit", "Re-create", "Delete"}
                : new String[]{"Edit", "Delete"};
        int popupW = 108;
        int optionH = 26;
        int popupX = panelX + layout.width - popupW - 6;
        int popupY = rowY + ROW_HEIGHT + 2;
        int listBottom = layout.listY
                + layout.visibleRows * (ROW_HEIGHT + GAP) - GAP;
        if (popupY + options.length * optionH > listBottom) {
            popupY = rowY - options.length * optionH - 2;
        }
        graphics.fill(popupX - 1, popupY - 1, popupX + popupW + 1,
                popupY + options.length * optionH + 1,
                withAlpha(POPUP, alpha));
        for (int i = 0; i < options.length; i++) {
            int oy = popupY + i * optionH;
            boolean hovered = contains(mouseX, mouseY,
                    popupX, oy, popupW, optionH);
            graphics.fill(popupX, oy, popupX + popupW, oy + optionH,
                    withAlpha(hovered ? BUTTON_HOVER : BUTTON_BASE, alpha));
            graphics.fill(popupX, oy, popupX + 3, oy + optionH,
                    withAlpha(i == options.length - 1 && options[i].equals("Delete")
                            ? 0xFFD45B5B : ACCENT, alpha));
            graphics.drawString(font, ScpFonts.roboto(options[i]),
                    popupX + 10, oy + 9, withAlpha(TEXT, alpha), false);
        }
    }

    private static ResourceLocation worldIcon(State state, LevelSummary world) {
        FaviconTexture existing = state.worldIcons.get(world.getLevelId());
        if (existing != null) return existing.textureLocation();
        FaviconTexture favicon = FaviconTexture.forWorld(
                Minecraft.getInstance().getTextureManager(), world.getLevelId());
        state.worldIcons.put(world.getLevelId(), favicon);
        Path path = world.getIcon();
        if (path != null && Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                favicon.upload(NativeImage.read(input));
            } catch (Exception ignored) {
            }
        }
        return favicon.textureLocation();
    }

    private static ResourceLocation serverIcon(State state, ServerData server) {
        if (state.dirtyServerIcons.remove(server.ip)) {
            FaviconTexture old = state.serverIcons.remove(server.ip);
            if (old != null) old.close();
        }
        FaviconTexture existing = state.serverIcons.get(server.ip);
        if (existing != null) return existing.textureLocation();
        byte[] bytes = server.getIconBytes();
        if (bytes == null) return UNKNOWN_SERVER;
        FaviconTexture favicon = FaviconTexture.forServer(
                Minecraft.getInstance().getTextureManager(), server.ip);
        state.serverIcons.put(server.ip, favicon);
        try (InputStream input = new ByteArrayInputStream(bytes)) {
            favicon.upload(NativeImage.read(input));
        } catch (Exception ignored) {
        }
        return favicon.textureLocation();
    }

    private static void drawIcon(GuiGraphics graphics, ResourceLocation icon,
            int x, int y, int size) {
        RenderSystem.enableBlend();
        graphics.blit(icon, x, y, 0.0F, 0.0F,
                size, size, size, size);
        RenderSystem.disableBlend();
    }

    private static Layout layout(CustomMainMenuScreen screen, int rowCount) {
        int primaryLeft = Math.max(42, Math.round(screen.width * 0.073F));
        int primaryWidth = Mth.clamp(Math.round(screen.width * 0.265F),
                220, 330);
        int x = primaryLeft + primaryWidth + 16;
        int y = Math.round(screen.height * 0.385F);
        int width = Mth.clamp(Math.round(screen.width * 0.365F), 310, 470);
        width = Math.min(width, Math.max(230, screen.width - x - 28));
        int listY = y + HEADER_HEIGHT + 8;
        int available = Math.max(ROW_HEIGHT,
                screen.height - listY - Math.max(18,
                        Math.round(screen.height * 0.035F)));
        int visibleRows = Mth.clamp((available + GAP) / (ROW_HEIGHT + GAP),
                1, Math.max(1, Math.min(rowCount, 7)));
        return new Layout(x, y, width, listY, visibleRows);
    }

    private static int rowAt(Layout layout, int panelX,
            double mouseX, double mouseY) {
        if (mouseX < panelX || mouseX >= panelX + layout.width
                || mouseY < layout.listY) return -1;
        int relative = (int) mouseY - layout.listY;
        int stride = ROW_HEIGHT + GAP;
        int row = relative / stride;
        if (row < 0 || row >= layout.visibleRows
                || relative % stride >= ROW_HEIGHT) return -1;
        return row;
    }

    private static AbstractButton findNamedButton(CustomMainMenuScreen screen,
            String name) {
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractButton button
                    && name.equalsIgnoreCase(button.getMessage().getString())) {
                return button;
            }
        }
        return null;
    }

    private static boolean isOver(AbstractButton button,
            ScreenEvent.MouseButtonPressed.Pre event) {
        return button != null && button.isMouseOver(
                event.getMouseX(), event.getMouseY());
    }

    private static void closeOtherPanels(CustomMainMenuScreen screen) {
        closeExtras(screen);
        closeSettings(screen);
    }

    private static void closeExtras(CustomMainMenuScreen screen) {
        try {
            Field field = CustomMainMenuScreen.class.getDeclaredField("extrasOpen");
            field.setAccessible(true);
            field.setBoolean(screen, false);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void closeSettings(CustomMainMenuScreen screen) {
        try {
            Field statesField = MainMenuSettingsPanelClient.class
                    .getDeclaredField("STATES");
            statesField.setAccessible(true);
            Map<CustomMainMenuScreen, ?> states =
                    (Map<CustomMainMenuScreen, ?>) statesField.get(null);
            Object settingsState = states.get(screen);
            if (settingsState == null) return;
            Field openField = settingsState.getClass().getDeclaredField("open");
            openField.setAccessible(true);
            openField.setBoolean(settingsState, false);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void closeIcons(Map<String, FaviconTexture> icons) {
        for (FaviconTexture icon : icons.values()) {
            try {
                icon.close();
            } catch (Exception ignored) {
            }
        }
        icons.clear();
    }

    private static String compact(Font font, String text, int width) {
        if (text == null) return "";
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("...")))
                + "...";
    }

    private static void drawScaled(GuiGraphics graphics, Font font,
            Component text, int x, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static boolean contains(double mouseX, double mouseY,
            int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static int withAlpha(int color, int alpha) {
        int base = (color >>> 24) & 0xFF;
        int combined = Math.round(base * (alpha / 255.0F));
        return (combined << 24) | (color & 0x00FFFFFF);
    }

    private static float approach(float current, float target, float amount) {
        if (current < target) return Math.min(target, current + amount);
        return Math.max(target, current - amount);
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static void playHover() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.25F, 0.28F));
    }

    private static void playSelect() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.55F));
    }

    private enum Mode {
        SINGLEPLAYER,
        MULTIPLAYER
    }

    private enum RowKind {
        CONTINUE,
        CREATE_WORLD,
        WORLD,
        ADD_SERVER,
        DIRECT_CONNECT,
        SERVER
    }

    private record Row(RowKind kind, Object value) {
        boolean hasOptions() {
            return kind == RowKind.WORLD || kind == RowKind.SERVER;
        }
    }

    private record Layout(int x, int y, int width, int listY,
            int visibleRows) {
        int height() {
            return HEADER_HEIGHT + 8
                    + visibleRows * (ROW_HEIGHT + GAP) - GAP;
        }
    }

    private static final class State {
        private Mode mode = Mode.SINGLEPLAYER;
        private boolean open;
        private float progress;
        private long lastFrameAt;
        private int scrollOffset;
        private int contextIndex = -1;
        private int hoveredToken = Integer.MIN_VALUE;

        private CompletableFuture<List<LevelSummary>> worldFuture;
        private List<LevelSummary> worlds = List.of();
        private boolean worldsReady;
        private final Map<String, FaviconTexture> worldIcons = new HashMap<>();

        private ServerList serverList;
        private List<ServerData> servers = new ArrayList<>();
        private ServerStatusPinger serverPinger;
        private final Map<String, FaviconTexture> serverIcons = new HashMap<>();
        private final Set<String> dirtyServerIcons = ConcurrentHashMap.newKeySet();
    }
}
