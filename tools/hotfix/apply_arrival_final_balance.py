from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(relative: str, old: str, new: str) -> None:
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(
            f"Expected one match in {relative}, found {count}: {old[:120]!r}"
        )
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


overlay = ROOT / "src/main/java/net/mcreator/scpadditions/client/ElevatorArrivalOverlay.java"
overlay.write_text(r'''package net.mcreator.scpadditions.client;

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
    private static final double CROSSHAIR_FADE_OUT_SECONDS = 0.30D;
    private static final double CROSSHAIR_FADE_IN_SECONDS = 0.30D;

    private static final int LINE_WHITE = 0xFFF7F8FC;
    private static final int TEXT_WHITE = 0xF7F8FC;
    private static final int FLOOR_TYPE_GRAY = 0xA9AFBA;
    private static final float SECTOR_SCALE = 2.10F;
    private static final float FLOOR_SCALE = 2.65F;
    private static final int LINE_TEXT_PADDING = 56;
    private static final int TEXT_LINE_GAP = 4;
    private static final int SECTOR_VERTICAL_BIAS = 3;
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
                CoreRoomElevatorModule.ZONE_SPLASH.get(), 1.0F));
    }

    public static void show(ElevatorArrivalDisplayData data) {
        prepare(data, 0);
    }

    public static void hide() {
        finishSequence(System.nanoTime());
    }

    @SubscribeEvent
    public static void renderCrosshair(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(
                VanillaGuiOverlay.CROSSHAIR.id())) {
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
        int floorShownY = lineY + 2 + TEXT_LINE_GAP;
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
            double elapsed = (now - cueStartedAtNanos)
                    / 1_000_000_000.0D;
            return (float) (1.0D - smoothProgress(elapsed,
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
''', encoding="utf-8")

packet = "src/main/java/net/mcreator/scpadditions/network/ElevatorArrivalDisplayPacket.java"
replace_once(
    packet,
    "    private final ElevatorArrivalDisplayData data;\n\n"
    "    public ElevatorArrivalDisplayPacket(ElevatorArrivalDisplayData data) {\n"
    "        this.data = data == null ? ElevatorArrivalDisplayData.NONE : data;\n"
    "    }\n",
    "    private final ElevatorArrivalDisplayData data;\n"
    "    private final int delayTicks;\n\n"
    "    public ElevatorArrivalDisplayPacket(ElevatorArrivalDisplayData data,\n"
    "            int delayTicks) {\n"
    "        this.data = data == null ? ElevatorArrivalDisplayData.NONE : data;\n"
    "        this.delayTicks = Math.max(0, Math.min(200, delayTicks));\n"
    "    }\n",
)
replace_once(
    packet,
    "        ElevatorArrivalDisplayData.write(buffer, message.data);\n",
    "        ElevatorArrivalDisplayData.write(buffer, message.data);\n"
    "        buffer.writeVarInt(message.delayTicks);\n",
)
replace_once(
    packet,
    "        return new ElevatorArrivalDisplayPacket(\n"
    "                ElevatorArrivalDisplayData.read(buffer));\n",
    "        return new ElevatorArrivalDisplayPacket(\n"
    "                ElevatorArrivalDisplayData.read(buffer),\n"
    "                buffer.readVarInt());\n",
)
replace_once(
    packet,
    "                () -> () -> ElevatorArrivalOverlay.show(message.data)));\n",
    "                () -> () -> ElevatorArrivalOverlay.prepare(message.data,\n"
    "                        message.delayTicks)));\n",
)

network = "src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java"
replace_once(
    network,
    "    public static void showElevatorArrival(ServerPlayer player,\n"
    "            net.mcreator.scpadditions.facility.elevator.\n"
    "                    ElevatorArrivalDisplayData data) {\n"
    "        if (player == null || data == null || !data.enabled()) return;\n"
    "        ScpAdditionsMod.PACKET_HANDLER.send(\n"
    "                PacketDistributor.PLAYER.with(() -> player),\n"
    "                new ElevatorArrivalDisplayPacket(data));\n"
    "    }\n",
    "    public static void showElevatorArrival(ServerPlayer player,\n"
    "            net.mcreator.scpadditions.facility.elevator.\n"
    "                    ElevatorArrivalDisplayData data, int delayTicks) {\n"
    "        if (player == null || data == null || !data.enabled()) return;\n"
    "        ScpAdditionsMod.PACKET_HANDLER.send(\n"
    "                PacketDistributor.PLAYER.with(() -> player),\n"
    "                new ElevatorArrivalDisplayPacket(data, delayTicks));\n"
    "    }\n",
)

carriage = "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java"
replace_once(
    carriage,
    "    private static final int ARRIVAL_SOUND_LEAD_TICKS = 20;\n",
    "    private static final int ARRIVAL_SOUND_LEAD_TICKS = 30;\n",
)
replace_once(
    carriage,
    "                    triggerArrivalDisplay(serverLevel);\n"
    "                    playElevatorSound(\n",
    "                    playElevatorSound(\n",
)
replace_once(
    carriage,
    "            player.playNotifySound(CoreRoomElevatorModule.ZONE_SPLASH.get(),\n"
    "                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);\n",
    "            ScpEntityNetwork.showElevatorArrival(player, data,\n"
    "                    ARRIVAL_SOUND_LEAD_TICKS);\n",
)
start = "    private void triggerArrivalDisplay(ServerLevel serverLevel) {\n"
end = "    private void playElevatorSound(net.minecraft.sounds.SoundEvent sound,\n"
path = ROOT / carriage
text = path.read_text(encoding="utf-8")
start_index = text.find(start)
end_index = text.find(end, start_index)
if start_index < 0 or end_index < 0:
    raise RuntimeError("Could not locate obsolete triggerArrivalDisplay method")
path.write_text(text[:start_index] + text[end_index:], encoding="utf-8")
