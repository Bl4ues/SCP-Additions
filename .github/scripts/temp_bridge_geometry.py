from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')
p = root / 'ConstructionSurface.java'
s = p.read_text()

def change(old, new, where):
    global s
    n = s.count(old)
    if n != 1:
        raise RuntimeError(f'{where}: expected one match, got {n}')
    s = s.replace(old, new, 1)

change('''        Map<SurfaceOverlaySlot, SurfaceAttachment> overlays,
        boolean flipped) {
    private static final int ARC_SAMPLES''', '''        Map<SurfaceOverlaySlot, SurfaceAttachment> overlays,
        boolean flipped, Vec3 topCurveOffset, BridgeAnchor bridge) {
    private static final int ARC_SAMPLES''', 'canonical record')
change('''        heightCurveOffset = heightCurveOffset == null
                ? Vec3.ZERO : heightCurveOffset;
        attachments = attachments''', '''        heightCurveOffset = heightCurveOffset == null
                ? Vec3.ZERO : heightCurveOffset;
        topCurveOffset = topCurveOffset == null ? Vec3.ZERO : topCurveOffset;
        attachments = attachments''', 'canonical defaults')
change('''    /** Compatibility constructor for every pre-overlay authored surface. */''', '''    /** Existing authored surfaces keep their original constructor and NBT layout. */
    public ConstructionSurface(UUID id, ResourceLocation dimension,
            Vec3 bottomStart, Vec3 bottomEnd, Vec3 topStart, Vec3 topEnd,
            Vec3 curveOffset, Vec3 heightCurveOffset,
            Map<SurfaceSlot, SurfaceAttachment> attachments,
            Map<SurfaceOverlaySlot, SurfaceAttachment> overlays,
            boolean flipped) {
        this(id, dimension, bottomStart, bottomEnd, topStart, topEnd,
                curveOffset, heightCurveOffset, attachments, overlays,
                flipped, Vec3.ZERO, null);
    }

    /** Compatibility constructor for every pre-overlay authored surface. */''', 'compatibility constructor')
change('''    public Vec3 topControl() {
        return topStart.add(topEnd).scale(0.5D).add(curveOffset);
    }''', '''    public Vec3 topControl() {
        return topStart.add(topEnd).scale(0.5D)
                .add(curveOffset).add(topCurveOffset);
    }''', 'separate boundary curvature')
change('''                topStart, topEnd, curveOffset, heightCurveOffset);''', '''                topStart, topEnd, curveOffset, heightCurveOffset,
                topCurveOffset);''', 'metrics cache key')
change('''            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset,
            Vec3 heightCurveOffset) {
    }

    private static final class GeometryMetrics''', '''            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset,
            Vec3 heightCurveOffset, Vec3 topCurveOffset) {
    }

    private static final class GeometryMetrics''', 'geometry key record')
change('''                nextCurveOffset, nextHeightCurveOffset, Map.of(), Map.of(),
                flipped);''', '''                nextCurveOffset, nextHeightCurveOffset, Map.of(), Map.of(),
                flipped, topCurveOffset, bridge);''', 'geometry remap prototype')
change('''                nextHeightCurveOffset, remapped, remappedOverlays, flipped);''', '''                nextHeightCurveOffset, remapped, remappedOverlays, flipped,
                topCurveOffset, bridge);''', 'geometry remap result')
start = s.index('    public ConstructionSurface withAttachment(')
end = s.index('    public CompoundTag save()', start)
segment = s[start:end]
if segment.count(', flipped);') != 6:
    raise RuntimeError(f'expected six immutable copy constructors, got {segment.count(", flipped);")}')
segment = segment.replace(', flipped);', ', flipped, topCurveOffset, bridge);')
s = s[:start] + segment + s[end:]
change('''        putVec(tag, "HeightCurveOffset", heightCurveOffset);
        tag.putBoolean("Flipped", flipped);''', '''        putVec(tag, "HeightCurveOffset", heightCurveOffset);
        if (topCurveOffset.lengthSqr() > 1.0E-12D)
            putVec(tag, "TopCurveOffset", topCurveOffset);
        if (bridge != null) tag.put("LinkedBridge", bridge.save());
        tag.putBoolean("Flipped", flipped);''', 'save linked geometry')
change('''                attachments, overlays, tag.getBoolean("Flipped"));''', '''                attachments, overlays, tag.getBoolean("Flipped"),
                tag.contains("TopCurveOffset", Tag.TAG_COMPOUND)
                        ? getVec(tag, "TopCurveOffset") : Vec3.ZERO,
                tag.contains("LinkedBridge", Tag.TAG_COMPOUND)
                        ? BridgeAnchor.load(tag.getCompound("LinkedBridge"))
                        : null);''', 'load linked geometry')
change('''    public record SurfaceSlot(int column, int row) {
    }''', '''    /** Two authored parent edges. The bridge cannot be pulled off them. */
    public record BridgeAnchor(UUID firstId, int firstEdge,
            UUID secondId, int secondEdge, boolean reverseSecond) {
        public BridgeAnchor {
            if (firstId == null || secondId == null
                    || firstId.equals(secondId)
                    || firstEdge < 0 || firstEdge > 3
                    || secondEdge < 0 || secondEdge > 3) {
                throw new IllegalArgumentException("Invalid linked Surface edges");
            }
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("First", firstId);
            tag.putInt("FirstEdge", firstEdge);
            tag.putUUID("Second", secondId);
            tag.putInt("SecondEdge", secondEdge);
            tag.putBoolean("ReverseSecond", reverseSecond);
            return tag;
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
                    tag.getBoolean("ReverseSecond"));
        }
    }

    public record SurfaceSlot(int column, int row) {
    }''', 'bridge anchor metadata')
p.write_text(s)

bridge = root / 'TransformSurfaceBridge.java'
bridge.write_text('''package com.bl4ues.scpclassifieddirective.facility.transform;

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
        return derive(id, first.dimension(), anchor, first, second,
                Vec3.ZERO, Map.of(), Map.of(), false);
    }

    public static ConstructionSurface reanchor(ConstructionSurface current,
            ConstructionSurface first, ConstructionSurface second) {
        if (current == null || current.bridge() == null
                || first == null || second == null) return current;
        return derive(current.id(), current.dimension(), current.bridge(),
                first, second, current.heightCurveOffset(),
                current.attachments(), current.overlays(), current.flipped());
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
        return new ConstructionSurface(id, dimension, a0, a1, b0, b1,
                aBend, new Vec3(0.0D, heightBend.y, 0.0D),
                attachments, overlays, flipped, bBend.subtract(aBend), link);
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
''')
print('Linked curved boundaries and persistent parent-edge metadata installed.')
