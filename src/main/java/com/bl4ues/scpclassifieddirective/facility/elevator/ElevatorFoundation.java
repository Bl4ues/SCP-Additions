package com.bl4ues.scpclassifieddirective.facility.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/** Shared server-side contracts and motion mathematics for Core Room elevators. */
public final class ElevatorFoundation {
    private static final double EPSILON = 1.0E-7D;

    private ElevatorFoundation() {
    }

    public enum TravelDirection {
        UP(1),
        DOWN(-1),
        NONE(0);

        private final int step;

        TravelDirection(int step) {
            this.step = step;
        }

        public int step() {
            return step;
        }

        public static TravelDirection between(double currentY, double targetY) {
            if (targetY > currentY + EPSILON) return UP;
            if (targetY < currentY - EPSILON) return DOWN;
            return NONE;
        }
    }

    /** Server-authoritative lifecycle of one moving carriage. */
    public enum Phase {
        IDLE_OPEN,
        DOOR_CLOSING,
        READY_TO_MOVE,
        MOVING,
        LEVELING,
        DOOR_OPENING,
        FAULT,
        STATION_CLOSING;

        public boolean doorsMayBeOpen() {
            return this == IDLE_OPEN || this == DOOR_OPENING;
        }

        public boolean acceptsMovementRequest() {
            return this == IDLE_OPEN;
        }
    }

    /** Stable sub-actions used by the two contextual interaction targets. */
    public enum ContextAction {
        UP("elevator_up", TravelDirection.UP),
        DOWN("elevator_down", TravelDirection.DOWN);

        private final String id;
        private final TravelDirection direction;

        ContextAction(String id, TravelDirection direction) {
            this.id = id;
            this.direction = direction;
        }

        public String id() {
            return id;
        }

        public TravelDirection direction() {
            return direction;
        }

        public static Optional<ContextAction> fromId(String id) {
            if (id == null || id.isBlank()) return Optional.empty();
            String normalized = id.trim().toLowerCase(Locale.ROOT);
            for (ContextAction action : values()) {
                if (action.id.equals(normalized)) return Optional.of(action);
            }
            return Optional.empty();
        }
    }

    /** One declared stop; cabinY is the exact world Y of the carriage floor. */
    public record FloorStop(BlockPos markerPos, int cabinY, String label) {
        public FloorStop {
            markerPos = Objects.requireNonNull(markerPos, "markerPos").immutable();
            label = label == null ? "" : label.trim();
        }
    }

    /** Immutable floor map ordered from bottom to top. */
    public static final class FloorLayout {
        private final BlockPos controllerPos;
        private final Direction facing;
        private final List<FloorStop> floors;

        public FloorLayout(BlockPos controllerPos, Direction facing,
                List<FloorStop> declaredFloors) {
            this.controllerPos = Objects.requireNonNull(controllerPos,
                    "controllerPos").immutable();
            this.facing = Objects.requireNonNull(facing, "facing");
            if (!facing.getAxis().isHorizontal()) {
                throw new IllegalArgumentException(
                        "Elevator facing must be horizontal");
            }
            if (declaredFloors == null || declaredFloors.isEmpty()) {
                throw new IllegalArgumentException(
                        "An elevator requires at least one floor marker");
            }

            List<FloorStop> sorted = new ArrayList<>(declaredFloors);
            sorted.sort(Comparator.comparingInt(FloorStop::cabinY));
            Set<Integer> occupiedHeights = new HashSet<>();
            for (FloorStop floor : sorted) {
                Objects.requireNonNull(floor, "floor");
                if (!occupiedHeights.add(floor.cabinY())) {
                    throw new IllegalArgumentException(
                            "Duplicate elevator floor height: " + floor.cabinY());
                }
                if (floor.cabinY() >= controllerPos.getY()) {
                    throw new IllegalArgumentException(
                            "Floor " + floor.cabinY()
                                    + " must be below top controller "
                                    + controllerPos.getY());
                }
            }
            floors = List.copyOf(sorted);
        }

        public BlockPos controllerPos() {
            return controllerPos;
        }

        public Direction facing() {
            return facing;
        }

        public List<FloorStop> floors() {
            return floors;
        }

        public int floorCount() {
            return floors.size();
        }

        public FloorStop floor(int index) {
            return floors.get(index);
        }

        public OptionalInt indexAt(double cabinY, double tolerance) {
            double accepted = Math.max(0.0D, tolerance);
            for (int index = 0; index < floors.size(); index++) {
                if (Math.abs(floors.get(index).cabinY() - cabinY) <= accepted) {
                    return OptionalInt.of(index);
                }
            }
            return OptionalInt.empty();
        }

