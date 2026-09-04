package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;

import java.util.List;

/** Last server snapshot used by the 079 target counter and facility map. */
public final class Scp079TrackingClientState {
    private static int totalLifeforms;
    private static int targets;
    private static int scpSubjects;
    private static List<Scp079PlayableNetwork.TrackerEntry> markers = List.of();

    private Scp079TrackingClientState() {
    }

    public static void update(int total, int currentTargets, int subjects,
            List<Scp079PlayableNetwork.TrackerEntry> entries) {
        totalLifeforms = Math.max(0, total);
        targets = Math.max(0, currentTargets);
        scpSubjects = Math.max(0, subjects);
        markers = entries == null ? List.of() : List.copyOf(entries);
    }

    public static int totalLifeforms() { return totalLifeforms; }
    public static int targets() { return targets; }
    public static int scpSubjects() { return scpSubjects; }
    public static List<Scp079PlayableNetwork.TrackerEntry> markers() {
        return markers;
    }

    public static void clear() {
        totalLifeforms = 0;
        targets = 0;
        scpSubjects = 0;
        markers = List.of();
    }
}
