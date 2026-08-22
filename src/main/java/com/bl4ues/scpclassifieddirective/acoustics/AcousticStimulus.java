package com.bl4ues.scpclassifieddirective.acoustics;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Immutable server-side evidence that something audible happened in the world. */
public record AcousticStimulus(
        ResourceKey<Level> dimension,
        Vec3 position,
        AcousticCategory category,
        float intensity,
        long gameTime,
        UUID sourceEntityId) {
}
