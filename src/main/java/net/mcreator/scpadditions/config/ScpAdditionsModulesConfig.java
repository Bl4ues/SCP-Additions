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
		public Crosshair crosshair = new Crosshair();
		public Inventory inventory = new Inventory();
		public Interactions interactions = new Interactions();
		public Hud hud = new Hud();
		public Vitals vitals = new Vitals();
		public Hunger hunger = new Hunger();
		public Toggle blink = new Toggle();
		public Audio audio = new Audio();
		public Accessibility accessibility = new Accessibility();
		public Debug debug = new Debug();

		@SerializedName("scp_173")
		public Toggle scp173 = new Toggle();

		private static Root defaults() {
			return new Root();
		}

		private Root normalize() {
			if (crosshair == null) crosshair = new Crosshair();
			crosshair.normalize();
			if (inventory == null) inventory = new Inventory();
			if (interactions == null) interactions = new Interactions();
			if (hud == null) hud = new Hud();
			if (vitals == null) vitals = new Vitals();
			if (hunger == null) hunger = new Hunger();
			if (blink == null) blink = new Toggle();
			if (audio == null) audio = new Audio();
			if (accessibility == null) accessibility = new Accessibility();
			if (debug == null) debug = new Debug();
			if (scp173 == null) scp173 = new Toggle();
			return this;
		}
	}

	public static final class Crosshair {
		public boolean enabled = true;

		@SerializedName("in_game_enabled")
		public boolean inGameEnabled = true;

		public double red = 1.0D;
		public double green = 1.0D;
		public double blue = 1.0D;
		public double alpha = 1.0D;

		private void normalize() {
			red = clampUnit(red);
			green = clampUnit(green);
			blue = clampUnit(blue);
			alpha = clampUnit(alpha);
		}
	}

	private static double clampUnit(double value) {
		if (!Double.isFinite(value)) return 1.0D;
		return Math.max(0.0D, Math.min(1.0D, value));
	}

	public static class Toggle {
		public boolean enabled = true;
	}

	public static final class Hud extends Toggle {
		@SerializedName("hide_active_effect_indicators")
		public boolean hideActiveEffectIndicators = true;

		@SerializedName("hide_empty_hand")
		public boolean hideEmptyHand = true;

		@SerializedName("disable_experience_bar")
		public boolean disableExperienceBar = true;

		@SerializedName("custom_oxygen_bar")
		public boolean customOxygenBar = true;
	}

	public static final class Interactions extends Toggle {
		@SerializedName("disable_in_creative")
		public boolean disableInCreative = false;
	}

	public static final class Inventory extends Toggle {
		@SerializedName("remember_ui_state")
		public boolean rememberUiState = true;

		@SerializedName("require_equipped_weapon_to_attack")
		public boolean requireEquippedWeaponToAttack = false;
	}

	public static final class Vitals {
		@SerializedName("custom_health_enabled")
		public boolean customHealthEnabled = true;

		@SerializedName("stamina_enabled")
		public boolean staminaEnabled = true;

		@SerializedName("horror_movement_enabled")
		public boolean horrorMovementEnabled = true;
	}

	public static final class Hunger {
		public boolean disabled = true;
	}

	public static final class Audio {
		@SerializedName("enter_sound_enabled")
		public boolean enterSoundEnabled = true;

		@SerializedName("save_game_sound_enabled")
		public boolean saveGameSoundEnabled = true;

		@SerializedName("replace_player_hurt_sounds")
		public boolean replacePlayerHurtSounds = true;

		@SerializedName("use_voice_profile_b")
		public boolean useVoiceProfileB = false;

		@SerializedName("mute_non_player_hit_sounds")
		public boolean muteNonPlayerHitSounds = false;

		@SerializedName("disable_vanilla_music")
		public boolean disableVanillaMusic = false;
	}

	public static final class Accessibility {
		@SerializedName("reduce_scp_012_visual_effects")
		public boolean reduceScp012VisualEffects = false;
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
