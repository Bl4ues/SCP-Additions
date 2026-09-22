from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')

def change(path, old, new, count=1):
    source = path.read_text()
    assert source.count(old) == count, (path, old[:90], source.count(old))
    path.write_text(source.replace(old, new))

surface = root / 'ConstructionSurface.java'
change(surface, 'import java.util.LinkedHashMap;\n', 'import java.util.ArrayList;\nimport java.util.LinkedHashMap;\nimport java.util.List;\n')
change(surface, '''    public Vec3 point(double u, double v) {
        Vec3 bottom = TransformMath.quadratic(bottomStart, bottomControl(),
''', '''    public Vec3 point(double u, double v) {
        // The linked roof must use the exact sampled parent boundaries.
        // Independently fitting two quadratic curves introduced a visible
        // opening whenever the parents used different arc-length grids.
        if (bridge != null && bridge.hasProfiles()) {
            Vec3 bottom = bridge.profilePoint(true, u);
            Vec3 top = bridge.profilePoint(false, u);
            double bulge = 4.0D * v * (1.0D - v);
            return bottom.scale(1.0D - v).add(top.scale(v))
                    .add(heightCurveOffset.scale(bulge));
        }
        Vec3 bottom = TransformMath.quadratic(bottomStart, bottomControl(),
''')
change(surface, '''    public Vec3 tangent(double u, double v) {
        Vec3 bottom = TransformMath.quadraticTangent(bottomStart,
''', '''    public Vec3 tangent(double u, double v) {
        if (bridge != null && bridge.hasProfiles()) {
            Vec3 a = point(Math.max(0.0D, u - 0.0005D), v);
            Vec3 b = point(Math.min(1.0D, u + 0.0005D), v);
            return TransformMath.safeNormalize(b.subtract(a),
                    new Vec3(1.0D, 0.0D, 0.0D));
        }
        Vec3 bottom = TransformMath.quadraticTangent(bottomStart,
''')
change(surface, '''    public Vec3 vertical(double u, double v) {
        Vec3 bottom = TransformMath.quadratic(bottomStart, bottomControl(),
''', '''    public Vec3 vertical(double u, double v) {
        if (bridge != null && bridge.hasProfiles()) {
            Vec3 derivative = bridge.profilePoint(false, u)
                    .subtract(bridge.profilePoint(true, u))
                    .add(heightCurveOffset.scale(4.0D * (1.0D - 2.0D * v)));
            return TransformMath.safeNormalize(derivative,
                    new Vec3(0.0D, 1.0D, 0.0D));
        }
        Vec3 bottom = TransformMath.quadratic(bottomStart, bottomControl(),
''')
change(surface, '''    public double gridParameter(double fraction) {
        return metrics().horizontal().parameter(fraction);
''', '''    public double gridParameter(double fraction) {
        // Parent profiles are already parameterized by their logical grid.
        // Reparameterizing them by the roof's centerline would move the
        // attachment vertices away from the source wall again.
        if (bridge != null && bridge.hasProfiles())
            return Math.max(0.0D, Math.min(1.0D, fraction));
        return metrics().horizontal().parameter(fraction);
''')
change(surface, '''        GeometryKey key = new GeometryKey(bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset,
                topCurveOffset);
''', '''        GeometryKey key = new GeometryKey(bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset,
                topCurveOffset, bridge);
''')
change(surface, '''    private record GeometryKey(Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset,
            Vec3 heightCurveOffset, Vec3 topCurveOffset) {
''', '''    private record GeometryKey(Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset,
            Vec3 heightCurveOffset, Vec3 topCurveOffset,
            BridgeAnchor bridge) {
''')
change(surface, '''    public ConstructionSurface withFlipped(boolean nextFlipped) {
        if (nextFlipped == flipped) return this;
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset, attachments,
                overlays, nextFlipped);
    }
''', '''    public ConstructionSurface withFlipped(boolean nextFlipped) {
        if (nextFlipped == flipped) return this;
        // Keep the parent link and separate top curve. Losing them on F turned
        // the constrained roof into a free Surface and collapsed its arch.
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset, attachments,
                overlays, nextFlipped, topCurveOffset, bridge);
    }
''')
s = surface.read_text()
start = s.index('    /** Two authored parent edges. The bridge cannot be pulled off them. */')
end = s.index('    public record SurfaceSlot(', start)
s = s[:start] + '''    /** Parent edges sampled in their own logical grids, persisted with the roof. */
    public record BridgeAnchor(UUID firstId, int firstEdge,
            UUID secondId, int secondEdge, boolean reverseSecond,
            List<Vec3> firstProfile, List<Vec3> secondProfile) {
        public BridgeAnchor(UUID firstId, int firstEdge,
                UUID secondId, int secondEdge, boolean reverseSecond) {
            this(firstId, firstEdge, secondId, secondEdge, reverseSecond,
                    List.of(), List.of());
        }

        public BridgeAnchor {
            if (firstId == null || secondId == null
                    || firstId.equals(secondId)
                    || firstEdge < 0 || firstEdge > 3
                    || secondEdge < 0 || secondEdge > 3) {
                throw new IllegalArgumentException("Invalid linked Surface edges");
            }
            firstProfile = firstProfile == null ? List.of()
                    : List.copyOf(firstProfile);
            secondProfile = secondProfile == null ? List.of()
                    : List.copyOf(secondProfile);
            if (firstProfile.size() != secondProfile.size()
                    || firstProfile.size() < 2) {
                firstProfile = List.of();
                secondProfile = List.of();
            }
        }

        public boolean hasProfiles() {
            return firstProfile.size() >= 2;
        }

        public Vec3 profilePoint(boolean first, double fraction) {
            List<Vec3> points = first ? firstProfile : secondProfile;
            if (points.isEmpty()) return Vec3.ZERO;
            double coordinate = Math.max(0.0D,
                    Math.min(1.0D, fraction)) * (points.size() - 1);
            int index = Math.min(points.size() - 2,
                    (int) Math.floor(coordinate));
            double part = coordinate - index;
            return points.get(index).scale(1.0D - part)
                    .add(points.get(index + 1).scale(part));
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("First", firstId);
            tag.putInt("FirstEdge", firstEdge);
            tag.putUUID("Second", secondId);
            tag.putInt("SecondEdge", secondEdge);
            tag.putBoolean("ReverseSecond", reverseSecond);
            saveProfile(tag, "FirstProfile", firstProfile);
            saveProfile(tag, "SecondProfile", secondProfile);
            return tag;
        }

        private static void saveProfile(CompoundTag tag, String name,
                List<Vec3> points) {
            if (points.isEmpty()) return;
            ListTag entries = new ListTag();
            for (Vec3 point : points) {
                CompoundTag entry = new CompoundTag();
                putVec(entry, "Point", point);
                entries.add(entry);
            }
            tag.put(name, entries);
        }

        private static List<Vec3> loadProfile(CompoundTag tag, String name) {
            ListTag entries = tag.getList(name, Tag.TAG_COMPOUND);
            if (entries.isEmpty() || entries.size() > 2049) return List.of();
            List<Vec3> points = new ArrayList<>(entries.size());
            for (int index = 0; index < entries.size(); index++)
                points.add(getVec(entries.getCompound(index), "Point"));
            return List.copyOf(points);
        }

        private static BridgeAnchor load(CompoundTag tag) {
            if (!tag.hasUUID("First") || !tag.hasUUID("Second")) return null;
            int firstEdge = tag.getInt("FirstEdge");
            int secondEdge = tag.getInt("SecondEdge");
            if (firstEdge < 0 || firstEdge > 3
                    || secondEdge < 0 || secondEdge > 3
                    || tag.getUUID("First").equals(tag.getUUID("Second")))
                return null;
            return new BridgeAnchor(tag.getUUID("First"), firstEdge,
                    tag.getUUID("Second"), secondEdge,
                    tag.getBoolean("ReverseSecond"),
                    loadProfile(tag, "FirstProfile"),
                    loadProfile(tag, "SecondProfile"));
        }
    }

''' + s[end:]
surface.write_text(s)

