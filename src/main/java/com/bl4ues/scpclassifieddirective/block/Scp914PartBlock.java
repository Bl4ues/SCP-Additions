package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

/** Invisible selectable structure/collision cell belonging to the large SCP-914. */
public final class Scp914PartBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Role> ROLE = EnumProperty.create("role", Role.class);

    private static final VoxelShape FULL = Shapes.block();
    private static final VoxelShape BOTTOM_PLATE = Block.box(0.0D, 0.0D, 0.0D,
            16.0D, 2.0D, 16.0D);
    private static final VoxelShape ROOF_BAND = Block.box(0.0D, 7.0D, 0.0D,
            16.0D, 10.0D, 16.0D);

    public enum Kind {
        RESERVATION,
        SOLID,
        DOOR
    }

    private enum DoorPose {
        NONE,
        CLOSED,
        OPEN
    }

    public enum Role implements StringRepresentable {
        RESERVED("reserved"),
        FRAME("frame"),
        BODY_FRONT("body_front"),
        BODY_BACK("body_back"),
        BODY_SIDE_NEG("body_side_neg"),
        BODY_SIDE_POS("body_side_pos"),
        BODY_FLOOR("body_floor"),
        BODY_ROOF("body_roof"),
        CABIN_SIDE_NEG("cabin_side_neg"),
        CABIN_SIDE_POS("cabin_side_pos"),
        CABIN_SIDE_CENTER("cabin_side_center"),
        CABIN_BACK("cabin_back"),
        CABIN_ROOF("cabin_roof"),

        DOOR_N6_LOWER("door_n6_lower", -6, 0, 0, DoorPose.CLOSED),
        DOOR_N5_LOWER("door_n5_lower", -5, 0, 0, DoorPose.CLOSED),
        DOOR_N4_LOWER("door_n4_lower", -4, 0, 0, DoorPose.CLOSED),
        DOOR_N6_UPPER("door_n6_upper", -6, 1, 0, DoorPose.CLOSED),
        DOOR_N5_UPPER("door_n5_upper", -5, 1, 0, DoorPose.CLOSED),
        DOOR_N4_UPPER("door_n4_upper", -4, 1, 0, DoorPose.CLOSED),
        DOOR_N6_TOP("door_n6_top", -6, 2, 0, DoorPose.CLOSED),
        DOOR_N5_TOP("door_n5_top", -5, 2, 0, DoorPose.CLOSED),
        DOOR_N4_TOP("door_n4_top", -4, 2, 0, DoorPose.CLOSED),

        DOOR_P4_LOWER("door_p4_lower", 4, 0, 0, DoorPose.CLOSED),
        DOOR_P5_LOWER("door_p5_lower", 5, 0, 0, DoorPose.CLOSED),
        DOOR_P6_LOWER("door_p6_lower", 6, 0, 0, DoorPose.CLOSED),
        DOOR_P4_UPPER("door_p4_upper", 4, 1, 0, DoorPose.CLOSED),
        DOOR_P5_UPPER("door_p5_upper", 5, 1, 0, DoorPose.CLOSED),
        DOOR_P6_UPPER("door_p6_upper", 6, 1, 0, DoorPose.CLOSED),
        DOOR_P4_TOP("door_p4_top", 4, 2, 0, DoorPose.CLOSED),
        DOOR_P5_TOP("door_p5_top", 5, 2, 0, DoorPose.CLOSED),
        DOOR_P6_TOP("door_p6_top", 6, 2, 0, DoorPose.CLOSED),

        DOOR_N_OPEN1_LOWER("door_n_o1_l", -6, 0, 1, DoorPose.OPEN),
        DOOR_N_OPEN1_UPPER("door_n_o1_u", -6, 1, 1, DoorPose.OPEN),
        DOOR_N_OPEN1_TOP("door_n_o1_t", -6, 2, 1, DoorPose.OPEN),
        DOOR_N_OPEN2_LOWER("door_n_o2_l", -6, 0, 2, DoorPose.OPEN),
        DOOR_N_OPEN2_UPPER("door_n_o2_u", -6, 1, 2, DoorPose.OPEN),
        DOOR_N_OPEN2_TOP("door_n_o2_t", -6, 2, 2, DoorPose.OPEN),
        DOOR_P_OPEN1_LOWER("door_p_o1_l", 6, 0, 1, DoorPose.OPEN),
        DOOR_P_OPEN1_UPPER("door_p_o1_u", 6, 1, 1, DoorPose.OPEN),
        DOOR_P_OPEN1_TOP("door_p_o1_t", 6, 2, 1, DoorPose.OPEN),
        DOOR_P_OPEN2_LOWER("door_p_o2_l", 6, 0, 2, DoorPose.OPEN),
        DOOR_P_OPEN2_UPPER("door_p_o2_u", 6, 1, 2, DoorPose.OPEN),
        DOOR_P_OPEN2_TOP("door_p_o2_t", 6, 2, 2, DoorPose.OPEN);

        private final String serializedName;
        private final int doorSide;
        private final int doorY;
        private final int doorForward;
        private final DoorPose doorPose;

        Role(String serializedName) {
            this(serializedName, 0, 0, 0, DoorPose.NONE);
        }

        Role(String serializedName, int doorSide, int doorY,
                int doorForward, DoorPose doorPose) {
            this.serializedName = serializedName;
            this.doorSide = doorSide;
            this.doorY = doorY;
            this.doorForward = doorForward;
            this.doorPose = doorPose;
        }

        public boolean isDoor() {
            return doorPose != DoorPose.NONE;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    private final Kind kind;

    public Scp914PartBlock(Kind kind) {
        super(properties(kind));
        this.kind = kind;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ROLE, Role.RESERVED));
    }

    private static BlockBehaviour.Properties properties(Kind kind) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .strength(6.0F, 1200.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noLootTable();
        if (kind == Kind.RESERVATION) properties = properties.noCollission();
        if (kind == Kind.DOOR) properties = properties.dynamicShape();
        return properties;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROLE);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        Role role = state.getValue(ROLE);
        if (kind == Kind.RESERVATION) {
            return role == Role.FRAME ? FULL : Shapes.empty();
        }
        if (kind == Kind.DOOR) {
            return doorActive(level, pos, state, role)
                    ? doorShape(state, role) : Shapes.empty();
        }
        return staticShape(state, role);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        if (kind == Kind.RESERVATION) return Shapes.empty();
        Role role = state.getValue(ROLE);
        if (kind == Kind.DOOR) {
            return doorActive(level, pos, state, role)
                    ? doorShape(state, role) : Shapes.empty();
        }
        return staticShape(state, role);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
            BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static VoxelShape staticShape(BlockState state, Role role) {
        Direction facing = state.getValue(FACING);
        return switch (role) {
            case BODY_FRONT -> frontSlab(facing, 6.0D);
            case BODY_BACK -> edgeSlab(facing.getOpposite(), 3.0D);
            case BODY_SIDE_NEG, BODY_SIDE_POS -> sideCenterSlab(facing, 3.0D);
            case BODY_FLOOR -> BOTTOM_PLATE;
            case BODY_ROOF, CABIN_ROOF -> ROOF_BAND;
            case CABIN_SIDE_NEG -> sideSlab(facing, false, 2.0D);
            case CABIN_SIDE_POS -> sideSlab(facing, true, 2.0D);
            case CABIN_SIDE_CENTER -> sideCenterSlab(facing, 2.0D);
            case CABIN_BACK -> frontSlab(facing, 2.0D);
            default -> Shapes.empty();
        };
    }

    public static VoxelShape frontSlab(Direction facing, double thickness) {
        return edgeSlab(facing, thickness);
    }

    private static VoxelShape sideSlab(Direction facing,
            boolean positiveLocalX, double thickness) {
        Direction localPositiveX = facing.getCounterClockWise();
        Direction edge = positiveLocalX
                ? localPositiveX : localPositiveX.getOpposite();
        return edgeSlab(edge, thickness);
    }

    private static VoxelShape sideCenterSlab(Direction facing, double thickness) {
        double min = 8.0D - thickness / 2.0D;
        double max = 8.0D + thickness / 2.0D;
        return facing.getAxis() == Direction.Axis.Z
                ? Block.box(min, 0.0D, 0.0D, max, 16.0D, 16.0D)
                : Block.box(0.0D, 0.0D, min, 16.0D, 16.0D, max);
    }

    private static VoxelShape edgeSlab(Direction direction, double thickness) {
        return switch (direction) {
            case NORTH -> Block.box(0.0D, 0.0D, 0.0D,
                    16.0D, 16.0D, thickness);
            case SOUTH -> Block.box(0.0D, 0.0D, 16.0D - thickness,
                    16.0D, 16.0D, 16.0D);
            case EAST -> Block.box(16.0D - thickness, 0.0D, 0.0D,
                    16.0D, 16.0D, 16.0D);
            case WEST -> Block.box(0.0D, 0.0D, 0.0D,
                    thickness, 16.0D, 16.0D);
            default -> Shapes.empty();
        };
    }

    private static VoxelShape doorShape(BlockState state, Role role) {
        double maxY = role.doorY == 2 ? 5.0D : 16.0D;
        Direction facing = state.getValue(FACING);
        if (role.doorPose == DoorPose.CLOSED) {
            return edgeSlabWithHeight(facing, 3.0D, maxY);
        }
        return openDoorSlab(facing, maxY);
    }

    private static VoxelShape edgeSlabWithHeight(Direction direction,
            double thickness, double maxY) {
        return switch (direction) {
            case NORTH -> Block.box(0.0D, 0.0D, 0.0D,
                    16.0D, maxY, thickness);
            case SOUTH -> Block.box(0.0D, 0.0D, 16.0D - thickness,
                    16.0D, maxY, 16.0D);
            case EAST -> Block.box(16.0D - thickness, 0.0D, 0.0D,
                    16.0D, maxY, 16.0D);
            case WEST -> Block.box(0.0D, 0.0D, 0.0D,
                    thickness, maxY, 16.0D);
            default -> Shapes.empty();
        };
    }

    private static VoxelShape openDoorSlab(Direction facing, double maxY) {
        return facing.getAxis() == Direction.Axis.Z
                ? Block.box(6.0D, 0.0D, 0.0D, 10.0D, maxY, 16.0D)
                : Block.box(0.0D, 0.0D, 6.0D, 16.0D, maxY, 10.0D);
    }

    private static boolean doorActive(BlockGetter level, BlockPos partPos,
            BlockState state, Role role) {
        if (!role.isDoor()) return false;
        Direction facing = state.getValue(FACING);
        Direction localPositiveX = facing.getCounterClockWise();
        BlockPos controllerPos = partPos
                .relative(localPositiveX, -role.doorSide)
                .relative(facing, -role.doorForward)
                .below(role.doorY);
        if (!(level.getBlockEntity(controllerPos)
                instanceof Scp914BlockEntity machine)
                || Scp914Structure.facing(machine.getBlockState()) != facing) {
            return false;
        }
        long elapsed = machine.refiningElapsedTicks();
        boolean closed = machine.isRefining()
                && elapsed >= Scp914BlockEntity.DOOR_CLOSED_TICK
                && elapsed < Scp914BlockEntity.PROCESS_AND_OPEN_TICK;
        return role.doorPose == DoorPose.CLOSED ? closed : !closed;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(Scp914Module.SCP_914_ITEM.get());
    }
}
