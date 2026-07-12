package net.mcreator.scpadditions.keycard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.UnifiedReaderItems;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KeycardReaderInteractionEvents {
    private KeycardReaderInteractionEvents() {
    }

    public static boolean tryOpenConfiguration(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null || !player.isShiftKeyDown()) {
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickReader(PlayerInteractEvent.RightClickBlock event) {
        // Process once through the main-hand event, while allowing the tool to
        // physically be held in either hand.
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()) {
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
            ScpEntityNetwork.openKeycardReaderScreen(serverPlayer, event.getPos(), descriptor.level());
        }
    }
}
