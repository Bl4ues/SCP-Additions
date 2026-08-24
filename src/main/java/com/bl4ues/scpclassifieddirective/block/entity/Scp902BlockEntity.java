package com.bl4ues.scpclassifieddirective.block.entity;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Locale;

/** Server-authoritative open/close animation and ticking state for SCP-902. */
public final class Scp902BlockEntity extends BlockEntity implements GeoBlockEntity {
    public enum Phase {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    private static final int TRANSITION_TICKS = 20;
    private static final int TICK_SOUND_INTERVAL = 20;
    private static final ResourceLocation ACHIEVEMENT = new ResourceLocation(
            "scp_classified_directive", "scp_902_achievement");

    private static final RawAnimation CLOSED_ANIMATION =
            RawAnimation.begin().thenLoop("closed");
    private static final RawAnimation OPENING_ANIMATION =
            RawAnimation.begin().thenPlay("opening");
    private static final RawAnimation OPEN_ANIMATION =
            RawAnimation.begin().thenLoop("open");
    private static final RawAnimation CLOSING_ANIMATION =
            RawAnimation.begin().thenPlay("closing");

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    private Phase phase;
    private int transitionTicks;
    private int tickSoundTicks;

    public Scp902BlockEntity(BlockPos pos, BlockState state) {
        super(ScpClassifiedDirectiveModBlockEntities.SCP_902.get(), pos, state);
        phase = defaultPhase(state);
    }

    public Phase phase() {
        return phase;
    }

    public boolean isTransitioning() {
        return phase == Phase.OPENING || phase == Phase.CLOSING;
    }

    public InteractionResult handleUse(Player player) {
        if (level == null || player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (isTransitioning()) return InteractionResult.CONSUME;

        if (phase == Phase.CLOSED) {
            phase = Phase.OPENING;
            transitionTicks = 0;
            playSound(ScpClassifiedDirectiveModSounds.SCP902OPENING.get());
            awardAchievement(player);
            showOpeningThought(player);
            markUpdated();
            return InteractionResult.CONSUME;
        }
        if (phase == Phase.OPEN) {
            phase = Phase.CLOSING;
            transitionTicks = 0;
            playSound(ScpClassifiedDirectiveModSounds.SCP902CLOSING.get());
            markUpdated();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.CONSUME;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            Scp902BlockEntity box) {
        if (level.isClientSide) return;

        if (++box.tickSoundTicks >= TICK_SOUND_INTERVAL) {
            box.tickSoundTicks = 0;
            box.playSound(ScpClassifiedDirectiveModSounds.SCP902.get());
            box.showNearbyTickThoughts();
        }

        if (!box.isTransitioning()) return;
        box.transitionTicks++;
        if (box.transitionTicks < TRANSITION_TICKS) return;
        box.finishTransition();
    }

    private void finishTransition() {
        if (level == null || level.isClientSide) return;
        boolean opening = phase == Phase.OPENING;
        if (!opening && phase != Phase.CLOSING) return;

        BlockState current = getBlockState();
        BlockState target = (opening
                ? ScpClassifiedDirectiveModBlocks.SCP_902_OPEN.get()
                : ScpClassifiedDirectiveModBlocks.SCP_902_CLOSED.get())
                .defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING,
                        current.getValue(BlockStateProperties.HORIZONTAL_FACING))
                .setValue(BlockStateProperties.WATERLOGGED,
                        current.getValue(BlockStateProperties.WATERLOGGED));

        // The legacy open/closed registry ids are intentionally retained for
        // save compatibility. The transition plays on this BE first, then the
        // target block creates a fresh BE already in its correct idle phase.
        level.setBlock(worldPosition, target, 3);
    }

    private void playSound(SoundEvent sound) {
        if (level == null || level.isClientSide || sound == null) return;
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS,
                1.0F, 1.0F);
    }

    private void awardAchievement(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Advancement advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(ACHIEVEMENT);
        if (advancement == null) return;
        AdvancementProgress progress = serverPlayer.getAdvancements()
                .getOrStartProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            serverPlayer.getAdvancements().award(advancement, criterion);
        }
    }

    private void showOpeningThought(Player player) {
        if (player == null || level == null || level.random.nextFloat() >= 0.55F) {
            return;
        }
        String[] thoughts = {
                "It's empty?",
                "What is making the noise?",
                "How could it be?",
                "What is this?"
        };
        player.displayClientMessage(Component.literal(
                thoughts[level.random.nextInt(thoughts.length)]), true);
    }

    private void showNearbyTickThoughts() {
        if (level == null || level.isClientSide) return;
        AABB area = new AABB(worldPosition).inflate(2.5D);
        for (Player player : level.getEntitiesOfClass(Player.class, area,
                Player::isAlive)) {
            if (level.random.nextFloat() >= 0.04F) continue;
            String[] thoughts = {
                    "What is this sound?",
                    "Where is this ticking coming from?",
                    "What's inside this box?",
                    "Where is the noise coming from?"
            };
            player.displayClientMessage(Component.literal(
                    thoughts[level.random.nextInt(thoughts.length)]), true);
        }
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private static Phase defaultPhase(BlockState state) {
        return state != null && state.is(ScpClassifiedDirectiveModBlocks.SCP_902_OPEN.get())
                ? Phase.OPEN : Phase.CLOSED;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Phase", phase.name().toLowerCase(Locale.ROOT));
        tag.putInt("TransitionTicks", transitionTicks);
        tag.putInt("TickSoundTicks", tickSoundTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        phase = parsePhase(tag.getString("Phase"), getBlockState());
        transitionTicks = Math.max(0,
                Math.min(TRANSITION_TICKS - 1, tag.getInt("TransitionTicks")));
        tickSoundTicks = Math.max(0,
                Math.min(TICK_SOUND_INTERVAL - 1, tag.getInt("TickSoundTicks")));
    }

    private static Phase parsePhase(String value, BlockState state) {
        if (value == null || value.isBlank()) return defaultPhase(state);
        try {
            return Phase.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return defaultPhase(state);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

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
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0D);
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "scp902", 0,
                state -> state.setAndContinue(switch (phase) {
                    case CLOSED -> CLOSED_ANIMATION;
                    case OPENING -> OPENING_ANIMATION;
                    case OPEN -> OPEN_ANIMATION;
                    case CLOSING -> CLOSING_ANIMATION;
                })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
