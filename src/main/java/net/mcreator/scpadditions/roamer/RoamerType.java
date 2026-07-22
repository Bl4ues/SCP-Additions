package net.mcreator.scpadditions.roamer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.Locale;

/** Central registry for SCP entities that own a recurring spawn cycle. */
public enum RoamerType {
    SCP_173("scp173", "SCP-173", true, 6000, 6000),
    SCP_106("scp106", "SCP-106", true, 2400, 6000);

    private final String commandId;
    private final String displayName;
    private final boolean spawnImplemented;
    private final int initialSpawnDelayTicks;
    private final int spawnIntervalTicks;

    RoamerType(String commandId, String displayName,
            boolean spawnImplemented, int initialSpawnDelayTicks,
            int spawnIntervalTicks) {
        this.commandId = commandId;
        this.displayName = displayName;
        this.spawnImplemented = spawnImplemented;
        this.initialSpawnDelayTicks = initialSpawnDelayTicks;
        this.spawnIntervalTicks = spawnIntervalTicks;
    }

    public String commandId() {
        return commandId;
    }

    public String displayName() {
        return displayName;
    }

    public boolean spawnImplemented() {
        return spawnImplemented;
    }

    public int initialSpawnDelayTicks() {
        return initialSpawnDelayTicks;
    }

    public int spawnIntervalTicks() {
        return spawnIntervalTicks;
    }

    public GameRules.Key<GameRules.BooleanValue> spawnRule() {
        return switch (this) {
            case SCP_173 -> ScpAdditionsModGameRules.SCP_173_SPAWN;
            case SCP_106 -> ScpAdditionsModGameRules.SCP_106_SPAWN;
        };
    }

    public boolean matches(Entity entity) {
        return switch (this) {
            case SCP_173 -> entity instanceof Scp173Entity;
            case SCP_106 -> entity instanceof Scp106Entity;
        };
    }

    public static RoamerType fromCommandId(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace("-", "").replace("_", "");
        for (RoamerType type : values()) {
            if (type.commandId.equals(normalized)) return type;
        }
        return null;
    }

    public static RoamerType fromEntity(Entity entity) {
        if (entity == null) return null;
        for (RoamerType type : values()) {
            if (type.matches(entity)) return type;
        }
        return null;
    }
}
