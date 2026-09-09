package com.bl4ues.scpclassifieddirective.hacking;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Owns one temporary hacking session per player. Unlocks remain server-authoritative. */
public final class HackingDeviceSessionManager {
    public static final int MAX_FAILURES = 3;
    private static final int SUCCESS_GRANT_DELAY_TICKS = 24;
    private static final int AUTO_RETURN_AFTER_GRANT_TICKS = 10;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private HackingDeviceSessionManager() {
    }

    public static int requiredWins(int accessLevel) {
        int level = Math.max(1, Math.min(6, accessLevel));
        return level <= 2 ? 1 : level <= 4 ? 2 : 3;
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
        HackingDevicePuzzle puzzle = createPuzzle(player.getRandom(),
                accessLevel, null);
        Session session = new Session(pos.immutable(), level.dimension(), hand,
                accessLevel, 1, 0, puzzle, false, false);
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

    public static void submit(ServerPlayer player, BlockPos pos, int answer) {
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

        HackingDevicePuzzle previous = session.puzzle;
        if (!isCorrect(previous, answer)) {
            session.failures++;
            playReaderResult(level, pos, false);
            if (session.failures >= MAX_FAILURES) {
                session.finished = true;
                HackingDeviceNetwork.sessionResult(player, pos,
                        HackingDeviceNetwork.ResultKind.LOCKED,
                        session.round, session.failures, null);
            } else {
                session.puzzle = createPuzzle(player.getRandom(),
                        session.accessLevel, previous.type());
                HackingDeviceNetwork.sessionResult(player, pos,
                        HackingDeviceNetwork.ResultKind.DENIED,
                        session.round, session.failures, session.puzzle);
            }
            return;
        }

        if (session.round >= requiredWins(session.accessLevel)) {
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
        session.puzzle = createPuzzle(player.getRandom(), session.accessLevel,
                previous.type());
        HackingDeviceNetwork.sessionResult(player, pos,
                HackingDeviceNetwork.ResultKind.ROUND_OK,
                session.round, session.failures, session.puzzle);
    }

    public static void exit(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.pos.equals(pos)) return;

        if (session.finished && session.failures < MAX_FAILURES
                && session.round >= requiredWins(session.accessLevel)) {
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
                session.hand, playReturnCue, session.countdownEnd,
                session.readyAt);
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

        boolean accepted;
        boolean objectContainmentUnit = level.getBlockEntity(pos)
                instanceof ObjectContainmentUnitModule.UnitBlockEntity;
        if (objectContainmentUnit) {
            ObjectContainmentUnitModule.UnitBlockEntity unit =
                    (ObjectContainmentUnitModule.UnitBlockEntity)
                            level.getBlockEntity(pos);
            ObjectContainmentUnitHackInvoker invoker =
                    (ObjectContainmentUnitHackInvoker) (Object) unit;
            invoker.scpclassifieddirective$playReaderSound(true);
            invoker.scpclassifieddirective$startOpening();
            accepted = true;
        } else {
            accepted = KeycardReaderLevels.activateAccepted(level, pos);
        }
        if (!accepted) return;

        session.granted = true;
        if (objectContainmentUnit) {
            session.countdownEnd = 0L;
            session.readyAt = 0L;
        } else {
            session.countdownEnd = level.getGameTime()
                    + HackingDeviceItem.PASSAGE_COUNTDOWN_TICKS;
            session.readyAt = session.countdownEnd
                    + HackingDeviceItem.BLINK_TOTAL_TICKS;
        }

        ServerPlayer player = level.getServer().getPlayerList()
                .getPlayer(playerId);
        if (player != null) {
            if (!objectContainmentUnit) {
                ItemStack held = player.getItemInHand(session.hand);
                if (held.getItem() instanceof HackingDeviceItem) {
                    HackingDeviceItem.armCooldown(held, session.countdownEnd,
                            session.readyAt);
                }
            }
            HackingDeviceNetwork.beginCooldown(player, pos,
                    session.countdownEnd, session.readyAt);
        }

        BlockPos target = pos.immutable();
        ScpClassifiedDirectiveMod.queueServerWork(AUTO_RETURN_AFTER_GRANT_TICKS,
                () -> returnGrantedDevice(level, target, playerId));
    }

    private static void returnGrantedDevice(ServerLevel level, BlockPos pos,
            UUID playerId) {
        Session session = SESSIONS.get(playerId);
        if (session == null || !session.granted || !session.pos.equals(pos)
                || !session.dimension.equals(level.dimension())) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList()
                .getPlayer(playerId);
        if (player == null) return;
        SESSIONS.remove(playerId);
        detachSession(player, session, true);
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

    private static boolean isCorrect(HackingDevicePuzzle puzzle, int answer) {
        if (puzzle == null) return false;
        return switch (puzzle.type()) {
            case CIRCUIT_PATH -> answer == puzzle.serverAnswer();
            case VISUAL_CHECKSUM -> checksumMatches(puzzle, answer);
            case FIREWALL_WINDOWS, HOLD_SIGNAL -> answer == 1;
            case FREQUENCY_LOCK -> Math.abs(answer - puzzle.data(0, 50))
                    <= puzzle.data(1, 5);
        };
    }

    private static boolean checksumMatches(HackingDevicePuzzle puzzle,
            int packedValues) {
        int count = Math.max(1, Math.min(5, puzzle.data(0, 3)));
        int max = Math.max(1, Math.min(7, puzzle.data(1, 3)));
        int target = puzzle.data(2, 0);
        int sum = 0;
        for (int index = 0; index < count; index++) {
            int value = packedValues >> index * 4 & 0xF;
            if (value > max) return false;
            sum += value;
        }
        return sum == target;
    }

    private static HackingDevicePuzzle createPuzzle(RandomSource random,
            int accessLevel, HackingDevicePuzzle.Type avoid) {
        int difficulty = Math.max(1, Math.min(6, accessLevel));
        HackingDevicePuzzle.Type[] types = HackingDevicePuzzle.Type.values();
        HackingDevicePuzzle.Type type;
        do {
            type = types[random.nextInt(types.length)];
        } while (types.length > 1 && type == avoid);

        return switch (type) {
            case CIRCUIT_PATH -> createCircuit(random, difficulty);
            case VISUAL_CHECKSUM -> createChecksum(random, difficulty);
            case FIREWALL_WINDOWS -> createFirewall(random, difficulty);
            case HOLD_SIGNAL -> createHoldSignal(random, difficulty);
            case FREQUENCY_LOCK -> createFrequency(random, difficulty);
        };
    }

    private static HackingDevicePuzzle createCircuit(RandomSource random,
            int difficulty) {
        int count = Math.min(8, 2 + difficulty);
        int targetMask = random.nextInt(1 << count);
        int initialMask = targetMask;
        int mismatches = Math.min(count, 1 + (difficulty - 1) / 2);
        int flipped = 0;
        while (Integer.bitCount(flipped) < mismatches) {
            flipped |= 1 << random.nextInt(count);
        }
        initialMask ^= flipped;
        return new HackingDevicePuzzle(HackingDevicePuzzle.Type.CIRCUIT_PATH,
                difficulty, new int[]{count, initialMask, targetMask},
                targetMask);
    }

    private static HackingDevicePuzzle createChecksum(RandomSource random,
            int difficulty) {
        int count = difficulty <= 2 ? 3 : difficulty <= 4 ? 4 : 5;
        int max = difficulty <= 2 ? 3 : difficulty <= 4 ? 4 : 5;
        int target = 1 + random.nextInt(Math.max(1, count * max));
        int packed = 0;
        int sum = 0;
        for (int index = 0; index < count; index++) {
            int value = random.nextInt(max + 1);
            packed |= value << index * 4;
            sum += value;
        }
        if (sum == target) {
            int value = packed & 0xF;
            int next = value < max ? value + 1 : value - 1;
            packed = packed & ~0xF | next;
        }
        return new HackingDevicePuzzle(
                HackingDevicePuzzle.Type.VISUAL_CHECKSUM, difficulty,
                new int[]{count, max, target, packed}, 0);
    }

    private static HackingDevicePuzzle createFirewall(RandomSource random,
            int difficulty) {
        int gates = 2 + difficulty / 2;
        int periodMs = Math.max(420, 1100 - (difficulty - 1) * 130);
        int seed = random.nextInt(10000);
        int timeoutMs = Math.max(6500, gates * periodMs * 7);
        return new HackingDevicePuzzle(
                HackingDevicePuzzle.Type.FIREWALL_WINDOWS, difficulty,
                new int[]{gates, periodMs, seed, timeoutMs}, 1);
    }

    private static HackingDevicePuzzle createHoldSignal(RandomSource random,
            int difficulty) {
        int center = 30 + random.nextInt(41);
        int halfWidth = Math.max(7, 15 - difficulty);
        int seed = random.nextInt(10000);
        int holdMs = 900 + difficulty * 250;
        int amplitude = 8 + difficulty * 3;
        int speed = 650 + difficulty * 120;
        int timeoutMs = 12000 - difficulty * 500;
        return new HackingDevicePuzzle(HackingDevicePuzzle.Type.HOLD_SIGNAL,
                difficulty, new int[]{center, halfWidth, seed, holdMs,
                        amplitude, speed, timeoutMs}, 1);
    }

    private static HackingDevicePuzzle createFrequency(RandomSource random,
            int difficulty) {
        int[] tolerances = {10, 8, 7, 5, 4, 3};
        int target = 15 + random.nextInt(71);
        int offset = 10 + difficulty * 4;
        if (random.nextBoolean()) offset = -offset;
        int initial = Math.max(0, Math.min(100, target + offset));
        int step = difficulty <= 2 ? 2 : 1;
        return new HackingDevicePuzzle(HackingDevicePuzzle.Type.FREQUENCY_LOCK,
                difficulty, new int[]{target, tolerances[difficulty - 1],
                        initial, step}, target);
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
        private long countdownEnd;
        private long readyAt;

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
