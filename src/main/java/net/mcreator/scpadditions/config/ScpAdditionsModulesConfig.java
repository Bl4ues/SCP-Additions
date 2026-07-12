package net.mcreator.scpadditions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
		try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			GSON.toJson(config, writer);
		}
	}

	public static Root get() {
		return current;
	}

	public static final class Root {
		public Toggle inventory = new Toggle();
		public Toggle interactions = new Toggle();
		public Toggle hud = new Toggle();
		public Vitals vitals = new Vitals();
		public Toggle blink = new Toggle();

		@SerializedName("scp_173")
		public EntityModule scp173 = new EntityModule();

		private static Root defaults() {
			return new Root();
		}

		private Root normalize() {
			if (inventory == null) inventory = new Toggle();
			if (interactions == null) interactions = new Toggle();
			if (hud == null) hud = new Toggle();
			if (vitals == null) vitals = new Vitals();
			if (blink == null) blink = new Toggle();
			if (scp173 == null) scp173 = new EntityModule();
			return this;
		}
	}

	public static class Toggle {
		public boolean enabled = true;
	}

	public static final class Vitals {
		@SerializedName("custom_health_enabled")
		public boolean customHealthEnabled = true;

		@SerializedName("stamina_enabled")
		public boolean staminaEnabled = true;

		@SerializedName("horror_movement_enabled")
		public boolean horrorMovementEnabled = true;
	}

	public static final class EntityModule extends Toggle {
		@SerializedName("natural_spawn_enabled")
		public boolean naturalSpawnEnabled = true;
	}
}
