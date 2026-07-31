package net.mcreator.scpadditions.facility.elevator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import java.util.Locale;

/** Per-station configuration for the animated elevator arrival title. */
public record ElevatorArrivalDisplayData(boolean enabled, Zone zone,
        String customZone, FloorType floorType, int floorNumber) {
    public static final int MAX_CUSTOM_ZONE_LENGTH = 48;
    public static final int MAX_FLOOR_NUMBER = 999;

    public static final ElevatorArrivalDisplayData NONE =
            new ElevatorArrivalDisplayData(false,
                    Zone.LIGHT_CONTAINMENT_ZONE, "",
                    FloorType.SUBLEVEL, 1);
    public static final ElevatorArrivalDisplayData EDITOR_DEFAULT =
            new ElevatorArrivalDisplayData(true,
                    Zone.LIGHT_CONTAINMENT_ZONE, "",
                    FloorType.SUBLEVEL, 1);

    public ElevatorArrivalDisplayData {
        zone = zone == null ? Zone.LIGHT_CONTAINMENT_ZONE : zone;
        floorType = floorType == null ? FloorType.SUBLEVEL : floorType;
        customZone = sanitize(customZone);
        floorNumber = Mth.clamp(floorNumber, 0, MAX_FLOOR_NUMBER);
        if (zone != Zone.CUSTOM) customZone = "";
    }

    public String sectorLabel() {
        String value = zone == Zone.CUSTOM ? customZone : zone.displayName();
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }

    public String floorTypeLabel() {
        return floorType.displayName().toUpperCase(Locale.ROOT);
    }

    public String floorNumberLabel() {
        return String.format(Locale.ROOT, "%02d", floorNumber);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Enabled", enabled);
        tag.putString("Zone", zone.name());
        tag.putString("CustomZone", customZone);
        tag.putString("FloorType", floorType.name());
        tag.putInt("FloorNumber", floorNumber);
        return tag;
    }

    public static ElevatorArrivalDisplayData load(CompoundTag tag) {
        if (tag == null || tag.isEmpty() || !tag.getBoolean("Enabled")) {
            return NONE;
        }
        return new ElevatorArrivalDisplayData(true,
                enumValue(Zone.class, tag.getString("Zone"),
                        Zone.LIGHT_CONTAINMENT_ZONE),
                tag.getString("CustomZone"),
                enumValue(FloorType.class, tag.getString("FloorType"),
                        FloorType.SUBLEVEL),
                tag.getInt("FloorNumber"));
    }

    public static void write(FriendlyByteBuf buffer,
            ElevatorArrivalDisplayData data) {
        ElevatorArrivalDisplayData clean = data == null ? NONE : data;
        buffer.writeBoolean(clean.enabled());
        buffer.writeEnum(clean.zone());
        buffer.writeUtf(clean.customZone(), MAX_CUSTOM_ZONE_LENGTH);
        buffer.writeEnum(clean.floorType());
        buffer.writeVarInt(clean.floorNumber());
    }

    public static ElevatorArrivalDisplayData read(FriendlyByteBuf buffer) {
        return new ElevatorArrivalDisplayData(buffer.readBoolean(),
                buffer.readEnum(Zone.class),
                buffer.readUtf(MAX_CUSTOM_ZONE_LENGTH),
                buffer.readEnum(FloorType.class),
                buffer.readVarInt());
    }

    private static String sanitize(String value) {
        String clean = value == null ? "" : value.strip()
                .replace('\n', ' ').replace('\r', ' ');
        if (clean.length() > MAX_CUSTOM_ZONE_LENGTH) {
            clean = clean.substring(0, MAX_CUSTOM_ZONE_LENGTH);
        }
        return clean;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type,
            String value, T fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public enum Zone {
        ENTRANCE_ZONE("Entrance Zone"),
        LIGHT_CONTAINMENT_ZONE("Light Containment Zone"),
        HEAVY_CONTAINMENT_ZONE("Heavy Containment Zone"),
        SUPER_HEAVY_CONTAINMENT_ZONE("Super Heavy Containment Zone"),
        CUSTOM("Custom");

        private final String displayName;

        Zone(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public enum FloorType {
        LEVEL("Level"),
        SUBLEVEL("Sublevel");

        private final String displayName;

        FloorType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }
}
