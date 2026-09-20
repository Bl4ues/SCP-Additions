package com.bl4ues.scpclassifieddirective.facility.transform.client;

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
