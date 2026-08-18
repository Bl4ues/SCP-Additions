package net.mcreator.scpadditions.scp330;

import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.advancement.ScpAdvancementAwards;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.util.List;
import java.util.UUID;

/** Authoritative SCP-330 candy limit and hand-loss state. */
public final class Scp330Hands {
    public static final String DISABLED_TAG = "scp_additions.scp330_hands_lost";
    private static final String COUNT_TAG = "scp_additions.scp330_candies_taken";
    private static final int DEATH_DELAY_TICKS = 140;

    private Scp330Hands() {
    }

    public static boolean isDisabled(Player player) {
        if (player == null) return false;
        if (player.level().isClientSide()) {
            return player.hasEffect(ScpAdditionsModMobEffects.SCP_330_HAND_LOSS.get());
        }
        return player.getPersistentData().getBoolean(DISABLED_TAG);
    }

    public static boolean takeCandy(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || player == null) {
            return false;
        }

        play(serverLevel, pos, ScpAdditionsModSounds.CANDY.get());

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
        if (!player.hasEffect(ScpAdditionsModMobEffects.SCP_330_HAND_LOSS.get())) {
            player.addEffect(new MobEffectInstance(
                    ScpAdditionsModMobEffects.SCP_330_HAND_LOSS.get(),
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
        player.removeEffect(ScpAdditionsModMobEffects.SCP_330_HAND_LOSS.get());
    }

    private static void severHands(ServerLevel level, BlockPos pos, Player player) {
        if (player.isCreative()) return;
        player.getPersistentData().putBoolean(DISABLED_TAG, true);
        player.addEffect(new MobEffectInstance(
                ScpAdditionsModMobEffects.SCP_330_HAND_LOSS.get(),
                Integer.MAX_VALUE, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                DEATH_DELAY_TICKS, 4, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                DEATH_DELAY_TICKS, 1, false, false));
        player.closeContainer();
        removeCandies(player);
        play(level, pos, ScpAdditionsModSounds.SCP330DEATH.get());
        player.hurt(damageSource(player), 10.0F);

        UUID playerId = player.getUUID();
        ScpAdditionsMod.queueServerWork(DEATH_DELAY_TICKS, () -> {
            ServerPlayer current = level.getServer().getPlayerList().getPlayer(playerId);
            if (current != null && current.isAlive()
                    && current.getPersistentData().getBoolean(DISABLED_TAG)) {
                current.hurt(damageSource(current), 1000.0F);
            }
        });
    }

    private static Item randomCandy(ServerLevel level) {
        return switch (level.random.nextInt(3)) {
            case 0 -> ScpAdditionsModItems.SCP_330_BLUE_CANDY.get();
            case 1 -> ScpAdditionsModItems.SCP_330_PINK_CANDY.get();
            default -> ScpAdditionsModItems.SCP_330_YELLOW_CANDY.get();
        };
    }

    private static void removeCandies(Player player) {
        List<Item> candies = List.of(
                ScpAdditionsModItems.SCP_330_BLUE_CANDY.get(),
                ScpAdditionsModItems.SCP_330_PINK_CANDY.get(),
                ScpAdditionsModItems.SCP_330_YELLOW_CANDY.get());
        player.getInventory().clearOrCountMatchingItems(
                stack -> candies.contains(stack.getItem()), -1,
                player.inventoryMenu.getCraftSlots());
    }

    private static DamageSource damageSource(LivingEntity entity) {
        return new DamageSource(entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.GENERIC)) {
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
