package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.init.DocumentItems;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Locale;
import java.util.UUID;

/** Server-authoritative single-document storage and animation state machine. */
public final class DocumentHolderBlockEntity extends BlockEntity
        implements GeoBlockEntity {
    public enum State {
        EMPTY,
        CLOSING_FILL,
        HOLDING,
        TAKING,
        EMPTY_OPEN,
        CLOSING_EMPTY
    }

    private static final int CLOSE_ANIMATION_TICKS = 24;
    private static final int TAKE_DELIVERY_TICK = 18;

    private static final RawAnimation EMPTY_ANIMATION =
            RawAnimation.begin().thenLoop("empty");
    private static final RawAnimation HOLDING_ANIMATION =
            RawAnimation.begin().thenLoop("holding");
    private static final RawAnimation TAKE_ANIMATION =
            RawAnimation.begin().thenPlay("take");
    private static final RawAnimation EMPTY_OPEN_ANIMATION =
            RawAnimation.begin().thenLoop("empty_open");
    private static final RawAnimation CLOSE_EMPTY_ANIMATION =
            RawAnimation.begin().thenPlay("close_empty");
    private static final RawAnimation CLOSE_FILL_ANIMATION =
            RawAnimation.begin().thenPlay("close_fill");

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    private State state = State.EMPTY;
    private ItemStack document = ItemStack.EMPTY;
    private int transitionTicks;
    private UUID pendingTaker;
    private InteractionHand pendingHand = InteractionHand.MAIN_HAND;

    public DocumentHolderBlockEntity(BlockPos pos, BlockState state) {
        super(DocumentHolderModule.blockEntityType(), pos, state);
    }

    public State state() {
        return state;
    }

    public boolean hasDocument() {
        return !document.isEmpty();
    }

    public boolean wouldHandle(Player player, InteractionHand hand) {
        if (player == null) return false;
        if (isTransitioning()) return true;

        InteractionHand documentHand = findDocumentHand(player, hand);
        return switch (state) {
            case EMPTY -> documentHand != null;
            case HOLDING -> bothHandsEmpty(player);
            case EMPTY_OPEN -> documentHand != null
                    || player.getItemInHand(hand).isEmpty();
            default -> true;
        };
    }

    public InteractionResult handleUse(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide || player == null) {
            return InteractionResult.PASS;
        }
        if (isTransitioning()) return InteractionResult.CONSUME;

        InteractionHand documentHand = findDocumentHand(player, hand);
        return switch (state) {
            case EMPTY -> documentHand != null && insertDocument(player, documentHand)
                    ? InteractionResult.CONSUME : InteractionResult.PASS;
            case HOLDING -> bothHandsEmpty(player) && beginTake(player, hand)
                    ? InteractionResult.CONSUME : InteractionResult.PASS;
            case EMPTY_OPEN -> {
                if (documentHand != null && insertDocument(player, documentHand)) {
                    yield InteractionResult.CONSUME;
                }
                if (player.getItemInHand(hand).isEmpty()) {
                    beginCloseEmpty();
                    yield InteractionResult.CONSUME;
                }
                yield InteractionResult.PASS;
            }
            default -> InteractionResult.CONSUME;
        };
    }

    private boolean insertDocument(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isDocument(held) || !document.isEmpty()) return false;

        document = held.copy();
        document.setCount(1);
        held.shrink(1);
        state = State.CLOSING_FILL;
        transitionTicks = 0;
        pendingTaker = null;
        pendingHand = InteractionHand.MAIN_HAND;
        markUpdated();
        return true;
    }

    private boolean beginTake(Player player, InteractionHand hand) {
        if (document.isEmpty()) {
            state = State.EMPTY_OPEN;
            markUpdated();
            return false;
        }
        state = State.TAKING;
        transitionTicks = 0;
        pendingTaker = player.getUUID();
        pendingHand = hand;
        markUpdated();
        return true;
    }

    private void beginCloseEmpty() {
        state = State.CLOSING_EMPTY;
        transitionTicks = 0;
        pendingTaker = null;
        markUpdated();
    }

    private boolean isTransitioning() {
        return state == State.CLOSING_FILL
                || state == State.CLOSING_EMPTY
                || state == State.TAKING;
    }

    public static void serverTick(Level level, BlockPos pos,
            BlockState blockState, DocumentHolderBlockEntity holder) {
        if (level.isClientSide || !holder.isTransitioning()) return;

        holder.transitionTicks++;
        if (holder.state == State.TAKING
                && holder.transitionTicks >= TAKE_DELIVERY_TICK) {
            holder.finishTake();
            return;
        }
        if (holder.transitionTicks < CLOSE_ANIMATION_TICKS) return;

        if (holder.state == State.CLOSING_FILL) {
            holder.state = holder.document.isEmpty()
                    ? State.EMPTY : State.HOLDING;
        } else if (holder.state == State.CLOSING_EMPTY) {
            holder.state = State.EMPTY;
        }
        holder.transitionTicks = 0;
        holder.markUpdated();
    }

    private void finishTake() {
        ItemStack taken = document.copy();
        document = ItemStack.EMPTY;

        if (!taken.isEmpty() && level instanceof ServerLevel serverLevel) {
            Player player = pendingTaker == null
                    ? null : serverLevel.getPlayerByUUID(pendingTaker);
            if (player != null) {
                InteractionHand hand = pendingHand == null
                        ? InteractionHand.MAIN_HAND : pendingHand;
                if (player.getItemInHand(hand).isEmpty()) {
                    player.setItemInHand(hand, taken);
                } else if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
            } else {
                Containers.dropItemStack(serverLevel,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D, taken);
            }
        }

        state = State.EMPTY_OPEN;
        transitionTicks = 0;
        pendingTaker = null;
        pendingHand = InteractionHand.MAIN_HAND;
        markUpdated();
    }

    public void dropStoredDocument() {
        if (level == null || level.isClientSide || document.isEmpty()) return;
        ItemStack dropped = document.copy();
        document = ItemStack.EMPTY;
        pendingTaker = null;
        Containers.dropItemStack(level,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D, dropped);
        setChanged();
    }

    private static InteractionHand findDocumentHand(Player player,
            InteractionHand preferred) {
        if (isDocument(player.getItemInHand(preferred))) return preferred;
        InteractionHand other = preferred == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return isDocument(player.getItemInHand(other)) ? other : null;
    }

    private static boolean bothHandsEmpty(Player player) {
        return player.getMainHandItem().isEmpty()
                && player.getOffhandItem().isEmpty();
    }

    private static boolean isDocument(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return DocumentItems.DOCUMENT_ID.equals(id);
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState blockState = getBlockState();
            level.sendBlockUpdated(worldPosition, blockState, blockState, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("HolderState", state.name().toLowerCase(Locale.ROOT));
        if (!document.isEmpty()) {
            tag.put("Document", document.save(new CompoundTag()));
        }
        if (pendingTaker != null) tag.putUUID("PendingTaker", pendingTaker);
        tag.putBoolean("PendingOffhand", pendingHand == InteractionHand.OFF_HAND);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        state = parseState(tag.getString("HolderState"));
        document = tag.contains("Document", Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound("Document")) : ItemStack.EMPTY;
        pendingTaker = tag.hasUUID("PendingTaker")
                ? tag.getUUID("PendingTaker") : null;
        pendingHand = tag.getBoolean("PendingOffhand")
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        transitionTicks = 0;
        sanitizeLoadedState();
    }

    private void sanitizeLoadedState() {
        if (document.isEmpty()) {
            state = switch (state) {
                case HOLDING, CLOSING_FILL -> State.EMPTY;
                case TAKING -> State.EMPTY_OPEN;
                default -> state;
            };
        } else {
            state = switch (state) {
                case EMPTY, EMPTY_OPEN -> State.HOLDING;
                case CLOSING_EMPTY -> State.CLOSING_FILL;
                default -> state;
            };
        }
    }

    private static State parseState(String value) {
        if (value == null || value.isBlank()) return State.EMPTY;
        try {
            return State.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return State.EMPTY;
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
        controllers.add(new AnimationController<>(this, "holder", 0,
                animationState -> animationState.setAndContinue(
                        switch (state) {
                            case EMPTY -> EMPTY_ANIMATION;
                            case CLOSING_FILL -> CLOSE_FILL_ANIMATION;
                            case HOLDING -> HOLDING_ANIMATION;
                            case TAKING -> TAKE_ANIMATION;
                            case EMPTY_OPEN -> EMPTY_OPEN_ANIMATION;
                            case CLOSING_EMPTY -> CLOSE_EMPTY_ANIMATION;
                        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
