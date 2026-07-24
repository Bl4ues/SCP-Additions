package net.mcreator.scpadditions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ScpAdditionsModulesConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("scpadditions").resolve("modules.json");
	private static final String BUNDLED_CONFIG = "config/scpadditions/modules.json";
	private static volatile Root current = Root.defaults();

	private ScpAdditionsModulesConfig() {
	}

	public static synchronized void load() {
		Root loaded = Root.defaults();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			if (Files.notExists(CONFIG_PATH)) {
				copyBundledConfig();
			}
			if (Files.exists(CONFIG_PATH)) {
				try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
					Root parsed = GSON.fromJson(reader, Root.class);
					if (parsed != null) loaded = parsed.normalize();
				}
			}
			writeConfig(loaded);
		} catch (IOException | JsonParseException exception) {
			ScpAdditionsMod.LOGGER.error("Failed to load {}. Using safe default module settings for this launch.", CONFIG_PATH, exception);
		}
		current = loaded;
		ScpAdditionsMod.LOGGER.info("Loaded SCP Additions module configuration from {}", CONFIG_PATH);
	}

	private static void copyBundledConfig() throws IOException {
		try (InputStream stream = ScpAdditionsModulesConfig.class.getClassLoader()
				.getResourceAsStream(BUNDLED_CONFIG)) {
			if (stream != null) {
				Files.copy(stream, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	private static void writeConfig(Root config) throws IOException {
		ConfigFilePersistence.writeWithBackup(CONFIG_PATH,
				GSON.toJson(config) + System.lineSeparator());
	}

	public static Root get() {
		return current;
	}

	public static boolean customInteractionsEnabledFor(Player player) {
		Interactions settings = current.interactions;
		return player != null
				&& !player.isSpectator()
				&& settings.enabled
				&& (!player.isCreative() || !settings.disableInCreative);
	}

	public static final class Root {
		public Inventory inventory = new Inventory();
		public Interactions interactions = new Interactions();
		public Toggle hud = new Toggle();
		public Vitals vitals = new Vitals();
		public Toggle blink = new Toggle();
		public Audio audio = new Audio();
		public Debug debug = new Debug();

		@SerializedName("scp_173")
		public Toggle scp173 = new Toggle();

		private static Root defaults() {
			return new Root();
		}

		private Root normalize() {
			if (inventory == null) inventory = new Inventory();
			if (interactions == null) interactions = new Interactions();
			if (hud == null) hud = new Toggle();
			if (vitals == null) vitals = new Vitals();
			if (blink == null) blink = new Toggle();
			if (audio == null) audio = new Audio();
			if (debug == null) debug = new Debug();
			if (scp173 == null) scp173 = new Toggle();
			return this;
		}
	}

	public static class Toggle {
		public boolean enabled = true;
	}

	public static final class Interactions extends Toggle {
		@SerializedName("disable_in_creative")
		public boolean disableInCreative = false;
	}

	public static final class Inventory extends Toggle {
		@SerializedName("remember_ui_state")
		public boolean rememberUiState = true;
	}

	public static final class Vitals {
		@SerializedName("custom_health_enabled")
		public boolean customHealthEnabled = true;

		@SerializedName("stamina_enabled")
		public boolean staminaEnabled = true;

		@SerializedName("horror_movement_enabled")
		public boolean horrorMovementEnabled = true;
	}

	public static final class Audio {
		@SerializedName("enter_sound_enabled")
		public boolean enterSoundEnabled = true;
	}

	public static final class Debug {
		@SerializedName("show_scp_079_energy_hud")
		public boolean showScp079EnergyHud = false;

		@SerializedName("show_scp_079_decision_log_hud")
		public boolean showScp079DecisionLogHud = false;

		@SerializedName("show_scp_spawn_timers_hud")
		public boolean showScpSpawnTimersHud = false;
	}
}
