package net.mcreator.scpadditions.roamer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Shared positional audio helpers for SCP-106 world actions. */
public final class Scp106Audio {
    private Scp106Audio() {
    }

    public static void playPhase(ServerLevel level, Vec3 position,
            float volume) {
        if (level == null || position == null || volume <= 0.0F) return;
        float pitch = 0.94F + level.getRandom().nextFloat() * 0.12F;
        level.playSound(null, position.x, position.y, position.z,
                ScpAdditionsModSounds.SCP_106_PHASE.get(),
                SoundSource.HOSTILE, volume, pitch);
    }
}
