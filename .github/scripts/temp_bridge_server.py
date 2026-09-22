from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective')
mgr = root / 'facility/transform/TransformConstructionManager.java'
net = root / 'facility/transform/network/TransformConstructionNetwork.java'
mapfile = root / 'client/scp079/Scp079FacilityMapScreen.java'

def patch(path, old, new, why):
    value = path.read_text()
    count = value.count(old)
    if count != 1:
        raise RuntimeError(f'{why}: expected one anchor, found {count}')
    path.write_text(value.replace(old, new, 1))

patch(mgr,
'''        ConstructionSurface next = surface.withGeometry(bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset);
        if ((long) next.columns() * next.rows() > MAX_SURFACE_SLOTS) return false;
        data.putSurface(next);
        refreshSurface(level.getServer(), id);
        return true;
    }

    public static boolean setSurfaceFlipped''',
'''        ConstructionSurface next;
        if (surface.bridge() != null) {
            // A linked ceiling has immutable parent borders. The client can
            // request only its vertical crown; never trust supplied corners.
            ConstructionSurface.BridgeAnchor anchor = surface.bridge();
            ConstructionSurface first = data.surface(anchor.firstId());
            ConstructionSurface second = data.surface(anchor.secondId());
            if (first == null || second == null) return false;
            ConstructionSurface anchored = TransformSurfaceBridge.reanchor(
                    surface, first, second);
            Vec3 crown = new Vec3(0.0D,
                    Math.max(-64.0D, Math.min(64.0D, heightCurveOffset.y)),
                    0.0D);
            next = anchored.withGeometry(anchored.bottomStart(),
                    anchored.bottomEnd(), anchored.topStart(),
                    anchored.topEnd(), anchored.curveOffset(), crown);
        } else {
            next = surface.withGeometry(bottomStart, bottomEnd,
                    topStart, topEnd, curveOffset, heightCurveOffset);
        }
        if ((long) next.columns() * next.rows() > MAX_SURFACE_SLOTS) return false;
        if (next.equals(surface)) return true;
        data.putSurface(next);
        refreshSurface(level.getServer(), id);
        if (surface.bridge() == null) {
            // Editing either parent reanchors each linked roof once per
            // committed edit, not during the per-frame gizmo preview.
            for (UUID childId : TransformSurfaceBridge.refreshDependents(data,
                    id)) refreshSurface(level.getServer(), childId);
        }
        return true;
    }

    /** Create a two-parent ceiling from an explicit pair of authored edges. */
    public static boolean createSurfaceBridge(ServerPlayer player,
            UUID newId, UUID firstId, int firstEdge,
            UUID secondId, int secondEdge) {
        if (!canEdit(player) || newId == null || firstId == null
                || secondId == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        if (data.surface(newId) != null) return false;
        ConstructionSurface first = data.surface(firstId);
        ConstructionSurface second = data.surface(secondId);
        if (first == null || second == null
                || !first.dimension().equals(level.dimension().location())
                || !second.dimension().equals(level.dimension().location())
                || firstEdge < 0 || firstEdge > 3
                || secondEdge < 0 || secondEdge > 3) return false;
        Vec3 middleFirst = TransformSurfaceBridge.edgePoint(first,
                firstEdge, 0.5D);
        Vec3 middleSecond = TransformSurfaceBridge.edgePoint(second,
                secondEdge, 0.5D);
        if (middleFirst.distanceToSqr(player.getEyePosition()) > 40.0D * 40.0D
                || middleSecond.distanceToSqr(player.getEyePosition()) > 40.0D * 40.0D
                || middleFirst.distanceToSqr(middleSecond) > 64.0D * 64.0D) {
            return false;
        }
        ConstructionSurface roof = TransformSurfaceBridge.create(newId,
                first, firstEdge, second, secondEdge);
        if (roof == null || (long) roof.columns() * roof.rows()
                > MAX_SURFACE_SLOTS) return false;
        data.putSurface(roof);
        refreshSurface(level.getServer(), roof.id());
        player.displayClientMessage(Component.literal(
                "Linked ceiling created. Drag its center up or down to bend it."),
                true);
        return true;
    }

    public static boolean setSurfaceFlipped''', 'server-authoritative linked roof updates')

