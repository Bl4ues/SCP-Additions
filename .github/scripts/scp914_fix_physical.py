from pathlib import Path

# Use the already registered SCP-914 sound events. The rebuilt module had
# invented ids which compile but have no sounds.json entries.
path = Path('src/main/java/com/bl4ues/scpclassifieddirective/scp914/Scp914Module.java')
text = path.read_text()
replacements = {
    'public static final RegistryObject<SoundEvent> WIND = sound("scp914wind");':
        'public static final RegistryObject<SoundEvent> WIND = ScpClassifiedDirectiveModSounds.SCP914KEY;',
    'public static final RegistryObject<SoundEvent> CLOSE = sound("scp914close");':
        'public static final RegistryObject<SoundEvent> CLOSE = ScpClassifiedDirectiveModSounds.SCP914DOORCLOSE;',
    'public static final RegistryObject<SoundEvent> OPEN = sound("scp914open");':
        'public static final RegistryObject<SoundEvent> OPEN = ScpClassifiedDirectiveModSounds.SCP914DOOROPEN;',
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f'Missing SCP-914 sound marker: {old}')
    text = text.replace(old, new, 1)
for index in range(1, 10):
    old = f'public static final RegistryObject<SoundEvent> GEAR_{index} = sound("scp914gear_{index}");'
    new = f'public static final RegistryObject<SoundEvent> GEAR_{index} = ScpClassifiedDirectiveModSounds.SCP914DIAL;'
    if old not in text:
        raise SystemExit(f'Missing SCP-914 gear marker {index}')
    text = text.replace(old, new, 1)
path.write_text(text)

# Physical dial dragging responds to the normal use button as well as the
# configurable contextual-interaction key.
path = Path('src/main/java/com/bl4ues/scpclassifieddirective/client/Scp914InteractionClient.java')
text = path.read_text()
old = '        boolean interactDown = Keybinds.CONTEXT_INTERACT.isDown();\n'
new = ('        boolean interactDown = Keybinds.CONTEXT_INTERACT.isDown()\n'
       '                || minecraft.options.keyUse.isDown();\n')
if old not in text:
    raise SystemExit('SCP-914 drag input marker missing')
path.write_text(text.replace(old, new, 1))

# The generic prompt renderer already implements hand-left/action-right. Only
# the dial needs the special centered hand with no text.
path = Path('src/main/java/com/bl4ues/scpclassifieddirective/inventory/client/ContextPromptClient.java')
text = path.read_text()
old = '                    boolean showAction = rule.showAction() && showName;'
new = ('                    boolean showAction = rule.showAction()\n'
       '                            && rule.action() != null\n'
       '                            && !rule.action().isBlank();')
count = text.count(old)
if count != 2:
    raise SystemExit(f'Expected 2 contextual action markers, found {count}')
text = text.replace(old, new)
start = text.index('    private static boolean renderScp914ControlPrompt(')
end = text.index('    private static void drawIcon(', start)
method = '''    private static boolean renderScp914ControlPrompt(
            GuiGraphics graphics, Minecraft minecraft, ContextTarget target,
            ScreenPoint point, int screenWidth, int screenHeight) {
        if (!"scp_914_dial".equals(target.interactionKey())) return false;

        float promptScale = target.promptScale();
        int iconSize = Math.max(24,
                Math.round(BASE_ICON_SIZE * promptScale));
        int screenX = Mth.clamp(point.x(), iconSize / 2 + 6,
                screenWidth - iconSize / 2 - 6);
        int screenY = Mth.clamp(point.y(), iconSize / 2 + 6,
                screenHeight - iconSize / 2 - 6);
        int iconX = screenX - iconSize / 2;
        int iconY = screenY - iconSize / 2;
        drawIcon(graphics, target.icon(), iconX, iconY, iconSize);
        return true;
    }

'''
text = text[:start] + method + text[end:]
path.write_text(text)

