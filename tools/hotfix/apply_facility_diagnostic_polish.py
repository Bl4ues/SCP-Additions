from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace(path: str, old: str, new: str, count: int = -1) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Missing replacement anchor in {path}: {old[:120]!r}")
    target.write_text(text.replace(old, new, count), encoding="utf-8")


screen = r'''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.FacilityDiagnosticsPacket;
import net.mcreator.scpadditions.network.FacilityDiagnosticsResetPacket;

/** Black-and-green Foundation facility diagnostic terminal. */
public final class FacilityDiagnosticsScreen extends Screen {
    private static final int GREEN = 0xFF62E17A;
    private static final int DIM_GREEN = 0xFF3F9850;
    private static final int BLACK = 0xFF020503;
    private static final int PANEL = 0xFF061009;
    private static final int BUTTON = 0xFF0A1B0D;
    private static final int BUTTON_HOVER = 0xFF10351A;

    private final FacilityDiagnosticsPacket data;
    private int resetX;
    private int resetY;
    private int resetWidth;
    private static final int RESET_HEIGHT = 18;
    private boolean resetRequested;

    private FacilityDiagnosticsScreen(FacilityDiagnosticsPacket data) {
        super(Component.literal("Foundation Facility Diagnostics"));
        this.data = data;
    }

    public static void open(FacilityDiagnosticsPacket data) {
        Minecraft.getInstance().setScreen(new FacilityDiagnosticsScreen(data));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BLACK);
        int panelWidth = Math.min(410, width - 24);
        int panelHeight = Math.min(246, height - 24);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        border(graphics, x, y, panelWidth, panelHeight, DIM_GREEN);

        int tx = x + 12;
        int ty = y + 10;
        line(graphics, "SCP FOUNDATION // FACILITY SYSTEMS DIAGNOSTIC",
                tx, ty, GREEN);
        line(graphics, "SESSION CLASS: INTERNAL OPERATIONS",
                tx, ty + 12, DIM_GREEN);
        rule(graphics, tx, ty + 34, panelWidth - 24);

        if (data.auxiliaryPowerOnline()) {
            renderOnline(graphics, mouseX, mouseY, tx, ty, panelWidth);
        } else {
            renderOffline(graphics, tx, ty, panelWidth);
        }

        line(graphics, "PRESS ESC TO TERMINATE SESSION", tx, ty + 224,
                DIM_GREEN);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderOnline(GuiGraphics graphics, int mouseX, int mouseY,
            int tx, int ty, int panelWidth) {
        line(graphics, "[ ANOMALOUS CONTAINMENT TELEMETRY ]",
                tx, ty + 44, GREEN);
        metric(graphics, "SCP SIGNATURES (UNCONTAINED)",
                data.uncontainedScps(), tx, ty + 58, panelWidth - 24);
        String condition = data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        metric(graphics, "CONTAINMENT ASSURANCE", condition,
                tx, ty + 70, panelWidth - 24);

        line(graphics, "[ SECURITY INFRASTRUCTURE BUS ]",
                tx, ty + 90, GREEN);
        metric(graphics, "TESLA NODES ACTIVE", data.activeTeslaGates(),
                tx, ty + 104, panelWidth - 24);
        metric(graphics, "TESLA NODES REGISTERED",
                data.registeredTeslaGates(), tx, ty + 116,
                panelWidth - 24);
        metric(graphics, "TESLA MANUAL OVERRIDE",
                data.teslaOverride() ? "ACTIVE" : "INACTIVE",
                tx, ty + 128, panelWidth - 24);
        metric(graphics, "DOOR CONTROL ENDPOINTS", data.connectedDoors(),
                tx, ty + 140, panelWidth - 24);

        rule(graphics, tx, ty + 158, panelWidth - 24);
        line(graphics, "AUXILIARY FACILITY BUS: ONLINE",
                tx, ty + 168, GREEN);
        line(graphics, "DATA SCOPE: REGISTERED FOUNDATION ASSETS",
                tx, ty + 180, DIM_GREEN);

        resetX = tx;
        resetY = ty + 196;
        resetWidth = panelWidth - 24;
        boolean hovered = inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT);
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, hovered ? BUTTON_HOVER : BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                hovered ? GREEN : DIM_GREEN);
        String label = resetRequested
                ? "REMOTE SESSION RESET REQUESTED"
                : "RESET REMOTE SESSION CACHE";
        centered(graphics, label, resetX, resetY, resetWidth, RESET_HEIGHT,
                resetRequested ? DIM_GREEN : GREEN);
    }

    private void renderOffline(GuiGraphics graphics, int tx, int ty,
            int panelWidth) {
        line(graphics, "[ SYSTEM AVAILABILITY ]", tx, ty + 44, GREEN);
        metric(graphics, "AUXILIARY POWER FEED", "OFFLINE",
                tx, ty + 62, panelWidth - 24);
        metric(graphics, "FACILITY DIAGNOSTIC BUS", "UNAVAILABLE",
                tx, ty + 74, panelWidth - 24);
        metric(graphics, "SECURITY TELEMETRY", "SUSPENDED",
                tx, ty + 86, panelWidth - 24);

        rule(graphics, tx, ty + 108, panelWidth - 24);
        line(graphics, "LIVE FACILITY DATA CANNOT BE ACQUIRED.",
                tx, ty + 122, DIM_GREEN);
        line(graphics, "RESTORE AUXILIARY POWER TO RESUME OPERATIONS.",
                tx, ty + 136, DIM_GREEN);
        line(graphics, "REMOTE FACILITY ACCESS IS ALREADY ISOLATED.",
                tx, ty + 158, GREEN);

        resetX = tx;
        resetY = ty + 196;
        resetWidth = panelWidth - 24;
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                DIM_GREEN);
        centered(graphics, "REMOTE SESSION RESET UNAVAILABLE",
                resetX, resetY, resetWidth, RESET_HEIGHT, DIM_GREEN);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && data.auxiliaryPowerOnline() && !resetRequested
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
            int x, int y, int width) {
        String left = label + " ";
        String right = String.valueOf(value);
        int dots = Math.max(2, (width - font.width(left) - font.width(right))
                / Math.max(1, font.width(".")));
        line(graphics, left + ".".repeat(dots) + right, x, y, GREEN);
    }

    private void centered(GuiGraphics graphics, String text, int x, int y,
            int width, int height, int color) {
        graphics.drawString(font, text,
                x + Math.max(0, (width - font.width(text)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2),
                color, false);
    }

    private void line(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    private void rule(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, DIM_GREEN);
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/client/FacilityDiagnosticsScreen.java", screen)

packet = r'''package net.mcreator.scpadditions.network;

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
        boolean auxiliaryPowerOnline, BlockPos terminalPos) {

    public FacilityDiagnosticsPacket {
        terminalPos = terminalPos == null
                ? BlockPos.ZERO : terminalPos.immutable();
    }

    public FacilityDiagnosticsPacket(DiagnosticSnapshot snapshot,
            BlockPos terminalPos) {
        this(snapshot.uncontainedScps(), snapshot.activeTeslaGates(),
                snapshot.registeredTeslaGates(), snapshot.teslaOverride(),
                snapshot.connectedDoors(), snapshot.auxiliaryPowerOnline(),
                terminalPos);
    }

    public static void encode(FacilityDiagnosticsPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.max(0, message.uncontainedScps));
        buffer.writeVarInt(Math.max(0, message.activeTeslaGates));
        buffer.writeVarInt(Math.max(0, message.registeredTeslaGates));
        buffer.writeBoolean(message.teslaOverride);
        buffer.writeVarInt(Math.max(0, message.connectedDoors));
        buffer.writeBoolean(message.auxiliaryPowerOnline);
        buffer.writeBlockPos(message.terminalPos);
    }

    public static FacilityDiagnosticsPacket decode(FriendlyByteBuf buffer) {
        return new FacilityDiagnosticsPacket(buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBlockPos());
    }

    public static void handle(FacilityDiagnosticsPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FacilityDiagnosticsScreen.open(message)));
        context.setPacketHandled(true);
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/network/FacilityDiagnosticsPacket.java", packet)

