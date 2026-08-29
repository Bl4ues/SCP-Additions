package com.bl4ues.scpclassifieddirective.block.entity;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
    private float dialAngle = ONE_TO_ONE_ANGLE;
    private Setting setting = Setting.ONE_TO_ONE;

    public Scp914BlockEntity(BlockPos pos, BlockState state) {
        super(ScpClassifiedDirectiveModBlockEntities.SCP_914.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, Scp914BlockEntity blockEntity) {
        if (level.isClientSide || !blockEntity.refining) return;
        long elapsed = level.getGameTime() - blockEntity.refiningStartGameTime;
        if (elapsed >= REFINING_TICKS) {
            blockEntity.refining = false;
            blockEntity.refiningStartGameTime = 0L;
            blockEntity.sync();
        }
    }

    public boolean beginRefining() {
        if (level == null || level.isClientSide || refining) return false;
        refining = true;
        refiningStartGameTime = level.getGameTime();
        sync();
        return true;
    }

    public boolean isRefining() {
        return refining;
    }

    public long refiningElapsedTicks() {
        return level == null || !refining ? 0L : Math.max(0L, level.getGameTime() - refiningStartGameTime);
    }

    public float getDialAngle() {
        return dialAngle;
    }

    public Setting getSetting() {
        return setting;
    }

    public void setDialAngle(float angle, boolean commitSetting) {
        float clamped = Math.max(ROUGH_ANGLE, Math.min(VERY_FINE_ANGLE, angle));
        if (Math.abs(clamped - dialAngle) < 0.001F && !commitSetting) return;
        dialAngle = clamped;
        if (commitSetting) setting = Setting.nearest(clamped);
        sync();
    }

    public void snapDialTo(Setting target) {
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
        tag.putFloat("Scp914DialAngle", dialAngle);
        tag.putString("Scp914Setting", setting.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        refining = tag.getBoolean("Scp914Refining");
        refiningStartGameTime = tag.getLong("Scp914RefiningStart");
        dialAngle = tag.contains("Scp914DialAngle") ? tag.getFloat("Scp914DialAngle") : ONE_TO_ONE_ANGLE;
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
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) load(tag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
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
        ROUGH(ROUGH_ANGLE),
        COARSE(COARSE_ANGLE),
        ONE_TO_ONE(ONE_TO_ONE_ANGLE),
        FINE(FINE_ANGLE),
        VERY_FINE(VERY_FINE_ANGLE);

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
