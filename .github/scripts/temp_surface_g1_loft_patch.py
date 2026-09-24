from pathlib import Path

ROOT = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')

def patch(path, before, after):
    content = path.read_text(encoding='utf-8')
    count = content.count(before)
    if count != 1:
        raise RuntimeError(f'{path.name}: expected unique anchor, found {count}: {before[:90]!r}')
    path.write_text(content.replace(before, after, 1), encoding='utf-8')

surface = ROOT / 'ConstructionSurface.java'
patch(surface, '''        if (bridge != null && bridge.hasProfiles()) {
            Vec3 bottom = bridge.profilePoint(true, u);
            Vec3 top = bridge.profilePoint(false, u);
            double bulge = 4.0D * v * (1.0D - v);
            return bottom.scale(1.0D - v).add(top.scale(v))
                    .add(heightCurveOffset.scale(bulge));
        }
''', '''        if (bridge != null && bridge.hasProfiles()) {
            Vec3 bottom = bridge.profilePoint(true, u);
            Vec3 top = bridge.profilePoint(false, u);
            if (bridge.hasOutwardProfiles()) {
                // Cubic Hermite loft: both parent edges own the roof position
                // AND its cross-sectional tangent. A quadratic height bulge
                // changes that tangent at the joint, so the editable crown
                // instead uses a quartic bump with zero endpoint derivatives.
                double span = Math.max(0.125D, bottom.distanceTo(top));
                Vec3 startTangent = bridge.outwardPoint(true, u)
                        .scale(span * 0.75D);
                Vec3 endTangent = bridge.outwardPoint(false, u)
                        .scale(-span * 0.75D);
                double v2 = v * v, v3 = v2 * v;
                double h00 = 2.0D * v3 - 3.0D * v2 + 1.0D;
                double h10 = v3 - 2.0D * v2 + v;
                double h01 = -2.0D * v3 + 3.0D * v2;
                double h11 = v3 - v2;
                double bulge = 16.0D * v2 * (1.0D - v) * (1.0D - v);
                return bottom.scale(h00).add(startTangent.scale(h10))
                        .add(top.scale(h01)).add(endTangent.scale(h11))
                        .add(heightCurveOffset.scale(bulge));
            }
            double bulge = 4.0D * v * (1.0D - v);
            return bottom.scale(1.0D - v).add(top.scale(v))
                    .add(heightCurveOffset.scale(bulge));
        }
''')
patch(surface, '''        if (bridge != null && bridge.hasProfiles()) {
            Vec3 derivative = bridge.profilePoint(false, u)
                    .subtract(bridge.profilePoint(true, u))
                    .add(heightCurveOffset.scale(4.0D * (1.0D - 2.0D * v)));
            return TransformMath.safeNormalize(derivative,
                    new Vec3(0.0D, 1.0D, 0.0D));
        }
''', '''        if (bridge != null && bridge.hasProfiles()) {
            Vec3 bottom = bridge.profilePoint(true, u);
            Vec3 top = bridge.profilePoint(false, u);
            if (bridge.hasOutwardProfiles()) {
                double span = Math.max(0.125D, bottom.distanceTo(top));
                Vec3 startTangent = bridge.outwardPoint(true, u)
                        .scale(span * 0.75D);
                Vec3 endTangent = bridge.outwardPoint(false, u)
                        .scale(-span * 0.75D);
                double v2 = v * v;
                double dh00 = 6.0D * v2 - 6.0D * v;
                double dh10 = 3.0D * v2 - 4.0D * v + 1.0D;
                double dh01 = -dh00;
                double dh11 = 3.0D * v2 - 2.0D * v;
                double dbulge = 32.0D * v * (1.0D - v)
                        * (1.0D - 2.0D * v);
                Vec3 derivative = bottom.scale(dh00)
                        .add(startTangent.scale(dh10))
                        .add(top.scale(dh01))
                        .add(endTangent.scale(dh11))
                        .add(heightCurveOffset.scale(dbulge));
                return TransformMath.safeNormalize(derivative,
                        new Vec3(0.0D, 1.0D, 0.0D));
            }
            Vec3 derivative = top.subtract(bottom)
                    .add(heightCurveOffset.scale(4.0D * (1.0D - 2.0D * v)));
            return TransformMath.safeNormalize(derivative,
                    new Vec3(0.0D, 1.0D, 0.0D));
        }
''')
patch(surface, '''            List<Vec3> firstProfile, List<Vec3> secondProfile,
            int firstCells, int secondCells) {
''', '''            List<Vec3> firstProfile, List<Vec3> secondProfile,
            int firstCells, int secondCells,
            List<Vec3> firstOutward, List<Vec3> secondOutward) {
        public BridgeAnchor(UUID firstId, int firstEdge,
                UUID secondId, int secondEdge, boolean reverseSecond,
                List<Vec3> firstProfile, List<Vec3> secondProfile,
                int firstCells, int secondCells) {
            this(firstId, firstEdge, secondId, secondEdge, reverseSecond,
                    firstProfile, secondProfile, firstCells, secondCells,
                    List.of(), List.of());
        }
''')
patch(surface, '''            secondProfile = secondProfile == null ? List.of()
                    : List.copyOf(secondProfile);
            firstCells = Math.max(0, Math.min(512, firstCells));
''', '''            secondProfile = secondProfile == null ? List.of()
                    : List.copyOf(secondProfile);
            firstOutward = firstOutward == null ? List.of()
                    : List.copyOf(firstOutward);
            secondOutward = secondOutward == null ? List.of()
                    : List.copyOf(secondOutward);
            if (firstOutward.size() < 2 || secondOutward.size() < 2) {
                firstOutward = List.of();
                secondOutward = List.of();
            }
            firstCells = Math.max(0, Math.min(512, firstCells));
''')
patch(surface, '''        public boolean hasProfiles() {
            return firstProfile.size() >= 2 && secondProfile.size() >= 2;
        }

        public Vec3 profilePoint(boolean first, double fraction) {
            List<Vec3> points = first ? firstProfile : secondProfile;
''', '''        public boolean hasProfiles() {
            return firstProfile.size() >= 2 && secondProfile.size() >= 2;
        }

        public boolean hasOutwardProfiles() {
            return firstOutward.size() >= 2 && secondOutward.size() >= 2;
        }

        public Vec3 outwardPoint(boolean first, double fraction) {
            List<Vec3> points = first ? firstOutward : secondOutward;
            return TransformMath.safeNormalize(interpolateProfile(points,
                    fraction), new Vec3(0.0D, 1.0D, 0.0D));
        }

        public Vec3 profilePoint(boolean first, double fraction) {
            List<Vec3> points = first ? firstProfile : secondProfile;
            return interpolateProfile(points, fraction);
        }

        private static Vec3 interpolateProfile(List<Vec3> points,
                double fraction) {
''')
patch(surface, '''            saveProfile(tag, "FirstProfile", firstProfile);
            saveProfile(tag, "SecondProfile", secondProfile);
            return tag;
''', '''            saveProfile(tag, "FirstProfile", firstProfile);
            saveProfile(tag, "SecondProfile", secondProfile);
            saveProfile(tag, "FirstOutward", firstOutward);
            saveProfile(tag, "SecondOutward", secondOutward);
            return tag;
''')
patch(surface, '''                    loadProfile(tag, "SecondProfile"),
                    tag.getInt("FirstCells"), tag.getInt("SecondCells"));
''', '''                    loadProfile(tag, "SecondProfile"),
                    tag.getInt("FirstCells"), tag.getInt("SecondCells"),
                    loadProfile(tag, "FirstOutward"),
                    loadProfile(tag, "SecondOutward"));
''')