reset_packet = r'''package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.block.SCP079SystemControlBlock;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;

import java.util.function.Supplier;

/** Server-authoritative remote-session reset from a nearby diagnostic terminal. */
public record FacilityDiagnosticsResetPacket(BlockPos terminalPos) {
    private static final double MAX_DISTANCE_SQR = 8.0D * 8.0D;

    public FacilityDiagnosticsResetPacket {
        terminalPos = terminalPos == null
                ? BlockPos.ZERO : terminalPos.immutable();
    }

    public static void encode(FacilityDiagnosticsResetPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.terminalPos);
    }

    public static FacilityDiagnosticsResetPacket decode(
            FriendlyByteBuf buffer) {
        return new FacilityDiagnosticsResetPacket(buffer.readBlockPos());
    }

    public static void handle(FacilityDiagnosticsResetPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            BlockPos pos = message.terminalPos();
            if (!level.hasChunkAt(pos)
                    || !(level.getBlockState(pos).getBlock()
                    instanceof SCP079SystemControlBlock)
                    || player.distanceToSqr(Vec3.atCenterOf(pos))
                    > MAX_DISTANCE_SQR) {
                return;
            }

            Scp079FacilityAccessManager.resetRemoteSession(player);
            ScpEntityNetwork.openFacilityDiagnostics(player,
                    Scp079FacilityAccessManager.currentDiagnosticSnapshot(
                            player), pos);
        });
        context.setPacketHandled(true);
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/network/FacilityDiagnosticsResetPacket.java", reset_packet)

replace(
    "src/main/java/net/mcreator/scpadditions/block/SCP079SystemControlBlock.java",
    '''        if (!level.isClientSide && level instanceof ServerLevel
                && player instanceof ServerPlayer serverPlayer) {
            if (!Scp079FacilityAccessManager.isAuxiliaryPowerOnline(level)) {
                serverPlayer.displayClientMessage(Component.literal(
                        "DIAGNOSTIC BUS UNAVAILABLE: AUXILIARY POWER ISOLATED"),
                        true);
            } else {
                ScpEntityNetwork.openFacilityDiagnostics(serverPlayer,
                        Scp079FacilityAccessManager.performDiagnosticScan(
                                serverPlayer));
            }
        }
