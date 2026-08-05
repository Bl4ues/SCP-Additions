from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content.rstrip() + "\n", encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    content = read(path)
    count = content.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one anchor in {path}, found {count}: {old[:120]!r}")
    write(path, content.replace(old, new, 1))


write(
    "src/main/java/net/mcreator/scpadditions/client/FacilityDiagnosticsScreen.java",
    r'''package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.FacilityDiagnosticsPacket;
import net.mcreator.scpadditions.network.FacilityDiagnosticsResetPacket;

/** ARC-Site-48 SCiPNET facility diagnostic terminal. */
public final class FacilityDiagnosticsScreen extends Screen {
    private static final ResourceLocation TERMINAL_LOGO = new ResourceLocation(
            "scp_additions", "textures/screens/terminallogo.png");

    private static final int BACKDROP = 0xFF0C1720;
    private static final int SHADOW = 0xFF05090C;
    private static final int BEZEL = 0xFF9A9A9A;
    private static final int BEZEL_LIGHT = 0xFFD8D8D4;
    private static final int BEZEL_DARK = 0xFF485864;
    private static final int SCREEN = 0xFF142735;
    private static final int HEADER = 0xFF1D3443;
    private static final int STEEL_BLUE = 0xFF283C4A;
    private static final int FOUNDATION_RED = 0xFFB53F41;
    private static final int OFF_WHITE = 0xFFEEEEEE;
    private static final int METAL_GRAY = 0xFFA1A0A2;
    private static final int SIGNAL_GOLD = 0xFFC49916;
    private static final int MUTED_BLUE = 0xFF72828D;
    private static final int BUTTON = 0xFF213746;
    private static final int BUTTON_HOVER = 0xFF304D5E;
    private static final int RESET_HEIGHT = 20;
    private static final int MAX_FRAME_WIDTH = 560;
    private static final int MAX_FRAME_HEIGHT = 420;

    private final FacilityDiagnosticsPacket data;
    private final long snapshotReceivedAtMillis;
    private int resetX;
    private int resetY;
    private int resetWidth;
    private boolean resetRequested;

    private FacilityDiagnosticsScreen(FacilityDiagnosticsPacket data) {
        super(ScpFonts.montserrat("ARC-Site-48 SCiPNET Diagnostics"));
        this.data = data;
        this.snapshotReceivedAtMillis = Util.getMillis();
    }

    public static void open(FacilityDiagnosticsPacket data) {
        Minecraft.getInstance().setScreen(new FacilityDiagnosticsScreen(data));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BACKDROP);

        int availableWidth = Math.max(4, width - 18);
        int availableHeight = Math.max(3, height - 18);
        int frameWidth = Math.min(Math.min(MAX_FRAME_WIDTH, availableWidth),
                Math.min(MAX_FRAME_HEIGHT, availableHeight) * 4 / 3);
        frameWidth = Math.max(4, frameWidth - frameWidth % 4);
        int frameHeight = frameWidth * 3 / 4;
        int frameX = (width - frameWidth) / 2;
        int frameY = (height - frameHeight) / 2;

        graphics.fill(frameX + 6, frameY + 8, frameX + frameWidth + 6,
                frameY + frameHeight + 8, SHADOW);
        graphics.fill(frameX, frameY, frameX + frameWidth,
                frameY + frameHeight, BEZEL_DARK);
        graphics.fill(frameX + 3, frameY + 3, frameX + frameWidth - 3,
                frameY + frameHeight - 3, BEZEL);
        graphics.fill(frameX + 6, frameY + 6, frameX + frameWidth - 6,
                frameY + frameHeight - 6, BEZEL_LIGHT);

        int screenX = frameX + 11;
        int screenY = frameY + 11;
        int screenWidth = frameWidth - 22;
        int screenHeight = frameHeight - 22;
        graphics.fill(screenX, screenY, screenX + screenWidth,
                screenY + screenHeight, SCREEN);
        drawScanlines(graphics, screenX, screenY, screenWidth, screenHeight);

        renderHeader(graphics, screenX, screenY, screenWidth);

        int contentX = screenX + 12;
        int contentY = screenY + 70;
        int contentWidth = screenWidth - 24;
        if (data.auxiliaryPowerOnline()) {
            renderOnline(graphics, mouseX, mouseY, contentX, contentY,
                    contentWidth);
        } else {
            renderOffline(graphics, contentX, contentY, contentWidth);
        }

        drawBody(graphics, "NODE ARC48-SYS-01 // ESC TO TERMINATE SESSION",
                contentX, screenY + screenHeight - 13, MUTED_BLUE);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 60, HEADER);
        graphics.fill(x, y + 57, x + width, y + 60, FOUNDATION_RED);

        graphics.pose().pushPose();
        graphics.pose().translate(x + 10.0F, y + 6.0F, 0.0F);
        float logoScale = 48.0F / 128.0F;
        graphics.pose().scale(logoScale, logoScale, 1.0F);
        graphics.blit(TERMINAL_LOGO, 0, 0, 0, 0,
                128, 128, 128, 128);
        graphics.pose().popPose();

        drawHeading(graphics, "SCiPNET TERMINAL v3.2.6",
                x + 68, y + 9, OFF_WHITE);
        drawHeading(graphics, "ARC-SITE-48 // INTERNAL SYSTEMS NODE",
                x + 68, y + 23, METAL_GRAY);
        drawHeading(graphics, "ARMED RESEARCH & CONTAINMENT FACILITY",
                x + 68, y + 37, MUTED_BLUE);

        Component site = heading("ARC-SITE-48");
        int badgeWidth = font.width(site) + 14;
        int badgeX = x + width - badgeWidth - 10;
        graphics.fill(badgeX, y + 8, badgeX + badgeWidth, y + 24,
                FOUNDATION_RED);
        centered(graphics, site, badgeX, y + 8, badgeWidth, 16, OFF_WHITE);
    }

    private void renderOnline(GuiGraphics graphics, int mouseX, int mouseY,
            int x, int y, int width) {
        sectionHeader(graphics, "CONTAINMENT INDEX", x, y, width);
        metric(graphics, "UNCONTAINED SCP SIGNATURES",
                twoDigits(data.uncontainedScps()), x, y + 20, width,
                data.uncontainedScps() == 0 ? SIGNAL_GOLD : FOUNDATION_RED);
        String integrity = data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        metric(graphics, "CONTAINMENT INTEGRITY", integrity,
                x, y + 34, width,
                data.uncontainedScps() == 0 ? SIGNAL_GOLD : FOUNDATION_RED);

        sectionHeader(graphics, "FACILITY TELEMETRY", x, y + 54, width);
        metric(graphics, "TESLA GATE SYSTEMS ACTIVE",
                twoDigits(data.activeTeslaGates()), x, y + 74, width,
                OFF_WHITE);
        metric(graphics, "TESLA GATE SYSTEMS REGISTERED",
                twoDigits(data.registeredTeslaGates()), x, y + 88, width,
                METAL_GRAY);
        metric(graphics, "TESLA GATE SYSTEMS MANUAL OVERRIDE",
                data.teslaOverride() ? "ACTIVE" : "INACTIVE",
                x, y + 102, width,
                data.teslaOverride() ? FOUNDATION_RED : METAL_GRAY);
        metric(graphics, "DOOR SYSTEM ENDPOINTS",
                twoDigits(data.connectedDoors()), x, y + 116, width,
                OFF_WHITE);

        statusStrip(graphics, "AUXILIARY POWER BUS", "ONLINE",
                x, y + 138, width, SIGNAL_GOLD);
        renderPurgeButton(graphics, mouseX, mouseY, x, y + 166, width, true);
    }

    private void renderOffline(GuiGraphics graphics, int x, int y, int width) {
        sectionHeader(graphics, "CONTAINMENT INDEX", x, y, width);
        metric(graphics, "UNCONTAINED SCP SIGNATURES", "UNAVAILABLE",
                x, y + 20, width, METAL_GRAY);
        metric(graphics, "CONTAINMENT INTEGRITY", "UNAVAILABLE",
                x, y + 34, width, METAL_GRAY);

        sectionHeader(graphics, "FACILITY TELEMETRY", x, y + 54, width);
        metric(graphics, "TESLA GATE SYSTEMS ACTIVE", "UNAVAILABLE",
                x, y + 74, width, METAL_GRAY);
        metric(graphics, "TESLA GATE SYSTEMS REGISTERED", "UNAVAILABLE",
                x, y + 88, width, METAL_GRAY);
        metric(graphics, "TESLA GATE SYSTEMS MANUAL OVERRIDE", "UNAVAILABLE",
                x, y + 102, width, METAL_GRAY);
        metric(graphics, "DOOR SYSTEM ENDPOINTS", "UNAVAILABLE",
                x, y + 116, width, METAL_GRAY);

        statusStrip(graphics, "AUXILIARY POWER BUS", "OFFLINE",
                x, y + 138, width, FOUNDATION_RED);
        renderPurgeButton(graphics, 0, 0, x, y + 166, width, false);
    }

    private void renderPurgeButton(GuiGraphics graphics, int mouseX,
            int mouseY, int x, int y, int width, boolean powered) {
        resetX = x;
        resetY = y;
        resetWidth = width;
        int cooldown = cooldownRemainingTicks();
        boolean enabled = powered && cooldown <= 0 && !resetRequested;
        boolean hovered = enabled && inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT);
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, hovered ? BUTTON_HOVER : BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                hovered ? FOUNDATION_RED : BEZEL_DARK);

        String label;
        int color;
        if (!powered) {
            label = "REMOTE SESSION CACHE PURGE UNAVAILABLE // AUX POWER OFFLINE";
            color = METAL_GRAY;
        } else if (resetRequested) {
            label = "REMOTE SESSION CACHE PURGE REQUESTED";
            color = METAL_GRAY;
        } else if (cooldown > 0) {
            label = "REMOTE SESSION CACHE LOCKOUT // "
                    + formatCooldown(cooldown);
            color = SIGNAL_GOLD;
        } else {
            label = "PURGE REMOTE SESSION CACHE";
            color = OFF_WHITE;
        }
        centered(graphics, body(label), resetX, resetY, resetWidth,
                RESET_HEIGHT, color);
    }

    private void sectionHeader(GuiGraphics graphics, String title,
            int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 15, STEEL_BLUE);
        graphics.fill(x, y, x + 4, y + 15, FOUNDATION_RED);
        drawHeading(graphics, title, x + 9, y + 3, OFF_WHITE);
    }

    private void statusStrip(GuiGraphics graphics, String label, String value,
            int x, int y, int width, int valueColor) {
        graphics.fill(x, y, x + width, y + 19, HEADER);
        border(graphics, x, y, width, 19, BEZEL_DARK);
        drawBody(graphics, label, x + 7, y + 5, METAL_GRAY);
        rightAligned(graphics, body(value), x + width - 7, y + 5,
                valueColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && data.auxiliaryPowerOnline() && !resetRequested
                && cooldownRemainingTicks() <= 0
                && inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT)) {
            resetRequested = true;
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                    new FacilityDiagnosticsResetPacket(data.terminalPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void metric(GuiGraphics graphics, String label, Object value,
            int x, int y, int width, int valueColor) {
        Component left = body(label + " ");
        Component right = body(String.valueOf(value));
        Component dot = body(".");
        int available = width - font.width(left) - font.width(right) - 8;
        int dots = Math.max(2, available / Math.max(1, font.width(dot)));
        drawBody(graphics, label + " " + ".".repeat(dots),
                x, y, METAL_GRAY);
        rightAligned(graphics, right, x + width, y, valueColor);
    }

    private void rightAligned(GuiGraphics graphics, Component text,
            int right, int y, int color) {
        graphics.drawString(font, text, right - font.width(text), y,
                color, false);
    }

    private void centered(GuiGraphics graphics, Component text, int x, int y,
            int width, int height, int color) {
        graphics.drawString(font, text,
                x + Math.max(0, (width - font.width(text)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2),
                color, false);
    }

    private void drawBody(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, body(text), x, y, color, false);
    }

    private void drawHeading(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, heading(text), x, y, color, false);
    }

    private static Component body(String text) {
        return ScpFonts.anonymousPro(text);
    }

    private static Component heading(String text) {
        return ScpFonts.montserrat(text);
    }

    private int cooldownRemainingTicks() {
        long elapsedMillis = Math.max(0L,
                Util.getMillis() - snapshotReceivedAtMillis);
        int elapsedTicks = (int) Math.min(Integer.MAX_VALUE,
                elapsedMillis / 50L);
        return Math.max(0, data.cachePurgeCooldownTicks() - elapsedTicks);
    }

    private static String formatCooldown(int ticks) {
        int totalSeconds = Math.max(0, (ticks + 19) / 20);
        return String.format("%02d:%02d", totalSeconds / 60,
                totalSeconds % 60);
    }

    private static String twoDigits(int value) {
        return String.format("%02d", Math.max(0, value));
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static void drawScanlines(GuiGraphics graphics, int x, int y,
            int width, int height) {
        for (int lineY = y + 1; lineY < y + height; lineY += 4) {
            graphics.fill(x, lineY, x + width, lineY + 1, 0x12000000);
        }
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
''')

