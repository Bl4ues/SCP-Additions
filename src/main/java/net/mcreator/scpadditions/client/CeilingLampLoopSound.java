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

/** Quiet positional electrical hum owned by one lit ceiling lamp. */
public final class CeilingLampLoopSound extends AbstractTickableSoundInstance {
    private final ClientLevel level;
    private final BlockPos pos;
    private boolean finished;

    public CeilingLampLoopSound(ClientLevel level, BlockPos pos) {
        super(ScpAdditionsModSounds.LAMP_LOOP.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.pos = pos.immutable();
        this.looping = true;
        this.delay = 0;
        this.volume = 0.32F;
        this.pitch = 0.98F + RandomSource.create().nextFloat() * 0.04F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || !isLitCeilingLamp(level.getBlockState(pos))) {
            finish();
        }
    }

    private static boolean isLitCeilingLamp(BlockState state) {
        return (state.is(UBlocksModule.SL1_LAMP.get())
                || state.is(UBlocksModule.SL1_FLICKERING_LAMP.get()))
                && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT);
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
        stop();
    }
}
