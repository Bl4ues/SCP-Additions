package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.LevelAccessor;

public final class DeconOpenUpdateTickProcedure {
    private DeconOpenUpdateTickProcedure() {
    }

    public static void execute(LevelAccessor world, double x, double y, double z) {
        DecontaminationCheckpointController.scanOpen(world, x, y, z);
    }
}
