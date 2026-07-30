from pathlib import Path

root = Path(__file__).resolve().parents[2]
java = root / "src/main/java/net/mcreator/scpadditions/facility/elevator"


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f"Could not locate {label}")
    return text.replace(old, new, 1)


path = java / "CoreRoomElevatorModule.java"
text = path.read_text(encoding="utf-8")

old = '''        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (!(level instanceof ServerLevel serverLevel)) return;
            Player player = placer instanceof Player found ? found : null;
            Direction facing = state.getValue(FACING);
            if (!CoreRoomElevatorManager.isValidStationPlacement(
                    serverLevel, pos, facing)) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable(
                            "message.scp_additions.elevator_floor_spacing",
                            MIN_FLOOR_SPACING, MAX_FLOOR_SPACING)
                            .withStyle(ChatFormatting.RED));
                }
                serverLevel.destroyBlock(pos, true, placer);
                return;
            }
            if (!placeParts(serverLevel, pos, facing, StructureKind.STATION,
                    stationCells(), player)) {
                serverLevel.destroyBlock(pos, true, placer);
                return;
            }
            CoreRoomElevatorManager.rebuildColumn(serverLevel, pos, player);
        }
'''
new = '''        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (!(level instanceof ServerLevel serverLevel)) return;
            Player player = placer instanceof Player found ? found : null;
            Direction facing = state.getValue(FACING);

            BlockPos snapped = CoreRoomElevatorManager.findStationSnap(
                    serverLevel, pos, facing);
            if (!snapped.equals(pos)) {
                BlockState occupied = serverLevel.getBlockState(snapped);
                if (occupied.is(BEAMS.get()) && occupied.getValue(GENERATED)) {
                    serverLevel.removeBlock(snapped, false);
                } else if (!occupied.canBeReplaced()) {
                    if (player != null) {
                        player.sendSystemMessage(Component.translatable(
                                "message.scp_additions.elevator_space_blocked")
                                .withStyle(ChatFormatting.RED));
                    }
                    serverLevel.destroyBlock(pos, true, placer);
                    return;
                }
                serverLevel.removeBlock(pos, false);
                serverLevel.setBlock(snapped, state, Block.UPDATE_ALL);
                pos = snapped;
            }

            if (!CoreRoomElevatorManager.isValidStationPlacement(
                    serverLevel, pos, facing)) {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable(
                            "message.scp_additions.elevator_floor_spacing",
                            MIN_FLOOR_SPACING, MAX_FLOOR_SPACING)
                            .withStyle(ChatFormatting.RED));
                }
                serverLevel.destroyBlock(pos, true, placer);
                return;
            }
            if (!placeParts(serverLevel, pos, facing, StructureKind.STATION,
                    stationCells(), player)) {
                serverLevel.destroyBlock(pos, true, placer);
                return;
            }
            CoreRoomElevatorManager.rebuildColumn(serverLevel, pos, player);
        }
'''
text = replace_once(text, old, new, "station placement")

old = '''        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            boolean gateSolid = !(level.getBlockEntity(pos)
                    instanceof StationBlockEntity station)
                    || station.isGateCollisionSolid();
            return CoreRoomElevatorGeometry.stationCellShape(
                    state.getValue(FACING), 0, 0, 0, gateSolid);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }
'''
new = '''        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return CoreRoomElevatorGeometry.stationSelectionCellShape();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            boolean gateSolid = !(level.getBlockEntity(pos)
                    instanceof StationBlockEntity station)
                    || station.isGateCollisionSolid();
            return CoreRoomElevatorGeometry.stationCellShape(
                    state.getValue(FACING), 0, 0, 0, gateSolid);
        }
'''
text = replace_once(text, old, new, "station shape split")

old = '''        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return CoreRoomElevatorGeometry.pulleyCellShape(
                    state.getValue(FACING), 0, 0, 0);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }
'''
new = '''        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return CoreRoomElevatorGeometry.pulleySelectionCellShape();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return CoreRoomElevatorGeometry.pulleyCellShape(
                    state.getValue(FACING), 0, 0, 0);
        }
'''
text = replace_once(text, old, new, "pulley shape split")

