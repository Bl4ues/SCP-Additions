from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective')

def replace(path, old, new, count=1):
    src = path.read_text()
    actual = src.count(old)
    if actual != count:
        raise RuntimeError(f'{path}: expected {count} occurrences, got {actual}: {old[:100]!r}')
    path.write_text(src.replace(old, new))

surface = root / 'facility/transform/ConstructionSurface.java'
bridge = root / 'facility/transform/TransformSurfaceBridge.java'
saved = root / 'facility/transform/TransformConstructionSavedData.java'
mapfile = root / 'client/scp079/Scp079FacilityMapScreen.java'

replace(surface, '''    public record BridgeAnchor(UUID firstId, int firstEdge,
            UUID secondId, int secondEdge, boolean reverseSecond,
            List<Vec3> firstProfile, List<Vec3> secondProfile) {
        public BridgeAnchor(UUID firstId, int firstEdge,
                UUID secondId, int secondEdge, boolean reverseSecond) {
            this(firstId, firstEdge, secondId, secondEdge, reverseSecond,
                    List.of(), List.of());
        }
''', '''    public record BridgeAnchor(UUID firstId, int firstEdge,
            UUID secondId, int secondEdge, boolean reverseSecond,
            List<Vec3> firstProfile, List<Vec3> secondProfile,
            int firstCells, int secondCells) {
        public BridgeAnchor(UUID firstId, int firstEdge,
                UUID secondId, int secondEdge, boolean reverseSecond) {
            this(firstId, firstEdge, secondId, secondEdge, reverseSecond,
                    List.of(), List.of(), 0, 0);
        }

        /** Read older linked roofs without changing their saved block slots. */
        public BridgeAnchor(UUID firstId, int firstEdge,
                UUID secondId, int secondEdge, boolean reverseSecond,
                List<Vec3> firstProfile, List<Vec3> secondProfile) {
            this(firstId, firstEdge, secondId, secondEdge, reverseSecond,
                    firstProfile, secondProfile, 0, 0);
        }
''')
replace(surface, '''            if (firstProfile.size() != secondProfile.size()
                    || firstProfile.size() < 2) {
                firstProfile = List.of();
                secondProfile = List.of();
            }
        }

        public boolean hasProfiles() {
            return firstProfile.size() >= 2;
''', '''            firstCells = Math.max(0, Math.min(512, firstCells));
            secondCells = Math.max(0, Math.min(512, secondCells));
            // Both parent edges are sampled on THEIR OWN logical grids. Their
            // sample counts need not agree: forcing a shared LCM grid omitted
            // authored parent vertices once the safety cap was reached.
            if (firstProfile.size() < 2 || secondProfile.size() < 2) {
                firstProfile = List.of();
                secondProfile = List.of();
            }
        }

        public boolean hasProfiles() {
            return firstProfile.size() >= 2 && secondProfile.size() >= 2;
''')
replace(surface, '''            tag.putBoolean("ReverseSecond", reverseSecond);
            saveProfile(tag, "FirstProfile", firstProfile);
''', '''            tag.putBoolean("ReverseSecond", reverseSecond);
            if (firstCells > 0) tag.putInt("FirstCells", firstCells);
            if (secondCells > 0) tag.putInt("SecondCells", secondCells);
            saveProfile(tag, "FirstProfile", firstProfile);
''')
replace(surface, '''                    tag.getBoolean("ReverseSecond"),
                    loadProfile(tag, "FirstProfile"),
                    loadProfile(tag, "SecondProfile"));
''', '''                    tag.getBoolean("ReverseSecond"),
                    loadProfile(tag, "FirstProfile"),
                    loadProfile(tag, "SecondProfile"),
                    tag.getInt("FirstCells"), tag.getInt("SecondCells"));
''')
replace(bridge, '''    private static int gcd(int a, int b) {
        while (b != 0) { int rem = a % b; a = b; b = rem; }
        return Math.max(1, a);
    }
''', '''    private static int boundarySamples(int parentCells) {
        // Each parent cell boundary is exactly one sample endpoint. The two
        // edge profiles may have different lengths; no LCM or 768-sample cap
        // silently discards one wall's authored vertices.
        return parentCells * Math.max(4, (48 + parentCells - 1) / parentCells);
    }
''')
replace(bridge, '''        // Sample the very same normalized grid fractions as the parents.
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
''', '''        // Two independently sampled half-roof boundaries, welded at their
        // shared centerline by ConstructionSurface.point(). Each half inherits
        // its own parent grid; a 7-cell wall need not be quantized onto the
        // same longitudinal divisions as a 10-cell wall.
        int firstCells = edgeCells(first, link.firstEdge());
        int secondCells = edgeCells(second, link.secondEdge());
        int firstSamples = boundarySamples(firstCells);
        int secondSamples = boundarySamples(secondCells);
        List<Vec3> firstProfile = new ArrayList<>(firstSamples + 1);
        List<Vec3> secondProfile = new ArrayList<>(secondSamples + 1);
        for (int index = 0; index <= firstSamples; index++) {
            double t = index / (double) firstSamples;
            firstProfile.add(edgeGridPoint(first, link.firstEdge(), t));
        }
        for (int index = 0; index <= secondSamples; index++) {
            double t = index / (double) secondSamples;
            secondProfile.add(edgeGridPoint(second, link.secondEdge(),
                    link.reverseSecond() ? 1.0D - t : t));
        }
        ConstructionSurface.BridgeAnchor updatedLink =
                new ConstructionSurface.BridgeAnchor(link.firstId(),
                        link.firstEdge(), link.secondId(), link.secondEdge(),
                        link.reverseSecond(), firstProfile, secondProfile,
                        firstCells, secondCells);
''')
replace(saved, '''        for (int index = 0; index < surfaces.size(); index++) {
            ConstructionSurface surface = ConstructionSurface.load(
                    surfaces.getCompound(index));
            if (surface != null) data.surfaces.put(surface.id(), surface);
        }
        return data;
''', '''        for (int index = 0; index < surfaces.size(); index++) {
            ConstructionSurface surface = ConstructionSurface.load(
                    surfaces.getCompound(index));
            if (surface != null) data.surfaces.put(surface.id(), surface);
        }
        // Upgrade existing authored linked ceilings on world load. Keep their
        // IDs, blocks, shape control and mother surfaces; only re-sample the
        // two saved edge profiles on their respective parent grids.
        boolean upgraded = false;
        for (ConstructionSurface roof : List.copyOf(data.surfaces.values())) {
            ConstructionSurface.BridgeAnchor link = roof.bridge();
            if (link == null || link.firstCells() > 0 && link.secondCells() > 0)
                continue;
            ConstructionSurface first = data.surfaces.get(link.firstId());
            ConstructionSurface second = data.surfaces.get(link.secondId());
            if (first == null || second == null) continue;
            ConstructionSurface refreshed = TransformSurfaceBridge.reanchor(
                    roof, first, second);
            if (!refreshed.equals(roof)) {
                data.surfaces.put(roof.id(), refreshed);
                upgraded = true;
            }
        }
        if (upgraded) data.setDirty();
        return data;
''')
replace(mapfile, '''        double thickness = Math.max(0.10D, transform.scale() * 0.10D);
''', '''        double thickness = Math.max(0.10D, transform.scale() * 0.165D);
''')
print('Split parent edge profiles and modest door bar thickness applied')
