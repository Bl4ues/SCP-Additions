package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import com.bl4ues.scpclassifieddirective.block.Scp079AuxiliaryPowerBlock;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;

/** Seamless positional generator loop with quick startup and shutdown fades. */
public final class AuxiliaryGeneratorLoopSound
        extends AbstractTickableSoundInstance {
    private static final float START_VOLUME = 0.015F;
    private static final float STOP_VOLUME = 0.001F;
    private static final float TARGET_VOLUME = 0.22F;
    private static final int FADE_IN_TICKS = 12;
    private static final int FADE_OUT_TICKS = 18;
    private static final double POSITION_LERP = 0.22D;

    private final ClientLevel level;
    private BlockPos target;
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean stopRequested;
    private boolean finished;

    public AuxiliaryGeneratorLoopSound(ClientLevel level, BlockPos pos) {
        super(ScpClassifiedDirectiveModSounds.AUXGEN.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.looping = true;
        this.delay = 0;
        this.volume = START_VOLUME;
        this.pitch = 1.0F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        retarget(pos);
        this.x = targetX;
        this.y = targetY;
        this.z = targetZ;
    }

    ClientLevel level() {
        return level;
    }

    void retarget(BlockPos newTarget) {
        this.target = newTarget.immutable();
        this.targetX = target.getX() + 0.5D;
        this.targetY = target.getY() + 0.5D;
        this.targetZ = target.getZ() + 0.5D;
        this.stopRequested = false;
    }

    void requestStop() {
        stopRequested = true;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || minecraft.player == null) {
            finish();
            return;
        }

        this.x += (targetX - this.x) * POSITION_LERP;
        this.y += (targetY - this.y) * POSITION_LERP;
        this.z += (targetZ - this.z) * POSITION_LERP;

        boolean powered = !stopRequested && level.hasChunkAt(target)
                && shouldPlayFor(level.getBlockState(target));
        if (powered) {
            float step = (TARGET_VOLUME - START_VOLUME) / FADE_IN_TICKS;
            this.volume = Mth.approach(this.volume, TARGET_VOLUME, step);
            return;
        }

        float step = TARGET_VOLUME / FADE_OUT_TICKS;
        this.volume = Mth.approach(this.volume, STOP_VOLUME, step);
        if (this.volume <= STOP_VOLUME + 0.0005F) finish();
    }

    static boolean shouldPlayFor(BlockState state) {
        return state.getBlock() instanceof Scp079AuxiliaryPowerBlock
                && state.hasProperty(Scp079AuxiliaryPowerBlock.POWERED)
                && state.getValue(Scp079AuxiliaryPowerBlock.POWERED);
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
        stop();
    }
}
