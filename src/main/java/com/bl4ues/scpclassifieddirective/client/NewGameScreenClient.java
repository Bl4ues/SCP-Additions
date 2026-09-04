package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.mixin.client.CreateWorldScreenInvoker;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Modern presentation layer over vanilla's real CreateWorldScreen.
 *
 * <p>The underlying screen and WorldCreationUiState remain authoritative so
 * Forge hooks, data-pack state and mod-injected creation controls keep their
 * normal lifecycle. Only the presentation is replaced.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NewGameScreenClient {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9FA6AD;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int CARD = 0xB20B0E12;
    private static final int BORDER = 0x70444C57;

    private static final Map<CreateWorldScreen, State> STATES =
            new WeakHashMap<>();

    private NewGameScreenClient() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) return;

        State existing = STATES.get(screen);
        if (existing != null) {
            existing.captureNewForeignWidgets(event.getListenersList());
            existing.hideVanillaWidgets();
            existing.refreshAfterReturn();
            return;
        }
        if (!NewGameCreationClient.consumeCustomPresentation(screen)) return;

        Screen parent = NewGameCreationClient.consumePendingParent();
        State state = new State(screen, parent, event.getListenersList());
        STATES.put(screen, state);
        state.initializeDefaults();
        state.buildControls(event);
        state.hideVanillaWidgets();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null) return;

        state.captureNewForeignWidgets(screen.children());
        state.hideVanillaWidgets();
        state.render(event.getGuiGraphics(), event.getMouseX(),
                event.getMouseY(), event.getPartialTick());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null || event.getScrollDelta() == 0.0D) return;
        if (state.scroll(event.getMouseX(), event.getMouseY(),
                event.getScrollDelta())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) return;
        State state = STATES.get(screen);
        if (state == null) return;
        if (event.getKeyCode() == 256 && state.beginExit()) event.setCanceled(true);
    }

    private static final class State {
        private final CreateWorldScreen screen;
        private final Screen parent;
        private final WorldCreationUiState ui;
        private final Set<GuiEventListener> vanilla =
                Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<GuiEventListener> custom =
                Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<GuiEventListener> foreign = new ArrayList<>();

        private EditBox worldName;
        private NewGameWidgets.Dropdown<GameModeChoice> gameMode;
        private NewGameWidgets.Dropdown<DifficultyChoice> difficulty;
        private NewGameWidgets.Toggle cheats;
        private NewGameWidgets.ActionButton back;
        private NewGameWidgets.ActionButton start;
        private NewGameWorldCard worldCard;

        private DifficultyChoice normalDifficulty = DifficultyChoice.EUCLID;
        private boolean closing;
        private float scrollOffset;

        private State(CreateWorldScreen screen, Screen parent,
                List<GuiEventListener> initialListeners) {
            this.screen = screen;
            this.parent = parent;
            this.ui = screen.getUiState();
            vanilla.addAll(initialListeners);
        }

        private void initializeDefaults() {
            ui.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
            ui.setDifficulty(Difficulty.NORMAL);
            ui.setAllowCheats(false);
            normalDifficulty = DifficultyChoice.EUCLID;
        }

        private void buildControls(ScreenEvent.Init.Post event) {
            Font font = Minecraft.getInstance().font;
            worldName = own(event, new EditBox(font, 0, 0, 260, 28,
                    ScpFonts.roboto("World Name")));
            worldName.setBordered(false);
            worldName.setMaxLength(32);
            worldName.setTextColor(TEXT);
            worldName.setTextColorUneditable(MUTED);
            worldName.setValue(ui.getName());
            worldName.setResponder(ui::setName);

            gameMode = own(event, new NewGameWidgets.Dropdown<>(
                    0, 0, 260, 30,
                    List.of(
                            new NewGameWidgets.Dropdown.Entry<>(GameModeChoice.SURVIVAL, "Survival"),
                            new NewGameWidgets.Dropdown.Entry<>(GameModeChoice.CREATIVE, "Creative"),
                            new NewGameWidgets.Dropdown.Entry<>(GameModeChoice.APOLLYON, "Apollyon")
                    ), GameModeChoice.SURVIVAL, this::setGameMode,
                    choice -> Component.literal(choice == null ? "Survival" : choice.label)));

            difficulty = own(event, new NewGameWidgets.Dropdown<>(
                    0, 0, 260, 30,
                    List.of(
                            new NewGameWidgets.Dropdown.Entry<>(DifficultyChoice.THAUMIEL, "Thaumiel"),
                            new NewGameWidgets.Dropdown.Entry<>(DifficultyChoice.SAFE, "Safe"),
                            new NewGameWidgets.Dropdown.Entry<>(DifficultyChoice.EUCLID, "Euclid"),
                            new NewGameWidgets.Dropdown.Entry<>(DifficultyChoice.KETER, "Keter")
                    ), DifficultyChoice.EUCLID, this::setDifficulty,
                    choice -> Component.literal(choice == null ? "Euclid" : choice.title)));

            cheats = own(event, new NewGameWidgets.Toggle(
                    0, 0, 260, 30, "Allow Cheats", false, ui::setAllowCheats));
            back = own(event, new NewGameWidgets.ActionButton(
                    0, 0, 150, 32, ScpFonts.roboto("Back"), this::beginExit));
            start = own(event, new NewGameWidgets.ActionButton(
                    0, 0, 190, 32, ScpFonts.roboto("Start Game"), this::startGame));

            worldCard = new NewGameWorldCard(screen, ui, listener -> {
                custom.add(listener);
                event.addListener(listener);
            });
        }

        private <T extends GuiEventListener> T own(ScreenEvent.Init.Post event, T listener) {
            custom.add(listener);
            event.addListener(listener);
            return listener;
        }

        private void refreshAfterReturn() {
            worldName.setValue(ui.getName());
            worldCard.refreshEntries();
        }

        private void captureNewForeignWidgets(List<? extends GuiEventListener> listeners) {
            for (GuiEventListener listener : listeners) {
                if (custom.contains(listener) || vanilla.contains(listener)
                        || foreign.contains(listener)) continue;
                foreign.add(listener);
            }
        }

        private void hideVanillaWidgets() {
            for (GuiEventListener listener : vanilla) {
                if (listener instanceof AbstractWidget widget) widget.visible = false;
            }
            for (GuiEventListener listener : foreign) {
                if (listener instanceof AbstractWidget widget) widget.visible = false;
            }
            for (GuiEventListener listener : custom) {
                if (listener instanceof AbstractWidget widget) widget.visible = true;
            }
        }

        private void setGameMode(GameModeChoice choice) {
            if (choice == null) return;
            gameMode.setSelected(choice);
            if (choice == GameModeChoice.APOLLYON) {
                if (!ui.isHardcore()) normalDifficulty = DifficultyChoice.from(ui.getDifficulty());
                ui.setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
                ui.setAllowCheats(false);
                cheats.setValue(false);
                cheats.active = false;
                difficulty.lock(ScpFonts.roboto("Apollyon"));
            } else {
                ui.setGameMode(choice.mode);
                ui.setDifficulty(normalDifficulty.difficulty);
                cheats.active = true;
                difficulty.unlock();
                difficulty.setSelected(normalDifficulty);
            }
            worldCard.refreshEntries();
        }

        private void setDifficulty(DifficultyChoice choice) {
            if (choice == null || ui.isHardcore()) return;
            normalDifficulty = choice;
            difficulty.setSelected(choice);
            ui.setDifficulty(choice.difficulty);
        }

        private DifficultyChoice displayedDifficulty() {
            return ui.isHardcore() ? DifficultyChoice.APOLLYON
                    : DifficultyChoice.from(ui.getDifficulty());
        }

        private void startGame() {
            if (closing) return;
            ui.setName(worldName.getValue());
            try {
                ((CreateWorldScreenInvoker) screen).scpclassifieddirective$invokeCreate();
            } catch (RuntimeException exception) {
                ScpClassifiedDirectiveMod.LOGGER.error(
                        "Could not start world creation from the New Game screen", exception);
            }
        }

        private boolean beginExit() {
            if (closing) return true;
            closing = true;
            ConfigCenterVisuals.beginExit();
            setCustomActive(false);
            return true;
        }

        private void finishExit() {
            if (!closing || !ConfigCenterVisuals.exitComplete()) return;
            closing = false;
            if (parent instanceof CustomMainMenuScreen menu) {
                menu.resumeFromConfiguration(ConfigCenterVisuals.outerAngle(),
                        ConfigCenterVisuals.innerAngle(), ConfigCenterVisuals.capturedBackground());
                Minecraft.getInstance().setScreen(menu);
            } else if (parent != null) {
                Minecraft.getInstance().setScreen(parent);
            } else {
                screen.onClose();
            }
        }

        private void setCustomActive(boolean active) {
            for (GuiEventListener listener : custom) {
                if (listener instanceof AbstractWidget widget) widget.active = active;
            }
        }

        private boolean scroll(double mouseX, double mouseY, double delta) {
            Layout layout = layout();
            if (mouseX < layout.x || mouseX >= layout.x + layout.width
                    || mouseY < layout.viewportTop || mouseY >= layout.viewportBottom) return false;
            float max = Math.max(0.0F, layout.contentHeight
                    - (layout.viewportBottom - layout.viewportTop));
            if (max <= 0.0F) return false;
            scrollOffset = Mth.clamp(scrollOffset + (delta > 0.0D ? -42.0F : 42.0F),
                    0.0F, max);
            return true;
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ConfigCenterVisuals.renderBackdrop(screen, graphics, mouseX, mouseY);
            Layout layout = layout();
            float alpha = ConfigCenterVisuals.contentAlpha();

            drawHeader(graphics, layout, alpha);
            positionControls(layout);

            graphics.enableScissor(layout.x, layout.viewportTop,
                    layout.x + layout.width, layout.viewportBottom);
            drawGameCard(graphics, layout, alpha);
            renderGameControls(graphics, mouseX, mouseY, partialTick);
            int worldY = layout.worldY - Math.round(scrollOffset);
            worldCard.renderBackground(graphics, layout.x, worldY, layout.width, alpha);
            worldCard.renderControls(graphics, mouseX, mouseY, partialTick);
            graphics.disableScissor();

            drawScrollbar(graphics, layout, alpha);
            drawFooter(graphics, layout, mouseX, mouseY, partialTick);
            if (closing) finishExit();
        }

        private void drawHeader(GuiGraphics graphics, Layout layout, float alpha) {
            Font font = Minecraft.getInstance().font;
            drawScaled(graphics, font, ScpFonts.montserrat("NEW GAME"),
                    layout.x, layout.headerY, 1.42F, applyAlpha(TEXT, alpha));
            drawScaled(graphics, font, ScpFonts.titillium("Start a new world"),
                    layout.x, layout.headerY + 25, 1.04F, applyAlpha(MUTED, alpha));
            graphics.fill(layout.x, layout.headerY + 48,
                    layout.x + layout.width, layout.headerY + 50, applyAlpha(ACCENT, alpha));
        }

        private void drawGameCard(GuiGraphics graphics, Layout layout, float alpha) {
            int cardX = layout.x;
            int cardY = layout.gameY - Math.round(scrollOffset);
            int cardW = layout.width;
            int cardH = layout.gameHeight;
            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, applyAlpha(CARD, alpha));
            graphics.fill(cardX, cardY, cardX + 4, cardY + cardH,
                    applyAlpha(ACCENT, alpha * 0.9F));
            graphics.fill(cardX + 18, cardY + 18, cardX + cardW - 18, cardY + 19,
                    applyAlpha(BORDER, alpha));

            int left = cardX + layout.innerPad;
            int controlsW = layout.controlsWidth;
            label(graphics, "WORLD NAME", left, cardY + 32, alpha);
            label(graphics, "GAME MODE", left, cardY + 98, alpha);
            label(graphics, "DIFFICULTY", left, cardY + 164, alpha);

            int summaryX = cardX + layout.summaryXOffset;
            int summaryY = cardY + 32;
            graphics.fill(summaryX - 18, cardY + 26, summaryX - 17, cardY + cardH - 24,
                    applyAlpha(BORDER, alpha));
            drawDifficultySummary(graphics, displayedDifficulty(),
                    summaryX, summaryY, layout.summaryWidth, alpha);
            graphics.fill(left, cardY + 252, left + controlsW, cardY + 253,
                    applyAlpha(BORDER, alpha * 0.70F));
        }

        private void positionControls(Layout layout) {
            int cardY = layout.gameY - Math.round(scrollOffset);
            int x = layout.x + layout.innerPad;
            int w = layout.controlsWidth;
            worldName.setX(x); worldName.setY(cardY + 52); worldName.setWidth(w); worldName.setHeight(30);
            gameMode.setX(x); gameMode.setY(cardY + 118); gameMode.setWidth(w); gameMode.setHeight(30);
            difficulty.setX(x); difficulty.setY(cardY + 184); difficulty.setWidth(w); difficulty.setHeight(30);
            cheats.setX(x); cheats.setY(cardY + 274); cheats.setWidth(w); cheats.setHeight(32);

            int worldY = layout.worldY - Math.round(scrollOffset);
            worldCard.position(layout.x, worldY, layout.width);

            back.setX(layout.x); back.setY(layout.footerY);
            back.setWidth(Math.min(160, Math.max(120, layout.width / 4))); back.setHeight(34);
            start.setWidth(Math.min(210, Math.max(170, layout.width / 3)));
            start.setX(layout.x + layout.width - start.getWidth()); start.setY(layout.footerY); start.setHeight(34);
        }

        private void renderGameControls(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            drawEditSurface(graphics, worldName);
            worldName.render(graphics, mouseX, mouseY, partialTick);
            cheats.render(graphics, mouseX, mouseY, partialTick);
            gameMode.render(graphics, mouseX, mouseY, partialTick);
            difficulty.render(graphics, mouseX, mouseY, partialTick);
        }

        private void drawScrollbar(GuiGraphics graphics, Layout layout, float alpha) {
            int viewport = layout.viewportBottom - layout.viewportTop;
            int max = Math.max(0, layout.contentHeight - viewport);
            if (max <= 0) return;
            int trackX = layout.x + layout.width + 6;
            graphics.fill(trackX, layout.viewportTop, trackX + 2, layout.viewportBottom,
                    applyAlpha(0x553A424D, alpha));
            int thumbH = Math.max(20, Math.round(viewport * viewport / (float) layout.contentHeight));
            int travel = Math.max(0, viewport - thumbH);
            int thumbY = layout.viewportTop + Math.round(travel * (scrollOffset / max));
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH,
                    applyAlpha(ACCENT, alpha));
        }

        private void drawFooter(GuiGraphics graphics, Layout layout,
                int mouseX, int mouseY, float partialTick) {
            float alpha = ConfigCenterVisuals.contentAlpha();
            graphics.fill(layout.x, layout.footerY - 12,
                    layout.x + layout.width, layout.footerY - 11, applyAlpha(BORDER, alpha));
            back.render(graphics, mouseX, mouseY, partialTick);
            start.render(graphics, mouseX, mouseY, partialTick);
        }

        private Layout layout() {
            int width = Mth.clamp(Math.round(screen.width * 0.56F),
                    Math.min(500, Math.max(320, screen.width - 32)), 820);
            width = Math.min(width, Math.max(300, screen.width - 30));
            int x = ConfigCenterVisuals.contentLeft(screen.width, width);
            int headerY = Math.max(26, Math.round(screen.height * 0.065F));
            int viewportTop = headerY + 62;
            int footerY = screen.height - Math.max(48, Math.round(screen.height * 0.075F));
            int viewportBottom = Math.max(viewportTop + 120, footerY - 18);
            int gameY = viewportTop + 6;
            int gameHeight = Mth.clamp(Math.round(screen.height * 0.54F), 330, 430);
            int worldY = gameY + gameHeight + 12;
            int worldHeight = worldCard == null ? 224 : worldCard.height(width);
            int contentHeight = worldY + worldHeight - viewportTop + 8;
            int innerPad = Mth.clamp(Math.round(width * 0.035F), 18, 30);
            int controlsWidth = Mth.clamp(Math.round(width * 0.40F), 210, 310);
            int summaryXOffset = innerPad + controlsWidth
                    + Mth.clamp(Math.round(width * 0.055F), 34, 54);
            int summaryWidth = Math.max(150, width - summaryXOffset - innerPad);
            return new Layout(x, width, headerY, viewportTop, viewportBottom,
                    footerY, gameY, gameHeight, worldY, worldHeight, contentHeight,
                    innerPad, controlsWidth, summaryXOffset, summaryWidth);
        }
    }

    private enum GameModeChoice {
        SURVIVAL("Survival", WorldCreationUiState.SelectedGameMode.SURVIVAL),
        CREATIVE("Creative", WorldCreationUiState.SelectedGameMode.CREATIVE),
        APOLLYON("Apollyon", WorldCreationUiState.SelectedGameMode.HARDCORE);
        private final String label;
        private final WorldCreationUiState.SelectedGameMode mode;
        GameModeChoice(String label, WorldCreationUiState.SelectedGameMode mode) {
            this.label = label; this.mode = mode;
        }
    }

    private enum DifficultyChoice {
        THAUMIEL("Thaumiel", "Peaceful Exploration", Difficulty.PEACEFUL, "thaumiel.png", List.of(
                "Minecraft's Peaceful Difficulty.",
                "Quicksaves, Decontamination Checkpoints, and Default Saves are available.",
                "SCP roaming encounters are disabled.")),
        SAFE("Safe", "Easy Containment", Difficulty.EASY, "safe.png", List.of(
                "Minecraft's Easy Difficulty.",
                "Quicksaves, Decontamination Checkpoints, and Default Saves are available.",
                "SCP roaming encounters occur less frequently.",
                "Tesla Gate suppression keeps threats away for longer.")),
        EUCLID("Euclid", "Standard Conditions", Difficulty.NORMAL, "euclid.png", List.of(
                "Minecraft's Normal Difficulty.",
                "Only Decontamination Checkpoints and Default Saves are available.",
                "SCP roaming encounters occur in a standard frequency.",
                "Standard Tesla Gate suppression against threats.")),
        KETER("Keter", "Critical Containment", Difficulty.HARD, "keter.png", List.of(
                "Minecraft's Hard Difficulty.",
                "Only Default Saves are available.",
                "SCP roaming encounters occur very frequently.",
                "Standard Tesla Gate suppression against threats.")),
        APOLLYON("Apollyon", "Hardcore Containment", Difficulty.HARD, "apollyon.png", List.of(
                "Minecraft's Hard Difficulty.",
                "Permanent Death.",
                "Saving is disabled.",
                "SCP roaming encounters occur very frequently.",
                "Standard Tesla Gate suppression against threats.",
                "Available only during world creation."));

        private final String title;
        private final String subtitle;
        private final Difficulty difficulty;
        private final ResourceLocation icon;
        private final List<String> bullets;
        DifficultyChoice(String title, String subtitle, Difficulty difficulty,
                String icon, List<String> bullets) {
            this.title = title; this.subtitle = subtitle; this.difficulty = difficulty;
            this.icon = new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "textures/gui/" + icon);
            this.bullets = bullets;
        }
        private static DifficultyChoice from(Difficulty difficulty) {
            if (difficulty == Difficulty.PEACEFUL) return THAUMIEL;
            if (difficulty == Difficulty.EASY) return SAFE;
            if (difficulty == Difficulty.HARD) return KETER;
            return EUCLID;
        }
    }

    private static void drawDifficultySummary(GuiGraphics graphics,
            DifficultyChoice choice, int x, int y, int width, float alpha) {
        Font font = Minecraft.getInstance().font;
        int iconSize = Mth.clamp(Math.round(width * 0.19F), 42, 64);
        drawDifficultyIcon(graphics, choice.icon, x, y, iconSize, alpha);
        int titleX = x + iconSize + 14;
        drawScaled(graphics, font, ScpFonts.montserrat(choice.title.toUpperCase(Locale.ROOT)),
                titleX, y + 4, 1.18F, applyAlpha(TEXT, alpha));
        drawScaled(graphics, font, ScpFonts.titillium(choice.subtitle),
                titleX, y + 24, 0.98F, applyAlpha(ACCENT_BRIGHT, alpha));
        if (choice == DifficultyChoice.EUCLID) {
            drawScaled(graphics, font, ScpFonts.roboto("RECOMMENDED"),
                    titleX, y + 43, 0.82F, applyAlpha(ACCENT, alpha));
        }
        int lineY = y + iconSize + 16;
        int textX = x + 14;
        int textWidth = Math.max(80, width - 18);
        for (String bullet : choice.bullets) {
            List<String> lines = wrap(font, bullet, Math.max(70, textWidth - 22));
            int markerY = lineY + 4;
            graphics.fill(x + 1, markerY, x + 5, markerY + 7, applyAlpha(ACCENT, alpha));
            for (String line : lines) {
                graphics.drawString(font, ScpFonts.roboto(line), textX, lineY,
                        applyAlpha(TEXT, alpha), false);
                lineY += 12;
            }
            lineY += 6;
        }
    }

    private static void drawDifficultyIcon(GuiGraphics graphics,
            ResourceLocation texture, int x, int y, int size, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager().getResource(texture).isEmpty()) return;
        float scale = size / 128.0F;
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.pose().pushPose(); graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F, 128, 128, 128, 128);
        graphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); RenderSystem.disableBlend();
    }

    private static void drawEditSurface(GuiGraphics graphics, EditBox box) {
        float alpha = ConfigCenterVisuals.contentAlpha();
        int x = box.getX(), y = box.getY(), w = box.getWidth(), h = box.getHeight();
        graphics.fill(x, y, x + w, y + h, applyAlpha(0xC00B0E12, alpha));
        int border = box.isFocused() ? ACCENT : BORDER;
        graphics.fill(x, y, x + w, y + 1, applyAlpha(border, alpha));
        graphics.fill(x, y + h - 1, x + w, y + h, applyAlpha(border, alpha));
        graphics.fill(x, y, x + 1, y + h, applyAlpha(border, alpha));
        graphics.fill(x + w - 1, y, x + w, y + h, applyAlpha(border, alpha));
    }

    private static void label(GuiGraphics graphics, String text, int x, int y, float alpha) {
        drawScaled(graphics, Minecraft.getInstance().font, ScpFonts.montserrat(text),
                x, y, 0.86F, applyAlpha(MUTED, alpha));
    }

    private static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(ScpFonts.roboto(candidate)) > maxWidth) {
                out.add(line.toString()); line.setLength(0); line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' '); line.append(word);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    private static void drawScaled(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose(); graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static int applyAlpha(int color, float alpha) {
        int source = (color >>> 24) & 0xFF;
        int out = Mth.clamp(Math.round(source * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return (out << 24) | (color & 0x00FFFFFF);
    }

    private record Layout(int x, int width, int headerY,
            int viewportTop, int viewportBottom, int footerY,
            int gameY, int gameHeight, int worldY, int worldHeight,
            int contentHeight, int innerPad, int controlsWidth,
            int summaryXOffset, int summaryWidth) {
    }
}
