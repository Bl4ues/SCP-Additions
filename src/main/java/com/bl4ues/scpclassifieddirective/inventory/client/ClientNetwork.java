package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.network.RequestInventorySyncPacket;

public final class ClientNetwork {

    private ClientNetwork() {
    }

    public static void requestInventorySync() {
        ModNetwork.CHANNEL.sendToServer(new RequestInventorySyncPacket());
    }
}
