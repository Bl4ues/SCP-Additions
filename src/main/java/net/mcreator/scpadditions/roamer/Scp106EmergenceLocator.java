package net.mcreator.scpadditions.roamer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.entity.Scp106Entity;

/** Finds grounded floor or wall exits for SCP-106 without altering terrain. */
public final class Scp106EmergenceLocator {
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    private static final double ENTITY_HALF_WIDTH = 0.45D;
    private static final double ENTITY_HEIGHT = 2.0D;
    private static final int INITIAL_ATTEMPTS = 96;
    private static final int AMBUSH_ATTEMPTS = 120;
    private static final int AMBUSH_ROOM_ATTEMPTS = 80;
    private static final int AMBUSH_FRONT_ATTEMPTS = 96;
    private static final byte VANISHING_STATE = 4;
    private static final double AMBUSH_HUNTER_SEARCH_RADIUS = 128.0D;
    private static final double AMBUSH_MOVEMENT_THRESHOLD_SQR = 0.0004D;
    private static final double AMBUSH_MIN_LEAD = 3.75D;
    private static final double AMBUSH_MAX_LEAD = 5.75D;
    private static final double AMBUSH_FORWARD_JITTER = 1.50D;
    private static final double AMBUSH_SIDE_SPREAD = 4.00D;

    private Scp106EmergenceLocator() {
    }

    public enum Emergence {
        GROUND,
        WALL
    }

    public record Placement(Vec3 position, float yaw, Emergence emergence) {
        public Placement {
            if (position == null) position = Vec3.ZERO;
            if (emergence == null) emergence = Emergence.GROUND;
        }
    }

    public static Placement findInitial(ServerLevel level, Player target,
            RandomSource random) {
        if (level == null || target == null || random == null) return null;

        int targetY = target.blockPosition().getY();
        for (int attempt = 0; attempt < INITIAL_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 2.25D + random.nextDouble() * 3.25D;
            Vec3 candidate = target.position().add(
                    Math.cos(angle) * distance, 0.0D,
                    Math.sin(angle) * distance);

            BlockPos standing = findStandingPosition(level,
                    Mth.floor(candidate.x), targetY,
                    Mth.floor(candidate.z), 2, 2);
            if (standing == null
                    || !sharesVisibleRoom(level, target, standing)) {
                continue;
            }

            boolean preferWall = random.nextFloat() < 0.65F;
            Placement wall = wallPlacement(level, standing, target, random);
            if (preferWall && wall != null) return wall;

            Placement ground = groundPlacement(standing, target);
            if (ground != null) return ground;
            if (wall != null) return wall;
        }
        return null;
    }

