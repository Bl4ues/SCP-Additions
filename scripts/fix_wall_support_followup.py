from pathlib import Path

path = Path("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java")
text = path.read_text(encoding="utf-8")

trash_wrong = '''    private static final class TrashbinBlock extends HorizontalDirectionalBlock {
        private TrashbinBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            return clickedFace.getAxis() == Direction.Axis.Y ? null
                    : defaultBlockState().setValue(FACING, clickedFace);
        }
'''
trash_correct = '''    private static final class TrashbinBlock extends HorizontalDirectionalBlock {
        private TrashbinBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }
'''
if trash_wrong not in text:
    raise RuntimeError("Unexpected TrashbinBlock source")
text = text.replace(trash_wrong, trash_correct, 1)

button_old = '''    private static final class DoorButtonBlock extends HorizontalDirectionalBlock {
        private final ButtonState buttonState;

        private DoorButtonBlock(ButtonState buttonState) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.0F, 10.0F)
                    .noCollission().noOcclusion().isRedstoneConductor((state, level, pos) -> false));
            this.buttonState = buttonState;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }
'''
button_new = '''    private static final class DoorButtonBlock extends HorizontalDirectionalBlock {
        private final ButtonState buttonState;

        private DoorButtonBlock(ButtonState buttonState) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.0F, 10.0F)
                    .noCollission().noOcclusion().isRedstoneConductor((state, level, pos) -> false));
            this.buttonState = buttonState;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            return clickedFace.getAxis() == Direction.Axis.Y ? null
                    : defaultBlockState().setValue(FACING, clickedFace);
        }
'''
if button_old not in text:
    raise RuntimeError("Unexpected DoorButtonBlock source")
text = text.replace(button_old, button_new, 1)
path.write_text(text, encoding="utf-8")