old = '''    public static final class CoreRoomFloorBlock extends Block {
        private CoreRoomFloorBlock() {
            super(BlockBehaviour.Properties.of().strength(4.0F, 12.0F)
                    .sound(SoundType.METAL));
        }
    }
'''
new = '''    public static final class CoreRoomFloorBlock
            extends HorizontalDirectionalBlock {
        private static final VoxelShape FLOOR_SHAPE = Block.box(
                0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);

        private CoreRoomFloorBlock() {
            super(BlockBehaviour.Properties.of().strength(4.0F, 12.0F)
                    .sound(SoundType.METAL).noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING,
                    Direction.NORTH));
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return FLOOR_SHAPE;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return FLOOR_SHAPE;
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
                BlockPos pos) {
            return Shapes.empty();
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
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
'''
text = replace_once(text, old, new, "Core Room floor block")

old = '''        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            if (!(level.getBlockEntity(pos) instanceof StructurePartBlockEntity part)) {
                return Shapes.empty();
            }
            BlockState masterState = level.getBlockState(part.masterPos());
            if (!masterState.hasProperty(FACING)) return Shapes.empty();
            Direction facing = masterState.getValue(FACING);
            if (part.kind() == StructureKind.PULLEY) {
                return CoreRoomElevatorGeometry.pulleyCellShape(facing,
                        part.localX(), part.localY(), part.localZ());
            }
            if (part.kind() == StructureKind.BEAMS) {
                return CoreRoomElevatorGeometry.beamCellShape(facing,
                        part.localX(), part.localY(), part.localZ());
            }
            boolean gateSolid = !(level.getBlockEntity(part.masterPos())
                    instanceof StationBlockEntity station)
                    || station.isGateCollisionSolid();
            return CoreRoomElevatorGeometry.stationCellShape(facing,
                    part.localX(), part.localY(), part.localZ(), gateSolid);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }
'''
new = '''        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.block();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            if (!(level.getBlockEntity(pos) instanceof StructurePartBlockEntity part)) {
                return Shapes.empty();
            }
            BlockState masterState = level.getBlockState(part.masterPos());
            if (!masterState.hasProperty(FACING)) return Shapes.empty();
            Direction facing = masterState.getValue(FACING);
            if (part.kind() == StructureKind.PULLEY) {
                return CoreRoomElevatorGeometry.pulleyCellShape(facing,
                        part.localX(), part.localY(), part.localZ());
            }
            if (part.kind() == StructureKind.BEAMS) {
                return CoreRoomElevatorGeometry.beamCellShape(facing,
                        part.localX(), part.localY(), part.localZ());
            }
            boolean gateSolid = !(level.getBlockEntity(part.masterPos())
                    instanceof StationBlockEntity station)
                    || station.isGateCollisionSolid();
            return CoreRoomElevatorGeometry.stationCellShape(facing,
                    part.localX(), part.localY(), part.localZ(), gateSolid);
        }
'''
text = replace_once(text, old, new, "multiblock part shape split")
path.write_text(text, encoding="utf-8")

manager_path = java / "CoreRoomElevatorManager.java"
manager = manager_path.read_text(encoding="utf-8")
marker = '''    public static boolean isValidStationPlacement(Level level, BlockPos pos,
            Direction facing) {
'''
method = '''    public static BlockPos findStationSnap(Level level, BlockPos desired,
            Direction facing) {
        BlockPos best = desired;
        int bestDistance = Integer.MAX_VALUE;
        boolean ambiguous = false;
        Set<BlockPos> columns = new HashSet<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                int columnX = desired.getX() + dx;
                int columnZ = desired.getZ() + dz;
                for (int y = level.getMinBuildHeight();
                        y < level.getMaxBuildHeight(); y++) {
                    BlockPos candidate = new BlockPos(columnX, y, columnZ);
                    BlockState state = level.getBlockState(candidate);
                    if (!state.is(CoreRoomElevatorModule.STATION.get())
                            || state.getValue(CoreRoomElevatorModule.FACING)
                            != facing) continue;
                    int spacing = Math.abs(y - desired.getY());
                    if (spacing < CoreRoomElevatorModule.MIN_FLOOR_SPACING
                            || spacing > CoreRoomElevatorModule.MAX_FLOOR_SPACING) {
                        continue;
                    }
                    BlockPos column = new BlockPos(columnX, desired.getY(),
                            columnZ);
                    if (!columns.add(column)) continue;
                    int distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        best = column;
                        bestDistance = distance;
                        ambiguous = false;
                    } else if (distance == bestDistance && !best.equals(column)) {
                        ambiguous = true;
                    }
                }
            }
        }
        return ambiguous ? desired : best;
    }

'''
manager = replace_once(manager, marker, method + marker, "station snap method")
manager_path.write_text(manager, encoding="utf-8")
