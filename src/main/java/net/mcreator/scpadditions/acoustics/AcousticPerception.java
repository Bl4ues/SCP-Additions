package net.mcreator.scpadditions.acoustics;

/**
 * A stimulus after listener-relative distance, age and world occlusion have
 * been evaluated. SCP AI should consume this instead of being handed an entity
 * target or a perfect player position.
 */
public record AcousticPerception(
        AcousticStimulus stimulus,
        double distance,
        int occlusionLayers,
        float perceivedIntensity,
        long ageTicks) {
}