write(
    "src/main/java/net/mcreator/scpadditions/network/FacilityDiagnosticsPacket.java",
    r'''package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.FacilityDiagnosticsScreen;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager.DiagnosticSnapshot;

import java.util.function.Supplier;

/** Opens the Foundation facility diagnostic screen. */
public record FacilityDiagnosticsPacket(int uncontainedScps,
        int activeTeslaGates, int registeredTeslaGates,
        boolean teslaOverride, int connectedDoors,
        boolean auxiliaryPowerOnline, int cachePurgeCooldownTicks,
        BlockPos terminalPos) {

    public FacilityDiagnosticsPacket {
        cachePurgeCooldownTicks = Math.max(0, cachePurgeCooldownTicks);
        terminalPos = terminalPos == null
                ? BlockPos.ZERO : terminalPos.immutable();
    }

    public FacilityDiagnosticsPacket(DiagnosticSnapshot snapshot,
            BlockPos terminalPos) {
        this(snapshot.uncontainedScps(), snapshot.activeTeslaGates(),
                snapshot.registeredTeslaGates(), snapshot.teslaOverride(),
                snapshot.connectedDoors(), snapshot.auxiliaryPowerOnline(),
                snapshot.cachePurgeCooldownTicks(), terminalPos);
    }

    public static void encode(FacilityDiagnosticsPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.max(0, message.uncontainedScps));
        buffer.writeVarInt(Math.max(0, message.activeTeslaGates));
        buffer.writeVarInt(Math.max(0, message.registeredTeslaGates));
        buffer.writeBoolean(message.teslaOverride);
        buffer.writeVarInt(Math.max(0, message.connectedDoors));
        buffer.writeBoolean(message.auxiliaryPowerOnline);
        buffer.writeVarInt(Math.max(0, message.cachePurgeCooldownTicks));
        buffer.writeBlockPos(message.terminalPos);
    }

    public static FacilityDiagnosticsPacket decode(FriendlyByteBuf buffer) {
        return new FacilityDiagnosticsPacket(buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBlockPos());
    }

    public static void handle(FacilityDiagnosticsPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FacilityDiagnosticsScreen.open(message)));
        context.setPacketHandled(true);
    }
}
''')

