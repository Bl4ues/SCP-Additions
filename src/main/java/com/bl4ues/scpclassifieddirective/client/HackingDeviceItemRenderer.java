package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class HackingDeviceItemRenderer
        extends GeoItemRenderer<HackingDeviceItem> {
    public HackingDeviceItemRenderer() {
        super(new HackingDeviceGeoModel());
    }
}
