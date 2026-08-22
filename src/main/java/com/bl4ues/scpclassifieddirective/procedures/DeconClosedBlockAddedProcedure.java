package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.LevelAccessor;

public final class DeconClosedBlockAddedProcedure {
    private DeconClosedBlockAddedProcedure() {
    }

    public static void execute(LevelAccessor world, double x, double y, double z) {
        DecontaminationCheckpointController.beginClosed(world, x, y, z);
    }
}
