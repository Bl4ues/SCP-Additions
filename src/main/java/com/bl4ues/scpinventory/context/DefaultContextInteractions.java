package com.bl4ues.scpinventory.context;

import com.bl4ues.scpinventory.ScpInventoryMod;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the authoritative bundled context interaction template. Keeping the
 * default in one JSON resource prevents the in-code fallback from drifting away
 * from the file shipped in the JAR.
 */
public final class DefaultContextInteractions {
    private static final String BUNDLED_CONFIG =
            "config/scpinventory/context_interactions.json";
    private static final String EMERGENCY_FALLBACK = """
            {
              "_comment": "Bundled context interaction defaults were unavailable.",
              "interactions": [],
              "examples": []
            }
            """;

    private DefaultContextInteractions() {
    }

    public static String loadBundledConfig() {
        try (InputStream stream = DefaultContextInteractions.class.getClassLoader()
                .getResourceAsStream(BUNDLED_CONFIG)) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error(
                    "Failed to read bundled context interaction defaults", exception);
        }
        return EMERGENCY_FALLBACK;
    }
}
