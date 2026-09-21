from pathlib import Path

ROOT = Path('src/main/java/com/bl4ues/scpclassifieddirective')
RENDERER = ROOT / 'facility/transform/client/TransformConstructionClientRenderer.java'
MAP = ROOT / 'client/scp079/Scp079FacilityMapScreen.java'


def replace_once(text, before, after, context):
    count = text.count(before)
    if count != 1:
        raise RuntimeError(f'{context}: expected one match, found {count}')
    return text.replace(before, after, 1)


renderer = RENDERER.read_text()
renderer = replace_once(renderer,
    '''                Vec3 first = sourceSamples.get(0);
                Vec3 middle = sourceSamples.get(24);
                Vec3 last = sourceSamples.get(48);
''',
    '''                // Broad-phase only. A partially overlapping old wall/roof
                // must not be rejected because its endpoints do not meet.
                AABB sourceBounds = SURFACE_EDGE_BOUNDS.get(surface.id())
                        .get(edge).inflate(0.55D);
''', 'Surface edge broad-phase')
start = renderer.index('                        List<Vec3> samples = edgeSamples.get(other.id())')
end_marker = '                                new HashMap<>());'
end = renderer.index(end_marker, start) + len(end_marker)
old_matcher = renderer[start:end]
assert 'score > 0.3675D' in old_matcher and 'closestEdgeFraction' in old_matcher
new_matcher = '''                        List<Vec3> samples = edgeSamples.get(other.id())
                                .get(otherEdge);
                        if (!sourceBounds.intersects(SURFACE_EDGE_BOUNDS
                                .get(other.id()).get(otherEdge).inflate(0.55D))) {
                            continue;
                        }
                        double otherU = otherEdge == 0 ? 0.0D
                                : otherEdge == 1 ? 1.0D : 0.5D;
                        double otherV = otherEdge == 2 ? 0.0D
                                : otherEdge == 3 ? 1.0D : 0.5D;
                        double dot = surface.gridNormal(
                                edge < 2 ? edge : 0.5D,
                                edge == 2 ? 0.0D : edge == 3 ? 1.0D : 0.5D)
                                .dot(other.gridNormal(otherU, otherV));
                        if (dot <= -0.2D) continue;

                        // Existing surfaces need not have identical lengths or
                        // perfectly matching endpoints. Only weld the contiguous
                        // portions whose *physical* edges are already nearby.
                        // Sample comparisons happen after committed geometry
                        // changes and are never run in the per-frame vertex pass.
                        int close = 0;
                        int run = 0;
                        int longestRun = 0;
                        double separation = 0.0D;
                        double firstFraction = Double.NaN;
                        double lastFraction = Double.NaN;
                        for (int sample = 0; sample <= 48; sample += 4) {
                            Vec3 point = sourceSamples.get(sample);
                            double mapped = closestEdgeFraction(samples, point);
                            if (!Double.isFinite(mapped)) {
                                run = 0;
                                continue;
                            }
                            double distance = point.distanceToSqr(
                                    edgePoint(other, otherEdge, mapped));
                            if (distance > 0.45D * 0.45D) {
                                run = 0;
                                continue;
                            }
                            close++;
                            longestRun = Math.max(longestRun, ++run);
                            separation += distance;
                            if (!Double.isFinite(firstFraction)) {
                                firstFraction = mapped;
                            }
                            lastFraction = mapped;
                        }
                        // A one-point intersection between unrelated edges is
                        // not a seam; require an actual shared arc interval.
                        if (close < 3 || longestRun < 3
                                || Math.abs(lastFraction - firstFraction)
                                        < 0.035D) continue;
                        double score = separation / close
                                + (13 - close) * 0.004D;
                        if (score >= bestScore) continue;
                        bestScore = score;
                        boolean partial = close < 12;
                        match = new MatchedSurfaceEdge(other, otherEdge,
                                lastFraction < firstFraction, partial, samples,
                                new HashMap<>());'''
renderer = renderer[:start] + new_matcher + renderer[end:]

