package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Shared logical/vanilla redstone query for transformed construction. */
public final class TransformPowerQuery {
    private TransformPowerQuery() {
    }

    public static boolean powered(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return false;
        BlockPos base = BlockPos.containing(center);
        if (level.hasNeighborSignal(base) || level.hasNeighborSignal(base.above())) {
            return true;
        }
        for (net.minecraft.core.Direction direction
                : net.minecraft.core.Direction.values()) {
            if (level.hasNeighborSignal(base.relative(direction))) return true;
        }
        return hasNearbyTransformedSource(level, center, 1.35D);
    }

    private static boolean hasNearbyTransformedSource(ServerLevel level,
            Vec3 center, double range) {
        double maxDistanceSqr = range * range;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        var dimension = level.dimension().location();
        for (TransformGroup group : data.groups()) {
            if (!group.dimension().equals(dimension)) continue;
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (!source(entry.getValue())) continue;
                Vec3 source = group.cellCenter(entry.getKey());
                double distance = source.distanceToSqr(center);
                if (distance > 1.0E-5D && distance <= maxDistanceSqr) return true;
            }
        }
        for (ConstructionSurface surface : data.surfaces()) {
            if (!surface.dimension().equals(dimension)) continue;
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (!source(entry.getValue().state())) continue;
                ConstructionSurface.SurfaceSlot slot = entry.getKey();
                double u = (slot.column() + 0.5D) / surface.columns();
                double v = (slot.row() + 0.5D) / surface.rows();
                Vec3 source = surface.gridPoint(u, v)
                        .add(surface.gridNormal(u, v).scale(0.5D));
                double distance = source.distanceToSqr(center);
                if (distance > 1.0E-5D && distance <= maxDistanceSqr) return true;
            }
        }
        return false;
    }

    private static boolean source(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(Blocks.REDSTONE_BLOCK)) return true;
        return state.hasProperty(BlockStateProperties.POWERED)
                && state.getValue(BlockStateProperties.POWERED);
    }
}
