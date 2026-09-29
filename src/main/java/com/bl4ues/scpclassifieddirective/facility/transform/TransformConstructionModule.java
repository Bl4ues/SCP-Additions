package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import com.bl4ues.scpclassifieddirective.item.OffGridConstructionToolItem;
import com.bl4ues.scpclassifieddirective.item.SurfaceConstructionToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Registry surface for the off-grid and parametric construction subsystem. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TransformConstructionModule {
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public static final IntegerProperty LIGHT_BLOCK = IntegerProperty.create(
            "light_block", 0, 15);
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

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TransformConstructionNetwork::register);
    }

    public static final class TransformProxyBlock extends Block {
        private TransformProxyBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(2.0F, 8.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(LIGHT))
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(LIGHT, 0).setValue(LIGHT_BLOCK, 0));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIGHT, LIGHT_BLOCK);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            if (level instanceof Level world && world.isClientSide) {
                return TransformConstructionClientBridge.selection(pos);
            }
            return TransformConstructionManager.proxySelectionShape(level, pos);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            if (level instanceof Level world && world.isClientSide) {
                return TransformConstructionClientBridge.collision(pos);
            }
            return TransformConstructionManager.proxyCollisionShape(level, pos);
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            // Rendering/AO must never see the axis-aligned technical voxel.
            // Light opacity is carried separately by LIGHT_BLOCK below, so
            // shaders do not paint rectangular seams over curved Surfaces.
            return Shapes.empty();
        }

        @Override
        public int getLightBlock(BlockState state, BlockGetter level,
                BlockPos pos) {
            return state.getValue(LIGHT_BLOCK);
        }

        @Override
        public boolean propagatesSkylightDown(BlockState state,
                BlockGetter level, BlockPos pos) {
            return state.getValue(LIGHT_BLOCK) == 0;
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) {
            int desiredLight = Math.max(0, Math.min(15,
                    TransformConstructionManager.proxyLight(level, pos)));
            int desiredLightBlock = Math.max(0, Math.min(15,
                    TransformConstructionManager.proxyLightBlock(level, pos)));
            if (desiredLight <= 0 && desiredLightBlock <= 0) {
                if (level.getBlockState(pos).is(this)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    level.getLightEngine().checkBlock(pos);
                }
                return;
            }
            if ((state.getValue(LIGHT) != desiredLight
                    || state.getValue(LIGHT_BLOCK) != desiredLightBlock)
                    && level.getBlockState(pos).is(this)) {
                level.setBlock(pos, state.setValue(LIGHT, desiredLight)
                                .setValue(LIGHT_BLOCK, desiredLightBlock),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                level.getLightEngine().checkBlock(pos);
            }
        }
    }
}
