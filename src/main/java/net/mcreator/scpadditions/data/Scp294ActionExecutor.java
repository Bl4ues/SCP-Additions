package net.mcreator.scpadditions.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Optional;

public final class Scp294ActionExecutor {
	private static final ResourceLocation DEFAULT_DAMAGE_TYPE = new ResourceLocation("scp_additions:scp294");

	private Scp294ActionExecutor() {
	}

	public static void executeActions(LevelAccessor world, double x, double y, double z, Entity entity, ListTag actions) {
		for (int i = 0; i < actions.size(); i++) {
			CompoundTag action = actions.getCompound(i).copy();
			int delay = Math.max(0, action.getInt("delay_ticks"));
			if (delay > 0) {
				ScpAdditionsMod.queueServerWork(delay, () -> executeAction(world, x, y, z, entity, action));
			} else {
				executeAction(world, x, y, z, entity, action);
			}
		}
	}

	private static void executeAction(LevelAccessor world, double x, double y, double z, Entity entity, CompoundTag action) {
		String type = action.getString("type");
		switch (type) {
			case "actionbar" -> showMessage(entity, action.getString("message"), true);
			case "message" -> showMessage(entity, action.getString("message"), false);
			case "sound" -> playSound(world, x, y, z, action.getString("sound"));
			case "particle" -> spawnParticles(world, x, y, z, action.getString("particle"), Math.max(1, action.getInt("count")), action.getFloat("radius"));
			case "visual_explosion" -> visualExplosion(world, x, y, z, action);
			case "effect" -> addEffect(entity, action);
			case "remove_effect" -> removeEffect(entity, action.getString("effect"));
			case "heal" -> heal(entity, action.getFloat("amount"));
			case "hurt" -> hurt(entity, action.getFloat("amount"));
			case "kill" -> kill(entity, action);
			case "set_fire" -> {
				if (entity != null) {
					entity.setSecondsOnFire(Math.max(1, action.getInt("seconds")));
				}
			}
			default -> {
				if (!type.isBlank()) {
					ScpAdditionsMod.LOGGER.warn("Unknown SCP-294 action type '{}'", type);
				}
			}
		}
	}

	private static void visualExplosion(LevelAccessor world, double x, double y, double z, CompoundTag action) {
		String sound = action.getString("sound");
		playSound(world, x, y, z, sound.isBlank() ? "minecraft:entity.generic.explode" : sound);
		spawnParticles(world, x, y + 0.75D, z, "flash", 1, 0.0F);
		spawnParticles(world, x, y + 0.75D, z, "explosion", 1, 0.0F);
		spawnParticles(world, x, y + 0.75D, z, "smoke", Math.max(8, action.getInt("count")), Math.max(0.35F, action.getFloat("radius")));
	}

	private static void showMessage(Entity entity, String message, boolean actionbar) {
		if (!message.isBlank() && entity instanceof Player player && !player.level().isClientSide()) {
			player.displayClientMessage(Component.literal(message), actionbar);
		}
	}

	private static void addEffect(Entity entity, CompoundTag action) {
		if (!(entity instanceof LivingEntity living)) {
			return;
		}
		MobEffect effect = getEffect(action.getString("effect"));
		if (effect == null) {
			return;
		}
		living.addEffect(new MobEffectInstance(
				effect,
				Math.max(1, action.getInt("duration")),
				Math.max(0, action.getInt("amplifier")),
				action.getBoolean("ambient"),
				!action.contains("visible", Tag.TAG_BYTE) || action.getBoolean("visible"),
				!action.contains("show_icon", Tag.TAG_BYTE) || action.getBoolean("show_icon")));
	}

	private static void removeEffect(Entity entity, String effectId) {
		if (entity instanceof LivingEntity living) {
			MobEffect effect = getEffect(effectId);
			if (effect != null) {
				living.removeEffect(effect);
			}
		}
	}

