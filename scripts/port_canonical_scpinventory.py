from pathlib import Path
import shutil
import re

ROOT = Path(__file__).resolve().parents[1]
CANONICAL = ROOT / "migration-reference/scpinventory-final/java/com/bl4ues/scpinventory"
TARGET = ROOT / "src/main/java/com/bl4ues/scpinventory"

PATTERNS = [
    "capability/*.java",
    "client/ClientInventoryBridge.java",
    "client/ClientKeyHandler.java",
    "client/ClientNetwork.java",
    "client/ClientPacketHandlers.java",
    "client/CoinCounterClient.java",
    "client/ContextConfigClientEvents.java",
    "client/ContextConfigClientHandler.java",
    "client/ContextPromptClickGuard.java",
    "client/ContextPromptClient.java",
    "client/ContextPromptIcons.java",
    "client/InventoryFullOverlay.java",
    "client/ItemConfigClientHandler.java",
    "client/Keybinds.java",
    "client/PickupPromptClient.java",
    "client/PickupPromptWorldEvents.java",
    "client/ScpFonts.java",
    "client/ShiftClickEquipHandler.java",
    "client/UsableHotbarSessionClient.java",
    "client/gui/**/*.java",
    "commands/*.java",
    "config/*.java",
    "context/*.java",
    "event/ScpInventoryMaintenanceEvents.java",
    "event/ScpInventoryUsableOutputGuardEvents.java",
    "events/BlockPickupHandler.java",
    "events/CapabilityEvents.java",
    "events/VanillaMirrorSyncHandler.java",
    "item/*.java",
    "network/ContextConfigDeletePacket.java",
    "network/ContextConfigOpenPacket.java",
    "network/ContextConfigReloadPacket.java",
    "network/ContextConfigSavePacket.java",
    "network/ContextConfigSelectPacket.java",
    "network/ContextInteractPacket.java",
    "network/DocumentActionPacket.java",
    "network/EquipmentActionPacket.java",
    "network/InventoryActionPacket.java",
    "network/InventoryFullPacket.java",
    "network/InventoryMovePacket.java",
    "network/ItemConfigDeletePacket.java",
    "network/ItemConfigOpenPacket.java",
    "network/ItemConfigOpenRequestPacket.java",
    "network/ItemConfigReloadPacket.java",
    "network/ItemConfigSavePacket.java",
    "network/KeyActionPacket.java",
    "network/MainUseActionPacket.java",
    "network/PickupItemPacket.java",
    "network/RequestInventorySyncPacket.java",
    "network/SyncInventoryPacket.java",
    "network/UsableSessionDropPacket.java",
    "network/UsableSessionReturnPacket.java",
    "network/UseHotbarItemPacket.java",
]


def copy_canonical_sources() -> None:
    if not CANONICAL.is_dir():
        raise RuntimeError(f"Canonical source directory is missing: {CANONICAL}")
    if TARGET.exists():
        shutil.rmtree(TARGET)
    TARGET.mkdir(parents=True, exist_ok=True)

    copied: set[Path] = set()
    for pattern in PATTERNS:
        for source in CANONICAL.glob(pattern):
            if not source.is_file():
                continue
            relative = source.relative_to(CANONICAL)
            if relative in copied:
                continue
            destination = TARGET / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
            copied.add(relative)

    if not copied:
        raise RuntimeError("No canonical files were selected")
    print(f"Copied {len(copied)} canonical Java files")


