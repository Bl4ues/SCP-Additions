from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective')

def edit(path,old,new,tag):
    text=path.read_text(); n=text.count(old)
    if n!=1:raise AssertionError(f'{tag} occurrence count {n}')
    path.write_text(text.replace(old,new,1))

geo=root/'facility/mapping/client/FacilityRoomOutlineGeometry.java'
edit(geo,'''    public Rectangle2D bounds() {
        return bounds;
    }

    public List<Layer> layers()''','''    public Rectangle2D bounds() {
        return bounds;
    }

    /** A separate rendering mask; the source's selectable room Area is never
     * modified when another room covers it in the SCP-079 map. */
    public Area areaCopy() {
        return new Area(merged);
    }

    public FacilityRoomOutlineGeometry visibleOutside(Area covered) {
        if (covered == null || covered.isEmpty()
                || !covered.getBounds2D().intersects(bounds)) return this;
        Area visible = new Area(merged);
        visible.subtract(covered);
        if (visible.equals(merged)) return this;
        return new FacilityRoomOutlineGeometry(visible,
                contours(visible), visible.getBounds2D(), layers);
    }

    public List<Layer> layers()''','cache visibility without altering room selection')

screen=root/'client/scp079/Scp079FacilityMapScreen.java'
edit(screen,'''import java.awt.geom.Rectangle2D;''','''import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;''','add Area import')
edit(screen,'''    private List<MapDoorMarker> cachedDoorMarkers = List.of();''','''    // Layer silhouettes change only if the hovered room moves to the top or
    // the operator selects a different floor, never with zoom/pan/render tick.
    private int visibleRoomFloor = Integer.MIN_VALUE;
    private List<UUID> visibleRoomOrder = List.of();
    private Map<UUID, FacilityRoomOutlineGeometry> visibleRoomGeometry = Map.of();
    private List<MapDoorMarker> cachedDoorMarkers = List.of();''','room visibility render cache')
edit(screen,'''        if (hoveredRoom != null) {
            FacilityRoomSnapshot hover = hoveredRoom;
            drawOrder.removeIf(room -> room.id().equals(hover.id()));
            drawOrder.add(hover);
        }

        int minY''','''        if (hoveredRoom != null) {
            FacilityRoomSnapshot hover = hoveredRoom;
            drawOrder.removeIf(room -> room.id().equals(hover.id()));
            drawOrder.add(hover);
        }
        // A lower room's contour should not shine through a higher room at a
        // shared corner. Compute visible Areas once per draw order, not once per
        // frame: the original areas remain authoritative for hover/door probes.
        List<UUID> order = drawOrder.stream()
                .map(FacilityRoomSnapshot::id).toList();
        if (visibleRoomFloor != floorIndex
                || !order.equals(visibleRoomOrder)) {
            Area covered = new Area();
            Map<UUID, FacilityRoomOutlineGeometry> visible = new HashMap<>();
            for (int index = drawOrder.size() - 1; index >= 0; index--) {
                FacilityRoomSnapshot room = drawOrder.get(index);
                FacilityRoomOutlineGeometry source = geometryByRoom.get(room);
                if (source == null || source.empty()) continue;
                visible.put(room.id(), source.visibleOutside(covered));
                covered.add(source.areaCopy());
            }
            visibleRoomFloor = floorIndex;
            visibleRoomOrder = order;
            visibleRoomGeometry = Map.copyOf(visible);
        }

        int minY''','cache occlusion with stable room order')
edit(screen,'''        for (FacilityRoomSnapshot room : drawOrder) {
            FacilityRoomOutlineGeometry geometry = geometryByRoom.get(room);
            boolean hovered''','''        for (FacilityRoomSnapshot room : drawOrder) {
            FacilityRoomOutlineGeometry geometry =
                    visibleRoomGeometry.getOrDefault(room.id(),
                            geometryByRoom.get(room));
            boolean hovered''','render only exposed silhouettes')
print('SCP-079 caches per-floor visible room silhouettes and preserves original hit geometry.')
