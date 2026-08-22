package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HazmatArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class HazmatArmorRenderer extends GeoArmorRenderer<HazmatArmorItem> {
    public HazmatArmorRenderer() {
        super(new HazmatArmorModel());
    }
}
