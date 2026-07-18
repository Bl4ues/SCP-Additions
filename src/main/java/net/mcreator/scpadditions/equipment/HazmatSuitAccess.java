package net.mcreator.scpadditions.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Stable registry-ID contract and full-set checks for the rebuilt Hazmat Suit.
 *
 * <p>The four internal armor IDs intentionally reuse the removed legacy IDs so
 * old saves can resolve them again once the items are registered. Gameplay
 * benefits are granted only for the complete set.</p>
 */
public final class HazmatSuitAccess {
    public static final ResourceLocation SUIT_ITEM_ID = id("hazmat_suit");
    public static final ResourceLocation HELMET_ITEM_ID = id("hazmat_suit_helmet");
    public static final ResourceLocation CHESTPLATE_ITEM_ID = id("hazmat_suit_chestplate");
    public static final ResourceLocation LEGGINGS_ITEM_ID = id("hazmat_suit_leggings");
    public static final ResourceLocation BOOTS_ITEM_ID = id("hazmat_suit_boots");

    public static final float STAMINA_DRAIN_MULTIPLIER = 2.0F;

    private HazmatSuitAccess() {
    }

    public static boolean isFullyEquipped(LivingEntity entity) {
        return entity != null
                && isItem(entity.getItemBySlot(EquipmentSlot.HEAD), HELMET_ITEM_ID)
                && isItem(entity.getItemBySlot(EquipmentSlot.CHEST), CHESTPLATE_ITEM_ID)
                && isItem(entity.getItemBySlot(EquipmentSlot.LEGS), LEGGINGS_ITEM_ID)
                && isItem(entity.getItemBySlot(EquipmentSlot.FEET), BOOTS_ITEM_ID);
    }

    public static boolean protectsEyes(LivingEntity entity) {
        return isFullyEquipped(entity);
    }

    public static boolean providesSealedProtection(LivingEntity entity) {
        return isFullyEquipped(entity);
    }

    public static float getStaminaDrainMultiplier(LivingEntity entity) {
        return isFullyEquipped(entity) ? STAMINA_DRAIN_MULTIPLIER : 1.0F;
    }

    public static boolean isInternalPiece(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return HELMET_ITEM_ID.equals(itemId)
                || CHESTPLATE_ITEM_ID.equals(itemId)
                || LEGGINGS_ITEM_ID.equals(itemId)
                || BOOTS_ITEM_ID.equals(itemId);
    }

    private static boolean isItem(ItemStack stack, ResourceLocation expectedId) {
        return stack != null && !stack.isEmpty()
                && expectedId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpAdditionsMod.MODID, path);
    }
}
