package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
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

        if (isConfiguredCodex(stack)) {
            return ScpItemType.CODEX;
        }

        ResourceLocation id = idOf(stack);
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);

        ScpItemType registryType = classifyRegistryPath(path);
        if (registryType == ScpItemType.KEY || registryType == ScpItemType.COIN
                || registryType == ScpItemType.AMMO) {
            return registryType;
        }

        if (isDefaultConsumable(stack, path)) {
            return ScpItemType.CONSUMABLE;
        }

        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) {
            return fromVanillaEquipmentSlot(armor.getEquipmentSlot());
        }

        if (item instanceof SwordItem || item instanceof ProjectileWeaponItem
                || item instanceof TridentItem) {
            return ScpItemType.WEAPON;
        }

        // Vanilla and correctly implemented modded tools should remain available
        // as active survival tools instead of being mistaken for weapons.
        if (item instanceof DiggerItem || item instanceof FishingRodItem
                || item instanceof ShearsItem || item instanceof FlintAndSteelItem
                || item instanceof BrushItem || item instanceof ShieldItem
                || item instanceof SpyglassItem) {
            return ScpItemType.USABLE;
        }

        UseAnim animation = stack.getUseAnimation();
        if (animation == UseAnim.BOW || animation == UseAnim.CROSSBOW
                || animation == UseAnim.SPEAR) {
            return ScpItemType.WEAPON;
        }
        if (animation == UseAnim.BLOCK || animation == UseAnim.SPYGLASS
                || animation == UseAnim.TOOT_HORN || animation == UseAnim.BRUSH) {
            return ScpItemType.USABLE;
        }

        // Name heuristics are deliberately last. They improve compatibility with
        // lightweight mods whose items do not subclass the normal vanilla types,
        // while every explicit JSON entry still wins before reaching this point.
        if (registryType != ScpItemType.MISCELLANEOUS) {
            return registryType;
        }

        return ScpItemType.MISCELLANEOUS;
    }

    public static Optional<ScpEquipmentSlot> getEquipmentSlot(ItemStack stack) {
        return getType(stack).getEquipmentSlot();
    }

    public static String getDisplayType(ItemStack stack) {
        return getType(stack).getDisplayName();
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

    public static boolean isAccessoryHand(ItemStack stack) {
        return getType(stack) == ScpItemType.ACCESSORY_HAND;
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

    private static ScpItemType classifyRegistryPath(String path) {
        if (path == null || path.isBlank()) {
            return ScpItemType.MISCELLANEOUS;
        }

        if ((path.contains("keycard") || path.contains("key_card")
                || path.contains("access_card") || path.contains("credential"))
                && !path.contains("reader")) {
            return ScpItemType.KEY;
        }
        if (path.equals("coin") || path.endsWith("_coin")
                || path.contains("currency_token")) {
            return ScpItemType.COIN;
        }
        if (containsAny(path, "ammo", "ammunition", "bullet", "cartridge",
                "magazine", "shell", "arrow", "crossbow_bolt")) {
            return ScpItemType.AMMO;
        }

        if (containsAny(path, "helmet", "headgear", "gas_mask", "gasmask")) {
            return ScpItemType.HEAD;
        }
        if (containsAny(path, "chestplate", "body_armor", "bodyarmour",
                "ballistic_vest")) {
            return ScpItemType.CHEST;
        }
        if (containsAny(path, "leggings", "leg_armor", "trousers")) {
            return ScpItemType.LEGS;
        }
        if (containsAny(path, "boots", "footwear", "shoes")) {
            return ScpItemType.FEET;
        }
        if (containsAny(path, "ring", "bracelet", "hand_accessory")) {
            return ScpItemType.ACCESSORY_HAND;
        }
        if (containsAny(path, "accessory", "amulet", "charm", "necklace",
                "trinket")) {
            return ScpItemType.ACCESSORY;
        }

        if (containsAny(path, "gun", "pistol", "rifle", "shotgun",
                "revolver", "smg", "carbine", "launcher", "weapon",
                "sword", "blade", "knife", "dagger", "machete",
                "spear", "katana", "baton", "fire_axe")) {
            return ScpItemType.WEAPON;
        }

        if (containsAny(path, "pickaxe", "shovel", "wrench", "hammer",
                "drill", "crowbar", "screwdriver", "shears", "fishing_rod",
                "flashlight", "torch", "lantern", "radio", "scanner",
                "detector", "geiger", "spray", "binocular", "spyglass",
                "shield", "mop", "writable_book", "handbook", "tool")) {
            return ScpItemType.USABLE;
        }

        if (containsAny(path, "medkit", "first_aid", "bandage", "medicine",
                "panacea", "pill", "tablet", "syringe", "candy",
                "ration", "food", "drink")) {
            return ScpItemType.CONSUMABLE;
        }

        return ScpItemType.MISCELLANEOUS;
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
                if (pair.length == 2
                        && pair[0].trim().equalsIgnoreCase("id")) {
                    candidate = pair[1].trim();
                    break;
                }
            }
            ResourceLocation configured = ResourceLocation.tryParse(candidate);
            if (itemId.equals(configured)) return true;
        }
        return false;
    }

    private static boolean isDefaultConsumable(ItemStack stack, String path) {
        if (stack.isEdible()) return true;
        UseAnim animation = stack.getUseAnimation();
        if (animation == UseAnim.EAT || animation == UseAnim.DRINK) return true;
        return path.equals("potion") || path.equals("splash_potion")
                || path.equals("lingering_potion") || path.endsWith("_potion")
                || path.contains("potion");
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) return true;
        }
        return false;
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
