package net.mcreator.scpadditions.config.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import net.mcreator.scpadditions.config.ConfigFilePersistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Opens the existing Configuration Center outside a world while keeping its
 * server-owned controls locked. This class deliberately creates no substitute
 * screens or vanilla-styled duplicate UI.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientPreferencesMenu {
    private static final Path PREFERENCES = FMLPaths.CONFIGDIR.get()
            .resolve("scpadditions").resolve("client_preferences.json");
    private static final String CONFIG_CLIENT =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient";
    private static final String HOME_SCREEN = CONFIG_CLIENT + "$HomeScreen";

    private ClientPreferencesMenu() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            migrateMainMenuMusicDefault();
            MinecraftForge.registerConfigScreen(ClientPreferencesMenu::open);
        });
    }

    public static Screen open(Minecraft minecraft, Screen parent) {
        if (minecraft.player != null && minecraft.getConnection() != null) {
            ConfigCenterClient.requestOpen(parent);
            return minecraft.screen;
        }

        try {
            JsonObject snapshot = ConfigCenterService.snapshot();
            JsonObject modules = snapshot.has(ConfigCenterService.MODULES)
                    && snapshot.get(ConfigCenterService.MODULES).isJsonObject()
                    ? snapshot.getAsJsonObject(
                            ConfigCenterService.MODULES).deepCopy()
                    : new JsonObject();
            ClientModulePreferences.applyTo(modules);
            snapshot.add(ConfigCenterService.MODULES, modules);

            JsonObject permissions = new JsonObject();
            permissions.addProperty("can_edit_server", false);
            snapshot.add("__permissions", permissions);

            Class<?> client = Class.forName(CONFIG_CLIENT);
            setStatic(client, "rootParent", parent);
            setStatic(client, "files", snapshot);
            setStatic(client, "homeNotice", "");

            Class<?> homeType = Class.forName(HOME_SCREEN);
            Constructor<?> constructor = homeType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Screen) constructor.newInstance();
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not open the local Configuration Center", exception);
            return parent;
        }
    }

    private static void setStatic(Class<?> owner, String name, Object value)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    /** Give existing preference files the advertised default for this new key. */
    private static void migrateMainMenuMusicDefault() {
        try {
            if (!Files.exists(PREFERENCES)) {
                ClientModulePreferences.load();
                return;
            }

            JsonElement parsed = JsonParser.parseString(
                    Files.readString(PREFERENCES, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                ClientModulePreferences.load();
                return;
            }

            JsonObject root = parsed.getAsJsonObject();
            JsonObject audio;
            if (root.has("audio") && root.get("audio").isJsonObject()) {
                audio = root.getAsJsonObject("audio");
            } else {
                audio = new JsonObject();
                root.add("audio", audio);
            }

            if (!audio.has("mainMenuMusicEnabled")) {
                audio.addProperty("mainMenuMusicEnabled", true);
                ConfigFilePersistence.writeWithBackup(PREFERENCES,
                        root.toString() + System.lineSeparator());
            }
            ClientModulePreferences.load();
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not migrate client menu-music preferences",
                    exception);
            ClientModulePreferences.load();
        }
    }
}
