#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
changed: list[str] = []


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old != text:
        path.write_text(text, encoding="utf-8")
        changed.append(rel)


def replace_once(rel: str, old: str, new: str) -> None:
    text = read(rel)
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"Expected source fragment not found in {rel}: {old[:120]!r}")
    write(rel, text.replace(old, new, 1))


def remove_once(rel: str, stale: str) -> None:
    text = read(rel)
    if stale not in text:
        return
    write(rel, text.replace(stale, "", 1))


replace_once(
    "src/main/java/com/bl4ues/scpinventory/config/ScpInventoryConfig.java",
    "    private static boolean loaded = false;\n",
    "    private static boolean loaded = false;\n"
    "    private static volatile boolean serverSnapshotActive = false;\n",
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/config/ScpInventoryConfig.java",
    '''    public static void reload() {
        loaded = false;
        load();
    }
''',
    '''    public static synchronized void applyServerSnapshot(
            List<String> serverItemRules,
            List<String> serverItemEffects,
            List<String> serverCodexDocuments,
            List<String> serverHiddenStatusEffects,
            List<String> serverScp173Targets) {
        itemRules = Collections.unmodifiableList(new ArrayList<>(serverItemRules));
        itemEffects = Collections.unmodifiableList(new ArrayList<>(serverItemEffects));
        codexDocuments = Collections.unmodifiableList(new ArrayList<>(serverCodexDocuments));
        hiddenStatusEffects = Collections.unmodifiableList(new ArrayList<>(serverHiddenStatusEffects));
        scp173Targets = Collections.unmodifiableList(new ArrayList<>(serverScp173Targets));
        serverSnapshotActive = true;
        loaded = true;
    }

    public static synchronized void clearServerSnapshot() {
        if (!serverSnapshotActive) {
            return;
        }
        serverSnapshotActive = false;
        loaded = false;
        load();
    }

    public static void reload() {
        if (serverSnapshotActive) {
            return;
        }
        loaded = false;
        load();
    }
''',
)

replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextInteractionRegistry.java",
    "    private static boolean loaded = false;\n",
    "    private static boolean loaded = false;\n"
    "    private static volatile String serverSnapshotJson;\n",
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextInteractionRegistry.java",
    '''    public static void reload() {
        loaded = false;
        load();
    }
''',
    '''    public static synchronized void applyServerSnapshot(String json) {
        serverSnapshotJson = json == null ? "" : json;
        loaded = false;
        load();
    }

    public static synchronized void clearServerSnapshot() {
        serverSnapshotJson = null;
        loaded = false;
        load();
    }

    public static void reload() {
        loaded = false;
        load();
    }
''',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextInteractionRegistry.java",
    '''            File file = ContextConfigManager.ensureConfigFile();

            JsonObject root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
''',
    '''            String snapshot = serverSnapshotJson;
            JsonObject root;
            if (snapshot != null && !snapshot.isBlank()) {
                root = JsonParser.parseString(snapshot).getAsJsonObject();
            } else {
                File file = ContextConfigManager.ensureConfigFile();
                root = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            }
''',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextInteractionRegistry.java",
    '''        if (type.isEmpty() || idText.isEmpty()) {
            return null;
        }
''',
    '''        if (type.isEmpty() || idText.isEmpty()) {
            return null;
        }
        // The old default 1499 entity rule resolves to a vanilla pig in the
        // external gas-mask mod. Ignore it even in pre-hotfix user configs.
        if ("entity".equals(type) && "gas_mask:scp_1499".equals(idText)) {
            return null;
        }
''',
)

replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java",
    '''    static File ensureConfigFile() {
''',
    '''    public static String readConfigJson() {
        try {
            return Files.readString(ensureConfigFile().toPath(),
                    StandardCharsets.UTF_8);
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error(
                    "Failed to read context interaction configuration",
                    exception);
            return "{\\"interactions\\":[]}";
        }
    }

    static File ensureConfigFile() {
''',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java",
    "import net.minecraftforge.registries.ForgeRegistries;\n",
    "import net.minecraftforge.registries.ForgeRegistries;\n"
    "import net.minecraftforge.server.ServerLifecycleHooks;\n",
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java",
    '''            ConfigFilePersistence.writeWithBackup(file.toPath(),
                    GSON.toJson(root) + System.lineSeparator());
''',
    '''            ConfigFilePersistence.writeWithBackup(file.toPath(),
                    GSON.toJson(root) + System.lineSeparator());
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ModNetwork.syncServerConfig(server.getPlayerList().getPlayers());
            }
''',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java",
    '''    public static int reload(CommandSourceStack source) {
        ContextInteractionRegistry.reload();
        source.sendSuccess(() -> Component.literal("[SCP Inventory] Context interactions reloaded.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
''',
    '''    public static int reload(CommandSourceStack source) {
        ContextInteractionRegistry.reload();
        ModNetwork.syncServerConfig(source.getServer().getPlayerList().getPlayers());
        source.sendSuccess(() -> Component.literal("[SCP Inventory] Context interactions reloaded.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
''',
)

replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextEntityConfigManager.java",
    "import net.minecraftforge.registries.ForgeRegistries;\n",
    "import net.minecraftforge.registries.ForgeRegistries;\n"
    "import net.minecraftforge.server.ServerLifecycleHooks;\n",
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextEntityConfigManager.java",
    '''            ConfigFilePersistence.writeWithBackup(file.toPath(),
                    GSON.toJson(root) + System.lineSeparator());
''',
    '''            ConfigFilePersistence.writeWithBackup(file.toPath(),
                    GSON.toJson(root) + System.lineSeparator());
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ModNetwork.syncServerConfig(server.getPlayerList().getPlayers());
            }
''',
)

write(
    "src/main/java/com/bl4ues/scpinventory/network/ServerConfigSyncPacket.java",
    '''package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Complete host-authoritative SCP Inventory configuration snapshot. */
public final class ServerConfigSyncPacket {
    private static final int MAX_LIST_ENTRIES = 16384;
    private static final int MAX_STRING_LENGTH = 1_000_000;

    private final List<String> itemRules;
    private final List<String> itemEffects;
    private final List<String> codexDocuments;
    private final List<String> hiddenStatusEffects;
    private final List<String> scp173Targets;
    private final String contextJson;

    public ServerConfigSyncPacket(
            List<String> itemRules,
            List<String> itemEffects,
            List<String> codexDocuments,
            List<String> hiddenStatusEffects,
            List<String> scp173Targets,
            String contextJson) {
        this.itemRules = List.copyOf(itemRules);
        this.itemEffects = List.copyOf(itemEffects);
        this.codexDocuments = List.copyOf(codexDocuments);
        this.hiddenStatusEffects = List.copyOf(hiddenStatusEffects);
        this.scp173Targets = List.copyOf(scp173Targets);
        this.contextJson = contextJson == null
                ? "{\\"interactions\\":[]}" : contextJson;
    }

    public static void encode(ServerConfigSyncPacket message,
            FriendlyByteBuf buffer) {
        writeStrings(buffer, message.itemRules);
        writeStrings(buffer, message.itemEffects);
        writeStrings(buffer, message.codexDocuments);
        writeStrings(buffer, message.hiddenStatusEffects);
        writeStrings(buffer, message.scp173Targets);
        buffer.writeUtf(message.contextJson, MAX_STRING_LENGTH);
    }

    public static ServerConfigSyncPacket decode(FriendlyByteBuf buffer) {
        return new ServerConfigSyncPacket(
                readStrings(buffer),
                readStrings(buffer),
                readStrings(buffer),
                readStrings(buffer),
                readStrings(buffer),
                buffer.readUtf(MAX_STRING_LENGTH));
    }

    public static void handle(ServerConfigSyncPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ScpInventoryConfig.applyServerSnapshot(
                    message.itemRules,
                    message.itemEffects,
                    message.codexDocuments,
                    message.hiddenStatusEffects,
                    message.scp173Targets);
            ContextInteractionRegistry.applyServerSnapshot(message.contextJson);
        });
        contextSupplier.get().setPacketHandled(true);
    }

    private static void writeStrings(FriendlyByteBuf buffer,
            List<String> values) {
        buffer.writeVarInt(values.size());
        for (String value : values) {
            buffer.writeUtf(value == null ? "" : value, MAX_STRING_LENGTH);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_LIST_ENTRIES) {
            throw new IllegalArgumentException(
                    "Invalid SCP Inventory config list size: " + size);
        }
        List<String> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            values.add(buffer.readUtf(MAX_STRING_LENGTH));
        }
        return values;
    }
}
''',
)