Path('src/main/java/com/bl4ues/scpclassifieddirective/block/Scp914PartBlock.java').write_text(r'''package com.bl4ues.scpclassifieddirective.block;

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
''')

Path('src/main/java/com/bl4ues/scpclassifieddirective/scp914/Scp914Structure.java').write_text(r'''package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.block.Scp914Block;
import com.bl4ues.scpclassifieddirective.block.Scp914PartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Physical coordinates and semantic multiblock footprint for rebuilt SCP-914. */
public final class Scp914Structure {
    private Scp914Structure() {
    }

    private static final int FORWARD_MIN = -3;
    private static final int FORWARD_MAX = 2;
    private static final int SIDE_MIN = -8;
    private static final int SIDE_MAX = 7;
    private static final int Y_MIN = 0;
    private static final int Y_MAX = 2;

    private static final double INTAKE_X = -4.80D;
    private static final double OUTPUT_X = 4.80D;
    private static final double CHAMBER_Y = 1.05D;
    private static final double CHAMBER_Z = 1.50D;
    private static final double CHAMBER_HALF_X = 0.92D;
    private static final double CHAMBER_MIN_Y = 0.12D;
    private static final double CHAMBER_MAX_Y = 2.18D;
    private static final double CHAMBER_MIN_Z = 0.66D;
    private static final double CHAMBER_MAX_Z = 2.34D;

    public static final double DIAL_X = 0.0D;
    public static final double DIAL_Y = 20.04D / 16.0D;
    public static final double DIAL_Z = -8.25D / 16.0D;
    public static final double WIND_KEY_X = 0.0D;
    public static final double WIND_KEY_Y = 14.5D / 16.0D;
    public static final double WIND_KEY_Z = -9.075D / 16.0D;

    public static Direction facing(BlockState state) {
        return state.hasProperty(Scp914Block.FACING)
                ? state.getValue(Scp914Block.FACING) : Direction.NORTH;
    }

    public static Vec3 localToWorld(BlockPos origin, Direction front,
            double localX, double localY, double localZ) {
        Direction rightFromViewer = front.getCounterClockWise();
        Direction back = front.getOpposite();
        return new Vec3(
                origin.getX() + 0.5D
                        + rightFromViewer.getStepX() * localX
                        + back.getStepX() * localZ,
                origin.getY() + localY,
                origin.getZ() + 0.5D
                        + rightFromViewer.getStepZ() * localX
                        + back.getStepZ() * localZ);
    }

    public static Vec3 dialAnchor(BlockPos origin, Direction front) {
        return localToWorld(origin, front, DIAL_X, DIAL_Y, DIAL_Z);
    }

    public static Vec3 windKeyAnchor(BlockPos origin, Direction front) {
        return localToWorld(origin, front, WIND_KEY_X, WIND_KEY_Y, WIND_KEY_Z);
    }

    public static Vec3 machineSoundCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, 0.0D, 1.25D, 1.05D);
    }

    public static Vec3 intakeCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, INTAKE_X, CHAMBER_Y, CHAMBER_Z);
    }

    public static Vec3 outputCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, OUTPUT_X, CHAMBER_Y, CHAMBER_Z);
    }

    public static Vec3 intakeDoorCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, INTAKE_X, 1.20D, 0.42D);
    }

    public static Vec3 outputDoorCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, OUTPUT_X, 1.20D, 0.42D);
    }

    public static AABB intakeArea(BlockPos origin, Direction front) {
        return chamberArea(origin, front, INTAKE_X);
    }

    public static AABB outputArea(BlockPos origin, Direction front) {
        return chamberArea(origin, front, OUTPUT_X);
    }

    private static AABB chamberArea(BlockPos origin, Direction front,
            double centerX) {
        Vec3 a = localToWorld(origin, front,
                centerX - CHAMBER_HALF_X, CHAMBER_MIN_Y, CHAMBER_MIN_Z);
        Vec3 b = localToWorld(origin, front,
                centerX + CHAMBER_HALF_X, CHAMBER_MAX_Y, CHAMBER_MAX_Z);
        return new AABB(
                Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
                Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    /** Includes reservation volume plus the six floor cells below each cabin. */
    public static List<BlockPos> requiredCells(BlockPos origin, Direction front) {
        List<BlockPos> result = new ArrayList<>();
        result.add(origin.immutable());
        for (PartPlacement placement : placements(origin, front)) {
            result.add(placement.pos());
        }
        return result;
    }

    public static List<BlockPos> collectObstructions(Level level, BlockPos origin,
            Direction front) {
        List<BlockPos> blocked = new ArrayList<>();
        for (PartPlacement placement : placements(origin, front)) {
            if (!level.getBlockState(placement.pos()).canBeReplaced()) {
                blocked.add(placement.pos().immutable());
            }
        }
        return blocked;
    }

    /** All helpers are preverified before any cell is written. */
    public static void placeHelpers(Level level, BlockPos origin, Direction front) {
        if (level.isClientSide) return;
        List<PartPlacement> expected = placements(origin, front);
        for (PartPlacement placement : expected) {
            if (!level.getBlockState(placement.pos()).canBeReplaced()) return;
        }

        List<BlockPos> placed = new ArrayList<>();
        for (PartPlacement placement : expected) {
            BlockState helper = stateFor(placement, front);
            if (!level.setBlock(placement.pos(), helper, 3)) {
                for (BlockPos pos : placed) {
                    BlockState state = level.getBlockState(pos);
                    if (isHelper(state)) level.setBlock(pos,
                            Blocks.AIR.defaultBlockState(), 3);
                }
                return;
            }
            placed.add(placement.pos());
        }
    }

    public static void clearHelpers(Level level, BlockPos origin, Direction front) {
        if (level.isClientSide) return;
        for (PartPlacement placement : placements(origin, front)) {
            BlockState state = level.getBlockState(placement.pos());
            if (isHelper(state)) {
                level.setBlock(placement.pos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static boolean isDoorCell(BlockPos origin, Direction front,
            BlockPos candidate) {
        for (PartPlacement placement : placements(origin, front)) {
            if (placement.kind() == Scp914PartBlock.Kind.DOOR
                    && placement.pos().equals(candidate)) return true;
        }
        return false;
    }

    private static boolean isHelper(BlockState state) {
        return state.is(Scp914Module.SCP_914_RESERVATION.get())
                || state.is(Scp914Module.SCP_914_COLLISION.get())
                || state.is(Scp914Module.SCP_914_DOOR_COLLISION.get());
    }

    private static BlockState stateFor(PartPlacement placement, Direction front) {
        BlockState state = switch (placement.kind()) {
            case RESERVATION -> Scp914Module.SCP_914_RESERVATION.get().defaultBlockState();
            case SOLID -> Scp914Module.SCP_914_COLLISION.get().defaultBlockState();
            case DOOR -> Scp914Module.SCP_914_DOOR_COLLISION.get().defaultBlockState();
        };
        return state.setValue(Scp914PartBlock.FACING, front)
                .setValue(Scp914PartBlock.ROLE, placement.role());
    }

    private static List<PartPlacement> placements(BlockPos origin, Direction front) {
        Map<BlockPos, PartPlacement> result = new LinkedHashMap<>();

        for (int forward = FORWARD_MIN; forward <= FORWARD_MAX; forward++) {
            for (int side = SIDE_MIN; side <= SIDE_MAX; side++) {
                for (int y = Y_MIN; y <= Y_MAX; y++) {
                    BlockPos pos = gridCell(origin, front, side, y, forward);
                    if (!pos.equals(origin)) put(result, pos,
                            Scp914PartBlock.Kind.RESERVATION,
                            Scp914PartBlock.Role.RESERVED);
                }
            }
        }

        for (int side = -2; side <= 2; side++) {
            for (int forward = -2; forward <= -1; forward++) {
                for (int y = 0; y <= 2; y++) {
                    put(result, gridCell(origin, front, side, y, forward),
                            Scp914PartBlock.Kind.SOLID,
                            Scp914PartBlock.Role.BODY);
                }
            }
            for (int y = 0; y <= 2; y++) {
                BlockPos pos = gridCell(origin, front, side, y, 0);
                if (!pos.equals(origin)) put(result, pos,
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.BODY_FRONT);
            }
        }

        addCabin(result, origin, front, -5);
        addCabin(result, origin, front, 5);
        return List.copyOf(result.values());
    }

    private static void addCabin(Map<BlockPos, PartPlacement> result,
            BlockPos origin, Direction front, int centerSide) {
        for (int side = centerSide - 1; side <= centerSide + 1; side++) {
            for (int forward = -2; forward <= -1; forward++) {
                put(result, gridCell(origin, front, side, -1, forward),
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.FLOOR);
            }
        }

        for (int wallSide : new int[] {centerSide - 1, centerSide + 1}) {
            for (int forward = -2; forward <= -1; forward++) {
                for (int y = 0; y <= 1; y++) {
                    put(result, gridCell(origin, front, wallSide, y, forward),
                            Scp914PartBlock.Kind.SOLID,
                            Scp914PartBlock.Role.CABIN_SIDE);
                }
            }
        }
        for (int side = centerSide - 1; side <= centerSide + 1; side++) {
            for (int y = 0; y <= 2; y++) {
                put(result, gridCell(origin, front, side, y, -3),
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.CABIN_BACK);
            }
            for (int forward = -2; forward <= -1; forward++) {
                put(result, gridCell(origin, front, side, 2, forward),
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.CABIN_ROOF);
            }
        }

        for (int side = centerSide - 1; side <= centerSide + 1; side++) {
            put(result, gridCell(origin, front, side, 0, 0),
                    Scp914PartBlock.Kind.DOOR, doorRole(side, 0));
            put(result, gridCell(origin, front, side, 1, 0),
                    Scp914PartBlock.Kind.DOOR, doorRole(side, 1));
        }
    }

    private static Scp914PartBlock.Role doorRole(int side, int y) {
        return switch (side) {
            case -6 -> y == 0 ? Scp914PartBlock.Role.DOOR_N6_LOWER
                    : Scp914PartBlock.Role.DOOR_N6_UPPER;
            case -5 -> y == 0 ? Scp914PartBlock.Role.DOOR_N5_LOWER
                    : Scp914PartBlock.Role.DOOR_N5_UPPER;
            case -4 -> y == 0 ? Scp914PartBlock.Role.DOOR_N4_LOWER
                    : Scp914PartBlock.Role.DOOR_N4_UPPER;
            case 4 -> y == 0 ? Scp914PartBlock.Role.DOOR_P4_LOWER
                    : Scp914PartBlock.Role.DOOR_P4_UPPER;
            case 5 -> y == 0 ? Scp914PartBlock.Role.DOOR_P5_LOWER
                    : Scp914PartBlock.Role.DOOR_P5_UPPER;
            case 6 -> y == 0 ? Scp914PartBlock.Role.DOOR_P6_LOWER
                    : Scp914PartBlock.Role.DOOR_P6_UPPER;
            default -> throw new IllegalArgumentException("Invalid SCP-914 door side " + side);
        };
    }

    private static void put(Map<BlockPos, PartPlacement> result, BlockPos pos,
            Scp914PartBlock.Kind kind, Scp914PartBlock.Role role) {
        result.put(pos.immutable(), new PartPlacement(pos.immutable(), kind, role));
    }

    private static BlockPos gridCell(BlockPos origin, Direction front,
            int side, int y, int forward) {
        Direction localPositiveX = front.getCounterClockWise();
        return origin.relative(front, forward)
                .relative(localPositiveX, side)
                .above(y);
    }

    private record PartPlacement(BlockPos pos, Scp914PartBlock.Kind kind,
            Scp914PartBlock.Role role) {
    }
}
''')
