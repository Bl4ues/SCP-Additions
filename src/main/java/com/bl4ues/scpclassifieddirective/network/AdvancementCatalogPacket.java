package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.client.PauseMenuNativePanelsClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/** Complete server-side advancement catalog for the custom Achievements panel. */
public final class AdvancementCatalogPacket {
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_ID_LENGTH = 256;
    private static final int MAX_COMPONENT_LENGTH = 16384;

    private final List<Entry> entries;

    public AdvancementCatalogPacket(List<Entry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static AdvancementCatalogPacket fromPlayer(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return new AdvancementCatalogPacket(List.of());
        }
        List<Entry> entries = new ArrayList<>();
        for (Advancement advancement : player.getServer().getAdvancements()
                .getAllAdvancements()) {
            DisplayInfo display = advancement.getDisplay();
            if (display == null) continue;
            Advancement root = advancement;
            while (root.getParent() != null) root = root.getParent();
            AdvancementProgress progress = player.getAdvancements()
                    .getOrStartProgress(advancement);
            entries.add(new Entry(advancement.getId().toString(),
                    root.getId().toString(), display.getTitle().copy(),
                    display.getDescription().copy(), display.getIcon().copy(),
                    display.getFrame(), display.isHidden(), progress.isDone()));
        }
        entries.sort(Comparator.comparing(Entry::rootId)
                .thenComparing(Entry::id));
        return new AdvancementCatalogPacket(entries);
    }

    public static void encode(AdvancementCatalogPacket message,
            FriendlyByteBuf buffer) {
        int size = Math.min(MAX_ENTRIES, message.entries.size());
        buffer.writeVarInt(size);
        for (int index = 0; index < size; index++) {
            Entry entry = message.entries.get(index);
            buffer.writeUtf(entry.id(), MAX_ID_LENGTH);
            buffer.writeUtf(entry.rootId(), MAX_ID_LENGTH);
            buffer.writeUtf(Component.Serializer.toJson(entry.title()),
                    MAX_COMPONENT_LENGTH);
            buffer.writeUtf(Component.Serializer.toJson(entry.description()),
                    MAX_COMPONENT_LENGTH);
            buffer.writeItem(entry.icon());
            buffer.writeVarInt(entry.frame().ordinal());
            buffer.writeBoolean(entry.hidden());
            buffer.writeBoolean(entry.done());
        }
    }

    public static AdvancementCatalogPacket decode(FriendlyByteBuf buffer) {
        int size = Mth.clamp(buffer.readVarInt(), 0, MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            String id = buffer.readUtf(MAX_ID_LENGTH);
            String rootId = buffer.readUtf(MAX_ID_LENGTH);
            Component title = parseComponent(buffer.readUtf(MAX_COMPONENT_LENGTH));
            Component description = parseComponent(
                    buffer.readUtf(MAX_COMPONENT_LENGTH));
            ItemStack icon = buffer.readItem();
            int frameId = buffer.readVarInt();
            FrameType[] frames = FrameType.values();
            FrameType frame = frameId >= 0 && frameId < frames.length
                    ? frames[frameId] : FrameType.TASK;
            boolean hidden = buffer.readBoolean();
            boolean done = buffer.readBoolean();
            entries.add(new Entry(id, rootId, title, description, icon,
                    frame, hidden, done));
        }
        return new AdvancementCatalogPacket(entries);
    }

    public static void handle(AdvancementCatalogPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PauseMenuNativePanelsClient
                        .replaceAdvancementCatalog(message.entries)));
        context.setPacketHandled(true);
    }

    private static Component parseComponent(String json) {
        Component parsed = Component.Serializer.fromJson(json);
        return parsed == null ? Component.empty() : parsed;
    }

    public record Entry(String id, String rootId, Component title,
            Component description, ItemStack icon, FrameType frame,
            boolean hidden, boolean done) {
        public Entry {
            if (id == null) id = "";
            if (rootId == null) rootId = id;
            if (title == null) title = Component.empty();
            if (description == null) description = Component.empty();
            if (icon == null) icon = ItemStack.EMPTY;
            if (frame == null) frame = FrameType.TASK;
        }
    }
}
