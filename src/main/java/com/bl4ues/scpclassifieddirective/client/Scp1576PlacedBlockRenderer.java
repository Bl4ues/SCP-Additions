package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.bl4ues.scpclassifieddirective.scp1576.Scp1576PlacedBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class Scp1576PlacedBlockRenderer
        extends GeoBlockRenderer<Scp1576PlacedBlockEntity> {
    public Scp1576PlacedBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new Scp1576PlacedGeoModel());
    }
}
