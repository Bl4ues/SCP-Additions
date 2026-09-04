package com.bl4ues.scpclassifieddirective.procedures;

import com.bl4ues.scpclassifieddirective.facility.Scp079ScreenState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

/** Idle SCP-079 stays dark unless a playable local-host view holds it active. */
public final class Scp079onPProcedure {
    private Scp079onPProcedure() {
    }

    public static void execute(LevelAccessor world, double x, double y,
            double z) {
        if (world instanceof ServerLevel level) {
            Scp079ScreenState.refreshLocal(level,
                    BlockPos.containing(x, y, z));
        }
    }
}
