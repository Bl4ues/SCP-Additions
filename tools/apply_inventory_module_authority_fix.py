from pathlib import Path


def read(path):
    return Path(path).read_text(encoding="utf-8")


def write(path, text):
    Path(path).write_text(text, encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def add_import(text, import_line, marker):
    if import_line in text:
        return text
    return replace_once(text, marker, marker + import_line, import_line.strip())


# Common runtime state: safe to load on either physical side.
write("src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java", '''package com.bl4ues.scpinventory.config;

import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

/**
 * Client view of the server-authoritative inventory module state.
 *
 * Integrated singleplayer can fall back to the local module configuration,
 * while dedicated-server clients use the value synchronized at login/reload.
 */
public final class InventoryModuleRuntimeState {
    private static volatile Boolean serverEnabled;

    private InventoryModuleRuntimeState() {
    }

    public static boolean isEnabledForClient() {
        Boolean synced = serverEnabled;
        return synced != null ? synced : ScpAdditionsModulesConfig.get().inventory.enabled;
    }

    public static void updateFromServer(boolean enabled) {
        serverEnabled = enabled;
    }

    public static void clearServerState() {
        serverEnabled = null;
    }
}
''')

write("src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java", '''package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record InventoryModuleStatePacket(boolean enabled) {
    public static void encode(InventoryModuleStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.enabled);
    }

    public static InventoryModuleStatePacket decode(FriendlyByteBuf buffer) {
        return new InventoryModuleStatePacket(buffer.readBoolean());
    }

    public static void handle(InventoryModuleStatePacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> InventoryModuleRuntimeState.updateFromServer(message.enabled));
        context.setPacketHandled(true);
    }
}
''')

write("src/main/java/com/bl4ues/scpinventory/events/InventoryModuleStateEvents.java", '''package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions")
public final class InventoryModuleStateEvents {
    private InventoryModuleStateEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.syncModuleState(player);
        }
    }
}
''')

# Network registration and centralized no-op behavior while disabled.
path = "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java"
text = read(path)
text = add_import(text,
                  "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                  "import net.mcreator.scpadditions.config.ui.ConfigCenterNetwork;\n")
text = text.replace('private static final String PROTOCOL_VERSION = "3";',
                    'private static final String PROTOCOL_VERSION = "4";')
text = replace_once(text,
'''        CHANNEL.registerMessage(id++, MainUseActionPacket.class, MainUseActionPacket::encode, MainUseActionPacket::decode, MainUseActionPacket::handle);
        CHANNEL.registerMessage(id++, ContextInteractPacket.class, ContextInteractPacket::encode, ContextInteractPacket::decode, ContextInteractPacket::handle);
''',
'''        CHANNEL.registerMessage(id++, MainUseActionPacket.class, MainUseActionPacket::encode, MainUseActionPacket::decode, MainUseActionPacket::handle);
        CHANNEL.registerMessage(id++, InventoryModuleStatePacket.class, InventoryModuleStatePacket::encode, InventoryModuleStatePacket::decode, InventoryModuleStatePacket::handle);
        CHANNEL.registerMessage(id++, ContextInteractPacket.class, ContextInteractPacket::encode, ContextInteractPacket::decode, ContextInteractPacket::handle);
''', "register module-state packet")
text = replace_once(text,
'''    public static void syncTo(ServerPlayer player, IScpInventory inventory) {
        if (player != null && inventory != null) {
''',
'''    public static void syncTo(ServerPlayer player, IScpInventory inventory) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
        if (player != null && inventory != null) {
''', "syncTo module guard")
text = replace_once(text,
'''    public static void showInventoryFull(ServerPlayer player) {
        if (player != null && !player.isSpectator()) {
''',
'''    public static void showInventoryFull(ServerPlayer player) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
        if (player != null && !player.isSpectator()) {
''', "inventory full module guard")
text = replace_once(text,
'''    public static void activateUsableItem(ServerPlayer player, int hotbarSlot,
            int sourceSlot, boolean continuousUse, ItemStack stack) {
        if (player != null && !player.isCreative() && !player.isSpectator()) {
''',
'''    public static void activateUsableItem(ServerPlayer player, int hotbarSlot,
            int sourceSlot, boolean continuousUse, ItemStack stack) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
        if (player != null && !player.isCreative() && !player.isSpectator()) {
''', "usable activation network guard")
text = replace_once(text,
'''    public static void showInventoryFull(ServerPlayer player) {
''',
'''    public static void syncModuleState(ServerPlayer player) {
        if (player == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new InventoryModuleStatePacket(ScpAdditionsModulesConfig.get().inventory.enabled));
    }

    public static void syncModuleState(Iterable<ServerPlayer> players) {
        if (players == null) return;
        for (ServerPlayer player : players) syncModuleState(player);
    }

    public static void showInventoryFull(ServerPlayer player) {
''', "module state helpers")
write(path, text)

# Server response to an inventory-open request is authoritative.
path = "src/main/java/com/bl4ues/scpinventory/network/RequestInventorySyncPacket.java"
text = read(path)
text = add_import(text,
                  "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                  "import net.minecraftforge.network.NetworkEvent;\n")
text = replace_once(text,
'''            if (player == null) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory ->
''',
'''            if (player == null) {
                return;
            }
            ModNetwork.syncModuleState(player);
            if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory ->
''', "request sync authority")
write(path, text)

# Every C2S gameplay action becomes inert while the server module is disabled.
packet_files = [
    "InventoryActionPacket.java",
    "EquipmentActionPacket.java",
    "KeyActionPacket.java",
    "DocumentActionPacket.java",
    "InventoryMovePacket.java",
    "PickupItemPacket.java",
    "UsableSessionReturnPacket.java",
    "UsableSessionDropPacket.java",
    "MainUseActionPacket.java",
]
for filename in packet_files:
    path = f"src/main/java/com/bl4ues/scpinventory/network/{filename}"
    text = read(path)
    text = add_import(text,
                      "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                      "import net.minecraftforge.network.NetworkEvent;\n")
    marker = "        ctx.get().enqueueWork(() -> {\n"
    if marker not in text:
        raise RuntimeError(f"{filename}: expected ctx.get().enqueueWork handler")
    text = text.replace(marker, marker
            + "            if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;\n", 1)
    write(path, text)

# Client key and screen use the synchronized state rather than a separate local
# modules.json when connected to a dedicated server.
path = "src/main/java/com/bl4ues/scpinventory/client/ClientKeyHandler.java"
text = read(path)
text = text.replace("import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                    "import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;\n")
text = text.replace("ScpAdditionsModulesConfig.get().inventory.enabled",
                    "InventoryModuleRuntimeState.isEnabledForClient()")
write(path, text)

path = "src/main/java/com/bl4ues/scpinventory/client/gui/ScpInventoryScreen.java"
text = read(path)
text = add_import(text,
                  "import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;\n",
                  "import com.bl4ues.scpinventory.capability.ScpInventoryCapability;\n")
text = replace_once(text,
'''    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
''',
'''    @Override
    public void tick() {
        super.tick();
        if (!InventoryModuleRuntimeState.isEnabledForClient()) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
''', "close disabled inventory screen")
write(path, text)

# Broadcast module state after both reload paths.
path = "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsReloadCommand.java"
text = read(path)
text = add_import(text,
                  "import com.bl4ues.scpinventory.network.ModNetwork;\n",
                  "import com.bl4ues.scpinventory.config.ScpInventoryConfig;\n")
text = replace_once(text,
'''            ContextInteractionRegistry.reload();
            source.sendSuccess(() -> Component.literal("SCP Additions configurations reloaded successfully.")
''',
'''            ContextInteractionRegistry.reload();
            ModNetwork.syncModuleState(source.getServer().getPlayerList().getPlayers());
            source.sendSuccess(() -> Component.literal("SCP Additions configurations reloaded successfully.")
''', "reload module broadcast")
write(path, text)

path = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterNetwork.java"
text = read(path)
text = replace_once(text,
'''                ConfigCenterService.SaveResult result = ConfigCenterService.saveBatch(player, message.changes);
                String snapshot = result.success() ? GSON.toJson(result.snapshot()) : "";
''',
'''                ConfigCenterService.SaveResult result = ConfigCenterService.saveBatch(player, message.changes);
                if (result.success()) {
                    com.bl4ues.scpinventory.network.ModNetwork.syncModuleState(
                            player.server.getPlayerList().getPlayers());
                }
                String snapshot = result.success() ? GSON.toJson(result.snapshot()) : "";
''', "config-center module broadcast")
write(path, text)

# Changelog: refine the already-added fix rather than creating another section.
path = "CHANGELOG.md"
text = read(path)
text = replace_once(text,
'''- Fixed `inventory.enabled: false` still allowing background tick handlers and packets to move items into the SCP Inventory;
''',
'''- Fixed `inventory.enabled: false` still allowing background tick handlers and gameplay packets to move items into the SCP Inventory; the server now synchronizes this module state to clients, blocks stale actions, and closes the custom screen when disabled;
''', "changelog inventory authority")
write(path, text)

# Static assertions before the workflow build.
for filename in packet_files:
    source = read(f"src/main/java/com/bl4ues/scpinventory/network/{filename}")
    if "if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;" not in source:
        raise RuntimeError(f"missing module guard in {filename}")
