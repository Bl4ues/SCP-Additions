from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def write(path, text):
    (ROOT / path).write_text(text, encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)


# ---------------------------------------------------------------------------
# Facility access semantics
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java"
text = read(path)

text = replace_once(text, '''        Scp079FacilityAccessSavedData data = data(server);
        data.setAuxiliaryPowerOnline(online);
        if (!online) {
            resetCompromise(server, data);
        }
        synchronizeAuxiliaryUnits(server, online);
        if (online) wakeTeslaGates(server, data);
''', '''        Scp079FacilityAccessSavedData data = data(server);
        data.setAuxiliaryPowerOnline(online);
        synchronizeAuxiliaryUnits(server, online);
        if (online) wakeTeslaGates(server, data);

        boolean controlActive = online && data.facilityAccess()
                && !data.hosts().isEmpty();
        server.getGameRules().getRule(
                ScpAdditionsModGameRules.SCP079CONTROLON)
                .set(controlActive, server);
        if (controlActive) {
            Scp079ProcessingManager.onControlEnabled(server.overworld());
        } else {
            Scp079ProcessingManager.onControlDisabled(server.overworld());
        }
''', "auxiliary power semantics")

text = replace_once(text, '''        return new DiagnosticSnapshot(uncontained, activeGates, totalGates,
                override, data.doors().size(), data.auxiliaryPowerOnline(),
                cachePurgeCooldownTicks(server));
''', '''        boolean unusualNetworkActivity = data.facilityAccess()
                && !data.hosts().isEmpty();
        return new DiagnosticSnapshot(uncontained, activeGates, totalGates,
                override, data.doors().size(), data.auxiliaryPowerOnline(),
                cachePurgeCooldownTicks(server), unusualNetworkActivity);
''', "diagnostic anomaly flag")

text = replace_once(text, '''    public static void registerHost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.hosts().add(tracked(level, pos))) data.markChanged();
    }

    public static void unregisterHost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.hosts().remove(tracked(level, pos))) data.markChanged();
        if (data.hosts().isEmpty()) resetCompromise(level.getServer(), data);
    }
''', '''    public static void registerHost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        MinecraftServer server = level.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        if (data.hosts().add(tracked(level, pos))) data.markChanged();
        if (data.facilityAccess() && data.auxiliaryPowerOnline()) {
            server.getGameRules().getRule(
                    ScpAdditionsModGameRules.SCP079CONTROLON)
                    .set(true, server);
            Scp079ProcessingManager.onControlEnabled(server.overworld());
        }
    }

    public static void unregisterHost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        MinecraftServer server = level.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        if (data.hosts().remove(tracked(level, pos))) data.markChanged();
        if (data.hosts().isEmpty()) {
            server.getGameRules().getRule(
                    ScpAdditionsModGameRules.SCP079CONTROLON)
                    .set(false, server);
            Scp079ProcessingManager.onControlDisabled(server.overworld());
        }
    }
''', "host persistence")

server_tick_pattern = re.compile(r'''    @SubscribeEvent
    public static void onServerTick\(TickEvent\.ServerTickEvent event\) \{.*?
    \}

    @SubscribeEvent
    public static void onChunkLoad''', re.S)
server_tick_replacement = '''    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        if (cachePurgeLocked(server)) {
            if (data.protocolExposed() || data.facilityAccess()
                    || data.discoveryProgress() > 0.0D) {
                resetCompromise(server, data);
            }
            if (server.getGameRules().getBoolean(
                    ScpAdditionsModGameRules.SCP079CONTROLON)) {
                server.getGameRules().getRule(
                        ScpAdditionsModGameRules.SCP079CONTROLON)
                        .set(false, server);
            }
            Scp079ProcessingManager.onControlDisabled(server.overworld());
            return;
        }

        if (data.auxiliaryPowerOnline() && data.protocolExposed()
                && !data.hosts().isEmpty() && !data.facilityAccess()) {
            addDiscovery(server, data, PASSIVE_DISCOVERY_PER_SECOND);
        }

        boolean expectedRule = data.auxiliaryPowerOnline()
                && data.facilityAccess() && !data.hosts().isEmpty();
        boolean currentRule = server.getGameRules().getBoolean(
                ScpAdditionsModGameRules.SCP079CONTROLON);
        if (currentRule != expectedRule) {
            server.getGameRules().getRule(
                    ScpAdditionsModGameRules.SCP079CONTROLON)
                    .set(expectedRule, server);
            if (expectedRule) {
                Scp079ProcessingManager.onControlEnabled(server.overworld());
            } else {
                Scp079ProcessingManager.onControlDisabled(server.overworld());
            }
        }

        // Force lazy AP regeneration/decay to advance even when no debug HUD
        // or SCP action happens to query the processing manager.
        Scp079ProcessingManager.getPower(server.overworld());
    }

    @SubscribeEvent
    public static void onChunkLoad'''
