from pathlib import Path
import re

terminal = Path('src/main/java/net/mcreator/scpadditions/block/SCP079SystemControlBlock.java')
text = terminal.read_text()
text = re.sub(r'// BEGIN GENERATED MODEL COLLISION.*?// END GENERATED MODEL COLLISION \(system terminal\)', '''// BEGIN GENERATED MODEL COLLISION
    private static final VoxelShape MODEL_SHAPE_NORTH = Shapes.or(
            Block.box(7, 0, 5, 20, 16, 15),
            Block.box(7, 0, -2, 20, 4, 6),
            Block.box(-4, 0, 3, 7, 8, 15));
    private static final VoxelShape MODEL_SHAPE_EAST = Shapes.or(
            Block.box(1, 0, 7, 11, 16, 20),
            Block.box(10, 0, 7, 18, 4, 20),
            Block.box(1, 0, -4, 13, 8, 7));
    private static final VoxelShape MODEL_SHAPE_SOUTH = Shapes.or(
            Block.box(-4, 0, 1, 9, 16, 11),
            Block.box(-4, 0, 10, 9, 4, 18),
            Block.box(9, 0, 1, 20, 8, 13));
    private static final VoxelShape MODEL_SHAPE_WEST = Shapes.or(
            Block.box(5, 0, -4, 15, 16, 9),
            Block.box(-2, 0, -4, 6, 4, 9),
            Block.box(3, 0, 9, 15, 8, 20));

    private static VoxelShape modelShape(Direction facing) {
        return switch (facing) {
            case EAST -> MODEL_SHAPE_EAST;
            case SOUTH -> MODEL_SHAPE_SOUTH;
            case WEST -> MODEL_SHAPE_WEST;
            default -> MODEL_SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return modelShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return modelShape(state.getValue(FACING));
    }
    // END GENERATED MODEL COLLISION (system terminal)''', text, flags=re.S)
text = text.replace('tooltip.add(Component.literal("SCiPNET facility diagnostic terminal."));\n        tooltip.add(Component.literal("Monitors site systems and remote session cache."));', 'tooltip.add(Component.literal("SCiPNET diagnostic terminal."));\n        tooltip.add(Component.literal("Telemetry and remote cache control."));')
terminal.write_text(text)

aux = Path('src/main/java/net/mcreator/scpadditions/block/Scp079AuxiliaryPowerBlock.java')
text = aux.read_text()
text = re.sub(r'// BEGIN GENERATED MODEL COLLISION.*?// END GENERATED MODEL COLLISION \(auxiliary generator\)', '''// BEGIN GENERATED MODEL COLLISION
    private static final VoxelShape MODEL_SHAPE_NORTH = Shapes.or(
            Block.box(-4, 0, 4, 20, 22, 30),
            Block.box(0, 0, -3, 16, 7, 5),
            Block.box(0, 22, 14, 4, 26, 21));
    private static final VoxelShape MODEL_SHAPE_EAST = Shapes.or(
            Block.box(-14, 0, -4, 12, 22, 20),
            Block.box(11, 0, 0, 19, 7, 16),
            Block.box(-5, 22, 0, 2, 26, 4));
    private static final VoxelShape MODEL_SHAPE_SOUTH = Shapes.or(
            Block.box(-4, 0, -14, 20, 22, 12),
            Block.box(0, 0, 11, 16, 7, 19),
            Block.box(12, 22, -5, 16, 26, 2));
    private static final VoxelShape MODEL_SHAPE_WEST = Shapes.or(
            Block.box(4, 0, -4, 30, 22, 20),
            Block.box(-3, 0, 0, 5, 7, 16),
            Block.box(14, 22, 12, 21, 26, 16));

    private static VoxelShape modelShape(Direction facing) {
        return switch (facing) {
            case EAST -> MODEL_SHAPE_EAST;
            case SOUTH -> MODEL_SHAPE_SOUTH;
            case WEST -> MODEL_SHAPE_WEST;
            default -> MODEL_SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return modelShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return modelShape(state.getValue(FACING));
    }
    // END GENERATED MODEL COLLISION (auxiliary generator)''', text, flags=re.S)
text = re.sub(r'tooltip\.add\(Component\.literal\("Redstone-powered.*?super\.appendHoverText', 'tooltip.add(Component.literal("Redstone-powered auxiliary generator."));\n        tooltip.add(Component.literal("Adds 0.1 AP/s; output stacks."));\n        super.appendHoverText', text, flags=re.S)
aux.write_text(text)

for path in [Path('src/main/java/net/mcreator/scpadditions/client/SystemTerminalBlockEntityRenderer.java'), Path('src/main/java/net/mcreator/scpadditions/client/SystemTerminalItemRenderer.java')]:
    text = path.read_text().replace('RenderType.entityCutoutNoCull(texture)', 'RenderType.entityTranslucent(texture)')
    path.write_text(text)

# trigger
