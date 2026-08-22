package com.bl4ues.scpclassifieddirective.scp1576;

import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticCategory;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticStimulusSystem;
import com.bl4ues.scpclassifieddirective.advancement.ScpAdvancementAwards;
import com.bl4ues.scpclassifieddirective.network.Scp1576Network;
import com.bl4ues.scpclassifieddirective.network.Scp1576StatePacket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server authority for SCP-1576 winding, active windows, and physical sources. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp1576Manager {
    public static final int WIND_TICKS = 20 * 4;
    public static final int VOICE_DELAY_TICKS = 20 * 2;
    public static final int VOICE_TICKS = 20 * 30;
    public static final int ACTIVE_TICKS = VOICE_DELAY_TICKS + VOICE_TICKS;
    public static final int COOLDOWN_TICKS = 20 * 120;

    private static final long ACTIVE_MILLIS = ACTIVE_TICKS * 50L;
    private static final long COOLDOWN_MILLIS = COOLDOWN_TICKS * 50L;
    private static final int SOURCE_SCAN_INTERVAL = 2;
    private static final int NETWORK_SYNC_INTERVAL = 5;
    private static final int ACOUSTIC_INTERVAL = 4;
    private static final int CONTAINER_CHUNK_RADIUS = 2;

    private static final String TAG_SESSION = "Scp1576Session";
    private static final String TAG_ACTIVE_UNTIL = "Scp1576ActiveUntil";
    private static final String TAG_COOLDOWN_UNTIL = "Scp1576CooldownUntil";

    private static final ResourceLocation ADVANCEMENT = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "afterlife_communicator");

    private static final Map<UUID, WindingSession> WINDING =
            new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveSession> ACTIVE =
            new ConcurrentHashMap<>();

    private Scp1576Manager() {
    }

    public static boolean canStart(ItemStack stack) {
        if (!isScp1576(stack)) return false;
        long now = System.currentTimeMillis();
        var tag = stack.getTag();
        if (tag == null) return true;
        return now >= tag.getLong(TAG_ACTIVE_UNTIL)
                && now >= tag.getLong(TAG_COOLDOWN_UNTIL);
    }

    public static boolean beginWinding(ServerPlayer player, InteractionHand hand,
            ItemStack stack) {
        if (player == null || !canStart(stack)
                || WINDING.containsKey(player.getUUID())) {
            return false;
        }

        UUID token = UUID.randomUUID();
        WINDING.put(player.getUUID(), new WindingSession(token, hand));
        Source source = playerSource(player);
        Scp1576Network.sendAll(Scp1576StatePacket.windStart(token,
                player.getUUID(), player.getGameProfile().getName(),
                source.dimension.location(), source.position.x,
                source.position.y, source.position.z));
        return true;
    }

    public static boolean cancelWinding(ServerPlayer player) {
        if (player == null) return false;
        WindingSession session = WINDING.remove(player.getUUID());
        if (session == null) return false;
        Source source = playerSource(player);
        Scp1576Network.sendAll(Scp1576StatePacket.windCancel(session.token,
                player.getUUID(), source.dimension.location(), source.position.x,
                source.position.y, source.position.z));
        return true;
    }

    public static boolean completeWinding(ServerPlayer player, ItemStack stack) {
        if (player == null || !isScp1576(stack)) return false;
        WindingSession winding = WINDING.remove(player.getUUID());
        if (winding == null) return false;

        long now = System.currentTimeMillis();
        long activeUntil = now + ACTIVE_MILLIS;
        long cooldownUntil = activeUntil + COOLDOWN_MILLIS;
        stack.getOrCreateTag().putUUID(TAG_SESSION, winding.token);
        stack.getOrCreateTag().putLong(TAG_ACTIVE_UNTIL, activeUntil);
        stack.getOrCreateTag().putLong(TAG_COOLDOWN_UNTIL, cooldownUntil);
        synchronizeActiveUsable(player, stack);

        Source source = playerSource(player);
        ActiveSession active = new ActiveSession(winding.token,
                player.getUUID(), player.getGameProfile().getName(), now,
                activeUntil, source);
        ACTIVE.put(winding.token, active);

        player.addEffect(new MobEffectInstance(
                Scp1576Module.SCP_1576_EFFECT.get(), ACTIVE_TICKS, 0,
                false, false, false));
        Scp1576Network.sendAll(packet(active,
                Scp1576StatePacket.ACTIVE_START, now));
        return true;
    }

    /**
     * Awards the contact achievement only after a dead voice is actually routed
     * through the communicator to at least one living voice-chat receiver.
     */
    public static void recordContact(MinecraftServer server, UUID sessionId) {
        if (server == null || sessionId == null) return;
        server.execute(() -> {
            ActiveSession session = ACTIVE.get(sessionId);
            if (session == null || session.contactAwarded) return;
            session.contactAwarded = true;
            ServerPlayer player = server.getPlayerList().getPlayer(session.hostId);
            if (player != null) {
                ScpAdvancementAwards.award(player, ADVANCEMENT);
            }
        });
    }

    /** Immutable snapshots consumed from the Simple Voice Chat packet thread. */
    public static List<VoiceSource> voiceSources(MinecraftServer server) {
        if (server == null || ACTIVE.isEmpty()) return List.of();
        List<VoiceSource> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (ActiveSession session : ACTIVE.values()) {
            Source source = session.source;
            if (source == null || now < session.startedAt + 2_000L
                    || now >= session.activeUntil) {
                continue;
            }
            result.add(new VoiceSource(session.token, session.hostId,
                    source.dimension, source.position,
                    elapsedTicks(session, now), remainingTicks(session, now)));
        }
        return List.copyOf(result);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        long now = System.currentTimeMillis();

        for (ActiveSession session : List.copyOf(ACTIVE.values())) {
            if (now >= session.activeUntil) {
                if (ACTIVE.remove(session.token, session)) {
                    Source source = session.source;
                    if (source != null) {
                        Scp1576Network.sendAll(Scp1576StatePacket.active(
                                Scp1576StatePacket.ACTIVE_STOP, session.token,
                                session.hostId, session.hostName,
                                source.dimension.location(), source.position.x,
                                source.position.y, source.position.z, false,
                                ACTIVE_TICKS, 0));
                    }
                }
                continue;
            }

            if (tick % SOURCE_SCAN_INTERVAL == 0) {
                Source located = locate(server, session.token, session.source);
                if (located != null) session.source = located;
            }

            Source source = session.source;
            if (source == null) continue;
            ServerLevel level = server.getLevel(source.dimension);
            if (level == null) continue;

            if (tick % ACOUSTIC_INTERVAL == 0) {
                AcousticStimulusSystem.emit(level, source.position,
                        AcousticCategory.VOICE, 0.85F, null);
            }
            if (tick % NETWORK_SYNC_INTERVAL == 0) {
                Scp1576Network.sendAll(packet(session,
                        Scp1576StatePacket.ACTIVE_UPDATE, now));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WINDING.clear();
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WindingSession winding = WINDING.remove(player.getUUID());
        if (winding == null) return;
        Source source = playerSource(player);
        Scp1576Network.sendAll(Scp1576StatePacket.windCancel(winding.token,
                player.getUUID(), source.dimension.location(), source.position.x,
                source.position.y, source.position.z));
    }

    private static void synchronizeActiveUsable(ServerPlayer player,
            ItemStack activated) {
        IScpInventory inventory = player.getCapability(
                ScpInventoryCapability.INSTANCE).resolve().orElse(null);
        if (inventory == null) return;
        ItemStack active = inventory.getActiveUsable();
        if (!isScp1576(active) || active == activated) return;
        active.setTag(activated.getTag() == null
                ? null : activated.getTag().copy());
        inventory.setActiveUsable(active);
    }

    private static Scp1576StatePacket packet(ActiveSession session, int action,
            long now) {
        Source source = session.source;
        boolean voiceOpen = now >= session.startedAt + 2_000L
                && now < session.activeUntil;
        return Scp1576StatePacket.active(action, session.token, session.hostId,
                session.hostName, source.dimension.location(), source.position.x,
                source.position.y, source.position.z, voiceOpen,
                elapsedTicks(session, now), remainingTicks(session, now));
    }

    private static int elapsedTicks(ActiveSession session, long now) {
        return Math.min(ACTIVE_TICKS, Math.max(0,
                (int) ((now - session.startedAt) / 50L)));
    }

    private static int remainingTicks(ActiveSession session, long now) {
        return Math.min(ACTIVE_TICKS, Math.max(0,
                (int) ((session.activeUntil - now + 49L) / 50L)));
    }

    private static Source locate(MinecraftServer server, UUID token,
            Source previous) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (containsToken(player.getInventory().items, token)
                    || containsToken(player.getInventory().offhand, token)) {
                return playerSource(player);
            }

            IScpInventory scpInventory = player.getCapability(
                    ScpInventoryCapability.INSTANCE).resolve().orElse(null);
            if (scpInventory != null
                    && (containsToken(scpInventory.getInventory(), token)
                    || matchesToken(scpInventory.getActiveUsable(), token))) {
                return playerSource(player);
            }
        }

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity
                        && matchesToken(itemEntity.getItem(), token)) {
                    return new Source(level.dimension(),
                            itemEntity.position().add(0.0D, 0.18D, 0.0D), null);
                }
                if (entity instanceof Container container) {
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        if (matchesToken(container.getItem(slot), token)) {
                            return new Source(level.dimension(),
                                    entity.position().add(0.0D,
                                            entity.getBbHeight() * 0.5D, 0.0D),
                                    null);
                        }
                    }
                }
            }
        }

        Source container = findContainer(server, token, previous);
        return container != null ? container : previous;
    }

    private static Source findContainer(MinecraftServer server, UUID token,
            Source previous) {
        Set<ChunkRef> checked = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -CONTAINER_CHUNK_RADIUS;
                    dx <= CONTAINER_CHUNK_RADIUS; dx++) {
                for (int dz = -CONTAINER_CHUNK_RADIUS;
                        dz <= CONTAINER_CHUNK_RADIUS; dz++) {
                    Source found = scanChunk(player.serverLevel(), center.x + dx,
                            center.z + dz, token, checked);
                    if (found != null) return found;
                }
            }
        }

        if (previous != null && previous.blockPos != null) {
            ServerLevel level = server.getLevel(previous.dimension);
            if (level != null) {
                ChunkPos center = new ChunkPos(previous.blockPos);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Source found = scanChunk(level, center.x + dx,
                                center.z + dz, token, checked);
                        if (found != null) return found;
                    }
                }
            }
        }
        return null;
    }

    private static Source scanChunk(ServerLevel level, int chunkX, int chunkZ,
            UUID token, Set<ChunkRef> checked) {
        ChunkRef ref = new ChunkRef(level.dimension(), chunkX, chunkZ);
        if (!checked.add(ref)) return null;
        ServerChunkCache cache = level.getChunkSource();
        LevelChunk chunk = cache.getChunkNow(chunkX, chunkZ);
        if (chunk == null) return null;

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (!(blockEntity instanceof Container container)
                    || blockEntity.isRemoved()) {
                continue;
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!matchesToken(container.getItem(slot), token)) continue;
                BlockPos pos = blockEntity.getBlockPos();
                return new Source(level.dimension(), Vec3.atCenterOf(pos),
                        pos.immutable());
            }
        }
        return null;
    }

    private static boolean containsToken(List<ItemStack> stacks, UUID token) {
        if (stacks == null || stacks.isEmpty()) return false;
        for (ItemStack stack : stacks) {
            if (matchesToken(stack, token)) return true;
        }
        return false;
    }

    private static boolean matchesToken(ItemStack stack, UUID token) {
        if (!isScp1576(stack) || token == null || stack.getTag() == null
                || !stack.getTag().hasUUID(TAG_SESSION)) {
            return false;
        }
        return token.equals(stack.getTag().getUUID(TAG_SESSION));
    }

    private static boolean isScp1576(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.is(Scp1576Module.SCP_1576.get());
    }

    private static Source playerSource(ServerPlayer player) {
        return new Source(player.level().dimension(),
                player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D),
                null);
    }

    private record WindingSession(UUID token, InteractionHand hand) {
    }

    private static final class ActiveSession {
        private final UUID token;
        private final UUID hostId;
        private final String hostName;
        private final long startedAt;
        private final long activeUntil;
        private volatile Source source;
        private boolean contactAwarded;

        private ActiveSession(UUID token, UUID hostId, String hostName,
                long startedAt, long activeUntil, Source source) {
            this.token = token;
            this.hostId = hostId;
            this.hostName = hostName;
            this.startedAt = startedAt;
            this.activeUntil = activeUntil;
            this.source = source;
        }
    }

    private record Source(ResourceKey<Level> dimension, Vec3 position,
            BlockPos blockPos) {
    }

    private record ChunkRef(ResourceKey<Level> dimension, int x, int z) {
    }

    public record VoiceSource(UUID sessionId, UUID hostId,
            ResourceKey<Level> dimension, Vec3 position, int ageTicks,
            int remainingTicks) {
        public int voiceAgeTicks() {
            return Math.max(0, ageTicks - VOICE_DELAY_TICKS);
        }
    }
}
