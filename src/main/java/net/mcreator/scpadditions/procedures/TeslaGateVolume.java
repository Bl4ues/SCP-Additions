package net.mcreator.scpadditions.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Shared physical footprint for Tesla Gate detection and lethal discharge. */
public final class TeslaGateVolume {
    private TeslaGateVolume() {
    }

    public static AABB at(double x, double y, double z) {
        BlockPos controller = BlockPos.containing(x, y, z);
        return new AABB(controller).inflate(1.0D);
    }

    public static boolean intersects(Entity entity, AABB volume) {
        return entity != null && entity.isAlive()
                && entity.getBoundingBox().intersects(volume);
    }
}
