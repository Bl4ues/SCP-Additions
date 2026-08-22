package com.bl4ues.scpclassifieddirective.acoustics;

/**
 * Semantic classes of sound evidence used by sound-driven SCP AI.
 *
 * The base range is deliberately gameplay-oriented rather than a physical
 * sound-pressure simulation: it describes how far an intensity-1 stimulus can
 * matter before listener-specific range scaling and world occlusion are applied.
 */
public enum AcousticCategory {
    FOOTSTEP(10.0F, 0.55F),
    SPRINT(20.0F, 1.00F),
    JUMP(12.0F, 0.75F),
    LAND(16.0F, 1.00F),
    DOOR(18.0F, 1.00F),
    BUTTON(11.0F, 0.70F),
    BLOCK(20.0F, 1.10F),
    INTERACTION(10.0F, 0.55F),
    VOICE(22.0F, 1.05F),
    BREATH(4.5F, 0.35F),
    GASP(16.0F, 1.25F),
    WEAPON(40.0F, 1.50F),
    OTHER(10.0F, 0.50F);

    private final float baseRange;
    private final float salience;

    AcousticCategory(float baseRange, float salience) {
        this.baseRange = baseRange;
        this.salience = salience;
    }

    public float baseRange() {
        return baseRange;
    }

    public float salience() {
        return salience;
    }
}
