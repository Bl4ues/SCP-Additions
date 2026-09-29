package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.CeilingLampAudioClient;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionSavedData;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** SL2 utility props whose models are authored from the SCP Unity references. */
public final class Sl2FacilityPropsModule {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape PNEUMATIC_NORTH = Shapes.or(
            Block.box(3.0D, 3.25D, 14.0D, 13.0D, 11.0D, 16.0D),
            Block.box(3.75D, 1.25D, 15.25D, 8.75D, 3.25D, 16.0D),
            Block.box(4.75D, 0.25D, 13.98D, 7.80D, 3.25D, 15.27D),
            Block.box(6.25D, 0.25D, 13.98D, 9.30D, 3.25D, 15.27D),
            Block.box(7.50D, 0.25D, 14.70D, 10.80D, 3.25D, 14.80D));
    private static final VoxelShape OUTLET_NORTH =
            Block.box(6.25D, 3.25D, 15.75D, 9.75D, 8.75D, 16.0D);

    public static final RegistryObject<Block> PNEUMATIC_PANEL =
            BLOCKS.register("pneumatic_panel",
                    () -> new WallFixtureBlock(PNEUMATIC_NORTH));
    public static final RegistryObject<Block> OUTLET =
            BLOCKS.register("outlet", () -> new WallFixtureBlock(OUTLET_NORTH));
    public static final RegistryObject<Block> ROUND_LAMP =
            BLOCKS.register("sl2_round_lamp", RoundLampBlock::new);

    public static final RegistryObject<Item> PNEUMATIC_PANEL_ITEM =
            ITEMS.register("pneumatic_panel", () -> new DecorativeItem(
                    PNEUMATIC_PANEL.get()));
    public static final RegistryObject<Item> OUTLET_ITEM =
            ITEMS.register("outlet", () -> new DecorativeItem(OUTLET.get()));
    public static final RegistryObject<Item> ROUND_LAMP_ITEM =
            ITEMS.register("sl2_round_lamp", () -> new RoundLampItem(
                    ROUND_LAMP.get()));

    private Sl2FacilityPropsModule() { }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static Item pneumaticPanelItem() { return PNEUMATIC_PANEL_ITEM.get(); }
    public static Item outletItem() { return OUTLET_ITEM.get(); }
    public static Item roundLampItem() { return ROUND_LAMP_ITEM.get(); }

    public static boolean isRoundLamp(Block block) {
        return block != null && block == ROUND_LAMP.get();
    }

    public static boolean isRoundLamp(BlockState state) {
        return state != null && isRoundLamp(state.getBlock());
    }

    public static boolean isWallFixture(Block block) {
        return block != null
                && (block == PNEUMATIC_PANEL.get() || block == OUTLET.get());
    }

    public static boolean isRigidFixture(Block block) {
        // Thin wall fixtures stay rigid. Round Lamp intentionally deforms with
        // Surface geometry so its full circular face follows curved walls and
        // linked ceilings instead of cutting through them as one flat plane.
        return isWallFixture(block);
    }

