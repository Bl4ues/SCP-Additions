package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.FacilityDecorativePropsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Small decorative facility equipment added to the Props section.
 *
 * <p>Static models stay baked-block based. Ceiling Ventilation is deliberately
 * isolated as GeckoLib block/item geometry because its fan is animated.
 */
public final class FacilityDecorativePropsModule {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Block> ELECTRICAL_PANEL =
            registerStatic("electrical_panel",
                    () -> new SupportedPropBlock(Placement.FLOOR_AND_WALL,
                            Shapes.or(
                                    Block.box(5.5D, 10.0D, 14.4D,
                                            10.5D, 18.0D, 16.0D),
                                    Block.box(8.0D, 0.0D, 15.0D,
                                            10.0D, 10.5D, 16.0D))));

    public static final RegistryObject<Block> HIGH_VOLTAGE_ELECTRICAL_BOX =
            registerStatic("high_voltage_electrical_box",
                    () -> new SupportedPropBlock(Placement.WALL,
                            Block.box(0.9D, 4.0D, 14.9D,
                                    15.0D, 13.6D, 16.0D)));

    public static final RegistryObject<Block> ELECTRICAL_CABINET =
            registerStatic("electrical_cabinet",
                    () -> new SupportedPropBlock(Placement.WALL,
                            Block.box(4.0D, 2.5D, 11.0D,
                                    12.0D, 13.5D, 16.0D)));

    public static final RegistryObject<Block> TWIN_EMERGENCY_LIGHT =
            registerStatic("twin_emergency_light",
                    () -> new SupportedPropBlock(Placement.WALL,
                            Block.box(4.0D, 4.5D, 13.8D,
                                    12.0D, 9.5D, 16.0D), 15));

    public static final RegistryObject<CeilingVentilationBlock>
            CEILING_VENTILATION = BLOCKS.register("ceiling_ventilation",
                    CeilingVentilationBlock::new);
    public static final RegistryObject<Item> CEILING_VENTILATION_ITEM =
            ITEMS.register("ceiling_ventilation",
                    () -> new CeilingVentilationItem(CEILING_VENTILATION.get()));
    public static final RegistryObject<BlockEntityType<CeilingVentilationBlockEntity>>
            CEILING_VENTILATION_BLOCK_ENTITY = BLOCK_ENTITIES.register(
                    "ceiling_ventilation",
                    () -> BlockEntityType.Builder.of(
                            CeilingVentilationBlockEntity::new,
                            CEILING_VENTILATION.get()).build(null));

    private FacilityDecorativePropsModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    public static Item electricalPanelItem() {
        return ELECTRICAL_PANEL.get().asItem();
    }

    public static Item highVoltageElectricalBoxItem() {
        return HIGH_VOLTAGE_ELECTRICAL_BOX.get().asItem();
    }

    public static Item electricalCabinetItem() {
        return ELECTRICAL_CABINET.get().asItem();
    }

    public static Item twinEmergencyLightItem() {
        return TWIN_EMERGENCY_LIGHT.get().asItem();
    }

    public static Item ceilingVentilationItem() {
        return CEILING_VENTILATION_ITEM.get();
    }

    private static RegistryObject<Block> registerStatic(String path,
            java.util.function.Supplier<? extends Block> factory) {
        RegistryObject<Block> block = BLOCKS.register(path, factory);
        ITEMS.register(path, () -> new DecorativeBlockItem(
                block.get(), new Item.Properties()));
        return block;
    }

    private enum Placement implements StringRepresentable {
        WALL("wall"),
        FLOOR_AND_WALL("floor_and_wall");

        private final String id;

