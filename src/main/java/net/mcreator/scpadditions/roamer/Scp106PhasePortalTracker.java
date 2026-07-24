package net.mcreator.scpadditions.roamer;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.init.ScpAdditionsModParticleTypes;

import java.util.Map;
import java.util.WeakHashMap;

/** Tracks the real collision surfaces crossed by SCP-106 while phasing. */
public final class Scp106PhasePortalTracker {
    private static final Map<Scp106Entity, TrackingState> STATES =
            new WeakHashMap<>();

    private Scp106PhasePortalTracker() {
    }

    public static void tick(Scp106Entity entity, boolean phasing) {
        if (entity == null || !entity.level().isClientSide) return;
        if (!phasing) {
            STATES.remove(entity);
            return;
        }

        AABB currentBox = entity.getBoundingBox().deflate(0.025D);
        boolean insideSolid = intersectsSolid(entity, currentBox);
        TrackingState state = STATES.get(entity);
        if (state == null) {
            STATES.put(entity, new TrackingState(currentBox, insideSolid));
            return;
        }

        if (!state.insideSolid && insideSolid) {
            spawnSurfacePortal(entity, findSurface(entity,
                    state.previousBox, currentBox, true));
        } else if (state.insideSolid && !insideSolid) {
            spawnSurfacePortal(entity, findSurface(entity,
                    state.previousBox, currentBox, false));
        }

        state.previousBox = currentBox;
        state.insideSolid = insideSolid;
    }

    private static boolean intersectsSolid(Scp106Entity entity, AABB box) {
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX - 1.0E-7D);
        int maxY = Mth.floor(box.maxY - 1.0E-7D);
        int maxZ = Mth.floor(box.maxZ - 1.0E-7D);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    BlockState state = entity.level().getBlockState(mutable);
                    VoxelShape shape = state.getCollisionShape(
                            entity.level(), mutable);
                    for (AABB local : shape.toAabbs()) {
                        if (local.move(x, y, z).intersects(box)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static Surface findSurface(Scp106Entity entity,
            AABB previousBox, AABB currentBox, boolean entering) {
        Vec3 previousCenter = center(previousBox);
        Vec3 currentCenter = center(currentBox);
        Vec3 movement = currentCenter.subtract(previousCenter);
        if (movement.lengthSqr() < 1.0E-6D) {
            movement = entity.getDeltaMovement();
        }
        if (movement.lengthSqr() < 1.0E-6D) return null;

        AABB swept = new AABB(
                Math.min(previousBox.minX, currentBox.minX),
                Math.min(previousBox.minY, currentBox.minY),
                Math.min(previousBox.minZ, currentBox.minZ),
                Math.max(previousBox.maxX, currentBox.maxX),
                Math.max(previousBox.maxY, currentBox.maxY),
                Math.max(previousBox.maxZ, currentBox.maxZ)).inflate(0.08D);
        AABB relevantBox = entering ? currentBox : previousBox;
        Vec3 reference = entering ? currentCenter : previousCenter;

        int minX = Mth.floor(swept.minX);
        int minY = Mth.floor(swept.minY);
        int minZ = Mth.floor(swept.minZ);
        int maxX = Mth.floor(swept.maxX - 1.0E-7D);
        int maxY = Mth.floor(swept.maxY - 1.0E-7D);
        int maxZ = Mth.floor(swept.maxZ - 1.0E-7D);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Surface best = null;
        double bestScore = Double.MAX_VALUE;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    BlockState state = entity.level().getBlockState(mutable);
                    VoxelShape shape = state.getCollisionShape(
                            entity.level(), mutable);
                    for (AABB local : shape.toAabbs()) {
                        AABB worldShape = local.move(x, y, z);
                        if (!worldShape.intersects(swept)
                                || !worldShape.inflate(0.04D)
                                .intersects(relevantBox)) {
                            continue;
                        }
                        Surface candidate = surfaceFor(worldShape,
                                reference, movement, entering);
                        double score = candidate.position.distanceToSqr(reference);
                        if (score < bestScore) {
                            best = candidate;
                            bestScore = score;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static Surface surfaceFor(AABB shape, Vec3 reference,
            Vec3 movement, boolean entering) {
        double absX = Math.abs(movement.x);
        double absY = Math.abs(movement.y);
        double absZ = Math.abs(movement.z);
        Vec3 normal;
        double x = clamp(reference.x, shape.minX, shape.maxX);
        double y = clamp(reference.y, shape.minY, shape.maxY);
        double z = clamp(reference.z, shape.minZ, shape.maxZ);

        if (absX >= absY && absX >= absZ) {
            boolean positive = movement.x >= 0.0D;
            x = positive
                    ? (entering ? shape.minX : shape.maxX)
                    : (entering ? shape.maxX : shape.minX);
            normal = new Vec3(positive == entering ? -1.0D : 1.0D,
                    0.0D, 0.0D);
        } else if (absZ >= absY) {
            boolean positive = movement.z >= 0.0D;
            z = positive
                    ? (entering ? shape.minZ : shape.maxZ)
                    : (entering ? shape.maxZ : shape.minZ);
            normal = new Vec3(0.0D, 0.0D,
                    positive == entering ? -1.0D : 1.0D);
        } else {
            boolean positive = movement.y >= 0.0D;
            y = positive
                    ? (entering ? shape.minY : shape.maxY)
                    : (entering ? shape.maxY : shape.minY);
            normal = new Vec3(0.0D,
                    positive == entering ? -1.0D : 1.0D, 0.0D);
        }
        return new Surface(new Vec3(x, y, z), normal);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (maximum <= minimum) return (minimum + maximum) * 0.5D;
        return Mth.clamp(value, minimum + 0.01D, maximum - 0.01D);
    }

    private static Vec3 center(AABB box) {
        return new Vec3((box.minX + box.maxX) * 0.5D,
                (box.minY + box.maxY) * 0.5D,
                (box.minZ + box.maxZ) * 0.5D);
    }

    private static void spawnSurfacePortal(Scp106Entity entity,
            Surface surface) {
        if (surface == null) return;
        Vec3 encodedNormal = surface.normal.scale(0.55D);
        entity.level().addParticle(
                ScpAdditionsModParticleTypes.SCP_106_PORTAL.get(),
                surface.position.x, surface.position.y, surface.position.z,
                encodedNormal.x, encodedNormal.y, encodedNormal.z);
    }

    private static final class TrackingState {
        private AABB previousBox;
        private boolean insideSolid;

        private TrackingState(AABB previousBox, boolean insideSolid) {
            this.previousBox = previousBox;
            this.insideSolid = insideSolid;
        }
    }

    private record Surface(Vec3 position, Vec3 normal) {
    }
}
