package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterDirectOpenClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Arms a specific Configuration Center section before its server snapshot opens. */
public record MachineConfigDirectOpenPacket(String section) {
    public MachineConfigDirectOpenPacket {
        section = section == null ? "" : section;
    }

    public static void encode(MachineConfigDirectOpenPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeUtf(message.section, 64);
    }

    public static MachineConfigDirectOpenPacket decode(FriendlyByteBuf buffer) {
        return new MachineConfigDirectOpenPacket(buffer.readUtf(64));
    }

    public static void handle(MachineConfigDirectOpenPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ConfigCenterDirectOpenClient.arm(message.section)));
        context.setPacketHandled(true);
    }
}
