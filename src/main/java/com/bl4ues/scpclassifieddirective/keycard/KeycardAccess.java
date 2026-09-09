package com.bl4ues.scpclassifieddirective.keycard;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.integration.PlayerItemAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shared lookup for physical keycard interactions and their visual representation. */
public final class KeycardAccess {
    private KeycardAccess() {
    }

    public static int highestLevel(Player player) {
        return PlayerItemAccess.highestLevel(player, KeycardAccess::level);
    }

    public static int level(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_6_KEYCARD.get())) return 6;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_5_KEYCARD.get())) return 5;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_4_KEYCARD.get())) return 4;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_3_KEYCARD.get())) return 3;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_2_KEYCARD.get())) return 2;
        if (stack.is(ScpClassifiedDirectiveModItems.LEVEL_1_KEYCARD.get())) return 1;
        return 0;
    }

    public static ItemStack visualStack(int level) {
        return new ItemStack(switch (Math.max(1, Math.min(6, level))) {
            case 6 -> ScpClassifiedDirectiveModItems.LEVEL_6_KEYCARD.get();
            case 5 -> ScpClassifiedDirectiveModItems.LEVEL_5_KEYCARD.get();
            case 4 -> ScpClassifiedDirectiveModItems.LEVEL_4_KEYCARD.get();
            case 3 -> ScpClassifiedDirectiveModItems.LEVEL_3_KEYCARD.get();
            case 2 -> ScpClassifiedDirectiveModItems.LEVEL_2_KEYCARD.get();
            default -> ScpClassifiedDirectiveModItems.LEVEL_1_KEYCARD.get();
        });
    }
}
