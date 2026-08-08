package com.bl4ues.scpinventory.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.Locale;
import java.util.Optional;

/**
 * Presentation/interaction profile for CONSUMABLE inventory rules. New use
 * families (bandage, pill, injection, etc.) can be added here without changing
 * the inventory category itself.
 */
public enum ScpConsumableType {
    FOOD("Food"),
    DRINK("Drink");

    private final String displayName;

    ScpConsumableType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<ScpConsumableType> fromConfigToken(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "FOOD", "EAT", "EATING" -> Optional.of(FOOD);
            case "DRINK", "DRINKING", "BEVERAGE" -> Optional.of(DRINK);
            default -> Optional.empty();
        };
    }

    public static ScpConsumableType infer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return FOOD;
        if (stack.getUseAnimation() == UseAnim.DRINK) return DRINK;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            String path = id.getPath().toLowerCase(Locale.ROOT);
            if (path.contains("drink") || path.contains("potion")
                    || path.contains("coffee") || path.contains("juice")
                    || path.contains("water") || path.contains("milk")
                    || path.contains("beverage")) {
                return DRINK;
            }
        }
        return FOOD;
    }
}
