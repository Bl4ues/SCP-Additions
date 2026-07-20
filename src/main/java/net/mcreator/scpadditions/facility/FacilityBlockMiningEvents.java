package net.mcreator.scpadditions.facility;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Gives all SCP Unity facility structures, props, doors and panel states
 * brick-equivalent mining resistance while preserving proper pickaxe speed.
 */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class FacilityBlockMiningEvents {
    private static final float BRICK_HARDNESS = 2.0F;
    private static final float WRONG_TO_CORRECT_TOOL_DIVISOR = 100.0F / 30.0F;

    private static volatile Set<Block> facilityBlocks;

    private FacilityBlockMiningEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        Player player = event.getEntity();
        if (state == null || player == null || player.isCreative()
                || !facilityBlocks().contains(state.getBlock())) {
            return;
        }

        BlockPos position = event.getPosition().orElse(player.blockPosition());
        float registeredHardness = state.getDestroySpeed(player.level(), position);
        if (registeredHardness < 0.0F) {
            return;
        }

        // Normalize every facility block to the same effective hardness as
        // vanilla bricks, independent of the legacy value on its block class.
        float normalizedSpeed = event.getOriginalSpeed()
                * registeredHardness / BRICK_HARDNESS;

        ItemStack tool = player.getMainHandItem();
        if (tool.canPerformAction(ItemAbilities.PICKAXE_DIG)) {
            float pickaxeSpeed;
            boolean alreadyRecognized = player.hasCorrectToolForDrops(state);
            if (alreadyRecognized) {
                pickaxeSpeed = event.getOriginalSpeed();
            } else {
                // Most legacy facility blocks predate their mining tags. Derive
                // the same tool speed from stone, then retain haste, fatigue,
                // water and airborne multipliers from the original event speed.
                float stateBaseSpeed = Math.max(1.0F,
                        tool.getDestroySpeed(state));
                float environmentMultiplier = event.getOriginalSpeed()
                        / stateBaseSpeed;
                float referenceSpeed = Math.max(1.0F,
                        tool.getDestroySpeed(Blocks.STONE.defaultBlockState()));
                int efficiency = EnchantmentHelper.getBlockEfficiency(player);
                if (referenceSpeed > 1.0F && efficiency > 0) {
                    referenceSpeed += efficiency * efficiency + 1.0F;
                }
                pickaxeSpeed = referenceSpeed * environmentMultiplier;
            }

            float harvestCompensation = alreadyRecognized
                    ? 1.0F : WRONG_TO_CORRECT_TOOL_DIVISOR;
            normalizedSpeed = Math.max(normalizedSpeed,
                    pickaxeSpeed * registeredHardness / BRICK_HARDNESS
                            * harvestCompensation);
        }

        event.setNewSpeed(Math.max(0.0F, normalizedSpeed));
    }

    private static Set<Block> facilityBlocks() {
        Set<Block> cached = facilityBlocks;
        if (cached != null) {
            return cached;
        }

        synchronized (FacilityBlockMiningEvents.class) {
            if (facilityBlocks == null) {
                Set<Block> blocks = Collections.newSetFromMap(
                        new IdentityHashMap<>());
                addRegistered(blocks, FacilityModule.BLOCKS);
                addRegistered(blocks, UBlocksModule.BLOCKS);
                addRegistered(blocks, MirroredDoorButtons.BLOCKS);
                addRegistered(blocks, LeftDoorButtons.BLOCKS);
                facilityBlocks = Collections.unmodifiableSet(blocks);
            }
            return facilityBlocks;
        }
    }

    private static void addRegistered(Set<Block> target,
            DeferredRegister<Block> registry) {
        registry.getEntries().forEach(entry -> target.add(entry.get()));
    }
}
