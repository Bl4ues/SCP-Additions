from pathlib import Path
r=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client')
p=r/'TransformSurfaceSnapClient.java'; s=p.read_text()
old='''import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;'''
new='''import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SurfaceHandle;'''
assert s.count(old)==1;s=s.replace(old,new)
anchor='''    private static Vec3 closestSurfaceBoundary(Vec3 point,
            UUID editingSurfaceId, ClientLevel level) {'''
insert='''    /** Exact quadratic edge join: both endpoints AND the Bezier control
     * offset come from the same neighboring edge. A midpoint-only snap could
     * align a handle while leaving holes along a curved ceiling seam.
     * Reversed endpoint order is accepted; the quadratic is symmetric.
     */
    static EdgeJoin matchEdge(ConstructionSurface moved, SurfaceHandle handle,
            UUID editingSurfaceId) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || moved == null || editingSurfaceId == null) {
            return null;
        }
        boolean horizontal = handle == SurfaceHandle.BOTTOM_EDGE
                || handle == SurfaceHandle.TOP_EDGE;
        boolean vertical = handle == SurfaceHandle.START_EDGE
                || handle == SurfaceHandle.END_EDGE;
        if (!horizontal && !vertical) return null;

        Vec3 start = switch (handle) {
            case BOTTOM_EDGE, START_EDGE -> moved.bottomStart();
            case TOP_EDGE -> moved.topStart();
            case END_EDGE -> moved.bottomEnd();
            default -> Vec3.ZERO;
        };
        Vec3 end = switch (handle) {
            case BOTTOM_EDGE -> moved.bottomEnd();
            case TOP_EDGE, END_EDGE -> moved.topEnd();
            case START_EDGE -> moved.topStart();
            default -> Vec3.ZERO;
        };
        Vec3 midpoint = switch (handle) {
            case BOTTOM_EDGE -> moved.point(0.5D, 0.0D);
            case TOP_EDGE -> moved.point(0.5D, 1.0D);
            case START_EDGE -> moved.point(0.0D, 0.5D);
            case END_EDGE -> moved.point(1.0D, 0.5D);
            default -> Vec3.ZERO;
        };
        final double maxDistanceSqr = 0.40D * 0.40D;
        double best = maxDistanceSqr * 3.0D;
        EdgeJoin result = null;
        for (ConstructionSurface candidate
                : TransformConstructionClientState.surfaces(
                        level.dimension().location())) {
            if (candidate.id().equals(editingSurfaceId)
                    || !withinBounds(midpoint, candidate)) continue;
            for (int boundary = 0; boundary < 2; boundary++) {
                Vec3 a = horizontal ? boundary == 0
                        ? candidate.bottomStart() : candidate.topStart()
                        : boundary == 0 ? candidate.bottomStart()
                        : candidate.bottomEnd();
                Vec3 b = horizontal ? boundary == 0
                        ? candidate.bottomEnd() : candidate.topEnd()
                        : boundary == 0 ? candidate.topStart()
                        : candidate.topEnd();
                Vec3 middle = horizontal
                        ? candidate.point(0.5D, boundary)
                        : candidate.point(boundary, 0.5D);
                double midDistance = midpoint.distanceToSqr(middle);
                if (midDistance > maxDistanceSqr) continue;
                for (boolean reverse : new boolean[]{false, true}) {
                    Vec3 first = reverse ? b : a;
                    Vec3 last = reverse ? a : b;
                    double ds = start.distanceToSqr(first);
                    double de = end.distanceToSqr(last);
                    double score = ds + de + midDistance;
                    if (ds > maxDistanceSqr || de > maxDistanceSqr
                            || score >= best) continue;
                    best = score;
                    result = new EdgeJoin(first, last,
                            horizontal ? candidate.curveOffset()
                                    : candidate.heightCurveOffset());
                }
            }
        }
        return result;
    }

    record EdgeJoin(Vec3 start, Vec3 end, Vec3 bend) { }

'''
assert s.count(anchor)==1;s=s.replace(anchor,insert+anchor);p.write_text(s)
p=r/'TransformConstructionClientControls.java';s=p.read_text()
old='''            Vec3 handlePoint = handlePosition(moved, handle);
            Vec3 adjustment = TransformSurfaceSnapClient.snap(handlePoint,
                    surface.id()).subtract(handlePoint);'''
new='''            // When the entire edge is close to another Surface boundary,
            // match both endpoints and its Bezier bend. Matching only the
            // midpoint would leave holes away from the handle.
            TransformSurfaceSnapClient.EdgeJoin join = edgeHandle(handle)
                    ? TransformSurfaceSnapClient.matchEdge(moved, handle,
                            surface.id()) : null;
            if (join != null) {
                switch (handle) {
                    case BOTTOM_EDGE -> {
                        bs = join.start();
                        be = join.end();
                        curve = join.bend();
                    }
                    case TOP_EDGE -> {
                        ts = join.start();
                        te = join.end();
                        curve = join.bend();
                    }
                    case START_EDGE -> {
                        bs = join.start();
                        ts = join.end();
                        heightCurve = join.bend();
                    }
                    case END_EDGE -> {
                        be = join.start();
                        te = join.end();
                        heightCurve = join.bend();
                    }
                    default -> { }
                }
            } else {
            Vec3 handlePoint = handlePosition(moved, handle);
            Vec3 adjustment = TransformSurfaceSnapClient.snap(handlePoint,
                    surface.id()).subtract(handlePoint);'''
assert s.count(old)==1;s=s.replace(old,new)
old='''                    case CENTER -> { }
                }
            }
        }

        ConstructionSurface next = surface.withGeometry(bs, be, ts, te, curve,'''
new='''                    case CENTER -> { }
                }
            }
            }
        }

        ConstructionSurface next = surface.withGeometry(bs, be, ts, te, curve,'''
assert s.count(old)==1;s=s.replace(old,new)
p.write_text(s)
print('Added exact neighboring Surface edge snap, with reversed-edge support and Ctrl opt-out.')