write(
    "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessSavedData.java",
    r'''package net.mcreator.scpadditions.facility;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/** Persistent state for the powered facility diagnostic bus and SCP-079 discovery. */
final class Scp079FacilityAccessSavedData extends SavedData {
    private static final String DATA_NAME = "scp_additions_scp079_facility_access";

    private boolean auxiliaryPowerOnline;
    private boolean protocolExposed;
    private boolean facilityAccess;
    private double discoveryProgress;
    private long cachePurgeLockoutUntilGameTime;

    private final Set<TrackedPosition> hosts = new LinkedHashSet<>();
    private final Set<TrackedPosition> doors = new LinkedHashSet<>();
    private final Set<TrackedPosition> teslaGates = new LinkedHashSet<>();
    private final Set<TrackedPosition> auxiliaryUnits = new LinkedHashSet<>();

    private Scp079FacilityAccessSavedData() {
    }

    static Scp079FacilityAccessSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                Scp079FacilityAccessSavedData::load,
                Scp079FacilityAccessSavedData::new,
                DATA_NAME);
    }

    private static Scp079FacilityAccessSavedData load(CompoundTag tag) {
        Scp079FacilityAccessSavedData data = new Scp079FacilityAccessSavedData();
        data.auxiliaryPowerOnline = tag.getBoolean("AuxiliaryPowerOnline");
        data.protocolExposed = tag.getBoolean("ProtocolExposed");
        data.facilityAccess = tag.getBoolean("FacilityAccess");
        if (tag.contains("DiscoveryProgress", Tag.TAG_ANY_NUMERIC)) {
            data.discoveryProgress = clamp(tag.getDouble("DiscoveryProgress"));
        }
        if (tag.contains("CachePurgeLockoutUntilGameTime",
                Tag.TAG_ANY_NUMERIC)) {
            data.cachePurgeLockoutUntilGameTime = Math.max(0L,
                    tag.getLong("CachePurgeLockoutUntilGameTime"));
        }
        readPositions(tag, "Hosts", data.hosts);
        readPositions(tag, "Doors", data.doors);
        readPositions(tag, "TeslaGates", data.teslaGates);
        readPositions(tag, "AuxiliaryUnits", data.auxiliaryUnits);
        return data;
    }

    boolean auxiliaryPowerOnline() {
        return auxiliaryPowerOnline;
    }

    void setAuxiliaryPowerOnline(boolean value) {
        if (auxiliaryPowerOnline == value) return;
        auxiliaryPowerOnline = value;
        setDirty();
    }

    boolean protocolExposed() {
        return protocolExposed;
    }

    void setProtocolExposed(boolean value) {
        if (protocolExposed == value) return;
        protocolExposed = value;
        setDirty();
    }

    boolean facilityAccess() {
        return facilityAccess;
    }

    void setFacilityAccess(boolean value) {
        if (facilityAccess == value) return;
        facilityAccess = value;
        setDirty();
    }

    double discoveryProgress() {
        return discoveryProgress;
    }

    void setDiscoveryProgress(double value) {
        double clamped = clamp(value);
        if (Math.abs(clamped - discoveryProgress) < 0.000001D) return;
        discoveryProgress = clamped;
        setDirty();
    }

    long cachePurgeLockoutUntilGameTime() {
        return cachePurgeLockoutUntilGameTime;
    }

    void setCachePurgeLockoutUntilGameTime(long value) {
        long sanitized = Math.max(0L, value);
        if (cachePurgeLockoutUntilGameTime == sanitized) return;
        cachePurgeLockoutUntilGameTime = sanitized;
        setDirty();
    }

    Set<TrackedPosition> hosts() {
        return hosts;
    }

    Set<TrackedPosition> doors() {
        return doors;
    }

    Set<TrackedPosition> teslaGates() {
        return teslaGates;
    }

    Set<TrackedPosition> auxiliaryUnits() {
        return auxiliaryUnits;
    }

    void markChanged() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("AuxiliaryPowerOnline", auxiliaryPowerOnline);
        tag.putBoolean("ProtocolExposed", protocolExposed);
        tag.putBoolean("FacilityAccess", facilityAccess);
        tag.putDouble("DiscoveryProgress", discoveryProgress);
        tag.putLong("CachePurgeLockoutUntilGameTime",
                cachePurgeLockoutUntilGameTime);
        writePositions(tag, "Hosts", hosts);
        writePositions(tag, "Doors", doors);
        writePositions(tag, "TeslaGates", teslaGates);
        writePositions(tag, "AuxiliaryUnits", auxiliaryUnits);
        return tag;
    }

    private static void writePositions(CompoundTag root, String key,
            Set<TrackedPosition> positions) {
        ListTag list = new ListTag();
        for (TrackedPosition position : positions) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", position.dimension());
            entry.putLong("Pos", position.packedPos());
            list.add(entry);
        }
        root.put(key, list);
    }

    private static void readPositions(CompoundTag root, String key,
            Set<TrackedPosition> target) {
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            String dimension = entry.getString("Dimension");
            if (!dimension.isBlank()) {
                target.add(new TrackedPosition(dimension, entry.getLong("Pos")));
            }
        }
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    record TrackedPosition(String dimension, long packedPos) {
        static TrackedPosition of(String dimension, long packedPos) {
            return new TrackedPosition(dimension, packedPos);
        }

        int chunkX() {
            return net.minecraft.core.BlockPos.getX(packedPos) >> 4;
        }

        int chunkZ() {
            return net.minecraft.core.BlockPos.getZ(packedPos) >> 4;
        }
    }
}
''')

