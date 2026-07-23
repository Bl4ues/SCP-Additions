package net.mcreator.scpadditions.facility;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent world storage for SCP-079's shared processing-power budget. */
final class Scp079ProcessingSavedData extends SavedData {
    private static final String DATA_NAME =
            "scp_additions_scp079_processing";
    private static final String POWER_TAG = "ProcessingPower";

    private double power = Scp079ProcessingManager.INITIAL_POWER;

    private Scp079ProcessingSavedData() {
    }

    static Scp079ProcessingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                Scp079ProcessingSavedData::load,
                Scp079ProcessingSavedData::new,
                DATA_NAME);
    }

    private static Scp079ProcessingSavedData load(CompoundTag tag) {
        Scp079ProcessingSavedData data = new Scp079ProcessingSavedData();
        if (tag.contains(POWER_TAG, Tag.TAG_ANY_NUMERIC)) {
            data.power = clamp(tag.getDouble(POWER_TAG));
        }
        return data;
    }

    double power() {
        return power;
    }

    void setPower(double value) {
        double clamped = clamp(value);
        if (Math.abs(clamped - power) < 0.000001D) return;
        power = clamped;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putDouble(POWER_TAG, power);
        return tag;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return Scp079ProcessingManager.INITIAL_POWER;
        }
        return Math.max(0.0D,
                Math.min(Scp079ProcessingManager.MAX_POWER, value));
    }
}
