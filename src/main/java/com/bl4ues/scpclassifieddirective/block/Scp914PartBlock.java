package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible semantic structural cell used by the oversized SCP-914. */
public final class Scp914PartBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Role> ROLE = EnumProperty.create("role", Role.class);

    private static final VoxelShape SIDE_X = Block.box(7.0D, 0.0D, 0.0D,
            9.0D, 16.0D, 16.0D);
    private static final VoxelShape SIDE_Z = Block.box(0.0D, 0.0D, 7.0D,
            16.0D, 16.0D, 9.0D);
    private static final VoxelShape ROOF = Block.box(0.0D, 7.0D, 0.0D,
            16.0D, 10.0D, 16.0D);

    public enum Kind {
        RESERVATION,
        SOLID,
        DOOR
    }

    public enum Role implements StringRepresentable {
        RESERVED("reserved"),
        BODY("body"),
        BODY_FRONT("body_front"),
        FLOOR("floor"),
        CABIN_SIDE("cabin_side"),
        CABIN_BACK("cabin_back"),
        CABIN_ROOF("cabin_roof"),
        DOOR_N6_LOWER("door_n6_lower", -6, 0),
        DOOR_N5_LOWER("door_n5_lower", -5, 0),
        DOOR_N4_LOWER("door_n4_lower", -4, 0),
        DOOR_N6_UPPER("door_n6_upper", -6, 1),
        DOOR_N5_UPPER("door_n5_upper", -5, 1),
        DOOR_N4_UPPER("door_n4_upper", -4, 1),
        DOOR_P4_LOWER("door_p4_lower", 4, 0),
        DOOR_P5_LOWER("door_p5_lower", 5, 0),
        DOOR_P6_LOWER("door_p6_lower", 6, 0),
        DOOR_P4_UPPER("door_p4_upper", 4, 1),
        DOOR_P5_UPPER("door_p5_upper", 5, 1),
        DOOR_P6_UPPER("door_p6_upper", 6, 1);

        private final String serializedName;
        private final int doorSide;
        private final int doorY;

        Role(String serializedName) {
            this(serializedName, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        Role(String serializedName, int doorSide, int doorY) {
            this.serializedName = serializedName;
            this.doorSide = doorSide;
            this.doorY = doorY;
        }

        public boolean isDoor() {
            return doorSide != Integer.MIN_VALUE;
        }

        public int doorSide() {
            return doorSide;
        }

        public int doorY() {
            return doorY;
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
                .strength(-1.0F, 3600000.0F)
                .noOcclusion()
                .noLootTable();
        if (kind == Kind.RESERVATION) properties = properties.noCollission();
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
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
            BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        if (kind == Kind.RESERVATION) return Shapes.empty();
        Role role = state.getValue(ROLE);
        if (kind == Kind.DOOR) {
            return role.isDoor() && doorClosed(level, pos, state, role)
                    ? frontPlane(state.getValue(FACING)) : Shapes.empty();
        }
        return switch (role) {
            case BODY -> Shapes.block();
            case BODY_FRONT -> bodyFront(state.getValue(FACING));
            case FLOOR -> Shapes.block();
            case CABIN_SIDE -> sidePlane(state.getValue(FACING));
            case CABIN_BACK -> frontPlane(state.getValue(FACING));
            case CABIN_ROOF -> ROOF;
            default -> Shapes.empty();
        };
    }

    private static VoxelShape sidePlane(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? SIDE_X : SIDE_Z;
    }

    private static VoxelShape frontPlane(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? SIDE_Z : SIDE_X;
    }

    private static VoxelShape bodyFront(Direction facing) {
        return switch (facing) {
            case NORTH -> Block.box(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);
            case SOUTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
            case EAST -> Block.box(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);
            case WEST -> Block.box(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            default -> Shapes.block();
        };
    }

    private static boolean doorClosed(BlockGetter level, BlockPos partPos,
            BlockState state, Role role) {
        Direction facing = state.getValue(FACING);
        Direction localPositiveX = facing.getCounterClockWise();
        BlockPos controllerPos = partPos
                .relative(localPositiveX, -role.doorSide())
                .below(role.doorY());
        if (!(level.getBlockEntity(controllerPos)
                instanceof Scp914BlockEntity machine)) {
            return false;
        }
        if (Scp914Structure.facing(machine.getBlockState()) != facing) {
            return false;
        }
        long elapsed = machine.refiningElapsedTicks();
        return machine.isRefining()
                && elapsed >= Scp914BlockEntity.DOOR_CLOSE_SOUND_TICK
                && elapsed < Scp914BlockEntity.PROCESS_AND_OPEN_TICK;
    }
}
