package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Shader-safe placed renderer for the Facility Diagnostic Terminal. */
public final class SystemTerminalBlockEntityRenderer
        extends GeoBlockRenderer<SystemTerminalBlockEntity> {
    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation(
            "scp_additions", "textures/block/system_terminal.png");
    private static final ResourceLocation GENERATED_GLOW_TEXTURE =
            new ResourceLocation("scp_additions",
                    "textures/block/system_terminal_glowmask.png");
    private static boolean glowTextureRegistered;

    public SystemTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new SystemTerminalGeoModel());

        /*
         * GeckoLib 4.4.9 keeps getEmissiveResource private. Register the same
         * generated AutoGlowingTexture ourselves, then expose it through the
         * vanilla eyes pass so shader wrappers do not swallow the emissive
         * block-entity layer.
         */
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            protected RenderType getRenderType(
                    SystemTerminalBlockEntity animatable) {
                ensureGlowTextureRegistered();
                return RenderType.eyes(GENERATED_GLOW_TEXTURE);
            }
        });
    }

    private static void ensureGlowTextureRegistered() {
        if (glowTextureRegistered) return;

        Minecraft.getInstance().getTextureManager().register(
                GENERATED_GLOW_TEXTURE,
                new AutoGlowingTexture(BASE_TEXTURE,
                        GENERATED_GLOW_TEXTURE));
        glowTextureRegistered = true;
    }

    @Override
    public boolean shouldRenderOffScreen(
            SystemTerminalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public RenderType getRenderType(SystemTerminalBlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture, true);
    }
}
