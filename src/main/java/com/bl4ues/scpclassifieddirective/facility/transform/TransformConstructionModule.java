package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.OffGridConstructionToolItem;
import com.bl4ues.scpclassifieddirective.item.SurfaceConstructionToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Registry surface for the off-grid and parametric construction subsystem. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TransformConstructionModule {
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public static final ResourceLocation PROXY_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "transform_construction_proxy");
    public static final ResourceLocation OFF_GRID_TOOL_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "off_grid_construction_tool");
    public static final ResourceLocation SURFACE_TOOL_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "surface_construction_tool");

    private TransformConstructionModule() {
    }

    public static TransformProxyBlock getProxy() {
        Block block = ForgeRegistries.BLOCKS.getValue(PROXY_ID);
        if (!(block instanceof TransformProxyBlock proxy)) {
            throw new IllegalStateException("Transform construction proxy is not registered yet");
        }
        return proxy;
    }

    public static Item getOffGridTool() {
        Item item = ForgeRegistries.ITEMS.getValue(OFF_GRID_TOOL_ID);
        if (item == null) throw new IllegalStateException(
                "Off-Grid Construction Tool is not registered yet");
        return item;
    }

    public static Item getSurfaceTool() {
        Item item = ForgeRegistries.ITEMS.getValue(SURFACE_TOOL_ID);
        if (item == null) throw new IllegalStateException(
                "Surface Construction Tool is not registered yet");
        return item;
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, PROXY_ID,
                TransformProxyBlock::new);
        event.register(ForgeRegistries.Keys.ITEMS, OFF_GRID_TOOL_ID,
                OffGridConstructionToolItem::new);
        event.register(ForgeRegistries.Keys.ITEMS, SURFACE_TOOL_ID,
                SurfaceConstructionToolItem::new);
    }

    public static final class TransformProxyBlock extends Block {
        private TransformProxyBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.0F, 8.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(LIGHT))
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIGHT);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return TransformConstructionManager.proxySelectionShape(level, pos);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return TransformConstructionManager.proxyCollisionShape(level, pos);
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            return TransformConstructionManager.proxyCollisionShape(level, pos);
        }

        @Override
        public int getLightBlock(BlockState state, BlockGetter level,
                BlockPos pos) {
            return 0;
        }
    }
}
