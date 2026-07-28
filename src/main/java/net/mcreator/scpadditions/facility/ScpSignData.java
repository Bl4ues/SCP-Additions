package net.mcreator.scpadditions.facility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Immutable, sanitized configuration stored by an SCP Sign Support. */
public record ScpSignData(String scpNumber,
        ContainmentClass containmentClass, String customContainmentClass,
        int clearanceLevel,
        AnomalyType anomalyType, String customAnomalyType,
        List<String> hazards) {
    public static final int MAX_SCP_NUMBER_LENGTH = 5;
    public static final int MAX_CONTAINMENT_CLASS_LENGTH = 20;
    public static final int MAX_ANOMALY_TYPE_LENGTH = 32;
    public static final int HAZARD_SLOTS = 3;

    public static final ScpSignData DEFAULT = new ScpSignData(
            "", ContainmentClass.SAFE, "", 1,
            AnomalyType.HARMLESS, "", List.of());

    public ScpSignData {
        scpNumber = digitsOnly(scpNumber, MAX_SCP_NUMBER_LENGTH);
        containmentClass = containmentClass == null
                ? ContainmentClass.SAFE : containmentClass;
        customContainmentClass = cleanText(customContainmentClass,
                MAX_CONTAINMENT_CLASS_LENGTH);
        clearanceLevel = Math.max(1, Math.min(6, clearanceLevel));
        anomalyType = anomalyType == null ? AnomalyType.HARMLESS : anomalyType;
        customAnomalyType = cleanText(customAnomalyType,
                MAX_ANOMALY_TYPE_LENGTH);
        hazards = ScpSignHazards.normalizeSlots(hazards);
    }

    public String scpLabel() {
        return scpNumber.isEmpty() ? "SCP-" : "SCP-" + scpNumber;
    }

    public String containmentLabel() {
        return containmentClass == ContainmentClass.CUSTOM
                ? fallback(customContainmentClass, "CUSTOM")
                : containmentClass.displayName();
    }

    public String anomalyLabel() {
        return anomalyType == AnomalyType.CUSTOM
                ? fallback(customAnomalyType, "CUSTOM ANOMALY")
                : anomalyType.displayName();
    }

    public ScpSignData withHazard(int slot, String id) {
        List<String> updated = new ArrayList<>(hazards);
        if (slot >= 0 && slot < HAZARD_SLOTS) updated.set(slot, id);
        return new ScpSignData(scpNumber, containmentClass,
                customContainmentClass, clearanceLevel, anomalyType,
                customAnomalyType, updated);
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String digitsOnly(String input, int maximum) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder result = new StringBuilder(maximum);
        for (int index = 0; index < input.length() && result.length() < maximum;
                index++) {
            char character = input.charAt(index);
            if (character >= '0' && character <= '9') result.append(character);
        }
        return result.toString();
    }

    private static String cleanText(String input, int maximum) {
        if (input == null || input.isBlank()) return "";
        StringBuilder result = new StringBuilder(maximum);
        input.codePoints().filter(codePoint -> !Character.isISOControl(codePoint))
                .map(Character::toUpperCase)
                .limit(maximum)
                .forEach(result::appendCodePoint);
        return result.toString().trim();
    }

    public enum ContainmentClass {
        SAFE("Safe"),
        EUCLID("Euclid"),
        KETER("Keter"),
        CUSTOM("Custom");

        private final String displayName;

        ContainmentClass(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName.toUpperCase(Locale.ROOT);
        }

        public static ContainmentClass parse(String value) {
            if (value == null) return SAFE;
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return SAFE;
            }
        }
    }

    public enum AnomalyType {
        HAZARDOUS("Hazardous Anomaly"),
        HARMLESS("Harmless Anomaly"),
        CUSTOM("Custom");

        private final String displayName;

        AnomalyType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName.toUpperCase(Locale.ROOT);
        }

        public static AnomalyType parse(String value) {
            if (value == null) return HARMLESS;
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return HARMLESS;
            }
        }
    }
}
