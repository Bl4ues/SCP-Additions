package com.bl4ues.scpclassifieddirective.data;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Reusable SCP-914 transformation operations for the rebuilt physical machine. */
public final class Scp914Processor {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "scp914"));

    private Scp914Processor() {
    }

    public static void processPlayer(ServerPlayer player, Vec3 outputCenter,
            Scp914RecipeManager.Setting setting) {
        if (!isAvailable(player)) return;

        player.connection.teleport(outputCenter.x, outputCenter.y, outputCenter.z,
                player.getYRot(), player.getXRot());

        switch (setting) {
            case ROUGH -> {
                hurtWithMessage(player, 18.0F, "scp914rough");
                ScpClassifiedDirectiveMod.queueServerWork(10, () -> {
                    if (isAvailable(player)) hurtWithMessage(player, 50.0F, "scp914rough");
                });
            }
            case COARSE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        200, 3, false, false));
                hurtWithMessage(player, 18.0F, "scp914coarse");
                ScpClassifiedDirectiveMod.queueServerWork(200, () -> {
                    if (isAvailable(player)) hurtWithMessage(player, 50.0F, "scp914coarse");
                });
            }
            case ONE_TO_ONE -> {
                Scp914SkinManager.assignRandomSkin(player);
                awardMetamorphosisAdvancement(player);
            }
            case FINE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        200, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP,
                        200, 1, false, false));
                ScpClassifiedDirectiveMod.queueServerWork(200, () -> {
                    if (isAvailable(player)) hurtWithMessage(player, 50.0F, "scp914fine");
                });
            }
            case VERY_FINE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        300, 5, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP,
                        300, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST,
                        300, 7, false, false));
                ScpClassifiedDirectiveMod.queueServerWork(300, () -> {
                    if (isAvailable(player)) hurtWithMessage(player, 80.0F, "scp914veryfine");
                });
            }
        }
    }

    private static boolean isAvailable(ServerPlayer player) {
        return player != null && !player.isRemoved() && player.isAlive()
                && player.connection != null;
    }

    private static void hurtWithMessage(ServerPlayer player, float amount,
            String translationKey) {
        var damageRegistry = player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE);
        var genericType = damageRegistry.getHolderOrThrow(DamageTypes.GENERIC);
        DamageSource source = new DamageSource(
                damageRegistry.getHolderOrThrow(DAMAGE_TYPE)) {
            @Override
            public boolean is(TagKey<DamageType> tag) {
                return super.is(tag) || genericType.is(tag);
            }

            @Override
            public Component getLocalizedDeathMessage(LivingEntity entity) {
                return Component.translatable("death.attack." + translationKey,
                        entity.getDisplayName());
            }
        };
        boolean wasAlive = player.isAlive();
        boolean damaged = player.hurt(source, amount);
        if (damaged && wasAlive && player.isDeadOrDying()
                && !"scp914coarse".equals(translationKey)) {
            player.level().playSound(null, player.blockPosition(),
                    ScpClassifiedDirectiveModSounds.SCP914DEATH.get(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private static void awardMetamorphosisAdvancement(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        Advancement advancement = server.getAdvancements().getAdvancement(
                new ResourceLocation("scp_classified_directive", "scp_914_metamorphosis"));
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements()
                .getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    public static void applyRecipe(ServerLevel level, Vec3 outputCenter,
            Scp914RecipeManager.RecipeMatch match) {
        if (level.random.nextFloat() > match.recipe().chance()) {
            consumeInputs(level, outputCenter, match);
            return;
        }

        ItemStack firstInputStack = match.firstInputStack();
        consumeInputs(level, outputCenter, match);

        for (Scp914RecipeManager.ItemOutput output :
                Scp914RecipeManager.rollItemOutputs(match.recipe(), level.random)) {
            ItemStack outputStack = Scp914RecipeManager.createItemOutput(
                    output, firstInputStack, match.recipe().copyInputNbt());
            if (!outputStack.isEmpty()) {
                ItemEntity outputEntity = new ItemEntity(level, outputCenter.x,
                        outputCenter.y, outputCenter.z, outputStack);
                outputEntity.setPickUpDelay(10);
                level.addFreshEntity(outputEntity);
            }
        }

        for (Scp914RecipeManager.EntityOutput output : match.recipe().entityOutputs()) {
            Optional<EntityType<?>> type = Scp914RecipeManager.getEntityType(output);
            if (type.isEmpty()) {
                ScpClassifiedDirectiveMod.LOGGER.warn(
                        "SCP-914 recipe {} points to missing entity output {}",
                        match.recipe().id(), output.entity());
                continue;
            }
            for (int i = 0; i < output.count(); i++) {
                Entity spawned = type.get().spawn(level,
                        BlockPos.containing(outputCenter), MobSpawnType.MOB_SUMMONED);
                if (spawned != null) spawned.setDeltaMovement(0, 0, 0);
            }
        }
    }

    private static void consumeInputs(ServerLevel level, Vec3 outputCenter,
            Scp914RecipeManager.RecipeMatch match) {
        for (Scp914RecipeManager.ItemUse itemUse : match.itemUses()) {
            ItemStack stack = itemUse.entity().getItem();
            stack.shrink(itemUse.count());
            if (stack.isEmpty()) itemUse.entity().discard();
            else itemUse.entity().setItem(stack);
        }

        boolean destructiveEntityPass = isInferredDestructivePass(match);
        for (Scp914RecipeManager.EntityUse entityUse : match.entityUses()) {
            if (!entityUse.consume()) continue;
            Entity entity = entityUse.entity();
            if (entity == null || entity.isRemoved()) continue;
            if (destructiveEntityPass) {
                killEntityAtOutput(level, outputCenter, entity);
            } else {
                entity.discard();
            }
        }
    }

    /**
     * Rough and Coarse are material-recovery settings. For inferred entity
     * inputs the entity is physically killed at the output chamber instead of
     * simply being discarded, allowing its own loot table, Forge hooks and
     * modded death behavior to determine the recovered drops.
     */
    private static void killEntityAtOutput(ServerLevel level, Vec3 outputCenter,
            Entity entity) {
        entity.teleportTo(outputCenter.x, outputCenter.y, outputCenter.z);
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof LivingEntity living) {
            living.setInvulnerable(false);
            DamageSource source = level.damageSources().generic();
            living.hurt(source, Float.MAX_VALUE);
            if (living.isAlive() && !living.isRemoved()) living.kill();
        } else {
            entity.kill();
        }
    }

    private static boolean isInferredDestructivePass(
            Scp914RecipeManager.RecipeMatch match) {
        ResourceLocation id = match.recipe().id();
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(
                id.getNamespace())) return false;
        String path = id.getPath();
        return path.startsWith("inferred/rough/")
                || path.startsWith("inferred/coarse/");
    }

    public static void consumeLooseItems(List<ItemEntity> items) {
        for (ItemEntity item : items) {
            if (item != null && !item.isRemoved()) item.discard();
        }
    }
}