patch(mgr,
'''        boolean changed = TransformConstructionSavedData.get(player.getServer())
                .removeSurface(id);
        if (changed) refreshSurface(player.getServer(), id);
        return changed;
    }

    private static boolean placeBlock''',
'''        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                player.getServer());
        for (ConstructionSurface candidate : data.surfaces()) {
            ConstructionSurface.BridgeAnchor link = candidate.bridge();
            if (link != null && (id.equals(link.firstId())
                    || id.equals(link.secondId()))) {
                player.displayClientMessage(Component.literal(
                        "Delete the linked ceiling before removing this wall."), true);
                return false;
            }
        }
        boolean changed = data.removeSurface(id);
        if (changed) refreshSurface(player.getServer(), id);
        return changed;
    }

    private static boolean placeBlock''', 'prevent orphaned linked ceiling')

patch(net,
'''        CHANNEL.registerMessage(22, UseSurfaceOverlay.class,
                UseSurfaceOverlay::encode, UseSurfaceOverlay::decode,
                UseSurfaceOverlay::handle);
''',
'''        CHANNEL.registerMessage(22, UseSurfaceOverlay.class,
                UseSurfaceOverlay::encode, UseSurfaceOverlay::decode,
                UseSurfaceOverlay::handle);
        CHANNEL.registerMessage(23, CreateLinkedSurface.class,
                CreateLinkedSurface::encode, CreateLinkedSurface::decode,
                CreateLinkedSurface::handle);
''', 'register roof creation packet')

patch(net,
'''    public static void cancelSurfaceAuthoring() {
        CHANNEL.sendToServer(new CancelSurfaceAuthoring());
    }
''',
'''    public static void cancelSurfaceAuthoring() {
        CHANNEL.sendToServer(new CancelSurfaceAuthoring());
    }

    public static void createLinkedSurface(UUID newId, UUID firstId,
            int firstEdge, UUID secondId, int secondEdge) {
        if (newId == null || firstId == null || secondId == null) return;
        CHANNEL.sendToServer(new CreateLinkedSurface(newId, firstId,
                firstEdge, secondId, secondEdge));
    }
''', 'client roof creation request')

patch(net,
'''    public record Delete(UUID id, boolean surface) {''',
'''    /** Intent only. Server resolves parent edges and validates all geometry. */
    public record CreateLinkedSurface(UUID id, UUID firstId, int firstEdge,
            UUID secondId, int secondEdge) {
        private static void encode(CreateLinkedSurface message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            buffer.writeUUID(message.firstId);
            buffer.writeVarInt(message.firstEdge);
            buffer.writeUUID(message.secondId);
            buffer.writeVarInt(message.secondEdge);
        }

        private static CreateLinkedSurface decode(FriendlyByteBuf buffer) {
            return new CreateLinkedSurface(buffer.readUUID(),
                    buffer.readUUID(), buffer.readVarInt(),
                    buffer.readUUID(), buffer.readVarInt());
        }

        private static void handle(CreateLinkedSurface message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                TransformConstructionManager.createSurfaceBridge(sender,
                        message.id, message.firstId, message.firstEdge,
                        message.secondId, message.secondEdge);
                sendSnapshot(sender);
            });
            context.setPacketHandled(true);
        }
    }

    public record Delete(UUID id, boolean surface) {''', 'server roof creation packet')

patch(mapfile,
'''        double thickness = Math.max(0.35D, transform.scale() * 0.45D);''',
'''        // The door occupies a slim physical strip in world-map units.
        // Keep it slender at every zoom without a fixed screen-pixel floor.
        double thickness = Math.max(0.25D, transform.scale() * 0.27D);''',
'door slab proportions')

print('Server-side linked ceilings and scale-correct slim door markers patched.')