	private static MobEffect getEffect(String effectId) {
		if (effectId == null || effectId.isBlank()) {
			return null;
		}
		return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectId));
	}

	private static void heal(Entity entity, float amount) {
		if (amount > 0 && entity instanceof LivingEntity living) {
			living.setHealth(Math.min(living.getMaxHealth(), living.getHealth() + amount));
		}
	}

	private static void hurt(Entity entity, float amount) {
		if (amount > 0 && entity instanceof LivingEntity living) {
			Level level = living.level();
			living.hurt(level.damageSources().generic(), amount);
		}
	}

	private static void kill(Entity entity, CompoundTag action) {
		if (!(entity instanceof LivingEntity living)) {
			return;
		}

		Level level = living.level();
		ResourceLocation damageTypeId = resolveDamageType(action);
		ResourceKey<DamageType> damageTypeKey = ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId);
		ResourceKey<DamageType> fallbackKey = ResourceKey.create(Registries.DAMAGE_TYPE, DEFAULT_DAMAGE_TYPE);
		Holder.Reference<DamageType> damageType = getDamageType(level, damageTypeKey).orElseGet(() -> level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(fallbackKey));
		living.hurt(new DamageSource(damageType), Float.MAX_VALUE);
	}

	private static Optional<Holder.Reference<DamageType>> getDamageType(Level level, ResourceKey<DamageType> key) {
		return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(key);
	}

	private static ResourceLocation resolveDamageType(CompoundTag action) {
		String configured = action.contains("damage_type", Tag.TAG_STRING) ? action.getString("damage_type") : "";
		if (!configured.isBlank()) {
			return new ResourceLocation(configured);
		}

		String marker = action.getString("message").toLowerCase();
		if (marker.contains("bleach")) return new ResourceLocation("scp_additions:scp294_bleach");
		if (marker.contains("106") || marker.contains("corrosive black")) return new ResourceLocation("scp_additions:scp294_106");
		if (marker.contains("butt ghost")) return new ResourceLocation("scp_additions:scp294_butt_ghost");
		if (marker.contains("carbon")) return new ResourceLocation("scp_additions:scp294_carbon");
		if (marker.contains("neutronium") || marker.contains("density")) return new ResourceLocation("scp_additions:scp294_neutronium");
		if (marker.contains("jewel") || marker.contains("lifeforce")) return new ResourceLocation("scp_additions:scp294_lifeforce");
		if (marker.contains("lava") || marker.contains("magma")) return new ResourceLocation("scp_additions:scp294_lava");
		if (marker.contains("quantum") || marker.contains("nitrogen") || marker.contains("helium") || marker.contains("hydrogen")) return new ResourceLocation("scp_additions:scp294_cold");
		if (marker.contains("radiation") || marker.contains("radioactive") || marker.contains("nuclear")) return new ResourceLocation("scp_additions:scp294_radiation");
		if (marker.contains("fear")) return new ResourceLocation("scp_additions:scp294_fear");
		if (marker.contains("overdose") || marker.contains("heroin")) return new ResourceLocation("scp_additions:scp294_overdose");
		if (marker.contains("glass")) return new ResourceLocation("scp_additions:scp294_glass");
		if (marker.contains("gold")) return new ResourceLocation("scp_additions:scp294_gold");
		if (marker.contains("metal") || marker.contains("iron")) return new ResourceLocation("scp_additions:scp294_metal");
		if (marker.contains("joy") || marker.contains("happiness")) return new ResourceLocation("scp_additions:scp294_happiness");
		if (marker.contains("death")) return new ResourceLocation("scp_additions:scp294_death");
		if (marker.contains("violent reaction") || marker.contains("antimatter") || marker.contains("tachyon") || marker.contains("quark") || marker.contains("682")) return new ResourceLocation("scp_additions:scp294_violent_reaction");
		return DEFAULT_DAMAGE_TYPE;
	}

	private static void playSound(LevelAccessor world, double x, double y, double z, String soundId) {
		if (!(world instanceof Level level)) {
			return;
		}
		SoundEvent sound = SoundEvents.GENERIC_DRINK;
		if (soundId != null && !soundId.isBlank()) {
			SoundEvent configured = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundId));
			if (configured != null) {
				sound = configured;
			}
		}
		if (!level.isClientSide()) {
			level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.NEUTRAL, 1, 1);
		} else {
			level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, 1, 1, false);
		}
	}

	private static void spawnParticles(LevelAccessor world, double x, double y, double z, String particle, int count, float radius) {
		if (!(world instanceof ServerLevel level)) {
			return;
		}
		ParticleOptions options = switch (particle) {
			case "smoke" -> ParticleTypes.SMOKE;
			case "flash" -> ParticleTypes.FLASH;
			case "flame" -> ParticleTypes.FLAME;
			case "happy" -> ParticleTypes.HAPPY_VILLAGER;
			case "splash" -> ParticleTypes.SPLASH;
			case "cloud" -> ParticleTypes.CLOUD;
			default -> ParticleTypes.EXPLOSION_EMITTER;
		};
		level.sendParticles(options, x, y, z, count, radius, radius, radius, 0.02D);
	}
}