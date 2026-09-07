package com.bl4ues.scpclassifieddirective.client.scp079;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;

/** Routes SCP-079's Inventory binding between boot, map, and leave-role flows. */
public final class Scp079LocalHostInputClient {
    private Scp079LocalHostInputClient() {
    }

    public static void handleInventoryKey(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START
                || !Scp079PlayableClient.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;

        boolean requested = false;
        while (minecraft.options.keyInventory.consumeClick()) requested = true;
        if (!requested) return;

        if (minecraft.options.keyShift.isDown()) {
            Scp079LeaveRoleScreen.open();
            return;
        }
        if (!Scp079PlayableClient.networkAvailable()) return;

        if (!Scp079PlayableClient.cameraMode()
                && !Scp079BootSequenceClient.completed()) {
            Scp079BootSequenceClient.open();
            return;
        }
        Scp079FacilityMapScreen.open();
    }
}
