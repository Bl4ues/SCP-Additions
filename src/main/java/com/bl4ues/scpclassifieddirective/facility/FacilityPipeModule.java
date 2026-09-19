package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Static SL2 wall-pipe props; usable on ordinary walls and transformed surfaces. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FacilityPipeModule {
    private static final String[] FINISHES = {"clean", "caution", "blue"};
    private static final String[] SUPPORTS = {"both", "right", "left", "none"};

    private FacilityPipeModule() {
    }

    public static List<Item> creativeItems() {
        List<Item> result = new ArrayList<>(12);
        for (String finish : FINISHES) {
            for (String supports : SUPPORTS) {
                Item item = ForgeRegistries.ITEMS.getValue(id(finish, supports));
                if (item != null) result.add(item);
            }
        }
        return List.copyOf(result);
    }

    private static ResourceLocation id(String finish, String supports) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                "wall_pipe_" + finish + "_" + supports);
    }

    private static String displayName(String finish, String supports) {
        String pipe = switch (finish) {
            case "caution" -> "Caution-Marked Wall Pipe";
            case "blue" -> "Blue Utility Pipe";
            default -> "Steel Wall Pipe";
        };
        String fittings = switch (supports) {
            case "both" -> "Dual Brackets";
            case "right" -> "Right Bracket";
            case "left" -> "Left Bracket";
            default -> "No Brackets";
        };
        return pipe + " (" + fittings + ")";
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        for (String finish : FINISHES) {
            for (String supports : SUPPORTS) {
                ResourceLocation id = id(finish, supports);
                event.register(ForgeRegistries.Keys.BLOCKS, id, PipeBlock::new);
                event.register(ForgeRegistries.Keys.ITEMS, id, () ->
                        new PipeItem(ForgeRegistries.BLOCKS.getValue(id),
                                displayName(finish, supports)));
            }
        }
    }

    private static final class PipeItem extends BlockItem {
        private final Component name;

        private PipeItem(Block block, String name) {
            super(block, new Item.Properties());
            this.name = Component.literal(name);
        }

        @Override
        public Component getName(ItemStack stack) {
            return name;
        }
    }

    public static final class PipeBlock extends HorizontalDirectionalBlock {
        private static final VoxelShape NORTH = Block.box(0, 10, 11, 16, 15, 16);
        private static final VoxelShape EAST = Block.box(0, 10, 0, 5, 15, 16);
        private static final VoxelShape SOUTH = Block.box(0, 10, 0, 16, 15, 5);
        private static final VoxelShape WEST = Block.box(11, 10, 0, 16, 15, 16);

        private PipeBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction face = context.getClickedFace();
            // Do not require a vanilla sturdy wall: curved Surface Tool walls
            // are represented by transformed geometry instead of a full cube.
            return defaultBlockState().setValue(FACING,
                    face.getAxis().isHorizontal() ? face
                            : context.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                default -> NORTH;
            };
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }
    }
}
