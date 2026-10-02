package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.DoubleWallPanelClient;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.model.data.ModelData;
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

public final class DoubleWallPanelModule {
    public static final String REMOVE_FRONT_INTERACTION =
            "remove_double_wall_panel_front";
    public static final String REMOVE_BACK_INTERACTION =
            "remove_double_wall_panel_back";
    public enum Side {
        FRONT,
        BACK
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<DoubleWallPanelBlock> BLOCK =
            BLOCKS.register("double_wall_panel", DoubleWallPanelBlock::new);
    public static final RegistryObject<Item> ITEM =
            ITEMS.register("double_wall_panel",
                    () -> new DoubleWallPanelItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<DoubleWallPanelBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register("double_wall_panel",
                    () -> BlockEntityType.Builder.of(
                            DoubleWallPanelBlockEntity::new,
                            BLOCK.get()).build(null));

    private DoubleWallPanelModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static final class DoubleWallPanelBlock extends BaseEntityBlock {
        private static final VoxelShape SHAPE =
                Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

        private DoubleWallPanelBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL));
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
            return new DoubleWallPanelBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public InteractionResult use(BlockState state, Level level,
                BlockPos pos, Player player, InteractionHand hand,
                BlockHitResult hit) {
            if (!(level.getBlockEntity(pos)
                    instanceof DoubleWallPanelBlockEntity panel)) {
                return InteractionResult.PASS;
            }
            Side side = sideForHit(state, hit.getDirection());
            if (side == null) return InteractionResult.PASS;

            ItemStack held = player.getItemInHand(hand);
            if (held.is(UnifiedReaderItems.SCREWDRIVER.get())) {
                if (!panel.hasMaterial(side)) return InteractionResult.PASS;
                if (level.isClientSide) return InteractionResult.SUCCESS;
                give(player, panel.removeMaterial(side));
                return InteractionResult.CONSUME;
            }

            if (!(held.getItem() instanceof BlockItem blockItem)) {
                return InteractionResult.PASS;
            }
            if (panel.hasMaterial(side)) {
                return level.isClientSide
                        ? InteractionResult.SUCCESS
                        : InteractionResult.CONSUME;
            }

            BlockState material = blockItem.getBlock().defaultBlockState();
            if (!CopycatPanelMaterial.valid(level, pos, material,
                    BLOCK.get(), WallPanelModule.BLOCK.get())) {
                return InteractionResult.FAIL;
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;

            panel.setMaterial(side, material);
            if (!player.isCreative()) held.shrink(1);
            SoundType sound = material.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getPlaceSound(),
                    SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            return InteractionResult.CONSUME;
        }

        public static Side sideForHit(BlockState state, Direction clickedFace) {
            if (state == null || clickedFace == null
                    || clickedFace.getAxis().isVertical()) return null;
            Direction front = state.getValue(
                    HorizontalDirectionalBlock.FACING);
            if (clickedFace == front) return Side.FRONT;
            if (clickedFace == front.getOpposite()) return Side.BACK;
            return null;
        }

        @Override
        public SoundType getSoundType(BlockState state, LevelReader level,
                BlockPos pos, @Nullable Entity entity) {
            if (level.getBlockEntity(pos)
                    instanceof DoubleWallPanelBlockEntity panel) {
                Side side = sideForEntity(state, pos, entity);
                BlockState material = panel.material(side);
                if (material.isAir()) {
                    material = panel.material(side == Side.FRONT
                            ? Side.BACK : Side.FRONT);
                }
                if (!material.isAir()) {
                    return material.getSoundType(level, pos, entity);
                }
            }
            return super.getSoundType(state, level, pos, entity);
        }

        private static Side sideForEntity(BlockState state, BlockPos pos,
                @Nullable Entity entity) {
            if (entity == null) return Side.FRONT;
            Direction front = state.getValue(
                    HorizontalDirectionalBlock.FACING);
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 delta = entity.position().subtract(center);
            double dot = delta.x * front.getStepX()
                    + delta.z * front.getStepZ();
            return dot >= 0.0D ? Side.FRONT : Side.BACK;
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (!level.isClientSide && !newState.is(this)
                    && level.getBlockEntity(pos)
                    instanceof DoubleWallPanelBlockEntity panel) {
                drop(level, pos, panel.storedMaterialItem(Side.FRONT));
                drop(level, pos, panel.storedMaterialItem(Side.BACK));
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

        private static void give(Player player, ItemStack stack) {
            if (stack.isEmpty()) return;
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }

        private static void drop(Level level, BlockPos pos, ItemStack stack) {
            if (stack.isEmpty()) return;
            Containers.dropItemStack(level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    stack);
        }
    }

    public static final class DoubleWallPanelBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final String FRONT_BLOCK = "FrontBlock";
        private static final String BACK_BLOCK = "BackBlock";

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);
        private BlockState front = Blocks.AIR.defaultBlockState();
        private BlockState back = Blocks.AIR.defaultBlockState();

        public DoubleWallPanelBlockEntity(BlockPos pos, BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state);
        }

        public boolean hasMaterial(Side side) {
            return !material(side).isAir();
        }

        public BlockState material(Side side) {
            return side == Side.BACK ? back : front;
        }

        public void setMaterial(Side side, BlockState state) {
            if (side == Side.BACK) back = safe(state);
            else front = safe(state);
            markUpdated();
        }

        public ItemStack removeMaterial(Side side) {
            BlockState previous = material(side);
            if (side == Side.BACK) back = Blocks.AIR.defaultBlockState();
            else front = Blocks.AIR.defaultBlockState();
            markUpdated();
            return CopycatPanelMaterial.item(previous);
        }

        public ItemStack storedMaterialItem(Side side) {
            return CopycatPanelMaterial.item(material(side));
        }

        public CompoundTag saveCopycatData() {
            CompoundTag tag = new CompoundTag();
            CopycatPanelMaterial.writeBlock(tag, FRONT_BLOCK, front);
            CopycatPanelMaterial.writeBlock(tag, BACK_BLOCK, back);
            return tag;
        }

        public void loadCopycatData(CompoundTag tag) {
            front = CopycatPanelMaterial.readBlock(tag, FRONT_BLOCK);
            back = CopycatPanelMaterial.readBlock(tag, BACK_BLOCK);
        }

        private static BlockState safe(BlockState state) {
            return state == null ? Blocks.AIR.defaultBlockState() : state;
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
            CopycatPanelMaterial.writeBlock(tag, FRONT_BLOCK, front);
            CopycatPanelMaterial.writeBlock(tag, BACK_BLOCK, back);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            loadCopycatData(tag);
            refreshClientModelData();
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
        public @NotNull ModelData getModelData() {
            return CopycatPanelMaterial.modelData(
                    front, back, worldPosition);
        }

        private void refreshClientModelData() {
            if (level == null || !level.isClientSide) return;
            requestModelDataUpdate();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state,
                    Block.UPDATE_ALL);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }

    public static final class DoubleWallPanelItem extends BlockItem
            implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private DoubleWallPanelItem(Block block) {
            super(block, new Item.Properties());
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private DoubleWallPanelClient.ItemRenderer renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.
                        BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new DoubleWallPanelClient.ItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.double_wall_panel.copy")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.double_wall_panel.reset")
                    .withStyle(ChatFormatting.AQUA));
            super.appendHoverText(stack, level, tooltip, flag);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }
    }
}
