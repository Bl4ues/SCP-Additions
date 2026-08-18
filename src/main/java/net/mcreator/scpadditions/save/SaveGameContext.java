package net.mcreator.scpadditions.save;

/**
 * Short-lived server-thread context used to label respawn-position writes made
 * by SCP Additions itself. Vanilla/modded writes that do not opt in are still
 * captured and classified as generic respawn points.
 */
public final class SaveGameContext {
    private static final ThreadLocal<SaveMethod> CURRENT = new ThreadLocal<>();

    private SaveGameContext() {
    }

    public static SaveMethod current() {
        SaveMethod method = CURRENT.get();
        return method == null ? SaveMethod.RESPAWN_POINT : method;
    }

    public static void run(SaveMethod method, Runnable action) {
        if (action == null) return;
        SaveMethod previous = CURRENT.get();
        if (method == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(method);
        }
        try {
            action.run();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
