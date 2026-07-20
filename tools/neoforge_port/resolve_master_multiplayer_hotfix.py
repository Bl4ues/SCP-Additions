#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
changed: list[str] = []


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, content: str) -> None:
    path = ROOT / rel
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old != content:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        changed.append(rel)


def replace_once(rel: str, old: str, new: str) -> None:
    source = read(rel)
    if new in source:
        return
    if old not in source:
        raise RuntimeError(f"Expected NeoForge source fragment not found in {rel}: {old[:160]!r}")
    write(rel, source.replace(old, new, 1))


# Client-side host snapshot lifetime.
replace_once(
    "src/main/java/com/bl4ues/scpinventory/client/ClientGameplayEvents.java",
    "import net.neoforged.api.distmarker.Dist;\n",
    "import net.neoforged.api.distmarker.Dist;\n"
    "import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;\n"
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

# Context interaction edits are server-authoritative and immediately broadcast.
replace_once(
    "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java",
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\n",
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\n"
    "import net.neoforged.neoforge.server.ServerLifecycleHooks;\n",
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
    "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java",
    '''    static File ensureConfigFile() {
''',
    '''    public static String readConfigJson() {
        try {
            return Files.readString(ensureConfigFile().toPath(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error(
                    "Failed to read context interaction configuration", exception);
            return "{\\"interactions\\":[]}";
        }
    }

    static File ensureConfigFile() {
''',
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
    "src/main/java/com/bl4ues/scpinventory/context/ContextEntityConfigManager.java",
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\n",
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\n"
    "import net.neoforged.neoforge.server.ServerLifecycleHooks;\n",
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

# Preserve wide player-camera observation while requiring configured mobs to
# face SCP-173 directly. SCP-131 receives its own intentional threshold.
replace_once(
    "src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java",
    '''    // The client camera is authoritative for whether SCP-173 is on screen. A
    // zero threshold deliberately covers the complete forward hemisphere so
    // every visible screen edge remains safe at any configured field of view.
    private static final double OBSERVED_DOT_THRESHOLD = 0.0D;
''',
    '''    // Player camera observation covers the full forward hemisphere. Generic
    // mobs must face the statue directly instead of freezing it from behind.
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
        AABB box = getBoundingBox();
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
        AABB box = getBoundingBox();
''',
)
scp_path = "src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java"
scp_text = read(scp_path)
geometry_start = scp_text.index("    private boolean isObservedGeometry(Vec3 eye, Vec3 look, double dotThreshold)")
geometry_end = scp_text.index("    private void reportClientObservation", geometry_start)
geometry = scp_text[geometry_start:geometry_end]
geometry_updated = geometry.replace("isVisibleSample(eye, look, new Vec3(", "isVisibleSample(eye, look, dotThreshold, new Vec3(")
if geometry_updated != geometry:
    write(scp_path, scp_text[:geometry_start] + geometry_updated + scp_text[geometry_end:])
replace_once(
    scp_path,
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

# Exact shared 3x3x3 Tesla volume for both detection and lethal discharge.
write(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java",
    '''package net.mcreator.scpadditions.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
        ResourceLocation activationSound = ResourceLocation.fromNamespaceAndPath(
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
                        BuiltInRegistries.SOUND_EVENT.get(activationSound),
                        SoundSource.HOSTILE, activationVolume,
                        manualOverride ? 1.25F : 1.0F);
            } else {
                level.playLocalSound(x, y, z,
                        BuiltInRegistries.SOUND_EVENT.get(activationSound),
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

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
                    if (this.getEntity() == null && this.getDirectEntity() == null) {
                        return messageEntity.getKillCredit() != null
                                ? Component.translatable(translateKey + ".player",
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
                    return !itemStack.isEmpty()
                            && itemStack.has(DataComponents.CUSTOM_NAME)
                            ? Component.translatable(translateKey + ".item",
                                    messageEntity.getDisplayName(), component,
                                    itemStack.getDisplayName())
                            : Component.translatable(translateKey,
                                    messageEntity.getDisplayName(), component);
                }
            }, LETHAL_DAMAGE);

            if (living instanceof ServerPlayer player) {
                AdvancementHolder advancement = player.server.getAdvancements().get(
                        ResourceLocation.fromNamespaceAndPath("scp_additions", "tesla"));
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

print(f"Resolved {len(changed)} NeoForge multiplayer hotfix files")
for rel in changed:
    print(rel)
