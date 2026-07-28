package net.mcreator.scpadditions.facility.elevator;

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

/**
 * Shared, renderer-independent primitives for the Core Room elevator.
 *
 * <p>The actual controller block entity, moving carriage entity, renderer and
 * collision solver are intentionally kept out of this class. This file defines
 * the contracts they will share so the implementation can be built without
 * coupling floor discovery, motion timing and authored model names to client
 * classes.</p>
 */
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

    /** Server-authoritative lifecycle of one carriage. */
    public enum Phase {
        IDLE_OPEN,
        DOOR_CLOSING,
        READY_TO_MOVE,
        MOVING,
        LEVELING,
        DOOR_OPENING,
        FAULT;

        public boolean doorsMayBeOpen() {
            return this == IDLE_OPEN || this == DOOR_OPENING;
        }

        public boolean acceptsMovementRequest() {
            return this == IDLE_OPEN;
        }
    }

    /** Stable action identifiers used by contextual interaction rules. */
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

    /**
     * One declared stop. {@code cabinY} is the exact world-space Y coordinate
     * of the carriage floor when it is level with this marker.
     */
    public record FloorStop(BlockPos markerPos, int cabinY, String label) {
        public FloorStop {
            markerPos = Objects.requireNonNull(markerPos, "markerPos").immutable();
            label = label == null ? "" : label.trim();
        }
    }

    /** Immutable, bottom-to-top floor map produced by the controller scan. */
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
                                    + " must be below the top controller at "
                                    + controllerPos.getY());
                }
            }
            this.floors = List.copyOf(sorted);
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
            for (int i = 0; i < floors.size(); i++) {
                if (Math.abs(floors.get(i).cabinY() - cabinY) <= accepted) {
                    return OptionalInt.of(i);
                }
            }
            return OptionalInt.empty();
        }

        public int nearestIndex(double cabinY) {
            int nearest = 0;
            double nearestDistance = Double.MAX_VALUE;
            for (int i = 0; i < floors.size(); i++) {
                double distance = Math.abs(floors.get(i).cabinY() - cabinY);
                if (distance < nearestDistance) {
                    nearest = i;
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
     * Acceleration-limited vertical movement. It produces a triangular profile
     * for short trips and a trapezoidal profile for longer ones, avoiding the
     * floaty whole-trip easing produced by a simple smoothstep.
     */
    public record MotionPlan(double startY, double endY, double acceleration,
            double peakSpeed, double accelerationTicks, double cruiseTicks,
            double totalTicks) {

        public MotionPlan {
            if (!Double.isFinite(startY) || !Double.isFinite(endY)
                    || !Double.isFinite(acceleration)
                    || !Double.isFinite(peakSpeed)
                    || !Double.isFinite(accelerationTicks)
                    || !Double.isFinite(cruiseTicks)
                    || !Double.isFinite(totalTicks)) {
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
            double distanceWhileAccelerating = 0.5D * acceleration
                    * timeToMaximum * timeToMaximum;
            double accelerationTicks;
            double cruiseTicks;
            double peakSpeed;

            if (distanceWhileAccelerating * 2.0D >= distance) {
                accelerationTicks = Math.sqrt(distance / acceleration);
                cruiseTicks = 0.0D;
                peakSpeed = acceleration * accelerationTicks;
            } else {
                accelerationTicks = timeToMaximum;
                peakSpeed = maximumSpeed;
                double cruiseDistance = distance
                        - (distanceWhileAccelerating * 2.0D);
                cruiseTicks = cruiseDistance / peakSpeed;
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

    /** Bone/locator names shared by the Blockbench file and renderer. */
    public static final class ModelBones {
        public static final String ROOT = "root";
        public static final String CABIN = "cabin";
        public static final String SHELL = "shell";
        public static final String FLOOR = "floor";
        public static final String CEILING = "ceiling";
        public static final String DOOR_LEFT = "door_left";
        public static final String DOOR_RIGHT = "door_right";
        public static final String BUTTON_UP = "button_up";
        public static final String BUTTON_DOWN = "button_down";
        public static final String BUTTON_UP_LIGHT = "button_up_light";
        public static final String BUTTON_DOWN_LIGHT = "button_down_light";
        public static final String CABLE_MOUNT_LEFT = "cable_mount_left";
        public static final String CABLE_MOUNT_RIGHT = "cable_mount_right";
        public static final String GUIDE_MOUNT_LEFT = "guide_mount_left";
        public static final String GUIDE_MOUNT_RIGHT = "guide_mount_right";

        private ModelBones() {
        }
    }

    private static double clamp(double value, double minimum,
            double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
