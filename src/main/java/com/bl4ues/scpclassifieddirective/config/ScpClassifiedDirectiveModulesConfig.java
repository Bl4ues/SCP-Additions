package com.bl4ues.scpclassifieddirective.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class ScpClassifiedDirectiveModulesConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("scp_classified_directive").resolve("modules.json");
	private static final String BUNDLED_CONFIG = "config/scp_classified_directive/modules.json";
	private static volatile Root current = Root.defaults();

	private ScpClassifiedDirectiveModulesConfig() {
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
			ScpClassifiedDirectiveMod.LOGGER.error("Failed to load {}. Using safe default module settings for this launch.", CONFIG_PATH, exception);
		}
		current = loaded;
		ScpClassifiedDirectiveMod.LOGGER.info("Loaded SCP: Classified Directive module configuration from {}", CONFIG_PATH);
	}

	private static void copyBundledConfig() throws IOException {
		try (InputStream stream = ScpClassifiedDirectiveModulesConfig.class.getClassLoader()
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

		@SerializedName("death_bodies")
		public Toggle deathBodies = new Toggle();

		@SerializedName("scp_173")
		public Toggle scp173 = new Toggle();

		public Stealth stealth = new Stealth();

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
			if (deathBodies == null) deathBodies = new Toggle();
			if (scp173 == null) scp173 = new Toggle();
			if (stealth == null) stealth = new Stealth();
			stealth.normalize();
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

	private static double clamp(double value, double minimum, double maximum,
			double fallback) {
		if (!Double.isFinite(value)) return fallback;
		return Math.max(minimum, Math.min(maximum, value));
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

		@SerializedName("action_bars_roboto")
		public boolean actionBarsRoboto = true;

		@SerializedName("disable_text_drop_shadows")
		public boolean disableTextDropShadows = true;

		@SerializedName("facility_chat_interface")
		public boolean facilityChatInterface = true;
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

		@SerializedName("custom_hotbar")
		public boolean customHotbar = true;
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

		@SerializedName("custom_item_interaction_sounds")
		public boolean customItemInteractionSounds = true;

		@SerializedName("replace_player_hurt_sounds")
		public boolean replacePlayerHurtSounds = true;

		@SerializedName("use_voice_profile_b")
		public boolean useVoiceProfileB = false;

		@SerializedName("mute_non_player_hit_sounds")
		public boolean muteNonPlayerHitSounds = false;

		@SerializedName("disable_vanilla_music")
		public boolean disableVanillaMusic = true;

		@SerializedName("main_menu_music_enabled")
		public boolean mainMenuMusicEnabled = true;
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

	/** Server-owned advanced crouch and visual-perception framework settings. */
	public static final class Stealth extends Toggle {
		public static final double STANDING_VISIBILITY = 1.0D;
		public static final double CROUCHING_VISIBILITY = 0.60D;
		public static final double CRAWLING_VISIBILITY = 0.30D;
		public static final int MAX_ACQUIRE_DELAY_TICKS = 50;
		public static final double DARKNESS_FLOOR = 0.18D;
		public static final double MINIMUM_CLOSE_RANGE = 2.5D;

		public transient double standingVisibility = STANDING_VISIBILITY;
		public transient double crouchingVisibility = CROUCHING_VISIBILITY;
		public transient double crawlingVisibility = CRAWLING_VISIBILITY;
		public transient int maxAcquireDelayTicks = MAX_ACQUIRE_DELAY_TICKS;
		public transient double darknessFloor = DARKNESS_FLOOR;
		public transient double minimumCloseRange = MINIMUM_CLOSE_RANGE;

		@SerializedName("perception_rules")
		public List<PerceptionRule> perceptionRules = defaultPerceptionRules();

		private void normalize() {
			standingVisibility = STANDING_VISIBILITY;
			crouchingVisibility = CROUCHING_VISIBILITY;
			crawlingVisibility = CRAWLING_VISIBILITY;
			maxAcquireDelayTicks = MAX_ACQUIRE_DELAY_TICKS;
			darknessFloor = DARKNESS_FLOOR;
			minimumCloseRange = MINIMUM_CLOSE_RANGE;

			List<PerceptionRule> sourceRules = perceptionRules == null
					? defaultPerceptionRules() : perceptionRules;
			List<PerceptionRule> editableRules = new ArrayList<>();
			for (PerceptionRule rule : sourceRules) {
				if (rule == null) continue;
				rule.normalize();
				if (!isIntegratedPerceptionEntity(rule.entity)) {
					editableRules.add(rule);
				}
			}

			List<PerceptionRule> normalizedRules = integratedPerceptionRules();
			normalizedRules.addAll(editableRules);
			perceptionRules = normalizedRules;
		}
	}

	/** One entity-specific override used by the generic perception service. */
	public static final class PerceptionRule {
		public String entity = "";
		public boolean omniscient;
		public boolean blind;

		@SerializedName("night_vision")
		public boolean nightVision;

		@SerializedName("visibility_multiplier")
		public double visibilityMultiplier = 1.0D;

		@SerializedName("range_multiplier")
		public double rangeMultiplier = 1.0D;

		@SerializedName("acquire_delay_multiplier")
		public double acquireDelayMultiplier = 1.0D;

		public PerceptionRule() {
		}

		private PerceptionRule(String entity) {
			this.entity = entity;
		}

		private void normalize() {
			if (entity == null) entity = "";
			entity = entity.trim();
			visibilityMultiplier = clamp(visibilityMultiplier, 0.0D, 4.0D, 1.0D);
			rangeMultiplier = clamp(rangeMultiplier, 0.0D, 4.0D, 1.0D);
			acquireDelayMultiplier = clamp(acquireDelayMultiplier, 0.0D, 4.0D, 1.0D);
		}
	}

	public static boolean isIntegratedPerceptionEntity(String entity) {
		if (entity == null) return false;
		return switch (entity.trim()) {
			case "scp_classified_directive:scp_106",
					"scp_classified_directive:scp_173",
					"scp_classified_directive:scp_939" -> true;
			default -> false;
		};
	}

	private static List<PerceptionRule> integratedPerceptionRules() {
		List<PerceptionRule> rules = new ArrayList<>();

		PerceptionRule scp106 = new PerceptionRule("scp_classified_directive:scp_106");
		scp106.omniscient = true;
		rules.add(scp106);

		PerceptionRule scp939 = new PerceptionRule("scp_classified_directive:scp_939");
		scp939.blind = true;
		rules.add(scp939);

		PerceptionRule scp173 = new PerceptionRule("scp_classified_directive:scp_173");
		scp173.visibilityMultiplier = 1.35D;
		scp173.acquireDelayMultiplier = 0.55D;
		rules.add(scp173);
		return rules;
	}

	private static List<PerceptionRule> defaultPerceptionRules() {
		List<PerceptionRule> rules = integratedPerceptionRules();

		PerceptionRule spider = new PerceptionRule("minecraft:spider");
		spider.nightVision = true;
		rules.add(spider);

		PerceptionRule caveSpider = new PerceptionRule("minecraft:cave_spider");
		caveSpider.nightVision = true;
		rules.add(caveSpider);
		return rules;
	}
}