def write_shims() -> None:
    (TARGET / "ScpInventoryMod.java").write_text(
        '''package com.bl4ues.scpinventory;

import org.apache.logging.log4j.Logger;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Resource namespace shim. This is not a second Forge mod entrypoint. */
public final class ScpInventoryMod {
    public static final String MODID = "scpinventory";
    public static final Logger LOGGER = ScpAdditionsMod.LOGGER;

    private ScpInventoryMod() {
    }
}
''',
        encoding="utf-8",
    )

    (TARGET / "client/ClientModEvents.java").write_text(
        '''package com.bl4ues.scpinventory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("scp_inventory_pickup_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null) {
                        PickupPromptClient.render(graphics, width, height, partialTick);
                    }
                });
        event.registerAboveAll("scp_inventory_context_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null) {
                        ContextPromptClient.render(graphics, width, height, partialTick);
                    }
                });
        event.registerAboveAll("scp_inventory_full_notice",
                (gui, graphics, partialTick, width, height) -> InventoryFullOverlay.render(graphics));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_SCP_INVENTORY);
        event.register(Keybinds.CONTEXT_INTERACT);
        event.register(Keybinds.CONTEXT_CONFIG_SELECT);
    }
}
''',
        encoding="utf-8",
    )

    (TARGET / "client/ClientGameplayEvents.java").write_text(
        '''package com.bl4ues.scpinventory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class ClientGameplayEvents {
    private ClientGameplayEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PickupPromptClient.clientTick();
        }
    }
}
''',
        encoding="utf-8",
    )

    (TARGET / "network/ModNetwork.java").write_text(
        '''package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static boolean registered;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpInventoryMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, InventoryFullPacket.class, InventoryFullPacket::encode, InventoryFullPacket::decode, InventoryFullPacket::handle);
        CHANNEL.registerMessage(id++, SyncInventoryPacket.class, SyncInventoryPacket::encode, SyncInventoryPacket::decode, SyncInventoryPacket::handle);
        CHANNEL.registerMessage(id++, RequestInventorySyncPacket.class, RequestInventorySyncPacket::encode, RequestInventorySyncPacket::decode, RequestInventorySyncPacket::handle);
        CHANNEL.registerMessage(id++, InventoryActionPacket.class, InventoryActionPacket::encode, InventoryActionPacket::decode, InventoryActionPacket::handle);
        CHANNEL.registerMessage(id++, EquipmentActionPacket.class, EquipmentActionPacket::encode, EquipmentActionPacket::decode, EquipmentActionPacket::handle);
        CHANNEL.registerMessage(id++, KeyActionPacket.class, KeyActionPacket::encode, KeyActionPacket::decode, KeyActionPacket::handle);
        CHANNEL.registerMessage(id++, DocumentActionPacket.class, DocumentActionPacket::encode, DocumentActionPacket::decode, DocumentActionPacket::handle);
        CHANNEL.registerMessage(id++, InventoryMovePacket.class, InventoryMovePacket::encode, InventoryMovePacket::decode, InventoryMovePacket::handle);
        CHANNEL.registerMessage(id++, PickupItemPacket.class, PickupItemPacket::encode, PickupItemPacket::decode, PickupItemPacket::handle);
        CHANNEL.registerMessage(id++, UseHotbarItemPacket.class, UseHotbarItemPacket::encode, UseHotbarItemPacket::decode, UseHotbarItemPacket::handle);
        CHANNEL.registerMessage(id++, UsableSessionReturnPacket.class, UsableSessionReturnPacket::encode, UsableSessionReturnPacket::decode, UsableSessionReturnPacket::handle);
        CHANNEL.registerMessage(id++, UsableSessionDropPacket.class, UsableSessionDropPacket::encode, UsableSessionDropPacket::decode, UsableSessionDropPacket::handle);
        CHANNEL.registerMessage(id++, MainUseActionPacket.class, MainUseActionPacket::encode, MainUseActionPacket::decode, MainUseActionPacket::handle);
        CHANNEL.registerMessage(id++, ContextInteractPacket.class, ContextInteractPacket::encode, ContextInteractPacket::decode, ContextInteractPacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigSelectPacket.class, ContextConfigSelectPacket::encode, ContextConfigSelectPacket::decode, ContextConfigSelectPacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigOpenPacket.class, ContextConfigOpenPacket::encode, ContextConfigOpenPacket::decode, ContextConfigOpenPacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigSavePacket.class, ContextConfigSavePacket::encode, ContextConfigSavePacket::decode, ContextConfigSavePacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigReloadPacket.class, ContextConfigReloadPacket::encode, ContextConfigReloadPacket::decode, ContextConfigReloadPacket::handle);
        CHANNEL.registerMessage(id++, ContextConfigDeletePacket.class, ContextConfigDeletePacket::encode, ContextConfigDeletePacket::decode, ContextConfigDeletePacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigOpenRequestPacket.class, ItemConfigOpenRequestPacket::encode, ItemConfigOpenRequestPacket::decode, ItemConfigOpenRequestPacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigOpenPacket.class, ItemConfigOpenPacket::encode, ItemConfigOpenPacket::decode, ItemConfigOpenPacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigSavePacket.class, ItemConfigSavePacket::encode, ItemConfigSavePacket::decode, ItemConfigSavePacket::handle);
        CHANNEL.registerMessage(id++, ItemConfigReloadPacket.class, ItemConfigReloadPacket::encode, ItemConfigReloadPacket::decode, ItemConfigReloadPacket::handle);
        CHANNEL.registerMessage(id, ItemConfigDeletePacket.class, ItemConfigDeletePacket::encode, ItemConfigDeletePacket::decode, ItemConfigDeletePacket::handle);
    }

    public static void syncTo(ServerPlayer player, IScpInventory inventory) {
        if (player != null && inventory != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncInventoryPacket(inventory.serializeNBT()));
        }
    }

    public static void showInventoryFull(ServerPlayer player) {
        if (player != null && !player.isCreative() && !player.isSpectator()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new InventoryFullPacket());
        }
    }

    public static void activateUsableItem(ServerPlayer player, int hotbarSlot,
            boolean continuousUse, ItemStack stack) {
        activateUsableItem(player, hotbarSlot, -1, continuousUse, stack);
    }

    public static void activateUsableItem(ServerPlayer player, int hotbarSlot,
            int sourceSlot, boolean continuousUse, ItemStack stack) {
        if (player != null && !player.isCreative() && !player.isSpectator()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new UseHotbarItemPacket(hotbarSlot, sourceSlot, continuousUse, stack));
        }
    }
}
''',
        encoding="utf-8",
    )


