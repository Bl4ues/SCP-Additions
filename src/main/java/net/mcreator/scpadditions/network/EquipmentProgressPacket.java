package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.bl4ues.scpadditions.compat.DistExecutor;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import net.mcreator.scpadditions.client.EquipmentProgressOverlay;
import net.mcreator.scpadditions.client.HazmatAudioClient;

import java.util.function.Supplier;

/** Server-to-client control packet for the reusable timed-equipment bar. */
public final class EquipmentProgressPacket {
    private static final int BEGIN = 0;
    private static final int SYNC = 1;
    private static final int COMPLETE = 2;
    private static final int CANCEL = 3;

    private final int action;
    private final int elapsedTicks;
    private final int durationTicks;

    private EquipmentProgressPacket(int action, int elapsedTicks,
            int durationTicks) {
        this.action = action;
        this.elapsedTicks = elapsedTicks;
        this.durationTicks = durationTicks;
    }

    public static EquipmentProgressPacket begin(int durationTicks) {
        return new EquipmentProgressPacket(BEGIN, 0, durationTicks);
    }

    public static EquipmentProgressPacket sync(int elapsedTicks,
            int durationTicks) {
        return new EquipmentProgressPacket(SYNC, elapsedTicks, durationTicks);
    }

    public static EquipmentProgressPacket complete() {
        return new EquipmentProgressPacket(COMPLETE, 0, 1);
    }

    public static EquipmentProgressPacket cancel() {
        return new EquipmentProgressPacket(CANCEL, 0, 1);
    }

    public static void encode(EquipmentProgressPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.action);
        buffer.writeVarInt(Math.max(0, message.elapsedTicks));
        buffer.writeVarInt(Math.max(1, message.durationTicks));
    }

    public static EquipmentProgressPacket decode(FriendlyByteBuf buffer) {
        int action = Math.max(BEGIN, Math.min(CANCEL, buffer.readVarInt()));
        int elapsed = Math.max(0, buffer.readVarInt());
        int duration = Math.max(1, buffer.readVarInt());
        return new EquipmentProgressPacket(action, elapsed, duration);
    }

    public static void handle(EquipmentProgressPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> applyClient(message)));
        context.setPacketHandled(true);
    }

    private static void applyClient(EquipmentProgressPacket message) {
        switch (message.action) {
            case BEGIN -> {
                EquipmentProgressOverlay.begin(message.durationTicks);
                HazmatAudioClient.beginForDuration(message.durationTicks);
            }
            case SYNC -> EquipmentProgressOverlay.syncProgress(
                    message.elapsedTicks, message.durationTicks);
            case COMPLETE -> {
                EquipmentProgressOverlay.complete();
                HazmatAudioClient.completeAction();
            }
            case CANCEL -> {
                EquipmentProgressOverlay.cancel();
                HazmatAudioClient.cancelAction();
            }
            default -> {
                EquipmentProgressOverlay.cancel();
                HazmatAudioClient.cancelAction();
            }
        }
    }
}
