package com.bl4ues.scpclassifieddirective.scp939;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticCategory;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticPerception;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticStimulus;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticStimulusSystem;

import java.util.Optional;
import java.util.UUID;

/**
 * SCP-939's sound memory and awareness state machine, intentionally detached
 * from vanilla visual targeting.
 *
 * <p>A running target does not merely leave disconnected points behind. Repeated
 * sounds from the same source let the 939 infer a short movement vector and
 * project the next likely sound position. That gives a blind predator competent
 * pursuit without handing it the player's live coordinates.</p>
 */
public final class Scp939AcousticBrain {
    private static final double HEARING_RANGE_MULTIPLIER = 1.16D;
    private static final float MIN_AUDIBLE_INTENSITY = 0.050F;
    private static final float IMMEDIATE_HUNT_INTENSITY = 0.46F;
    private static final float HUNT_CONFIDENCE = 0.68F;
    private static final float CONFIDENCE_DECAY_PER_TICK = 0.0050F;
    private static final float ENVIRONMENT_CONFIDENCE_CAP = 0.52F;

    private static final int EVIDENCE_LOOKBACK_TICKS = 3;
    private static final int ALERT_REACTION_TICKS = 8;
    private static final int HUNT_EVIDENCE_GRACE_TICKS = 30;
    private static final int LOST_SEARCH_TO_SEARCH_TICKS = 82;
    private static final int SEARCH_GIVE_UP_TICKS = 20 * 12;

    private static final double MAX_TRACKED_SPEED_PER_TICK = 0.48D;
    private static final double MAX_PREDICTION_DISTANCE = 2.60D;

    private Scp939AwarenessState state = Scp939AwarenessState.IDLE;
    private Vec3 lastKnownPosition;
    private AcousticCategory lastCategory;
    private UUID lastSourceId;
    private float confidence;
    private long lastEvidenceTick = Long.MIN_VALUE / 4L;
    private long lastBrainTick = Long.MIN_VALUE / 4L;
    private long alertUntilTick = Long.MIN_VALUE / 4L;
    private EvidenceSignature lastEvidenceSignature;

    private UUID trackedSourceId;
    private Vec3 trackedSourcePosition;
    private long trackedSourceTick = Long.MIN_VALUE / 4L;
    private Vec3 trackedVelocity = Vec3.ZERO;

    /**
     * Advances the awareness model once. reachedLastKnownPosition comes from
     * navigation, never from distance to a hidden player.
     */
    public Snapshot tick(ServerLevel level, Vec3 listenerPosition,
            boolean reachedLastKnownPosition) {
        if (level == null || listenerPosition == null) return snapshot(0L);

        long now = level.getGameTime();
        decayConfidence(now);

        long lookback = Math.max(0L, now - EVIDENCE_LOOKBACK_TICKS);
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
        UUID sourceId = stimulus.sourceEntityId();
        boolean environmental = sourceId == null;
        boolean sameSource = !environmental
                && sourceId.equals(lastSourceId)
                && now - lastEvidenceTick <= 60L;

        float categoryWeight = categoryConfidenceWeight(stimulus.category());
        float evidenceStrength = Mth.clamp(
                perception.perceivedIntensity() * 1.22F * categoryWeight,
                0.0F, 1.0F);
        confidence = Mth.clamp(confidence * 0.70F
                + evidenceStrength * 0.60F + (sameSource ? 0.08F : 0.0F),
                0.0F, 1.0F);
        if (environmental) {
            confidence = Math.min(confidence, ENVIRONMENT_CONFIDENCE_CAP);
        }

        updateTrajectory(sourceId, stimulus.position(), now);
        lastKnownPosition = environmental
                ? stimulus.position()
                : predictedPosition(stimulus.position(), stimulus.category());
        lastCategory = stimulus.category();
        lastSourceId = sourceId;
        lastEvidenceTick = now;
    }

