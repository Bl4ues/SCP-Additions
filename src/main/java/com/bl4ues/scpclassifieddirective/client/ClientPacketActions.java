package com.bl4ues.scpclassifieddirective.client;

/** Client-only endpoints invoked through the common packet bridge. */
public final class ClientPacketActions {
    private ClientPacketActions() {
    }

    public static void playScareSound() {
        BlinkClient.playScareSound();
        Scp173EncounterAudioClient.onScare();
    }

    public static void playScp1176Music() {
        Scp1176MusicClient.play();
    }

    public static void startScp106Chase() {
        Scp106ChaseAudioClient.start();
    }

    public static void stopScp106Chase() {
        Scp106ChaseAudioClient.stop();
    }

    public static void playEnterSound() {
        EnterSoundClient.play();
    }
}
