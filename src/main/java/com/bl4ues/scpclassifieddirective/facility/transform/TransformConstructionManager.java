package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface.SurfaceAttachment;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface.SurfaceSlot;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server authority and cached physical proxy index for transformed construction.
 * Geometry is authored in doubles; vanilla proxy cells only bridge collision,
 * selection, lighting and interaction back into Minecraft's integer block grid.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformConstructionManager {
    private static final int GROUP_SUBDIVISIONS = 3;
    private static final double SURFACE_SELECTION_THICKNESS = 0.055D;
    private static final int MAX_GROUP_CELLS = 16_384;
    private static final int MAX_SURFACE_SLOTS = 65_536;

    private static final Map<MinecraftServer, SpatialIndex> INDEXES =
            new WeakHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, PendingSurface>>
            PENDING_SURFACES = new WeakHashMap<>();

    private TransformConstructionManager() {
    }

    public static boolean canEdit(Player player) {
        return player != null && player.isCreative();
    }

    public static TransformGroup createGroup(ServerPlayer player, BlockPos clicked,
            Direction face) {
        if (!canEdit(player) || clicked == null || face == null
                || !(player.level() instanceof ServerLevel level)) return null;
        Vec3 origin = Vec3.atCenterOf(clicked.relative(face));
        TransformGroup group = TransformGroup.empty(level.dimension().location(),
                origin);
        TransformConstructionSavedData.get(level.getServer()).putGroup(group);
        refreshGroup(level.getServer(), group.id());
        player.displayClientMessage(Component.literal(
                "Off-grid grid created. Place a block on the green cell, or select it with the tool to transform it."),
                true);
        return group;
    }

    /** Compatibility entry point for older callers that only have a hit point. */
    public static TransformGroup createGroup(ServerPlayer player, Vec3 hit,
            Direction face) {
        if (hit == null || face == null) return null;
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        BlockPos clicked = BlockPos.containing(hit.subtract(
                normal.scale(1.0E-4D)));
        return createGroup(player, clicked, face);
    }

    public static void selectSurfacePoint(ServerPlayer player, Vec3 hit) {
        if (!canEdit(player) || hit == null
                || !(player.level() instanceof ServerLevel level)) return;
        Map<UUID, PendingSurface> pending = PENDING_SURFACES.computeIfAbsent(
                level.getServer(), ignored -> new HashMap<>());
        PendingSurface first = pending.remove(player.getUUID());
        if (first == null || !first.dimension().equals(level.dimension())) {
            pending.put(player.getUUID(), new PendingSurface(level.dimension(), hit));
            player.displayClientMessage(Component.literal(
                    "Surface baseline start selected. Right-click the baseline end."), true);
            return;
        }

        Vec3 end = new Vec3(hit.x, first.point().y, hit.z);
        if (end.distanceToSqr(first.point()) < 0.04D) {
            player.displayClientMessage(Component.literal(
                    "Surface baseline is too short."), true);
            return;
        }
        ConstructionSurface surface = ConstructionSurface.wall(
                level.dimension().location(), first.point(), end, 3.0D);
        if ((long) surface.columns() * surface.rows() > MAX_SURFACE_SLOTS) {
            player.displayClientMessage(Component.literal(
                    "Surface is too large."), true);
            return;
        }
        TransformConstructionSavedData.get(level.getServer()).putSurface(surface);
        refreshSurface(level.getServer(), surface.id());
        player.displayClientMessage(Component.literal(
                "Construction surface created at 3 blocks high. Select its handles with the Surface Construction Tool to reshape, tilt or bend it."),
                true);
    }

    public static void cancelSurface(ServerPlayer player) {
        if (!canEdit(player) || player.getServer() == null) return;
        Map<UUID, PendingSurface> pending = PENDING_SURFACES.get(player.getServer());
        boolean removed = pending != null && pending.remove(player.getUUID()) != null;
        player.displayClientMessage(Component.literal(removed
                ? "Surface baseline selection cancelled."
                : "No surface baseline selection is active."), true);
    }

    public static VoxelShape proxySelectionShape(BlockGetter level,
            BlockPos pos) {
        ProxyCell cell = proxyCell(level, pos);
        return cell == null ? Shapes.empty() : cell.selection();
    }

    public static VoxelShape proxyCollisionShape(BlockGetter level,
            BlockPos pos) {
        ProxyCell cell = proxyCell(level, pos);
        return cell == null ? Shapes.empty() : cell.collision();
    }

    /**
     * Group-only collision, including cells occupied by ordinary vanilla blocks.
     * This is a collision hot path: never build the spatial index from here.
     * Normal refresh/startup owns index construction; absent cache means no
     * transformed contribution for this query.
     */
    public static VoxelShape offGridCollisionShape(BlockGetter getter,
            BlockPos pos) {
        if (!(getter instanceof ServerLevel level) || pos == null) {
            return Shapes.empty();
        }
        SpatialIndex cached = INDEXES.get(level.getServer());
        if (cached == null) return Shapes.empty();
        ProxyCell cell = cached.cell(level.dimension().location(), pos);
        return cell == null ? Shapes.empty() : cell.groupCollision();
    }

    /**
     * Cached transformed collision from both rigid Off-Grid groups and curved
     * Surfaces. This is used from BlockState#getCollisionShape and therefore
     * must never construct/rebuild the spatial index on demand.
     */
    public static VoxelShape transformedCollisionShape(BlockGetter getter,
            BlockPos pos) {
        if (!(getter instanceof ServerLevel level) || pos == null) {
            return Shapes.empty();
        }
        SpatialIndex cached = INDEXES.get(level.getServer());
        if (cached == null) return Shapes.empty();
        ProxyCell cell = cached.cell(level.dimension().location(), pos);
        return cell == null ? Shapes.empty() : cell.collision();
    }

    public static int proxyLight(BlockGetter level, BlockPos pos) {
        ProxyCell cell = proxyCell(level, pos);
        return cell == null ? 0 : cell.light();
    }

    public static List<TransformGroup> groups(ServerLevel level) {
        if (level == null) return List.of();
        ResourceLocation dimension = level.dimension().location();
        return TransformConstructionSavedData.get(level.getServer()).groups()
                .stream().filter(group -> group.dimension().equals(dimension))
                .toList();
    }

    public static List<ConstructionSurface> surfaces(ServerLevel level) {
        if (level == null) return List.of();
        ResourceLocation dimension = level.dimension().location();
        return TransformConstructionSavedData.get(level.getServer()).surfaces()
                .stream().filter(surface -> surface.dimension().equals(dimension))
                .toList();
    }

    public static boolean updateGroup(ServerPlayer player, UUID id,
            Vec3 origin, float rotationX, float rotationY, float rotationZ) {
        if (!canEdit(player) || id == null || origin == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        TransformGroup group = data.group(id);
        if (group == null || !group.dimension().equals(level.dimension().location())) {
            return false;
        }
        TransformGroup next = group.withTransform(origin, normalize(rotationX),
                normalize(rotationY), normalize(rotationZ));
        data.putGroup(next);
        refreshGroup(level.getServer(), id);
        return true;
    }

    public static boolean updateSurface(ServerPlayer player, UUID id,
            Vec3 bottomStart, Vec3 bottomEnd, Vec3 topStart, Vec3 topEnd,
            Vec3 curveOffset) {
        if (player == null || player.getServer() == null) return false;
        ConstructionSurface current =
                TransformConstructionSavedData.get(player.getServer()).surface(id);
        Vec3 heightCurve = current == null ? Vec3.ZERO
                : current.heightCurveOffset();
        return updateSurface(player, id, bottomStart, bottomEnd, topStart,
                topEnd, curveOffset, heightCurve);
    }

    public static boolean updateSurface(ServerPlayer player, UUID id,
            Vec3 bottomStart, Vec3 bottomEnd, Vec3 topStart, Vec3 topEnd,
            Vec3 curveOffset, Vec3 heightCurveOffset) {
        if (!canEdit(player) || id == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        ConstructionSurface surface = data.surface(id);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface next = surface.withGeometry(bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset);
        if ((long) next.columns() * next.rows() > MAX_SURFACE_SLOTS) return false;
        data.putSurface(next);
        refreshSurface(level.getServer(), id);
        return true;
    }

    public static boolean setSurfaceFlipped(ServerPlayer player, UUID id,
            boolean flipped) {
        if (!canEdit(player) || id == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        ConstructionSurface surface = data.surface(id);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface next = surface.withFlipped(flipped);
        data.putSurface(next);
        refreshSurface(level.getServer(), id);
        return true;
    }

    public static boolean placeGroupBlock(ServerPlayer player, UUID groupId,
            GridPos sourceCell, GridPos targetCell, Direction outwardLocal,
            Vec3 hit) {
        if (!canEdit(player) || groupId == null || sourceCell == null
                || targetCell == null || outwardLocal == null || hit == null
                || !(player.level() instanceof ServerLevel level)
                || !(player.getMainHandItem().getItem()
                        instanceof BlockItem blockItem)) {
            return false;
        }
        if (player.getEyePosition().distanceToSqr(hit) > 36.0D * 36.0D) {
            return false;
        }

        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        TransformGroup group = data.group(groupId);
        if (group == null || !group.dimension().equals(
                level.dimension().location())) return false;

        BlockState source = group.cells().getOrDefault(sourceCell,
                Blocks.AIR.defaultBlockState());
        GridPos expected = source.isAir() ? sourceCell : sourceCell.offset(
                outwardLocal.getStepX(), outwardLocal.getStepY(),
                outwardLocal.getStepZ());
        // The client authors against the group's clean local 1x1 grid. Validate
        // that discrete intent instead of re-deriving a cell by rounding a
        // transformed world-space hit a second time.
        if (!expected.equals(targetCell)) return false;
        GridPos target = expected;
        BlockState payload;
        Vec3 localHit = TransformMath.worldToLocal(group.origin(), hit,
                group.rotationX(), group.rotationY(), group.rotationZ());
        Vec3 supportOffset = localHit.subtract(sourceCell.x(), sourceCell.y(),
                sourceCell.z());
        TransformWallFixturePlacement.Placement special =
                TransformWallFixturePlacement.resolve(blockItem, outwardLocal,
                        supportOffset.x, supportOffset.z);
        if (special != null) {
            Direction shift = special.logicalShift();
            target = target.offset(shift.getStepX(), shift.getStepY(),
                    shift.getStepZ());
            payload = special.state();
        } else {
            payload = TransformPlacementStateRuntime.groupPlacementState(
                    player, blockItem, group, target, outwardLocal, hit);
        }

        if (group.cells().size() >= MAX_GROUP_CELLS
                && !group.cells().containsKey(target)) return false;
        if (!group.cells().getOrDefault(target,
                Blocks.AIR.defaultBlockState()).isAir()) {
            TransformConstructionNetwork.sendBlockedPlacement(player,
                    BlockPos.containing(group.cellCenter(target)));
            player.displayClientMessage(Component.literal(
                    "That off-grid cell is already occupied."), true);
            return true;
        }
        TransformGroup next = group.withCell(target, payload);
        data.putGroup(next);
        refreshGroupCell(level.getServer(), groupId, target);
        TransformConstructionNetwork.broadcastGroupCell(level, groupId,
                target, payload);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;
    }

    private static GridPos nearestGridCell(Vec3 local) {
        return new GridPos((int) Math.floor(local.x + 0.5D),
                (int) Math.floor(local.y + 0.5D),
                (int) Math.floor(local.z + 0.5D));
    }

    public static boolean placeSurfaceBlock(ServerPlayer player, UUID surfaceId,
            SurfaceSlot slot, Vec3 hit) {
        if (!canEdit(player) || surfaceId == null || slot == null || hit == null
                || !(player.level() instanceof ServerLevel level)
                || !(player.getMainHandItem().getItem()
                        instanceof BlockItem blockItem)) {
            return false;
        }
        if (player.getEyePosition().distanceToSqr(hit) > 36.0D * 36.0D) {
            return false;
        }

        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())
                || slot.column() < 0 || slot.column() >= surface.columns()
                || slot.row() < 0 || slot.row() >= surface.rows()) {
            return false;
        }

        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 center = surface.gridPoint(u, v);
        if (center.distanceToSqr(hit) > 2.25D) return false;

        SurfaceSlot targetSlot = slot;
        Vec3 tangent = surface.gridFrameTangent(u, v).normalize();
        Vec3 normal = surface.gridNormal(u, v).normalize();
        Vec3 delta = hit.subtract(center);
        double localX = delta.dot(tangent);
        double localZ = delta.dot(normal);
        TransformWallFixturePlacement.Placement special =
                TransformWallFixturePlacement.resolve(blockItem,
                        Direction.SOUTH, localX, localZ);
        BlockState payload;
        if (special != null) {
            int frameSign = surface.flipped() ? -1 : 1;
            int column = slot.column()
                    + special.logicalShift().getStepX() * frameSign;
            int row = slot.row()
                    + special.logicalShift().getStepY();
            if (column < 0 || column >= surface.columns()
                    || row < 0 || row >= surface.rows()) {
                return false;
            }
            targetSlot = new SurfaceSlot(column, row);
            payload = special.state();
        } else {
            payload = TransformPlacementStateRuntime.surfacePlacementState(
                    player, blockItem, surface, slot, hit);
        }

        if (surface.attachments().containsKey(targetSlot)) {
            double tu = (targetSlot.column() + 0.5D) / surface.columns();
            double tv = (targetSlot.row() + 0.5D) / surface.rows();
            TransformConstructionNetwork.sendBlockedPlacement(player,
                    BlockPos.containing(surface.gridPoint(tu, tv)
                            .add(surface.gridNormal(tu, tv).scale(0.5D))));
            player.displayClientMessage(Component.literal(
                    "That surface cell is already occupied."), true);
            return true;
        }

        boolean deform = !payload.hasBlockEntity();
        ConstructionSurface next = surface.withAttachment(
                targetSlot, payload, deform);
        data.putSurface(next);
        refreshSurfaceSlot(level.getServer(), surfaceId, targetSlot);
        TransformConstructionNetwork.broadcastSurfaceSlot(level, surfaceId,
                targetSlot, payload, deform);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;
    }

    public static boolean placeSurfaceOverlay(ServerPlayer player,
            UUID surfaceId, SurfaceSlot slot, int normalSign, Vec3 hit) {
        if (!canEdit(player) || surfaceId == null || slot == null || hit == null
                || !(player.level() instanceof ServerLevel level)
                || !(player.getMainHandItem().getItem()
                        instanceof BlockItem blockItem)) return false;
        int side = normalSign < 0 ? -1 : 1;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())
                || slot.column() < 0 || slot.column() >= surface.columns()
                || slot.row() < 0 || slot.row() >= surface.rows()) return false;
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 center = surface.gridPoint(u, v);
        if (player.getEyePosition().distanceToSqr(hit) > 36.0D * 36.0D
                || center.distanceToSqr(hit) > 2.25D) return false;
        SurfaceSlot targetSlot = slot;
        Vec3 tangent = surface.gridFrameTangent(u, v).scale(side).normalize();
        Vec3 normal = surface.gridNormal(u, v).scale(side).normalize();
        Vec3 delta = hit.subtract(center);
        double localX = delta.dot(tangent);
        double localZ = delta.dot(normal);
        TransformWallFixturePlacement.Placement special =
                TransformWallFixturePlacement.resolve(blockItem,
                        Direction.SOUTH, localX, localZ);
        BlockState payload;
        if (special != null) {
            int frameSign = (surface.flipped() ? -1 : 1) * side;
            int column = slot.column()
                    + special.logicalShift().getStepX() * frameSign;
            int row = slot.row()
                    + special.logicalShift().getStepY();
            if (column < 0 || column >= surface.columns()
                    || row < 0 || row >= surface.rows()) {
                return false;
            }
            targetSlot = new SurfaceSlot(column, row);
            payload = special.state();
        } else {
            payload = TransformPlacementStateRuntime.surfacePlacementState(
                    player, blockItem, surface, slot, hit, side);
        }

        if (surface.overlay(targetSlot, side) != null) {
            double tu = (targetSlot.column() + 0.5D) / surface.columns();
            double tv = (targetSlot.row() + 0.5D) / surface.rows();
            TransformConstructionNetwork.sendBlockedPlacement(player,
                    BlockPos.containing(surface.gridPoint(tu, tv)));
            player.displayClientMessage(Component.literal(
                    "That side of the surface cell is already occupied."), true);
            return true;
        }
        boolean deform = !payload.hasBlockEntity();
        ConstructionSurface next = surface.withOverlay(targetSlot, side,
                payload, deform);
        data.putSurface(next);
        refreshSurfaceSlot(level.getServer(), surfaceId, targetSlot);
        TransformConstructionNetwork.broadcastSurfaceOverlay(level, surfaceId,
                targetSlot, side, payload, deform);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;
    }

    public static boolean removeSurfaceOverlay(ServerPlayer player, UUID id,
            SurfaceSlot slot, int normalSign) {
        if (!canEdit(player) || id == null || slot == null
                || !(player.level() instanceof ServerLevel level)) return false;
        int side = normalSign < 0 ? -1 : 1;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(id);
        if (surface == null || surface.overlay(slot, side) == null) return false;
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 center = surface.gridPoint(u, v)
                .add(surface.gridNormal(u, v).scale(side * 0.5D));
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;
        data.putSurface(surface.withoutOverlay(slot, side));
        refreshSurfaceSlot(level.getServer(), id, slot);
        TransformConstructionNetwork.broadcastSurfaceOverlayRemoved(level, id,
                slot, side);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;
    }

    public static boolean removeGroupCell(ServerPlayer player, UUID id,
            GridPos cell) {
        if (!canEdit(player) || id == null || cell == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        TransformGroup group = data.group(id);
        if (group == null || !group.dimension().equals(
                level.dimension().location())) return false;
        BlockState state = group.cells().get(cell);
        if (state == null || state.isAir()
                || player.getEyePosition().distanceToSqr(
                        group.cellCenter(cell)) > 36.0D) return false;

        TransformGroup next = group.withoutCell(cell);
        if (next.cells().isEmpty()) {
            data.removeGroup(id);
            refreshGroup(level.getServer(), id);
            // Deleting the owner itself still needs a structural snapshot.
            return true;
        }
        data.putGroup(next);
        refreshGroupCell(level.getServer(), id, cell);
        TransformConstructionNetwork.broadcastGroupCellRemoved(level, id, cell);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;
    }

    public static boolean removeSurfaceSlot(ServerPlayer player, UUID id,
            SurfaceSlot slot) {
        if (!canEdit(player) || id == null || slot == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(id);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface.SurfaceAttachment attachment =
                surface.attachments().get(slot);
        if (attachment == null || attachment.state().isAir()) return false;
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 center = surface.gridPoint(u, v)
                .add(surface.gridNormal(u, v).scale(0.5D));
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;

        data.putSurface(surface.withoutAttachment(slot));
        refreshSurfaceSlot(level.getServer(), id, slot);
        TransformConstructionNetwork.broadcastSurfaceSlotRemoved(
                level, id, slot);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;
    }

    public static boolean removeGroup(ServerPlayer player, UUID id) {
        if (!canEdit(player) || id == null || player.getServer() == null) {
            return false;
        }
        boolean changed = TransformConstructionSavedData.get(player.getServer())
                .removeGroup(id);
        if (changed) refreshGroup(player.getServer(), id);
        return changed;
    }

    public static boolean removeSurface(ServerPlayer player, UUID id) {
        if (!canEdit(player) || id == null || player.getServer() == null) {
            return false;
        }
        boolean changed = TransformConstructionSavedData.get(player.getServer())
                .removeSurface(id);
        if (changed) refreshSurface(player.getServer(), id);
        return changed;
    }

    private static boolean placeBlock(ServerPlayer player, BlockPos proxyPos,
            BlockHitResult hit, ItemStack stack) {
        if (!canEdit(player) || !(stack.getItem() instanceof BlockItem blockItem)
                || !(player.level() instanceof ServerLevel level)) return false;
        ProxyCell proxy = index(level.getServer()).cell(level.dimension().location(),
                proxyPos);
        if (proxy == null) return false;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        BlockState payload = blockItem.getBlock().defaultBlockState();

        GroupHit groupHit = nearestGroupHit(data, level.dimension().location(),
                proxy.groupIds(), hit.getLocation());
        if (groupHit != null) {
            TransformGroup group = data.group(groupHit.groupId());
            GridPos target = groupHit.cell();
            BlockState existing = group.cells().getOrDefault(target,
                    Blocks.AIR.defaultBlockState());
            if (!existing.isAir()) {
                Vec3 local = TransformMath.worldToLocal(group.origin(),
                        hit.getLocation(), group.rotationX(), group.rotationY(),
                        group.rotationZ());
                Vec3 delta = local.subtract(target.x(), target.y(), target.z());
                Direction direction = dominantDirection(delta);
                target = target.offset(direction.getStepX(), direction.getStepY(),
                        direction.getStepZ());
            }
            if (group.cells().size() >= MAX_GROUP_CELLS
                    && !group.cells().containsKey(target)) return false;
            TransformGroup next = group.withCell(target, payload);
            data.putGroup(next);
            refreshGroupCell(level.getServer(), group.id(), target);
            TransformConstructionNetwork.broadcastGroupCell(level,
                    group.id(), target, payload);
            TransformConstructionNetwork.acknowledgeRevision(
                    level.getServer());
            return true;
        }

        SurfaceHit surfaceHit = nearestSurfaceHit(data,
                level.dimension().location(), proxy.surfaceIds(),
                hit.getLocation());
        if (surfaceHit != null) {
            ConstructionSurface surface = data.surface(surfaceHit.surfaceId());
            if (surface.attachments().containsKey(surfaceHit.slot())) {
                double u = (surfaceHit.slot().column() + 0.5D)
                        / surface.columns();
                double v = (surfaceHit.slot().row() + 0.5D)
                        / surface.rows();
                TransformConstructionNetwork.sendBlockedPlacement(player,
                        BlockPos.containing(surface.gridPoint(u, v).add(
                                surface.gridNormal(u, v).scale(0.5D))));
                player.displayClientMessage(Component.literal(
                        "That surface cell is already occupied."), true);
                return true;
            }
            boolean deform = !payload.hasBlockEntity();
            ConstructionSurface next = surface.withAttachment(surfaceHit.slot(),
                    payload, deform);
            data.putSurface(next);
            refreshSurfaceSlot(level.getServer(), surface.id(),
                    surfaceHit.slot());
            TransformConstructionNetwork.broadcastSurfaceSlot(level,
                    surface.id(), surfaceHit.slot(), payload, deform);
            TransformConstructionNetwork.acknowledgeRevision(
                    level.getServer());
            return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(
                        TransformConstructionModule.getProxy())
                || !(event.getItemStack().getItem() instanceof BlockItem)) {
            return;
        }

        // Placement authority lives in the transformed logical-grid packets
        // (PlaceGroupBlock / PlaceSurfaceBlock). The old proxy-hit fallback
        // used the axis-aligned vanilla BlockPos and could race the logical
        // packet, placing a second block in a different cell and refreshing
        // physics twice. Keep proxies physical, never semantic.
        event.setCanceled(true);
        event.setCancellationResult(
                net.minecraft.world.InteractionResult.SUCCESS);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !event.getState().is(TransformConstructionModule.getProxy())) {
            return;
        }
        event.setCanceled(true);
        ProxyCell proxy = index(level.getServer()).cell(level.dimension().location(),
                event.getPos());
        if (proxy == null) return;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        Vec3 probe = Vec3.atCenterOf(event.getPos());
        GroupHit groupHit = nearestGroupHit(data, level.dimension().location(),
                proxy.groupIds(), probe);
        if (groupHit != null) {
            TransformGroup group = data.group(groupHit.groupId());
            if (group != null) {
                TransformGroup next = group.withoutCell(groupHit.cell());
                if (next.cells().isEmpty()) {
                    data.removeGroup(group.id());
                    refreshGroup(level.getServer(), group.id());
                    // Owner deletion remains a structural revision and will be
                    // distributed by the normal snapshot path.
                } else {
                    data.putGroup(next);
                    refreshGroupCell(level.getServer(), group.id(),
                            groupHit.cell());
                    TransformConstructionNetwork.broadcastGroupCellRemoved(
                            level, group.id(), groupHit.cell());
                    TransformConstructionNetwork.acknowledgeRevision(
                            level.getServer());
                }
                return;
            }
        }
        SurfaceHit surfaceHit = nearestSurfaceHit(data,
                level.dimension().location(), proxy.surfaceIds(), probe);
        if (surfaceHit != null) {
            ConstructionSurface surface = data.surface(surfaceHit.surfaceId());
            if (surface != null && surface.attachments().containsKey(
                    surfaceHit.slot())) {
                data.putSurface(surface.withoutAttachment(
                        surfaceHit.slot()));
                refreshSurfaceSlot(level.getServer(), surface.id(),
                        surfaceHit.slot());
                TransformConstructionNetwork.broadcastSurfaceSlotRemoved(
                        level, surface.id(), surfaceHit.slot());
                TransformConstructionNetwork.acknowledgeRevision(
                        level.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ensureChunkProxies(level, event.getChunk().getPos().x,
                event.getChunk().getPos().z);
    }

    public static synchronized void refresh(MinecraftServer server) {
        if (server == null) return;
        // Structural edits can add/remove transformed redstone sources.
        TransformPowerQuery.invalidate(server);
        SpatialIndex previous = INDEXES.get(server);
        SpatialIndex next = buildIndex(server);
        INDEXES.put(server, next);
        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation dimension = level.dimension().location();
            Set<Long> changed = new LinkedHashSet<>();
            if (previous != null) changed.addAll(previous.positions(dimension));
            changed.addAll(next.positions(dimension));
            for (long packed : changed) {
                BlockPos pos = BlockPos.of(packed);
                if (!level.hasChunkAt(pos)) continue;
                materializeProxyCell(level, pos,
                        next.cell(dimension, pos));
            }
        }
    }

    public static synchronized void refreshGroup(MinecraftServer server,
            UUID groupId) {
        refreshOwner(server, groupId, false, true);
    }

    public static synchronized void refreshGroupRuntime(MinecraftServer server,
            UUID groupId) {
        refreshOwner(server, groupId, false, false);
    }

    public static synchronized void refreshSurface(MinecraftServer server,
            UUID surfaceId) {
        refreshOwner(server, surfaceId, true, true);
    }

    public static synchronized void refreshSurfaceRuntime(MinecraftServer server,
            UUID surfaceId) {
        refreshOwner(server, surfaceId, true, false);
    }

    public static synchronized void refreshGroupCell(MinecraftServer server,
            UUID groupId, GridPos cell) {
        refreshGroupCell(server, groupId, cell, true);
    }

    public static synchronized void refreshGroupCellRuntime(
            MinecraftServer server, UUID groupId, GridPos cell) {
        refreshGroupCell(server, groupId, cell, false);
    }

    public static synchronized void refreshSurfaceSlot(MinecraftServer server,
            UUID surfaceId, SurfaceSlot slot) {
        refreshSurfaceSlot(server, surfaceId, slot, true);
    }

    public static synchronized void refreshSurfaceSlotRuntime(
            MinecraftServer server, UUID surfaceId, SurfaceSlot slot) {
        refreshSurfaceSlot(server, surfaceId, slot, false);
    }

    /**
     * Rebuild only one logical owner. This is the normal editing/runtime path;
     * full refresh() is reserved for startup/recovery.
     */
    private static void refreshOwner(MinecraftServer server, UUID id,
            boolean surfaceOwner, boolean invalidatePower) {
        if (server == null || id == null) return;

        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        if (invalidatePower) {
            if (surfaceOwner) {
                ConstructionSurface surface = data.surface(id);
                if (surface == null) {
                    for (ServerLevel level : server.getAllLevels()) {
                        TransformPowerQuery.removeSurface(server,
                                level.dimension().location(), id);
                    }
                } else {
                    TransformPowerQuery.refreshSurface(server, surface);
                }
            } else {
                TransformGroup group = data.group(id);
                if (group == null) {
                    for (ServerLevel level : server.getAllLevels()) {
                        TransformPowerQuery.removeGroup(server,
                                level.dimension().location(), id);
                    }
                } else {
                    TransformPowerQuery.refreshGroup(server, group);
                }
            }
        }

        SpatialIndex index = INDEXES.get(server);
        if (index == null) {
            refresh(server);
            return;
        }

        Set<OwnerKey> previousKeys = index.ownerKeys(id, surfaceOwner);
        Map<ResourceLocation, Set<Long>> affected = new LinkedHashMap<>();
        for (OwnerKey key : previousKeys) {
            affected.computeIfAbsent(key.dimension(), ignored ->
                    new LinkedHashSet<>()).addAll(index.ownerPositions(key));
            index.removeOwner(key);
        }

        if (surfaceOwner) {
            ConstructionSurface surface = data.surface(id);
            if (surface != null) {
                addSurface(index, surface);
                for (OwnerKey key : index.ownerKeys(id, true)) {
                    affected.computeIfAbsent(key.dimension(), ignored ->
                            new LinkedHashSet<>()).addAll(
                                    index.ownerPositions(key));
                }
            }
        } else {
            TransformGroup group = data.group(id);
            if (group != null) {
                addGroup(index, group);
                for (OwnerKey key : index.ownerKeys(id, false)) {
                    affected.computeIfAbsent(key.dimension(), ignored ->
                            new LinkedHashSet<>()).addAll(
                                    index.ownerPositions(key));
                }
            }
        }

        materializeAffected(server, index, affected);
        if (surfaceOwner) {
            TransformSurfaceDoorRuntime.structuralSurfaceChanged(server, id);
            TransformAlarmRuntime.structuralSurfaceChanged(server, id);
            TransformPoweredBlockRuntime.structuralSurfaceChanged(server, id);
            TransformDoorRuntime.acknowledgeStructuralRevision(server);
        } else {
            TransformDoorRuntime.structuralGroupChanged(server, id);
            TransformAlarmRuntime.structuralGroupChanged(server, id);
            TransformPoweredBlockRuntime.structuralGroupChanged(server, id);
            TransformSurfaceDoorRuntime.acknowledgeStructuralRevision(server);
        }
    }

    private static void refreshGroupCell(MinecraftServer server,
            UUID id, GridPos cell, boolean invalidatePower) {
        if (server == null || id == null || cell == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        TransformGroup group = data.group(id);

        if (invalidatePower) {
            if (group == null) {
                for (ServerLevel level : server.getAllLevels()) {
                    TransformPowerQuery.removeGroup(server,
                            level.dimension().location(), id);
                }
            } else {
                TransformPowerQuery.refreshGroupCell(server, group, cell);
            }
        }

        SpatialIndex index = INDEXES.get(server);
        if (index == null) {
            refresh(server);
            return;
        }

        LogicalPart part = LogicalPart.group(cell);
        Map<ResourceLocation, Set<Long>> affected = new LinkedHashMap<>();
        if (group != null) {
            OwnerKey key = OwnerKey.groupCell(group.dimension(), id, cell);
            affected.computeIfAbsent(group.dimension(), ignored ->
                    new LinkedHashSet<>()).addAll(index.ownerPositions(key));
            index.removeOwner(key);
        } else {
            // Owner deletion is rare. Only this recovery path needs to search
            // by structure id; normal cell edits have an exact deterministic
            // owner key and stay O(1).
            for (OwnerKey key : index.ownerKeys(id, false)) {
                if (!part.equals(key.part())) continue;
                affected.computeIfAbsent(key.dimension(), ignored ->
                        new LinkedHashSet<>()).addAll(index.ownerPositions(key));
                index.removeOwner(key);
            }
        }

        if (group != null && group.cells().containsKey(cell)) {
            addGroupCell(index, group, cell);
            OwnerKey key = OwnerKey.groupCell(group.dimension(), id, cell);
            affected.computeIfAbsent(group.dimension(), ignored ->
                    new LinkedHashSet<>()).addAll(index.ownerPositions(key));
        }
        materializeAffected(server, index, affected);
        if (invalidatePower) {
            TransformDoorRuntime.structuralCellChanged(server, id, cell);
            TransformAlarmRuntime.structuralGroupCellChanged(server, id, cell);
            TransformPoweredBlockRuntime.structuralGroupCellChanged(
                    server, id, cell);
            TransformSurfaceDoorRuntime.acknowledgeStructuralRevision(server);
        }
    }

    private static void refreshSurfaceSlot(MinecraftServer server,
            UUID id, SurfaceSlot slot, boolean invalidatePower) {
        if (server == null || id == null || slot == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        ConstructionSurface surface = data.surface(id);

        if (invalidatePower) {
            if (surface == null) {
                for (ServerLevel level : server.getAllLevels()) {
                    TransformPowerQuery.removeSurface(server,
                            level.dimension().location(), id);
                }
            } else {
                TransformPowerQuery.refreshSurfaceSlot(server, surface, slot);
            }
        }

        SpatialIndex index = INDEXES.get(server);
        if (index == null) {
            refresh(server);
            return;
        }

        LogicalPart part = LogicalPart.surface(slot);
        Map<ResourceLocation, Set<Long>> affected = new LinkedHashMap<>();
        if (surface != null) {
            OwnerKey key = OwnerKey.surfaceSlot(surface.dimension(), id, slot);
            affected.computeIfAbsent(surface.dimension(), ignored ->
                    new LinkedHashSet<>()).addAll(index.ownerPositions(key));
            index.removeOwner(key);
        } else {
            for (OwnerKey key : index.ownerKeys(id, true)) {
                if (!part.equals(key.part())) continue;
                affected.computeIfAbsent(key.dimension(), ignored ->
                        new LinkedHashSet<>()).addAll(index.ownerPositions(key));
                index.removeOwner(key);
            }
        }

        if (surface != null
                && slot.column() >= 0 && slot.column() < surface.columns()
                && slot.row() >= 0 && slot.row() < surface.rows()) {
            addSurfaceSlot(index, surface, slot);
            OwnerKey key = OwnerKey.surfaceSlot(surface.dimension(), id, slot);
            affected.computeIfAbsent(surface.dimension(), ignored ->
                    new LinkedHashSet<>()).addAll(index.ownerPositions(key));
        }
        materializeAffected(server, index, affected);
        if (invalidatePower) {
            TransformSurfaceDoorRuntime.structuralSlotChanged(server, id, slot);
            TransformAlarmRuntime.structuralSurfaceSlotChanged(server, id, slot);
            TransformPoweredBlockRuntime.structuralSurfaceSlotChanged(
                    server, id, slot);
            TransformDoorRuntime.acknowledgeStructuralRevision(server);
        }
    }

    private static void materializeAffected(MinecraftServer server,
            SpatialIndex index,
            Map<ResourceLocation, Set<Long>> affected) {
        for (Map.Entry<ResourceLocation, Set<Long>> entry
                : affected.entrySet()) {
            ServerLevel level = levelByDimension(server, entry.getKey());
            if (level == null) continue;
            for (long packed : entry.getValue()) {
                BlockPos pos = BlockPos.of(packed);
                if (!level.hasChunkAt(pos)) continue;
                materializeProxyCell(level, pos,
                        index.cell(entry.getKey(), pos));
            }
        }
    }

    private static ServerLevel levelByDimension(MinecraftServer server,
            ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) return level;
        }
        return null;
    }

    private static void materializeProxyCell(ServerLevel level, BlockPos pos,
            ProxyCell cell) {
        BlockState current = level.getBlockState(pos);
        int flags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
        if (cell == null || !materialize(cell)) {
            if (current.is(TransformConstructionModule.getProxy())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
            }
            return;
        }
        if (current.isAir()
                || current.is(TransformConstructionModule.getProxy())
                || current.canBeReplaced()) {
            BlockState wanted = TransformConstructionModule.getProxy()
                    .defaultBlockState()
                    .setValue(TransformConstructionModule.LIGHT, cell.light());
            if (!current.equals(wanted)) level.setBlock(pos, wanted, flags);
        }
    }

    private static void ensureChunkProxies(ServerLevel level, int chunkX,
            int chunkZ) {
        SpatialIndex index = index(level.getServer());
        ResourceLocation dimension = level.dimension().location();
        for (long packed : index.positions(dimension)) {
            BlockPos pos = BlockPos.of(packed);
            if ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ) {
                continue;
            }
            materializeProxyCell(level, pos,
                    index.cell(dimension, pos));
        }
    }

    private static boolean materialize(ProxyCell cell) {
        // Selection is now resolved by the authored local-grid raycasts. Do not
        // place a technical vanilla block merely so Minecraft has something to
        // outline. Proxies exist only when the parent world needs a physical
        // bridge for collision or emitted light.
        return cell != null
                && (!cell.collision().isEmpty() || cell.light() > 0);
    }

    private static boolean canOccupy(ServerLevel level, TransformGroup group,
            ConstructionSurface surface) {
        return canOccupy(level, group, surface, null, null);
    }

    private static boolean canOccupy(ServerLevel level, TransformGroup group,
            ConstructionSurface surface, UUID replacingGroup,
            UUID replacingSurface) {
        return firstObstruction(level, group, surface, replacingGroup,
                replacingSurface) == null;
    }

    private static BlockPos firstObstruction(ServerLevel level,
            TransformGroup group, ConstructionSurface surface,
            UUID replacingGroup, UUID replacingSurface) {
        SpatialIndex base = buildIndex(level.getServer(), null, null,
                replacingGroup, replacingSurface);
        SpatialIndex candidate = new SpatialIndex();
        if (group != null) addGroup(candidate, group);
        if (surface != null) addSurface(candidate, surface);
        ResourceLocation dimension = level.dimension().location();
        for (long packed : candidate.positions(dimension)) {
            BlockPos pos = BlockPos.of(packed);
            ProxyCell cell = candidate.cell(dimension, pos);
            if (cell == null) continue;
            ProxyCell occupied = base.cell(dimension, pos);
            if (occupied != null && !cell.collision().isEmpty()
                    && !occupied.collision().isEmpty()
                    && materiallyObstructed(cell.collision(),
                            occupied.collision())) {
                return pos;
            }
            if (cell.collision().isEmpty()) continue;
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir()
                    || existing.is(TransformConstructionModule.getProxy())
                    || existing.canBeReplaced()) continue;
            VoxelShape worldShape = existing.getCollisionShape(level, pos,
                    CollisionContext.empty());
            if (!worldShape.isEmpty() && materiallyObstructed(
                    cell.collision(), worldShape)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Rotated/deformed proxy collision is an AABB approximation. Rejecting a
     * placement for one tiny overlap creates the familiar "half a block of air
     * is forbidden" problem on arcs. Keep the free physical portion and only
     * reject when most of the candidate volume is actually buried.
     */
    private static boolean materiallyObstructed(VoxelShape candidate,
            VoxelShape occupied) {
        double volume = shapeVolume(candidate);
        if (volume < 1.0E-6D) return false;
        double overlap = overlapVolume(candidate, occupied);
        if (overlap <= 1.0E-5D) return false;
        double ratio = overlap / volume;

        // Transformed construction is an authoring system, not a vanilla block
        // placement validator. Existing solid cells already keep their own
        // collision because proxies never replace them, so overlapping visual
        // geometry should only be rejected when the candidate is essentially
        // buried. Keep this proportional: an absolute leftover-volume threshold
        // disproportionately rejects thin details such as buttons and trims.
        return ratio > 0.985D;
    }

    private static double shapeVolume(VoxelShape shape) {
        double volume = 0.0D;
        for (AABB box : shape.toAabbs()) {
            volume += Math.max(0.0D, box.getXsize())
                    * Math.max(0.0D, box.getYsize())
                    * Math.max(0.0D, box.getZsize());
        }
        return volume;
    }

    private static double overlapVolume(VoxelShape first, VoxelShape second) {
        double volume = 0.0D;
        for (AABB a : first.toAabbs()) {
            for (AABB b : second.toAabbs()) {
                double x = Math.max(0.0D,
                        Math.min(a.maxX, b.maxX) - Math.max(a.minX, b.minX));
                double y = Math.max(0.0D,
                        Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY));
                double z = Math.max(0.0D,
                        Math.min(a.maxZ, b.maxZ) - Math.max(a.minZ, b.minZ));
                volume += x * y * z;
            }
        }
        return volume;
    }

    private static ProxyCell proxyCell(BlockGetter getter, BlockPos pos) {
        if (!(getter instanceof ServerLevel level)) return null;
        return index(level.getServer()).cell(level.dimension().location(), pos);
    }

    private static synchronized SpatialIndex index(MinecraftServer server) {
        return INDEXES.computeIfAbsent(server, TransformConstructionManager::buildIndex);
    }

    private static SpatialIndex buildIndex(MinecraftServer server) {
        return buildIndex(server, null, null, null, null);
    }

    private static SpatialIndex buildIndex(MinecraftServer server,
            TransformGroup replacementGroup,
            ConstructionSurface replacementSurface, UUID replacingGroup,
            UUID replacingSurface) {
        TransformConstructionSavedData data = TransformConstructionSavedData.get(server);
        SpatialIndex index = new SpatialIndex();
        for (TransformGroup group : data.groups()) {
            if (replacingGroup != null && replacingGroup.equals(group.id())) continue;
            addGroup(index, group);
        }
        if (replacementGroup != null) addGroup(index, replacementGroup);
        for (ConstructionSurface surface : data.surfaces()) {
            if (replacingSurface != null && replacingSurface.equals(surface.id())) {
                continue;
            }
            addSurface(index, surface);
        }
        if (replacementSurface != null) addSurface(index, replacementSurface);
        return index;
    }

    private static void addGroup(SpatialIndex index, TransformGroup group) {
        for (GridPos cell : group.cells().keySet()) {
            addGroupCell(index, group, cell);
        }
    }

    private static void addGroupCell(SpatialIndex index, TransformGroup group,
            GridPos cell) {
        BlockState state = group.cells().get(cell);
        if (state == null) return;
        OwnerKey owner = OwnerKey.groupCell(group.dimension(), group.id(), cell);
        boolean placeholder = state.isAir();
        if (placeholder) {
            AABB selection = new AABB(cell.x() - 0.5D, cell.y() - 0.5D,
                    cell.z() - 0.5D, cell.x() + 0.5D, cell.y() + 0.5D,
                    cell.z() + 0.5D);
            addWorldBox(index, owner, transformedBounds(group, selection),
                    true, false, 0);
            return;
        }

        AABB selection = new AABB(cell.x() - 0.5D,
                cell.y() - 0.5D, cell.z() - 0.5D,
                cell.x() + 0.5D, cell.y() + 0.5D, cell.z() + 0.5D);
        addWorldBox(index, owner, transformedBounds(group, selection),
                true, false, state.getLightEmission());

        VoxelShape collision = FacilityModule.isFacilityDoor(state)
                && FacilityModule.isDoorPassable(state)
                ? Shapes.empty()
                : state.getCollisionShape(EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO, CollisionContext.empty());
        if (collision.isEmpty()) return;
        int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;
        double inv = 1.0D / subdivisions;
        collision.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double boxX = maxX - minX;
            double boxY = maxY - minY;
            double boxZ = maxZ - minZ;
            for (int sx = 0; sx < subdivisions; sx++) {
                for (int sy = 0; sy < subdivisions; sy++) {
                    for (int sz = 0; sz < subdivisions; sz++) {
                        AABB local = new AABB(
                                cell.x() - 0.5D + minX + boxX * sx * inv,
                                cell.y() - 0.5D + minY + boxY * sy * inv,
                                cell.z() - 0.5D + minZ + boxZ * sz * inv,
                                cell.x() - 0.5D + minX
                                        + boxX * (sx + 1) * inv,
                                cell.y() - 0.5D + minY
                                        + boxY * (sy + 1) * inv,
                                cell.z() - 0.5D + minZ
                                        + boxZ * (sz + 1) * inv);
                        addWorldBox(index, owner,
                                transformedBounds(group, local),
                                false, true, state.getLightEmission());
                    }
                }
            }
        });
    }

    private static void addSurface(SpatialIndex index,
            ConstructionSurface surface) {
        int columns = surface.columns();
        int rows = surface.rows();
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                addSurfaceSlot(index, surface, new SurfaceSlot(column, row));
            }
        }
    }

    private static void addSurfaceSlot(SpatialIndex index,
            ConstructionSurface surface, SurfaceSlot slot) {
        OwnerKey owner = OwnerKey.surfaceSlot(surface.dimension(),
                surface.id(), slot);
        SurfaceAttachment attachment = surface.attachments().get(slot);
        AABB selection = surfaceSlotBounds(surface, slot.column(), slot.row(),
                SURFACE_SELECTION_THICKNESS);
        addWorldBox(index, owner, selection, true, false, 0);
        if (attachment != null && !attachment.state().isAir()) {
            for (AABB collision : TransformSurfaceGeometry.collisionBoxes(
                    surface, slot, attachment)) {
                addWorldBox(index, owner, collision, false, true,
                        attachment.state().getLightEmission());
            }
        }
        for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                SurfaceAttachment> overlay : surface.overlays().entrySet()) {
            if (!overlay.getKey().slot().equals(slot)
                    || overlay.getValue().state().isAir()) continue;
            for (AABB collision : TransformSurfaceGeometry.collisionBoxes(
                    surface, slot, overlay.getValue(),
                    overlay.getKey().normalSign(), true)) {
                addWorldBox(index, owner, collision, false, true,
                        overlay.getValue().state().getLightEmission());
            }
        }
    }

    private static AABB transformedBounds(TransformGroup group, AABB local) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int xi = 0; xi < 2; xi++) {
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    Vec3 localPoint = new Vec3(xi == 0 ? local.minX : local.maxX,
                            yi == 0 ? local.minY : local.maxY,
                            zi == 0 ? local.minZ : local.maxZ);
                    Vec3 world = TransformMath.localToWorld(group.origin(),
                            localPoint, group.rotationX(), group.rotationY(),
                            group.rotationZ());
                    minX = Math.min(minX, world.x);
                    minY = Math.min(minY, world.y);
                    minZ = Math.min(minZ, world.z);
                    maxX = Math.max(maxX, world.x);
                    maxY = Math.max(maxY, world.y);
                    maxZ = Math.max(maxZ, world.z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AABB surfaceSlotBounds(ConstructionSurface surface,
            int column, int row, double thickness) {
        int columns = surface.columns();
        int rows = surface.rows();
        double u0 = column / (double) columns;
        double u1 = (column + 1.0D) / columns;
        double v0 = row / (double) rows;
        double v1 = (row + 1.0D) / rows;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double half = thickness * 0.5D;
        for (int ui = 0; ui <= 2; ui++) {
            double u = u0 + (u1 - u0) * ui / 2.0D;
            for (int vi = 0; vi <= 2; vi++) {
                double v = v0 + (v1 - v0) * vi / 2.0D;
                Vec3 point = surface.gridPoint(u, v);
                Vec3 normal = surface.gridNormal(u, v).scale(half);
                for (int sign : new int[]{-1, 1}) {
                    Vec3 world = point.add(normal.scale(sign));
                    minX = Math.min(minX, world.x);
                    minY = Math.min(minY, world.y);
                    minZ = Math.min(minZ, world.z);
                    maxX = Math.max(maxX, world.x);
                    maxY = Math.max(maxY, world.y);
                    maxZ = Math.max(maxZ, world.z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void addWorldBox(SpatialIndex index,
            OwnerKey owner, AABB worldBox, boolean selection,
            boolean collision, int light) {
        int minX = (int) Math.floor(worldBox.minX);
        int minY = (int) Math.floor(worldBox.minY);
        int minZ = (int) Math.floor(worldBox.minZ);
        int maxX = (int) Math.floor(Math.nextDown(worldBox.maxX));
        int maxY = (int) Math.floor(Math.nextDown(worldBox.maxY));
        int maxZ = (int) Math.floor(Math.nextDown(worldBox.maxZ));
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    AABB cell = new AABB(x, y, z, x + 1.0D, y + 1.0D,
                            z + 1.0D);
                    AABB clipped = intersect(worldBox, cell);
                    if (clipped == null) continue;
                    AABB local = clipped.move(-x, -y, -z);
                    index.add(owner, pos, local, selection, collision, light);
                }
            }
        }
    }

    private static AABB intersect(AABB a, AABB b) {
        double minX = Math.max(a.minX, b.minX);
        double minY = Math.max(a.minY, b.minY);
        double minZ = Math.max(a.minZ, b.minZ);
        double maxX = Math.min(a.maxX, b.maxX);
        double maxY = Math.min(a.maxY, b.maxY);
        double maxZ = Math.min(a.maxZ, b.maxZ);
        return minX < maxX && minY < maxY && minZ < maxZ
                ? new AABB(minX, minY, minZ, maxX, maxY, maxZ) : null;
    }

    private static boolean nearOrthogonal(TransformGroup group) {
        return nearRightAngle(group.rotationX())
                && nearRightAngle(group.rotationY())
                && nearRightAngle(group.rotationZ());
    }

    private static boolean nearRightAngle(float degrees) {
        double normalized = Math.abs(degrees % 90.0F);
        return normalized < 0.01D || Math.abs(normalized - 90.0D) < 0.01D;
    }

    private static float normalize(float degrees) {
        float value = degrees % 360.0F;
        if (value > 180.0F) value -= 360.0F;
        if (value <= -180.0F) value += 360.0F;
        return value;
    }

    private static GroupHit nearestGroupHit(TransformConstructionSavedData data,
            ResourceLocation dimension, Set<UUID> ids, Vec3 world) {
        GroupHit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (UUID id : ids) {
            TransformGroup group = data.group(id);
            if (group == null || !group.dimension().equals(dimension)) continue;
            Vec3 local = TransformMath.worldToLocal(group.origin(), world,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            for (GridPos cell : group.cells().keySet()) {
                double distance = local.distanceToSqr(
                        new Vec3(cell.x(), cell.y(), cell.z()));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new GroupHit(id, cell);
                }
            }
        }
        return best;
    }

    private static SurfaceHit nearestSurfaceHit(TransformConstructionSavedData data,
            ResourceLocation dimension, Set<UUID> ids, Vec3 world) {
        SurfaceHit best = null;
        double bestDistance = Double.MAX_VALUE;
        for (UUID id : ids) {
            ConstructionSurface surface = data.surface(id);
            if (surface == null || !surface.dimension().equals(dimension)) continue;
            int columns = surface.columns();
            int rows = surface.rows();
            for (int column = 0; column < columns; column++) {
                double u = (column + 0.5D) / columns;
                for (int row = 0; row < rows; row++) {
                    double v = (row + 0.5D) / rows;
                    double distance = surface.gridPoint(u, v).distanceToSqr(world);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new SurfaceHit(id, new SurfaceSlot(column, row));
                    }
                }
            }
        }
        return best;
    }

    private static Direction dominantDirection(Vec3 delta) {
        double ax = Math.abs(delta.x);
        double ay = Math.abs(delta.y);
        double az = Math.abs(delta.z);
        if (ay >= ax && ay >= az) {
            return delta.y >= 0.0D ? Direction.UP : Direction.DOWN;
        }
        if (ax >= az) return delta.x >= 0.0D ? Direction.EAST : Direction.WEST;
        return delta.z >= 0.0D ? Direction.SOUTH : Direction.NORTH;
    }

    private record PendingSurface(ResourceKey<Level> dimension, Vec3 point) {
    }

    private record GroupHit(UUID groupId, GridPos cell) {
    }

    private record SurfaceHit(UUID surfaceId, SurfaceSlot slot) {
    }

    public record ProxyCell(VoxelShape selection, VoxelShape collision,
            VoxelShape groupCollision, VoxelShape surfaceCollision,
            int light, Set<UUID> groupIds, Set<UUID> surfaceIds) {
    }

    private static final class MutableProxyCell {
        private VoxelShape selection = Shapes.empty();
        private VoxelShape collision = Shapes.empty();
        private VoxelShape groupCollision = Shapes.empty();
        private VoxelShape surfaceCollision = Shapes.empty();
        private int light;
        private ProxyCell frozen;
        private final Set<UUID> groupIds = new LinkedHashSet<>();
        private final Set<UUID> surfaceIds = new LinkedHashSet<>();

        private void add(AABB local, UUID groupId, UUID surfaceId,
                boolean selection, boolean collision, int light) {
            VoxelShape shape = Shapes.create(local);
            if (selection) this.selection = Shapes.or(this.selection, shape);
            if (collision) {
                this.collision = Shapes.or(this.collision, shape);
                if (groupId != null) {
                    this.groupCollision = Shapes.or(this.groupCollision, shape);
                }
                if (surfaceId != null) {
                    this.surfaceCollision = Shapes.or(this.surfaceCollision, shape);
                }
            }
            this.light = Math.max(this.light, Math.max(0, Math.min(15, light)));
            if (groupId != null) groupIds.add(groupId);
            if (surfaceId != null) surfaceIds.add(surfaceId);
            frozen = null;
        }

        private void merge(ProxyCell other) {
            if (other == null) return;
            this.selection = Shapes.or(this.selection, other.selection());
            this.collision = Shapes.or(this.collision, other.collision());
            this.groupCollision = Shapes.or(this.groupCollision,
                    other.groupCollision());
            this.surfaceCollision = Shapes.or(this.surfaceCollision,
                    other.surfaceCollision());
            this.light = Math.max(this.light, other.light());
            this.groupIds.addAll(other.groupIds());
            this.surfaceIds.addAll(other.surfaceIds());
            frozen = null;
        }

        private ProxyCell freeze() {
            if (frozen == null) {
                frozen = new ProxyCell(selection.optimize(), collision.optimize(),
                        groupCollision.optimize(), surfaceCollision.optimize(),
                        light, Set.copyOf(groupIds), Set.copyOf(surfaceIds));
            }
            return frozen;
        }
    }

    private record LogicalPart(int x, int y, int z) {
        private static LogicalPart group(GridPos cell) {
            return new LogicalPart(cell.x(), cell.y(), cell.z());
        }

        private static LogicalPart surface(SurfaceSlot slot) {
            return new LogicalPart(slot.column(), slot.row(), 0);
        }
    }

    private record OwnerKey(ResourceLocation dimension, UUID id,
            boolean surface, LogicalPart part) {
        private static OwnerKey groupCell(ResourceLocation dimension, UUID id,
                GridPos cell) {
            return new OwnerKey(dimension, id, false,
                    LogicalPart.group(cell));
        }

        private static OwnerKey surfaceSlot(ResourceLocation dimension, UUID id,
                SurfaceSlot slot) {
            return new OwnerKey(dimension, id, true,
                    LogicalPart.surface(slot));
        }
    }

    private static final class SpatialIndex {
        private final Map<OwnerKey, Map<Long, MutableProxyCell>> owners =
                new LinkedHashMap<>();
        private final Map<ResourceLocation, Map<Long, Set<OwnerKey>>> cells =
                new LinkedHashMap<>();
        private final Map<ResourceLocation, Map<Long, ProxyCell>> frozen =
                new LinkedHashMap<>();

        private void add(OwnerKey owner, BlockPos pos, AABB local,
                boolean selection, boolean collision, int light) {
            UUID groupId = owner.surface() ? null : owner.id();
            UUID surfaceId = owner.surface() ? owner.id() : null;
            long packed = pos.asLong();
            owners.computeIfAbsent(owner, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(packed, ignored -> new MutableProxyCell())
                    .add(local, groupId, surfaceId, selection, collision, light);
            cells.computeIfAbsent(owner.dimension(),
                            ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(packed, ignored -> new LinkedHashSet<>())
                    .add(owner);
            Map<Long, ProxyCell> cache = frozen.get(owner.dimension());
            if (cache != null) cache.remove(packed);
        }

        private ProxyCell cell(ResourceLocation dimension, BlockPos pos) {
            Map<Long, Set<OwnerKey>> dimensionCells = cells.get(dimension);
            if (dimensionCells == null) return null;
            long packed = pos.asLong();
            Set<OwnerKey> ownerSet = dimensionCells.get(packed);
            if (ownerSet == null || ownerSet.isEmpty()) return null;

            Map<Long, ProxyCell> cache = frozen.computeIfAbsent(dimension,
                    ignored -> new LinkedHashMap<>());
            ProxyCell existing = cache.get(packed);
            if (existing != null) return existing;

            MutableProxyCell aggregate = new MutableProxyCell();
            for (OwnerKey owner : ownerSet) {
                Map<Long, MutableProxyCell> ownerCells = owners.get(owner);
                MutableProxyCell contribution = ownerCells == null ? null
                        : ownerCells.get(packed);
                if (contribution != null) aggregate.merge(contribution.freeze());
            }
            ProxyCell result = aggregate.freeze();
            cache.put(packed, result);
            return result;
        }

        private Set<Long> positions(ResourceLocation dimension) {
            Map<Long, Set<OwnerKey>> dimensionCells = cells.get(dimension);
            return dimensionCells == null ? Set.of()
                    : Set.copyOf(dimensionCells.keySet());
        }

        private Set<Long> ownerPositions(OwnerKey owner) {
            Map<Long, MutableProxyCell> ownerCells = owners.get(owner);
            return ownerCells == null ? Set.of()
                    : Set.copyOf(ownerCells.keySet());
        }

        private Set<OwnerKey> ownerKeys(UUID id, boolean surface) {
            Set<OwnerKey> result = new LinkedHashSet<>();
            for (OwnerKey key : owners.keySet()) {
                if (key.surface() == surface && key.id().equals(id)) {
                    result.add(key);
                }
            }
            return result;
        }

        private void removeOwner(OwnerKey owner) {
            Map<Long, MutableProxyCell> removed = owners.remove(owner);
            if (removed == null || removed.isEmpty()) return;
            Map<Long, Set<OwnerKey>> dimensionCells =
                    cells.get(owner.dimension());
            Map<Long, ProxyCell> cache = frozen.get(owner.dimension());
            for (long packed : removed.keySet()) {
                if (dimensionCells != null) {
                    Set<OwnerKey> set = dimensionCells.get(packed);
                    if (set != null) {
                        set.remove(owner);
                        if (set.isEmpty()) dimensionCells.remove(packed);
                    }
                }
                if (cache != null) cache.remove(packed);
            }
            if (dimensionCells != null && dimensionCells.isEmpty()) {
                cells.remove(owner.dimension());
            }
            if (cache != null && cache.isEmpty()) {
                frozen.remove(owner.dimension());
            }
        }
    }}
