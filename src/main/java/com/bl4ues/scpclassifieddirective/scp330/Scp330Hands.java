package com.bl4ues.scpclassifieddirective.scp330;

import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.advancement.ScpAdvancementAwards;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModMobEffects;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;

import java.util.List;
import java.util.UUID;

/** Authoritative SCP-330 candy limit and hand-loss state. */
public final class Scp330Hands {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "scp330"));
    public static final String DISABLED_TAG = "scp_classified_directive.scp330_hands_lost";
    private static final String COUNT_TAG = "scp_classified_directive.scp330_candies_taken";
    private static final int DEATH_DELAY_TICKS = 140;

    private Scp330Hands() {
    }

    public static boolean isDisabled(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) {
            return player.hasEffect(ScpClassifiedDirectiveModMobEffects.SCP_330_HAND_LOSS.get());
        }
        return player.getPersistentData().getBoolean(DISABLED_TAG);
    }

    public static boolean takeCandy(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || player == null) {
            return false;
        }

        play(serverLevel, pos, ScpClassifiedDirectiveModSounds.CANDY.get());

        // Creative mode is an authoring/testing context, not a containment
        // challenge. It may interact with SCP-330 freely without advancing the
        // persistent two-candy counter or ever entering the hand-loss state.
        if (player.isCreative()) {
            ItemHandlerHelper.giveItemToPlayer(player,
                    new ItemStack(randomCandy(serverLevel)));
            return true;
        }

        if (isDisabled(player)) return false;

        int taken = Math.max(0, player.getPersistentData().getInt(COUNT_TAG));
        if (taken < 2) {
            ItemHandlerHelper.giveItemToPlayer(player,
                    new ItemStack(randomCandy(serverLevel)));
            int nextTaken = taken + 1;
            player.getPersistentData().putInt(COUNT_TAG, nextTaken);
            if (nextTaken == 2 && player instanceof ServerPlayer serverPlayer) {
                ScpAdvancementAwards.award(serverPlayer,
                        ScpAdvancementAwards.SWEET_TOOTH);
            }
            return true;
        }

        severHands(serverLevel, pos, player);
        return true;
    }

    public static void maintain(Player player) {
        if (player == null || player.level().isClientSide()
                || !player.getPersistentData().getBoolean(DISABLED_TAG)) return;
        if (!player.hasEffect(ScpClassifiedDirectiveModMobEffects.SCP_330_HAND_LOSS.get())) {
            player.addEffect(new MobEffectInstance(
                    ScpClassifiedDirectiveModMobEffects.SCP_330_HAND_LOSS.get(),
                    Integer.MAX_VALUE, 0, false, false, false));
        }
        if (player.containerMenu != player.inventoryMenu) player.closeContainer();
    }

    public static void resetAfterDeath(Player player) {
        if (player == null) return;
        player.getPersistentData().remove(DISABLED_TAG);
        player.getPersistentData().putInt(COUNT_TAG, 0);
        player.getPersistentData().remove("candy0");
        player.getPersistentData().remove("candy1");
        player.getPersistentData().remove("candy2");
        player.removeEffect(ScpClassifiedDirectiveModMobEffects.SCP_330_HAND_LOSS.get());
    }

    private static void severHands(ServerLevel level, BlockPos pos, Player player) {
        if (player.isCreative()) return;
        player.getPersistentData().putBoolean(DISABLED_TAG, true);
        player.addEffect(new MobEffectInstance(
                ScpClassifiedDirectiveModMobEffects.SCP_330_HAND_LOSS.get(),
                Integer.MAX_VALUE, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                DEATH_DELAY_TICKS, 4, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                DEATH_DELAY_TICKS, 1, false, false));
        player.closeContainer();
        removeCandies(player);
        play(level, pos, ScpClassifiedDirectiveModSounds.SCP330DEATH.get());
        player.hurt(damageSource(player), 10.0F);

        UUID playerId = player.getUUID();
        ScpClassifiedDirectiveMod.queueServerWork(DEATH_DELAY_TICKS, () -> {
            ServerPlayer current = level.getServer().getPlayerList().getPlayer(playerId);
            if (current != null && current.isAlive()
                    && current.getPersistentData().getBoolean(DISABLED_TAG)) {
                current.hurt(damageSource(current), 1000.0F);
            }
        });
    }

    private static Item randomCandy(ServerLevel level) {
        return switch (level.random.nextInt(3)) {
            case 0 -> ScpClassifiedDirectiveModItems.SCP_330_BLUE_CANDY.get();
            case 1 -> ScpClassifiedDirectiveModItems.SCP_330_PINK_CANDY.get();
            default -> ScpClassifiedDirectiveModItems.SCP_330_YELLOW_CANDY.get();
        };
    }

    private static void removeCandies(Player player) {
        List<Item> candies = List.of(
                ScpClassifiedDirectiveModItems.SCP_330_BLUE_CANDY.get(),
                ScpClassifiedDirectiveModItems.SCP_330_PINK_CANDY.get(),
                ScpClassifiedDirectiveModItems.SCP_330_YELLOW_CANDY.get());
        player.getInventory().clearOrCountMatchingItems(
                stack -> candies.contains(stack.getItem()), -1,
                player.inventoryMenu.getCraftSlots());
    }

    private static DamageSource damageSource(LivingEntity entity) {
        var damageRegistry = entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE);
        var genericType = damageRegistry.getHolderOrThrow(DamageTypes.GENERIC);
        return new DamageSource(damageRegistry.getHolderOrThrow(DAMAGE_TYPE)) {
            @Override
            public boolean is(TagKey<DamageType> tag) {
                return super.is(tag) || genericType.is(tag);
            }

            @Override
            public Component getLocalizedDeathMessage(LivingEntity victim) {
                return Component.translatable("death.attack.scp330",
                        victim.getDisplayName());
            }
        };
    }

    private static void play(ServerLevel level, BlockPos pos,
            net.minecraft.sounds.SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F,
                0.95F + level.random.nextFloat() * 0.1F);
    }
}
