package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Common-side indirection for the client mirror of transformed proxy geometry.
 * Dedicated servers never reference client classes; the client installs a
 * provider after the first construction snapshot arrives.
 */
public final class TransformConstructionClientBridge {
    private static volatile Provider provider;

    private TransformConstructionClientBridge() {
    }

    public static void install(Provider next) {
        provider = next;
    }

    public static VoxelShape selection(BlockPos pos) {
        Provider current = provider;
        return current == null || pos == null ? Shapes.empty()
                : current.selection(pos);
    }

    public static VoxelShape collision(BlockPos pos) {
        Provider current = provider;
        return current == null || pos == null ? Shapes.empty()
                : current.collision(pos);
    }

    public static VoxelShape offGridCollision(BlockPos pos) {
        Provider current = provider;
        return current == null || pos == null ? Shapes.empty()
                : current.offGridCollision(pos);
    }

    public interface Provider {
        VoxelShape selection(BlockPos pos);

        VoxelShape collision(BlockPos pos);

        VoxelShape offGridCollision(BlockPos pos);
    }
}
