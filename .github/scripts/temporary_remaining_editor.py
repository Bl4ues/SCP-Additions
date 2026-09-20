from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective')
def patch(path, old, new):
    p=root/path; s=p.read_text(); count=s.count(old)
    assert count==1,(str(path),old[:100],count)
    p.write_text(s.replace(old,new,1))

# The square uses a centered 18px fill, but the actual ink in Minecraft's
# numerals is optically high/right relative to that box.
patch(Path('client/scp079/Scp079FacilityMapScreen.java'),
'''        pose.pushPose();
        pose.scale(1.5F, 1.5F, 1.0F);
        // Font glyphs are 8px high in a 9px line box.''',
'''        pose.pushPose();
        // Optical correction in badge-local space: tracks the entire badge's
        // zoom transform, including when it is smaller than one GUI pixel.
        pose.translate(-0.65F, 0.65F, 0.0F);
        pose.scale(1.5F, 1.5F, 1.0F);
        // Font glyphs are 8px high in a 9px line box.''')

renderer=Path('facility/transform/client/TransformConstructionClientRenderer.java')
# A sparse preview is sufficient while a handle is in motion. Full cell grid
# returns on release, without lowering the quality of the built blocks.
patch(renderer,
'''        int columns = surface.columns();
        int rows = surface.rows();
        for (int column = 0; column <= columns; column++) {
            double u = column / (double) columns;
            Vec3 previous = visibleSurfaceGridPoint(surface, u, 0.0D,
                    camera);
            int samples = Math.max(4, rows * 2);''',
'''        int columns = surface.columns();
        int rows = surface.rows();
        boolean preview = TransformConstructionClientControls.previewingSurface(
                surface.id());
        int columnStep = preview ? Math.max(1, (columns + 17) / 18) : 1;
        int rowStep = preview ? Math.max(1, (rows + 11) / 12) : 1;
        for (int column = 0; column <= columns; column += columnStep) {
            double u = column / (double) columns;
            Vec3 previous = visibleSurfaceGridPoint(surface, u, 0.0D,
                    camera);
            int samples = preview ? Math.max(4, Math.min(20, rows))
                    : Math.max(4, rows * 2);''')
patch(renderer,
'''        for (int row = 0; row <= rows; row++) {
            double v = row / (double) rows;
            Vec3 previous = visibleSurfaceGridPoint(surface, 0.0D, v,
                    camera);
            int samples = Math.max(8, columns * 3);''',
'''        for (int row = 0; row <= rows; row += rowStep) {
            double v = row / (double) rows;
            Vec3 previous = visibleSurfaceGridPoint(surface, 0.0D, v,
                    camera);
            int samples = preview ? Math.max(8, Math.min(36, columns))
                    : Math.max(8, columns * 3);''')
patch(renderer,
'''        if (active) renderGroupGizmo(pose, lines, group);
''','''        // Gizmos have their own depth-free pass after the level is rendered.
''')
patch(renderer,
'''            renderHandles(pose, lines, surface, selected.handle());
            renderSurfaceGizmo(pose, lines, surface, selected.handle());
            renderSurfaceSides(pose, lines, surface);''',
'''            renderSurfaceSides(pose, lines, surface);
            // Handle spheres and axes are rendered after the world so walls
            // never hide the controls that are intentionally selectable through it.''')
# This second event is AFTER_LEVEL, not AFTER_SOLID_BLOCKS: later translucent
# world passes must not paint over the editor gizmos.
patch(renderer,
'''    private static void renderGroup(Minecraft minecraft, PoseStack pose,
''',
'''    @SubscribeEvent
    public static void renderEditorGizmos(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.player.isCreative()) return;
        Selection selection = TransformConstructionClientState.selection();
        if (selection == null) return;
        boolean editor = minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getOffGridTool())
                || minecraft.player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getSurfaceTool());
        if (!editor) return;
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer xray = buffers.getBuffer(TransformEditorRenderTypes.GIZMO_LINES);
        if (selection.type() == SelectionType.GROUP) {
            TransformGroup group = TransformConstructionClientState.group(
                    selection.id());
            if (group != null && group.origin().distanceToSqr(camera)
                    < MAX_RENDER_DISTANCE_SQR) renderGroupGizmo(pose, xray, group);
        } else {
            ConstructionSurface surface = TransformConstructionClientState.surface(
                    selection.id());
            if (surface != null) {
                double[] uv = switch (selection.handle()) {
                    case BOTTOM_START -> new double[]{0, 0};
                    case BOTTOM_END -> new double[]{1, 0};
                    case TOP_START -> new double[]{0, 1};
                    case TOP_END -> new double[]{1, 1};
                    case BOTTOM_EDGE -> new double[]{0.5, 0};
                    case TOP_EDGE -> new double[]{0.5, 1};
                    case START_EDGE -> new double[]{0, 0.5};
                    case END_EDGE -> new double[]{1, 0.5};
                    case CENTER -> new double[]{0.5, 0.5};
                };
                if (surface.gridPoint(uv[0], uv[1]).distanceToSqr(camera)
                        < MAX_RENDER_DISTANCE_SQR) {
                    renderHandles(pose, xray, surface, selection.handle());
                    renderSurfaceGizmo(pose, xray, surface, selection.handle());
                }
            }
        }
        buffers.endBatch(TransformEditorRenderTypes.GIZMO_LINES);
        pose.popPose();
    }

    private static void renderGroup(Minecraft minecraft, PoseStack pose,
''')
renderTypes=root/'facility/transform/client/TransformEditorRenderTypes.java'
assert not renderTypes.exists()
renderTypes.write_text('''package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import java.util.OptionalDouble;

/** Editor-only axes render over solid and translucent geometry, without
 * disabling the depth test or depth writes for any ordinary world pass. */
public final class TransformEditorRenderTypes extends RenderType {
    private TransformEditorRenderTypes() {
        super("editor_gizmos", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 256, false, false, () -> {}, () -> {});
    }

    public static final RenderType GIZMO_LINES = create(
            "scp_classified_directive_editor_gizmos",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 256, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.5D)))
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
}
''')
print('Editor preview density reduced; independent depth-free gizmo pass added; clearance digit optically centered.')