    public static BlockState roundLampState(Direction facing, boolean lit) {
        Direction safe = facing == null ? Direction.UP : facing;
        return ROUND_LAMP.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, safe)
                .setValue(LIT, lit);
    }

    public static boolean setWorldLampLit(ServerLevel level, BlockPos pos,
            boolean lit, boolean transitionSound) {
        if (level == null || pos == null || !level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!isRoundLamp(state) || state.getValue(LIT) == lit) return false;
        level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
        if (transitionSound) playTransition(level, pos, lit);
        return true;
    }

    public static void playTransition(Level level, BlockPos pos,
            boolean turningOn) {
        if (level == null || pos == null || level.isClientSide) return;
        level.playSound(null, pos, turningOn
                        ? ScpClassifiedDirectiveModSounds.LAMP_ON.get()
                        : ScpClassifiedDirectiveModSounds.LAMP_OFF.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.55F, 1.0F);
    }


    public record SurfaceLampRef(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            boolean overlay, BlockPos worldPos) {
    }

    /**
     * Surface lamps are stored in transform saved data rather than as vanilla
     * blocks. Expose the ones physically inside a mapped room so SCP-079 and
     * client audio can treat them exactly like ordinary Round Lamps.
     */
    public static List<SurfaceLampRef> surfaceLampsInRoom(ServerLevel level,
            FacilityRoomSnapshot room, boolean litOnly) {
        if (level == null || room == null || level.getServer() == null) {
            return List.of();
        }
        List<SurfaceLampRef> result = new ArrayList<>();
        for (ConstructionSurface surface :
                TransformConstructionSavedData.get(level.getServer()).surfaces()) {
            if (!surface.dimension().equals(level.dimension().location())) continue;
            for (var entry : surface.attachments().entrySet()) {
                BlockState state = entry.getValue().state();
                if (!isRoundLamp(state)
                        || litOnly && !state.getValue(LIT)) continue;
                var slot = entry.getKey();
                var center = TransformSurfaceGeometry.cellCenter(surface, slot,
                        TransformSurfaceGeometry.MAIN_SIDE, false);
                if (insideRoom(room, center.x, center.y, center.z)) {
                    result.add(new SurfaceLampRef(surface.id(), slot,
                            TransformSurfaceGeometry.MAIN_SIDE, false,
                            BlockPos.containing(center)));
                }
            }
            for (var entry : surface.overlays().entrySet()) {
                BlockState state = entry.getValue().state();
                if (!isRoundLamp(state)
                        || litOnly && !state.getValue(LIT)) continue;
                var key = entry.getKey();
                var center = TransformSurfaceGeometry.cellCenter(surface,
                        key.slot(), key.normalSign(), true);
                if (insideRoom(room, center.x, center.y, center.z)) {
                    result.add(new SurfaceLampRef(surface.id(), key.slot(),
                            key.normalSign(), true,
                            BlockPos.containing(center)));
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean insideRoom(FacilityRoomSnapshot room,
            double x, double y, double z) {
        for (FacilityFloorPatch patch : room.patches()) {
            if (y < patch.y() - 1.0D || y > patch.y() + 8.5D) continue;
            if (patch.containsXZ(x, z)) return true;
        }
        return false;
    }

    /**
     * Runtime state mutation for a lamp attached to a curved Surface. This is a
     * quiet state update: geometry ownership stays untouched while proxy light,
     * renderer state and clients receive the one-cell delta.
     */
    public static boolean setSurfaceLampLit(ServerLevel level,
            SurfaceLampRef ref, boolean lit, boolean transitionSound) {
        if (level == null || ref == null || level.getServer() == null) {
            return false;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(level.getServer());
        ConstructionSurface surface = data.surface(ref.surfaceId());
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;

        ConstructionSurface.SurfaceAttachment attachment = ref.overlay()
                ? surface.overlay(ref.slot(), ref.normalSign())
                : surface.attachments().get(ref.slot());
        if (attachment == null || !isRoundLamp(attachment.state())
                || attachment.state().getValue(LIT) == lit) return false;

        BlockState updated = attachment.state().setValue(LIT, lit);
        ConstructionSurface next = ref.overlay()
                ? surface.withOverlay(ref.slot(), ref.normalSign(), updated,
                        attachment.deform())
                : surface.withAttachment(ref.slot(), updated,
                        attachment.deform());
        data.putSurfaceState(next);
        TransformConstructionManager.refreshSurfaceSlotRuntime(
                level.getServer(), surface.id(), ref.slot());
        if (ref.overlay()) {
            TransformConstructionNetwork.broadcastSurfaceOverlay(level,
                    surface.id(), ref.slot(), ref.normalSign(), updated,
                    attachment.deform());
        } else {
            TransformConstructionNetwork.broadcastSurfaceSlot(level,
                    surface.id(), ref.slot(), updated, attachment.deform());
        }
        if (transitionSound) playTransition(level, ref.worldPos(), lit);
        return true;
    }

    private static final class WallFixtureBlock
            extends HorizontalDirectionalBlock {
        private final VoxelShape northShape;

        private WallFixtureBlock(VoxelShape northShape) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            this.northShape = northShape;
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction face = context.getClickedFace();
            if (face.getAxis() == Direction.Axis.Y) return null;
            BlockState state = defaultBlockState().setValue(FACING, face);
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            return WallMountedSupportEvents.hasWallSupport(level, pos,
                    state.getValue(FACING));
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighbor, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            return state.canSurvive(level, pos)
                    ? super.updateShape(state, direction, neighbor, level,
                            pos, neighborPos)
                    : Blocks.AIR.defaultBlockState();
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

    private static final class RoundLampBlock extends DirectionalBlock {
        private RoundLampBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(0.8F, 8.0F)
                    .lightLevel(state -> state.getValue(LIT) ? 15 : 0)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.UP).setValue(LIT, true));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, LIT);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = roundLampState(context.getClickedFace(), true);
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            Direction facing = state.getValue(FACING);
            BlockPos support = pos.relative(facing.getOpposite());
            return level.getBlockState(support).isFaceSturdy(
                    level, support, facing);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighbor, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            return state.canSurvive(level, pos)
                    ? super.updateShape(state, direction, neighbor, level,
                            pos, neighborPos)
                    : Blocks.AIR.defaultBlockState();
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            if (level.isClientSide) {
                if (state.getValue(LIT)) {
                    CeilingLampAudioClient.ensureLoop(level, pos);
                }
                return;
            }
            if (oldState.getBlock() != this && state.getValue(LIT)) {
                playTransition(level, pos, true);
            }
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) {
            if (!state.getValue(LIT)
                    && !Scp079RoomAbilityManager.isLightSuppressed(level, pos)) {
                setWorldLampLit(level, pos, true, true);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos,
                RandomSource random) {
            if (state.getValue(LIT)) {
                CeilingLampAudioClient.ensureLoop(level, pos);
            }
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return roundLampShape(state.getValue(FACING));
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
    }

    private static final class DecorativeItem extends BlockItem {
        private DecorativeItem(Block block) {
            super(block, new Item.Properties());
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

    private static final class RoundLampItem extends BlockItem {
        private RoundLampItem(Block block) {
            super(block, new Item.Properties());
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_classified_directive.sublevel_2")
                    .withStyle(ChatFormatting.BLUE));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }

    private static VoxelShape roundLampShape(Direction facing) {
        // Match the model's 25% centre-anchored reduction. The same centred
        // local footprint is then rotated for vanilla placement or deformed by
        // a curved Surface, so the lamp never shrinks toward one edge.
        final double min = 4.8125D;
        final double max = 11.1875D;
        return switch (facing) {
            case DOWN -> Block.box(min, 15.5, min, max, 16, max);
            case NORTH -> Block.box(min, min, 15.5, max, max, 16);
            case SOUTH -> Block.box(min, min, 0, max, max, 0.5);
            case WEST -> Block.box(15.5, min, min, 16, max, max);
            case EAST -> Block.box(0, min, min, 0.5, max, max);
            default -> Block.box(min, 0, min, max, 0.5, max);
        };
    }

    /**
     * One real block-light source for a Surface-mounted lamp. Collision can be
     * tessellated over several world cells on a curve; using every collision
     * fragment as a light source creates the unnatural bright stripe seen on
     * curved walls. Keep geometry deformed but lighting point-like and let
     * Minecraft's normal block-light propagation illuminate every surface.
     */
    public static Vec3 surfaceLampLightPosition(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            boolean overlay) {
        return TransformSurfaceGeometry.lightPosition(surface, slot,
                roundLampState(Direction.SOUTH, true), normalSign, overlay);
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
