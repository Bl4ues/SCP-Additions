package net.mcreator.scpadditions.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Keeps SCP Additions tooltip punctuation consistent without flattening styles. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TooltipPunctuationNormalizer {
    private TooltipPunctuationNormalizer() {
    }

    @SubscribeEvent
    public static void normalizeTooltip(ItemTooltipEvent event) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(
                event.getItemStack().getItem());
        if (itemId == null
                || !ScpAdditionsMod.MODID.equals(itemId.getNamespace())) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        for (int index = 1; index < tooltip.size(); index++) {
            Component line = tooltip.get(index);
            String text = line.getString();
            if (!text.endsWith(".") || text.endsWith("...")) {
                continue;
            }
            tooltip.set(index, withoutTerminalPeriod(line));
        }
    }

    private static Component withoutTerminalPeriod(Component component) {
        List<StyledText> segments = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                segments.add(new StyledText(text, style));
            }
            return Optional.<Void>empty();
        }, Style.EMPTY);

        if (segments.isEmpty()) {
            return component;
        }

        int finalIndex = segments.size() - 1;
        StyledText finalSegment = segments.get(finalIndex);
        if (!finalSegment.text().endsWith(".")) {
            return component;
        }

        segments.set(finalIndex, new StyledText(
                finalSegment.text().substring(0,
                        finalSegment.text().length() - 1),
                finalSegment.style()));

        MutableComponent rebuilt = Component.empty();
        for (StyledText segment : segments) {
            if (!segment.text().isEmpty()) {
                rebuilt.append(Component.literal(segment.text())
                        .withStyle(segment.style()));
            }
        }
        return rebuilt;
    }

    private record StyledText(String text, Style style) {
    }
}