manager = "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java"
replace_once(
    manager,
    "    private static final double SCAN_DISCOVERY = 8.0D;\n",
    "    private static final double SCAN_DISCOVERY = 8.0D;\n"
    "    private static final long CACHE_PURGE_LOCKOUT_TICKS =\n"
    "            5L * 60L * 20L;\n")

replace_once(
    manager,
    "    public static boolean auxiliaryPowerOnline(MinecraftServer server) {\n"
    "        return server != null && data(server).auxiliaryPowerOnline();\n"
    "    }\n",
    "    public static boolean auxiliaryPowerOnline(MinecraftServer server) {\n"
    "        return server != null && data(server).auxiliaryPowerOnline();\n"
    "    }\n\n"
    "    public static int cachePurgeCooldownTicks(MinecraftServer server) {\n"
    "        if (server == null) return 0;\n"
    "        Scp079FacilityAccessSavedData data = data(server);\n"
    "        long remaining = data.cachePurgeLockoutUntilGameTime()\n"
    "                - server.overworld().getGameTime();\n"
    "        if (remaining <= 0L) {\n"
    "            if (data.cachePurgeLockoutUntilGameTime() != 0L) {\n"
    "                data.setCachePurgeLockoutUntilGameTime(0L);\n"
    "            }\n"
    "            return 0;\n"
    "        }\n"
    "        return (int) Math.min(Integer.MAX_VALUE, remaining);\n"
    "    }\n\n"
    "    private static boolean cachePurgeLocked(MinecraftServer server) {\n"
    "        return cachePurgeCooldownTicks(server) > 0;\n"
    "    }\n")

