package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.vitals.VitalsModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * Client-only perceptual lies produced by SCP-572.
 *
 * Nothing in this class changes authoritative health or weapon strength on the
 * server. The holder merely sees a much better weapon and no evidence of being
 * injured until the katana leaves their hands.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp572ClientEffects {
    private static final double APPARENT_ATTACK_DAMAGE = 25.0D;
    private static final double APPARENT_ATTACK_SPEED = 4.0D;
    private static final UUID APPARENT_BALANCE_UUID = UUID.fromString(
            "fbe4ef38-b55f-4aa3-8b69-e3f0cc75f572");
    private static final AttributeModifier APPARENT_BALANCE =
            new AttributeModifier(APPARENT_BALANCE_UUID,
                    "SCP-572 apparent balance", 100.0D,
                    AttributeModifier.Operation.ADDITION);

    private static LocalPlayer spoofedHealthPlayer;
    private static float realHealthBeforeOverlay;

    private Scp572ClientEffects() {
    }

    /** The perceptual effect applies when SCP-572 is physically in either hand. */
    public static boolean isHeldBy(Player player) {
        return player != null
                && (player.getMainHandItem().is(
                        ScpClassifiedDirectiveModItems.SCP_572.get())
                || player.getOffhandItem().is(
                        ScpClassifiedDirectiveModItems.SCP_572.get()));
    }

    /**
     * Make the local attack-strength calculation believe the katana is
     * exceptionally well balanced, while also erasing the local hurt timer that
     * drives Minecraft's damage camera tilt and hurt rendering.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        restoreVanillaHealthSpoof();

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            if (player.getMainHandItem().is(
                    ScpClassifiedDirectiveModItems.SCP_572.get())) {
                if (attackSpeed.getModifier(APPARENT_BALANCE_UUID) == null) {
                    attackSpeed.addTransientModifier(APPARENT_BALANCE);
                }
            } else {
                attackSpeed.removeModifier(APPARENT_BALANCE_UUID);
            }
        }

        if (isHeldBy(player)) {
            player.hurtTime = 0;
        }
    }

    /**
     * Vanilla hearts read LocalPlayer health directly. Spoof that value only
     * for the few instructions in which the vanilla health overlay is drawn,
     * then restore it immediately in the matching Post event.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void beforeVanillaHealth(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || event.isCanceled() || VitalsModule.healthHudEnabled()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (!isHeldBy(player)) return;

        restoreVanillaHealthSpoof();
        spoofedHealthPlayer = player;
        realHealthBeforeOverlay = player.getHealth();
        player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterVanillaHealth(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            restoreVanillaHealthSpoof();
        }
    }

    /**
     * The real attribute modifiers still exist on the stack so the server can
     * calculate combat normally. Only the completed client tooltip is edited.
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ScpClassifiedDirectiveModItems.SCP_572.get())) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        int headerIndex = findAttributeHeader(tooltip);
        int insertionIndex;

        if (headerIndex >= 0) {
            int start = headerIndex;
            if (start > 0 && tooltip.get(start - 1).getString().isEmpty()) {
                start--;
            }

            int end = headerIndex + 1;
            while (end < tooltip.size() && isAttributeModifierLine(tooltip.get(end))) {
                end++;
            }
            tooltip.subList(start, end).clear();
            insertionIndex = start;
        } else {
            insertionIndex = tooltip.size();
        }

        tooltip.add(insertionIndex++, CommonComponents.EMPTY);
        tooltip.add(insertionIndex++, Component.translatable(
                "item.modifiers." + EquipmentSlot.MAINHAND.getName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(insertionIndex++, apparentAttributeLine(
                APPARENT_ATTACK_DAMAGE, Attributes.ATTACK_DAMAGE));
        tooltip.add(insertionIndex, apparentAttributeLine(
                APPARENT_ATTACK_SPEED, Attributes.ATTACK_SPEED));
    }

    private static Component apparentAttributeLine(double value,
            net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        return Component.translatable("attribute.modifier.equals.0",
                ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value),
                Component.translatable(attribute.getDescriptionId()))
                .withStyle(ChatFormatting.DARK_GREEN);
    }

    private static int findAttributeHeader(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            String key = translationKey(tooltip.get(i));
            if (key.startsWith("item.modifiers.")) return i;
        }
        return -1;
    }

    private static boolean isAttributeModifierLine(Component component) {
        return translationKey(component).startsWith("attribute.modifier.");
    }

    private static String translationKey(Component component) {
        if (component.getContents() instanceof TranslatableContents translated) {
            return translated.getKey();
        }
        return "";
    }

    private static void restoreVanillaHealthSpoof() {
        if (spoofedHealthPlayer != null) {
            spoofedHealthPlayer.setHealth(realHealthBeforeOverlay);
            spoofedHealthPlayer = null;
        }
    }
}