text, count = server_tick_pattern.subn(server_tick_replacement, text, count=1)
if count != 1:
    raise RuntimeError(f"server tick rewrite: expected 1 match, found {count}")

text = replace_once(text, '''        if (changed) data.markChanged();
        if (data.hosts().isEmpty()
                && (data.protocolExposed() || data.facilityAccess()
                || data.discoveryProgress() > 0.0D)) {
            resetCompromise(server, data);
        }
''', '''        if (changed) data.markChanged();
        if (data.hosts().isEmpty()) {
            server.getGameRules().getRule(
                    ScpAdditionsModGameRules.SCP079CONTROLON)
                    .set(false, server);
            Scp079ProcessingManager.onControlDisabled(server.overworld());
        }
''', "pruned host persistence")

text = replace_once(text, '''    public record DiagnosticSnapshot(int uncontainedScps,
            int activeTeslaGates, int registeredTeslaGates,
            boolean teslaOverride, int connectedDoors,
            boolean auxiliaryPowerOnline,
            int cachePurgeCooldownTicks) {
        public static final DiagnosticSnapshot EMPTY =
                new DiagnosticSnapshot(0, 0, 0, false, 0, false, 0);
    }
''', '''    public record DiagnosticSnapshot(int uncontainedScps,
            int activeTeslaGates, int registeredTeslaGates,
            boolean teslaOverride, int connectedDoors,
            boolean auxiliaryPowerOnline,
            int cachePurgeCooldownTicks,
            boolean unusualNetworkActivity) {
        public static final DiagnosticSnapshot EMPTY =
                new DiagnosticSnapshot(0, 0, 0, false, 0, false, 0, false);
    }
''', "snapshot record")

write(path, text)

# ---------------------------------------------------------------------------
# Network snapshot
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/network/FacilityDiagnosticsPacket.java"
text = read(path)
text = replace_once(text, '''        boolean teslaOverride, int connectedDoors,
        boolean auxiliaryPowerOnline, int cachePurgeCooldownTicks,
        BlockPos terminalPos) {
''', '''        boolean teslaOverride, int connectedDoors,
        boolean auxiliaryPowerOnline, int cachePurgeCooldownTicks,
        boolean unusualNetworkActivity, BlockPos terminalPos) {
''', "packet record")
text = replace_once(text, '''                snapshot.connectedDoors(), snapshot.auxiliaryPowerOnline(),
                snapshot.cachePurgeCooldownTicks(), terminalPos);
''', '''                snapshot.connectedDoors(), snapshot.auxiliaryPowerOnline(),
                snapshot.cachePurgeCooldownTicks(),
                snapshot.unusualNetworkActivity(), terminalPos);
''', "packet snapshot constructor")
text = replace_once(text, '''        buffer.writeBoolean(message.auxiliaryPowerOnline);
        buffer.writeVarInt(Math.max(0, message.cachePurgeCooldownTicks));
        buffer.writeBlockPos(message.terminalPos);
''', '''        buffer.writeBoolean(message.auxiliaryPowerOnline);
        buffer.writeVarInt(Math.max(0, message.cachePurgeCooldownTicks));
        buffer.writeBoolean(message.unusualNetworkActivity);
        buffer.writeBlockPos(message.terminalPos);
''', "packet encode")
text = replace_once(text, '''                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBlockPos());
''', '''                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBlockPos());
''', "packet decode")
write(path, text)

