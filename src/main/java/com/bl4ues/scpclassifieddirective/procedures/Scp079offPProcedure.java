package com.bl4ues.scpclassifieddirective.procedures;

import com.bl4ues.scpclassifieddirective.facility.Scp079ScreenState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

/** The lit CRT now expires on a short action pulse instead of player gaze. */
public final class Scp079offPProcedure {
    private Scp079offPProcedure() {
    }

    public static void execute(LevelAccessor world, double x, double y,
            double z) {
        if (world instanceof ServerLevel level) {
            Scp079ScreenState.settle(level,
                    BlockPos.containing(x, y, z));
        }
    }
}
