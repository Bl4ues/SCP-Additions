#!/usr/bin/env python3
from __future__ import annotations

import json
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
    if new in text:
        return
    if old not in text:
        raise RuntimeError(f"Expected source fragment not found in {rel}: {old[:120]!r}")
    write(rel, text.replace(old, new, 1))


def replace_all(rel: str, old: str, new: str) -> None:
    text = read(rel)
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"Expected source fragment not found in {rel}: {old[:120]!r}")
    write(rel, text.replace(old, new))


# SCP-294 outputs that point to an obsolete built-in item without an item model
# become the generic NBT-backed cup instead of a missing-texture item.
replace_once(
    "src/main/java/net/mcreator/scpadditions/data/Scp294DrinkManager.java",
    '''\tprivate static ResourceLocation normalizeLegacyDrinkItem(ResourceLocation requested) {
\t\tif (ScpAdditionsMod.MODID.equals(requested.getNamespace())
\t\t\t\t&& LEGACY_DRINK_ITEM_PATHS.contains(requested.getPath())) {
\t\t\treturn GENERIC_CUP;
\t\t}
\t\treturn requested;
\t}
''',
    '''\tprivate static ResourceLocation normalizeLegacyDrinkItem(ResourceLocation requested) {
\t\tif (!ScpAdditionsMod.MODID.equals(requested.getNamespace())
\t\t\t\t|| GENERIC_CUP.equals(requested)) {
\t\t\treturn requested;
\t\t}
\t\tif (LEGACY_DRINK_ITEM_PATHS.contains(requested.getPath())
\t\t\t\t|| !hasBundledItemModel(requested)) {
\t\t\tScpAdditionsMod.LOGGER.warn(
\t\t\t\t\t"SCP-294 result {} has no usable item model; using the generic configurable cup",
\t\t\t\t\trequested);
\t\t\treturn GENERIC_CUP;
\t\t}
\t\treturn requested;
\t}

\tprivate static boolean hasBundledItemModel(ResourceLocation itemId) {
\t\tString resourcePath = "assets/" + itemId.getNamespace()
\t\t\t\t+ "/models/item/" + itemId.getPath() + ".json";
\t\treturn Scp294DrinkManager.class.getClassLoader().getResource(resourcePath) != null;
\t}
''',
)

# Players retain the complete forward camera hemisphere. Generic mobs now need
# to look directly at the statue instead of freezing it from a 180-degree arc.
replace_once(
    "src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java",
    '''    // The client camera is authoritative for whether SCP-173 is on screen. A
    // zero threshold deliberately covers the complete forward hemisphere so
    // every visible screen edge remains safe at any configured field of view.
    private static final double OBSERVED_DOT_THRESHOLD = 0.0D;
''',
    '''    // The client camera remains authoritative for players and intentionally
    // covers the complete forward hemisphere. Generic mobs, however, must face
    // the statue directly instead of freezing it from a broad 180-degree arc.
    private static final double PLAYER_OBSERVED_DOT_THRESHOLD = 0.0D;
    private static final double MOB_OBSERVED_DOT_THRESHOLD = 0.8660254037844386D;
    private static final double SCP_131_OBSERVED_DOT_THRESHOLD = 0.70D;
''',
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java",
    '''    private boolean isObservedGeometry(LivingEntity observer) {
        Vec3 eye = observer.getEyePosition(1.0F);
        Vec3 look = observer.getViewVector(1.0F).normalize();
        return isObservedGeometry(eye, look);
    }

    private boolean isObservedGeometry(Vec3 eye, Vec3 look) {
''',
    '''    private boolean isObservedGeometry(LivingEntity observer) {
        Vec3 eye = observer.getEyePosition(1.0F);
        Vec3 look = observer.getViewVector(1.0F).normalize();
        double threshold = observer instanceof Player
                ? PLAYER_OBSERVED_DOT_THRESHOLD
                : observer instanceof AbstractScp131Entity
                        ? SCP_131_OBSERVED_DOT_THRESHOLD
                        : MOB_OBSERVED_DOT_THRESHOLD;
        return isObservedGeometry(eye, look, threshold);
    }

    private boolean isObservedGeometry(Vec3 eye, Vec3 look) {
        return isObservedGeometry(eye, look, PLAYER_OBSERVED_DOT_THRESHOLD);
    }

    private boolean isObservedGeometry(Vec3 eye, Vec3 look, double dotThreshold) {
''',
)
replace_all(
    "src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java",
    "isVisibleSample(eye, look, ",
    "isVisibleSample(eye, look, dotThreshold, ",
)
replace_once(
    "src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java",
    '''    private boolean isVisibleSample(Vec3 eye, Vec3 look, Vec3 point) {
        Vec3 toPoint = point.subtract(eye);
        double distance = toPoint.length();
        if (distance <= 0.001D) return true;
        double dot = look.dot(toPoint.scale(1.0D / distance));
        return dot >= OBSERVED_DOT_THRESHOLD && hasVisualLineOfSightThroughTransparentBlocks(eye, point);
    }
''',
    '''    private boolean isVisibleSample(Vec3 eye, Vec3 look, double dotThreshold, Vec3 point) {
        Vec3 toPoint = point.subtract(eye);
        double distance = toPoint.length();
        if (distance <= 0.001D) return true;
        double dot = look.dot(toPoint.scale(1.0D / distance));
        return dot >= dotThreshold && hasVisualLineOfSightThroughTransparentBlocks(eye, point);
    }
''',
)

