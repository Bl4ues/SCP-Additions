package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.client.gui.HackingDeviceScreen;
import com.bl4ues.scpclassifieddirective.hacking.HackingDevicePuzzle;
import com.bl4ues.scpclassifieddirective.network.HackingDeviceNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Client presentation/input state for the rotating Chaos Insurgency breach suite. */
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

    private static final long LOADING_NANOS = 1_450_000_000L;
    private static final long BOOT_NANOS = 1_350_000_000L;
    private static final long ROUND_FLASH_NANOS = 700_000_000L;
    private static final long DENIED_FLASH_NANOS = 950_000_000L;
    private static final long LOCKED_NANOS = 1_650_000_000L;
    private static final long SUCCESS_VISUAL_NANOS = 1_250_000_000L;
    private static final long SUCCESS_TIMEOUT_NANOS = 3_000_000_000L;
    private static final long COOLDOWN_ATTACHED_NANOS = 520_000_000L;

    private static BlockPos pos;
    private static int accessLevel;
    private static int round;
    private static int failures;
    private static HackingDevicePuzzle puzzle;
    private static Phase phase = Phase.LOADING;
    private static long phaseStarted;
    private static long puzzleStarted;
    private static boolean waitingForServer;
    private static HackingDevicePuzzle queuedPuzzle;
    private static int queuedRound;
    private static boolean exitSent;
    private static int lastBootCue = -1;
    private static long cooldownEnd;
    private static long readyAt;

    private static int selectedIndex;
    private static int circuitMask;
    private static int checksumPacked;
    private static int firewallGate;
    private static int firewallLane = 1;
    private static int frequencyValue;
    private static int signalTrim;
    private static long holdEnteredNanos;

    private HackingDeviceMinigameClient() {
    }

    public static void start(BlockPos target, int level, int newRound,
            int newFailures, HackingDevicePuzzle newPuzzle) {
        pos = target == null ? BlockPos.ZERO : target.immutable();
        accessLevel = Math.max(1, Math.min(6, level));
        round = Math.max(1, newRound);
        failures = Math.max(0, newFailures);
        puzzle = newPuzzle;
        waitingForServer = false;
        queuedPuzzle = null;
        queuedRound = round;
        exitSent = false;
        lastBootCue = -1;
        cooldownEnd = 0L;
        readyAt = 0L;
        resetPuzzleState();
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

    public static int requiredWins() {
        return accessLevel <= 2 ? 1 : accessLevel <= 4 ? 2 : 3;
    }

    public static HackingDevicePuzzle puzzle() {
        return puzzle;
    }

    public static Phase phase() {
        update();
        return phase;
    }

    public static boolean waitingForServer() {
        return waitingForServer;
    }

    public static int selectedIndex() {
        return selectedIndex;
    }

    public static int circuitMask() {
        return circuitMask;
    }

    public static int checksumPacked() {
        return checksumPacked;
    }

    public static int checksumSum() {
        if (puzzle == null) return 0;
        int count = Math.max(1, Math.min(5, puzzle.data(0, 3)));
        int sum = 0;
        for (int i = 0; i < count; i++) sum += checksumValue(i);
        return sum;
    }

    public static int checksumValue(int index) {
        return checksumPacked >> Math.max(0, index) * 4 & 0xF;
    }

    public static int firewallGate() {
        return firewallGate;
    }

    public static int firewallLane() {
        return firewallLane;
    }

    public static int firewallOpenLane() {
        if (puzzle == null) return 1;
        int periodMs = Math.max(250, puzzle.data(1, 800));
        int seed = puzzle.data(2, 0);
        long elapsedMs = Math.max(0L, System.nanoTime() - puzzleStarted)
                / 1_000_000L;
        long phaseIndex = elapsedMs / periodMs;
        return Math.floorMod(seed + firewallGate * 2 + (int) phaseIndex, 3);
    }

    public static int frequencyValue() {
        return frequencyValue;
    }

    public static float holdMarker() {
        if (puzzle == null) return 50.0F;
        double seconds = Math.max(0L, System.nanoTime() - puzzleStarted)
                / 1_000_000_000.0D;
        double seed = puzzle.data(2, 0) * 0.013D;
        double speed = puzzle.data(5, 900) / 1000.0D;
        double amplitude = puzzle.data(4, 12);
        double drift = Math.sin(seconds * speed * 3.1D + seed) * amplitude
                + Math.sin(seconds * speed * 1.37D + seed * 0.41D)
                * amplitude * 0.32D;
        return (float) Mth.clamp(50.0D + drift + signalTrim, 0.0D, 100.0D);
    }

    public static float holdProgress() {
        if (puzzle == null || holdEnteredNanos == 0L) return 0.0F;
        long required = Math.max(500, puzzle.data(3, 1500)) * 1_000_000L;
        return Mth.clamp((System.nanoTime() - holdEnteredNanos)
                / (float) required, 0.0F, 1.0F);
    }

    public static long puzzleElapsedMs() {
        return Math.max(0L, System.nanoTime() - puzzleStarted) / 1_000_000L;
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
        int count = 1 + (int) Math.floor(phaseProgress() * 5.0D);
        count = Math.max(1, Math.min(5, count));
        if (count != lastBootCue) {
            lastBootCue = count;
            HackingDeviceAudioClient.playBootPulse(pos, count);
        }
        return count;
    }

    public static void moveSelection(int delta) {
        update();
        if (phase != Phase.PUZZLE || waitingForServer || puzzle == null) return;

        switch (puzzle.type()) {
            case CIRCUIT_PATH -> {
                int count = Math.max(1, puzzle.data(0, 3));
                selectedIndex = Math.floorMod(selectedIndex + delta, count);
            }
            case VISUAL_CHECKSUM -> {
                int count = Math.max(1, puzzle.data(0, 3));
                selectedIndex = Math.floorMod(selectedIndex + delta, count);
            }
            case FIREWALL_WINDOWS -> firewallLane = Math.floorMod(
                    firewallLane + delta, 3);
            case HOLD_SIGNAL -> {
                int step = accessLevel <= 2 ? 4 : 3;
                signalTrim = Mth.clamp(signalTrim + delta * step, -55, 55);
            }
            case FREQUENCY_LOCK -> {
                int step = Math.max(1, puzzle.data(3, 1));
                frequencyValue = Mth.clamp(frequencyValue + delta * step,
                        0, 100);
            }
        }
        HackingDeviceAudioClient.playNavigate(pos);
    }

    /** Primary SPACE action. */
    public static void primary() {
        update();
        if (phase != Phase.PUZZLE || waitingForServer || puzzle == null) return;

        switch (puzzle.type()) {
            case CIRCUIT_PATH -> {
                circuitMask ^= 1 << selectedIndex;
                HackingDeviceAudioClient.playStatus(pos);
            }
            case VISUAL_CHECKSUM -> {
                int max = Math.max(1, puzzle.data(1, 3));
                int shift = selectedIndex * 4;
                int current = checksumPacked >> shift & 0xF;
                int next = (current + 1) % (max + 1);
                checksumPacked = checksumPacked & ~(0xF << shift)
                        | next << shift;
                HackingDeviceAudioClient.playStatus(pos);
            }
            case FIREWALL_WINDOWS -> breachFirewallWindow();
            case HOLD_SIGNAL -> HackingDeviceAudioClient.playStatus(pos);
            case FREQUENCY_LOCK -> submit();
        }
    }

    /** Compatibility alias for the old checksum probe input. */
    public static void probe() {
        primary();
    }

    public static void submit() {
        update();
        if (phase != Phase.PUZZLE || waitingForServer || puzzle == null
                || pos == null) {
            return;
        }
        switch (puzzle.type()) {
            case CIRCUIT_PATH -> submitAnswer(circuitMask);
            case VISUAL_CHECKSUM -> submitAnswer(checksumPacked);
            case FREQUENCY_LOCK -> submitAnswer(frequencyValue);
            case FIREWALL_WINDOWS -> HackingDeviceAudioClient.playStatus(pos);
            case HOLD_SIGNAL -> HackingDeviceAudioClient.playStatus(pos);
        }
    }

    public static void onResult(HackingDeviceNetwork.ResultKind result,
            int newRound, int newFailures, HackingDevicePuzzle nextPuzzle) {
        if (!active()) return;
        waitingForServer = false;
        failures = Math.max(0, newFailures);
        queuedPuzzle = nextPuzzle;
        queuedRound = Math.max(1, newRound);

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
        cooldownEnd = 0L;
        readyAt = 0L;
        holdEnteredNanos = 0L;
    }

    private static void breachFirewallWindow() {
        int gates = Math.max(1, puzzle.data(0, 2));
        if (firewallLane != firewallOpenLane()) {
            submitAnswer(0);
            return;
        }
        firewallGate++;
        HackingDeviceAudioClient.playConfirm(pos);
        if (firewallGate >= gates) submitAnswer(1);
    }

    private static void submitAnswer(int answer) {
        if (waitingForServer || pos == null) return;
        waitingForServer = true;
        HackingDeviceAudioClient.playConfirm(pos);
        HackingDeviceNetwork.submitCandidate(pos, answer);
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
            case PUZZLE -> updateActivePuzzle();
            case ROUND_OK -> {
                if (elapsed >= ROUND_FLASH_NANOS) {
                    applyQueuedPuzzle();
                    setPhase(Phase.PUZZLE);
                }
            }
            case DENIED -> {
                if (elapsed >= DENIED_FLASH_NANOS) {
                    applyQueuedPuzzle();
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
        }
    }

    private static void updateActivePuzzle() {
        if (puzzle == null || waitingForServer) return;
        if (puzzle.type() == HackingDevicePuzzle.Type.HOLD_SIGNAL) {
            updateHoldSignal();
        } else if (puzzle.type() == HackingDevicePuzzle.Type.FIREWALL_WINDOWS) {
            int timeoutMs = Math.max(3000, puzzle.data(3, 8000));
            if (puzzleElapsedMs() >= timeoutMs) submitAnswer(0);
        }
    }

    private static void updateHoldSignal() {
        float marker = holdMarker();
        int center = puzzle.data(0, 50);
        int halfWidth = Math.max(2, puzzle.data(1, 10));
        long now = System.nanoTime();
        if (marker >= center - halfWidth && marker <= center + halfWidth) {
            if (holdEnteredNanos == 0L) holdEnteredNanos = now;
            long holdNanos = Math.max(500, puzzle.data(3, 1500)) * 1_000_000L;
            if (now - holdEnteredNanos >= holdNanos) {
                submitAnswer(1);
                return;
            }
        } else {
            holdEnteredNanos = 0L;
        }

        int timeoutMs = Math.max(4000, puzzle.data(6, 9000));
        if (marker <= 0.5F || marker >= 99.5F
                || puzzleElapsedMs() >= timeoutMs) {
            submitAnswer(0);
        }
    }

    private static void applyQueuedPuzzle() {
        round = queuedRound;
        if (queuedPuzzle != null) puzzle = queuedPuzzle;
        queuedPuzzle = null;
        resetPuzzleState();
    }

    private static void resetPuzzleState() {
        selectedIndex = 0;
        firewallGate = 0;
        firewallLane = 1;
        signalTrim = 0;
        holdEnteredNanos = 0L;
        puzzleStarted = System.nanoTime();
        if (puzzle == null) {
            circuitMask = 0;
            checksumPacked = 0;
            frequencyValue = 50;
            return;
        }
        circuitMask = puzzle.data(1, 0);
        checksumPacked = puzzle.data(3, 0);
        frequencyValue = puzzle.data(2, 50);
    }

    private static void setPhase(Phase next) {
        phase = next;
        phaseStarted = System.nanoTime();
        if (next == Phase.PUZZLE) puzzleStarted = phaseStarted;
    }
}
