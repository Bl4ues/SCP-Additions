package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.WallPanelClient;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.BlockHitResult;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Thin facility panel that can borrow the appearance and sounds of one solid
 * block while retaining its own geometry.
 */
public final class WallPanelModule {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<WallPanelBlock> BLOCK =
            BLOCKS.register("wall_panel", WallPanelBlock::new);
    public static final RegistryObject<Item> ITEM =
            ITEMS.register("wall_panel", () -> new WallPanelItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<WallPanelBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register("wall_panel",
                    () -> BlockEntityType.Builder.of(
                            WallPanelBlockEntity::new, BLOCK.get()).build(null));

    private WallPanelModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static final class WallPanelBlock extends BaseEntityBlock {
        private static final VoxelShape NORTH_SHAPE =
                Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);

        private WallPanelBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(HorizontalDirectionalBlock.FACING,
                            Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(
                    HorizontalDirectionalBlock.FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new WallPanelBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return rotateNorthShape(NORTH_SHAPE,
                    state.getValue(HorizontalDirectionalBlock.FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public InteractionResult use(BlockState state, Level level,
                BlockPos pos, Player player, InteractionHand hand,
                BlockHitResult hit) {
            if (!(level.getBlockEntity(pos)
                    instanceof WallPanelBlockEntity panel)) {
                return InteractionResult.PASS;
            }

            ItemStack held = player.getItemInHand(hand);
            if (held.is(UnifiedReaderItems.SCREWDRIVER.get())) {
                if (!panel.hasMaterial()) return InteractionResult.PASS;
                if (level.isClientSide) return InteractionResult.SUCCESS;

                ItemStack returned = panel.removeMaterial();
                if (!returned.isEmpty()
                        && !player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
                return InteractionResult.CONSUME;
            }

            if (!(held.getItem() instanceof BlockItem blockItem)) {
                return InteractionResult.PASS;
            }

            // A configured panel must be reset with the Screwdriver before a
            // different material can be installed.
            if (panel.hasMaterial()) {
                return level.isClientSide
                        ? InteractionResult.SUCCESS
                        : InteractionResult.CONSUME;
            }

            BlockState material = blockItem.getBlock().defaultBlockState();
            if (!isValidMaterial(level, pos, material)) {
                return InteractionResult.FAIL;
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;

            ItemStack stored = held.copy();
            stored.setCount(1);
            panel.setMaterial(material, stored);
            if (!player.isCreative()) held.shrink(1);

            SoundType sound = material.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getPlaceSound(),
                    SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            return InteractionResult.CONSUME;
        }

        private static boolean isValidMaterial(Level level, BlockPos pos,
                BlockState material) {
            if (material.isAir()
                    || material.is(BLOCK.get())
                    || !material.getFluidState().isEmpty()
                    || material.getRenderShape() != RenderShape.MODEL) {
                return false;
            }
            return Block.isShapeFullBlock(
                    material.getCollisionShape(level, pos));
        }

        @Override
        public SoundType getSoundType(BlockState state, LevelReader level,
                BlockPos pos, @Nullable Entity entity) {
            if (level.getBlockEntity(pos)
                    instanceof WallPanelBlockEntity panel
                    && panel.hasMaterial()) {
                BlockState material = panel.materialState();
                if (!material.isAir()) {
                    return material.getSoundType(level, pos, entity);
                }
            }
            return super.getSoundType(state, level, pos, entity);
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!level.isClientSide && !newState.is(this)
                    && level.getBlockEntity(pos)
                    instanceof WallPanelBlockEntity panel) {
                ItemStack material = panel.storedMaterialItem();
                if (!material.isEmpty()) {
                    Containers.dropItemStack(level,
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            material);
                }
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(HorizontalDirectionalBlock.FACING,
                    rotation.rotate(state.getValue(
                            HorizontalDirectionalBlock.FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(
                    HorizontalDirectionalBlock.FACING)));
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

    public static final class WallPanelBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final String MATERIAL_BLOCK = "MaterialBlock";
        private static final String MATERIAL_ITEM = "MaterialItem";

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);
        private BlockState materialState = Blocks.AIR.defaultBlockState();
        private ItemStack materialItem = ItemStack.EMPTY;

        public WallPanelBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        public boolean hasMaterial() {
            return !materialState.isAir() && !materialItem.isEmpty();
        }

        public BlockState materialState() {
            return materialState;
        }

        public ItemStack storedMaterialItem() {
            return materialItem.copy();
        }

        public void setMaterial(BlockState state, ItemStack item) {
            materialState = state == null
                    ? Blocks.AIR.defaultBlockState() : state;
            materialItem = item == null ? ItemStack.EMPTY : item.copy();
            if (!materialItem.isEmpty()) materialItem.setCount(1);
            markUpdated();
        }

        public ItemStack removeMaterial() {
            ItemStack removed = materialItem.copy();
            materialState = Blocks.AIR.defaultBlockState();
            materialItem = ItemStack.EMPTY;
            markUpdated();
            return removed;
        }

        private void markUpdated() {
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
            if (!materialState.isAir()) {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(
                        materialState.getBlock());
                if (id != null) {
                    tag.putString(MATERIAL_BLOCK, id.toString());
                }
            }
            if (!materialItem.isEmpty()) {
                tag.put(MATERIAL_ITEM,
                        materialItem.save(new CompoundTag()));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            materialState = Blocks.AIR.defaultBlockState();
            materialItem = ItemStack.EMPTY;

            if (tag.contains(MATERIAL_BLOCK)) {
                try {
                    ResourceLocation id =
                            new ResourceLocation(tag.getString(MATERIAL_BLOCK));
                    Block block = ForgeRegistries.BLOCKS.getValue(id);
                    if (block != null && block != Blocks.AIR
                            && block != BLOCK.get()) {
                        materialState = block.defaultBlockState();
                    }
                } catch (RuntimeException ignored) {
                    materialState = Blocks.AIR.defaultBlockState();
                }
            }

            if (tag.contains(MATERIAL_ITEM)) {
                materialItem = ItemStack.of(
                        tag.getCompound(MATERIAL_ITEM));
            }
            if (materialState.isAir()) {
                materialItem = ItemStack.EMPTY;
            } else if (materialItem.isEmpty()) {
                materialItem = new ItemStack(materialState.getBlock());
            } else {
                materialItem.setCount(1);
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
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Static model.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }

    public static final class WallPanelItem extends BlockItem
            implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private WallPanelItem(Block block) {
            super(block, new Item.Properties());
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private WallPanelClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.
                        BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new WallPanelClient.ItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.wall_panel.copy")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.wall_panel.reset")
                    .withStyle(ChatFormatting.AQUA));
            super.appendHoverText(stack, level, tooltip, flag);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            // Static model.
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }
}
