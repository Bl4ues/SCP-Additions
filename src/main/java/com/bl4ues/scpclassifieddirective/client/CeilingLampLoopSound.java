package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import com.bl4ues.scpclassifieddirective.facility.UBlocksModule;
import com.bl4ues.scpclassifieddirective.facility.Sl2FacilityPropsModule;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;

/** Positional electrical hum smoothly retargeted to a nearby powered ceiling lamp. */
public final class CeilingLampLoopSound extends AbstractTickableSoundInstance {
    private static final double POSITION_LERP = 0.18D;

    private final ClientLevel level;
    private Vec3 target;
    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean finished;

    public CeilingLampLoopSound(ClientLevel level, BlockPos pos) {
        this(level, Vec3.atCenterOf(pos));
    }

    public CeilingLampLoopSound(ClientLevel level, Vec3 position) {
        super(ScpClassifiedDirectiveModSounds.LAMP_LOOP.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.116F;
        this.pitch = 0.98F + RandomSource.create().nextFloat() * 0.04F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        Vec3 safe = position == null ? Vec3.ZERO : position;
        this.target = safe;
        this.x = this.targetX = safe.x;
        this.y = this.targetY = safe.y;
        this.z = this.targetZ = safe.z;
    }

    ClientLevel level() {
        return level;
    }

    Vec3 target() {
        return target;
    }

    void retarget(Vec3 newTarget) {
        if (newTarget == null) return;
        this.target = newTarget;
        this.targetX = newTarget.x;
        this.targetY = newTarget.y;
        this.targetZ = newTarget.z;
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
        BlockState targetState = level.getBlockState(
                BlockPos.containing(target));
        this.volume = UBlocksModule.isWallDetailLamp(targetState)
                ? 0.0464F : 0.116F;
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
        if (UBlocksModule.isWallDetailLamp(state)) {
            return state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT);
        }
        return Sl2FacilityPropsModule.isRoundLamp(state)
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
