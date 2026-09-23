from pathlib import Path
path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/ConstructionSurface.java')
s = path.read_text()
def once(old, new):
    global s
    if s.count(old) != 1:
        raise RuntimeError(f'Patch site not unique ({s.count(old)}): {old[:80]}')
    s = s.replace(old, new, 1)

once('''import net.minecraft.nbt.CompoundTag;
''','''import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
''')
once('''import net.minecraft.world.level.block.Blocks;
''','''import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
''')
once('''import net.minecraft.world.phys.Vec3;
''','''import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
''')

old = '''        Map<SurfaceSlot, SurfaceAttachment> remapped = new LinkedHashMap<>();
        Map<SurfaceOverlaySlot, SurfaceAttachment> remappedOverlays =
                new LinkedHashMap<>();
        for (int column = 0; column < newColumns; column++) {
            int oldColumn = Math.min(oldColumns - 1,
                    (int) Math.floor((column + 0.5D) * oldColumns / newColumns));
            for (int row = 0; row < newRows; row++) {
                int oldRow = Math.min(oldRows - 1,
                        (int) Math.floor((row + 0.5D) * oldRows / newRows));
                SurfaceSlot original = new SurfaceSlot(oldColumn, oldRow);
                SurfaceSlot target = new SurfaceSlot(column, row);
                SurfaceAttachment attachment = attachments.get(original);
                if (attachment != null) remapped.put(target, attachment);
                for (int sign : new int[] {-1, 1}) {
                    SurfaceAttachment overlay = overlays.get(
                            new SurfaceOverlaySlot(original, sign));
                    if (overlay != null) remappedOverlays.put(
                            new SurfaceOverlaySlot(target, sign), overlay);
                }
            }
        }
'''
new = '''        Map<SurfaceSlot, SurfaceAttachment> remapped = new LinkedHashMap<>();
        Map<SurfaceOverlaySlot, SurfaceAttachment> remappedOverlays =
                new LinkedHashMap<>();
        // Only continuous, deformable full-cube construction may spread across
        // newly created cells. A moved keycard reader, pipe, fixture or overlay
        // must not silently turn into several duplicate functional blocks.
        Map<BlockState, Boolean> fullCubeCache = new LinkedHashMap<>();
        java.util.Set<SurfaceSlot> structural = new java.util.HashSet<>();
        for (Map.Entry<SurfaceSlot, SurfaceAttachment> entry
                : attachments.entrySet()) {
            SurfaceAttachment attachment = entry.getValue();
            if (attachment.deform() && fullCubeCache.computeIfAbsent(
                    attachment.state(), ConstructionSurface::fullCubeShape)) {
                structural.add(entry.getKey());
            }
        }
        for (int column = 0; column < newColumns; column++) {
            int oldColumn = Math.min(oldColumns - 1,
                    (int) Math.floor((column + 0.5D) * oldColumns / newColumns));
            for (int row = 0; row < newRows; row++) {
                int oldRow = Math.min(oldRows - 1,
                        (int) Math.floor((row + 0.5D) * oldRows / newRows));
                SurfaceSlot original = new SurfaceSlot(oldColumn, oldRow);
                if (structural.contains(original)) {
                    remapped.put(new SurfaceSlot(column, row),
                            attachments.get(original));
                }
            }
        }
        // Keep non-structural payloads and overlays as discrete, single
        // instances, using their prior center-to-center placement semantics.
        for (Map.Entry<SurfaceSlot, SurfaceAttachment> entry
                : attachments.entrySet()) {
            if (!structural.contains(entry.getKey())) {
                remapped.put(remapSlot(entry.getKey(), oldColumns, oldRows,
                        newColumns, newRows), entry.getValue());
            }
        }
        for (Map.Entry<SurfaceOverlaySlot, SurfaceAttachment> entry
                : overlays.entrySet()) {
            SurfaceSlot target = remapSlot(entry.getKey().slot(), oldColumns,
                    oldRows, newColumns, newRows);
            remappedOverlays.putIfAbsent(new SurfaceOverlaySlot(target,
                    entry.getKey().normalSign()), entry.getValue());
        }
'''
once(old, new)
anchor = '''    public ConstructionSurface withAttachment(SurfaceSlot slot,
'''
extra = '''    private static boolean fullCubeShape(BlockState state) {
        if (state == null || state.isAir()
                || !TransformSurfaceGeometry.canDeform(state)) return false;
        List<AABB> boxes = state.getCollisionShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                CollisionContext.empty()).toAabbs();
        if (boxes.size() != 1) return false;
        AABB box = boxes.get(0);
        return box.minX >= -1.0E-6D && box.minY >= -1.0E-6D
                && box.minZ >= -1.0E-6D && box.maxX <= 1.000001D
                && box.maxY <= 1.000001D && box.maxZ <= 1.000001D
                && box.minX < 1.0E-6D && box.minY < 1.0E-6D
                && box.minZ < 1.0E-6D && box.maxX > 0.999999D
                && box.maxY > 0.999999D && box.maxZ > 0.999999D;
    }

    private static SurfaceSlot remapSlot(SurfaceSlot slot, int oldColumns,
            int oldRows, int newColumns, int newRows) {
        int column = Math.min(newColumns - 1,
                (int) Math.floor((slot.column() + 0.5D)
                        * newColumns / oldColumns));
        int row = Math.min(newRows - 1,
                (int) Math.floor((slot.row() + 0.5D)
                        * newRows / oldRows));
        return new SurfaceSlot(column, row);
    }

'''
once(anchor, extra + anchor)
path.write_text(s)
# Exercise the two distinct mapping policies that the Java implementation uses.
for old_cols, old_rows, new_cols, new_rows in [(7, 6, 9, 12), (9, 12, 7, 6)]:
    full = {(x, y) for x in range(old_cols) for y in range(old_rows)}
    expanded = {(x, y) for x in range(new_cols) for y in range(new_rows)
                if (min(old_cols-1, int((x+.5)*old_cols/new_cols)),
                    min(old_rows-1, int((y+.5)*old_rows/new_rows))) in full}
    assert len(expanded) == new_cols * new_rows
    distinct = {(old_cols//2, old_rows//2)}
    moved = {(min(new_cols-1,int((x+.5)*new_cols/old_cols)),
              min(new_rows-1,int((y+.5)*new_rows/old_rows))) for x,y in distinct}
    assert len(moved) == 1
print('Checked complete structural coverage and single-instance fixture mapping')
