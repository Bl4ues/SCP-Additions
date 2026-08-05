from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def patch(path, old, new, label):
    file = ROOT / path
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 match, found {count}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")

patch(
    "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java",
    '''        boolean unusualNetworkActivity = data.facilityAccess()
                && !data.hosts().isEmpty();
''',
    '''        // A learned remote session remains cached even while its physical
        // host is absent; only an explicit terminal purge clears the advisory.
        boolean unusualNetworkActivity = data.facilityAccess();
''',
    "persistent network advisory")

patch(
    "src/main/java/net/mcreator/scpadditions/block/Scp079AuxiliaryPowerBlock.java",
    '''        tooltip.add(Component.literal(
                "Isolation removes SCP-079 access but disables those systems."));
''',
    '''        tooltip.add(Component.literal(
                "Isolation suspends remote control but preserves cached access."));
''',
    "auxiliary tooltip")

patch(
    "src/main/java/net/mcreator/scpadditions/client/FacilityDiagnosticsScreen.java",
    '''        String warningLineOne = cooldown > 0
                ? "REINDEX ACTIVE // TELEMETRY IS READING SLOWLY"
                : "PURGE TERMINATES REMOTE TECHNICIAN SESSIONS";
        String warningLineTwo = cooldown > 0
                ? "ACCESS TOKENS INVALID // EST. " + formatCooldown(cooldown)
                : "FACILITY INDEX REBUILD REQUIRES APPROX. 05:00";
''',
    '''        String warningLineOne = cooldown > 0
                ? "REINDEX ACTIVE // TELEMETRY READING SLOWLY"
                : "WARNING // REMOTE TECHNICIAN SESSIONS END";
        String warningLineTwo = cooldown > 0
                ? "REMOTE TOKENS INVALID // EST. " + formatCooldown(cooldown)
                : "INDEX REBUILD REQUIRES APPROX. 05:00";
''',
    "compact disclaimer copy")

print("SCP-079 persistence refinements applied")
