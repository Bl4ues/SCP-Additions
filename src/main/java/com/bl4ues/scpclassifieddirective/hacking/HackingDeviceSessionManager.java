package com.bl4ues.scpclassifieddirective.hacking;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
import com.bl4ues.scpclassifieddirective.mixin.ObjectContainmentUnitHackInvoker;
import com.bl4ues.scpclassifieddirective.network.HackingDeviceNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Owns one temporary hacking session per player. Nothing client-side can grant access. */
public final class HackingDeviceSessionManager {
    public static final int TOTAL_ROUNDS = 3;
    public static final int MAX_FAILURES = 3;
    private static final int SUCCESS_GRANT_DELAY_TICKS = 24;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private HackingDeviceSessionManager() {
    }

    public static void start(ServerPlayer player, BlockPos pos,
            InteractionHand hand) {
        if (player == null || pos == null || hand == null
                || !(player.level() instanceof ServerLevel level)
                || !HackingDeviceAttachmentManager.isAttached(level, pos)) {
            return;
        }
        int accessLevel = Math.max(1,
                com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents
                        .configurableLevel(level, pos));
        Session session = new Session(pos.immutable(), level.dimension(), hand,
                accessLevel, 1, 0, createPuzzle(player.getRandom()), false,
                false);
        SESSIONS.put(player.getUUID(), session);
        HackingDeviceNetwork.startSession(player, session.pos,
                session.accessLevel, session.round, session.failures,
                session.puzzle);
    }

    public static boolean hasActiveTarget(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        ResourceKey<Level> dimension = level.dimension();
        for (Session session : SESSIONS.values()) {
            if (session.dimension.equals(dimension)
                    && session.pos.equals(pos)) return true;
        }
        return false;
    }

    public static void submit(ServerPlayer player, BlockPos pos,
            int candidateIndex) {
        if (player == null || pos == null
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.finished || !session.pos.equals(pos)
                || !session.dimension.equals(level.dimension())
                || !HackingDeviceAttachmentManager.isAttached(level, pos)) {
            return;
        }
        int index = Math.max(0, Math.min(3, candidateIndex));
        if (index != session.puzzle.correctIndex()) {
            session.failures++;
            playReaderResult(level, pos, false);
            if (session.failures >= MAX_FAILURES) {
                session.finished = true;
                HackingDeviceNetwork.sessionResult(player, pos,
                        HackingDeviceNetwork.ResultKind.LOCKED,
                        session.round, session.failures, null);
            } else {
                HackingDeviceNetwork.sessionResult(player, pos,
                        HackingDeviceNetwork.ResultKind.DENIED,
                        session.round, session.failures, session.puzzle);
            }
            return;
        }

        if (session.round >= TOTAL_ROUNDS) {
            session.finished = true;
            HackingDeviceNetwork.sessionResult(player, pos,
                    HackingDeviceNetwork.ResultKind.SUCCESS,
                    session.round, session.failures, null);
            BlockPos target = pos.immutable();
            UUID playerId = player.getUUID();
            ScpClassifiedDirectiveMod.queueServerWork(SUCCESS_GRANT_DELAY_TICKS,
                    () -> grantIfStillValid(level, target, playerId));
            return;
        }

        session.round++;
        session.puzzle = createPuzzle(player.getRandom());
        HackingDeviceNetwork.sessionResult(player, pos,
                HackingDeviceNetwork.ResultKind.ROUND_OK,
                session.round, session.failures, session.puzzle);
    }

    public static void exit(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.pos.equals(pos)) return;

        // Once the third frame is valid the breach has succeeded. Exiting during
        // the lock animation must not turn a solved hack back into a failure.
        if (session.finished && session.failures < MAX_FAILURES
                && session.round >= TOTAL_ROUNDS) {
            ServerLevel level = sessionLevel(player, session);
            if (level != null) grantIfStillValid(level, session.pos,
                    player.getUUID());
        }