# ---------------------------------------------------------------------------
# Terminal UI: reconstruction state, subtle intrusion advisory, compact warning
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/client/FacilityDiagnosticsScreen.java"
text = read(path)
text = replace_once(text, '''    private static final ResourceLocation TERMINAL_TEXT = new ResourceLocation(
            "scp_additions", "scipnet_terminal");
''', '''    private static final ResourceLocation TERMINAL_TEXT = new ResourceLocation(
            "scp_additions", "scipnet_terminal");
    private static final ResourceLocation TERMINAL_CAPTION = new ResourceLocation(
            "scp_additions", "scipnet_caption");
''', "caption font id")
text = replace_once(text, '''    private static final int MAX_FRAME_WIDTH = 560;
    private static final int MAX_FRAME_HEIGHT = 420;
''', '''    private static final int MAX_FRAME_WIDTH = 560;
    private static final int MAX_FRAME_HEIGHT = 420;
    private static final int CACHE_PURGE_TOTAL_TICKS = 5 * 60 * 20;
''', "reindex duration constant")

old = '''        String signatures = powered ? twoDigits(data.uncontainedScps())
                : "UNAVAILABLE";
        String integrity = powered
                ? data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL"
                : "UNAVAILABLE";
'''
new = '''        boolean reindexing = powered && cooldownRemainingTicks() > 0;
        int reindex = reindexProgressPercent();
        String signatures = !powered ? "UNAVAILABLE"
                : reindexing ? "READING " + reindex + "%"
                : twoDigits(data.uncontainedScps());
        String integrity = !powered ? "UNAVAILABLE"
                : reindexing ? "REASSESSING"
                : data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
'''
text = replace_once(text, old, new, "containment reindex values")
text = replace_once(text, '''        drawBody(graphics, powered ? "SCOPE // SITE-WIDE REGISTRY"
                        : "REGISTRY LINK // SUSPENDED",
                x + 9, y + height - 15, MUTED_BLUE);
''', '''        drawBody(graphics, !powered ? "REGISTRY LINK // SUSPENDED"
                        : reindexing ? "REGISTRY REINDEX // " + reindex + "%"
                        : "SCOPE // SITE-WIDE REGISTRY",
                x + 9, y + height - 15,
                reindexing ? SIGNAL_GOLD : MUTED_BLUE);
''', "containment reindex footer")

old = '''        String teslaGrid = powered
                ? twoDigits(data.activeTeslaGates()) + " / "
                        + twoDigits(data.registeredTeslaGates()) + " ONLINE"
                : "UNAVAILABLE";
        String override = powered
                ? data.teslaOverride() ? "ACTIVE" : "INACTIVE"
                : "UNAVAILABLE";
        String doors = powered ? twoDigits(data.connectedDoors())
                : "UNAVAILABLE";
'''
new = '''        boolean reindexing = powered && cooldownRemainingTicks() > 0;
        int reindex = reindexProgressPercent();
        String teslaGrid = !powered ? "UNAVAILABLE"
                : reindexing ? "READING " + reindex + "%"
                : twoDigits(data.activeTeslaGates()) + " / "
                        + twoDigits(data.registeredTeslaGates()) + " ONLINE";
        String override = !powered ? "UNAVAILABLE"
                : reindexing ? "VERIFYING"
                : data.teslaOverride() ? "ACTIVE" : "INACTIVE";
        String doors = !powered ? "UNAVAILABLE"
                : reindexing ? "INDEXING " + reindex + "%"
                : twoDigits(data.connectedDoors());
'''
text = replace_once(text, old, new, "facility reindex values")
text = replace_once(text, '''        drawBody(graphics, powered ? "LINK // ENDPOINT REGISTRY CURRENT"
                        : "ENDPOINT BUS // SUSPENDED",
                x + 9, y + height - 15, MUTED_BLUE);
''', '''        drawBody(graphics, !powered ? "ENDPOINT BUS // SUSPENDED"
                        : reindexing ? "TELEMETRY REBUILD // " + reindex + "%"
                        : "LINK // ENDPOINT REGISTRY CURRENT",
                x + 9, y + height - 15,
                reindexing ? SIGNAL_GOLD : MUTED_BLUE);
''', "facility reindex footer")