    public static Placement findAmbush(ServerLevel level, Player target,
            RandomSource random) {
        if (level == null || target == null || random == null) return null;

        Vec3 targetPosition = target.position();
        Vec3 hunterOrigin = findHunterOrigin(level, target);
        Vec3 travel = observedMovement(target);
        Vec3 awayFromHunter = hunterOrigin == null
                ? Vec3.ZERO
                : horizontal(targetPosition.subtract(hunterOrigin));

        // When instantaneous player movement is too small to be reliable, the
        // old hunter-to-target axis is a better prediction than camera facing.
        // This keeps walking backwards from making SCP-106 reappear behind the
        // route merely because the player is still looking at it.
        if (travel.lengthSqr() < AMBUSH_MOVEMENT_THRESHOLD_SQR
                && awayFromHunter.lengthSqr() > 0.0001D) {
            travel = awayFromHunter;
        }
        if (travel.lengthSqr() < 0.0001D) {
            travel = horizontal(target.getLookAngle());
        }
        if (travel.lengthSqr() < 0.0001D) {
            travel = new Vec3(0.0D, 0.0D, 1.0D);
        }

        if (awayFromHunter.lengthSqr() < 0.0001D) {
            awayFromHunter = travel;
        }

        double awayDot = travel.dot(awayFromHunter);
        double originWeight = awayDot >= 0.0D ? 0.35D : 0.12D;
        Vec3 interceptionDirection = horizontal(
                travel.add(awayFromHunter.scale(originWeight)));
        if (interceptionDirection.lengthSqr() < 0.0001D) {
            interceptionDirection = travel;
        }

        Vec3 right = new Vec3(-interceptionDirection.z, 0.0D,
                interceptionDirection.x);
        double hunterDistance = hunterOrigin == null ? 0.0D
                : Math.sqrt(targetPosition.distanceToSqr(hunterOrigin));
        double leadDistance = Mth.clamp(
                AMBUSH_MIN_LEAD + observedSpeed(target) * 8.0D
                        + hunterDistance * 0.04D,
                AMBUSH_MIN_LEAD, AMBUSH_MAX_LEAD);
        Vec3 predictedIntercept = targetPosition.add(
                interceptionDirection.scale(leadDistance));
        boolean movingAwayFromHunter = hunterOrigin != null && awayDot > 0.10D;
        int targetY = target.blockPosition().getY();

        for (int attempt = 0; attempt < AMBUSH_ATTEMPTS; attempt++) {
            Vec3 candidate;
            if (attempt < AMBUSH_FRONT_ATTEMPTS) {
                double forwardJitter = (random.nextDouble() - 0.5D)
                        * AMBUSH_FORWARD_JITTER * 2.0D;
                double side = (random.nextDouble() - 0.5D)
                        * AMBUSH_SIDE_SPREAD;
                candidate = predictedIntercept
                        .add(interceptionDirection.scale(forwardJitter))
                        .add(right.scale(side));
            } else {
                // Broader fallback cone, still ahead of actual travel instead
                // of reverting to an unrestricted circle around the player.
                double distance = 2.5D + random.nextDouble() * 3.0D;
                double side = (random.nextDouble() - 0.5D) * 6.5D;
                candidate = targetPosition
                        .add(travel.scale(distance))
                        .add(right.scale(side));
            }

            BlockPos standing = findStandingPosition(level,
                    Mth.floor(candidate.x), targetY, Mth.floor(candidate.z),
                    2, 3);
            if (standing == null) continue;

            Vec3 standingCenter = Vec3.atBottomCenterOf(standing);
            Vec3 fromTarget = horizontal(standingCenter.subtract(targetPosition));
            if (fromTarget.lengthSqr() < 0.0001D
                    || fromTarget.dot(travel) < 0.15D) {
                continue;
            }
            if (movingAwayFromHunter
                    && standingCenter.distanceToSqr(hunterOrigin)
                    <= targetPosition.distanceToSqr(hunterOrigin) + 0.75D) {
                continue;
            }
            if (attempt < AMBUSH_ROOM_ATTEMPTS
                    && !sharesVisibleRoom(level, target, standing)) {
                continue;
            }

            boolean preferWall = random.nextFloat() < 0.68F;
            Placement wall = wallPlacement(level, standing, target, random);
            if (preferWall && wall != null) return wall;

            Placement ground = groundPlacement(standing, target);
            if (ground != null) return ground;
            if (wall != null) return wall;
        }
        return null;
    }

