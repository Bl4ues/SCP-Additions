package net.mcreator.scpadditions.scp939;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.acoustics.AcousticCategory;
import net.mcreator.scpadditions.acoustics.AcousticPerception;
import net.mcreator.scpadditions.acoustics.AcousticStimulus;
import net.mcreator.scpadditions.acoustics.AcousticStimulusSystem;

import java.util.Optional;
import java.util.UUID;

/**
 * SCP-939's sound memory and awareness state machine, intentionally detached
 * from navigation and Mob targeting.
 *
 * The brain only receives listener-relative acoustic perceptions and remembers
 * the last evidenced position. It never asks for the nearest player and never
 * receives a perfect target position from an encounter director.
 */
public final class Scp939AcousticBrain {
    private static final double HEARING_RANGE_MULTIPLIER = 1.05D;
    private static final float MIN_AUDIBLE_INTENSITY = 0.055F;
    private static final float IMMEDIATE_HUNT_INTENSITY = 0.58F;
    private static final float HUNT_CONFIDENCE = 0.78F;
    private static final float CONFIDENCE_DECAY_PER_TICK = 0.0060F;
    private static final float ENVIRONMENT_CONFIDENCE_CAP = 0.52F;

    private static final int ALERT_REACTION_TICKS = 10;
    private static final int HUNT_EVIDENCE_GRACE_TICKS = 22;
    private static final int LOST_SEARCH_TO_SEARCH_TICKS = 70;
    private static final int SEARCH_GIVE_UP_TICKS = 20 * 12;

    private Scp939AwarenessState state = Scp939AwarenessState.IDLE;
    private Vec3 lastKnownPosition;
    private AcousticCategory lastCategory;
    private UUID lastSourceId;
    private float confidence;
    private long lastEvidenceTick = Long.MIN_VALUE / 4L;
    private long lastBrainTick = Long.MIN_VALUE / 4L;
    private long alertUntilTick = Long.MIN_VALUE / 4L;
    private EvidenceSignature lastEvidenceSignature;

    /**
     * Advances the awareness model once. reachedLastKnownPosition should come
     * from navigation, not from distance to a player.
     */
    public Snapshot tick(ServerLevel level, Vec3 listenerPosition,
            boolean reachedLastKnownPosition) {
        if (level == null || listenerPosition == null) return snapshot(0L);

        long now = level.getGameTime();
        decayConfidence(now);

        long lookback = Math.max(0L, now - 2L);
        Optional<AcousticPerception> perceived =
                AcousticStimulusSystem.loudest(level, listenerPosition,
                        lookback, HEARING_RANGE_MULTIPLIER,
                        MIN_AUDIBLE_INTENSITY);

        boolean receivedNewEvidence = false;
        AcousticPerception evidence = null;
        if (perceived.isPresent()) {
            AcousticPerception candidate = perceived.get();
            EvidenceSignature signature = EvidenceSignature.of(
                    candidate.stimulus());
            if (!signature.equals(lastEvidenceSignature)) {
                evidence = candidate;
                lastEvidenceSignature = signature;
                observe(candidate, now);
                receivedNewEvidence = true;
            }
        }

        transition(now, receivedNewEvidence, evidence,
                reachedLastKnownPosition);
        lastBrainTick = now;
        return snapshot(now);
    }

    private void observe(AcousticPerception perception, long now) {
        AcousticStimulus stimulus = perception.stimulus();
        boolean environmental = stimulus.sourceEntityId() == null;
        boolean sameSource = !environmental
                && stimulus.sourceEntityId().equals(lastSourceId)
                && now - lastEvidenceTick <= 60L;

        float categoryWeight = categoryConfidenceWeight(stimulus.category());
        float evidenceStrength = Mth.clamp(
                perception.perceivedIntensity() * 1.18F * categoryWeight,
                0.0F, 1.0F);
        confidence = Mth.clamp(confidence * 0.68F
                + evidenceStrength * 0.58F + (sameSource ? 0.07F : 0.0F),
                0.0F, 1.0F);
        if (environmental) {
            confidence = Math.min(confidence, ENVIRONMENT_CONFIDENCE_CAP);
        }

        lastKnownPosition = stimulus.position();
        lastCategory = stimulus.category();
        lastSourceId = stimulus.sourceEntityId();
        lastEvidenceTick = now;
    }

