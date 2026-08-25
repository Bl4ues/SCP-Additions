package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Applies the common durability rules for Foundation facility infrastructure.
 *
 * <p>Public structures, props, panels, readers, elevator blocks and every
 * animated door state share an effective hardness of 37.5. An iron pickaxe has
 * speed 6, while a diamond pickaxe has speed 8 and obsidian has hardness 50;
 * therefore iron mines these blocks in the same time diamond mines obsidian.
 * Iron tier or better is mandatory.</p>
 *
 * <p>Explosion lists discard Foundation blocks before destruction. Manual door
 * families remain vulnerable, while panel-operated heavy doors are protected.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class FacilityBlockMiningEvents {
    private static final float FOUNDATION_EFFECTIVE_HARDNESS = 37.5F;
    private static final int MINIMUM_PICKAXE_LEVEL = Tiers.IRON.getLevel();

    private static volatile Set<Block> facilityBlocks;

    private FacilityBlockMiningEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        Player player = event.getEntity();
        if (state == null || player == null) {
            return;
        }

        BlockPos position = event.getPosition().orElse(player.blockPosition());
        FacilityStructureBreakGuard.observeMining(player.level(), position,
                state);

        if (player.isCreative()
                || !facilityBlocks().contains(state.getBlock())) {
            return;
        }

        float registeredHardness = state.getDestroySpeed(player.level(),
                position);
        if (registeredHardness < 0.0F) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (!isIronTierPickaxe(tool)) {
            event.setNewSpeed(0.0F);
            return;
        }

        int efficiency = EnchantmentHelper.getBlockEfficiency(player);
        float stateToolSpeed = withEfficiency(
                Math.max(1.0F, tool.getDestroySpeed(state)), efficiency);
        float referencePickaxeSpeed = withEfficiency(
                Math.max(1.0F, tool.getDestroySpeed(
                        Blocks.STONE.defaultBlockState())), efficiency);

        float environmentMultiplier = stateToolSpeed > 0.0F
                ? event.getOriginalSpeed() / stateToolSpeed : 1.0F;
        float normalizedSpeed = referencePickaxeSpeed * environmentMultiplier
                * registeredHardness / FOUNDATION_EFFECTIVE_HARDNESS;
        event.setNewSpeed(Math.max(0.0F, normalizedSpeed));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        Set<Block> protectedBlocks = facilityBlocks();
        event.getAffectedBlocks().removeIf(position -> {
            BlockState state = level.getBlockState(position);
            return protectedBlocks.contains(state.getBlock())
                    && !isManualDoor(state);
        });
    }

    private static boolean isIronTierPickaxe(ItemStack tool) {
        return tool.canPerformAction(ToolActions.PICKAXE_DIG)
                && tool.getItem() instanceof TieredItem tiered
                && tiered.getTier().getLevel() >= MINIMUM_PICKAXE_LEVEL;
    }

    private static float withEfficiency(float baseSpeed, int efficiency) {
        if (baseSpeed > 1.0F && efficiency > 0) {
            return baseSpeed + efficiency * efficiency + 1.0F;
        }
        return baseSpeed;
    }

    private static boolean isManualDoor(BlockState state) {
        if (!FacilityModule.isFacilityDoor(state)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return !path.startsWith("default_door")
                && !path.startsWith("default_clos_")
                && !path.startsWith("yellow_")
                && !path.startsWith("black_");
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
                addRegistered(blocks, TeslaGateTerminalTableModule.BLOCKS);
                addRegistered(blocks, MirroredDoorButtons.BLOCKS);
                addRegistered(blocks, LeftDoorButtons.BLOCKS);
                addRegistered(blocks, CoreRoomElevatorModule.BLOCKS);
                addRegistered(blocks, ObjectContainmentUnitModule.BLOCKS);
                blocks.add(DocumentHolderModule.block());
                addGeneratedFacilityBlocks(blocks);
                addRegisteredBlock(blocks, "tesla_gate_collision");
                addRegisteredBlock(blocks, "decontamination_collision");
                facilityBlocks = Collections.unmodifiableSet(blocks);
            }
            return facilityBlocks;
        }
    }

    private static void addGeneratedFacilityBlocks(Set<Block> target) {
        ScpClassifiedDirectiveModBlocks.REGISTRY.getEntries().forEach(entry -> {
            String path = entry.getId().getPath();
            if (isGeneratedFacilityPath(path)) {
                target.add(entry.get());
            }
        });
    }

    private static boolean isGeneratedFacilityPath(String path) {
        return path.startsWith("button_")
                || path.startsWith("tesla_")
                || path.startsWith("decon_")
                || path.contains("reader")
                || path.equals("scp_079_system_control")
                || path.equals("scp_079_auxiliary_power")
                || path.equals("scp_079control")
                || path.equals("scp_079controloff");
    }

    private static void addRegisteredBlock(Set<Block> target, String path) {
        Block block = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path));
        if (block != null && block != Blocks.AIR) {
            target.add(block);
        }
    }

    private static void addRegistered(Set<Block> target,
            DeferredRegister<Block> registry) {
        registry.getEntries().forEach(entry -> target.add(entry.get()));
    }
}