bridge = root / 'TransformSurfaceBridge.java'
change(bridge, '''        return derive(id, first.dimension(), anchor, first, second,
                Vec3.ZERO, Map.of(), Map.of(), false);
''', '''        ConstructionSurface roof = derive(id, first.dimension(), anchor,
                first, second, Vec3.ZERO, Map.of(), Map.of(), false);
        // A newly bridged ceiling occupies the exterior/upward side by
        // default, regardless of the order in which its parent edges were
        // selected. F can still deliberately reverse that side afterwards.
        return roof.gridNormal(0.5D, 0.5D).y < -0.15D
                ? roof.withFlipped(true) : roof;
''')
change(bridge, '''    private static ConstructionSurface derive(UUID id,
''', '''    private static Vec3 edgeGridPoint(ConstructionSurface parent, int edge,
            double t) {
        return switch (edge) {
            case 0 -> parent.gridPoint(0.0D, t);
            case 1 -> parent.gridPoint(1.0D, t);
            case 2 -> parent.gridPoint(t, 0.0D);
            case 3 -> parent.gridPoint(t, 1.0D);
            default -> throw new IllegalArgumentException("Unknown Surface edge");
        };
    }

    private static int edgeCells(ConstructionSurface parent, int edge) {
        return edge < 2 ? parent.rows() : parent.columns();
    }

    private static int gcd(int a, int b) {
        while (b != 0) { int rem = a % b; a = b; b = rem; }
        return Math.max(1, a);
    }

    private static ConstructionSurface derive(UUID id,
''')
change(bridge, '''        return new ConstructionSurface(id, dimension, a0, a1, b0, b1,
                aBend, new Vec3(0.0D, heightBend.y, 0.0D),
                attachments, overlays, flipped, bBend.subtract(aBend), link);
''', '''        // Sample the very same normalized grid fractions as the parents.
        // A common multiple of their edge cell counts makes the authored
        // vertices of both walls explicit vertices of the roof boundaries.
        int firstCells = edgeCells(first, link.firstEdge());
        int secondCells = edgeCells(second, link.secondEdge());
        int common = firstCells / gcd(firstCells, secondCells) * secondCells;
        int samples = Math.min(768, Math.max(48, common * 4));
        List<Vec3> firstProfile = new ArrayList<>(samples + 1);
        List<Vec3> secondProfile = new ArrayList<>(samples + 1);
        for (int index = 0; index <= samples; index++) {
            double t = index / (double) samples;
            firstProfile.add(edgeGridPoint(first, link.firstEdge(), t));
            secondProfile.add(edgeGridPoint(second, link.secondEdge(),
                    link.reverseSecond() ? 1.0D - t : t));
        }
        ConstructionSurface.BridgeAnchor updatedLink =
                new ConstructionSurface.BridgeAnchor(link.firstId(),
                        link.firstEdge(), link.secondId(), link.secondEdge(),
                        link.reverseSecond(), firstProfile, secondProfile);
        return new ConstructionSurface(id, dimension, a0, a1, b0, b1,
                aBend, new Vec3(0.0D, heightBend.y, 0.0D),
                attachments, overlays, flipped, bBend.subtract(aBend),
                updatedLink);
''')
print('Linked Surface patch applied')
