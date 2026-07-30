package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class FacilityTooltipEvents {
    private static final Set<String> TESLA = Set.of(
            "tesla_bottom", "tesla_mid_1", "tesla_mid_2",
            "tesla_bottom_alt", "tesla_top_alt");
    private static final Map<String, String> SUBLEVEL_ONE_ROOMS = Map.ofEntries(
            Map.entry("archival_bottom", "tooltip.scp_additions.archival_storage_sl1a"),
            Map.entry("archival_mid", "tooltip.scp_additions.archival_storage_sl1a"),
            Map.entry("archival_top", "tooltip.scp_additions.archival_storage_sl1a"),
            Map.entry("archival_bot_1", "tooltip.scp_additions.archival_storage_sl1a"),
            Map.entry("archival_mid_2", "tooltip.scp_additions.archival_storage_sl1a"),
            Map.entry("office_bottom", "tooltip.scp_additions.patron_offices"),
            Map.entry("office_mid", "tooltip.scp_additions.patron_offices"),
            Map.entry("office_top", "tooltip.scp_additions.patron_offices"),
            Map.entry("skyroom_bot_1", "tooltip.scp_additions.skyroom_cafeteria_lounge"),
            Map.entry("skyroom_bot_2", "tooltip.scp_additions.skyroom_cafeteria_lounge"),
            Map.entry("skyroom_mid", "tooltip.scp_additions.skyroom_cafeteria_lounge"),
            Map.entry("skyroom_top_alt", "tooltip.scp_additions.skyroom_cafeteria_lounge"),
            Map.entry("skyroom_block", "tooltip.scp_additions.skyroom_cafeteria_lounge"),
            Map.entry("security_bot", "tooltip.scp_additions.security_office"),
            Map.entry("security_mid", "tooltip.scp_additions.security_office"),
            Map.entry("security_top", "tooltip.scp_additions.security_office"));

    private FacilityTooltipEvents() {}

    @SubscribeEvent
    public static void appendFacilityTooltips(ItemTooltipEvent event) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !ScpAdditionsMod.MODID.equals(id.getNamespace())) return;
        String path = id.getPath();
        if (TESLA.contains(path)) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.scp_additions.tesla_gate_room")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        String roomKey = SUBLEVEL_ONE_ROOMS.get(path);
        if (roomKey != null) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.scp_additions.sublevel_1")
                    .withStyle(ChatFormatting.BLUE));
            event.getToolTip().add(Component.translatable(roomKey)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
