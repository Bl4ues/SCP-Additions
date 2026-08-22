package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.facility.ArchivistsChairBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ArchivistsChairItemRenderer
        extends GeoItemRenderer<ArchivistsChairBlockItem> {
    public ArchivistsChairItemRenderer() {
        super(new ArchivistsChairItemModel());
    }
}
