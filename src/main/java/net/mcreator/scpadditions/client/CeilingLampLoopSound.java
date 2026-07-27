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

/** Positional electrical hum retargeted to the nearest powered ceiling lamp. */
public final class CeilingLampLoopSound extends AbstractTickableSoundInstance {
    private final ClientLevel level;
    private BlockPos pos;
    private boolean finished;

    public CeilingLampLoopSound(ClientLevel level, BlockPos pos) {
        super(ScpAdditionsModSounds.LAMP_LOOP.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.58F;
        this.pitch = 0.98F + RandomSource.create().nextFloat() * 0.04F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        retarget(pos);
    }

    ClientLevel level() {
        return level;
    }

    void retarget(BlockPos target) {
        this.pos = target.immutable();
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || minecraft.player == null) finish();
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