    /**
     * Learns direction only from consecutive evidenced positions. Teleports or
     * implausibly large jumps reset the estimate instead of becoming psychic
     * velocity samples.
     */
    private void updateTrajectory(UUID sourceId, Vec3 position, long now) {
        if (sourceId == null || position == null) {
            clearTrajectory();
            return;
        }

        if (!sourceId.equals(trackedSourceId)
                || trackedSourcePosition == null
                || trackedSourceTick <= Long.MIN_VALUE / 8L) {
            trackedSourceId = sourceId;
            trackedSourcePosition = position;
            trackedSourceTick = now;
            trackedVelocity = Vec3.ZERO;
            return;
        }

        long elapsed = now - trackedSourceTick;
        if (elapsed <= 0L || elapsed > 20L) {
            trackedSourcePosition = position;
            trackedSourceTick = now;
            trackedVelocity = trackedVelocity.scale(0.35D);
            return;
        }

        Vec3 delta = position.subtract(trackedSourcePosition);
        Vec3 sample = delta.scale(1.0D / elapsed);
        double horizontalSpeed = Math.sqrt(sample.x * sample.x
                + sample.z * sample.z);
        if (!Double.isFinite(horizontalSpeed)
                || horizontalSpeed > 1.20D) {
            trackedVelocity = Vec3.ZERO;
        } else {
            if (horizontalSpeed > MAX_TRACKED_SPEED_PER_TICK) {
                double scale = MAX_TRACKED_SPEED_PER_TICK / horizontalSpeed;
                sample = new Vec3(sample.x * scale,
                        Mth.clamp(sample.y, -0.22D, 0.22D),
                        sample.z * scale);
            } else {
                sample = new Vec3(sample.x,
                        Mth.clamp(sample.y, -0.22D, 0.22D), sample.z);
            }
            trackedVelocity = trackedVelocity.scale(0.35D)
                    .add(sample.scale(0.65D));
        }

        trackedSourcePosition = position;
        trackedSourceTick = now;
    }

    private Vec3 predictedPosition(Vec3 observed,
            AcousticCategory category) {
        if (observed == null || trackedVelocity.lengthSqr() < 0.00020D) {
            return observed;
        }

        double leadTicks = predictionTicks(category);
        Vec3 prediction = trackedVelocity.scale(leadTicks);
        double horizontal = Math.sqrt(prediction.x * prediction.x
                + prediction.z * prediction.z);
        if (horizontal > MAX_PREDICTION_DISTANCE) {
            double scale = MAX_PREDICTION_DISTANCE / horizontal;
            prediction = new Vec3(prediction.x * scale,
                    prediction.y * Math.min(1.0D, scale),
                    prediction.z * scale);
        }
        return observed.add(prediction);
    }

    private static double predictionTicks(AcousticCategory category) {
        if (category == null) return 1.0D;
        return switch (category) {
            case SPRINT -> 4.5D;
            case FOOTSTEP -> 3.0D;
            case JUMP, LAND -> 3.5D;
            case VOICE, GASP -> 2.5D;
            case WEAPON, BLOCK, DOOR -> 1.5D;
            case BUTTON, INTERACTION, BREATH, OTHER -> 1.0D;
        };
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
            case SPRINT, LAND -> 0.94F;
            case JUMP, BLOCK -> 0.84F;
            case FOOTSTEP -> 0.75F;
            case DOOR -> 0.66F;
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

    private void clearTrajectory() {
        trackedSourceId = null;
        trackedSourcePosition = null;
        trackedSourceTick = Long.MIN_VALUE / 4L;
        trackedVelocity = Vec3.ZERO;
    }

    private void clearToIdle() {
        state = Scp939AwarenessState.IDLE;
        lastKnownPosition = null;
        lastCategory = null;
        lastSourceId = null;
        confidence = 0.0F;
        lastEvidenceTick = Long.MIN_VALUE / 4L;
        lastEvidenceSignature = null;
        clearTrajectory();
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
