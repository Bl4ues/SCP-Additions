package com.bl4ues.scpclassifieddirective.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Dedicated triangle-list render types for the Alarm projected-light mesh.
 *
 * <p>The vanilla entity translucent/eyes render types are QUAD based. Feeding
 * clipped triangles through them by duplicating the last vertex creates a
 * degenerate second triangle for every primitive. Vanilla usually tolerates
 * that, but shader packs are free to process the quad/degenerate triangle
 * differently. At the projection boundary that manifested as the persistent
 * bright blades and detached triangular fragments which survived every geometry
 * rewrite.</p>
 *
 * <p>The projector is intrinsically a triangle mesh, so render it as one.</p>
 */
public final class AlarmProjectionRenderTypes {
    private static final Map<ResourceLocation, RenderType> WASH = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> BLOOM = new HashMap<>();

    private AlarmProjectionRenderTypes() {
    }

    public static RenderType wash(ResourceLocation texture) {
        return WASH.computeIfAbsent(texture, Types::wash);
    }

    public static RenderType bloom(ResourceLocation texture) {
        return BLOOM.computeIfAbsent(texture, Types::bloom);
    }

    /**
     * Extending RenderStateShard gives access to vanilla's protected render
     * state constants without duplicating GL setup manually.
     */
    private static final class Types extends RenderStateShard {
        private Types() {
            super("scp_cd_alarm_projection_states", () -> { }, () -> { });
        }

        private static RenderType wash(ResourceLocation texture) {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(
                            texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true);
            return RenderType.create(
                    "scp_cd_alarm_projection_wash",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.TRIANGLES,
                    2048,
                    false,
                    true,
                    state);
        }

        private static RenderType bloom(ResourceLocation texture) {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(
                            texture, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false);
            return RenderType.create(
                    "scp_cd_alarm_projection_bloom",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.TRIANGLES,
                    2048,
                    false,
                    true,
                    state);
        }
    }
}
