package net.mcreator.scpadditions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.mcreator.scpadditions.world.inventory.PlayerCorpseMenu;

/**
 * Vanilla-style fallback for corpse storage when SCP Inventory is disabled.
 * When SCP Inventory is enabled the existing StorageContainerScreenEvents hook
 * replaces this screen with ScpStorageContainerScreen automatically.
 */
public final class PlayerCorpseScreen extends AbstractContainerScreen<PlayerCorpseMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private final int rows;

    public PlayerCorpseScreen(PlayerCorpseMenu menu, Inventory inventory,
            Component title) {
        super(menu, inventory, title);
        rows = menu.storageRows();
        imageHeight = 114 + rows * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
            int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = leftPos;
        int y = topPos;
        int upperHeight = rows * 18 + 17;
        graphics.blit(CONTAINER_BACKGROUND, x, y,
                0, 0, imageWidth, upperHeight);
        graphics.blit(CONTAINER_BACKGROUND, x, y + upperHeight,
                0, 126, imageWidth, 96);
    }
}
