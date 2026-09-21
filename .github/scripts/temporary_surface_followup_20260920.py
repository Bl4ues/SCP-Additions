from pathlib import Path

ROOT = Path('src/main/java/com/bl4ues/scpclassifieddirective')

def patch(path, old, new, label):
    text = path.read_text()
    found = text.count(old)
    if found != 1:
        raise AssertionError(f'{label}: expected exactly one match, got {found}')
    path.write_text(text.replace(old, new, 1))

renderer = ROOT / 'facility/transform/client/TransformConstructionClientRenderer.java'
patch(renderer,
'''        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column() + 1, slot.row()));
        // A changed border block''',
'''        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column() + 1, slot.row()));
        // Breaking a full block also exposes the DOWN/UP faces of the two
        // vertical neighbours. Rebuild all four immediate neighbours so
        // formerly culled faces return without invalidating the whole wall.
        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column(), slot.row() - 1));
        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column(), slot.row() + 1));
        // A changed border block''', 'reveal vertical faces after deleting a block')

patch(renderer,
'''    private static final Map<UUID, CachedGroup> GROUP_MESHES =
            new HashMap<>();''',
'''    // Capture the actual world-space camera transform during the solid pass;
    // AFTER_LEVEL uses a different PoseStack under some shader pipelines.
    private static org.joml.Matrix4f editorWorldPose;
    private static org.joml.Matrix3f editorWorldNormal;
    private static final Map<UUID, CachedGroup> GROUP_MESHES =
            new HashMap<>();''', 'editor matrix cache')

patch(renderer,
'''        DIRTY_SURFACE_SLOTS.clear();
    }

    static void markGroupCellDirty''',
'''        DIRTY_SURFACE_SLOTS.clear();
        editorWorldPose = null;
        editorWorldNormal = null;
    }

    static void markGroupCellDirty''', 'clear editor matrix cache')

patch(renderer,
'''        // Use precisely the same camera-relative pose as the Surface mesh.
        // The old AFTER_LEVEL callback supplied a different projection frame,
        // leaving axes apparently attached to the screen rather than the handle.
        renderEditorGizmos(minecraft, pose, buffers, camera);
        pose.popPose();''',
'''        // Save the known-good world transform for the final editor pass.
        // Render only after *all* world layers: translucent/deferred shaders
        // otherwise paint over the gizmos drawn at AFTER_SOLID_BLOCKS.
        if (minecraft.player.isCreative()
                && TransformConstructionClientState.selection() != null) {
            editorWorldPose = new org.joml.Matrix4f(pose.last().pose());
            editorWorldNormal = new org.joml.Matrix3f(pose.last().normal());
        } else {
            editorWorldPose = null;
            editorWorldNormal = null;
        }
        pose.popPose();''', 'capture editor pose before final world render')

patch(renderer,
'''    private static void renderEditorGizmos(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera) {''',
'''    @SubscribeEvent
    public static void renderEditorGizmosAfterWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL
                || editorWorldPose == null || editorWorldNormal == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        PoseStack pose = new PoseStack();
        pose.last().pose().set(editorWorldPose);
        pose.last().normal().set(editorWorldNormal);
        editorWorldPose = null;
        editorWorldNormal = null;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        renderEditorGizmos(minecraft, pose, buffers,
                event.getCamera().getPosition());
        buffers.endBatch(TransformEditorRenderTypes.GIZMO_LINES);
    }

    private static void renderEditorGizmos(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera) {''', 'render gizmo after world using actual world pose')

patch(renderer,
'''        int columnStep = preview ? Math.max(1, (columns + 17) / 18) : 1;
        int rowStep = preview ? Math.max(1, (rows + 11) / 12) : 1;''',
'''        // Guides need not draw hundreds of lines at editor distance.
        // Cap their cost even on first selection, before drag/preview starts.
        int columnStep = Math.max(1, (columns + (preview ? 17 : 39))
                / (preview ? 18 : 40));
        int rowStep = Math.max(1, (rows + (preview ? 11 : 19))
                / (preview ? 12 : 20));''', 'cap grid sampling on editor selection')

patch(renderer,
'''            int samples = preview ? Math.max(4, Math.min(20, rows))
                    : Math.max(4, rows * 2);''',
'''            int samples = preview ? Math.max(4, Math.min(20, rows))
                    : Math.max(4, Math.min(96, rows * 2));''', 'cap guide vertical sampling')
patch(renderer,
'''            int samples = preview ? Math.max(8, Math.min(36, columns))
                    : Math.max(8, columns * 3);''',
'''            int samples = preview ? Math.max(8, Math.min(36, columns))
                    : Math.max(8, Math.min(144, columns * 3));''', 'cap guide horizontal sampling')

print('Renderer: restored deleted-block faces, final-pass world gizmos, bounded editor guides.')
