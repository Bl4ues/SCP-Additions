from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

def replace(path: str, old: str, new: str, count: int = -1) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Missing replacement anchor in {path}: {old[:120]!r}")
    target.write_text(text.replace(old, new, count), encoding="utf-8")

manager = "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java"
replace(manager,
        "import net.minecraft.world.entity.Entity;\n",
        "")
replace(manager,
        "import net.mcreator.scpadditions.entity.Scp106Entity;\nimport net.mcreator.scpadditions.entity.Scp173Entity;\n",
        "import net.mcreator.scpadditions.roamer.RoamerManager;\nimport net.mcreator.scpadditions.roamer.RoamerType;\n")
replace(manager,
        "if (data.auxiliaryPowerOnline() && !data.hosts().isEmpty()\n                && !data.facilityAccess()) {\n            data.setProtocolExposed(true);\n            addDiscovery(server, data, SCAN_DISCOVERY);\n        }",
        "if (data.auxiliaryPowerOnline() && !data.hosts().isEmpty()\n                && !data.facilityAccess() && !data.protocolExposed()) {\n            data.setProtocolExposed(true);\n            addDiscovery(server, data, SCAN_DISCOVERY);\n        }")
replace(manager,
        "if (data.facilityAccess()\n                && (!data.auxiliaryPowerOnline() || data.hosts().isEmpty())) {\n            resetCompromise(server, data);\n            return;\n        }",
        "if (!data.auxiliaryPowerOnline()\n                || (data.hosts().isEmpty()\n                && (data.protocolExposed() || data.facilityAccess()\n                || data.discoveryProgress() > 0.0D))) {\n            if (data.protocolExposed() || data.facilityAccess()\n                    || data.discoveryProgress() > 0.0D) {\n                resetCompromise(server, data);\n            }\n            return;\n        }")
replace(manager,
        "    private static int countUncontainedScps(MinecraftServer server) {\n        int count = 0;\n        for (ServerLevel level : server.getAllLevels()) {\n            for (Entity entity : level.getAllEntities()) {\n                if (entity.isAlive() && (entity instanceof Scp173Entity\n                        || entity instanceof Scp106Entity)) {\n                    count++;\n                }\n            }\n        }\n        return count;\n    }",
        "    private static int countUncontainedScps(MinecraftServer server) {\n        int count = 0;\n        for (RoamerType type : RoamerType.values()) {\n            if (RoamerManager.hasActive(server, type)\n                    && !RoamerManager.isContained(server, type)) {\n                count++;\n            }\n        }\n        return count;\n    }")
replace(manager,
        "if (data.hosts().isEmpty() && data.facilityAccess()) {\n            resetCompromise(server, data);\n        }",
        "if (data.hosts().isEmpty()\n                && (data.protocolExposed() || data.facilityAccess()\n                || data.discoveryProgress() > 0.0D)) {\n            resetCompromise(server, data);\n        }")

facility = "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java"
replace(facility,
        "                case CLOSED -> {\n                    if (doorPowered(level, pos)) startOpening(level, pos, state, family);\n                }",
        "                case CLOSED -> {\n                    if (doorPowered(level, pos)) {\n                        startOpening(level, pos, state, family);\n                        Scp079FacilityAccessManager.recordActivity(level,\n                                Scp079FacilityAccessManager.Activity.DOOR_OPENED);\n                    }\n                }")

buttons = "src/main/java/net/mcreator/scpadditions/facility/DoorButtonIndependentInteractionEvents.java"
replace(buttons,
        "\n        if (playerInitiated && phase == Phase.CLOSED) {\n            Scp079FacilityAccessManager.recordActivity(level,\n                    Scp079FacilityAccessManager.Activity.DOOR_OPENED);\n        }\n",
        "\n")

scp012_door = "src/main/java/net/mcreator/scpadditions/scp012/Scp012DoorAccess.java"
replace(scp012_door,
        "import net.mcreator.scpadditions.facility.Scp079DecisionLog;",
        "import net.mcreator.scpadditions.facility.Scp079DecisionLog;\nimport net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;")
replace(scp012_door,
        "                \"attempt \" + attempts + \" for \"\n                        + player.getGameProfile().getName()\n                        + \" · contest total \" + Math.round(spent) + \" AP\");\n        return true;",
        "                \"attempt \" + attempts + \" for \"\n                        + player.getGameProfile().getName()\n                        + \" · contest total \" + Math.round(spent) + \" AP\");\n        Scp079FacilityAccessManager.awardFirstInterference(player);\n        return true;")

scp012 = "src/main/java/net/mcreator/scpadditions/scp012/Scp012InfluenceEvents.java"
replace(scp012,
        "                                    + player.getGameProfile().getName()\n                                    + protection);",
        "                                    + player.getGameProfile().getName()\n                                    + protection);\n                    Scp079FacilityAccessManager.awardFirstInterference(player);")

tesla = "src/main/java/net/mcreator/scpadditions/facility/Scp079TeslaSuppression.java"
replace(tesla,
        "                        + \" pursuing \" + targetName + \" · \" + mode\n                        + \" · \" + SUPPRESSION_TICKS / 20.0D + \"s\");\n        return true;",
        "                        + \" pursuing \" + targetName + \" · \" + mode\n                        + \" · \" + SUPPRESSION_TICKS / 20.0D + \"s\");\n        if (target != null) {\n            Scp079FacilityAccessManager.awardFirstInterference(target);\n        }\n        return true;")

for path in [
    ROOT / ".github/workflows/polish-scp079-facility-discovery.yml",
    ROOT / "tools/hotfix/POLISH_SCP079_FACILITY_DISCOVERY",
    ROOT / "tools/hotfix/polish_scp079_facility_discovery.py",
]:
    if path.exists():
        path.unlink()
