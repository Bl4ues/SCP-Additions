package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * SCP Additions presentation for vanilla spawn-region loading. The underlying
 * LevelLoadingScreen remains in control of loading and progress; only its
 * render pass is replaced, so failures can always fall back to vanilla.
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

    private static final long SPINNER_CYCLE_MS = 3200L;
    private static final long SPINNER_ROTATE_MS = 2520L;

    private static final Map<LevelLoadingScreen, ProgressAnimation> ANIMATIONS =
            new WeakHashMap<>();

    private static volatile Field progressListenerField;
    private static volatile boolean progressFieldLookupAttempted;
    private static volatile boolean reflectionFailureLogged;
    private static volatile boolean renderFailureLogged;

    private CustomWorldLoadingScreenClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!ClientModulePreferences.customLoadingScreenEnabled()
                || !(event.getScreen() instanceof LevelLoadingScreen screen)) {
            return;
        }

        StoringChunkProgressListener listener = progressListener(screen);
        if (listener == null) return;

        try {
            render(event.getGuiGraphics(), screen, listener.getProgress());
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

    private static void render(GuiGraphics graphics, LevelLoadingScreen screen,
            int rawProgress) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int progress = Mth.clamp(rawProgress, 0, 100);
        long now = Util.getMillis();

        drawCoverBackground(graphics, width, height);

        int barWidth = Math.min(width - 48,
                Math.max(260, Math.round(width * 0.52F)));
        int spinnerSize = Mth.clamp(height / 10, 30, 46);
        int spinnerGap = Math.max(12, spinnerSize / 3);
        int barX = (width - barWidth) / 2;
        int barY = Math.round(height * 0.84F);
        int trackRight = barX + barWidth - spinnerSize - spinnerGap;
        int trackWidth = Math.max(80, trackRight - barX);

        ProgressAnimation animation = ANIMATIONS.computeIfAbsent(screen,
                ignored -> new ProgressAnimation());
        float displayed = animation.update(progress / 100.0F, now);
        int filled = Mth.clamp(Math.round(trackWidth * displayed), 0,
                trackWidth);

        drawProgressTrack(graphics, barX, barY, trackRight, filled);
        drawProgressText(graphics, minecraft.font, barX, barY, trackRight,
                progress);

        int spinnerCenterX = trackRight + spinnerGap + spinnerSize / 2;
        drawSpinner(graphics, spinnerCenterX, barY, spinnerSize, now);
    }

    private static void drawCoverBackground(GuiGraphics graphics, int width,
            int height) {
        if (width <= 0 || height <= 0) return;

        double destinationAspect = (double) width / (double) height;
        double sourceAspect = (double) BACKGROUND_WIDTH
                / (double) BACKGROUND_HEIGHT;
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

        graphics.blit(BACKGROUND, 0, 0, width, height, sourceX, sourceY,
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
            int left, int y, int right, int progress) {
        Component stage = ScpFonts.roboto(stageText(progress));
        Component percent = ScpFonts.roboto(progress + "%");
        int textY = y - font.lineHeight - 7;
        graphics.drawString(font, stage, left, textY, TEXT_MUTED, false);
        graphics.drawString(font, percent, right - font.width(percent), textY,
                TEXT, false);
    }

    private static String stageText(int progress) {
        if (progress <= 0) return "PREPARING SPAWN REGION";
        if (progress >= 100) return "FINALIZING WORLD";
        return "GENERATING SPAWN REGION";
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

    private static final class ProgressAnimation {
        private float displayed;
        private long lastUpdate;

        private float update(float target, long now) {
            target = Mth.clamp(target, 0.0F, 1.0F);
            if (lastUpdate == 0L) {
                lastUpdate = now;
                displayed = Math.max(0.0F, target - 0.04F);
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
