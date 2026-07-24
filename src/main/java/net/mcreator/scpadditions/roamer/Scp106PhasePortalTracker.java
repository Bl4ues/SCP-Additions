package net.mcreator.scpadditions.roamer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
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

    public static void begin(Scp106Entity entity) {
        if (entity == null || entity.level().isClientSide
                || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB currentBox = entity.getBoundingBox().deflate(0.025D);
        Vec3 direction = entity.getDeltaMovement();
        LivingEntity target = entity.getTarget();
        if (target != null) {
            direction = target.position().subtract(entity.position());
        }
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = entity.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (direction.lengthSqr() < 1.0E-6D) return;

        direction = direction.normalize().scale(1.35D);
        AABB anticipatedBox = currentBox.move(direction);
        Surface entry = findSurface(entity, currentBox,
                anticipatedBox, true, false);
        if (entry != null) {
            spawnSurfacePortal(serverLevel, entry);
        }

        TrackingState state = new TrackingState(currentBox,
                intersectsSolid(entity, currentBox));
        state.lastEntryPortalTick = entity.tickCount;
        STATES.put(entity, state);
    }

    public static void tick(Scp106Entity entity, boolean phasing) {
        if (entity == null || entity.level().isClientSide
                || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
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

        Vec3 movement = center(currentBox).subtract(center(state.previousBox));
        if (movement.lengthSqr() < 1.0E-6D) {
            movement = entity.getDeltaMovement();
        }

        if (!state.insideSolid && insideSolid
                && entity.tickCount - state.lastEntryPortalTick > 2) {
            Surface entry = findSurface(entity,
                    state.previousBox, currentBox, true, true);
            if (entry != null) {
                spawnSurfacePortal(serverLevel, entry);
                state.lastEntryPortalTick = entity.tickCount;
            }
        } else if (state.insideSolid && !insideSolid
                && entity.tickCount - state.lastExitPortalTick > 2) {
            Surface exit = findSurface(entity,
                    state.previousBox, currentBox, false, true);
            if (exit != null) {
                spawnSurfacePortal(serverLevel, exit);
                state.lastExitPortalTick = entity.tickCount;
            }
        }

        if (movement.lengthSqr() > 1.0E-6D) {
            AABB anticipatedBox = currentBox.move(movement.scale(1.75D));
            boolean anticipatedInside = intersectsSolid(entity, anticipatedBox);

            if (!insideSolid && anticipatedInside
                    && entity.tickCount - state.lastEntryPortalTick > 2) {
                Surface entry = findSurface(entity,
                        currentBox, anticipatedBox, true, true);
                if (entry != null) {
                    spawnSurfacePortal(serverLevel, entry);
                    state.lastEntryPortalTick = entity.tickCount;
                }
            } else if (insideSolid && !anticipatedInside
                    && entity.tickCount - state.lastExitPortalTick > 2) {
                Surface exit = findSurface(entity,
                        currentBox, anticipatedBox, false, true);
                if (exit != null) {
                    spawnSurfacePortal(serverLevel, exit);
                    state.lastExitPortalTick = entity.tickCount;
                }
            } else if (!insideSolid && !anticipatedInside
                    && entity.tickCount - state.lastSweptPortalTick > 2) {
                Surface entry = findSurface(entity,
                        currentBox, anticipatedBox, true, false);
                Surface exit = findSurface(entity,
                        currentBox, anticipatedBox, false, false);
                if (entry != null && exit != null
                        && entry.position().distanceToSqr(exit.position())
                        > 0.0025D) {
                    spawnSurfacePortal(serverLevel, entry);
                    spawnSurfacePortal(serverLevel, exit);
                    state.lastSweptPortalTick = entity.tickCount;
                    state.lastEntryPortalTick = entity.tickCount;
                    state.lastExitPortalTick = entity.tickCount;
                }
            }
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
            AABB previousBox, AABB currentBox, boolean entering,
            boolean requireEndpointContact) {
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
        Vec3 reference = requireEndpointContact
                ? (entering ? currentCenter : previousCenter)
                : (entering ? previousCenter : currentCenter);

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
                        if (!worldShape.intersects(swept)) continue;
                        if (requireEndpointContact
                                && !worldShape.inflate(0.04D)
                                .intersects(relevantBox)) {
                            continue;
                        }
                        if (!requireEndpointContact
                                && (worldShape.inflate(0.01D)
                                .intersects(previousBox)
                                || worldShape.inflate(0.01D)
                                .intersects(currentBox))) {
                            continue;
                        }

                        Surface candidate = surfaceFor(worldShape,
                                reference, movement, entering);
                        double score = candidate.position()
                                .distanceToSqr(reference);
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

    private static void spawnSurfacePortal(ServerLevel level,
            Surface surface) {
        if (surface == null) return;
        Vec3 normal = surface.normal();
        Vec3 position = surface.position().add(normal.scale(0.045D));
        Vec3 encodedNormal = normal.scale(0.55D);
        level.sendParticles(
                ScpAdditionsModParticleTypes.SCP_106_PORTAL.get(),
                position.x, position.y, position.z,
                0, encodedNormal.x, encodedNormal.y, encodedNormal.z, 1.0D);
    }

    private static final class TrackingState {
        private AABB previousBox;
        private boolean insideSolid;
        private int lastEntryPortalTick = Integer.MIN_VALUE;
        private int lastExitPortalTick = Integer.MIN_VALUE;
        private int lastSweptPortalTick = Integer.MIN_VALUE;

        private TrackingState(AABB previousBox, boolean insideSolid) {
            this.previousBox = previousBox;
            this.insideSolid = insideSolid;
        }
    }

    private record Surface(Vec3 position, Vec3 normal) {
    }
}