''',
    '''        if (!level.isClientSide && level instanceof ServerLevel
                && player instanceof ServerPlayer serverPlayer) {
            ScpEntityNetwork.openFacilityDiagnostics(serverPlayer,
                    Scp079FacilityAccessManager.performDiagnosticScan(
                            serverPlayer), pos);
        }
''')

replace(
    "src/main/java/net/mcreator/scpadditions/block/SCP079SystemControlBlock.java",
    '''        tooltip.add(Component.literal(
                "Requires the Auxiliary Facility Bus to be online."));
''',
    '''        tooltip.add(Component.literal(
                "Auxiliary power is required for live telemetry."));
''')

replace(
    "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java",
    '''    public static DiagnosticSnapshot performDiagnosticScan(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return DiagnosticSnapshot.EMPTY;
        Scp079FacilityAccessSavedData data = data(server);
        pruneLoadedPositions(server, data);

        if (data.auxiliaryPowerOnline() && !data.hosts().isEmpty()
                && !data.facilityAccess() && !data.protocolExposed()) {
            data.setProtocolExposed(true);
            addDiscovery(server, data, SCAN_DISCOVERY);
        }

        int uncontained = countUncontainedScps(server);
        boolean override = server.getGameRules().getBoolean(
                ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean configured = server.getGameRules().getBoolean(
                ScpAdditionsModGameRules.TESLAGATEON);
        int totalGates = data.teslaGates().size();
        int activeGates = data.auxiliaryPowerOnline()
                && (configured || override) ? totalGates : 0;
        return new DiagnosticSnapshot(uncontained, activeGates, totalGates,
                override, data.doors().size());
    }
''',
    '''    public static DiagnosticSnapshot performDiagnosticScan(
            ServerPlayer player) {
        return diagnosticSnapshot(player, true);
    }

    public static DiagnosticSnapshot currentDiagnosticSnapshot(
            ServerPlayer player) {
        return diagnosticSnapshot(player, false);
    }

    private static DiagnosticSnapshot diagnosticSnapshot(ServerPlayer player,
            boolean exposeProtocol) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null) return DiagnosticSnapshot.EMPTY;
        Scp079FacilityAccessSavedData data = data(server);
        pruneLoadedPositions(server, data);

        if (exposeProtocol && data.auxiliaryPowerOnline()
                && !data.hosts().isEmpty() && !data.facilityAccess()
                && !data.protocolExposed()) {
            data.setProtocolExposed(true);
            addDiscovery(server, data, SCAN_DISCOVERY);
        }

        int uncontained = countUncontainedScps(server);
        boolean override = server.getGameRules().getBoolean(
                ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean configured = server.getGameRules().getBoolean(
                ScpAdditionsModGameRules.TESLAGATEON);
        int totalGates = data.teslaGates().size();
        int activeGates = data.auxiliaryPowerOnline()
                && (configured || override) ? totalGates : 0;
        return new DiagnosticSnapshot(uncontained, activeGates, totalGates,
                override, data.doors().size(), data.auxiliaryPowerOnline());
    }

    public static void resetRemoteSession(ServerPlayer actor) {
        MinecraftServer server = actor == null ? null : actor.getServer();
        if (server == null) return;
        resetCompromise(server, data(server));
        actor.displayClientMessage(Component.literal(
                "REMOTE SESSION CACHE: RESET COMPLETE"), true);
    }
