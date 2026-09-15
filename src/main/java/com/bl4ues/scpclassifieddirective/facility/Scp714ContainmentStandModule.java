package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.Scp714ContainmentStandClient;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.init.Scp714Items;
import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpPickupRouter;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.sound.InventoryInteractionSoundFeedback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Dedicated physical display stand for one SCP-714 instance. */
public final class Scp714ContainmentStandModule {
    public static final String PATH = "scp_714_containment_stand";
    public static final String PLACE_INTERACTION = "place_scp_714_on_stand";
    public static final String TAKE_INTERACTION = "take_scp_714_from_stand";
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Block> BLOCK = BLOCKS.register(PATH,
            StandBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new StandItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<StandBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register(PATH, () -> BlockEntityType.Builder.of(
                    StandBlockEntity::new, BLOCK.get()).build(null));

    private Scp714ContainmentStandModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static final class StandBlock extends BaseEntityBlock {
        // Mirrors the Object Containment Unit's principal pedestal footprint,
        // deliberately omitting its reader extension and glass lid/case.
        private static final VoxelShape BODY_NORTH = Shapes.or(
                box(5.0D, 0.0D, 5.0D, 11.0D, 4.0D, 11.0D),
                box(6.0D, 4.0D, 6.0D, 10.0D, 12.0D, 10.0D),
                box(5.0D, 12.0D, 4.0D, 11.0D, 15.0D, 12.0D),
                box(1.0D, 15.0D, 1.0D, 15.0D, 16.0D, 15.0D))
                .optimize();
        // Shape queries are hot during placement/collision checks. Precompute
        // every facing once instead of rebuilding unions on each query.
        private static final VoxelShape BODY_EAST =
                rotateNorthShape(BODY_NORTH, Direction.EAST);
        private static final VoxelShape BODY_SOUTH =
                rotateNorthShape(BODY_NORTH, Direction.SOUTH);
        private static final VoxelShape BODY_WEST =
                rotateNorthShape(BODY_NORTH, Direction.WEST);

        private StandBlock() {
            super(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(3.0F, 1200.0F)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any().setValue(FACING,
                    Direction.NORTH));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new StandBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof StandBlockEntity stand)) {
                return InteractionResult.PASS;
            }

            if (stand.hasRing()) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return InteractionResult.PASS;
                }
                ItemStack ring = new ItemStack(Scp714Items.SCP_714.get());
                if (!giveToPlayer(serverPlayer, ring)) {
                    return InteractionResult.FAIL;
                }
                stand.setHasRing(false);
                InventoryInteractionSoundFeedback.pickup(serverPlayer);
                return InteractionResult.CONSUME;
            }

            ItemStack held = player.getItemInHand(hand);
            if (!held.is(Scp714Items.SCP_714.get())) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;

            // This is storage, not Creative-mode block placement: moving the ring
            // into the stand always transfers the concrete item instance.
            held.shrink(1);
            stand.setHasRing(true);
            if (player instanceof ServerPlayer serverPlayer) {
                InventoryInteractionSoundFeedback.pickup(serverPlayer);
            }
            return InteractionResult.CONSUME;
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!state.is(newState.getBlock()) && !level.isClientSide
                    && level.getBlockEntity(pos) instanceof StandBlockEntity stand
                    && stand.hasRing()) {
                popResource(level, pos, new ItemStack(Scp714Items.SCP_714.get()));
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case EAST -> BODY_EAST;
                case SOUTH -> BODY_SOUTH;
                case WEST -> BODY_WEST;
                default -> BODY_NORTH;
            };
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            return Shapes.empty();
        }

        @Override
        public boolean propagatesSkylightDown(BlockState state, BlockGetter level,
                BlockPos pos) {
            return true;
        }

        @Override
        public int getLightBlock(BlockState state, BlockGetter level,
                BlockPos pos) {
            return 0;
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING,
                    rotation.rotate(state.getValue(FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        public List<ItemStack> getDrops(BlockState state,
                LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(ITEM.get()));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(ITEM.get());
        }
    }

    public static final class StandBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final RawAnimation REMOVED =
                RawAnimation.begin().thenLoop("removed");
        private static final RawAnimation HOLDING =
                RawAnimation.begin().thenLoop("holding");

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);
        private boolean hasRing;

        private StandBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        public boolean hasRing() {
            return hasRing;
        }

        public void setHasRing(boolean value) {
            if (hasRing == value) return;
            hasRing = value;
            setChanged();
            if (level != null && !level.isClientSide) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state,
                        Block.UPDATE_ALL);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("HasScp714", hasRing);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            hasRing = tag.getBoolean("HasScp714");
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
            return new AABB(worldPosition).inflate(1.0D)
                    .expandTowards(0.0D, 1.0D, 0.0D);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this,
                    "scp_714_containment_stand", 0,
                    state -> state.setAndContinue(hasRing ? HOLDING : REMOVED)));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }

    public static final class StandItem extends BlockItem implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private StandItem(Block block) {
            super(block, new Item.Properties().stacksTo(16));
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private Scp714ContainmentStandClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
                        getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new Scp714ContainmentStandClient.ItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Inventory always renders the empty authored stand.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }

    private static boolean giveToPlayer(ServerPlayer player, ItemStack stack) {
        if (ScpClassifiedDirectiveModulesConfig.get().inventory.enabled
                && !player.isCreative() && !player.isSpectator()) {
            IScpInventory inventory = player.getCapability(
                    ScpInventoryCapability.INSTANCE).resolve().orElse(null);
            if (inventory == null) return false;
            int accepted = ScpPickupRouter.accept(inventory, player, stack.copy());
            if (accepted < stack.getCount()) {
                ModNetwork.showInventoryFull(player);
                return false;
            }
            ModNetwork.syncTo(player, inventory);
            return true;
        }
        return player.addItem(stack.copy());
    }

    private static VoxelShape rotateNorthShape(VoxelShape source,
            Direction facing) {
        if (facing == Direction.NORTH) return source;
        VoxelShape current = source;
        int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int turn = 0; turn < turns; turn++) {
            VoxelShape[] rotated = {Shapes.empty()};
            current.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(
                            1.0D - maxZ, minY, minX,
                            1.0D - minZ, maxY, maxX)));
            current = rotated[0].optimize();
        }
        return current;
    }
}
