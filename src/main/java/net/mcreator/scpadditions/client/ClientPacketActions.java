package net.mcreator.scpadditions.client;


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
}
