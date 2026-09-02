package com.bl4ues.scpclassifieddirective.roamer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;

import java.util.Locale;

/** Central registry for SCP entities that own a recurring spawn cycle. */
public enum RoamerType {
    SCP_173("scp173", "SCP-173", true),
    SCP_106("scp106", "SCP-106", true),
    SCP_939("scp939", "SCP-939", true);

    private final String commandId;
    private final String displayName;
    private final boolean spawnImplemented;

    RoamerType(String commandId, String displayName,
            boolean spawnImplemented) {
        this.commandId = commandId;
        this.displayName = displayName;
        this.spawnImplemented = spawnImplemented;
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

    public GameRules.Key<GameRules.BooleanValue> spawnRule() {
        return switch (this) {
            case SCP_173 -> ScpClassifiedDirectiveModGameRules.SCP_173_SPAWN;
            case SCP_106 -> ScpClassifiedDirectiveModGameRules.SCP_106_SPAWN;
            case SCP_939 -> ScpClassifiedDirectiveModGameRules.SCP_939_SPAWN;
        };
    }

    public boolean matches(Entity entity) {
        return switch (this) {
            case SCP_173 -> entity instanceof Scp173Entity;
            case SCP_106 -> entity instanceof Scp106Entity;
            case SCP_939 -> entity instanceof Scp939Entity;
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
