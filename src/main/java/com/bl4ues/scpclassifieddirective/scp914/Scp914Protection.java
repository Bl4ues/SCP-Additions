package com.bl4ues.scpclassifieddirective.scp914;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

/** Shared mining and destruction rules for every cell of the SCP-914 structure. */
public final class Scp914Protection {
    /**
     * Iron pickaxe speed (6) against hardness 37.5 matches a diamond pickaxe
     * (speed 8) against obsidian hardness 50 under vanilla mining math.
     */
    public static final float HARDNESS = 37.5F;

    /** Bedrock-class blast resistance; entity destruction is denied separately. */
    public static final float BLAST_RESISTANCE = 3_600_000.0F;

    private Scp914Protection() {
    }

    public static boolean canMine(Player player) {
        if (player == null) return false;
        if (player.isCreative()) return true;

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ItemTags.PICKAXES)
                || !(stack.getItem() instanceof TieredItem tiered)) {
            return false;
        }
        return tiered.getTier().getLevel() >= Tiers.IRON.getLevel();
    }
}
