package com.bl4ues.scpclassifieddirective.keycard;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;
import com.bl4ues.scpclassifieddirective.network.ScpEntityNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KeycardReaderInteractionEvents {
    public static final String SAVED_LEVEL_TAG = "ScpClassifiedDirectiveKeycardReaderLevel";
    private static final Map<UUID, SuppressedInteraction> SUPPRESSED_INTERACTIONS = new HashMap<>();

    private KeycardReaderInteractionEvents() {
    }

    public static boolean tryOpenConfiguration(ServerPlayer player, BlockPos pos) {
        return tryHandleInteraction(player, pos, false, false);
    }

    /**
     * Returns the configurable authorization level for either a wall reader or
     * another device that deliberately reuses the reader configuration UI.
     */
    public static int configurableLevel(Level level, BlockPos pos) {
        if (level == null || pos == null) return 0;

        KeycardReaderLevels.ReaderDescriptor descriptor =
                KeycardReaderLevels.describe(level.getBlockState(pos));
        if (descriptor != null) return descriptor.level();

        if (level.getBlockEntity(pos)
                instanceof ObjectContainmentUnitModule.UnitBlockEntity unit) {
            return unit.requiredLevel();
        }
        return 0;
    }

    /** Applies a level without replacing devices that store it in block-entity data. */
    public static boolean applyConfigurableLevel(Level level, BlockPos pos,
            int requestedLevel) {
        if (level == null || pos == null || requestedLevel < 1
                || requestedLevel > 6) {
            return false;
        }

        KeycardReaderLevels.ReaderDescriptor descriptor =
                KeycardReaderLevels.describe(level.getBlockState(pos));
        if (descriptor != null) {
            return descriptor.level() == requestedLevel
                    || KeycardReaderLevels.replaceLevel(level, pos,
                            requestedLevel);
        }

        if (level.getBlockEntity(pos)
                instanceof ObjectContainmentUnitModule.UnitBlockEntity unit) {
            unit.setRequiredLevel(requestedLevel);
            return true;
        }
        return false;
    }

    public static boolean tryHandleInteraction(ServerPlayer player, BlockPos pos,
            boolean shiftDown, boolean controlDown) {
        if (player == null || pos == null) {
            return false;
        }

        ItemStack screwdriver = screwdriver(player);
        if (screwdriver.isEmpty()) {
            return false;
        }

        int currentLevel = configurableLevel(player.level(), pos);
        if (currentLevel == 0) {
            return false;
        }

        if (controlDown) {
            int savedLevel = screwdriver.hasTag()
                    ? screwdriver.getTag().getInt(SAVED_LEVEL_TAG) : 0;
            if (savedLevel < 1 || savedLevel > 6) {
                player.displayClientMessage(Component.translatable(
                        "message.scp_classified_directive.keycard_reader_no_saved_level")
                        .withStyle(ChatFormatting.RED), true);
            } else if (currentLevel == savedLevel
                    || applyConfigurableLevel(player.level(), pos, savedLevel)) {
                player.displayClientMessage(Component.translatable(
                        "message.scp_classified_directive.keycard_reader_level_applied", savedLevel)
                        .withStyle(ChatFormatting.GREEN), true);
            }
        } else if (shiftDown) {
            screwdriver.getOrCreateTag().putInt(SAVED_LEVEL_TAG, currentLevel);
            player.displayClientMessage(Component.translatable(
                    "message.scp_classified_directive.keycard_reader_level_copied", currentLevel)
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            ScpEntityNetwork.openKeycardReaderScreen(player, pos, currentLevel);
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickReader(PlayerInteractEvent.RightClickBlock event) {
        // Process once through the main-hand event, while allowing the tool to
        // physically be held in either hand.
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        boolean hasScrewdriver = event.getEntity().getMainHandItem().is(UnifiedReaderItems.SCREWDRIVER.get())
                || event.getEntity().getOffhandItem().is(UnifiedReaderItems.SCREWDRIVER.get());
        if (!hasScrewdriver) {
            return;
        }

        if (configurableLevel(event.getLevel(), event.getPos()) == 0) {
            return;
        }

        // Stop the normal keycard swipe procedure from running underneath the
        // configuration interaction.
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (consumeSuppressedInteraction(serverPlayer, event.getPos())) return;
            tryHandleInteraction(serverPlayer, event.getPos(), serverPlayer.isShiftKeyDown(), false);
        }
    }

    public static ItemStack screwdriver(net.minecraft.world.entity.player.Player player) {
        if (player.getMainHandItem().is(UnifiedReaderItems.SCREWDRIVER.get())) return player.getMainHandItem();
        if (player.getOffhandItem().is(UnifiedReaderItems.SCREWDRIVER.get())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public static void suppressNextInteraction(ServerPlayer player, BlockPos pos) {
        SUPPRESSED_INTERACTIONS.put(player.getUUID(),
                new SuppressedInteraction(pos.immutable(), player.level().getGameTime() + 1L));
    }

    private static boolean consumeSuppressedInteraction(ServerPlayer player, BlockPos pos) {
        SuppressedInteraction interaction = SUPPRESSED_INTERACTIONS.remove(player.getUUID());
        return interaction != null && interaction.pos().equals(pos)
                && player.level().getGameTime() <= interaction.expiresAt();
    }

    private record SuppressedInteraction(BlockPos pos, long expiresAt) {
    }
}
