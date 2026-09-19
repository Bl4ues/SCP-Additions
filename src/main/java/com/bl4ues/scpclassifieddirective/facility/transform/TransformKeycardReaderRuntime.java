package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface.SurfaceAttachment;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface.SurfaceSlot;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import com.bl4ues.scpclassifieddirective.keycard.KeycardAccess;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Keycard-reader behavior for transformed local grids.
 *
 * Legacy reader blocks require a real BlockPos for their generated procedures.
 * A transformed grid deliberately has no fake parent-world position, so reader
 * authorization and its short redstone pulse live at the logical address while
 * sound/reach use the visible fixture position.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformKeycardReaderRuntime {
    private static final int ACCEPT_TICKS = 5 * 20;
    private static final int DENIED_TICKS = 60;
    private static final Map<MinecraftServer, Map<ReaderKey, Pending>> PENDING =
            new WeakHashMap<>();

    private TransformKeycardReaderRuntime() {
    }

    public static boolean useGroupCell(ServerPlayer player, UUID groupId,
            GridPos cell) {
        if (player == null || groupId == null || cell == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        TransformGroup group = data.group(groupId);
        if (group == null || !group.dimension().equals(
                level.dimension().location())) return false;
        BlockState state = group.cells().get(cell);
        KeycardReaderLevels.ReaderDescriptor reader =
                normalReader(state);
        if (reader == null) return false;

        GridPos visual = TransformWallFixturePlacement.visualCell(cell, state);
        Vec3 center = group.cellCenter(visual);
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;
        return authorize(level, player, ReaderTarget.group(
                group, cell, state), reader, center);
    }

    public static boolean useSurfaceSlot(ServerPlayer player, UUID surfaceId,
            SurfaceSlot slot) {
        if (player == null || surfaceId == null || slot == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        SurfaceAttachment attachment = surface.attachments().get(slot);
        if (attachment == null) return false;
        KeycardReaderLevels.ReaderDescriptor reader =
                normalReader(attachment.state());
        if (reader == null) return false;

        SurfaceSlot visual = TransformWallFixturePlacement.visualSlot(
                surface, slot, attachment.state(), 1);
        Vec3 center = TransformSurfaceGeometry.cellCenter(
                surface, visual, 1, false);
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;
        return authorize(level, player, ReaderTarget.surface(
                surface, slot, 0, attachment.state()), reader, center);
    }

    public static boolean useSurfaceOverlay(ServerPlayer player, UUID surfaceId,
            SurfaceSlot slot, int normalSign) {
        if (player == null || surfaceId == null || slot == null
                || !(player.level() instanceof ServerLevel level)) return false;
        int side = normalSign < 0 ? -1 : 1;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        SurfaceAttachment attachment = surface.overlay(slot, side);
        if (attachment == null) return false;
        KeycardReaderLevels.ReaderDescriptor reader =
                normalReader(attachment.state());
        if (reader == null) return false;

        SurfaceSlot visual = TransformWallFixturePlacement.visualSlot(
                surface, slot, attachment.state(), side);
        Vec3 center = TransformSurfaceGeometry.cellCenter(
                surface, visual, side, true);
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;
        return authorize(level, player, ReaderTarget.surface(
                surface, slot, side, attachment.state()), reader, center);
    }

    private static KeycardReaderLevels.ReaderDescriptor normalReader(
            BlockState state) {
        KeycardReaderLevels.ReaderDescriptor descriptor =
                state == null ? null : KeycardReaderLevels.describe(state);
        if (descriptor == null || state.getBlock()
                != KeycardReaderLevels.normalBlock(
                        descriptor.level(), descriptor.side())) {
            return null;
        }
        return descriptor;
    }

    private static boolean authorize(ServerLevel level, ServerPlayer player,
            ReaderTarget target, KeycardReaderLevels.ReaderDescriptor reader,
            Vec3 physical) {
        int cardLevel = KeycardAccess.highestLevel(player);
        if (cardLevel <= 0) return false;
        boolean accepted = cardLevel >= reader.level();
        Block block = accepted
                ? KeycardReaderLevels.acceptedBlock(
                        reader.level(), reader.side())
                : KeycardReaderLevels.deniedBlock(
                        reader.level(), reader.side());
        BlockState next = copyProperties(target.state(),
                block.defaultBlockState());
        set(level, target, next);

        play(level, physical, accepted ? "accessgranted" : "accessdenied");
        ReaderKey key = target.key();
        PENDING.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
                .put(key, new Pending(level.getServer().getTickCount()
                        + (accepted ? ACCEPT_TICKS : DENIED_TICKS),
                        reader.level(), reader.side()));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        Map<ReaderKey, Pending> pending = PENDING.get(server);
        if (pending == null || pending.isEmpty()) return;
        int tick = server.getTickCount();
        Iterator<Map.Entry<ReaderKey, Pending>> iterator =
                pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ReaderKey, Pending> entry = iterator.next();
            if (tick < entry.getValue().tick()) continue;
            reset(server, entry.getKey(), entry.getValue());
            iterator.remove();
        }
    }

    private static void reset(MinecraftServer server, ReaderKey key,
            Pending pending) {
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        if (key.groupId() != null) {
            TransformGroup group = data.group(key.groupId());
            if (group == null) return;
            BlockState current = group.cells().get(key.groupCell());
            KeycardReaderLevels.ReaderDescriptor descriptor =
                    current == null ? null : KeycardReaderLevels.describe(current);
            if (!matches(descriptor, pending)) return;
            ServerLevel level = level(server, group.dimension());
            if (level == null) return;
            set(level, ReaderTarget.group(group, key.groupCell(), current),
                    copyProperties(current, KeycardReaderLevels.normalBlock(
                            pending.level(), pending.side()).defaultBlockState()));
            return;
        }

        ConstructionSurface surface = data.surface(key.surfaceId());
        if (surface == null) return;
        SurfaceAttachment attachment = key.normalSign() == 0
                ? surface.attachments().get(key.surfaceSlot())
                : surface.overlay(key.surfaceSlot(), key.normalSign());
        if (attachment == null) return;
        KeycardReaderLevels.ReaderDescriptor descriptor =
                KeycardReaderLevels.describe(attachment.state());
        if (!matches(descriptor, pending)) return;
        ServerLevel level = level(server, surface.dimension());
        if (level == null) return;
        set(level, ReaderTarget.surface(surface, key.surfaceSlot(),
                        key.normalSign(), attachment.state()),
                copyProperties(attachment.state(),
                        KeycardReaderLevels.normalBlock(
                                pending.level(), pending.side())
                                .defaultBlockState()));
    }

    private static boolean matches(
            KeycardReaderLevels.ReaderDescriptor descriptor, Pending pending) {
        return descriptor != null && descriptor.level() == pending.level()
                && descriptor.side() == pending.side();
    }

    private static void set(ServerLevel level, ReaderTarget target,
            BlockState next) {
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        if (target.group() != null) {
            TransformGroup updated = target.group().withCell(
                    target.groupCell(), next);
            data.putGroupState(updated);
            TransformPowerQuery.refreshGroupCell(level.getServer(), updated,
                    target.groupCell());
            TransformConstructionNetwork.broadcastGroupCell(level,
                    updated.id(), target.groupCell(), next);
            return;
        }

        SurfaceAttachment old = target.normalSign() == 0
                ? target.surface().attachments().get(target.surfaceSlot())
                : target.surface().overlay(target.surfaceSlot(),
                        target.normalSign());
        if (old == null) return;
        ConstructionSurface updated;
        if (target.normalSign() == 0) {
            updated = target.surface().withAttachment(target.surfaceSlot(),
                    next, old.deform());
            TransformConstructionNetwork.broadcastSurfaceSlot(level,
                    updated.id(), target.surfaceSlot(), next, old.deform());
        } else {
            updated = target.surface().withOverlay(target.surfaceSlot(),
                    target.normalSign(), next, old.deform());
            TransformConstructionNetwork.broadcastSurfaceOverlay(level,
                    updated.id(), target.surfaceSlot(), target.normalSign(),
                    next, old.deform());
        }
        data.putSurfaceState(updated);
        TransformPowerQuery.refreshSurfaceSlot(level.getServer(), updated,
                target.surfaceSlot());
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        if (from.hasProperty(HorizontalDirectionalBlock.FACING)
                && result.hasProperty(HorizontalDirectionalBlock.FACING)) {
            result = result.setValue(HorizontalDirectionalBlock.FACING,
                    from.getValue(HorizontalDirectionalBlock.FACING));
        }
        if (from.hasProperty(BlockStateProperties.WATERLOGGED)
                && result.hasProperty(BlockStateProperties.WATERLOGGED)) {
            result = result.setValue(BlockStateProperties.WATERLOGGED,
                    from.getValue(BlockStateProperties.WATERLOGGED));
        }
        return result;
    }

    private static void play(ServerLevel level, Vec3 position, String soundId) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, soundId));
        if (sound != null) {
            level.playSound(null, position.x, position.y, position.z,
                    sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private static ServerLevel level(MinecraftServer server,
            ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) return level;
        }
        return null;
    }

    private record Pending(int tick, int level,
            KeycardReaderLevels.Side side) {
    }

    private record ReaderKey(UUID groupId, GridPos groupCell,
            UUID surfaceId, SurfaceSlot surfaceSlot, int normalSign) {
        static ReaderKey group(UUID id, GridPos cell) {
            return new ReaderKey(id, cell, null, null, 0);
        }

        static ReaderKey surface(UUID id, SurfaceSlot slot, int normalSign) {
            return new ReaderKey(null, null, id, slot,
                    normalSign == 0 ? 0 : (normalSign < 0 ? -1 : 1));
        }
    }

    private record ReaderTarget(TransformGroup group, GridPos groupCell,
            ConstructionSurface surface, SurfaceSlot surfaceSlot,
            int normalSign, BlockState state) {
        static ReaderTarget group(TransformGroup group, GridPos cell,
                BlockState state) {
            return new ReaderTarget(group, cell, null, null, 0, state);
        }

        static ReaderTarget surface(ConstructionSurface surface,
                SurfaceSlot slot, int normalSign, BlockState state) {
            return new ReaderTarget(null, null, surface, slot,
                    normalSign == 0 ? 0 : (normalSign < 0 ? -1 : 1), state);
        }

        ReaderKey key() {
            return group != null ? ReaderKey.group(group.id(), groupCell)
                    : ReaderKey.surface(surface.id(), surfaceSlot, normalSign);
        }
    }
}
