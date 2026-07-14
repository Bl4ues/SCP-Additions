package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Collections;
import java.util.List;

/**
 * Invisible relay occupying the upper half of a two-block-tall heavy door.
 *
 * Heavy door controllers are stored at floor level, while wall buttons are
 * normally mounted around the visual upper half. The relay polls redstone around
 * both upper levels and exposes it as a normal neighboring signal to the base
 * controller, allowing the existing door state machine to remain unchanged.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HeavyDoorPowerRelay {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpAdditionsMod.MODID);

    public static final RegistryObject<Block> RELAY = BLOCKS.register(
            "heavy_door_power_relay", RelayBlock::new);

    private HeavyDoorPowerRelay() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    @SubscribeEvent
    public static void onDoorPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !isHeavyDoorState(event.getPlacedBlock().getBlock())) {
            return;
        }
        ensureRelay(level, event.getPos());
    }

    @SubscribeEvent
    public static void onNearbyNeighborUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos source = event.getPos();
        for (int dy = 0; dy <= 2; dy++) {
            int baseY = source.getY() - dy;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos candidate = new BlockPos(
                            source.getX() + dx, baseY, source.getZ() + dz);
                    if (isHeavyDoorState(level.getBlockState(candidate).getBlock())) {
                        ensureRelay(level, candidate);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDoorBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !isHeavyDoorState(event.getState().getBlock())) {
            return;
        }

        BlockPos relayPos = event.getPos().above();
        if (level.getBlockState(relayPos).is(RELAY.get())) {
            level.removeBlock(relayPos, false);
        }
    }

    private static void ensureRelay(ServerLevel level, BlockPos basePos) {
        BlockPos relayPos = basePos.above();
        BlockState current = level.getBlockState(relayPos);
        if (current.is(RELAY.get())) {
            return;
        }
        if (current.canBeReplaced()) {
            level.setBlock(relayPos, RELAY.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static boolean isHeavyDoorState(Block block) {
        return belongsTo(FacilityModule.DEFAULT_DOOR, block)
                || belongsTo(FacilityModule.YELLOW_DOOR, block)
                || belongsTo(FacilityModule.BLACK_DOOR, block);
    }

    private static boolean belongsTo(FacilityModule.DoorFamily family, Block block) {
        if (block == family.closed().get() || block == family.open().get()) {
            return true;
        }
        return family.opening().stream().anyMatch(entry -> entry.get() == block)
                || family.closing().stream().anyMatch(entry -> entry.get() == block);
    }

    private static final class RelayBlock extends Block {
        private static final BooleanProperty POWERED = BlockStateProperties.POWERED;

        private RelayBlock() {
            super(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3_600_000.0F)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any().setValue(POWERED, false));
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return true;
        }

        @Override
        public int getSignal(BlockState state, BlockGetter getter,
                BlockPos pos, Direction direction) {
            return state.getValue(POWERED) ? 15 : 0;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (!level.isClientSide) {
                level.scheduleTick(pos, this, 1);
            }
        }

        @Override
        public void tick(BlockState state, ServerLevel level,
                BlockPos pos, RandomSource random) {
            if (!isHeavyDoorState(level.getBlockState(pos.below()).getBlock())) {
                level.removeBlock(pos, false);
                return;
            }

            boolean powered = hasExternalPower(level, pos);
            if (state.getValue(POWERED) != powered) {
                level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
                level.updateNeighborsAt(pos, this);
            }
            level.scheduleTick(pos, this, 2);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(POWERED);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.emptyList();
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            // Intentionally empty: this block is only a redstone bridge.
        }

        private static boolean hasExternalPower(Level level, BlockPos relayPos) {
            return probe(level, relayPos, relayPos)
                    || probe(level, relayPos.above(), relayPos);
        }

        private static boolean probe(Level level, BlockPos probePos, BlockPos relayPos) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = probePos.relative(direction);
                if (neighborPos.equals(relayPos)) {
                    continue;
                }
                BlockState neighbor = level.getBlockState(neighborPos);
                if (neighbor.is(RELAY.get())) {
                    continue;
                }
                Direction towardProbe = direction.getOpposite();
                if (neighbor.getSignal(level, neighborPos, towardProbe) > 0
                        || neighbor.getDirectSignal(level, neighborPos, towardProbe) > 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