    private static Vec3 findHunterOrigin(ServerLevel level, Player target) {
        AABB search = target.getBoundingBox().inflate(
                AMBUSH_HUNTER_SEARCH_RADIUS);
        Scp106Entity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Scp106Entity scp106 : level.getEntitiesOfClass(
                Scp106Entity.class, search,
                entity -> entity.isAlive()
                        && entity.getEncounterState() == VANISHING_STATE
                        && entity.getTarget() == target)) {
            double distance = scp106.distanceToSqr(target);
            if (distance < closestDistance) {
                closest = scp106;
                closestDistance = distance;
            }
        }
        return closest == null ? null : closest.position();
    }

    private static Vec3 observedMovement(Player target) {
        Vec3 displacement = new Vec3(target.getX() - target.xo, 0.0D,
                target.getZ() - target.zo);
        Vec3 velocity = new Vec3(target.getDeltaMovement().x, 0.0D,
                target.getDeltaMovement().z);

        if (displacement.lengthSqr() >= AMBUSH_MOVEMENT_THRESHOLD_SQR) {
            if (velocity.lengthSqr() >= AMBUSH_MOVEMENT_THRESHOLD_SQR
                    && displacement.dot(velocity) > 0.0D) {
                return horizontal(displacement.scale(0.75D)
                        .add(velocity.scale(0.25D)));
            }
            return horizontal(displacement);
        }
        return velocity.lengthSqr() >= AMBUSH_MOVEMENT_THRESHOLD_SQR
                ? horizontal(velocity) : Vec3.ZERO;
    }

    private static double observedSpeed(Player target) {
        double displacementX = target.getX() - target.xo;
        double displacementZ = target.getZ() - target.zo;
        double displacement = Math.sqrt(displacementX * displacementX
                + displacementZ * displacementZ);
        Vec3 velocity = target.getDeltaMovement();
        double velocityLength = Math.sqrt(velocity.x * velocity.x
                + velocity.z * velocity.z);
        return Math.max(displacement, velocityLength);
    }

    private static boolean sharesVisibleRoom(ServerLevel level,
            Player target, BlockPos standing) {
        Vec3 targetEye = target.getEyePosition();
        Vec3 emergenceCenter = Vec3.atBottomCenterOf(standing)
                .add(0.0D, 1.0D, 0.0D);
        Vec3 horizontal = emergenceCenter.subtract(targetEye)
                .multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() > 36.0D
                || Math.abs(emergenceCenter.y - targetEye.y) > 3.0D) {
            return false;
        }

        Vec3 perpendicular = horizontal.lengthSqr() < 0.0001D
                ? new Vec3(0.35D, 0.0D, 0.0D)
                : new Vec3(-horizontal.z, 0.0D, horizontal.x)
                        .normalize().scale(0.35D);
        int clearRays = 0;
        if (hasClearRoomRay(level, target, targetEye, emergenceCenter)) {
            clearRays++;
        }
        if (hasClearRoomRay(level, target,
                targetEye.add(perpendicular.scale(0.45D)),
                emergenceCenter.add(perpendicular))) {
            clearRays++;
        }
        if (hasClearRoomRay(level, target,
                targetEye.subtract(perpendicular.scale(0.45D)),
                emergenceCenter.subtract(perpendicular))) {
            clearRays++;
        }
        return clearRays >= 2;
    }

    private static boolean hasClearRoomRay(ServerLevel level, Player target,
            Vec3 start, Vec3 end) {
        HitResult obstruction = level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target));
        return obstruction.getType() == HitResult.Type.MISS;
    }

    private static Placement groundPlacement(BlockPos standing, Player target) {
        Vec3 position = Vec3.atBottomCenterOf(standing);
        Vec3 toTarget = target.position().subtract(position);
        return new Placement(position, yawFor(toTarget.x, toTarget.z),
                Emergence.GROUND);
    }

    private static Placement wallPlacement(ServerLevel level,
            BlockPos standing, Player target, RandomSource random) {
        Vec3 center = Vec3.atBottomCenterOf(standing);
        int start = random.nextInt(HORIZONTAL_DIRECTIONS.length);
        for (int offset = 0; offset < HORIZONTAL_DIRECTIONS.length; offset++) {
            Direction outward = HORIZONTAL_DIRECTIONS[
                    (start + offset) % HORIZONTAL_DIRECTIONS.length];
            BlockPos wallBase = standing.relative(outward.getOpposite());
            BlockPos wallUpper = wallBase.above();
            if (!hasCollision(level, wallBase)
                    || !hasCollision(level, wallUpper)) {
                continue;
            }

            Vec3 toTarget = target.position().subtract(center);
            double frontDot = toTarget.x * outward.getStepX()
                    + toTarget.z * outward.getStepZ();
            if (frontDot < -0.5D) continue;

            // Face the open side directly. The animation begins behind
            // the model on local +Z, so its initial pose remains inside the wall.
            float wallYaw = yawFor(outward.getStepX(), outward.getStepZ());
            return new Placement(center, wallYaw, Emergence.WALL);
        }
        return null;
    }

    private static BlockPos findStandingPosition(ServerLevel level, int x,
            int targetY, int z, int scanUp, int scanDown) {
        int maximum = Math.max(scanUp, scanDown);
        for (int offset = 0; offset <= maximum; offset++) {
            if (offset <= scanDown) {
                BlockPos down = new BlockPos(x, targetY - offset, z);
                if (isValidStandingPosition(level, down)) return down;
            }
            if (offset > 0 && offset <= scanUp) {
                BlockPos up = new BlockPos(x, targetY + offset, z);
                if (isValidStandingPosition(level, up)) return up;
            }
        }
        return null;
    }

    private static boolean isValidStandingPosition(ServerLevel level,
            BlockPos position) {
        if (!level.getWorldBorder().isWithinBounds(position)) return false;
        BlockPos floorPos = position.below();
        BlockState floor = level.getBlockState(floorPos);
        if (!floor.isFaceSturdy(level, floorPos, Direction.UP)) return false;
        if (!level.getFluidState(position).isEmpty()
                || !level.getFluidState(position.above()).isEmpty()) {
            return false;
        }

        double x = position.getX() + 0.5D;
        double y = position.getY();
        double z = position.getZ() + 0.5D;
        AABB box = new AABB(x - ENTITY_HALF_WIDTH, y,
                z - ENTITY_HALF_WIDTH, x + ENTITY_HALF_WIDTH,
                y + ENTITY_HEIGHT, z + ENTITY_HALF_WIDTH);
        return level.noCollision(box);
    }

    private static boolean hasCollision(ServerLevel level, BlockPos position) {
        return !level.getBlockState(position)
                .getCollisionShape(level, position).isEmpty();
    }

    private static Vec3 horizontal(Vec3 vector) {
        Vec3 horizontal = new Vec3(vector.x, 0.0D, vector.z);
        double length = horizontal.length();
        return length <= 0.0001D ? horizontal
                : horizontal.scale(1.0D / length);
    }

    private static float yawFor(double x, double z) {
        return (float) (Mth.atan2(z, x) * Mth.RAD_TO_DEG) - 90.0F;
    }
}
