package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.MineZeroLoadVotePacket;

/** Client-side state for the optional cooperative MineZero death flow. */
public final class MineZeroClientState {
    private static boolean active;
    private static int livingPlayers;
    private static int deadPlayers;
    private static int votes;
    private static int requiredVotes;
    private static boolean spectating;
    private static boolean restoring;
    private static long spectateChangedAt;
    private static long restoreStartedAt = -1L;

    private MineZeroClientState() {
    }

    public static void receiveState(boolean openScreen, String cause,
            int living, int dead, int voteCount, int required) {
        active = true;
        livingPlayers = Math.max(0, living);
        deadPlayers = Math.max(0, dead);
        votes = Math.max(0, voteCount);
        requiredVotes = Math.max(0, required);

        if (livingPlayers == 0 && spectating) {
            stopSpectating();
        }

        if (openScreen) {
            Minecraft.getInstance().setScreen(
                    ScpDeathScreen.mineZero(Component.literal(
                            cause == null || cause.isBlank()
                                    ? "Unknown cause of death." : cause)));
        }
    }

    public static boolean active() {
        return active;
    }

    public static int livingPlayers() {
        return livingPlayers;
    }

    public static int deadPlayers() {
        return deadPlayers;
    }

    public static int votes() {
        return votes;
    }

    public static int requiredVotes() {
        return requiredVotes;
    }

    public static boolean allDead() {
        return active && deadPlayers > 0 && livingPlayers == 0;
    }

    public static boolean spectating() {
        return spectating && MineZeroSpectateClient.active();
    }

    public static boolean restoring() {
        return restoring;
    }

    public static float spectateLayoutProgress() {
        long elapsed = Math.max(0L, Util.getMillis() - spectateChangedAt);
        float t = net.minecraft.util.Mth.clamp(elapsed / 520.0F,
                0.0F, 1.0F);
        t = t * t * (3.0F - 2.0F * t);
        return spectating ? t : 1.0F - t;
    }

    public static void startSpectating() {
        if (!active || livingPlayers <= 0) return;
        MineZeroSpectateClient.start();
        if (!MineZeroSpectateClient.active()) return;
        spectating = true;
        spectateChangedAt = Util.getMillis();
    }

    public static void stopSpectating() {
        if (!spectating && !MineZeroSpectateClient.active()) return;
        spectating = false;
        spectateChangedAt = Util.getMillis();
        MineZeroSpectateClient.stop();
    }

    public static void cycleSpectatedPlayer(int direction) {
        if (spectating) MineZeroSpectateClient.cycle(direction);
    }

    public static String spectatedName() {
        return MineZeroSpectateClient.targetName();
    }

    public static void orbit(double deltaX, double deltaY) {
        if (spectating) MineZeroSpectateClient.orbit(deltaX, deltaY);
    }

    public static void voteToRestore() {
        if (!allDead() || restoring) return;
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(new MineZeroLoadVotePacket());
    }

    public static void beginRestore() {
        restoring = true;
        restoreStartedAt = Util.getMillis();
        stopSpectating();
        if (Minecraft.getInstance().screen instanceof ScpDeathScreen screen) {
            screen.beginMineZeroRestore();
        }
    }

    public static void finishRestore() {
        restoring = false;
        active = false;
        livingPlayers = 0;
        deadPlayers = 0;
        votes = 0;
        requiredVotes = 0;
        stopSpectating();
        SaveGameClientState.suppressForLoadGame();
        EnterSoundClient.play();
        MineZeroRestoreVisualClient.start();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ScpDeathScreen) {
            minecraft.setScreen(null);
        }
    }

    public static float restoreZoomProgress() {
        if (!restoring || restoreStartedAt < 0L) return 0.0F;
        float t = net.minecraft.util.Mth.clamp(
                (Util.getMillis() - restoreStartedAt) / 600.0F,
                0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
