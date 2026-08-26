package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only procedural electrical effect; populated in the visual pass commit. */
public final class TeslaGateElectricity {
    private TeslaGateElectricity() {
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state) {
        // Intentionally empty until the gate geometry/volume migration is installed.
    }
}