replace_once(
    manager,
    "        if (exposeProtocol && data.auxiliaryPowerOnline()\n"
    "                && !data.hosts().isEmpty() && !data.facilityAccess()\n"
    "                && !data.protocolExposed()) {\n",
    "        if (exposeProtocol && data.auxiliaryPowerOnline()\n"
    "                && !cachePurgeLocked(server)\n"
    "                && !data.hosts().isEmpty() && !data.facilityAccess()\n"
    "                && !data.protocolExposed()) {\n")

replace_once(
    manager,
    "        return new DiagnosticSnapshot(uncontained, activeGates, totalGates,\n"
    "                override, data.doors().size(), data.auxiliaryPowerOnline());\n",
    "        return new DiagnosticSnapshot(uncontained, activeGates, totalGates,\n"
    "                override, data.doors().size(), data.auxiliaryPowerOnline(),\n"
    "                cachePurgeCooldownTicks(server));\n")

replace_once(
    manager,
    "    public static void resetRemoteSession(ServerPlayer actor) {\n"
    "        MinecraftServer server = actor == null ? null : actor.getServer();\n"
    "        if (server == null) return;\n"
    "        resetCompromise(server, data(server));\n"
    "        actor.displayClientMessage(Component.literal(\n"
    "                \"REMOTE SESSION CACHE: RESET COMPLETE\"), true);\n"
    "    }\n",
    "    public static void resetRemoteSession(ServerPlayer actor) {\n"
    "        MinecraftServer server = actor == null ? null : actor.getServer();\n"
    "        if (server == null) return;\n"
    "        Scp079FacilityAccessSavedData data = data(server);\n"
    "        if (!data.auxiliaryPowerOnline()) {\n"
    "            actor.displayClientMessage(Component.literal(\n"
    "                    \"CACHE PURGE UNAVAILABLE: AUXILIARY POWER OFFLINE\"),\n"
    "                    true);\n"
    "            return;\n"
    "        }\n"
    "        int remaining = cachePurgeCooldownTicks(server);\n"
    "        if (remaining > 0) {\n"
    "            actor.displayClientMessage(Component.literal(\n"
    "                    \"REMOTE SESSION CACHE LOCKOUT: \"\n"
    "                            + ((remaining + 19) / 20) + \"s\"), true);\n"
    "            return;\n"
    "        }\n"
    "        resetCompromise(server, data);\n"
    "        data.setCachePurgeLockoutUntilGameTime(\n"
    "                server.overworld().getGameTime()\n"
    "                        + CACHE_PURGE_LOCKOUT_TICKS);\n"
    "        actor.displayClientMessage(Component.literal(\n"
    "                \"REMOTE SESSION CACHE: PURGE COMPLETE\"), true);\n"
    "    }\n")

