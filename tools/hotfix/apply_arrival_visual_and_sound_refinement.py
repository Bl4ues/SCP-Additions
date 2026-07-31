from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[2]


def replace_once(relative: str, old: str, new: str) -> None:
    path = ROOT / relative
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one match in {relative}, found {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


# Lower text inside custom edit boxes by two additional pixels.
for relative in (
    "src/main/java/net/mcreator/scpadditions/client/gui/ElevatorArrivalEditorScreen.java",
    "src/main/java/com/bl4ues/scpinventory/client/gui/ContextAnchorEditorScreen.java",
):
    replace_once(
        relative,
        "int offset = Math.max(0, (getHeight() - 9) / 2);",
        "int offset = Math.max(0, (getHeight() - 9) / 2 + 2);",
    )

# Replace the overlay with proportions and masking matching the supplied Unity reference.
overlay = ROOT / "src/main/java/net/mcreator/scpadditions/client/ElevatorArrivalOverlay.java"
overlay.write_text(r'''package net.mcreator.scpadditions.client;

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
    private static final float SECTOR_SCALE = 2.25F;
    private static final float FLOOR_SCALE = 2.65F;
    private static final int LINE_TEXT_PADDING = 56;
    private static final int TEXT_LINE_GAP = 4;
    private static final float WEIGHT_OFFSET = 0.35F;

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
        double time = (System.nanoTime() - startedAtNanos)
                / 1_000_000_000.0D;
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
        int sectorShownY = lineY - sectorHeight - TEXT_LINE_GAP;
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
        graphics.pose().translate(WEIGHT_OFFSET, 0.0F, 0.0F);
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

# Register zone_splash as an elevator sound event.
replace_once(
    "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java",
    "    public static final RegistryObject<SoundEvent> ELEVATOR_BUTTON_ACCEPT =\n"
    "            sound(\"elevator_button_accept\");\n",
    "    public static final RegistryObject<SoundEvent> ELEVATOR_BUTTON_ACCEPT =\n"
    "            sound(\"elevator_button_accept\");\n"
    "    public static final RegistryObject<SoundEvent> ZONE_SPLASH =\n"
    "            sound(\"zone_splash\");\n",
)

# Schedule the cue exactly one second before the display/door-opening transition.
carriage = "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java"
replace_once(
    carriage,
    "    public static final int TRAVEL_TICKS = 8 * 20;\n"
    "    private static final double FLOOR_EPSILON = 0.035D;\n",
    "    public static final int TRAVEL_TICKS = 8 * 20;\n"
    "    private static final int ARRIVAL_SOUND_LEAD_TICKS = 20;\n"
    "    private static final int ARRIVAL_SOUND_MOVING_TICK =\n"
    "            TRAVEL_TICKS + LEVELING_TICKS - ARRIVAL_SOUND_LEAD_TICKS;\n"
    "    private static final double FLOOR_EPSILON = 0.035D;\n",
)
replace_once(
    carriage,
    "        double progress = soundSyncedProgress(normalized);\n"
    "        setPos(getX(), Mth.lerp(progress, motionStartY, motionEndY), getZ());\n",
    "        double progress = soundSyncedProgress(normalized);\n"
    "        if (phaseTicks == ARRIVAL_SOUND_MOVING_TICK\n"
    "                && level() instanceof ServerLevel serverLevel) {\n"
    "            triggerArrivalCue(serverLevel);\n"
    "        }\n"
    "        setPos(getX(), Mth.lerp(progress, motionStartY, motionEndY), getZ());\n",
)
replace_once(
    carriage,
    "    private void triggerArrivalDisplay(ServerLevel serverLevel) {\n",
    "    private void triggerArrivalCue(ServerLevel serverLevel) {\n"
    "        int floor = targetFloorIndex();\n"
    "        if (floor < 0 || floor >= floorHeights.length) return;\n"
    "        BlockPos stationPos = new BlockPos(columnX(),\n"
    "                floorHeights[floor], columnZ());\n"
    "        if (!(serverLevel.getBlockEntity(stationPos)\n"
    "                instanceof CoreRoomElevatorModule.StationBlockEntity station)) {\n"
    "            return;\n"
    "        }\n"
    "        ElevatorArrivalDisplayData data = station.arrivalDisplay();\n"
    "        if (!data.enabled() || data.sectorLabel().isBlank()) return;\n"
    "        AABB passengers = cabinInteriorBox().inflate(0.18D, 0.12D,\n"
    "                0.18D);\n"
    "        for (ServerPlayer player : serverLevel.players()) {\n"
    "            if (!player.isAlive() || player.isSpectator()\n"
    "                    || !passengers.intersects(player.getBoundingBox())) {\n"
    "                continue;\n"
    "            }\n"
    "            player.playNotifySound(CoreRoomElevatorModule.ZONE_SPLASH.get(),\n"
    "                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);\n"
    "        }\n"
    "    }\n\n"
    "    private void triggerArrivalDisplay(ServerLevel serverLevel) {\n",
)

# Add the sound definition while preserving the compact JSON style.
sounds_path = ROOT / "src/main/resources/assets/scp_additions/sounds.json"
sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
sounds["zone_splash"] = {
    "sounds": [{
        "name": "scp_additions:zone_splash",
        "stream": False,
        "volume": 1.0,
    }]
}
sounds_path.write_text(json.dumps(sounds, separators=(",", ":")), encoding="utf-8")

ogg = ROOT / "src/main/resources/assets/scp_additions/sounds/zone_splash.ogg"
if not ogg.is_file():
    raise RuntimeError("zone_splash.ogg is missing from the current master assets")
