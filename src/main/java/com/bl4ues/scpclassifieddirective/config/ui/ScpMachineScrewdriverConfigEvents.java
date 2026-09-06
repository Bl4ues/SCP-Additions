package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.inventory.network.MachineConfigDirectOpenPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Structure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Screwdriver shortcut from physical SCP-294/SCP-914 to their native editors. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScpMachineScrewdriverConfigEvents {
    public static final String SCP_294_SECTION = "SCP-294 Drinks";
    public static final String SCP_914_SECTION = "SCP-914 Recipes";

    private ScpMachineScrewdriverConfigEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || KeycardReaderInteractionEvents.screwdriver(player).isEmpty()) {
            return;
        }

        String section = sectionFor(level, event.getPos());
        if (section == null) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!ConfigCenterService.canEdit(player)) {
            player.displayClientMessage(Component.literal(
                    "Insufficient permission to edit server configuration.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MachineConfigDirectOpenPacket(section));
        ConfigCenterNetwork.openFor(player, ModNetwork.CHANNEL);
    }

    private static String sectionFor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ScpClassifiedDirectiveModBlocks.SCP_294.get())) {
            return SCP_294_SECTION;
        }
        if (state.is(Scp914Module.SCP_914.get())) {
            return SCP_914_SECTION;
        }
        if (!state.is(Scp914Module.SCP_914_RESERVATION.get())
                && !state.is(Scp914Module.SCP_914_COLLISION.get())
                && !state.is(Scp914Module.SCP_914_DOOR_COLLISION.get())) {
            return null;
        }
        return Scp914Structure.findController(level, pos, state) == null
                ? null : SCP_914_SECTION;
    }
}
