from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective')
renderer = root / 'facility/transform/client/TransformConstructionClientRenderer.java'
mask = root / 'inventory/client/PickupOutlineRenderer.java'

s = renderer.read_text()
needle = '''    private static CachedSurfaceSlot buildSurfaceSlot(Minecraft minecraft,
'''
if s.count(needle) != 1: raise RuntimeError('Missing unique outline insertion point')
method = '''    /** Draw exactly the cached world-space payload of a Surface slot into the
     * context mask. Rebuilding a second local-frame version of an offset door
     * button put its outline one cell away on curved walls. Using the same
     * prepared vertices as the visible Surface guarantees matching geometry. */
    public static boolean renderSurfaceContextOutline(Minecraft minecraft,
            TransformContextTargetClient.Target target, PoseStack pose,
            MultiBufferSource buffers, Vec3 camera) {
        if (target == null || target.surfaceSlot() == null
                || minecraft.level == null) return false;
        ConstructionSurface surface = TransformConstructionClientState.surface(
                target.surfaceId());
        if (surface == null) return false;
        boolean overlay = target.kind()
                == TransformContextTargetClient.Kind.SURFACE_OVERLAY;
        int side = overlay ? target.normalSign()
                : TransformSurfaceGeometry.MAIN_SIDE;
        ConstructionSurface.SurfaceAttachment attachment = overlay
                ? surface.overlay(target.surfaceSlot(), side)
                : surface.attachments().get(target.surfaceSlot());
        if (attachment == null || attachment.state().isAir()) return false;
        CachedSurface current = SURFACE_MESHES.get(surface.id());
        CachedSurfaceSlot mesh = current != null && current.surface() == surface
                ? overlay
                    ? current.overlays().get(new ConstructionSurface.SurfaceOverlaySlot(
                            target.surfaceSlot(), side))
                    : current.slots().get(target.surfaceSlot())
                : null;
        if (mesh == null) {
            mesh = buildSurfaceSlot(minecraft, surface,
                    target.surfaceSlot(), attachment, side, overlay);
        }
        if (mesh == null) return false;
        for (Map.Entry<RenderType, List<PreparedVertex>> layer
                : mesh.layers().entrySet()) {
            VertexConsumer consumer = buffers.getBuffer(layer.getKey());
            for (PreparedVertex vertex : layer.getValue()) {
                Vec3 world = vertex.position().subtract(camera);
                consumer.vertex(pose.last().pose(), (float) world.x,
                                (float) world.y, (float) world.z)
                        .color(255, 255, 255, 255)
                        .uv(vertex.u(), vertex.v())
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                        .normal(pose.last().normal(),
                                (float) vertex.normal().x,
                                (float) vertex.normal().y,
                                (float) vertex.normal().z)
                        .endVertex();
            }
        }
        return true;
    }

'''
renderer.write_text(s.replace(needle, method + needle))

s = mask.read_text()
needle = '''import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState;
'''
if s.count(needle) != 1: raise RuntimeError('Missing outline import point')
s = s.replace(needle, needle + '''import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientRenderer;
''')
needle = '''        Vec3 cameraPosition = camera.getPosition();
        poseStack.pushPose();
        try {
            if (target.kind() == TransformContextTargetClient.Kind.GROUP) {
'''
replacement = '''        Vec3 cameraPosition = camera.getPosition();
        if (target.kind() != TransformContextTargetClient.Kind.GROUP
                && TransformConstructionClientRenderer.renderSurfaceContextOutline(
                        minecraft, target, poseStack, OUTLINE_BUFFER,
                        cameraPosition)) {
            return;
        }
        poseStack.pushPose();
        try {
            if (target.kind() == TransformContextTargetClient.Kind.GROUP) {
'''
if s.count(needle) != 1: raise RuntimeError('Missing unique transformed mask point')
mask.write_text(s.replace(needle, replacement))
