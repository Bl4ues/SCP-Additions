from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')
p=root/'TransformPowerQuery.java'
s=p.read_text(encoding='utf-8')
a='''    public static boolean powered(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot consumer) {'''
b='''    /**
     * A wall-mounted button can sit one local cell to the side AND one cell
     * behind the doorway controller. Vanilla's supporting wall would conduct
     * that signal, whereas the Off-Grid grid has no vanilla neighbor events.
     * Restrict this extra diagonal path to real door controls and to the
     * physical footprint of the doorway, not arbitrary room-wide sources.
     */
    public static boolean doorPanelPowered(ServerLevel level,
            TransformGroup group, TransformGroup.GridPos door,
            net.minecraft.core.Direction facing, boolean heavy) {
        if (level == null || group == null || door == null
                || facing == null || !facing.getAxis().isHorizontal()) {
            return false;
        }
        if (!index(level.getServer()).hasSourceNear(
                level.dimension().location(), group.cellCenter(door), 3.25D)) {
            return false;
        }
        net.minecraft.core.Direction side = facing.getClockWise();
        int maxHeight = heavy ? 2 : 0;
        for (int height = 0; height <= maxHeight; height++) {
            for (int lateral : new int[]{-1, 1}) {
                for (int depth : new int[]{-1, 1}) {
                    TransformGroup.GridPos panel = door.offset(
                            side.getStepX() * lateral
                                    + facing.getStepX() * depth,
                            height,
                            side.getStepZ() * lateral
                                    + facing.getStepZ() * depth);
                    if (panelSourceAtVisualCell(group, panel)) return true;
                }
            }
        }
        return false;
    }

    private static boolean panelSourceAtVisualCell(TransformGroup group,
            TransformGroup.GridPos visualCell) {
        TransformGroup.GridPos[] candidates = {
                visualCell,
                visualCell.offset(1, 0, 0),
                visualCell.offset(-1, 0, 0),
                visualCell.offset(0, 0, 1),
                visualCell.offset(0, 0, -1)
        };
        for (TransformGroup.GridPos anchor : candidates) {
            BlockState state = group.cells().get(anchor);
            if (!source(state) || !(
                    TransformWallFixturePlacement.isDoorButton(state)
                    || KeycardReaderLevels.describe(state) != null
                    || state.getBlock()
                            instanceof net.minecraft.world.level.block.ButtonBlock
                    || state.getBlock()
                            instanceof net.minecraft.world.level.block.LeverBlock)) {
                continue;
            }
            if (visualCell.equals(
                    TransformWallFixturePlacement.visualCell(anchor, state))) {
                return true;
            }
        }
        return false;
    }

    public static boolean powered(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot consumer) {'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s,encoding='utf-8')

p=root/'TransformDoorRuntime.java'
s=p.read_text(encoding='utf-8')
a='''            if (address.stage() == DoorStage.CLOSED && powered) {'''
b='''            if (!powered
                    && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                powered = TransformPowerQuery.doorPanelPowered(level, group,
                        ref.cell(), state.getValue(HorizontalDirectionalBlock.FACING),
                        HeavyDoorPowerRelay.isHeavyDoorState(state.getBlock()));
            }
            if (address.stage() == DoorStage.CLOSED && powered) {'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s,encoding='utf-8')
