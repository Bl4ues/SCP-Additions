package net.mcreator.scpadditions.keycard;

import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.UnifiedReaderItems;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KeycardReaderInteractionEvents {
    public static final String SAVED_LEVEL_TAG = "ScpAdditionsKeycardReaderLevel";
    private static final Map<UUID, SuppressedInteraction> SUPPRESSED_INTERACTIONS = new HashMap<>();

    private KeycardReaderInteractionEvents() {
    }

    public static boolean tryOpenConfiguration(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null) {
            return false;
        }

        boolean hasScrewdriver = player.getMainHandItem().is(UnifiedReaderItems.SCREWDRIVER.get())
                || player.getOffhandItem().is(UnifiedReaderItems.SCREWDRIVER.get());
        if (!hasScrewdriver) {
            return false;
        }

        KeycardReaderLevels.ReaderDescriptor descriptor = KeycardReaderLevels.describe(
                player.level().getBlockState(pos));
        if (descriptor == null) {
            return false;
        }

        ScpEntityNetwork.openKeycardReaderScreen(player, pos, descriptor.level());
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

        KeycardReaderLevels.ReaderDescriptor descriptor = KeycardReaderLevels.describe(
                event.getLevel().getBlockState(event.getPos()));
        if (descriptor == null) {
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
            if (serverPlayer.isShiftKeyDown()) {
                ItemStack screwdriver = screwdriver(serverPlayer);
                screwdriver.getOrCreateTag().putInt(SAVED_LEVEL_TAG, descriptor.level());
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.scp_additions.keycard_reader_level_copied", descriptor.level())
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                ScpEntityNetwork.openKeycardReaderScreen(serverPlayer, event.getPos(), descriptor.level());
            }
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