replace_once(
    manager,
    "        if (!data.auxiliaryPowerOnline() || !data.protocolExposed()\n"
    "                || data.hosts().isEmpty() || data.facilityAccess()) {\n",
    "        if (!data.auxiliaryPowerOnline() || cachePurgeLocked(server)\n"
    "                || !data.protocolExposed() || data.hosts().isEmpty()\n"
    "                || data.facilityAccess()) {\n")

replace_once(
    manager,
    "        MinecraftServer server = event.getServer();\n"
    "        Scp079FacilityAccessSavedData data = data(server);\n"
    "        if (!data.auxiliaryPowerOnline()\n",
    "        MinecraftServer server = event.getServer();\n"
    "        Scp079FacilityAccessSavedData data = data(server);\n"
    "        if (cachePurgeLocked(server)) {\n"
    "            if (data.protocolExposed() || data.facilityAccess()\n"
    "                    || data.discoveryProgress() > 0.0D) {\n"
    "                resetCompromise(server, data);\n"
    "            }\n"
    "            if (server.getGameRules().getBoolean(\n"
    "                    ScpAdditionsModGameRules.SCP079CONTROLON)) {\n"
    "                server.getGameRules().getRule(\n"
    "                        ScpAdditionsModGameRules.SCP079CONTROLON)\n"
    "                        .set(false, server);\n"
    "            }\n"
    "            return;\n"
    "        }\n"
    "        if (!data.auxiliaryPowerOnline()\n")