''')

replace(
    "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java",
    '''    private static void resetCompromise(MinecraftServer server,
            Scp079FacilityAccessSavedData data) {
        boolean wasActive = data.facilityAccess();
        data.setProtocolExposed(false);
        data.setDiscoveryProgress(0.0D);
        data.setFacilityAccess(false);
        server.getGameRules().getRule(
                ScpAdditionsModGameRules.SCP079CONTROLON)
                .set(false, server);
        if (wasActive) Scp079ProcessingManager.onControlDisabled(server.overworld());
    }
''',
    '''    private static void resetCompromise(MinecraftServer server,
            Scp079FacilityAccessSavedData data) {
        data.setProtocolExposed(false);
        data.setDiscoveryProgress(0.0D);
        data.setFacilityAccess(false);
        server.getGameRules().getRule(
                ScpAdditionsModGameRules.SCP079CONTROLON)
                .set(false, server);
        Scp079ProcessingManager.resetPower(server.overworld());
    }
''')

replace(
    "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java",
    '''    private static int countUncontainedScps(MinecraftServer server) {
        int count = 0;
        for (RoamerType type : RoamerType.values()) {
            if (RoamerManager.hasActive(server, type)
                    && !RoamerManager.isContained(server, type)) {
                count++;
            }
        }
        return count;
    }
''',
    '''    private static int countUncontainedScps(MinecraftServer server) {
        int count = 0;
        for (RoamerType type : RoamerType.values()) {
            if (!RoamerManager.isContained(server, type)) {
                count += RoamerManager.activeCount(server, type);
            }
        }
        return count;
    }
''')

replace(
    "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java",
    '''    public record DiagnosticSnapshot(int uncontainedScps,
            int activeTeslaGates, int registeredTeslaGates,
            boolean teslaOverride, int connectedDoors) {
        public static final DiagnosticSnapshot EMPTY =
                new DiagnosticSnapshot(0, 0, 0, false, 0);
    }
''',
    '''    public record DiagnosticSnapshot(int uncontainedScps,
            int activeTeslaGates, int registeredTeslaGates,
            boolean teslaOverride, int connectedDoors,
            boolean auxiliaryPowerOnline) {
        public static final DiagnosticSnapshot EMPTY =
                new DiagnosticSnapshot(0, 0, 0, false, 0, false);
    }
