package com.bl4ues.scpclassifieddirective.block.entity;

import com.bl4ues.scpclassifieddirective.data.Scp914RecipeManager;
import com.bl4ues.scpclassifieddirective.scp914.Scp914CycleProcessor;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/** Authoritative state and animation host for the rebuilt SCP-914. */
public final class Scp914BlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int REFINING_TICKS = 300;
    public static final int DOOR_CLOSE_SOUND_TICK = 27;
    public static final int PROCESS_AND_OPEN_TICK = 209;
    public static final float ROUGH_ANGLE = -90.0F;
    public static final float COARSE_ANGLE = -45.0F;
    public static final float ONE_TO_ONE_ANGLE = 0.0F;
    public static final float FINE_ANGLE = 45.0F;
    public static final float VERY_FINE_ANGLE = 90.0F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation REFINING = RawAnimation.begin().thenPlay("refining");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private boolean refining;
    private long refiningStartGameTime;
    private boolean closeSoundPlayed;
    private boolean cycleProcessed;
    private boolean openSoundPlayed;
    private float dialAngle = ONE_TO_ONE_ANGLE;
    private Setting setting = Setting.ONE_TO_ONE;

    public Scp914BlockEntity(BlockPos pos, BlockState state) {
        super(Scp914Module.SCP_914_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            Scp914BlockEntity blockEntity) {
        if (level.isClientSide || !blockEntity.refining
                || !(level instanceof ServerLevel serverLevel)) return;

        long elapsed = level.getGameTime() - blockEntity.refiningStartGameTime;
        Direction front = Scp914Structure.facing(state);

        if (!blockEntity.closeSoundPlayed
                && elapsed >= DOOR_CLOSE_SOUND_TICK) {
            blockEntity.closeSoundPlayed = true;
            playAt(serverLevel, Scp914Structure.intakeDoorCenter(pos, front),
                    Scp914Module.CLOSE.get(), 1.25F);
            playAt(serverLevel, Scp914Structure.outputDoorCenter(pos, front),
                    Scp914Module.CLOSE.get(), 1.25F);
            blockEntity.sync();
        }

        if (!blockEntity.cycleProcessed && elapsed >= PROCESS_AND_OPEN_TICK) {
            blockEntity.cycleProcessed = true;
            Scp914CycleProcessor.process(serverLevel, pos, front,
                    blockEntity.recipeSetting());
            blockEntity.sync();
        }

        if (!blockEntity.openSoundPlayed && elapsed >= PROCESS_AND_OPEN_TICK) {
            blockEntity.openSoundPlayed = true;
            playAt(serverLevel, Scp914Structure.intakeDoorCenter(pos, front),
                    Scp914Module.OPEN.get(), 1.25F);
            playAt(serverLevel, Scp914Structure.outputDoorCenter(pos, front),
                    Scp914Module.OPEN.get(), 1.25F);
            blockEntity.sync();
        }

        if (elapsed >= REFINING_TICKS) {
            blockEntity.refining = false;
            blockEntity.refiningStartGameTime = 0L;
            blockEntity.closeSoundPlayed = false;
            blockEntity.cycleProcessed = false;
            blockEntity.openSoundPlayed = false;
            blockEntity.sync();
        }
    }

    public boolean beginRefining() {
        if (!(level instanceof ServerLevel serverLevel) || refining) return false;
        refining = true;
        refiningStartGameTime = level.getGameTime();
        closeSoundPlayed = false;
        cycleProcessed = false;
        openSoundPlayed = false;
        Direction front = Scp914Structure.facing(getBlockState());
        playAt(serverLevel,
                Scp914Structure.machineSoundCenter(worldPosition, front),
                Scp914Module.WIND.get(), 1.15F);
        playAt(serverLevel,
                Scp914Structure.machineSoundCenter(worldPosition, front),
                Scp914Module.REFINING.get(), 2.0F);
        sync();
        return true;
    }

    private static void playAt(ServerLevel level, Vec3 position,
            SoundEvent sound, float volume) {
        level.playSound(null, position.x, position.y, position.z, sound,
                SoundSource.BLOCKS, volume, 1.0F);
    }

    private Scp914RecipeManager.Setting recipeSetting() {
        return switch (setting) {
            case ROUGH -> Scp914RecipeManager.Setting.ROUGH;
            case COARSE -> Scp914RecipeManager.Setting.COARSE;
            case ONE_TO_ONE -> Scp914RecipeManager.Setting.ONE_TO_ONE;
            case FINE -> Scp914RecipeManager.Setting.FINE;
            case VERY_FINE -> Scp914RecipeManager.Setting.VERY_FINE;
        };
    }

    public boolean isRefining() {
        return refining;
    }

    public long refiningElapsedTicks() {
        return level == null || !refining ? 0L
                : Math.max(0L, level.getGameTime() - refiningStartGameTime);
    }

    public float getDialAngle() {
        return dialAngle;
    }

    public Setting getSetting() {
        return setting;
    }

    public void setDialAngle(float angle, boolean commitSetting) {
        if (refining) return;
        float clamped = Math.max(ROUGH_ANGLE,
                Math.min(VERY_FINE_ANGLE, angle));
        if (Math.abs(clamped - dialAngle) < 0.001F && !commitSetting) return;
        dialAngle = clamped;
        if (commitSetting) setting = Setting.nearest(clamped);
        sync();
    }

    public void snapDialTo(Setting target) {
        if (refining || target == null) return;
        setting = target;
        dialAngle = target.angle;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Scp914Refining", refining);
        tag.putLong("Scp914RefiningStart", refiningStartGameTime);
        tag.putBoolean("Scp914CloseSoundPlayed", closeSoundPlayed);
        tag.putBoolean("Scp914CycleProcessed", cycleProcessed);
        tag.putBoolean("Scp914OpenSoundPlayed", openSoundPlayed);
        tag.putFloat("Scp914DialAngle", dialAngle);
        tag.putString("Scp914Setting", setting.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        refining = tag.getBoolean("Scp914Refining");
        refiningStartGameTime = tag.getLong("Scp914RefiningStart");
        closeSoundPlayed = tag.getBoolean("Scp914CloseSoundPlayed");
        cycleProcessed = tag.getBoolean("Scp914CycleProcessed");
        openSoundPlayed = tag.getBoolean("Scp914OpenSoundPlayed");
        dialAngle = tag.contains("Scp914DialAngle")
                ? tag.getFloat("Scp914DialAngle") : ONE_TO_ONE_ANGLE;
        try {
            setting = Setting.valueOf(tag.getString("Scp914Setting"));
        } catch (IllegalArgumentException exception) {
            setting = Setting.nearest(dialAngle);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection,
            ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) load(tag);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "scp914", 0,
                state -> state.setAndContinue(refining ? REFINING : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(10.0D, 5.0D, 10.0D);
    }

    public enum Setting {
        ROUGH(ROUGH_ANGLE), COARSE(COARSE_ANGLE), ONE_TO_ONE(ONE_TO_ONE_ANGLE),
        FINE(FINE_ANGLE), VERY_FINE(VERY_FINE_ANGLE);

        private final float angle;

        Setting(float angle) {
            this.angle = angle;
        }

        public float angle() {
            return angle;
        }

        public static Setting nearest(float angle) {
            Setting nearest = ONE_TO_ONE;
            float best = Float.MAX_VALUE;
            for (Setting candidate : values()) {
                float distance = Math.abs(candidate.angle - angle);
                if (distance < best) {
                    nearest = candidate;
                    best = distance;
                }
            }
            return nearest;
        }
    }
}