# Exact model footprint: controller block plus one block in every direction.
# That is the requested 3x3x3 volume and matches the model's -1..+2 Y limits.
write(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateVolume.java",
    '''package net.mcreator.scpadditions.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Shared physical footprint for Tesla Gate detection and lethal discharge. */
public final class TeslaGateVolume {
    private TeslaGateVolume() {
    }

    public static AABB at(double x, double y, double z) {
        BlockPos controller = BlockPos.containing(x, y, z);
        return new AABB(controller).inflate(1.0D);
    }

    public static boolean intersects(Entity entity, AABB volume) {
        return entity != null && entity.isAlive()
                && entity.getBoundingBox().intersects(volume);
    }
}
''',
)
write(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java",
    '''package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

public class TeslaGateUpdateTickProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        boolean manualOverride = world.getLevelData().getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean teslaGateOn = world.getLevelData().getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
        if (manualOverride && !teslaGateOn
                && world instanceof Level level && !level.isClientSide()) {
            world.getLevelData().getGameRules()
                    .getRule(ScpAdditionsModGameRules.TESLAGATEON)
                    .set(true, level.getServer());
            teslaGateOn = true;
        }
        if (!teslaGateOn && !manualOverride) {
            return;
        }

        int activationDelay = manualOverride ? 1 : 5;
        ResourceLocation activationSound = new ResourceLocation(
                "scp_additions", manualOverride ? "overcharge" : "teslaactivate");
        float activationVolume = manualOverride ? 2.0F : 1.0F;

        AABB volume = TeslaGateVolume.at(x, y, z);
        if (world.getEntitiesOfClass(LivingEntity.class, volume,
                entity -> TeslaGateVolume.intersects(entity, volume)).isEmpty()) {
            return;
        }

        if (world instanceof Level level) {
            if (!level.isClientSide()) {
                level.playSound(null, BlockPos.containing(x, y, z),
                        ForgeRegistries.SOUND_EVENTS.getValue(activationSound),
                        SoundSource.HOSTILE, activationVolume,
                        manualOverride ? 1.25F : 1.0F);
            } else {
                level.playLocalSound(x, y, z,
                        ForgeRegistries.SOUND_EVENTS.getValue(activationSound),
                        SoundSource.HOSTILE, activationVolume,
                        manualOverride ? 1.25F : 1.0F, false);
            }
        }
        ScpAdditionsMod.queueServerWork(activationDelay,
                () -> TeslaGateTransitionHelper.transitionIfCurrent(
                        world, x, y, z,
                        ScpAdditionsModBlocks.TESLA_GATE,
                        ScpAdditionsModBlocks.TESLA_ACTIVE));
    }
}
''',
)
write(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGatePulseHelper.java",
    '''package net.mcreator.scpadditions.procedures;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.List;
import java.util.function.Supplier;

public final class TeslaGatePulseHelper {
    private static final float LETHAL_DAMAGE = 200.0F;

    private TeslaGatePulseHelper() {
    }

    public static void pulseAndTransition(LevelAccessor world, double x, double y,
            double z, Supplier<? extends Block> expectedBlock,
            Supplier<? extends Block> nextBlock) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (world.getBlockState(pos).getBlock() != expectedBlock.get()) {
            return;
        }

        boolean manualOverride = world.getLevelData().getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
        if (manualOverride) {
            emitOverrideParticles(world, x, y, z);
        }

        AABB volume = TeslaGateVolume.at(x, y, z);
        List<LivingEntity> entities = world.getEntitiesOfClass(
                LivingEntity.class, volume,
                entity -> TeslaGateVolume.intersects(entity, volume));
        for (LivingEntity living : entities) {
            living.hurt(new DamageSource(living.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.GENERIC)) {
                @Override
                public Component getLocalizedDeathMessage(
                        LivingEntity messageEntity) {
                    String translateKey = "death.attack.teslagate";
                    if (this.getEntity() == null
                            && this.getDirectEntity() == null) {
                        return messageEntity.getKillCredit() != null
                                ? Component.translatable(
                                        translateKey + ".player",
                                        messageEntity.getDisplayName(),
                                        messageEntity.getKillCredit().getDisplayName())
                                : Component.translatable(translateKey,
                                        messageEntity.getDisplayName());
                    }
                    Component component = this.getEntity() == null
                            ? this.getDirectEntity().getDisplayName()
                            : this.getEntity().getDisplayName();
                    ItemStack itemStack = ItemStack.EMPTY;
                    if (this.getEntity() instanceof LivingEntity sourceLiving) {
                        itemStack = sourceLiving.getMainHandItem();
                    }
                    return !itemStack.isEmpty() && itemStack.hasCustomHoverName()
                            ? Component.translatable(translateKey + ".item",
                                    messageEntity.getDisplayName(), component,
                                    itemStack.getDisplayName())
                            : Component.translatable(translateKey,
                                    messageEntity.getDisplayName(), component);
                }
            }, LETHAL_DAMAGE);

            if (living instanceof ServerPlayer player) {
                Advancement advancement = player.server.getAdvancements()
                        .getAdvancement(new ResourceLocation("scp_additions:tesla"));
                if (advancement != null) {
                    AdvancementProgress progress = player.getAdvancements()
                            .getOrStartProgress(advancement);
                    if (!progress.isDone()) {
                        for (String criteria : progress.getRemainingCriteria()) {
                            player.getAdvancements().award(advancement, criteria);
                        }
                    }
                }
            }
        }

        ScpAdditionsMod.queueServerWork(manualOverride ? 1 : 3,
                () -> TeslaGateTransitionHelper.transitionIfCurrent(
                        world, x, y, z, expectedBlock, nextBlock));
    }

    private static void emitOverrideParticles(LevelAccessor world,
            double x, double y, double z) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                x + 0.5D, y + 1.05D, z + 0.5D,
                8, 0.45D, 0.55D, 0.45D, 0.03D);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                x + 0.5D, y + 0.95D, z + 0.5D,
                2, 0.35D, 0.30D, 0.35D, 0.01D);
    }
}
''',
)

lang_path = ROOT / "src/main/resources/assets/scp_additions/lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.setdefault("advancements.scp_572_a_achievement.title", "The Chosen One")
new_lang = json.dumps(lang, indent=4, ensure_ascii=False) + "\n"
if new_lang != lang_path.read_text(encoding="utf-8"):
    lang_path.write_text(new_lang, encoding="utf-8")
    changed.append(str(lang_path.relative_to(ROOT)))

context_path = ROOT / "config/scpinventory/context_interactions.json"
context = json.loads(context_path.read_text(encoding="utf-8"))
entries = context.get("interactions", [])
filtered = [
    entry for entry in entries
    if not (
        isinstance(entry, dict)
        and str(entry.get("type", "")).lower() == "entity"
        and entry.get("id") == "gas_mask:scp_1499"
    )
]
if filtered != entries:
    context["interactions"] = filtered
    context_path.write_text(
        json.dumps(context, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8")
    changed.append(str(context_path.relative_to(ROOT)))

print(f"Gameplay hotfix changed {len(changed)} files")
for rel in changed:
    print(rel)