    private void transition(long now, boolean newEvidence,
            AcousticPerception evidence, boolean reachedLastKnownPosition) {
        boolean strongEvidence = evidence != null
                && canConfirmPrey(evidence.stimulus())
                && evidence.perceivedIntensity() >= IMMEDIATE_HUNT_INTENSITY;
        boolean huntEvidence = strongEvidence
                || (lastSourceId != null && confidence >= HUNT_CONFIDENCE);
        long evidenceAge = evidenceAge(now);

        switch (state) {
            case IDLE -> {
                if (newEvidence) {
                    state = huntEvidence
                            ? Scp939AwarenessState.CONFIRMED_HUNT
                            : Scp939AwarenessState.HEARD_SOUND;
                    alertUntilTick = now + ALERT_REACTION_TICKS;
                }
            }
            case HEARD_SOUND -> {
                if (newEvidence && huntEvidence) {
                    state = Scp939AwarenessState.CONFIRMED_HUNT;
                } else if (now >= alertUntilTick) {
                    state = Scp939AwarenessState.INVESTIGATE;
                }
            }
            case INVESTIGATE -> {
                if (newEvidence && huntEvidence) {
                    state = Scp939AwarenessState.CONFIRMED_HUNT;
                } else if (reachedLastKnownPosition) {
                    state = Scp939AwarenessState.SEARCH;
                }
            }
            case SEARCH -> {
                if (newEvidence) {
                    state = huntEvidence
                            ? Scp939AwarenessState.CONFIRMED_HUNT
                            : Scp939AwarenessState.INVESTIGATE;
                } else if (evidenceAge >= SEARCH_GIVE_UP_TICKS) {
                    clearToIdle();
                }
            }
            case CONFIRMED_HUNT -> {
                if (!newEvidence
                        && evidenceAge > HUNT_EVIDENCE_GRACE_TICKS) {
                    state = Scp939AwarenessState.LOST_SEARCH;
                }
            }
            case LOST_SEARCH -> {
                if (newEvidence) {
                    state = huntEvidence
                            ? Scp939AwarenessState.CONFIRMED_HUNT
                            : Scp939AwarenessState.INVESTIGATE;
                } else if (reachedLastKnownPosition
                        || evidenceAge >= LOST_SEARCH_TO_SEARCH_TICKS) {
                    state = Scp939AwarenessState.SEARCH;
                }
            }
        }
    }

    private static boolean canConfirmPrey(AcousticStimulus stimulus) {
        if (stimulus == null || stimulus.sourceEntityId() == null) return false;
        return switch (stimulus.category()) {
            case SPRINT, JUMP, LAND, VOICE, GASP, WEAPON, BLOCK, DOOR -> true;
            case FOOTSTEP, BUTTON, INTERACTION, BREATH, OTHER -> false;
        };
    }

    private static float categoryConfidenceWeight(AcousticCategory category) {
        if (category == null) return 0.60F;
        return switch (category) {
            case VOICE, GASP, WEAPON -> 1.00F;
            case SPRINT, LAND -> 0.92F;
            case JUMP, BLOCK -> 0.82F;
            case FOOTSTEP -> 0.72F;
            case DOOR -> 0.64F;
            case BUTTON, INTERACTION -> 0.52F;
            case BREATH -> 0.46F;
            case OTHER -> 0.40F;
        };
    }

    private void decayConfidence(long now) {
        if (lastBrainTick <= Long.MIN_VALUE / 8L || now <= lastBrainTick) return;
        long elapsed = Math.min(200L, now - lastBrainTick);
        confidence = Math.max(0.0F,
                confidence - elapsed * CONFIDENCE_DECAY_PER_TICK);
    }

    private long evidenceAge(long now) {
        if (lastEvidenceTick <= Long.MIN_VALUE / 8L) return Long.MAX_VALUE;
        return Math.max(0L, now - lastEvidenceTick);
    }

    private void clearToIdle() {
        state = Scp939AwarenessState.IDLE;
        lastKnownPosition = null;
        lastCategory = null;
        lastSourceId = null;
        confidence = 0.0F;
        lastEvidenceTick = Long.MIN_VALUE / 4L;
        lastEvidenceSignature = null;
    }

    public void reset() {
        clearToIdle();
        lastBrainTick = Long.MIN_VALUE / 4L;
        alertUntilTick = Long.MIN_VALUE / 4L;
    }

    public Scp939AwarenessState state() {
        return state;
    }

    public Vec3 lastKnownPosition() {
        return lastKnownPosition;
    }

    public float confidence() {
        return confidence;
    }

    public Snapshot snapshot(long now) {
        return new Snapshot(state, lastKnownPosition, confidence,
                lastCategory, lastSourceId, evidenceAge(now));
    }

    public record Snapshot(
            Scp939AwarenessState state,
            Vec3 lastKnownPosition,
            float confidence,
            AcousticCategory lastCategory,
            UUID lastSourceId,
            long evidenceAgeTicks) {
    }

    private record EvidenceSignature(
            long gameTime,
            AcousticCategory category,
            UUID sourceId,
            long xBits,
            long yBits,
            long zBits) {
        private static EvidenceSignature of(AcousticStimulus stimulus) {
            return new EvidenceSignature(stimulus.gameTime(),
                    stimulus.category(), stimulus.sourceEntityId(),
                    Double.doubleToLongBits(stimulus.position().x),
                    Double.doubleToLongBits(stimulus.position().y),
                    Double.doubleToLongBits(stimulus.position().z));
        }
    }
}
