package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScpItemEffects {

    private ScpItemEffects() {
    }

    public static boolean hasNoStaminaModifier(ItemStack stack) {
        return hasEffect(stack, ItemEffect.NO_STAMINA);
    }

    public static boolean hasNoStaminaModifierEquipped(Player player) {
        if (player == null) {
            return false;
        }

        if (hasNoStaminaModifier(player.getMainHandItem()) || hasNoStaminaModifier(player.getOffhandItem())) {
            return true;
        }

        for (ItemStack armorStack : player.getArmorSlots()) {
            if (hasNoStaminaModifier(armorStack)) {
                return true;
            }
        }

        AtomicBoolean hasEffect = new AtomicBoolean(false);
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory ->
                hasEffect.set(hasNoStaminaModifierEquipped(inventory))
        );
        return hasEffect.get();
    }

    public static boolean hasNoStaminaModifierEquipped(IScpInventory inventory) {
        if (inventory == null) {
            return false;
        }

        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            if (hasNoStaminaModifier(inventory.getEquipment(slot))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasNoStaminaModifierEquipped(Player player, IScpInventory inventory) {
        if (player == null) {
            return false;
        }

        if (hasNoStaminaModifier(player.getMainHandItem()) || hasNoStaminaModifier(player.getOffhandItem())) {
            return true;
        }

        for (ItemStack armorStack : player.getArmorSlots()) {
            if (hasNoStaminaModifier(armorStack)) {
                return true;
            }
        }

        return hasNoStaminaModifierEquipped(inventory);
    }

    private static boolean hasEffect(ItemStack stack, ItemEffect effect) {
        if (stack == null || stack.isEmpty() || effect == null) {
            return false;
        }

        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (stackId == null) {
            return false;
        }

        for (String rawRule : ScpInventoryConfig.itemEffects()) {
            Optional<ConfiguredItemEffect> rule = parseEffectRule(rawRule);
            if (rule.isPresent() && rule.get().itemId().equals(stackId) && rule.get().effect() == effect) {
                return true;
            }
        }
        return false;
    }

    private static Optional<ConfiguredItemEffect> parseEffectRule(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) {
            return Optional.empty();
        }

        String[] parts = rawRule.split("\\|", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        ResourceLocation itemId = ResourceLocation.tryParse(parts[0].trim());
        if (itemId == null) {
            return Optional.empty();
        }

        Optional<ItemEffect> effect = ItemEffect.fromConfigToken(parts[1]);
        return effect.map(itemEffect -> new ConfiguredItemEffect(itemId, itemEffect));
    }

    private enum ItemEffect {
        NO_STAMINA;

        private static Optional<ItemEffect> fromConfigToken(String token) {
            if (token == null || token.isBlank()) {
                return Optional.empty();
            }

            String normalized = token.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            return switch (normalized) {
                case "NO_STAMINA", "ZERO_STAMINA", "DISABLE_STAMINA", "STAMINA_DISABLED" -> Optional.of(NO_STAMINA);
                default -> Optional.empty();
            };
        }
    }

    private record ConfiguredItemEffect(ResourceLocation itemId, ItemEffect effect) {
    }
}
