package com.bl4ues.scpclassifieddirective.save;

import java.util.Locale;

/** Player-facing origin of the most recent effective respawn save. */
public enum SaveMethod {
    QUICK_SAVE("quicksave", "Quicksave"),
    CHECKPOINT("checkpoint", "Checkpoint"),
    RESPAWN_POINT("respawn_point", "Respawn Point"),
    WORLD_SPAWN("world_spawn", "World Spawn");

    private final String id;
    private final String displayName;

    SaveMethod(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static SaveMethod fromId(String raw) {
        if (raw == null || raw.isBlank()) return WORLD_SPAWN;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (SaveMethod method : values()) {
            if (method.id.equals(normalized)
                    || method.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return method;
            }
        }
        return RESPAWN_POINT;
    }
}
