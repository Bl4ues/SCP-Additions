package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp079EnergyClientState;
import net.mcreator.scpadditions.facility.Scp079DecisionLog.DecisionOutcome;
import net.mcreator.scpadditions.facility.Scp079DecisionLog.DecisionType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server-to-client snapshot of the event-driven SCP-079 decision feed. */
public final class Scp079DecisionPacket {
    private static final int MAX_ENTRIES = 12;
    private static final int MAX_DIMENSION_LENGTH = 96;
    private static final int MAX_CONTEXT_LENGTH = 160;

    private final List<DecisionEntry> entries;

    public Scp079DecisionPacket(List<DecisionEntry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static void encode(Scp079DecisionPacket message,
            FriendlyByteBuf buffer) {
        int size = Math.min(MAX_ENTRIES, message.entries.size());
        buffer.writeVarInt(size);
        for (int index = 0; index < size; index++) {
            DecisionEntry entry = message.entries.get(index);
            buffer.writeVarLong(Math.max(0L, entry.sequence()));
            buffer.writeVarInt(entry.type().ordinal());
            buffer.writeVarInt(entry.outcome().ordinal());
            buffer.writeBlockPos(entry.pos());
            buffer.writeUtf(entry.dimension(), MAX_DIMENSION_LENGTH);
            buffer.writeUtf(entry.context(), MAX_CONTEXT_LENGTH);
            buffer.writeFloat(Math.max(0.0F, entry.cost()));
            buffer.writeVarInt(Mth.clamp(entry.ageTicks(), 0,
                    net.mcreator.scpadditions.facility.Scp079DecisionLog
                            .CLIENT_LIFETIME_TICKS));
        }
    }

    public static Scp079DecisionPacket decode(FriendlyByteBuf buffer) {
        int size = Mth.clamp(buffer.readVarInt(), 0, MAX_ENTRIES);
        List<DecisionEntry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            long sequence = Math.max(0L, buffer.readVarLong());
            DecisionType type = enumById(DecisionType.values(),
                    buffer.readVarInt(), DecisionType.ABORTED_ACTION);
            DecisionOutcome outcome = enumById(DecisionOutcome.values(),
                    buffer.readVarInt(), DecisionOutcome.ABORTED);
            BlockPos pos = buffer.readBlockPos();
            String dimension = buffer.readUtf(MAX_DIMENSION_LENGTH);
            String context = buffer.readUtf(MAX_CONTEXT_LENGTH);
            float cost = Math.max(0.0F, buffer.readFloat());
            int ageTicks = Mth.clamp(buffer.readVarInt(), 0,
                    net.mcreator.scpadditions.facility.Scp079DecisionLog
                            .CLIENT_LIFETIME_TICKS);
            entries.add(new DecisionEntry(sequence, type, outcome, pos,
                    dimension, context, cost, ageTicks));
        }
        return new Scp079DecisionPacket(entries);
    }

    public static void handle(Scp079DecisionPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp079EnergyClientState.replaceDecisions(
                        message.entries)));
        context.setPacketHandled(true);
    }

    private static <T> T enumById(T[] values, int id, T fallback) {
        return id >= 0 && id < values.length ? values[id] : fallback;
    }

    public record DecisionEntry(long sequence, DecisionType type,
            DecisionOutcome outcome, BlockPos pos, String dimension,
            String context, float cost, int ageTicks) {
        public DecisionEntry {
            if (type == null) type = DecisionType.ABORTED_ACTION;
            if (outcome == null) outcome = DecisionOutcome.ABORTED;
            if (pos == null) pos = BlockPos.ZERO;
            if (dimension == null) dimension = "";
            if (context == null) context = "";
            cost = Math.max(0.0F, cost);
            ageTicks = Math.max(0, ageTicks);
        }
    }
}
