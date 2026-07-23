package net.mcreator.scpadditions.scp012;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

/** Lightweight A* used only inside SCP-012's ten-block influence radius. */
public final class Scp012Pathfinder {
    private static final int MAX_VISITED = 700;
    private static final int CACHE_TICKS = 12;
    private static final Map<UUID, CachedPath> CACHE = new HashMap<>();
    private static final Map<UUID, CachedReachability> REACHABILITY = new HashMap<>();

    private Scp012Pathfinder() {
    }

    public static boolean hasOpenPath(ServerLevel level,
            ServerPlayer player, Vec3 goal) {
        if (directlyReachable(level, player, goal)) return true;

        UUID id = player.getUUID();
        BlockPos goalPos = BlockPos.containing(goal.x, goal.y, goal.z);
        long time = level.getGameTime();
        CachedReachability cached = REACHABILITY.get(id);
        if (cached != null && cached.expiresAt >= time
  && cached.goal.equals(goalPos)) {
            return cached.reachable;
        }

        List<BlockPos> path = find(level, player, goalPos);
        boolean reachable = !path.isEmpty()
  && path.get(path.size() - 1).distManhattan(goalPos) <= 1;
        REACHABILITY.put(id, new CachedReachability(
  goalPos, reachable, time + 6));
        return reachable;
    }

    public static Vec3 nextWaypoint(ServerLevel level, ServerPlayer player,
                                    Vec3 goal) {
        if (directlyReachable(level, player, goal)) return goal;

        BlockPos goalPos = BlockPos.containing(goal.x, goal.y, goal.z);
        CachedPath cached = CACHE.get(player.getUUID());
        long time = level.getGameTime();
        if (cached == null || cached.expiresAt < time
                || !cached.goal.equals(goalPos)
                || cached.nodes.isEmpty()) {
            List<BlockPos> path = find(level, player, goalPos);
            cached = new CachedPath(goalPos, path, time + CACHE_TICKS, 0);
            CACHE.put(player.getUUID(), cached);
        }

        int index = cached.index;
        while (index < cached.nodes.size()) {
            BlockPos node = cached.nodes.get(index);
            Vec3 point = new Vec3(node.getX() + 0.5D, node.getY(),
                    node.getZ() + 0.5D);
            if (player.position().distanceToSqr(point) > 0.30D) {
                CACHE.put(player.getUUID(), cached.withIndex(index));
                return point;
            }
            index++;
        }
        CACHE.remove(player.getUUID());
        return goal;
    }

    public static void clear(ServerPlayer player) {
        if (player == null) return;
        CACHE.remove(player.getUUID());
        REACHABILITY.remove(player.getUUID());
    }

    private static boolean directlyReachable(ServerLevel level,
                                             ServerPlayer player, Vec3 goal) {
        Vec3 start = player.position();
        AABB base = player.getBoundingBox();
        for (int step = 1; step <= 8; step++) {
            double t = step / 8.0D;
            double x = start.x + (goal.x - start.x) * t;
            double y = start.y + (goal.y - start.y) * t;
            double z = start.z + (goal.z - start.z) * t;
            AABB moved = base.move(x - player.getX(), y - player.getY(),
                    z - player.getZ());
            if (!level.noCollision(player, moved)) return false;
        }
        return true;
    }

    private static List<BlockPos> find(ServerLevel level, ServerPlayer player,
                                       BlockPos goal) {
        BlockPos start = player.blockPosition();
        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(node -> node.score));
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Map<BlockPos, Double> cost = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        open.add(new Node(start, heuristic(start, goal)));
        cost.put(start, 0.0D);
        BlockPos closest = start;
        double closestHeuristic = heuristic(start, goal);
        int visited = 0;

        while (!open.isEmpty() && visited++ < MAX_VISITED) {
            Node node = open.poll();
            BlockPos current = node.pos;
            if (!closed.add(current)) continue;

            double currentHeuristic = heuristic(current, goal);
            if (currentHeuristic < closestHeuristic) {
                closest = current;
                closestHeuristic = currentHeuristic;
            }
            if (current.distManhattan(goal) <= 1) {
                closest = current;
                break;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos horizontal = current.relative(direction);
                for (int vertical : new int[]{0, 1, -1}) {
                    BlockPos next = horizontal.offset(0, vertical, 0);
                    if (closed.contains(next) || !withinSearch(start, next)
                            || !walkable(level, player, next)) {
                        continue;
                    }
                    double newCost = cost.getOrDefault(current,
                            Double.MAX_VALUE) + 1.0D + Math.abs(vertical) * 0.35D;
                    if (newCost >= cost.getOrDefault(next,
                            Double.MAX_VALUE)) continue;
                    cost.put(next, newCost);
                    parent.put(next, current);
                    open.add(new Node(next, newCost + heuristic(next, goal)));
                }
            }
        }

        if (closest.equals(start)) return List.of();
        List<BlockPos> reversed = new ArrayList<>();
        BlockPos cursor = closest;
        while (!cursor.equals(start)) {
            reversed.add(cursor);
            cursor = parent.get(cursor);
            if (cursor == null) return List.of();
        }
        List<BlockPos> result = new ArrayList<>(reversed.size());
        for (int i = reversed.size() - 1; i >= 0; i--) {
            result.add(reversed.get(i));
        }
        return result;
    }

    private static boolean withinSearch(BlockPos start, BlockPos pos) {
        return Math.abs(pos.getX() - start.getX()) <= 12
                && Math.abs(pos.getZ() - start.getZ()) <= 12
                && Math.abs(pos.getY() - start.getY()) <= 4;
    }

    private static boolean walkable(ServerLevel level, ServerPlayer player,
                                    BlockPos pos) {
        AABB moved = player.getBoundingBox().move(
                pos.getX() + 0.5D - player.getX(),
                pos.getY() - player.getY(),
                pos.getZ() + 0.5D - player.getZ());
        if (!level.noCollision(player, moved)) return false;
        return !level.getBlockState(pos.below())
                .getCollisionShape(level, pos.below()).isEmpty();
    }

    private static double heuristic(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX())
                + Math.abs(first.getZ() - second.getZ())
                + Math.abs(first.getY() - second.getY()) * 0.75D;
    }

    private record Node(BlockPos pos, double score) {
    }

    private record CachedReachability(BlockPos goal, boolean reachable,
                                      long expiresAt) {
    }

    private record CachedPath(BlockPos goal, List<BlockPos> nodes,
                              long expiresAt, int index) {
        private CachedPath withIndex(int nextIndex) {
            return new CachedPath(goal, nodes, expiresAt, nextIndex);
        }
    }
}