        public int nearestIndex(double cabinY) {
            int nearest = 0;
            double nearestDistance = Double.MAX_VALUE;
            for (int index = 0; index < floors.size(); index++) {
                double distance = Math.abs(floors.get(index).cabinY() - cabinY);
                if (distance < nearestDistance) {
                    nearest = index;
                    nearestDistance = distance;
                }
            }
            return nearest;
        }

        public Optional<FloorStop> adjacent(int currentIndex,
                TravelDirection direction) {
            Objects.requireNonNull(direction, "direction");
            if (direction == TravelDirection.NONE) return Optional.empty();
            int target = currentIndex + direction.step();
            return target >= 0 && target < floors.size()
                    ? Optional.of(floors.get(target))
                    : Optional.empty();
        }
    }

    /**
     * Acceleration-limited vertical motion. Short trips use a triangular speed
     * profile; longer trips use acceleration, cruise and deceleration phases.
     */
    public record MotionPlan(double startY, double endY, double acceleration,
            double peakSpeed, double accelerationTicks, double cruiseTicks,
            double totalTicks) {

        public MotionPlan {
            if (!finite(startY, endY, acceleration, peakSpeed,
                    accelerationTicks, cruiseTicks, totalTicks)) {
                throw new IllegalArgumentException(
                        "Elevator motion values must be finite");
            }
            if (acceleration < 0.0D || peakSpeed < 0.0D
                    || accelerationTicks < 0.0D || cruiseTicks < 0.0D
                    || totalTicks < 0.0D) {
                throw new IllegalArgumentException(
                        "Elevator motion values cannot be negative");
            }
        }

        public static MotionPlan create(double startY, double endY,
                double maximumSpeed, double acceleration) {
            double distance = Math.abs(endY - startY);
            if (distance <= EPSILON) {
                return new MotionPlan(startY, endY, 0.0D, 0.0D,
                        0.0D, 0.0D, 0.0D);
            }
            if (!(maximumSpeed > 0.0D) || !(acceleration > 0.0D)
                    || !Double.isFinite(maximumSpeed)
                    || !Double.isFinite(acceleration)) {
                throw new IllegalArgumentException(
                        "Maximum speed and acceleration must be positive and finite");
            }

            double timeToMaximum = maximumSpeed / acceleration;
            double accelerationDistance = 0.5D * acceleration
                    * timeToMaximum * timeToMaximum;
            double accelerationTicks;
            double cruiseTicks;
            double peakSpeed;

            if (accelerationDistance * 2.0D >= distance) {
                accelerationTicks = Math.sqrt(distance / acceleration);
                cruiseTicks = 0.0D;
                peakSpeed = acceleration * accelerationTicks;
            } else {
                accelerationTicks = timeToMaximum;
                peakSpeed = maximumSpeed;
                cruiseTicks = (distance - accelerationDistance * 2.0D)
                        / peakSpeed;
            }

            return new MotionPlan(startY, endY, acceleration, peakSpeed,
                    accelerationTicks, cruiseTicks,
                    accelerationTicks * 2.0D + cruiseTicks);
        }

        public int durationTicks() {
            return (int) Math.ceil(totalTicks);
        }

        public MotionSample sample(double elapsedTicks) {
            double distance = Math.abs(endY - startY);
            if (distance <= EPSILON || totalTicks <= EPSILON) {
                return new MotionSample(endY, 0.0D, 1.0D, true);
            }

            double time = clamp(elapsedTicks, 0.0D, totalTicks);
            double accelerationDistance = 0.5D * acceleration
                    * accelerationTicks * accelerationTicks;
            double travelled;
            double speed;

            if (time < accelerationTicks) {
                travelled = 0.5D * acceleration * time * time;
                speed = acceleration * time;
            } else if (time < accelerationTicks + cruiseTicks) {
                double cruiseTime = time - accelerationTicks;
                travelled = accelerationDistance + peakSpeed * cruiseTime;
                speed = peakSpeed;
            } else {
                double remaining = totalTicks - time;
                travelled = distance
                        - 0.5D * acceleration * remaining * remaining;
                speed = acceleration * remaining;
            }

            double sign = Math.signum(endY - startY);
            double progress = clamp(travelled / distance, 0.0D, 1.0D);
            boolean complete = time >= totalTicks - EPSILON;
            return new MotionSample(startY + sign * travelled,
                    complete ? 0.0D : sign * speed, progress, complete);
        }
    }

    public record MotionSample(double positionY, double velocityY,
            double progress, boolean complete) {
    }

    private static boolean finite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) return false;
        }
        return true;
    }

    private static double clamp(double value, double minimum,
            double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
