package net.mcreator.scpadditions.client;

import java.util.UUID;

/** Client-only endpoints invoked through the common packet bridge. */
public final class ClientPacketActions {
    private ClientPacketActions() {
    }

    public static void playScareSound() {
        BlinkClient.playScareSound();
    }

    public static void playScp1176Music() {
        Scp1176MusicClient.play();
    }

    public static void playEnterSound() {
        WorldEntrySoundClient.play();
    }

    public static void setScp106ChaseMusic(UUID sourceId, boolean active) {
        Scp106ChaseMusicClient.setActive(sourceId, active);
    }
}