        SESSIONS.remove(player.getUUID());
        detachSession(player, session, true);
    }

    public static void abort(ServerPlayer player) {
        if (player == null) return;
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        detachSession(player, session, false);
    }

    public static void abortTarget(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        List<UUID> remove = new ArrayList<>();
        for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
            Session session = entry.getValue();
            if (session.dimension.equals(level.dimension())
                    && session.pos.equals(pos)) remove.add(entry.getKey());
        }
        for (UUID id : remove) SESSIONS.remove(id);
    }

    private static void detachSession(ServerPlayer player, Session session,
            boolean playReturnCue) {
        ServerLevel level = sessionLevel(player, session);
        if (level == null) return;
        HackingDeviceAttachmentManager.detach(player, level, session.pos,
                session.hand, playReturnCue);
    }

    private static ServerLevel sessionLevel(ServerPlayer player,
            Session session) {
        MinecraftServer server = player == null ? null : player.getServer();
        return server == null || session == null ? null
                : server.getLevel(session.dimension);
    }

    private static void grantIfStillValid(ServerLevel level, BlockPos pos,
            UUID playerId) {
        Session session = SESSIONS.get(playerId);
        if (session == null || !session.finished || session.granted
                || !session.pos.equals(pos)
                || !session.dimension.equals(level.dimension())
                || !HackingDeviceAttachmentManager.isAttached(level, pos)) {
            return;
        }
        session.granted = true;
        if (level.getBlockEntity(pos)
                instanceof ObjectContainmentUnitModule.UnitBlockEntity unit) {
            ObjectContainmentUnitHackInvoker invoker =
                    (ObjectContainmentUnitHackInvoker) (Object) unit;
            invoker.scpclassifieddirective$playReaderSound(true);
            invoker.scpclassifieddirective$startOpening();
            return;
        }
        KeycardReaderLevels.activateAccepted(level, pos);
    }

    private static void playReaderResult(ServerLevel level, BlockPos pos,
            boolean accepted) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                        accepted ? "accessgranted" : "accessdenied"));
        if (sound != null) {
            level.playSound(null, pos, sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private static HackingDevicePuzzle createPuzzle(RandomSource random) {
        int[] bytes = new int[4];
        for (int index = 0; index < 4; index++) bytes[index] = random.nextInt(256);
        int missing = random.nextInt(4);
        int checksum = bytes[0] ^ bytes[1] ^ bytes[2] ^ bytes[3];
        int correct = bytes[missing] & 0xFF;

        List<Integer> pool = new ArrayList<>();
        pool.add(correct);
        while (pool.size() < 4) {
            int delta = switch (pool.size()) {
                case 1 -> 0x10;
                case 2 -> 0x01;
                default -> 0x11;
            };
            int candidate = (correct ^ delta ^ random.nextInt(4)) & 0xFF;
            if (!pool.contains(candidate)) pool.add(candidate);
        }
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        int[] candidates = new int[4];
        int correctIndex = 0;
        for (int index = 0; index < 4; index++) {
            candidates[index] = pool.get(index);
            if (candidates[index] == correct) correctIndex = index;
        }
        bytes[missing] = 0;
        return new HackingDevicePuzzle(bytes, missing, checksum, candidates,
                correctIndex);
    }

    private static final class Session {
        private final BlockPos pos;
        private final ResourceKey<Level> dimension;
        private final InteractionHand hand;
        private final int accessLevel;
        private int round;
        private int failures;
        private HackingDevicePuzzle puzzle;
        private boolean finished;
        private boolean granted;

        private Session(BlockPos pos, ResourceKey<Level> dimension,
                InteractionHand hand, int accessLevel, int round, int failures,
                HackingDevicePuzzle puzzle, boolean finished, boolean granted) {
            this.pos = pos;
            this.dimension = dimension;
            this.hand = hand;
            this.accessLevel = accessLevel;
            this.round = round;
            this.failures = failures;
            this.puzzle = puzzle;
            this.finished = finished;
            this.granted = granted;
        }
    }
}
