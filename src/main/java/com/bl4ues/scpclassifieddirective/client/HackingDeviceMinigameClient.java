package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.client.gui.HackingDeviceScreen;
import com.bl4ues.scpclassifieddirective.hacking.HackingDevicePuzzle;
import com.bl4ues.scpclassifieddirective.network.HackingDeviceNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Client presentation/input state for the physical hexadecimal checksum breach. */
public final class HackingDeviceMinigameClient {
    public enum Phase {
        LOADING,
        BOOT,
        PUZZLE,
        ROUND_OK,
        DENIED,
        LOCKED,
        SUCCESS,
        COOLDOWN
    }

    private static final long LOADING_NANOS = 1_650_000_000L;
    private static final long BOOT_NANOS = 1_250_000_000L;
    private static final long ROUND_FLASH_NANOS = 620_000_000L;
    private static final long DENIED_FLASH_NANOS = 900_000_000L;
    private static final long LOCKED_NANOS = 1_650_000_000L;
    private static final long SUCCESS_VISUAL_NANOS = 1_200_000_000L;
    private static final long SUCCESS_TIMEOUT_NANOS = 3_000_000_000L;
    private static final long COOLDOWN_ATTACHED_NANOS = 520_000_000L;
    private static final long PROBE_NANOS = 950_000_000L;
    private static final int MAX_PROBES = 2;

    private static BlockPos pos;
    private static int accessLevel;
    private static int round;
    private static int failures;
    private static HackingDevicePuzzle puzzle;
    private static Phase phase = Phase.LOADING;
    private static long phaseStarted;
    private static int selectedIndex;
    private static int probesUsed;
    private static int probeChecksum = -1;
    private static long probeUntil;
    private static boolean waitingForServer;
    private static HackingDevicePuzzle queuedPuzzle;
    private static int queuedRound;
    private static boolean exitSent;
    private static int lastBootCue = -1;
    private static long cooldownEnd;
    private static long readyAt;

    private HackingDeviceMinigameClient() {
    }

    public static void start(BlockPos target, int level, int newRound,
            int newFailures, HackingDevicePuzzle newPuzzle) {
        pos = target == null ? BlockPos.ZERO : target.immutable();
        accessLevel = Math.max(1, Math.min(6, level));
        round = Math.max(1, newRound);
        failures = Math.max(0, newFailures);
        puzzle = newPuzzle;
        selectedIndex = 0;
        probesUsed = 0;
        probeChecksum = -1;
        probeUntil = 0L;
        waitingForServer = false;
        queuedPuzzle = null;
        queuedRound = round;
        exitSent = false;
        lastBootCue = -1;
        cooldownEnd = 0L;
        readyAt = 0L;
        setPhase(Phase.LOADING);
    }

    public static boolean active() {
        return pos != null;
    }

    public static BlockPos pos() {
        return pos;
    }

    public static int accessLevel() {
        return accessLevel;
    }

    public static int round() {
        return round;
    }

    public static int failures() {
        return failures;
    }

    public static HackingDevicePuzzle puzzle() {
        return puzzle;
    }

    public static Phase phase() {
        update();
        return phase;
    }

    public static int selectedIndex() {
        return selectedIndex;
    }

    public static int probesUsed() {
        return probesUsed;
    }

    public static int maxProbes() {
        return MAX_PROBES;
    }

    public static int probeChecksum() {
        return System.nanoTime() < probeUntil ? probeChecksum : -1;
    }

    public static boolean waitingForServer() {
        return waitingForServer;
    }

    public static long cooldownEnd() {
        return cooldownEnd;
    }

    public static long readyAt() {
        return readyAt;
    }

