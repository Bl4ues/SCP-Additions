package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ScpItemClassifier {
    private static final ResourceLocation CANONICAL_SCP_ADDITIONS_COIN =
            new ResourceLocation("scp_additions", "coin");
    private static final TagKey<Item> AUTO_WEAPON = itemTag("auto_weapon");
    private static final TagKey<Item> AUTO_USABLE = itemTag("auto_usable");
    private static final TagKey<Item> AUTO_MISCELLANEOUS = itemTag("auto_miscellaneous");
    private static final Map<Class<?>, Boolean> AIR_USE_OVERRIDES = new ConcurrentHashMap<>();

    private ScpItemClassifier() {
    }

    public static ScpItemType getType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ScpItemType.MISCELLANEOUS;
        }

        if (ScpPickupRouter.isCoinMirror(stack)
                || ScpPickupRouter.isHarmfulMirror(stack)) {
            return ScpItemType.MISCELLANEOUS;
        }

        if (isCanonicalScpAdditionsCoin(stack)) {
            return ScpItemType.MISCELLANEOUS;
        }

        Optional<CodexDocumentDefinition> codexDocument = getCodexDocument(stack);
        if (codexDocument.isPresent()) {
            return ScpItemType.CODEX;
        }

        Optional<ScpItemType> configuredType = getConfiguredType(stack);
        if (configuredType.isPresent()) {
            ScpItemType type = configuredType.get();
            return type == ScpItemType.COIN
                    ? ScpItemType.MISCELLANEOUS : type;
        }

        if (stack.is(AUTO_MISCELLANEOUS)) {
            return ScpItemType.MISCELLANEOUS;
        }
        if (stack.is(AUTO_WEAPON)) {
            return ScpItemType.WEAPON;
        }
        if (stack.is(AUTO_USABLE)) {
            return ScpItemType.USABLE;
        }

        // Thrown potions are manually activated tools. Only drinkable potions
        // belong to the consumable category.
        if (isThrownPotion(stack)) {
            return ScpItemType.USABLE;
        }

        if (isDefaultConsumable(stack)) {
            return ScpItemType.CONSUMABLE;
        }

        if (stack.getItem() instanceof ArmorItem armorItem) {
            return fromVanillaEquipmentSlot(armorItem.getEquipmentSlot());
        }

        if (stack.getItem() instanceof BlockItem) {
            return ScpItemType.PLACEABLE;
        }

        if (isDefaultWeapon(stack)) {
            return ScpItemType.WEAPON;
        }

        if (isDefaultUsable(stack)) {
            return ScpItemType.USABLE;
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
        return stack != null && !stack.isEmpty()
                && !ScpPickupRouter.isCoinMirror(stack)
                && (isCanonicalScpAdditionsCoin(stack)
                        || getConfiguredType(stack).orElse(null)
                        == ScpItemType.COIN);
    }

    public static boolean isMirroredMainItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || ScpPickupRouter.isCoinMirror(stack)
                || ScpPickupRouter.isHarmfulMirror(stack)) {
            return false;
        }
        if (isCanonicalScpAdditionsCoin(stack)) {
            return true;
        }
        ScpItemType type = getConfiguredType(stack).orElse(null);
        return type == ScpItemType.COIN || type == ScpItemType.AMMO;
    }

    public static boolean isHarmful(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemStack copy = stack.copy();
        ScpPickupRouter.stripHarmfulMirror(copy);
        ScpPickupRouter.stripNoMergeMarker(copy);
        ScpPickupRouter.stripUsableSession(copy);
        return getConfiguredType(copy).orElse(null) == ScpItemType.HARMFUL;
    }

    public static boolean isUsable(ItemStack stack) {
        ScpItemType type = getType(stack);
        return type == ScpItemType.USABLE || type == ScpItemType.PLACEABLE;
    }

    public static boolean isAccessoryHand(ItemStack stack) {
        return getType(stack) == ScpItemType.ACCESSORY_HAND;
    }

    public static Optional<ResourceLocation> getConfiguredCoinItemId() {
        for (String rawRule : ScpInventoryConfig.itemRules()) {
            Optional<ConfiguredItemRule> rule = parseItemRule(rawRule);
            if (rule.isPresent() && rule.get().type() == ScpItemType.COIN) {
                return Optional.of(rule.get().itemId());
            }
        }
        return Optional.empty();
    }

    public static ItemStack getConfiguredCoinStack() {
        return getConfiguredCoinItemId()
                .flatMap(id -> BuiltInRegistries.ITEM.getOptional(id)
                        .map(ItemStack::new))
                .orElse(ItemStack.EMPTY);
    }

    public static boolean isConfiguredMirroredMainItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (stackId == null) {
            return false;
        }
        for (String rawRule : ScpInventoryConfig.itemRules()) {
            Optional<ConfiguredItemRule> rule = parseItemRule(rawRule);
            if (rule.isPresent() && rule.get().itemId().equals(stackId)
                    && isMirroredMainType(rule.get().type())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMirroredMainType(ScpItemType type) {
        return type == ScpItemType.COIN || type == ScpItemType.AMMO;
    }

    public static String getCodexDisplayName(ItemStack stack) {
        return getCodexDocument(stack)
                .map(document -> document.getDisplayName(stack))
                .orElseGet(() -> stack == null || stack.isEmpty()
                        ? "Unknown Document" : stack.getHoverName().getString());
    }

    public static CodexDocumentDefinition getCodexDefinitionOrFallback(
            ItemStack stack) {
        return getCodexDocument(stack)
                .orElseGet(() -> CodexDocumentDefinition.fallback(stack));
    }

    public static Optional<CodexDocumentDefinition> getCodexDocument(
            ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        for (String rawRule : ScpInventoryConfig.codexDocuments()) {
            Optional<CodexDocumentDefinition> definition =
                    CodexDocumentDefinition.parse(rawRule);
            if (definition.isPresent() && definition.get().matches(stack)) {
                return definition;
            }
        }
        return Optional.empty();
    }

    private static boolean isThrownPotion(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof SplashPotionItem
                || item instanceof LingeringPotionItem) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.equals("splash_potion")
                || path.equals("lingering_potion")
                || path.endsWith("_splash_potion")
                || path.endsWith("_lingering_potion");
    }

    private static boolean isDefaultConsumable(ItemStack stack) {
        if (isThrownPotion(stack)) {
            return false;
        }
        if (stack.isEdible()) {
            return true;
        }

        UseAnim animation = stack.getUseAnimation();
        if (animation == UseAnim.EAT || animation == UseAnim.DRINK) {
            return true;
        }

        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (stackId == null) {
            return false;
        }

        String path = stackId.getPath();
        return path.equals("potion")
                || path.endsWith("_potion")
                || path.contains("potion");
    }

    private static boolean isDefaultWeapon(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SwordItem
                || item instanceof ProjectileWeaponItem
                || item instanceof TridentItem) {
            return true;
        }

        UseAnim animation = stack.getUseAnimation();
        return animation == UseAnim.BOW
                || animation == UseAnim.CROSSBOW
                || animation == UseAnim.SPEAR;
    }

    private static boolean isDefaultUsable(ItemStack stack) {
        if (isThrownPotion(stack)) {
            return true;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem) {
            return false;
        }

        UseAnim animation = stack.getUseAnimation();
        if (animation == UseAnim.BLOCK
                || animation == UseAnim.SPYGLASS
                || animation == UseAnim.TOOT_HORN
                || animation == UseAnim.BRUSH) {
            return true;
        }

        if (item instanceof FishingRodItem
                || item instanceof FlintAndSteelItem
                || item instanceof ShearsItem
                || item instanceof BucketItem
                || item instanceof EnderpearlItem
                || item instanceof SnowballItem
                || item instanceof EggItem
                || item instanceof ExperienceBottleItem
                || item instanceof FireworkRocketItem) {
            return true;
        }

        return overridesAirUse(item);
    }

    private static boolean overridesAirUse(Item item) {
        return AIR_USE_OVERRIDES.computeIfAbsent(item.getClass(), itemClass -> {
            try {
                Method use = itemClass.getMethod(
                        "use", Level.class, Player.class, InteractionHand.class);
                return use.getDeclaringClass() != Item.class;
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        });
    }

    private static Optional<ScpItemType> getConfiguredType(ItemStack stack) {
        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (stackId == null) {
            return Optional.empty();
        }

        for (String rawRule : ScpInventoryConfig.itemRules()) {
            Optional<ConfiguredItemRule> rule = parseItemRule(rawRule);
            if (rule.isPresent() && rule.get().itemId().equals(stackId)) {
                return Optional.of(rule.get().type());
            }
        }
        return Optional.empty();
    }

    private static boolean isCanonicalScpAdditionsCoin(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return CANONICAL_SCP_ADDITIONS_COIN.equals(
                BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static Optional<ConfiguredItemRule> parseItemRule(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) {
            return Optional.empty();
        }

        String[] parts = rawRule.split("\\|", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        ResourceLocation configuredId =
                ResourceLocation.tryParse(parts[0].trim());
        if (configuredId == null) {
            return Optional.empty();
        }

        Optional<ScpItemType> type =
                ScpItemType.fromConfigToken(parts[1]);
        return type.map(scpItemType ->
                new ConfiguredItemRule(configuredId, scpItemType));
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

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM,
                new ResourceLocation("scp_additions", path));
    }

    private record ConfiguredItemRule(ResourceLocation itemId,
            ScpItemType type) {
    }
}