bridge = ROOT / 'TransformSurfaceBridge.java'
patch(bridge, '''    private static int edgeCells(ConstructionSurface parent, int edge) {
''', '''    /** Derivative pointing out of the selected mother edge and into the
     * child's side of the join; reverseSecond is longitudinal only. */
    private static Vec3 edgeOutward(ConstructionSurface parent, int edge,
            double t) {
        Vec3 tangent = edge < 2
                ? parent.gridTangent(edge == 0 ? 0.0D : 1.0D, t)
                : parent.gridVertical(t, edge == 2 ? 0.0D : 1.0D);
        return tangent.scale(edge == 0 || edge == 2 ? -1.0D : 1.0D);
    }

    private static int edgeCells(ConstructionSurface parent, int edge) {
''')
patch(bridge, '''        List<Vec3> firstProfile = new ArrayList<>(firstSamples + 1);
        List<Vec3> secondProfile = new ArrayList<>(secondSamples + 1);
''', '''        List<Vec3> firstProfile = new ArrayList<>(firstSamples + 1);
        List<Vec3> secondProfile = new ArrayList<>(secondSamples + 1);
        // A 4-per-cell tangent table is sufficient to interpolate smooth
        // parent derivatives without duplicating their 24-per-cell positions.
        int firstTangents = Math.max(1, Math.min(2048, firstCells * 4));
        int secondTangents = Math.max(1, Math.min(2048, secondCells * 4));
        List<Vec3> firstOutward = new ArrayList<>(firstTangents + 1);
        List<Vec3> secondOutward = new ArrayList<>(secondTangents + 1);
        for (int index = 0; index <= firstTangents; index++) {
            firstOutward.add(edgeOutward(first, link.firstEdge(),
                    index / (double) firstTangents));
        }
        for (int index = 0; index <= secondTangents; index++) {
            double t = index / (double) secondTangents;
            secondOutward.add(edgeOutward(second, link.secondEdge(),
                    link.reverseSecond() ? 1.0D - t : t));
        }
''')
patch(bridge, '''                link.reverseSecond(), firstProfile, secondProfile,
                firstCells, secondCells);
''', '''                link.reverseSecond(), firstProfile, secondProfile,
                firstCells, secondCells, firstOutward, secondOutward);
''')

