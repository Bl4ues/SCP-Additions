package net.mcreator.scpadditions.scp939;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Optional voice-mimic bridge. The concrete voice capture/playback bridge is
 * installed separately; entity AI can safely call this even without it. */
public final class Scp939MimicryHooks {
    private Scp939MimicryHooks() {
    }

    public static boolean request(ServerLevel level, UUID scp939Id,
            Vec3 position, UUID preferredSpeaker) {
        return false;
    }
}
