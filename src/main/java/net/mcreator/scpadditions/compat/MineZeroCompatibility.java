package net.mcreator.scpadditions.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Method;

/**
 * Optional MineZero bridge. No MineZero class is linked at compile time, so the
 * SCP Additions jar remains usable when the mod is absent.
 */
public final class MineZeroCompatibility {
    public static final String MOD_ID = "mine_zero";
    private static final String MANAGER =
            "boomcow.minezero.checkpoint.CheckpointManager";
    private static final String DATA =
            "boomcow.minezero.checkpoint.CheckpointData";

    private static volatile Method setCheckpoint;
    private static volatile Method restoreCheckpoint;
    private static volatile Method checkpointDataGet;
    private static volatile Method getAnchorUuid;
    private static volatile boolean lookupAttempted;
    private static final ThreadLocal<Boolean> RESTORING =
            ThreadLocal.withInitial(() -> false);

    private MineZeroCompatibility() {
    }

    public static boolean installed() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean enabled() {
        return installed() && ModCompatibilityConfig.mineZeroEnabled();
    }

    public static boolean restoring() {
        return Boolean.TRUE.equals(RESTORING.get());
    }

    public static boolean hasCheckpoint(ServerPlayer player) {
        if (!enabled() || player == null) return false;
        try {
            ensureMethods();
            if (checkpointDataGet == null || getAnchorUuid == null) return false;
            Object data = checkpointDataGet.invoke(null, player.serverLevel());
            return data != null && getAnchorUuid.invoke(data) != null;
        } catch (ReflectiveOperationException exception) {
            logFailure("inspect MineZero checkpoint", exception);
            return false;
        }
    }

    public static boolean createCheckpoint(ServerPlayer anchor) {
        if (!enabled() || anchor == null || restoring()) return false;
        try {
            ensureMethods();
            if (setCheckpoint == null) return false;
            setCheckpoint.invoke(null, anchor);
            MineZeroScpCheckpoint.capture(anchor.server);
            return true;
        } catch (ReflectiveOperationException exception) {
            logFailure("create MineZero checkpoint", exception);
            return false;
        }
    }

    public static boolean restoreCheckpoint(ServerPlayer anchor) {
        if (!enabled() || anchor == null || restoring()) return false;
        RESTORING.set(true);
        try {
            ensureMethods();
            if (restoreCheckpoint == null) return false;
            restoreCheckpoint.invoke(null, anchor);
            MineZeroScpCheckpoint.restore(anchor.server);
            return true;
        } catch (ReflectiveOperationException exception) {
            logFailure("restore MineZero checkpoint", exception);
            return false;
        } finally {
            RESTORING.set(false);
        }
    }

    private static synchronized void ensureMethods()
            throws ReflectiveOperationException {
        if (lookupAttempted) return;
        lookupAttempted = true;
        if (!installed()) return;

        Class<?> manager = Class.forName(MANAGER);
        setCheckpoint = manager.getMethod("setCheckpoint", ServerPlayer.class);
        restoreCheckpoint = manager.getMethod("restoreCheckpoint",
                ServerPlayer.class);

        Class<?> data = Class.forName(DATA);
        checkpointDataGet = data.getMethod("get",
                net.minecraft.server.level.ServerLevel.class);
        getAnchorUuid = data.getMethod("getAnchorPlayerUUID");
    }

    private static void logFailure(String operation, Exception exception) {
        ScpAdditionsMod.LOGGER.error("Could not {} through MineZero compatibility",
                operation, exception);
    }
}