renderer = replace_once(renderer,
    '''        return found > 0 ? result.scale(1.0D / found) : null;
    }

    private static VertexFrame rigidFrame''',
    '''        if (found == 0) return null;
        // Adjacent curved surfaces may use different cell subdivisions: even
        // after their sampled border points agree, their polygon chords can
        // leave a narrow sky slit between samples. Continue each joined edge
        // 0.055 block OUTSIDE its own parametric domain, into the neighbouring
        // solid surface. This gives both meshes a small physical overlap rather
        // than relying on two independently tessellated lines touching at
        // infinitely thin vertices. The authored curve, interior vertices,
        // collision, and saved geometry are unchanged.
        Vec3 overlap = Vec3.ZERO;
        if (Math.abs(u) < 1.0E-6D) {
            overlap = overlap.subtract(surface.gridTangent(u, v));
        } else if (Math.abs(u - 1.0D) < 1.0E-6D) {
            overlap = overlap.add(surface.gridTangent(u, v));
        }
        if (Math.abs(v) < 1.0E-6D) {
            overlap = overlap.subtract(surface.gridVertical(u, v));
        } else if (Math.abs(v - 1.0D) < 1.0E-6D) {
            overlap = overlap.add(surface.gridVertical(u, v));
        }
        Vec3 joined = result.scale(1.0D / found);
        return overlap.lengthSqr() < 1.0E-10D ? joined
                : joined.add(overlap.normalize().scale(0.055D));
    }

    private static VertexFrame rigidFrame''', 'Surface seam physical overlap')
RENDERER.write_text(renderer)

screen = MAP.read_text()
screen = replace_once(screen,
    '''            renderFloorSelector(graphics, mouseX, mouseY, floor);
            renderMap(graphics, mouseX, mouseY, floor);
        }

        renderTopActions''',
    '''            renderMap(graphics, mouseX, mouseY, floor);
            // The dropdown is a HUD control. Render it after the world map so
            // overlapping/upper-floor silhouettes cannot cover its choices.
            renderFloorSelector(graphics, mouseX, mouseY, floor);
        }

        renderTopActions''', 'Dropdown render order')
screen = replace_once(screen,
    '''                hovered || floorMenuOpen ? 0xD51A3545 : 0xB8122835);''',
    '''                hovered || floorMenuOpen ? 0xFF1A3545 : 0xFF122835);''',
    'Dropdown opaque button background')
screen = replace_once(screen,
    '''                    i == floorIndex ? 0xE1265064
                            : rowHover ? 0xE11A3A4B : 0xE10B202B);''',
    '''                    i == floorIndex ? 0xFF265064
                            : rowHover ? 0xFF1A3A4B : 0xFF0B202B);''',
    'Dropdown opaque option backgrounds')
screen = replace_once(screen,
    '''        double thickness = Mth.clamp(transform.scale() * 0.45D,
                3.5D, 6.0D);''',
    '''        // Marker thickness is a world-space dimension, like its length.
        // A fixed 3.5px floor made distant doors wider than their mapped rooms.
        double thickness = Math.max(0.35D, transform.scale() * 0.45D);''',
    'Zoom-relative door thickness')
screen = replace_once(screen,
    '''            int size = 21;
            graphics.pose().pushPose();''',
    '''            // Surveillance marker geometry scales with its room; only
            // the accompanying SCP number remains a fixed-size text label.
            int size = Math.max(2, (int) Math.round(
                    transform.scale() * 1.2D));
            graphics.pose().pushPose();''', 'Zoom-relative tracker arrow')
screen = replace_once(screen,
    '''                    y + 13, 1.03F, 0xFFFF765D);''',
    '''                    y + size / 2 + 4, 1.03F, 0xFFFF765D);''',
    'Tracker number follows marker position')
screen = replace_once(screen,
    '''        return (float) Mth.clamp(transform.scale() * (0.38D / 9.0D),
                0.16D, 1.0D);''',
    '''        return (float) (transform.scale() * (0.38D / 9.0D));''',
    'Keycard clearance badge zoom')
MAP.write_text(screen)
print('Applied scoped Surface border attachment and SCP-079 map UI changes.')