def adapt_client_packet_handlers() -> None:
    handlers = TARGET / "client/ClientPacketHandlers.java"
    text = handlers.read_text(encoding="utf-8")
    text = re.sub(r"\n    public static void showScp131Notice\(.*?\n    }\n", "\n", text, flags=re.S)
    text = re.sub(r"\n    public static void setBlinkWatcherActive\(.*?\n    }\n", "\n", text, flags=re.S)
    text = re.sub(r"\n    public static void playScareSound\(\).*?\n    }\n", "\n", text, flags=re.S)
    handlers.write_text(text, encoding="utf-8")


def retarget_subscribers() -> None:
    for java in TARGET.rglob("*.java"):
        text = java.read_text(encoding="utf-8")
        text = text.replace(
            "@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID",
            "@Mod.EventBusSubscriber(modid = \"scp_additions\"",
        )
        text = text.replace(
            "@Mod.EventBusSubscriber(modid = \"scpinventory\"",
            "@Mod.EventBusSubscriber(modid = \"scp_additions\"",
        )
        text = text.replace(
            "@Mod.EventBusSubscriber(value = Dist.CLIENT)",
            "@Mod.EventBusSubscriber(modid = \"scp_additions\", value = Dist.CLIENT)",
        )
        java.write_text(text, encoding="utf-8")


def add_toggle_guards() -> None:
    key_handler = TARGET / "client/ClientKeyHandler.java"
    text = key_handler.read_text(encoding="utf-8")
    text = text.replace(
        "import net.minecraft.client.Minecraft;\n",
        "import net.minecraft.client.Minecraft;\n"
        "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
    )
    text = text.replace(
        "if (Keybinds.OPEN_SCP_INVENTORY.consumeClick()) {",
        "if (Keybinds.OPEN_SCP_INVENTORY.consumeClick()\n"
        "                && ScpAdditionsModulesConfig.get().inventory.enabled) {",
    )
    key_handler.write_text(text, encoding="utf-8")

    pickup_handler = TARGET / "events/BlockPickupHandler.java"
    text = pickup_handler.read_text(encoding="utf-8")
    text = text.replace(
        "import net.minecraftforge.fml.common.Mod;\n",
        "import net.minecraftforge.fml.common.Mod;\n"
        "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
    )
    text = text.replace(
        "public static void onEntityJoinLevel(EntityJoinLevelEvent event) {\n",
        "public static void onEntityJoinLevel(EntityJoinLevelEvent event) {\n"
        "        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;\n",
    )
    text = text.replace(
        "public static void onItemPickup(EntityItemPickupEvent event) {\n",
        "public static void onItemPickup(EntityItemPickupEvent event) {\n"
        "        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;\n",
    )
    pickup_handler.write_text(text, encoding="utf-8")


def disable_coin_mirrors() -> None:
    router = TARGET / "item/ScpPickupRouter.java"
    text = router.read_text(encoding="utf-8")
    replacement = '''    private static int acceptCoin(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemStack stored = stack.copy();
        stripNoMergeMarker(stored);
        stripCoinMirror(stored);
        return inventory.addInventoryItems(stored);
    }

'''
    text, count = re.subn(
        r"    private static int acceptCoin\(.*?\n    private static int acceptHarmful",
        replacement + "    private static int acceptHarmful",
        text,
        flags=re.S,
    )
    if count != 1:
        raise RuntimeError(f"Expected one acceptCoin method, changed {count}")
    router.write_text(text, encoding="utf-8")


def remove_prototype() -> None:
    provisional = ROOT / "src/main/java/net/mcreator/scpadditions/inventory"
    keep = {"ScpInventoryAccess.java", "ScpInventoryIntegration.java"}
    if not provisional.exists():
        return
    for file in provisional.rglob("*.java"):
        if file.parent == provisional and file.name in keep:
            continue
        file.unlink()
    for directory in sorted((p for p in provisional.rglob("*") if p.is_dir()), reverse=True):
        try:
            directory.rmdir()
        except OSError:
            pass


def update_additions_bootstrap() -> None:
    main = ROOT / "src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java"
    text = main.read_text(encoding="utf-8")
    text = text.replace(
        "import net.mcreator.scpadditions.inventory.ScpInventoryNetwork;\n", ""
    )
    text = text.replace(
        "\t\tScpInventoryNetwork.register();",
        "\t\tcom.bl4ues.scpinventory.network.ModNetwork.register();",
    )
    main.write_text(text, encoding="utf-8")


def main() -> None:
    copy_canonical_sources()
    write_shims()
    adapt_client_packet_handlers()
    retarget_subscribers()
    add_toggle_guards()
    disable_coin_mirrors()
    remove_prototype()
    update_additions_bootstrap()
    print("Canonical SCP Inventory port prepared")


if __name__ == "__main__":
    main()
