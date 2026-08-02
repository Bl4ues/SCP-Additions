package net.mcreator.scpadditions.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class DocumentClientHooks {
    private DocumentClientHooks() {
    }

    public static void openEditor(InteractionHand hand, ItemStack stack) {
        DocumentEditorScreen.open(hand, stack);
    }
}