text = replace_once(text, '''        String cacheState = !powered ? "UNAVAILABLE"
                : cooldown > 0 ? "LOCKOUT " + formatCooldown(cooldown)
                : resetRequested ? "PURGE REQUESTED" : "READY";
        int cacheColor = !powered ? METAL_GRAY
                : cooldown > 0 ? SIGNAL_GOLD : OFF_WHITE;
''', '''        String cacheState = !powered ? "UNAVAILABLE"
                : cooldown > 0 ? "REINDEX " + formatCooldown(cooldown)
                : resetRequested ? "PURGE REQUESTED"
                : data.unusualNetworkActivity() ? "REVIEW ADVISED" : "READY";
        int cacheColor = !powered ? METAL_GRAY
                : cooldown > 0 || data.unusualNetworkActivity()
                        ? SIGNAL_GOLD : OFF_WHITE;
''', "operations cache advisory")

text = replace_once(text, '''        centeredBody(graphics,
                "WARNING // PURGE TERMINATES ACTIVE REMOTE MAINTENANCE SESSIONS",
                x + 8, y + 66, width - 16, 10, FOUNDATION_RED);
        centeredBody(graphics,
                "CURRENT ACCESS TOKENS WILL BE INVALIDATED.",
                x + 8, y + 78, width - 16, 10, MUTED_BLUE);
''', '''        String warningLineOne = cooldown > 0
                ? "REINDEX ACTIVE // TELEMETRY IS READING SLOWLY"
                : "PURGE TERMINATES REMOTE TECHNICIAN SESSIONS";
        String warningLineTwo = cooldown > 0
                ? "ACCESS TOKENS INVALID // EST. " + formatCooldown(cooldown)
                : "FACILITY INDEX REBUILD REQUIRES APPROX. 05:00";
        centeredCaption(graphics, warningLineOne, resetX, y + 47,
                resetWidth, 9, cooldown > 0 ? SIGNAL_GOLD : FOUNDATION_RED);
        centeredCaption(graphics, warningLineTwo, resetX, y + 58,
                resetWidth, 9, MUTED_BLUE);
''', "compact purge disclaimer")

log_pattern = re.compile(r'''    private void renderSystemLog\(GuiGraphics graphics, int x, int y,
            int width, boolean powered\) \{.*?
    \}

    private void renderFooter''', re.S)
log_replacement = '''    private void renderSystemLog(GuiGraphics graphics, int x, int y,
            int width, boolean powered) {
        int height = 88;
        graphics.fill(x, y, x + width, y + height, 0xB5142B38);
        border(graphics, x, y, width, height, DIM_BLUE);
        drawHeading(graphics, "SYSTEM LOG", x + 9, y + 7, METAL_GRAY);
        rightAligned(graphics, body("BUFFER 03"), x + width - 9,
                y + 7, MUTED_BLUE);
        graphics.fill(x + 8, y + 20, x + width - 8, y + 21, DIM_BLUE);

        int cooldown = cooldownRemainingTicks();
        int reindex = reindexProgressPercent();
        if (!powered) {
            logLine(graphics, "PWR/01", "AUXILIARY POWER BUS OFFLINE",
                    x + 9, y + 28, FOUNDATION_RED);
            logLine(graphics, "NET/12", "LIVE FACILITY TELEMETRY SUSPENDED",
                    x + 9, y + 43, METAL_GRAY);
            logLine(graphics, "SEC/07", "SESSION CACHE CONTROL UNAVAILABLE",
                    x + 9, y + 58, METAL_GRAY);
        } else if (cooldown > 0) {
            logLine(graphics, "IDX/03", "FACILITY INDEX REBUILD IN PROGRESS",
                    x + 9, y + 28, SIGNAL_GOLD);
            logLine(graphics, "TEL/06",
                    "TELEMETRY CHANNELS READING // " + reindex + "%",
                    x + 9, y + 43, METAL_GRAY);
            logLine(graphics, "SEC/07",
                    "REMOTE TECHNICIAN TOKENS INVALIDATED",
                    x + 9, y + 58, METAL_GRAY);
        } else if (data.unusualNetworkActivity()) {
            logLine(graphics, "SYS/00", "FACILITY SNAPSHOT ACQUIRED",
                    x + 9, y + 28, OFF_WHITE);
            logLine(graphics, "NET/47", "UNUSUAL NETWORK ACTIVITY DETECTED",
                    x + 9, y + 43, SIGNAL_GOLD);
            logLine(graphics, "SEC/12",
                    "CONTACT SITE NETWORK INTEGRITY FOR REVIEW",
                    x + 9, y + 58, METAL_GRAY);
        } else {
            logLine(graphics, "SYS/00", "FACILITY SNAPSHOT ACQUIRED",
                    x + 9, y + 28, OFF_WHITE);
            logLine(graphics, "NET/12", "ENDPOINT REGISTRY SYNCHRONIZED",
                    x + 9, y + 43, METAL_GRAY);
            logLine(graphics, "SEC/07", "REMOTE SESSION CACHE READY",
                    x + 9, y + 58, METAL_GRAY);
        }

        Component prompt = body("ARC48:SCIPNET>");
        graphics.drawString(font, prompt, x + 9, y + 74, MUTED_BLUE, false);
        if ((Util.getMillis() / 500L) % 2L == 0L) {
            int cursorX = x + 13 + font.width(prompt);
            graphics.fill(cursorX, y + 75, cursorX + 5, y + 83,
                    METAL_GRAY);
        }
    }

    private void renderFooter'''
