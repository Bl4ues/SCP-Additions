package com.bl4ues.scpadditions.compat;

import java.util.function.Supplier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Compatibility wrapper for the removed Forge DistExecutor helper. */
public final class DistExecutor {
    private DistExecutor() {
    }

    public static void unsafeRunWhenOn(
            Dist dist,
            Supplier<? extends Supplier<? extends Runnable>> workSupplier) {
        if (FMLEnvironment.dist == dist) {
            Supplier<? extends Runnable> work = workSupplier.get();
            if (work != null) {
                Runnable runnable = work.get();
                if (runnable != null) runnable.run();
            }
        }
    }
}
