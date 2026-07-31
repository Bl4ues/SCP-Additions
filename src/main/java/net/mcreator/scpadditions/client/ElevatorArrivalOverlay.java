package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.elevator.CoreRoomElevatorModule;
import net.mcreator.scpadditions.facility.elevator.ElevatorArrivalDisplayData;

/** Animated SCP: Unity-style sector and floor announcement. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ElevatorArrivalOverlay {
    private static final double LINE_IN_START = 0.00D;
    private static final double LINE_IN_END = 0.32D;
    private static final double SECTOR_IN_START = 0.22D;
    private static final double SECTOR_IN_END = 0.52D;
    private static final double FLOOR_IN_START = 0.34D;
    private static final double FLOOR_IN_END = 0.68D;
    private static final double SECTOR_OUT_START = 5.55D;
    private static final double SECTOR_OUT_END = 5.82D;
    private static final double FLOOR_OUT_START = 5.68D;
    private static final double FLOOR_OUT_END = 5.96D;
    private static final double LINE_OUT_START = 5.96D;
    private static final double LINE_OUT_END = 6.30D;
    private static final double CROSSHAIR_HIDE_LEAD_SECONDS = 0.50D;
    private static final double CROSSHAIR_FADE_OUT_SECONDS = 0.30D;
    private static final double CROSSHAIR_FADE_IN_SECONDS = 0.30D;

    private static final int LINE_WHITE = 0xFFF7F8FC;
    private static final int TEXT_WHITE = 0xF7F8FC;
    private static final int FLOOR_TYPE_GRAY = 0xA9AFBA;
    private static final float SECTOR_SCALE = 2.10F;
    private static final float FLOOR_SCALE = 2.76F;
    private static final int LINE_TEXT_PADDING = 32;
    private static final int TEXT_LINE_GAP = 4;
    private static final int SECTOR_VERTICAL_BIAS = 0;
    private static final int FLOOR_VERTICAL_BIAS = 10;
    private static final ResourceLocation CROSSHAIR_TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/icons.png");

    private static ElevatorArrivalDisplayData current =
            ElevatorArrivalDisplayData.NONE;
    private static long cueStartedAtNanos;
    private static long scheduledStartNanos;
    private static long startedAtNanos;
    private static long crosshairRestoreStartedAtNanos;
    private static boolean pending;
    private static boolean active;
    private static boolean restoringCrosshair;

    private ElevatorArrivalOverlay() {
    }

    public static void prepare(ElevatorArrivalDisplayData data,
            int delayTicks) {
        if (data == null || !data.enabled()
                || data.sectorLabel().isBlank()) {
            return;
        }
        long now = System.nanoTime();
        current = data;
        cueStartedAtNanos = now;
        scheduledStartNanos = now
                + Math.max(0, delayTicks) * 50_000_000L;
        startedAtNanos = 0L;
        crosshairRestoreStartedAtNanos = 0L;
        pending = true;
        active = false;
        restoringCrosshair = false;

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                CoreRoomElevatorModule.ZONE_SPLASH.get(), 1.0F, 2.0F));
    }

    public static void show(ElevatorArrivalDisplayData data) {
        prepare(data, 0);
    }

    public static void hide() {
        finishSequence(System.nanoTime());
    }

    /** Current opacity used by both the vanilla and custom crosshair renderers. */
    public static float crosshairOpacity() {
        long now = System.nanoTime();
        updateTimeline(now);
        return crosshairOpacity(now);
    }

    @SubscribeEvent
    public static void renderCrosshair(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(
                VanillaGuiOverlay.CROSSHAIR.id())) {
            return;
        }
        if (InventoryModuleRuntimeState.customCrosshairEnabledForClient()) {
            return;
        }
        long now = System.nanoTime();
        updateTimeline(now);
        float opacity = crosshairOpacity(now);
        if (opacity >= 0.999F) return;

        event.setCanceled(true);
        if (opacity <= 0.001F) return;

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int x = (minecraft.getWindow().getGuiScaledWidth() - 15) / 2;
        int y = (minecraft.getWindow().getGuiScaledHeight() - 15) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        graphics.blit(CROSSHAIR_TEXTURE, x, y, 0, 0, 15, 15);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        long now = System.nanoTime();
        updateTimeline(now);
        if (!active) return;
        double time = (now - startedAtNanos) / 1_000_000_000.0D;
        if (time < 0.0D || time >= LINE_OUT_END) {
            finishSequence(now);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int centerX = width / 2;
        int lineY = Math.round(height * 0.49F);

        MutableComponent sector = Component.literal(current.sectorLabel())
                .withStyle(style -> style.withFont(ScpFonts.TITILLIUM_WEB)
                        .withColor(TEXT_WHITE));
        MutableComponent floor = Component.empty()
                .append(Component.literal(current.floorTypeLabel() + " ")
                        .withStyle(style -> style
                                .withFont(ScpFonts.TITILLIUM_WEB)
                                .withColor(FLOOR_TYPE_GRAY)))
                .append(Component.literal(current.floorNumberLabel())
                        .withStyle(style -> style
                                .withFont(ScpFonts.TITILLIUM_WEB)
                                .withColor(TEXT_WHITE)));

        int lineLimit = Math.max(180,
                Math.min(width - 48, Math.round(width * 0.62F)));
        int textLimit = Math.max(120, lineLimit - LINE_TEXT_PADDING);
        float sectorScale = fittedScale(minecraft.font.width(sector),
                SECTOR_SCALE, textLimit);
        float floorScale = fittedScale(minecraft.font.width(floor),
                FLOOR_SCALE, textLimit);
        int sectorWidth = Math.round(
                minecraft.font.width(sector) * sectorScale);
        int floorWidth = Math.round(
                minecraft.font.width(floor) * floorScale);
        int maximumLineWidth = Mth.clamp(
                Math.max(sectorWidth, floorWidth) + LINE_TEXT_PADDING,
                180, lineLimit);

        double lineProgress = time < LINE_OUT_START
                ? easedSegment(time, LINE_IN_START, LINE_IN_END)
                : 1.0D - easedSegment(time,
                        LINE_OUT_START, LINE_OUT_END);
        double sectorProgress = time < SECTOR_OUT_START
                ? easedSegment(time, SECTOR_IN_START, SECTOR_IN_END)
                : 1.0D - easedSegment(time,
                        SECTOR_OUT_START, SECTOR_OUT_END);
        double floorProgress = time < FLOOR_OUT_START
                ? easedSegment(time, FLOOR_IN_START, FLOOR_IN_END)
                : 1.0D - easedSegment(time,
                        FLOOR_OUT_START, FLOOR_OUT_END);

        int lineWidth = Math.max(0,
                (int) Math.round(maximumLineWidth * lineProgress));
        int sectorHeight = Math.max(1,
                Math.round(minecraft.font.lineHeight * sectorScale));
        int floorHeight = Math.max(1,
                Math.round(minecraft.font.lineHeight * floorScale));

        int sectorHiddenY = lineY + 2;
        int sectorShownY = lineY - sectorHeight - TEXT_LINE_GAP
                + SECTOR_VERTICAL_BIAS;
        int floorHiddenY = lineY - floorHeight - 2;
        int floorShownY = lineY + 2 + TEXT_LINE_GAP
                + FLOOR_VERTICAL_BIAS;
        int sectorY = Math.round(Mth.lerp((float) sectorProgress,
                sectorHiddenY, sectorShownY));
        int floorY = Math.round(Mth.lerp((float) floorProgress,
                floorHiddenY, floorShownY));

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        int clipLeft = Math.max(0,
                centerX - maximumLineWidth / 2 - 12);
        int clipRight = Math.min(width,
                centerX + maximumLineWidth / 2 + 12);

        graphics.enableScissor(clipLeft, Math.max(0, lineY - 64),
                clipRight, Math.max(0, lineY));
        drawCenteredScaled(graphics, minecraft, sector,
                centerX, sectorY, sectorScale, LINE_WHITE);
        graphics.disableScissor();

        graphics.enableScissor(clipLeft, Math.min(height, lineY + 2),
                clipRight, Math.min(height, lineY + 68));
        drawCenteredScaled(graphics, minecraft, floor,
                centerX, floorY, floorScale, LINE_WHITE);
        graphics.disableScissor();

        if (lineWidth > 0) {
            graphics.fill(centerX - lineWidth / 2, lineY,
                    centerX + (lineWidth + 1) / 2,
                    lineY + 2, LINE_WHITE);
        }
        graphics.pose().popPose();
    }

    private static void updateTimeline(long now) {
        if (pending && now >= scheduledStartNanos) {
            pending = false;
            active = true;
            startedAtNanos = scheduledStartNanos;
        }
    }

    private static void finishSequence(long now) {
        if (!pending && !active) return;
        pending = false;
        active = false;
        current = ElevatorArrivalDisplayData.NONE;
        cueStartedAtNanos = 0L;
        scheduledStartNanos = 0L;
        startedAtNanos = 0L;
        crosshairRestoreStartedAtNanos = now;
        restoringCrosshair = true;
    }

    private static float crosshairOpacity(long now) {
        if (pending) {
            double secondsUntilStart = (scheduledStartNanos - now)
                    / 1_000_000_000.0D;
            if (secondsUntilStart >= CROSSHAIR_HIDE_LEAD_SECONDS) {
                return 1.0F;
            }
            double fadeElapsed = CROSSHAIR_HIDE_LEAD_SECONDS
                    - secondsUntilStart;
            return (float) (1.0D - smoothProgress(fadeElapsed,
                    CROSSHAIR_FADE_OUT_SECONDS));
        }
        if (active) return 0.0F;
        if (restoringCrosshair) {
            double elapsed = (now - crosshairRestoreStartedAtNanos)
                    / 1_000_000_000.0D;
            float opacity = (float) smoothProgress(elapsed,
                    CROSSHAIR_FADE_IN_SECONDS);
            if (opacity >= 0.999F) {
                restoringCrosshair = false;
                return 1.0F;
            }
            return opacity;
        }
        return 1.0F;
    }

    private static double smoothProgress(double elapsed, double duration) {
        if (duration <= 0.0D) return 1.0D;
        double value = Mth.clamp(elapsed / duration, 0.0D, 1.0D);
        return value * value * (3.0D - 2.0D * value);
    }

    private static float fittedScale(int rawWidth, float preferred,
            int maximumWidth) {
        if (rawWidth <= 0) return preferred;
        return Math.min(preferred, maximumWidth / (float) rawWidth);
    }

    private static void drawCenteredScaled(GuiGraphics graphics,
            Minecraft minecraft, Component text, int centerX, int y,
            float scale, int color) {
        int textWidth = minecraft.font.width(text);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, text,
                -textWidth / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private static double easedSegment(double time, double start,
            double end) {
        if (end <= start) return time >= end ? 1.0D : 0.0D;
        double value = Mth.clamp((time - start) / (end - start),
                0.0D, 1.0D);
        return value * value * (3.0D - 2.0D * value);
    }
}