text, count = log_pattern.subn(log_replacement, text, count=1)
if count != 1:
    raise RuntimeError(f"system log rewrite: expected 1 match, found {count}")

text = replace_once(text, '''    private void drawBody(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, body(text), x, y, color, false);
    }
''', '''    private void drawBody(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, body(text), x, y, color, false);
    }

    private void centeredCaption(GuiGraphics graphics, String text, int x,
            int y, int width, int height, int color) {
        Component component = caption(text);
        graphics.drawString(font, component,
                x + Math.max(0, (width - font.width(component)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2),
                color, false);
    }
''', "caption renderer")
text = replace_once(text, '''    private static Component heading(String text) {
        return ScpFonts.montserrat(text);
    }

    private int cooldownRemainingTicks() {
''', '''    private static Component caption(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(TERMINAL_CAPTION));
    }

    private static Component heading(String text) {
        return ScpFonts.montserrat(text);
    }

    private int reindexProgressPercent() {
        int remaining = cooldownRemainingTicks();
        if (remaining <= 0) return 100;
        double completed = 1.0D - Math.min(1.0D,
                remaining / (double) CACHE_PURGE_TOTAL_TICKS);
        return Math.max(0, Math.min(99,
                (int) Math.floor(completed * 100.0D)));
    }

    private int cooldownRemainingTicks() {
''', "caption component and reindex progress")
write(path, text)

# Dedicated small-but-high-oversample caption font.
caption_path = ROOT / "src/main/resources/assets/scp_additions/font/scipnet_caption.json"
caption_path.write_text('''{
  "providers": [
    {
      "type": "ttf",
      "file": "scp_additions:titillium_web_regular.ttf",
      "shift": [0, 1],
      "size": 9.5,
      "oversample": 8.0
    },
    {
      "type": "reference",
      "id": "minecraft:default"
    }
  ]
}
''', encoding="utf-8")

# Changelog note.
path = "CHANGELOG.md"
text = read(path)
marker = "# Changelog\n"
entry = '''# Changelog

## SCP-079 auxiliary isolation and SCiPNET reindexing

- Auxiliary power isolation now suspends SCP-079 actions and drains AP toward 25 without erasing learned facility access;
- Remote-session cache purge is now the sole operation that clears learned access and forces a five-minute SCiPNET index rebuild;
- Added gradual reconstruction telemetry, a compact technician-session warning, and a subtle unusual-network-activity advisory after SCP-079 gains access.
'''
if text.startswith(marker):
    text = entry + text[len(marker):]
else:
    raise RuntimeError("CHANGELOG header not found")
write(path, text)

print("SCP-079 persistent access and SCiPNET reindex patch applied")
