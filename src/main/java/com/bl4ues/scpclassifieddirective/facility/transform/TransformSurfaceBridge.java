package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent, server-authoritative connection between two existing Surface edges. */
public final class TransformSurfaceBridge {
    private TransformSurfaceBridge() { }

    public static Vec3 edgePoint(ConstructionSurface surface, int edge,
            double t) {
        return switch (edge) {
            case 0 -> surface.point(0.0D, t);
            case 1 -> surface.point(1.0D, t);
            case 2 -> surface.point(t, 0.0D);
            case 3 -> surface.point(t, 1.0D);
            default -> throw new IllegalArgumentException("Unknown Surface edge");
        };
    }

    private static Vec3 edgeBend(ConstructionSurface parent, int edge,
            boolean reverse) {
        Vec3 start = edgePoint(parent, edge, reverse ? 1.0D : 0.0D);
        Vec3 end = edgePoint(parent, edge, reverse ? 0.0D : 1.0D);
        Vec3 middle = edgePoint(parent, edge, 0.5D);
        return middle.subtract(start.add(end).scale(0.5D)).scale(2.0D);
    }

    public static ConstructionSurface create(UUID id,
            ConstructionSurface first, int firstEdge,
            ConstructionSurface second, int secondEdge) {
        if (id == null || first == null || second == null
                || first.id().equals(second.id())
                || first.bridge() != null || second.bridge() != null
                || !first.dimension().equals(second.dimension())
                || firstEdge < 0 || firstEdge > 3
                || secondEdge < 0 || secondEdge > 3) return null;
        Vec3 a0 = edgePoint(first, firstEdge, 0.0D);
        Vec3 a1 = edgePoint(first, firstEdge, 1.0D);
        Vec3 b0 = edgePoint(second, secondEdge, 0.0D);
        Vec3 b1 = edgePoint(second, secondEdge, 1.0D);
        boolean reverse = a0.distanceToSqr(b1) + a1.distanceToSqr(b0)
                < a0.distanceToSqr(b0) + a1.distanceToSqr(b1);
        ConstructionSurface.BridgeAnchor anchor =
                new ConstructionSurface.BridgeAnchor(first.id(), firstEdge,
                        second.id(), secondEdge, reverse);
        ConstructionSurface roof = derive(id, first.dimension(), anchor,
                first, second, Vec3.ZERO, Map.of(), Map.of(), false);
        // A newly bridged ceiling occupies the exterior/upward side by
        // default, regardless of the order in which its parent edges were
        // selected. F can still deliberately reverse that side afterwards.
        return roof.gridNormal(0.5D, 0.5D).y < -0.15D
                ? roof.withFlipped(true) : roof;
    }

    public static ConstructionSurface reanchor(ConstructionSurface current,
            ConstructionSurface first, ConstructionSurface second) {
        if (current == null || current.bridge() == null
                || first == null || second == null) return current;
        // A parent edit can change the linked roof's longitudinal or
        // transverse cell count. Regrid its existing blocks against the new
        // geometry instead of copying their old indices into a larger mesh.
        ConstructionSurface geometry = derive(current.id(),
                current.dimension(), current.bridge(), first, second,
                current.heightCurveOffset(), Map.of(), Map.of(),
                current.flipped());
        return current.regridTo(geometry);
    }

    private static Vec3 edgeGridPoint(ConstructionSurface parent, int edge,
            double t) {
        return switch (edge) {
            case 0 -> parent.gridPoint(0.0D, t);
            case 1 -> parent.gridPoint(1.0D, t);
            case 2 -> parent.gridPoint(t, 0.0D);
            case 3 -> parent.gridPoint(t, 1.0D);
            default -> throw new IllegalArgumentException("Unknown Surface edge");
        };
    }

    /** Derivative pointing out of the selected mother edge and into the
     * child's side of the join; reverseSecond is longitudinal only. */
    private static Vec3 edgeOutward(ConstructionSurface parent, int edge,
            double t) {
        Vec3 tangent = edge < 2
                ? parent.gridTangent(edge == 0 ? 0.0D : 1.0D, t)
                : parent.gridVertical(t, edge == 2 ? 0.0D : 1.0D);
        return tangent.scale(edge == 0 || edge == 2 ? -1.0D : 1.0D);
    }

    private static int edgeCells(ConstructionSurface parent, int edge) {
        return edge < 2 ? parent.rows() : parent.columns();
    }

    static int boundarySamples(int parentCells) {
        // Save the actual parent-side polygonal chord positions, not just a
        // coarse approximation of its quadratic curve. A roof can only share
        // the rendered edge if its persisted profile has the same cut points
        // as the parent's (up to 24 subdivisions per logical cell). Keep the
        // NBT profile within BridgeAnchor.loadProfile's 2049-point limit for
        // unusually large authored walls.
        int cells = Math.max(1, parentCells);
        return cells * Math.max(1, Math.min(24, 2048 / cells));
    }

    private static ConstructionSurface derive(UUID id,
            net.minecraft.resources.ResourceLocation dimension,
            ConstructionSurface.BridgeAnchor link,
            ConstructionSurface first, ConstructionSurface second,
            Vec3 heightBend,
            Map<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> attachments,
            Map<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> overlays,
            boolean flipped) {
        Vec3 a0 = edgePoint(first, link.firstEdge(), 0.0D);
        Vec3 a1 = edgePoint(first, link.firstEdge(), 1.0D);
        Vec3 b0 = edgePoint(second, link.secondEdge(),
                link.reverseSecond() ? 1.0D : 0.0D);
        Vec3 b1 = edgePoint(second, link.secondEdge(),
                link.reverseSecond() ? 0.0D : 1.0D);
        Vec3 aBend = edgeBend(first, link.firstEdge(), false);
        Vec3 bBend = edgeBend(second, link.secondEdge(),
                link.reverseSecond());
        // Two independently sampled half-roof boundaries, welded at their
        // shared centerline by ConstructionSurface.point(). Each half inherits
        // its own parent grid; a 7-cell wall need not be quantized onto the
        // same longitudinal divisions as a 10-cell wall.
        int firstCells = edgeCells(first, link.firstEdge());
        int secondCells = edgeCells(second, link.secondEdge());
        int firstSamples = boundarySamples(firstCells);
        int secondSamples = boundarySamples(secondCells);
        List<Vec3> firstProfile = new ArrayList<>(firstSamples + 1);
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
                        firstCells, secondCells, firstOutward, secondOutward);
        return new ConstructionSurface(id, dimension, a0, a1, b0, b1,
                aBend, new Vec3(0.0D, heightBend.y, 0.0D),
                attachments, overlays, flipped, bBend.subtract(aBend),
                updatedLink);
    }

    /** Reanchor only direct dependent roofs after a committed parent edit. */
    public static List<UUID> refreshDependents(
            TransformConstructionSavedData data, UUID changedId) {
        List<UUID> refreshed = new ArrayList<>();
        for (ConstructionSurface old : data.surfaces()) {
            ConstructionSurface.BridgeAnchor link = old.bridge();
            if (link == null || (!link.firstId().equals(changedId)
                    && !link.secondId().equals(changedId))) continue;
            ConstructionSurface first = data.surface(link.firstId());
            ConstructionSurface second = data.surface(link.secondId());
            if (first == null || second == null) continue;
            ConstructionSurface next = reanchor(old, first, second);
            if (!next.equals(old)) {
                data.putSurface(next);
                refreshed.add(next.id());
            }
        }
        return refreshed;
    }
}
