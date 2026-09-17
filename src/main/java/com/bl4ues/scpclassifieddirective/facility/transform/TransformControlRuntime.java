package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Physical button/lever interaction for transformed block payloads. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformControlRuntime {
    private static final double MAX_DISTANCE_SQR = 2.25D;
    private static final int BUTTON_TICKS = 20;
    private static final Map<MinecraftServer, Map<ControlKey, Integer>> RELEASES =
            new WeakHashMap<>();

    private TransformControlRuntime() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || event.getItemStack().getItem() instanceof BlockItem
                || !level.getBlockState(event.getPos()).is(
                        TransformConstructionModule.getProxy())) return;
        ControlHit hit = nearest(level, event.getHitVec().getLocation());
        if (hit == null) return;
        BlockState state = hit.state();
        if (!state.hasProperty(BlockStateProperties.POWERED)) return;

        if (state.getBlock() instanceof ButtonBlock) {
            if (state.getValue(BlockStateProperties.POWERED)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            set(level, hit, state.setValue(BlockStateProperties.POWERED, true));
            Vec3 center = hit.center();
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS,
                    0.3F, 0.6F);
            RELEASES.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
                    .put(hit.key(), level.getServer().getTickCount() + BUTTON_TICKS);
        } else if (state.getBlock() instanceof LeverBlock) {
            boolean powered = !state.getValue(BlockStateProperties.POWERED);
            set(level, hit, state.setValue(BlockStateProperties.POWERED, powered));
            Vec3 center = hit.center();
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                    0.3F, powered ? 0.6F : 0.5F);
        } else {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        Map<ControlKey, Integer> releases = RELEASES.get(server);
        if (releases == null || releases.isEmpty()) return;
        int tick = server.getTickCount();
        Iterator<Map.Entry<ControlKey, Integer>> iterator =
                releases.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ControlKey, Integer> entry = iterator.next();
            if (tick < entry.getValue()) continue;
            release(server, entry.getKey());
            iterator.remove();
        }
    }

    private static void release(MinecraftServer server, ControlKey key) {
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                server);
        if (key.groupId() != null) {
            TransformGroup group = data.group(key.groupId());
            if (group == null) return;
            ServerLevel level = level(server, group.dimension());
            BlockState state = group.cells().get(key.gridPos());
            if (level == null || state == null
                    || !(state.getBlock() instanceof ButtonBlock)
                    || !state.hasProperty(BlockStateProperties.POWERED)
                    || !state.getValue(BlockStateProperties.POWERED)) return;
            BlockState next = state.setValue(BlockStateProperties.POWERED, false);
            TransformGroup updated = group.withCell(key.gridPos(), next);
            data.putGroupState(updated);
            TransformConstructionNetwork.broadcastGroupCell(level, group.id(),
                    key.gridPos(), next);
            Vec3 center = group.cellCenter(key.gridPos());
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS,
                    0.3F, 0.5F);
            return;
        }
        ConstructionSurface surface = data.surface(key.surfaceId());
        if (surface == null) return;
        ServerLevel level = level(server, surface.dimension());
        ConstructionSurface.SurfaceAttachment attachment =
                surface.attachments().get(key.slot());
        if (level == null || attachment == null
                || !(attachment.state().getBlock() instanceof ButtonBlock)
                || !attachment.state().hasProperty(BlockStateProperties.POWERED)
                || !attachment.state().getValue(BlockStateProperties.POWERED)) {
            return;
        }
        BlockState nextState = attachment.state().setValue(
                BlockStateProperties.POWERED, false);
        ConstructionSurface updated = surface.withAttachment(key.slot(),
                nextState, attachment.deform());
        data.putSurfaceState(updated);
        TransformConstructionNetwork.broadcastSurfaceSlot(level, surface.id(),
                key.slot(), nextState, attachment.deform());
        Vec3 center = surfaceCenter(surface, key.slot());
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS,
                0.3F, 0.5F);
    }

    private static void set(ServerLevel level, ControlHit hit,
            BlockState state) {
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        if (hit.group() != null) {
            TransformGroup updated = hit.group().withCell(hit.gridPos(), state);
            data.putGroupState(updated);
            TransformConstructionNetwork.broadcastGroupCell(level,
                    hit.group().id(), hit.gridPos(), state);
        } else {
            ConstructionSurface.SurfaceAttachment old = hit.surface()
                    .attachments().get(hit.slot());
            if (old == null) return;
            ConstructionSurface updated = hit.surface().withAttachment(hit.slot(),
                    state, old.deform());
            data.putSurfaceState(updated);
            TransformConstructionNetwork.broadcastSurfaceSlot(level,
                    hit.surface().id(), hit.slot(), state, old.deform());
        }
    }

    private static ControlHit nearest(ServerLevel level, Vec3 world) {
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        var dimension = level.dimension().location();
        ControlHit best = null;
        double bestDistance = MAX_DISTANCE_SQR;
        for (TransformGroup group : data.groups()) {
            if (!group.dimension().equals(dimension)) continue;
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (!control(entry.getValue())) continue;
                Vec3 center = group.cellCenter(entry.getKey());
                double distance = center.distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = ControlHit.group(group, entry.getKey(),
                            entry.getValue(), center);
                }
            }
        }
        for (ConstructionSurface surface : data.surfaces()) {
            if (!surface.dimension().equals(dimension)) continue;
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (!control(entry.getValue().state())) continue;
                Vec3 center = surfaceCenter(surface, entry.getKey());
                double distance = center.distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = ControlHit.surface(surface, entry.getKey(),
                            entry.getValue().state(), center);
                }
            }
        }
        return best;
    }

    private static boolean control(BlockState state) {
        return state != null && (state.getBlock() instanceof ButtonBlock
                || state.getBlock() instanceof LeverBlock);
    }

    private static Vec3 surfaceCenter(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        return surface.gridPoint(u, v)
                .add(surface.gridNormal(u, v).scale(0.5D));
    }

    private static ServerLevel level(MinecraftServer server,
            net.minecraft.resources.ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) return level;
        }
        return null;
    }

    private record ControlKey(UUID groupId, TransformGroup.GridPos gridPos,
            UUID surfaceId, ConstructionSurface.SurfaceSlot slot) {
        private static ControlKey group(UUID id, TransformGroup.GridPos pos) {
            return new ControlKey(id, pos, null, null);
        }

        private static ControlKey surface(UUID id,
                ConstructionSurface.SurfaceSlot slot) {
            return new ControlKey(null, null, id, slot);
        }
    }

    private record ControlHit(TransformGroup group, TransformGroup.GridPos gridPos,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            BlockState state, Vec3 center) {
        private static ControlHit group(TransformGroup group,
                TransformGroup.GridPos pos, BlockState state, Vec3 center) {
            return new ControlHit(group, pos, null, null, state, center);
        }

        private static ControlHit surface(ConstructionSurface surface,
                ConstructionSurface.SurfaceSlot slot, BlockState state,
                Vec3 center) {
            return new ControlHit(null, null, surface, slot, state, center);
        }

        private ControlKey key() {
            return group != null ? ControlKey.group(group.id(), gridPos)
                    : ControlKey.surface(surface.id(), slot);
        }
    }
}
