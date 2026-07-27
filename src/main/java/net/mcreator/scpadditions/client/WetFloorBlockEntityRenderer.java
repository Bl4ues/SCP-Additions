
package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.mcreator.scpadditions.facility.WetFloorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class WetFloorBlockEntityRenderer
        extends GeoBlockRenderer<WetFloorBlockEntity> {
    public WetFloorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new WetFloorGeoModel());
    }
}
