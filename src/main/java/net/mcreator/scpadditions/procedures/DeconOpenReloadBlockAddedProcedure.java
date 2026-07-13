package net.mcreator.scpadditions.procedures;

import net.minecraft.world.level.LevelAccessor;

public final class DeconOpenReloadBlockAddedProcedure {
    private DeconOpenReloadBlockAddedProcedure() {
    }

    public static void execute(LevelAccessor world, double x, double y, double z) {
        DecontaminationCheckpointController.finishReload(world, x, y, z);
    }
}
