package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;

import java.util.Locale;

/** Shared floor/zone classification for playable SCP-079 camera travel. */
public final class Scp079CameraTravelRules {
    public static final double BASE_COST = 3.0D;

    private Scp079CameraTravelRules() { }

    /** 1 = same floor, 2 = another floor in the zone, 3 = another zone. */
    public static int multiplier(FacilityRoomSnapshot current,
            FacilityRoomSnapshot target) {
        if (current == null || target == null) return 1;
        if (current.id().equals(target.id())) return 1;
        if (differentZone(current, target)) return 3;
        if (differentFloor(current, target)) return 2;
        return 1;
    }

    public static double baseCost(FacilityRoomSnapshot current,
            FacilityRoomSnapshot target) {
        return BASE_COST * multiplier(current, target);
    }

    public static boolean differentFloor(FacilityRoomSnapshot current,
            FacilityRoomSnapshot target) {
        if (current == null || target == null) return false;
        String currentLabel = floorKey(current);
        String targetLabel = floorKey(target);
        if (!currentLabel.isBlank() || !targetLabel.isBlank()) {
            return !currentLabel.equals(targetLabel);
        }
        int currentY = current.patches().isEmpty()
                ? 0 : current.patches().get(0).y();
        int targetY = target.patches().isEmpty()
                ? 0 : target.patches().get(0).y();
        return Math.abs(currentY - targetY) > 3;
    }

    public static boolean differentZone(FacilityRoomSnapshot current,
            FacilityRoomSnapshot target) {
        if (current == null || target == null) return false;
        String a = zoneKey(current);
        String b = zoneKey(target);
        // Unknown authoring labels should not suddenly turn every floor change
        // into a cross-zone jump. In that case the floor tier remains authoritative.
        return !a.isBlank() && !b.isBlank() && !a.equals(b);
    }

    private static String floorKey(FacilityRoomSnapshot room) {
        return normalize(room.floorLongLabel()) + "\n"
                + normalize(room.floorShortLabel());
    }

    private static String zoneKey(FacilityRoomSnapshot room) {
        String longLabel = normalize(room.floorLongLabel());
        String shortLabel = normalize(room.floorShortLabel());
        String combined = longLabel + " " + shortLabel;

        if (combined.contains("light containment")
                || token(combined, "lcz")) return "lcz";
        if (combined.contains("heavy containment")
                || token(combined, "hcz")) return "hcz";
        if (combined.contains("entrance zone")
                || token(combined, "ez")) return "ez";
        if (combined.contains("surface")) return "surface";

        String candidate = !shortLabel.isBlank() ? shortLabel : longLabel;
        if (candidate.isBlank()) return "";
        int separator = candidate.indexOf(" - ");
        if (separator > 0) candidate = candidate.substring(0, separator).trim();
        // Floor-only labels such as "Sublevel 2" carry no zone information.
        if (candidate.startsWith("sublevel") || candidate.startsWith("sl")
                && candidate.length() > 2
                && Character.isDigit(candidate.charAt(2))) return "";
        return candidate;
    }

    private static boolean token(String value, String token) {
        int index = -1;
        while ((index = value.indexOf(token, index + 1)) >= 0) {
            boolean left = index == 0
                    || !Character.isLetterOrDigit(value.charAt(index - 1));
            int end = index + token.length();
            boolean right = end >= value.length()
                    || !Character.isLetterOrDigit(value.charAt(end));
            if (left && right) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? ""
                : value.strip().toLowerCase(Locale.ROOT)
                        .replaceAll("\\s+", " ");
    }
}
