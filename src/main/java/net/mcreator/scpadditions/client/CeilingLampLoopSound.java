package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.mcreator.scpadditions.facility.UBlocksModule;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Positional electrical hum smoothly retargeted to a nearby powered ceiling lamp. */
public final class CeilingLampLoopSound extends AbstractTickableSoundInstance {
    private static final double POSITION_LERP = 0.18D;

    private final ClientLevel level;
    private BlockPos target;
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean finished;

    public CeilingLampLoopSound(ClientLevel level, BlockPos pos) {
        super(ScpAdditionsModSounds.LAMP_LOOP.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.29F;
        this.pitch = 0.98F + RandomSource.create().nextFloat() * 0.04F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.target = pos.immutable();
        this.x = this.targetX = pos.getX() + 0.5D;
        this.y = this.targetY = pos.getY() + 0.5D;
        this.z = this.targetZ = pos.getZ() + 0.5D;
    }

    ClientLevel level() {
        return level;
    }

    BlockPos target() {
        return target;
    }

    void retarget(BlockPos newTarget) {
        this.target = newTarget.immutable();
        this.targetX = target.getX() + 0.5D;
        this.targetY = target.getY() + 0.5D;
        this.targetZ = target.getZ() + 0.5D;
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
    }

    static boolean shouldPlayFor(BlockState state) {
        if (state.is(UBlocksModule.SL1_LAMP.get())) {
            return state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT);
        }
        if (state.is(UBlocksModule.SL1_FLICKERING_LAMP.get())) {
            return state.hasProperty(BlockStateProperties.POWERED)
                    && state.getValue(BlockStateProperties.POWERED);
        }
        return false;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
        stop();
    }
}
