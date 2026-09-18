package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
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
        refresh(level.getServer());
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
        refresh(level.getServer());
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
        refresh(level.getServer());
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
        refresh(level.getServer());
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
        refresh(level.getServer());
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
        Vec3 localHit = TransformMath.worldToLocal(group.origin(), hit,
                group.rotationX(), group.rotationY(), group.rotationZ());
        Vec3 outside = localHit.add(
                Vec3.atLowerCornerOf(outwardLocal.getNormal()).scale(0.501D));
        GridPos hitTarget = nearestGridCell(outside);
        GridPos target = source.isAir() ? sourceCell : hitTarget;
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

        BlockState payload = TransformPlacementStateRuntime.groupPlacementState(
                player, blockItem, group, target, outwardLocal, hit);
        TransformGroup next = group.withCell(target, payload);
        data.putGroup(next);
        refresh(level.getServer());
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

        if (surface.attachments().containsKey(slot)) {
            TransformConstructionNetwork.sendBlockedPlacement(player,
                    BlockPos.containing(center.add(
                            surface.gridNormal(u, v).scale(0.5D))));
            player.displayClientMessage(Component.literal(
                    "That surface cell is already occupied."), true);
            return true;
        }

        BlockState payload = TransformPlacementStateRuntime.surfacePlacementState(
                player, blockItem, surface, slot, hit);
        boolean deform = !payload.hasBlockEntity();
        ConstructionSurface next = surface.withAttachment(slot, payload, deform);
        data.putSurface(next);
        refresh(level.getServer());
        return true;
    }

    public static boolean removeGroup(ServerPlayer player, UUID id) {
        if (!canEdit(player) || id == null || player.getServer() == null) {
            return false;
        }
        boolean changed = TransformConstructionSavedData.get(player.getServer())
                .removeGroup(id);
        if (changed) refresh(player.getServer());
        return changed;
    }

    public static boolean removeSurface(ServerPlayer player, UUID id) {
        if (!canEdit(player) || id == null || player.getServer() == null) {
            return false;
        }
        boolean changed = TransformConstructionSavedData.get(player.getServer())
                .removeSurface(id);
        if (changed) refresh(player.getServer());
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
            refresh(level.getServer());
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
            refresh(level.getServer());
            return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(
                        TransformConstructionModule.getProxy())) return;
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BlockItem)) return;
        if (placeBlock(player, event.getPos(), event.getHitVec(), stack)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
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
                if (next.cells().isEmpty()) data.removeGroup(group.id());
                else data.putGroup(next);
                refresh(level.getServer());
                return;
            }
        }
        SurfaceHit surfaceHit = nearestSurfaceHit(data,
                level.dimension().location(), proxy.surfaceIds(), probe);
        if (surfaceHit != null) {
            ConstructionSurface surface = data.surface(surfaceHit.surfaceId());
            if (surface != null && surface.attachments().containsKey(
                    surfaceHit.slot())) {
                data.putSurface(surface.withoutAttachment(surfaceHit.slot()));
                refresh(level.getServer());
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
                ProxyCell cell = next.cell(dimension, pos);
                BlockState current = level.getBlockState(pos);
                if (cell == null || !materialize(cell)) {
                    if (current.is(TransformConstructionModule.getProxy())) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                net.minecraft.world.level.block.Block.UPDATE_ALL);
                    }
                } else if (current.isAir()
                        || current.is(TransformConstructionModule.getProxy())
                        || current.canBeReplaced()) {
                    level.setBlock(pos,
                            TransformConstructionModule.getProxy()
                                    .defaultBlockState()
                                    .setValue(TransformConstructionModule.LIGHT,
                                            cell.light()),
                            net.minecraft.world.level.block.Block.UPDATE_ALL);
                }
            }
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
            ProxyCell cell = index.cell(dimension, pos);
            BlockState current = level.getBlockState(pos);
            if (cell != null && materialize(cell) && (current.isAir()
                    || current.is(TransformConstructionModule.getProxy())
                    || current.canBeReplaced())) {
                level.setBlock(pos,
                        TransformConstructionModule.getProxy().defaultBlockState()
                                .setValue(TransformConstructionModule.LIGHT,
                                        cell.light()),
                        net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
    }

    private static boolean materialize(ProxyCell cell) {
        return cell != null && (!cell.groupIds().isEmpty()
                || !cell.collision().isEmpty() || cell.light() > 0);
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
        for (Map.Entry<GridPos, BlockState> entry : group.cells().entrySet()) {
            GridPos cell = entry.getKey();
            BlockState state = entry.getValue();
            boolean placeholder = state == null || state.isAir();
            if (placeholder) {
                AABB selection = new AABB(cell.x() - 0.5D, cell.y() - 0.5D,
                        cell.z() - 0.5D, cell.x() + 0.5D, cell.y() + 0.5D,
                        cell.z() + 0.5D);
                addWorldBox(index, group.dimension(), transformedBounds(group,
                                selection), group.id(), null, true, false, 0);
                continue;
            }

            // Authoring selection deliberately stays a clean local 1x1x1 cell.
            // Physical collision below remains derived from the payload VoxelShape.
            // Mixing both made thin wall controls and animated doors almost
            // impossible to select after rotation.
            AABB selection = new AABB(cell.x() - 0.5D, cell.y() - 0.5D,
                    cell.z() - 0.5D, cell.x() + 0.5D, cell.y() + 0.5D,
                    cell.z() + 0.5D);
            addWorldBox(index, group.dimension(),
                    transformedBounds(group, selection), group.id(), null,
                    true, false, state.getLightEmission());

            VoxelShape collision = state.getCollisionShape(
                    EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                    CollisionContext.empty());
            if (collision.isEmpty()) continue;
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
                                    cell.x() - 0.5D + minX + boxX * (sx + 1) * inv,
                                    cell.y() - 0.5D + minY + boxY * (sy + 1) * inv,
                                    cell.z() - 0.5D + minZ + boxZ * (sz + 1) * inv);
                            addWorldBox(index, group.dimension(),
                                    transformedBounds(group, local), group.id(),
                                    null, false, true, state.getLightEmission());
                        }
                    }
                }
            });
        }
    }

    private static void addSurface(SpatialIndex index,
            ConstructionSurface surface) {
        int columns = surface.columns();
        int rows = surface.rows();
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                SurfaceSlot slot = new SurfaceSlot(column, row);
                SurfaceAttachment attachment = surface.attachments().get(slot);
                AABB selection = surfaceSlotBounds(surface, column, row,
                        SURFACE_SELECTION_THICKNESS);
                addWorldBox(index, surface.dimension(), selection, null,
                        surface.id(), true, false, 0);
                if (attachment == null || attachment.state().isAir()) continue;
                for (AABB collision : TransformSurfaceGeometry.collisionBoxes(
                        surface, slot, attachment)) {
                    addWorldBox(index, surface.dimension(), collision, null,
                            surface.id(), false, true,
                            attachment.state().getLightEmission());
                }
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
            ResourceLocation dimension, AABB worldBox, UUID groupId,
            UUID surfaceId, boolean selection, boolean collision, int light) {
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
                    index.add(dimension, pos, local, groupId, surfaceId,
                            selection, collision, light);
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
            int light, Set<UUID> groupIds, Set<UUID> surfaceIds) {
    }

    private static final class MutableProxyCell {
        private VoxelShape selection = Shapes.empty();
        private VoxelShape collision = Shapes.empty();
        private int light;
        private ProxyCell frozen;
        private final Set<UUID> groupIds = new LinkedHashSet<>();
        private final Set<UUID> surfaceIds = new LinkedHashSet<>();

        private void add(AABB local, UUID groupId, UUID surfaceId,
                boolean selection, boolean collision, int light) {
            VoxelShape shape = Shapes.create(local);
            if (selection) this.selection = Shapes.or(this.selection, shape);
            if (collision) this.collision = Shapes.or(this.collision, shape);
            this.light = Math.max(this.light, Math.max(0, Math.min(15, light)));
            if (groupId != null) groupIds.add(groupId);
            if (surfaceId != null) surfaceIds.add(surfaceId);
            frozen = null;
        }

        private ProxyCell freeze() {
            if (frozen == null) {
                frozen = new ProxyCell(selection.optimize(), collision.optimize(),
                        light, Set.copyOf(groupIds), Set.copyOf(surfaceIds));
            }
            return frozen;
        }
    }

    private static final class SpatialIndex {
        private final Map<ResourceLocation, Map<Long, MutableProxyCell>> cells =
                new LinkedHashMap<>();

        private void add(ResourceLocation dimension, BlockPos pos, AABB local,
                UUID groupId, UUID surfaceId, boolean selection,
                boolean collision, int light) {
            cells.computeIfAbsent(dimension, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(pos.asLong(), ignored -> new MutableProxyCell())
                    .add(local, groupId, surfaceId, selection, collision, light);
        }

        private ProxyCell cell(ResourceLocation dimension, BlockPos pos) {
            Map<Long, MutableProxyCell> dimensionCells = cells.get(dimension);
            MutableProxyCell cell = dimensionCells == null ? null
                    : dimensionCells.get(pos.asLong());
            return cell == null ? null : cell.freeze();
        }

        private Set<Long> positions(ResourceLocation dimension) {
            Map<Long, MutableProxyCell> dimensionCells = cells.get(dimension);
            return dimensionCells == null ? Set.of()
                    : Set.copyOf(dimensionCells.keySet());
        }
    }
}