replace_once(
    "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java",
    '    private static final String PROTOCOL_VERSION = "5";\n',
    '    private static final String PROTOCOL_VERSION = "6";\n',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java",
    '''        CHANNEL.registerMessage(id++, ItemConfigDeletePacket.class, ItemConfigDeletePacket::encode, ItemConfigDeletePacket::decode, ItemConfigDeletePacket::handle);
        CHANNEL.registerMessage(id++, CraftingStateSyncPacket.class, CraftingStateSyncPacket::encode, CraftingStateSyncPacket::decode, CraftingStateSyncPacket::handle);
''',
    '''        CHANNEL.registerMessage(id++, ItemConfigDeletePacket.class, ItemConfigDeletePacket::encode, ItemConfigDeletePacket::decode, ItemConfigDeletePacket::handle);
        CHANNEL.registerMessage(id++, ServerConfigSyncPacket.class, ServerConfigSyncPacket::encode, ServerConfigSyncPacket::decode, ServerConfigSyncPacket::handle);
        CHANNEL.registerMessage(id++, CraftingStateSyncPacket.class, CraftingStateSyncPacket::encode, CraftingStateSyncPacket::decode, CraftingStateSyncPacket::handle);
''',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java",
    '''    public static void showInventoryFull(ServerPlayer player) {
''',
    '''    public static void syncServerConfig(ServerPlayer player) {
        if (player == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ServerConfigSyncPacket(
                        com.bl4ues.scpinventory.config.ScpInventoryConfig.itemRules(),
                        com.bl4ues.scpinventory.config.ScpInventoryConfig.itemEffects(),
                        com.bl4ues.scpinventory.config.ScpInventoryConfig.codexDocuments(),
                        com.bl4ues.scpinventory.config.ScpInventoryConfig.hiddenStatusEffects(),
                        com.bl4ues.scpinventory.config.ScpInventoryConfig.scp173Targets(),
                        com.bl4ues.scpinventory.context.ContextConfigManager.readConfigJson()));
    }

    public static void syncServerConfig(Iterable<ServerPlayer> players) {
        if (players == null) return;
        for (ServerPlayer player : players) {
            syncServerConfig(player);
        }
    }

    public static void showInventoryFull(ServerPlayer player) {
''',
)

replace_once(
    "src/main/java/com/bl4ues/scpinventory/events/InventoryModuleStateEvents.java",
    '''        ModNetwork.syncModuleState(player);
        updateDisabledState(player);
''',
    '''        ModNetwork.syncModuleState(player);
        ModNetwork.syncServerConfig(player);
        updateDisabledState(player);
''',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/config/ItemConfigManager.java",
    '''        ScpInventoryConfig.reload();
        player.sendSystemMessage(Component.literal("[SCP Inventory] Saved item rule for ").withStyle(ChatFormatting.GREEN)
''',
    '''        ScpInventoryConfig.reload();
        ModNetwork.syncServerConfig(player.server.getPlayerList().getPlayers());
        player.sendSystemMessage(Component.literal("[SCP Inventory] Saved item rule for ").withStyle(ChatFormatting.GREEN)
''',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/config/ItemConfigManager.java",
    '''        ScpInventoryConfig.reload();
        player.sendSystemMessage(Component.literal("[SCP Inventory] " + (removed ? "Removed" : "No rule for") + " item rule ").withStyle(removed ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
''',
    '''        ScpInventoryConfig.reload();
        ModNetwork.syncServerConfig(player.server.getPlayerList().getPlayers());
        player.sendSystemMessage(Component.literal("[SCP Inventory] " + (removed ? "Removed" : "No rule for") + " item rule ").withStyle(removed ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
''',
)
remove_once(
    "src/main/java/com/bl4ues/scpinventory/network/ItemConfigSavePacket.java",
    "            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ItemConfigReloadPacket());\n",
)
remove_once(
    "src/main/java/com/bl4ues/scpinventory/network/ItemConfigDeletePacket.java",
    "            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ItemConfigReloadPacket());\n",
)

replace_once(
    "src/main/java/com/bl4ues/scpinventory/client/ClientGameplayEvents.java",
    "import net.minecraftforge.event.TickEvent;\n",
    "import net.minecraftforge.event.TickEvent;\n"
    "import net.minecraftforge.client.event.ClientPlayerNetworkEvent;\n"
    "import com.bl4ues.scpinventory.config.ScpInventoryConfig;\n"
    "import com.bl4ues.scpinventory.context.ContextInteractionRegistry;\n",
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/client/ClientGameplayEvents.java",
    '''    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
''',
    '''    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ScpInventoryConfig.clearServerSnapshot();
        ContextInteractionRegistry.clearServerSnapshot();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
''',
)

replace_once(
    "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsReloadCommand.java",
    '''            ModNetwork.syncModuleState(source.getServer().getPlayerList().getPlayers());
            source.sendSuccess(() -> Component.literal("SCP Additions configurations reloaded successfully.")
''',
    '''            ModNetwork.syncModuleState(source.getServer().getPlayerList().getPlayers());
            ModNetwork.syncServerConfig(source.getServer().getPlayerList().getPlayers());
            source.sendSuccess(() -> Component.literal("SCP Additions configurations reloaded successfully.")
''',
)

print(f"Multiplayer config hotfix changed {len(changed)} files")
for rel in changed:
    print(rel)
