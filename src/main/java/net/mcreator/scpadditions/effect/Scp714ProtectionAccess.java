package net.mcreator.scpadditions.effect;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.item.ScpItemEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Shared compatibility access for hazards neutralized or weakened by SCP-714.
 *
 * <p>Future systems such as SCP-012 should call this class instead of checking
 * item registry IDs directly. That keeps compatible items configurable through
 * the SCP Inventory item-effect system.</p>
 */
public final class Scp714ProtectionAccess {
    private Scp714ProtectionAccess() {
    }

    public static boolean isProtected(Player player) {
        return ScpItemEffects.hasScp714ProtectionEquipped(player);
    }

    public static boolean isProtected(Player player, IScpInventory inventory) {
        return ScpItemEffects.hasScp714ProtectionEquipped(player, inventory);
    }

    public static boolean providesProtection(ItemStack stack) {
        return ScpItemEffects.hasScp714Protection(stack);
    }
}
