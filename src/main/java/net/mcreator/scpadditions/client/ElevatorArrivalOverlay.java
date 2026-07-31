package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
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

    private static final int LINE_WHITE = 0xFFF7F8FC;
    private static final int TEXT_WHITE = 0xF7F8FC;
    private static final int FLOOR_TYPE_GRAY = 0xA9AFBA;
    private static final float TEXT_SCALE = 1.65F;

    private static ElevatorArrivalDisplayData current =
            ElevatorArrivalDisplayData.NONE;
    private static long startedAtNanos;
    private static boolean active;

    private ElevatorArrivalOverlay() {
    }

    public static void show(ElevatorArrivalDisplayData data) {
        if (data == null || !data.enabled()
                || data.sectorLabel().isBlank()) {
            return;
        }
        current = data;
        startedAtNanos = System.nanoTime();
        active = true;
    }

    public static void hide() {
        active = false;
        current = ElevatorArrivalDisplayData.NONE;
        startedAtNanos = 0L;
    }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        if (!active) return;
        double time = (System.nanoTime() - startedAtNanos) / 1_000_000_000.0D;
        if (time < 0.0D || time >= LINE_OUT_END) {
            hide();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int centerX = width / 2;
        int lineY = Math.round(height * 0.49F);
        int maximumLineWidth = Mth.clamp(
                Math.round(width * 0.46F), 180, 760);

        double lineProgress = time < LINE_OUT_START
                ? easedSegment(time, LINE_IN_START, LINE_IN_END)
                : 1.0D - easedSegment(time, LINE_OUT_START, LINE_OUT_END);
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
        int sectorHiddenY = lineY - 4;
        int sectorShownY = lineY - 22;
        int floorHiddenY = lineY - 7;
        int floorShownY = lineY + 8;
        int sectorY = Math.round(Mth.lerp((float) sectorProgress,
                sectorHiddenY, sectorShownY));
        int floorY = Math.round(Mth.lerp((float) floorProgress,
                floorHiddenY, floorShownY));

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

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1000.0F);
        int clipLeft = Math.max(0, centerX - maximumLineWidth / 2 - 12);
        int clipRight = Math.min(width,
                centerX + maximumLineWidth / 2 + 12);

        graphics.enableScissor(clipLeft, Math.max(0, lineY - 44),
                clipRight, Math.max(0, lineY - 1));
        drawCenteredScaled(graphics, minecraft, sector,
                centerX, sectorY, TEXT_SCALE, LINE_WHITE);
        graphics.disableScissor();

        graphics.enableScissor(clipLeft, Math.min(height, lineY + 2),
                clipRight, Math.min(height, lineY + 42));
        drawCenteredScaled(graphics, minecraft, floor,
                centerX, floorY, TEXT_SCALE, LINE_WHITE);
        graphics.disableScissor();

        if (lineWidth > 0) {
            graphics.fill(centerX - lineWidth / 2, lineY,
                    centerX + (lineWidth + 1) / 2, lineY + 2, LINE_WHITE);
        }
        graphics.pose().popPose();
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
