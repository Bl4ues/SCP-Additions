package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.item.SystemTerminalItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Double-sided item renderer for the SCiPNET terminal. */
public final class SystemTerminalItemRenderer
        extends GeoItemRenderer<SystemTerminalItem> {
    public SystemTerminalItemRenderer() {
        super(new SystemTerminalItemGeoModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(SystemTerminalItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
