package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Conservative redstone adapter for ordinary transformed blocks whose state is
 * represented by POWERED (and optionally LIT). Interactive controls, door-like
 * OPEN blocks and Alarm use their dedicated runtimes instead.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformPoweredBlockRuntime {
    private static final int UPDATE_INTERVAL = 2;
    private static final Map<MinecraftServer, PoweredIndex> INDEXES =
            new WeakHashMap<>();

    private TransformPoweredBlockRuntime() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % UPDATE_INTERVAL != 0) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        PoweredIndex index = INDEXES.get(server);
        if (index == null || index.revision() != data.revision()) {
            index = PoweredIndex.build(data);
            INDEXES.put(server, index);
        }

        for (GroupRef ref : index.groups()) {
            TransformGroup group = data.group(ref.groupId());
            if (group == null) continue;
            BlockState state = group.cells().get(ref.cell());
            if (!eligible(state)) continue;
            ServerLevel level = level(server, group.dimension());
            if (level == null) continue;
            boolean powered = TransformPowerQuery.powered(
                    level, group, ref.cell());
            BlockState updated = poweredState(state, powered);
            if (updated.equals(state)) continue;
            data.putGroupState(group.withCell(ref.cell(), updated));
            TransformConstructionNetwork.broadcastGroupCell(level,
                    group.id(), ref.cell(), updated);
            TransformConstructionManager.refreshGroupCellRuntime(
                    server, group.id(), ref.cell());
        }

        for (SurfaceRef ref : index.surfaces()) {
            ConstructionSurface surface = data.surface(ref.surfaceId());
            if (surface == null) continue;
            ConstructionSurface.SurfaceAttachment attachment =
                    ref.normalSign() == 0
                            ? surface.attachments().get(ref.slot())
                            : surface.overlay(ref.slot(), ref.normalSign());
            if (attachment == null || !eligible(attachment.state())) continue;
            ServerLevel level = level(server, surface.dimension());
            if (level == null) continue;
            boolean powered = TransformPowerQuery.powered(
                    level, surface, ref.slot());
            BlockState updated = poweredState(attachment.state(), powered);
            if (updated.equals(attachment.state())) continue;

            ConstructionSurface next;
            if (ref.normalSign() == 0) {
                next = surface.withAttachment(ref.slot(), updated,
                        attachment.deform());
                TransformConstructionNetwork.broadcastSurfaceSlot(level,
                        surface.id(), ref.slot(), updated, attachment.deform());
            } else {
                next = surface.withOverlay(ref.slot(), ref.normalSign(),
                        updated, attachment.deform());
                TransformConstructionNetwork.broadcastSurfaceOverlay(level,
                        surface.id(), ref.slot(), ref.normalSign(), updated,
                        attachment.deform());
            }
            data.putSurfaceState(next);
            TransformConstructionManager.refreshSurfaceSlotRuntime(
                    server, surface.id(), ref.slot());
        }
    }

    public static synchronized void structuralGroupCellChanged(
            MinecraftServer server, UUID groupId, TransformGroup.GridPos cell) {
        PoweredIndex index = INDEXES.get(server);
        if (server == null || index == null || groupId == null || cell == null) {
            return;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<GroupRef> groups = new ArrayList<>(index.groups());
        groups.removeIf(ref -> ref.groupId().equals(groupId)
                && ref.cell().equals(cell));
        TransformGroup group = data.group(groupId);
        if (group != null && eligible(group.cells().get(cell))) {
            groups.add(new GroupRef(groupId, cell));
        }
        INDEXES.put(server, new PoweredIndex(data.revision(),
                List.copyOf(groups), index.surfaces()));
    }

    public static synchronized void structuralSurfaceSlotChanged(
            MinecraftServer server, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
        PoweredIndex index = INDEXES.get(server);
        if (server == null || index == null || surfaceId == null || slot == null) {
            return;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<SurfaceRef> surfaces = new ArrayList<>(index.surfaces());
        surfaces.removeIf(ref -> ref.surfaceId().equals(surfaceId)
                && ref.slot().equals(slot));
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface != null) {
            ConstructionSurface.SurfaceAttachment main =
                    surface.attachments().get(slot);
            if (main != null && eligible(main.state())) {
                surfaces.add(new SurfaceRef(surfaceId, slot, 0));
            }
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                if (entry.getKey().slot().equals(slot)
                        && eligible(entry.getValue().state())) {
                    surfaces.add(new SurfaceRef(surfaceId, slot,
                            entry.getKey().normalSign()));
                }
            }
        }
        INDEXES.put(server, new PoweredIndex(data.revision(), index.groups(),
                List.copyOf(surfaces)));
    }

    public static synchronized void structuralGroupChanged(
            MinecraftServer server, UUID groupId) {
        PoweredIndex index = INDEXES.get(server);
        if (server == null || index == null || groupId == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<GroupRef> groups = new ArrayList<>(index.groups());
        groups.removeIf(ref -> ref.groupId().equals(groupId));
        TransformGroup group = data.group(groupId);
        if (group != null) {
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (eligible(entry.getValue())) {
                    groups.add(new GroupRef(groupId, entry.getKey()));
                }
            }
        }
        INDEXES.put(server, new PoweredIndex(data.revision(),
                List.copyOf(groups), index.surfaces()));
    }

    public static synchronized void structuralSurfaceChanged(
            MinecraftServer server, UUID surfaceId) {
        PoweredIndex index = INDEXES.get(server);
        if (server == null || index == null || surfaceId == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<SurfaceRef> surfaces = new ArrayList<>(index.surfaces());
        surfaces.removeIf(ref -> ref.surfaceId().equals(surfaceId));
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface != null) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (eligible(entry.getValue().state())) {
                    surfaces.add(new SurfaceRef(surfaceId, entry.getKey(), 0));
                }
            }
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                if (eligible(entry.getValue().state())) {
                    surfaces.add(new SurfaceRef(surfaceId,
                            entry.getKey().slot(),
                            entry.getKey().normalSign()));
                }
            }
        }
        INDEXES.put(server, new PoweredIndex(data.revision(), index.groups(),
                List.copyOf(surfaces)));
    }

    public static synchronized void acknowledgeStructuralRevision(
            MinecraftServer server) {
        PoweredIndex index = INDEXES.get(server);
        if (server == null || index == null) return;
        long revision = TransformConstructionSavedData.get(server).revision();
        if (index.revision() != revision) {
            INDEXES.put(server, new PoweredIndex(revision,
                    index.groups(), index.surfaces()));
        }
    }

    private static ServerLevel level(MinecraftServer server,
            ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (dimension.equals(level.dimension().location())) return level;
        }
        return null;
    }

    private record GroupRef(UUID groupId, TransformGroup.GridPos cell) {
    }

    private record SurfaceRef(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
    }

    private record PoweredIndex(long revision, List<GroupRef> groups,
            List<SurfaceRef> surfaces) {
        private static PoweredIndex build(TransformConstructionSavedData data) {
            List<GroupRef> groups = new ArrayList<>();
            List<SurfaceRef> surfaces = new ArrayList<>();
            for (TransformGroup group : data.groups()) {
                for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                        : group.cells().entrySet()) {
                    if (eligible(entry.getValue())) {
                        groups.add(new GroupRef(group.id(), entry.getKey()));
                    }
                }
            }
            for (ConstructionSurface surface : data.surfaces()) {
                for (Map.Entry<ConstructionSurface.SurfaceSlot,
                        ConstructionSurface.SurfaceAttachment> entry
                        : surface.attachments().entrySet()) {
                    if (eligible(entry.getValue().state())) {
                        surfaces.add(new SurfaceRef(surface.id(),
                                entry.getKey(), 0));
                    }
                }
                for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                        ConstructionSurface.SurfaceAttachment> entry
                        : surface.overlays().entrySet()) {
                    if (eligible(entry.getValue().state())) {
                        surfaces.add(new SurfaceRef(surface.id(),
                                entry.getKey().slot(),
                                entry.getKey().normalSign()));
                    }
                }
            }
            return new PoweredIndex(data.revision(), List.copyOf(groups),
                    List.copyOf(surfaces));
        }
    }

    private static boolean eligible(BlockState state) {
        return state != null
                && state.hasProperty(BlockStateProperties.POWERED)
                && !state.hasProperty(BlockStateProperties.OPEN)
                && !(state.getBlock() instanceof ButtonBlock)
                && !(state.getBlock() instanceof LeverBlock)
                && !AlarmModule.isController(state);
    }

    private static BlockState poweredState(BlockState state, boolean powered) {
        BlockState updated = state.setValue(BlockStateProperties.POWERED, powered);
        if (updated.hasProperty(BlockStateProperties.LIT)) {
            updated = updated.setValue(BlockStateProperties.LIT, powered);
        }
        return updated;
    }

}
