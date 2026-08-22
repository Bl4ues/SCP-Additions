
package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.bl4ues.scpclassifieddirective.facility.WetFloorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class WetFloorBlockEntityRenderer
        extends GeoBlockRenderer<WetFloorBlockEntity> {
    public WetFloorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new WetFloorGeoModel());
    }
}