''')

replace(
    "src/main/java/net/mcreator/scpadditions/facility/Scp079ProcessingManager.java",
    '''    public static void onControlDisabled(LevelAccessor level) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null) return;
        synchronized (STATES) {
            State state = state(server, false);
            update(server, state);
            state.active = false;
        }
    }
''',
    '''    public static void onControlDisabled(LevelAccessor level) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null) return;
        synchronized (STATES) {
            State state = state(server, false);
            update(server, state);
            state.active = false;
        }
    }

    public static void resetPower(LevelAccessor level) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null) return;
        synchronized (STATES) {
            State state = state(server, false);
            state.active = false;
            state.data.setPower(0.0D);
            state.recentSpend.clear();
            state.lastPurposeTick.clear();
            state.lastTick = server.getTickCount();
        }
        LAST_CLIENT_SYNC.clear();
    }
''')

replace(
    "src/main/java/net/mcreator/scpadditions/roamer/RoamerManager.java",
    '''    public static boolean hasActive(MinecraftServer server, RoamerType type) {
        if (server == null || type == null) return false;
        synchronized (STATES) {
            return !data(server, type).activeEntityIds.isEmpty();
        }
    }
''',
    '''    public static boolean hasActive(MinecraftServer server, RoamerType type) {
        return activeCount(server, type) > 0;
    }

    public static int activeCount(MinecraftServer server, RoamerType type) {
        if (server == null || type == null) return 0;
        synchronized (STATES) {
            return data(server, type).activeEntityIds.size();
        }
    }
''')

replace(
    "src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
    '''        ScpAdditionsMod.addNetworkMessage(
                ElevatorArrivalDisplayPacket.class,
                ElevatorArrivalDisplayPacket::encode,
                ElevatorArrivalDisplayPacket::decode,
                ElevatorArrivalDisplayPacket::handle);
''',
    '''        ScpAdditionsMod.addNetworkMessage(
                ElevatorArrivalDisplayPacket.class,
                ElevatorArrivalDisplayPacket::encode,
                ElevatorArrivalDisplayPacket::decode,
                ElevatorArrivalDisplayPacket::handle);
        ScpAdditionsMod.addNetworkMessage(FacilityDiagnosticsResetPacket.class,
                FacilityDiagnosticsResetPacket::encode,
                FacilityDiagnosticsResetPacket::decode,
                FacilityDiagnosticsResetPacket::handle);
''')

replace(
    "src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
    '''    public static void openFacilityDiagnostics(ServerPlayer player,
            net.mcreator.scpadditions.facility.Scp079FacilityAccessManager
                    .DiagnosticSnapshot snapshot) {
        if (player == null || snapshot == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FacilityDiagnosticsPacket(snapshot));
    }
''',
    '''    public static void openFacilityDiagnostics(ServerPlayer player,
            net.mcreator.scpadditions.facility.Scp079FacilityAccessManager
                    .DiagnosticSnapshot snapshot, BlockPos terminalPos) {
        if (player == null || snapshot == null || terminalPos == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FacilityDiagnosticsPacket(snapshot, terminalPos));
    }
''')

replace(
    "CHANGELOG.md",
    '''- Added a black-and-green Foundation diagnostic interface reporting vague global containment, Tesla-grid, override, and connected-door telemetry;
''',
    '''- Added a black-and-green Foundation diagnostic interface reporting vague global containment, Tesla-grid, override, and connected-door telemetry; it remains accessible without auxiliary power to report system unavailability and includes a manual remote-session cache reset;
''')

# Remove the temporary integration mechanism from the final branch state.
for temporary in (
    ".github/workflows/apply-facility-diagnostic-polish.yml",
    "tools/hotfix/APPLY_FACILITY_DIAGNOSTIC_POLISH",
    "tools/hotfix/apply_facility_diagnostic_polish.py",
):
    path = ROOT / temporary
    if path.exists():
        path.unlink()
