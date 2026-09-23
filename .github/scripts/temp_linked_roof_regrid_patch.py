from pathlib import Path

surface_path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/ConstructionSurface.java')
bridge_path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/TransformSurfaceBridge.java')
surface = surface_path.read_text()
bridge = bridge_path.read_text()

def replace_once(source, old, new):
    found = source.count(old)
    if found != 1:
        raise RuntimeError(f'Expected one patch site, found {found}: {old[:90]!r}')
    return source.replace(old, new, 1)

old = '''        if (attachments.isEmpty() && overlays.isEmpty()) return geometry;

        int oldColumns = columns();
        int oldRows = rows();
        int newColumns = geometry.columns();
        int newRows = geometry.rows();
        Map<SurfaceSlot, SurfaceAttachment> remapped = new LinkedHashMap<>();
        for (Map.Entry<SurfaceSlot, SurfaceAttachment> entry
                : attachments.entrySet()) {
            SurfaceSlot mapped = remapSlot(entry.getKey(), oldColumns, oldRows,
                    newColumns, newRows);
            remapped.putIfAbsent(mapped, entry.getValue());
        }
        Map<SurfaceOverlaySlot, SurfaceAttachment> remappedOverlays =
                new LinkedHashMap<>();
        for (Map.Entry<SurfaceOverlaySlot, SurfaceAttachment> entry
                : overlays.entrySet()) {
            SurfaceSlot mapped = remapSlot(entry.getKey().slot(), oldColumns,
                    oldRows, newColumns, newRows);
            remappedOverlays.putIfAbsent(new SurfaceOverlaySlot(mapped,
                    entry.getKey().normalSign()), entry.getValue());
        }
        return new ConstructionSurface(id, dimension, nextBottomStart,
                nextBottomEnd, nextTopStart, nextTopEnd, nextCurveOffset,
                nextHeightCurveOffset, remapped, remappedOverlays, flipped,
                topCurveOffset, bridge);
    }

    private static SurfaceSlot remapSlot(SurfaceSlot slot, int oldColumns,
            int oldRows, int newColumns, int newRows) {
        double u = (slot.column() + 0.5D) / oldColumns;
        double v = (slot.row() + 0.5D) / oldRows;
        int column = Math.max(0, Math.min(newColumns - 1,
                (int) Math.floor(u * newColumns)));
        int row = Math.max(0, Math.min(newRows - 1,
                (int) Math.floor(v * newRows)));
        return new SurfaceSlot(column, row);
    }
'''
new = '''        return regridTo(geometry);
    }

    /** Preserve physical cell coverage when a reshape changes the logical grid.
     * Mapping each OLD block to a single new slot left entire empty roof rows
     * when a linked ceiling's crown increased its arc length. Instead sample
     * every NEW slot from the corresponding old grid region. Authored empty
     * regions remain empty; a fully covered ceiling stays fully covered. */
    public ConstructionSurface regridTo(ConstructionSurface geometry) {
        if (geometry == null || !id.equals(geometry.id())
                || !dimension.equals(geometry.dimension())) {
            throw new IllegalArgumentException("Cannot regrid unrelated Surfaces");
        }
        if (attachments.isEmpty() && overlays.isEmpty()) return geometry;
        int oldColumns = columns();
        int oldRows = rows();
        int newColumns = geometry.columns();
        int newRows = geometry.rows();
        if (oldColumns == newColumns && oldRows == newRows) {
            return new ConstructionSurface(geometry.id(), geometry.dimension(),
                    geometry.bottomStart(), geometry.bottomEnd(),
                    geometry.topStart(), geometry.topEnd(),
                    geometry.curveOffset(), geometry.heightCurveOffset(),
                    attachments, overlays, geometry.flipped(),
                    geometry.topCurveOffset(), geometry.bridge());
        }
        Map<SurfaceSlot, SurfaceAttachment> remapped = new LinkedHashMap<>();
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
        return new ConstructionSurface(geometry.id(), geometry.dimension(),
                geometry.bottomStart(), geometry.bottomEnd(),
                geometry.topStart(), geometry.topEnd(),
                geometry.curveOffset(), geometry.heightCurveOffset(),
                remapped, remappedOverlays, geometry.flipped(),
                geometry.topCurveOffset(), geometry.bridge());
    }
'''
surface = replace_once(surface, old, new)
old_bridge = '''        return derive(current.id(), current.dimension(), current.bridge(),
                first, second, current.heightCurveOffset(),
                current.attachments(), current.overlays(), current.flipped());'''
new_bridge = '''        // A parent edit can change the linked roof's longitudinal or
        // transverse cell count. Regrid its existing blocks against the new
        // geometry instead of copying their old indices into a larger mesh.
        ConstructionSurface geometry = derive(current.id(),
                current.dimension(), current.bridge(), first, second,
                current.heightCurveOffset(), Map.of(), Map.of(),
                current.flipped());
        return current.regridTo(geometry);'''
bridge = replace_once(bridge, old_bridge, new_bridge)
surface_path.write_text(surface)
bridge_path.write_text(bridge)

# The previous old-slot-to-new-slot mapping could not cover a growing grid.
# Check that inverse center sampling covers both expanded and contracted full
# grids and does not invent blocks within an authored empty longitudinal strip.
for old_width, old_height, new_width, new_height in [(7, 6, 9, 12), (9, 12, 7, 6), (7, 6, 7, 13), (7, 6, 13, 6)]:
    old = {(x, y) for x in range(old_width) for y in range(old_height)}
    new = {(x, y) for x in range(new_width) for y in range(new_height)
           if (min(old_width - 1, int((x + 0.5) * old_width / new_width)),
               min(old_height - 1, int((y + 0.5) * old_height / new_height))) in old}
    assert len(new) == new_width * new_height, 'Regrid lost full roof coverage'
print('Verified inverse grid coverage for expanding and contracting roofs')
print('Applied linked roof and parent regrid correction')