saved = ROOT / 'TransformConstructionSavedData.java'
patch(saved, '''                    && link.secondProfile().size() == TransformSurfaceBridge
                            .boundarySamples(link.secondCells()) + 1)
''', '''                    && link.secondProfile().size() == TransformSurfaceBridge
                            .boundarySamples(link.secondCells()) + 1
                    && link.hasOutwardProfiles())
''')

renderer = ROOT / 'client/TransformConstructionClientRenderer.java'
patch(renderer, '''        int linkedHorizontal = linkedSteps == null ? 0 :
                Math.max(slot.row() == 0 && minY < 1.0E-5D
                        ? linkedSteps[2] : 0,
                        slot.row() == surface.rows() - 1
                        && maxY > 0.99999D ? linkedSteps[3] : 0);
        int linkedVertical = linkedSteps == null ? 0 :
                Math.max(slot.column() == 0 && minX < 1.0E-5D
                        ? linkedSteps[0] : 0,
                        slot.column() == surface.columns() - 1
                        && maxX > 0.99999D ? linkedSteps[1] : 0);
''', '''        // ALL mother rows/columns inherit contact-edge knots. Limiting the
        // dense grid to the topmost row left T-junctions where that row met
        // the next coarse row: the apparent cut ran THROUGH the mother wall.
        int linkedHorizontal = linkedSteps == null ? 0
                : Math.max(linkedSteps[2], linkedSteps[3]);
        int linkedVertical = linkedSteps == null ? 0
                : Math.max(linkedSteps[0], linkedSteps[1]);
''')
patch(renderer, '''        List<Double> alongX = slot.row() == 0 && minY < 1.0E-5D
                ? edges.get(2) : null;
        if (slot.row() == surface.rows() - 1 && maxY > 0.99999D
                && edges.containsKey(3)) alongX = edges.get(3);
''', '''        List<Double> alongX = edges.get(2);
        if (edges.containsKey(3)) {
            if (alongX == null) alongX = edges.get(3);
            else {
                java.util.TreeSet<Double> both = new java.util.TreeSet<>(alongX);
                both.addAll(edges.get(3));
                alongX = List.copyOf(both);
            }
        }
''')
patch(renderer, '''        List<Double> alongY = slot.column() == 0 && minX < 1.0E-5D
                ? edges.get(0) : null;
        if (slot.column() == surface.columns() - 1
                && maxX > 0.99999D && edges.containsKey(1))
            alongY = edges.get(1);
''', '''        List<Double> alongY = edges.get(0);
        if (edges.containsKey(1)) {
            if (alongY == null) alongY = edges.get(1);
            else {
                java.util.TreeSet<Double> both = new java.util.TreeSet<>(alongY);
                both.addAll(edges.get(1));
                alongY = List.copyOf(both);
            }
        }
''')
print('Patched Hermite linked-roof G1 loft, parent edge tangent persistence, migration and uniform mother tessellation')