        Placement(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    private static final class SupportedPropBlock
            extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
        private static final BooleanProperty WATERLOGGED =
                BlockStateProperties.WATERLOGGED;

        private final Placement placement;
        private final VoxelShape northShape;

        private SupportedPropBlock(Placement placement, VoxelShape northShape) {
            this(placement, northShape, 0);
        }

        private SupportedPropBlock(Placement placement, VoxelShape northShape,
                int lightLevel) {
            super(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1.0F, 10.0F)
                    .lightLevel(state -> lightLevel)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            this.placement = placement;
            this.northShape = northShape;
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, WATERLOGGED);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction facing;
            if (placement == Placement.WALL) {
                Direction clicked = context.getClickedFace();
                if (clicked.getAxis() == Direction.Axis.Y) return null;
                facing = clicked;
            } else {
                if (context.getClickedFace() != Direction.UP) return null;
                facing = context.getHorizontalDirection().getOpposite();
            }

            boolean waterlogged = context.getLevel().getFluidState(
                    context.getClickedPos()).getType() == Fluids.WATER;
            BlockState state = defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(WATERLOGGED, waterlogged);
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            Direction facing = state.getValue(FACING);
            boolean wall = WallMountedSupportEvents.hasWallSupport(
                    level, pos, facing);
            if (!wall) return false;
            if (placement != Placement.FLOOR_AND_WALL) return true;
            BlockPos below = pos.below();
            return level.getBlockState(below).isFaceSturdy(
                    level, below, Direction.UP);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighbor, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (state.getValue(WATERLOGGED)) {
                level.scheduleTick(pos, Fluids.WATER,
                        Fluids.WATER.getTickDelay(level));
            }
            return state.canSurvive(level, pos)
                    ? super.updateShape(state, direction, neighbor, level,
                            pos, neighborPos)
                    : Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED)
                    ? Fluids.WATER.getSource(false)
                    : super.getFluidState(state);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return rotateNorthShape(northShape, state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
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
    }

    public static final class CeilingVentilationBlock
            extends BaseEntityBlock implements SimpleWaterloggedBlock {
        public static final BooleanProperty WATERLOGGED =
                BlockStateProperties.WATERLOGGED;
        private static final VoxelShape BODY =
                Block.box(0.0D, 7.0D, 0.0D, 16.0D, 16.0D, 16.0D);

        private CeilingVentilationBlock() {
            super(BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL)
                    .strength(1.5F, 12.0F)
                    .lightLevel(state -> 12)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(HorizontalDirectionalBlock.FACING,
                            Direction.NORTH)
                    .setValue(WATERLOGGED, false));
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CeilingVentilationBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING, WATERLOGGED);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            if (context.getClickedFace() != Direction.DOWN) return null;
            boolean waterlogged = context.getLevel().getFluidState(
                    context.getClickedPos()).getType() == Fluids.WATER;
            BlockState state = defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING,
                            context.getHorizontalDirection().getOpposite())
                    .setValue(WATERLOGGED, waterlogged);
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            BlockPos above = pos.above();
            return level.getBlockState(above).isFaceSturdy(
                    level, above, Direction.DOWN);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighbor, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (state.getValue(WATERLOGGED)) {
                level.scheduleTick(pos, Fluids.WATER,
                        Fluids.WATER.getTickDelay(level));
            }
            if (direction == Direction.UP && !state.canSurvive(level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, direction, neighbor, level,
                    pos, neighborPos);
        }

        @Override
        public FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED)
                    ? Fluids.WATER.getSource(false)
                    : super.getFluidState(state);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return BODY;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return BODY;
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
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
    }

    public static final class CeilingVentilationBlockEntity extends BlockEntity
            implements GeoBlockEntity {
        private static final RawAnimation IDLE =
                RawAnimation.begin().thenLoop("idle");

        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        public CeilingVentilationBlockEntity(BlockPos pos, BlockState state) {
            super(CEILING_VENTILATION_BLOCK_ENTITY.get(), pos, state);
        }

        @Override
        public void registerControllers(
                AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this,
                    "ceiling_ventilation", 0,
                    state -> state.setAndContinue(IDLE)));
        }

        @Override
        public double getTick(Object blockEntity) {
            return level == null ? 0.0D : level.getGameTime();
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return animationCache;
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(2.0D, 1.0D, 2.0D);
        }
    }

    public static final class CeilingVentilationItem extends BlockItem
            implements GeoItem {
        private final AnimatableInstanceCache animationCache =
                GeckoLibUtil.createInstanceCache(this);

        private CeilingVentilationItem(Block block) {
            super(block, new Item.Properties());
            SingletonGeoAnimatable.registerSyncedAnimatable(this);
        }

        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions() {
                private FacilityDecorativePropsClient.CeilingVentilationItemRenderer
                        renderer;

                @Override
                public @NotNull net.minecraft.client.renderer.
                        BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) {
                        renderer = new FacilityDecorativePropsClient.
                                CeilingVentilationItemRenderer();
                    }
                    return renderer;
                }
            });
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.decorative_prop")
                    .withStyle(ChatFormatting.GRAY));
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

    private static final class DecorativeBlockItem extends BlockItem {
        private DecorativeBlockItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.decorative_prop")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
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
