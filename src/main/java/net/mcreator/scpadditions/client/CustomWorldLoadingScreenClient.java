package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SCP Additions presentation for the complete vanilla world-entry sequence.
 * Vanilla screens remain responsible for loading; this class owns only their
 * presentation and keeps one visual card alive across all intermediate screens.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CustomWorldLoadingScreenClient {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/screens/menu/loading_screen.png");
    private static final ResourceLocation LOGO_OUTER = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/screens/menu/loading_1.png");
    private static final ResourceLocation LOGO_INNER = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/screens/menu/loading_2.png");

    private static final int BACKGROUND_WIDTH = 1920;
    private static final int BACKGROUND_HEIGHT = 1080;
    private static final int LOGO_TEXTURE_SIZE = 512;

    private static final int ACCENT = 0xFFC59A2A;
    private static final int ACCENT_BRIGHT = 0xFFE5D49A;
    private static final int TRACK = 0xFF46505E;
    private static final int TRACK_FAINT = 0x66323A47;
    private static final int TEXT = 0xFFF7F8FC;
    private static final int TEXT_MUTED = 0xFF9CA3AF;
    private static final int CARD_GOLD = 0xFFC99B18;

    private static final long SPINNER_CYCLE_MS = 3200L;
    private static final long SPINNER_ROTATE_MS = 2520L;
    private static final long CARD_FADE_IN_MS = 1400L;
    private static final long CARD_FADE_OUT_MS = 1200L;
    private static final long CARD_LIFETIME_MS = 25_000L;
    private static final float CARD_MAX_ZOOM = 1.16F;
    private static final long SESSION_CLEAR_DELAY_MS = 750L;

    private static final Set<String> READING_KEYS = Set.of(
            "selectWorld.data_read");
    private static final Set<String> PREPARING_KEYS = Set.of(
            "createWorld.preparing",
            "menu.preparingLevel",
            "menu.generatingLevel");
    private static final Set<String> JOINING_KEYS = Set.of(
            "connect.joining",
            "connect.connecting");
    private static final Set<String> TERRAIN_KEYS = Set.of(
            "multiplayer.downloadingTerrain");

    private static volatile Field progressListenerField;
    private static volatile boolean progressFieldLookupAttempted;
    private static volatile boolean reflectionFailureLogged;
    private static volatile boolean renderFailureLogged;
    private static LoadingSession activeSession;

    private CustomWorldLoadingScreenClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!ClientModulePreferences.customLoadingScreenEnabled()) {
            activeSession = null;
            return;
        }

        Screen screen = event.getScreen();
        LoadingPhase candidatePhase = phase(screen);
        if (candidatePhase == null) return;

        try {
            int rawGenerationProgress = -1;
            if (screen instanceof LevelLoadingScreen levelLoadingScreen) {
                StoringChunkProgressListener listener = progressListener(levelLoadingScreen);
                if (listener == null) return;
                rawGenerationProgress = Mth.clamp(listener.getProgress(), 0, 100);
                candidatePhase = rawGenerationProgress >= 100
                        ? LoadingPhase.FINALIZING
                        : LoadingPhase.GENERATING;
            }

            long now = Util.getMillis();
            LoadingSession session = session(now);
            LoadingPhase effectivePhase = session.enterPhase(candidatePhase,
                    Minecraft.getInstance().hasSingleplayerServer(), now);
            session.rotateCardIfNeeded(now);
            render(event.getGuiGraphics(), session, effectivePhase,
                    rawGenerationProgress, now);
            event.setCanceled(true);
        } catch (RuntimeException exception) {
            if (!renderFailureLogged) {
                renderFailureLogged = true;
                ScpAdditionsMod.LOGGER.warn(
                        "Custom world loading screen failed to render; using vanilla loading presentation",
                        exception);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || activeSession == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || minecraft.screen != null) {
            return;
        }
        if (Util.getMillis() - activeSession.lastSeenAt > SESSION_CLEAR_DELAY_MS) {
            activeSession = null;
        }
    }

    /**
     * A world load owns exactly one session. Gaps between vanilla screens are a
     * normal part of Minecraft's integrated-server startup, not a signal that a
     * new load began. Creating a replacement session here used to re-roll the
     * informational card for one or two frames during those gaps.
     */
    private static LoadingSession session(long now) {
        if (activeSession == null) {
            activeSession = new LoadingSession(now,
                    LoadingScreenRegistry.loadDefinitions());
        }
        activeSession.lastSeenAt = now;
        return activeSession;
    }

    private static void render(GuiGraphics graphics, LoadingSession session,
            LoadingPhase phase, int rawGenerationProgress, long now) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        drawCoverTexture(graphics, BACKGROUND, width, height);
        drawCard(graphics, minecraft.font, session, width, height, now);

        int barWidth = Math.min(width - 48,
                Math.max(260, Math.round(width * 0.52F)));
        int spinnerSize = Mth.clamp(height / 10, 30, 46);
        int spinnerGap = Math.max(12, spinnerSize / 3);
        int barX = (width - barWidth) / 2;
        int barY = Math.round(height * 0.905F);
        int trackRight = barX + barWidth - spinnerSize - spinnerGap;
        int trackWidth = Math.max(80, trackRight - barX);

        float target = phaseProgress(session, phase, rawGenerationProgress, now);
        float displayed = session.progressAnimation.update(target, now);
        int filled = Mth.clamp(Math.round(trackWidth * displayed), 0, trackWidth);
        int percent = Mth.clamp(Math.round(displayed * 100.0F), 0, 100);

        drawProgressTrack(graphics, barX, barY, trackRight, filled);
        drawProgressText(graphics, minecraft.font, barX, barY, trackRight,
                phase.label, percent);

        int spinnerCenterX = trackRight + spinnerGap + spinnerSize / 2;
        drawSpinner(graphics, spinnerCenterX, barY, spinnerSize, now);
    }

    private static float phaseProgress(LoadingSession session,
            LoadingPhase phase, int rawGenerationProgress, long now) {
        float target;
        if (phase == LoadingPhase.GENERATING && rawGenerationProgress >= 0) {
            float generation = Mth.clamp(rawGenerationProgress / 100.0F,
                    0.0F, 1.0F);
            target = 0.14F + generation * 0.74F;
        } else if (phase == LoadingPhase.FINALIZING) {
            target = 0.90F;
        } else {
            float elapsed = Math.max(0.0F,
                    (now - session.phaseStartedAt) / 1000.0F);
            float drift = 1.0F - (float) Math.exp(-1.35F * elapsed);
            target = Mth.lerp(drift, phase.start, phase.end);
        }
        session.highestTarget = Math.max(session.highestTarget, target);
        return session.highestTarget;
    }

    private static LoadingPhase phase(Screen screen) {
        if (screen instanceof LevelLoadingScreen) return LoadingPhase.GENERATING;

        // Minecraft#setLevel uses a ProgressScreen with "connect.joining" stored
        // in its internal progress header rather than Screen#getTitle(), then
        // forces a render tick. During singleplayer startup this bridge can also
        // appear before spawn generation, so the session resolves its semantic
        // phase after seeing the rest of the load state.
        if (screen instanceof ProgressScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (activeSession != null
                    || (minecraft.level == null
                    && minecraft.hasSingleplayerServer())) {
                return LoadingPhase.JOINING;
            }
        }
        if (screen instanceof ReceivingLevelScreen) {
            return LoadingPhase.LOADING_TERRAIN;
        }

        String key = translationKey(screen.getTitle());
        if (READING_KEYS.contains(key)) return LoadingPhase.READING;
        if (PREPARING_KEYS.contains(key)) return LoadingPhase.PREPARING;
        if (JOINING_KEYS.contains(key)) return LoadingPhase.JOINING;
        if (TERRAIN_KEYS.contains(key)) return LoadingPhase.LOADING_TERRAIN;

        String text = screen.getTitle().getString().trim().toLowerCase(Locale.ROOT);
        if (text.contains("reading world data")) return LoadingPhase.READING;
        if (text.contains("preparing world") || text.contains("generating world")) {
            return LoadingPhase.PREPARING;
        }
        if (text.contains("joining world") || text.contains("connecting")) {
            return LoadingPhase.JOINING;
        }
        if (text.contains("loading terrain")) return LoadingPhase.LOADING_TERRAIN;
        return null;
    }

    private static String translationKey(Component component) {
        if (component != null
                && component.getContents() instanceof TranslatableContents t) {
            return t.getKey();
        }
        return "";
    }

    private static void drawCard(GuiGraphics graphics, Font font,
            LoadingSession session, int width, int height, long now) {
        LoadingScreenRegistry.Definition card = session.card;
        if (card == null) return;

        long age = Math.max(0L, now - session.cardStartedAt);
        float fadeIn = smootherStep(Math.min(1.0F,
                age / (float) CARD_FADE_IN_MS));
        float fadeOut = 1.0F;
        long fadeOutStart = CARD_LIFETIME_MS - CARD_FADE_OUT_MS;
        if (age > fadeOutStart) {
            fadeOut = 1.0F - smootherStep(Math.min(1.0F,
                    (age - fadeOutStart) / (float) CARD_FADE_OUT_MS));
        }
        float alpha = Mth.clamp(fadeIn * fadeOut, 0.0F, 1.0F);
        float zoomProgress = Math.min(1.0F,
                age / (float) CARD_LIFETIME_MS);
        float zoom = Mth.lerp(zoomProgress, 1.0F, CARD_MAX_ZOOM);

        drawZoomedOverlay(graphics, card.texture(), width, height, zoom, alpha);
        drawCardText(graphics, font, card, width, height, alpha);
    }

    private static void drawZoomedOverlay(GuiGraphics graphics,
            ResourceLocation texture, int width, int height, float zoom,
            float alpha) {
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
        graphics.pose().scale(zoom, zoom, 1.0F);
        graphics.pose().translate(-width / 2.0F, -height / 2.0F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        drawCoverTexture(graphics, texture, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static void drawCardText(GuiGraphics graphics, Font font,
            LoadingScreenRegistry.Definition card, int width, int height,
            float alpha) {
        float referenceScale = height / 540.0F;
        int white = withAlpha(TEXT, alpha);
        int gold = withAlpha(CARD_GOLD, alpha);

        float leftX = width * 0.037F;
        float titleY = height * 0.047F;
        float subtitleY = height * 0.131F;
        float titleScale = 4.10F * referenceScale;
        float subtitleScale = 2.25F * referenceScale;

        Component leftTitle = ScpFonts.montserrat(
                card.leftTitle().toUpperCase(Locale.ROOT));
        Component leftSubtitle = ScpFonts.montserrat(
                card.leftSubtitle().toUpperCase(Locale.ROOT));
        drawScaledText(graphics, font, leftTitle, leftX, titleY,
                titleScale, white);
        drawScaledText(graphics, font, leftSubtitle, leftX, subtitleY,
                subtitleScale, white);

        float rightEdge = width * 0.963F;
        float rightLabelY = height * 0.047F;
        float rightValueY = height * 0.091F;
        float rightLabelScale = 1.58F * referenceScale;
        float rightValueScale = 2.38F * referenceScale;
        Component rightLabel = ScpFonts.titillium(
                card.rightLabel().toUpperCase(Locale.ROOT));
        Component rightValue = ScpFonts.titillium(
                card.rightValue().toUpperCase(Locale.ROOT));
        drawScaledTextRight(graphics, font, rightLabel, rightEdge,
                rightLabelY, rightLabelScale, white);
        drawScaledTextRight(graphics, font, rightValue, rightEdge,
                rightValueY, rightValueScale, gold);

        Component description = ScpFonts.titillium(card.description());
        float descriptionScale = 1.70F * referenceScale;
        if (card.descriptionAnchor()
                == LoadingScreenRegistry.DescriptionAnchor.LEFT) {
            float x = width * 0.060F;
            float y = height * 0.295F;
            float boxWidth = width * 0.255F;
            drawWrappedText(graphics, font, description, x, y, boxWidth,
                    descriptionScale, white, false);
        } else {
            float x = width * 0.130F;
            float y = height * 0.725F;
            float boxWidth = width * 0.740F;
            drawWrappedText(graphics, font, description, x, y, boxWidth,
                    descriptionScale, white, true);
        }
    }

    private static void drawScaledText(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawScaledTextRight(GuiGraphics graphics, Font font,
            Component text, float right, float y, float scale, int color) {
        float width = font.width(text) * scale;
        drawScaledText(graphics, font, text, right - width, y, scale, color);
    }

    private static void drawWrappedText(GuiGraphics graphics, Font font,
            Component text, float x, float y, float width, float scale,
            int color, boolean centered) {
        int unscaledWidth = Math.max(20, Math.round(width / scale));
        List<FormattedCharSequence> lines = font.split(text, unscaledWidth);
        float lineAdvance = (font.lineHeight + 3) * scale;
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            float drawX = x;
            if (centered) {
                float lineWidth = font.width(line) * scale;
                drawX += (width - lineWidth) / 2.0F;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(drawX, y + index * lineAdvance, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, line, 0, 0, color, false);
            graphics.pose().popPose();
        }
    }

    private static int withAlpha(int color, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static void drawCoverTexture(GuiGraphics graphics,
            ResourceLocation texture, int width, int height) {
        if (width <= 0 || height <= 0) return;

        double destinationAspect = (double) width / (double) height;
        double sourceAspect = (double) BACKGROUND_WIDTH / BACKGROUND_HEIGHT;
        float sourceX = 0.0F;
        float sourceY = 0.0F;
        int sourceWidth = BACKGROUND_WIDTH;
        int sourceHeight = BACKGROUND_HEIGHT;

        if (destinationAspect > sourceAspect) {
            sourceHeight = Math.max(1,
                    (int) Math.round(BACKGROUND_WIDTH / destinationAspect));
            sourceY = (BACKGROUND_HEIGHT - sourceHeight) / 2.0F;
        } else if (destinationAspect < sourceAspect) {
            sourceWidth = Math.max(1,
                    (int) Math.round(BACKGROUND_HEIGHT * destinationAspect));
            sourceX = (BACKGROUND_WIDTH - sourceWidth) / 2.0F;
        }

        graphics.blit(texture, 0, 0, width, height, sourceX, sourceY,
                sourceWidth, sourceHeight, BACKGROUND_WIDTH,
                BACKGROUND_HEIGHT);
    }

    private static void drawProgressTrack(GuiGraphics graphics, int left,
            int y, int right, int filled) {
        int width = Math.max(1, right - left);

        graphics.fill(left, y, right, y + 1, TRACK);
        graphics.fill(left, y + 4, right, y + 5, TRACK_FAINT);

        for (int index = 0; index <= 4; index++) {
            int tickX = left + Math.round(width * (index / 4.0F));
            int tickHeight = index == 0 || index == 4 ? 7 : 4;
            graphics.fill(tickX, y - tickHeight / 2, tickX + 1,
                    y + (tickHeight + 1) / 2, TRACK);
        }

        if (filled > 0) {
            int progressRight = Math.min(right, left + filled);
            graphics.fill(left, y - 1, progressRight, y + 2, ACCENT);
            int headLeft = Math.max(left, progressRight - 1);
            graphics.fill(headLeft, y - 3, progressRight + 1, y + 4,
                    ACCENT_BRIGHT);
        }

        graphics.fill(left - 4, y - 1, left - 1, y + 2, ACCENT);
        graphics.fill(right + 1, y - 1, right + 4, y + 2, TRACK);
    }

    private static void drawProgressText(GuiGraphics graphics, Font font,
            int left, int y, int right, String label, int percent) {
        Component stage = ScpFonts.roboto(label);
        Component percentText = ScpFonts.roboto(percent + "%");
        int textY = y - font.lineHeight - 7;
        graphics.drawString(font, stage, left, textY, TEXT_MUTED, false);
        graphics.drawString(font, percentText,
                right - font.width(percentText), textY, TEXT, false);
    }

    private static void drawSpinner(GuiGraphics graphics, int centerX,
            int centerY, int size, long now) {
        long cycleTime = Math.floorMod(now, SPINNER_CYCLE_MS);
        float rotationProgress = cycleTime >= SPINNER_ROTATE_MS
                ? 1.0F : cycleTime / (float) SPINNER_ROTATE_MS;
        float eased = smootherStep(rotationProgress);
        float outerAngle = 360.0F * eased;
        float innerAngle = -360.0F * eased;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawRotatedTexture(graphics, LOGO_OUTER, centerX, centerY, size,
                outerAngle);
        drawRotatedTexture(graphics, LOGO_INNER, centerX, centerY, size,
                innerAngle);
        RenderSystem.disableBlend();
    }

    private static void drawRotatedTexture(GuiGraphics graphics,
            ResourceLocation texture, int centerX, int centerY, int size,
            float angleDegrees) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDegrees));
        int half = size / 2;
        graphics.blit(texture, -half, -half, size, size, 0.0F, 0.0F,
                LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE, LOGO_TEXTURE_SIZE,
                LOGO_TEXTURE_SIZE);
        graphics.pose().popPose();
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static StoringChunkProgressListener progressListener(
            LevelLoadingScreen screen) {
        Field field = progressListenerField();
        if (field == null) return null;
        try {
            Object value = field.get(screen);
            return value instanceof StoringChunkProgressListener listener
                    ? listener : null;
        } catch (IllegalAccessException exception) {
            if (!reflectionFailureLogged) {
                reflectionFailureLogged = true;
                ScpAdditionsMod.LOGGER.warn(
                        "Could not read vanilla world-generation progress; using vanilla loading presentation",
                        exception);
            }
            return null;
        }
    }

    private static Field progressListenerField() {
        if (progressFieldLookupAttempted) return progressListenerField;
        synchronized (CustomWorldLoadingScreenClient.class) {
            if (progressFieldLookupAttempted) return progressListenerField;
            progressFieldLookupAttempted = true;
            for (Field field : LevelLoadingScreen.class.getDeclaredFields()) {
                if (StoringChunkProgressListener.class.isAssignableFrom(
                        field.getType())) {
                    field.setAccessible(true);
                    progressListenerField = field;
                    break;
                }
            }
            if (progressListenerField == null && !reflectionFailureLogged) {
                reflectionFailureLogged = true;
                ScpAdditionsMod.LOGGER.warn(
                        "Vanilla LevelLoadingScreen progress field was not found; using vanilla loading presentation");
            }
            return progressListenerField;
        }
    }

    private enum LoadingPhase {
        READING("READING WORLD DATA", 0.02F, 0.10F),
        PREPARING("PREPARING WORLD", 0.10F, 0.14F),
        GENERATING("GENERATING SPAWN REGION", 0.14F, 0.88F),
        FINALIZING("FINALIZING WORLD", 0.88F, 0.90F),
        JOINING("JOINING WORLD", 0.90F, 0.95F),
        LOADING_TERRAIN("LOADING TERRAIN", 0.95F, 0.995F);

        private final String label;
        private final float start;
        private final float end;

        LoadingPhase(String label, float start, float end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }

    private static final class LoadingSession {
        private final List<LoadingScreenRegistry.Definition> definitions;
        private final ProgressAnimation progressAnimation = new ProgressAnimation();
        private LoadingScreenRegistry.Definition card;
        private LoadingPhase phase;
        private long phaseStartedAt;
        private long cardStartedAt;
        private long lastSeenAt;
        private float highestTarget;
        private boolean generationSeen;

        private LoadingSession(long now,
                List<LoadingScreenRegistry.Definition> definitions) {
            this.definitions = definitions;
            this.card = LoadingScreenRegistry.choose(definitions, null);
            this.cardStartedAt = now;
            this.lastSeenAt = now;
        }

        /**
         * Vanilla singleplayer calls Minecraft#setLevel and forces a temporary
         * "Joining world" ProgressScreen before it shows LevelLoadingScreen.
         * Presenting that literal internal ordering made the UI jump from 90%
         * JOINING back to GENERATING. Treat the early bridge as PREPARING, then
         * keep subsequent phases monotonic for the remainder of the session.
         */
        private LoadingPhase enterPhase(LoadingPhase candidate,
                boolean singleplayer, long now) {
            LoadingPhase resolved = candidate;
            if (singleplayer && candidate == LoadingPhase.JOINING
                    && !generationSeen) {
                resolved = LoadingPhase.PREPARING;
            }

            if (candidate == LoadingPhase.GENERATING
                    || candidate == LoadingPhase.FINALIZING) {
                generationSeen = true;
            }

            if (phase != null && resolved.ordinal() < phase.ordinal()) {
                return phase;
            }

            if (phase != resolved) {
                phase = resolved;
                phaseStartedAt = now;
            }
            return phase;
        }

        private void rotateCardIfNeeded(long now) {
            if (now - cardStartedAt < CARD_LIFETIME_MS) return;
            ResourceLocation previous = card == null ? null : card.id();
            card = LoadingScreenRegistry.choose(definitions, previous);
            cardStartedAt = now;
        }
    }

    private static final class ProgressAnimation {
        private float displayed = -1.0F;
        private long lastUpdate;

        private float update(float target, long now) {
            target = Mth.clamp(target, 0.0F, 1.0F);
            if (lastUpdate == 0L) {
                lastUpdate = now;
                displayed = target;
                return displayed;
            }

            float deltaSeconds = Math.min(0.1F,
                    Math.max(0.0F, (now - lastUpdate) / 1000.0F));
            lastUpdate = now;
            if (target < displayed) {
                displayed = target;
            } else {
                float blend = 1.0F
                        - (float) Math.exp(-8.0F * deltaSeconds);
                displayed += (target - displayed) * blend;
            }
            if (target >= 1.0F && displayed >= 0.99F) displayed = 1.0F;
            return Mth.clamp(displayed, 0.0F, 1.0F);
        }
    }
}
