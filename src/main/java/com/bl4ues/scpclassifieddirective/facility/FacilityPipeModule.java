package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionSavedData;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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
import org.jetbrains.annotations.Nullable;

/** Three decorative pipe finishes share one physical connection pattern. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FacilityPipeModule {
    private static final String[] FINISHES = {"clean", "caution", "blue"};
    // Preserve all twelve registry IDs for existing worlds. Only the three
    // original dual-bracket items appear publicly; the bracket is now a state.
    private static final String[] SUPPORTS = {"both", "right", "left", "none"};
    public static final IntegerProperty BRACKETS =
            IntegerProperty.create("brackets", 0, 3);
    private static final int NONE = 0;
    private static final int START = 1;
    private static final int END = 2;
    private static final int BOTH = START | END;
    private static final int MAX_RUN = 512;

    private FacilityPipeModule() {
    }

    public static List<Item> creativeItems() {
        List<Item> result = new ArrayList<>(FINISHES.length);
        for (String finish : FINISHES) {
            Item item = ForgeRegistries.ITEMS.getValue(id(finish, "both"));
            if (item != null) result.add(item);
        }
        return List.copyOf(result);
    }

    public static ItemStack pick(BlockState state) {
        if (!(state.getBlock() instanceof PipeBlock pipe)) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(id(pipe.finish, "both"));
        return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private static ResourceLocation id(String finish, String supports) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                "wall_pipe_" + finish + "_" + supports);
    }

    private static String displayName(String finish) {
        return switch (finish) {
            case "caution" -> "Caution-Marked Wall Pipe";
            case "blue" -> "Blue Utility Pipe";
            default -> "Steel Wall Pipe";
        };
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        for (String finish : FINISHES) {
            for (String supports : SUPPORTS) {
                ResourceLocation id = id(finish, supports);
                event.register(ForgeRegistries.Keys.BLOCKS, id,
                        () -> new PipeBlock(finish, supports));
                event.register(ForgeRegistries.Keys.ITEMS, id, () ->
                        new PipeItem(ForgeRegistries.BLOCKS.getValue(id),
                                displayName(finish)));
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

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, level, tooltip, flag);
            tooltip.add(Component.literal("Decorative Only")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean sameRun(BlockState first, BlockState next) {
        return first != null && next != null
                && first.getBlock() instanceof PipeBlock
                && next.getBlock() instanceof PipeBlock
                && first.getValue(HorizontalDirectionalBlock.FACING)
                == next.getValue(HorizontalDirectionalBlock.FACING);
    }

    private static int supportsAt(int index, int length) {
        boolean start = index == 0;
        boolean end = (length == 1 && index == 0)
                || (length == 2 && index == 1)
                || (index > 0 && index % 3 == 2);
        return (start ? START : NONE) | (end ? END : NONE);
    }

    private static void refreshWorld(Level level, BlockPos changed,
            Direction facing, boolean removed) {
        if (level.isClientSide) return;
        Direction along = facing.getClockWise();
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos seed : new BlockPos[]{changed,
                changed.relative(along), changed.relative(along.getOpposite())}) {
            if (seed.equals(changed) && removed || visited.contains(seed)) continue;
            BlockState seedState = level.getBlockState(seed);
            if (!(seedState.getBlock() instanceof PipeBlock)
                    || seedState.getValue(HorizontalDirectionalBlock.FACING)
                    != facing) continue;
            BlockPos first = seed;
            for (int i = 0; i < MAX_RUN; i++) {
                BlockPos prior = first.relative(along.getOpposite());
                if (removed && prior.equals(changed)
                        || !sameRun(seedState, level.getBlockState(prior))) break;
                first = prior;
            }
            List<BlockPos> run = new ArrayList<>();
            for (int i = 0; i < MAX_RUN; i++) {
                BlockPos cursor = first.relative(along, i);
                if (removed && cursor.equals(changed)
                        || !sameRun(seedState, level.getBlockState(cursor))) break;
                run.add(cursor);
                visited.add(cursor);
            }
            for (int i = 0; i < run.size(); i++) {
                BlockPos pos = run.get(i);
                BlockState state = level.getBlockState(pos);
                int brackets = supportsAt(i, run.size());
                if (state.getValue(BRACKETS) != brackets) {
                    // Geometry and collision remain unchanged. Avoid recursively
                    // notifying every neighbor for a purely visual bracket.
                    level.setBlock(pos, state.setValue(BRACKETS, brackets),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** Recompute logical group connections, including mixtures of finishes. */
    public static void refreshGroup(ServerLevel level, UUID id) {
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        TransformGroup group = data.group(id);
        if (group == null) return;
        Set<TransformGroup.GridPos> visited = new HashSet<>();
        TransformGroup updated = group;
        List<TransformGroup.GridPos> changed = new ArrayList<>();
        for (var entry : group.cells().entrySet()) {
            TransformGroup.GridPos seed = entry.getKey();
            BlockState seedState = entry.getValue();
            if (!(seedState.getBlock() instanceof PipeBlock)
                    || visited.contains(seed)) continue;
            Direction along = seedState.getValue(
                    HorizontalDirectionalBlock.FACING).getClockWise();
            TransformGroup.GridPos first = seed;
            for (int i = 0; i < MAX_RUN; i++) {
                TransformGroup.GridPos prior = first.offset(
                        -along.getStepX(), 0, -along.getStepZ());
                if (!sameRun(seedState, group.cells().get(prior))) break;
                first = prior;
            }
            List<TransformGroup.GridPos> run = new ArrayList<>();
            for (int i = 0; i < MAX_RUN; i++) {
                TransformGroup.GridPos cursor = first.offset(
                        along.getStepX() * i, 0, along.getStepZ() * i);
                if (!sameRun(seedState, group.cells().get(cursor))) break;
                run.add(cursor);
                visited.add(cursor);
            }
            for (int i = 0; i < run.size(); i++) {
                TransformGroup.GridPos pos = run.get(i);
                BlockState state = group.cells().get(pos);
                int brackets = supportsAt(i, run.size());
                if (state.getValue(BRACKETS) != brackets) {
                    updated = updated.withCell(pos,
                            state.setValue(BRACKETS, brackets));
                    changed.add(pos);
                }
            }
        }
        if (changed.isEmpty()) return;
        data.putGroupState(updated);
        for (TransformGroup.GridPos cell : changed) {
            TransformConstructionNetwork.broadcastGroupCell(level, id, cell,
                    updated.cells().get(cell));
        }
    }

    /** Each Surface layer is an independent run on the physical curved face. */
    public static void refreshSurface(ServerLevel level, UUID id) {
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(id);
        if (surface == null) return;
        ConstructionSurface updated = surface;
        record Change(ConstructionSurface.SurfaceSlot slot, int layer,
                      BlockState state, boolean deform) { }
        List<Change> changes = new ArrayList<>();
        for (int layer : new int[]{0, -1, 1}) {
            int side = layer == 0 ? TransformSurfaceGeometry.MAIN_SIDE : layer;
            int frameSign = (surface.flipped() ? -1 : 1) * side;
            Set<ConstructionSurface.SurfaceSlot> visited = new HashSet<>();
            for (int row = 0; row < surface.rows(); row++) {
                for (int column = 0; column < surface.columns(); column++) {
                    ConstructionSurface.SurfaceSlot seed =
                            new ConstructionSurface.SurfaceSlot(column, row);
                    ConstructionSurface.SurfaceAttachment initial = layer == 0
                            ? surface.attachments().get(seed)
                            : surface.overlay(seed, layer);
                    if (initial == null
                            || !(initial.state().getBlock() instanceof PipeBlock)
                            || visited.contains(seed)) continue;
                    BlockState state = initial.state();
                    // Pipe axes on Surface must follow its local X direction.
                    int localX = state.getValue(
                            HorizontalDirectionalBlock.FACING)
                            .getClockWise().getStepX();
                    if (localX == 0) continue;
                    int step = localX * frameSign;
                    ConstructionSurface.SurfaceSlot first = seed;
                    for (int i = 0; i < MAX_RUN; i++) {
                        ConstructionSurface.SurfaceSlot prior =
                                new ConstructionSurface.SurfaceSlot(
                                        first.column() - step, row);
                        ConstructionSurface.SurfaceAttachment candidate =
                                layer == 0 ? surface.attachments().get(prior)
                                        : surface.overlay(prior, layer);
                        if (candidate == null
                                || !sameRun(state, candidate.state())) break;
                        first = prior;
                    }
                    List<ConstructionSurface.SurfaceSlot> run =
                            new ArrayList<>();
                    for (int i = 0; i < MAX_RUN; i++) {
                        ConstructionSurface.SurfaceSlot current =
                                new ConstructionSurface.SurfaceSlot(
                                        first.column() + step * i, row);
                        ConstructionSurface.SurfaceAttachment candidate =
                                layer == 0 ? surface.attachments().get(current)
                                        : surface.overlay(current, layer);
                        if (candidate == null
                                || !sameRun(state, candidate.state())) break;
                        run.add(current);
                        visited.add(current);
                    }
                    for (int i = 0; i < run.size(); i++) {
                        ConstructionSurface.SurfaceSlot pos = run.get(i);
                        ConstructionSurface.SurfaceAttachment attachment =
                                layer == 0 ? surface.attachments().get(pos)
                                        : surface.overlay(pos, layer);
                        int brackets = supportsAt(i, run.size());
                        if (attachment.state().getValue(BRACKETS) == brackets) {
                            continue;
                        }
                        BlockState next = attachment.state().setValue(
                                BRACKETS, brackets);
                        updated = layer == 0
                                ? updated.withAttachment(pos, next,
                                        attachment.deform())
                                : updated.withOverlay(pos, layer, next,
                                        attachment.deform());
                        changes.add(new Change(pos, layer, next,
                                attachment.deform()));
                    }
                }
            }
        }
        if (changes.isEmpty()) return;
        data.putSurfaceState(updated);
        for (Change change : changes) {
            if (change.layer() == 0) {
                TransformConstructionNetwork.broadcastSurfaceSlot(level, id,
                        change.slot(), change.state(), change.deform());
            } else {
                TransformConstructionNetwork.broadcastSurfaceOverlay(level, id,
                        change.slot(), change.layer(), change.state(),
                        change.deform());
            }
        }
    }

    public static final class PipeBlock extends HorizontalDirectionalBlock {
        private static final VoxelShape NORTH = Block.box(0, 10, 11, 16, 15, 16);
        private static final VoxelShape EAST = Block.box(0, 10, 0, 5, 15, 16);
        private static final VoxelShape SOUTH = Block.box(0, 10, 0, 16, 15, 5);
        private static final VoxelShape WEST = Block.box(11, 10, 0, 16, 15, 16);
        private final String finish;

        private PipeBlock(String finish, String originalSupports) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            this.finish = finish;
            int initial = switch (originalSupports) {
                case "right" -> START;
                case "left" -> END;
                case "none" -> NONE;
                default -> BOTH;
            };
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(BRACKETS, initial));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, BRACKETS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction face = context.getClickedFace();
            // Do not require a vanilla sturdy wall: curved Surface walls are
            // represented by transformed geometry rather than a solid cube.
            return defaultBlockState().setValue(FACING,
                    face.getAxis().isHorizontal() ? face
                            : context.getHorizontalDirection().getOpposite());
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (!oldState.is(this)) {
                refreshWorld(level, pos, state.getValue(FACING), false);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState next, boolean moving) {
            if (!next.is(this)) {
                refreshWorld(level, pos, state.getValue(FACING), true);
            }
            super.onRemove(state, level, pos, next, moving);
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
            return state.setValue(FACING,
                    rotation.rotate(state.getValue(FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }
    }
}