    public static int cooldownSeconds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || cooldownEnd <= 0L) return 0;
        long remaining = cooldownEnd - minecraft.level.getGameTime();
        if (remaining <= 0L) return 0;
        return (int) Math.min(5L, Math.max(1L, (remaining + 19L) / 20L));
    }

    public static double phaseProgress() {
        update();
        long elapsed = Math.max(0L, System.nanoTime() - phaseStarted);
        long duration = switch (phase) {
            case LOADING -> LOADING_NANOS;
            case BOOT -> BOOT_NANOS;
            case ROUND_OK -> ROUND_FLASH_NANOS;
            case DENIED -> DENIED_FLASH_NANOS;
            case LOCKED -> LOCKED_NANOS;
            case SUCCESS -> SUCCESS_VISUAL_NANOS;
            case COOLDOWN -> COOLDOWN_ATTACHED_NANOS;
            default -> 1L;
        };
        return Mth.clamp(elapsed / (double) duration, 0.0D, 1.0D);
    }

    public static int bootLineCount() {
        if (phase() != Phase.BOOT) return 0;
        int count = 1 + (int) Math.floor(phaseProgress() * 4.0D);
        count = Math.max(1, Math.min(4, count));
        if (count != lastBootCue) {
            lastBootCue = count;
            HackingDeviceAudioClient.playBootPulse(pos, count);
        }
        return count;
    }

    public static void moveSelection(int delta) {
        update();
        if (phase != Phase.PUZZLE || waitingForServer || puzzle == null) return;
        selectedIndex = Math.floorMod(selectedIndex + delta, 4);
        probeChecksum = -1;
        HackingDeviceAudioClient.playNavigate(pos);
    }

    public static void probe() {
        update();
        if (phase != Phase.PUZZLE || waitingForServer || puzzle == null
                || probesUsed >= MAX_PROBES) {
            return;
        }
        probeChecksum = puzzle.computedChecksum(selectedIndex);
        probesUsed++;
        probeUntil = System.nanoTime() + PROBE_NANOS;
        HackingDeviceAudioClient.playStatus(pos);
    }

    public static void submit() {
        update();
        if (phase != Phase.PUZZLE || waitingForServer || puzzle == null
                || pos == null) {
            return;
        }
        waitingForServer = true;
        HackingDeviceAudioClient.playConfirm(pos);
        HackingDeviceNetwork.submitCandidate(pos, selectedIndex);
    }

    public static void onResult(HackingDeviceNetwork.ResultKind result,
            int newRound, int newFailures, HackingDevicePuzzle nextPuzzle) {
        if (!active()) return;
        waitingForServer = false;
        failures = Math.max(0, newFailures);
        queuedPuzzle = nextPuzzle;
        queuedRound = Math.max(1, newRound);
        probeChecksum = -1;
        probeUntil = 0L;

        switch (result) {
            case ROUND_OK -> {
                HackingDeviceAudioClient.playConfirm(pos);
                setPhase(Phase.ROUND_OK);
            }
            case DENIED -> {
                HackingDeviceAudioClient.playDenied(pos);
                setPhase(Phase.DENIED);
            }
            case LOCKED -> {
                HackingDeviceAudioClient.playDenied(pos);
                setPhase(Phase.LOCKED);
            }
            case SUCCESS -> {
                HackingDeviceAudioClient.playConfirm(pos);
                setPhase(Phase.SUCCESS);
            }
        }
    }

    public static void beginCooldown(long newCooldownEnd, long newReadyAt) {
        if (!active()) return;
        cooldownEnd = Math.max(0L, newCooldownEnd);
        readyAt = Math.max(cooldownEnd, newReadyAt);
        HackingDeviceAudioClient.playConfirm(pos);
        setPhase(Phase.COOLDOWN);
    }

    public static void requestExit() {
        if (!active() || exitSent) return;
        exitSent = true;
        HackingDeviceNetwork.exitSession(pos);
        HackingDeviceFocusClient.end();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof HackingDeviceScreen) {
            minecraft.setScreen(null);
        }
    }

    public static void clear() {
        pos = null;
        puzzle = null;
        queuedPuzzle = null;
        waitingForServer = false;
        exitSent = false;
        probeChecksum = -1;
        probeUntil = 0L;
        cooldownEnd = 0L;
        readyAt = 0L;
    }

    private static void update() {
        if (!active()) return;
        long elapsed = System.nanoTime() - phaseStarted;
        switch (phase) {
            case LOADING -> {
                if (elapsed >= LOADING_NANOS) {
                    HackingDeviceAudioClient.playStatus(pos);
                    setPhase(Phase.BOOT);
                }
            }
            case BOOT -> {
                if (elapsed >= BOOT_NANOS) setPhase(Phase.PUZZLE);
            }
            case ROUND_OK -> {
                if (elapsed >= ROUND_FLASH_NANOS) {
                    applyQueuedPuzzle();
                    setPhase(Phase.PUZZLE);
                }
            }
            case DENIED -> {
                if (elapsed >= DENIED_FLASH_NANOS) {
                    if (queuedPuzzle != null) puzzle = queuedPuzzle;
                    queuedPuzzle = null;
                    selectedIndex = 0;
                    probesUsed = 0;
                    setPhase(Phase.PUZZLE);
                }
            }
            case LOCKED -> {
                if (elapsed >= LOCKED_NANOS) requestExit();
            }
            case SUCCESS -> {
                if (elapsed >= SUCCESS_TIMEOUT_NANOS) requestExit();
            }
            case COOLDOWN -> {
                if (elapsed >= COOLDOWN_ATTACHED_NANOS) requestExit();
            }
            default -> {
            }
        }
    }

    private static void applyQueuedPuzzle() {
        round = queuedRound;
        if (queuedPuzzle != null) puzzle = queuedPuzzle;
        queuedPuzzle = null;
        selectedIndex = 0;
        probesUsed = 0;
    }

    private static void setPhase(Phase next) {
        phase = next;
        phaseStarted = System.nanoTime();
    }
}