replace_once(
    manager,
    "    private static void addDiscovery(MinecraftServer server,\n"
    "            Scp079FacilityAccessSavedData data, double amount) {\n"
    "        data.setDiscoveryProgress(data.discoveryProgress() + amount);\n",
    "    private static void addDiscovery(MinecraftServer server,\n"
    "            Scp079FacilityAccessSavedData data, double amount) {\n"
    "        if (cachePurgeLocked(server)) return;\n"
    "        data.setDiscoveryProgress(data.discoveryProgress() + amount);\n")

replace_once(
    manager,
    "    public record DiagnosticSnapshot(int uncontainedScps,\n"
    "            int activeTeslaGates, int registeredTeslaGates,\n"
    "            boolean teslaOverride, int connectedDoors,\n"
    "            boolean auxiliaryPowerOnline) {\n"
    "        public static final DiagnosticSnapshot EMPTY =\n"
    "                new DiagnosticSnapshot(0, 0, 0, false, 0, false);\n"
    "    }\n",
    "    public record DiagnosticSnapshot(int uncontainedScps,\n"
    "            int activeTeslaGates, int registeredTeslaGates,\n"
    "            boolean teslaOverride, int connectedDoors,\n"
    "            boolean auxiliaryPowerOnline,\n"
    "            int cachePurgeCooldownTicks) {\n"
    "        public static final DiagnosticSnapshot EMPTY =\n"
    "                new DiagnosticSnapshot(0, 0, 0, false, 0, false, 0);\n"
    "    }\n")

changelog = "CHANGELOG.md"
replace_once(
    changelog,
    "- Restyled the Facility Diagnostic Terminal as an ARC-Site-48 SCiPNET v3.2.6 CRT interface using the site crest palette, retained offline service reporting and remote-session cache purge controls, and corrected uncontained SCP signatures to represent enabled SCP profiles released from containment rather than only currently spawned entities;\n",
    "- Restyled the Facility Diagnostic Terminal as a fixed 4:3 ARC-Site-48 SCiPNET v3.2.6 CRT interface using the complete site crest, Montserrat headings, Anonymous Pro telemetry, matching online and offline diagnostic sections, and a five-minute remote-session cache lockout that prevents SCP-079 from restarting protocol discovery while active; corrected uncontained SCP signatures to represent enabled SCP profiles released from containment rather than only currently spawned entities;\n")
