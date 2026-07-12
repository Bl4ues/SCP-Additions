package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

import java.util.Locale;
import java.util.Optional;

/** Item classification shared by pickup routing, UI and equipment actions. */
public final class ScpItemClassifier {
    private ScpItemClassifier() {
    }

    public static ScpItemType getType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ScpItemType.MISCELLANEOUS;
        }

        Optional<ScpItemType> configured = getConfiguredType(stack);
        if (configured.isPresent()) {
            return configured.get();
        }

        if (isImplicitKey(stack)) {
            return ScpItemType.KEY;
        }

        if (isConfiguredCodex(stack)) {
            return ScpItemType.CODEX;
        }

        if (isDefaultConsumable(stack)) {
            return ScpItemType.CONSUMABLE;
        }

        if (stack.getItem() instanceof ArmorItem armor) {
            return fromVanillaEquipmentSlot(armor.getEquipmentSlot());
        }

        return ScpItemType.MISCELLANEOUS;
    }

    public static Optional<ScpEquipmentSlot> getEquipmentSlot(ItemStack stack) {
        return getType(stack).getEquipmentSlot();
    }

    public static boolean isCoin(ItemStack stack) {
        return getType(stack) == ScpItemType.COIN;
    }

    public static boolean isKey(ItemStack stack) {
        return getType(stack) == ScpItemType.KEY;
    }

    public static boolean isCodex(ItemStack stack) {
        return getType(stack) == ScpItemType.CODEX;
    }

    public static boolean isUsable(ItemStack stack) {
        return getType(stack) == ScpItemType.USABLE;
    }

    public static Optional<ScpItemType> getConfiguredType(ItemStack stack) {
        ResourceLocation itemId = idOf(stack);
        if (itemId == null) return Optional.empty();

        for (String rawRule : ScpInventoryConfig.itemRules()) {
            Optional<ConfiguredRule> parsed = parseRule(rawRule);
            if (parsed.isPresent() && parsed.get().itemId().equals(itemId)) {
                return Optional.of(parsed.get().type());
            }
        }
        return Optional.empty();
    }

    private static boolean isImplicitKey(ItemStack stack) {
        ResourceLocation id = idOf(stack);
        if (id == null) return false;
        String path = id.getPath();
        if (path.equals("security_credentials")) return true;
        return (path.equals("keycard") || path.endsWith("_keycard"))
                && !path.contains("reader");
    }

    private static boolean isConfiguredCodex(ItemStack stack) {
        ResourceLocation itemId = idOf(stack);
        if (itemId == null) return false;

        for (String raw : ScpInventoryConfig.codexDocuments()) {
            if (raw == null || raw.isBlank()) continue;
            String trimmed = raw.trim();
            String candidate = trimmed;
            for (String part : trimmed.split(";|\\r?\\n")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2 && pair[0].trim().equalsIgnoreCase("id")) {
                    candidate = pair[1].trim();
                    break;
                }
            }
            ResourceLocation configured = ResourceLocation.tryParse(candidate);
            if (itemId.equals(configured)) return true;
        }
        return false;
    }

    private static boolean isDefaultConsumable(ItemStack stack) {
        if (stack.isEdible()) return true;
        UseAnim animation = stack.getUseAnimation();
        if (animation == UseAnim.EAT || animation == UseAnim.DRINK) return true;

        ResourceLocation id = idOf(stack);
        if (id == null) return false;
        String path = id.getPath();
        return path.equals("potion")
                || path.equals("splash_potion")
                || path.equals("lingering_potion")
                || path.endsWith("_potion")
                || path.contains("potion");
    }

    private static ResourceLocation idOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private static Optional<ConfiguredRule> parseRule(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String[] parts = raw.split("\\|", 2);
        if (parts.length != 2) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
        Optional<ScpItemType> type = ScpItemType.fromConfigToken(
                parts[1].trim().toUpperCase(Locale.ROOT));
        if (id == null || type.isEmpty()) return Optional.empty();
        return Optional.of(new ConfiguredRule(id, type.get()));
    }

    private static ScpItemType fromVanillaEquipmentSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ScpItemType.HEAD;
            case CHEST -> ScpItemType.CHEST;
            case LEGS -> ScpItemType.LEGS;
            case FEET -> ScpItemType.FEET;
            default -> ScpItemType.MISCELLANEOUS;
        };
    }

    private record ConfiguredRule(ResourceLocation itemId, ScpItemType type) {
    }
}
