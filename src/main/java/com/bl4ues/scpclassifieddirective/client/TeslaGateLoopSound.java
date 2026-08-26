package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.block.entity.TeslaGateBlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 20-second positional electrical ambience for one powered Tesla Gate. */
public final class TeslaGateLoopSound extends AbstractTickableSoundInstance {
    private static final float START_VOLUME = 0.012F;
    private static final double MAX_DISTANCE = 20.0D;
    private static final double SENSOR_EDGE_DISTANCE = 1.6D;
    private static final double FULL_APPROACH_DISTANCE = 6.0D;

    private final ClientLevel level;
    private final BlockPos pos;
    private boolean finished;

    TeslaGateLoopSound(ClientLevel level, BlockPos pos) {
        super(ScpClassifiedDirectiveModSounds.TESLA_LOOP.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.pos = pos.immutable();
        this.looping = true;
        this.delay = 0;
        this.volume = START_VOLUME; // Minecraft discards loops that begin at zero.
        this.pitch = 1.0F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 1.75D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || minecraft.player == null) {
            finish();
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TeslaGateBlockEntity gate)
                || !gate.isPowered()) {
            finish();
            return;
        }

        double distance = minecraft.player.distanceToSqr(
                pos.getX() + 0.5D, pos.getY() + 1.7D, pos.getZ() + 0.5D);
        if (distance > MAX_DISTANCE * MAX_DISTANCE) {
            finish();
            return;
        }

        double linearDistance = Math.sqrt(distance);
        float proximity = Mth.clamp((float) ((FULL_APPROACH_DISTANCE
                - Math.max(0.0D, linearDistance - SENSOR_EDGE_DISTANCE))
                / FULL_APPROACH_DISTANCE), 0.0F, 1.0F);
        float spatialTarget = 0.055F + proximity * proximity * 0.72F;
        float recovery = gate.getRecoveryFactor();
        float target = Math.max(0.008F, spatialTarget * recovery);

        // Fast enough to react to movement, slow enough that the 20 s loop never
        // pumps audibly between adjacent ticks or snaps back after a discharge.
        float smoothing = target < volume ? 0.18F : 0.045F;
        volume += (target - volume) * smoothing;
    }

    boolean isFinished() {
        return finished;
    }

    ClientLevel level() {
        return level;
    }

    BlockPos pos() {
        return pos;
    }

    void finish() {
        if (finished) return;
        finished = true;
        stop();
        TeslaGateAudioClient.onLoopFinished(this);
    }
}
