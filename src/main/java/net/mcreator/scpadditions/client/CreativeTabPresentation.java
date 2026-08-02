package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.AreaUnderConstructionSignModule;
import net.mcreator.scpadditions.facility.FacilityModule;
import net.mcreator.scpadditions.init.ScpAdditionsModTabs;

import java.util.List;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CreativeTabPresentation {
    private static final long ICON_DISPLAY_MILLIS = 900L;
    private static final int HEADER_WIDTH = 162;
    private static final int HEADER_HEIGHT = 18;

    private static long lastIconStep = Long.MIN_VALUE;

    private CreativeTabPresentation() {
    }

    @SubscribeEvent
    public static void beforeRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;

        updateTabIcons();
        if (CreativeModeInventoryScreen.selectedTab ==
                FacilityModule.SCP_FACILITY_BLOCKS.get()
                && screen.searchBox.getValue().isEmpty()) {
            ensureFacilitySectionLayout(screen);
        }
    }

    @SubscribeEvent
    public static void afterRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;

        CreativeModeTab selected = CreativeModeInventoryScreen.selectedTab;
        if (isScpAdditionsTab(selected)) {
            renderShortTitle(screen, event.getGuiGraphics(), shortTitle(selected));
        }

        if (selected == FacilityModule.SCP_FACILITY_BLOCKS.get()
                && screen.searchBox.getValue().isEmpty()) {
            renderFacilityHeaders(screen, event.getGuiGraphics());
        }
    }

    private static void updateTabIcons() {
        long step = System.currentTimeMillis() / ICON_DISPLAY_MILLIS;
        if (step == lastIconStep) return;
        lastIconStep = step;

        setIcon(ScpAdditionsModTabs.SCP_ADDITIONS.get(),
                ScpAdditionsModTabs.scpAdditionsStacks(), step);
        setIcon(ScpAdditionsModTabs.SC_PADDITIONS_SC_PS.get(),
                ScpAdditionsModTabs.scpStacks(), step);
        setIcon(FacilityModule.SCP_FACILITY_BLOCKS.get(),
                AreaUnderConstructionSignModule.creativeTabIconStacks(), step);
    }

    private static void setIcon(CreativeModeTab tab, List<ItemStack> stacks,
            long step) {
        if (stacks.isEmpty()) return;
        int index = Math.floorMod(step, stacks.size());
        tab.iconItemStack = stacks.get(index).copy();
    }

    private static void ensureFacilitySectionLayout(
            CreativeModeInventoryScreen screen) {
        List<ItemStack> layout =
                AreaUnderConstructionSignModule.creativeTabDisplayStacks();
        List<ItemStack> current = screen.getMenu().items;
        if (hasSectionLayout(current, layout.size())) return;

        current.clear();
        layout.forEach(stack -> current.add(stack.copy()));
        screen.scrollOffs = 0.0F;
        screen.getMenu().scrollTo(0.0F);
    }

    private static boolean hasSectionLayout(List<ItemStack> items,
            int expectedSize) {
        if (items.size() != expectedSize || items.size() < 9) return false;
        for (int i = 0; i < 9; i++) {
            if (!items.get(i).isEmpty()) return false;
        }
        return true;
    }

    private static void renderFacilityHeaders(
            CreativeModeInventoryScreen screen, GuiGraphics graphics) {
        int currentRow = rowIndexForScroll(screen.scrollOffs,
                screen.getMenu().items.size());
        int sectionRow = 0;
        int left = screen.getGuiLeft() + 8;
        int top = screen.getGuiTop() + 17;
        for (FacilityModule.CreativeSection section :
                AreaUnderConstructionSignModule.creativeSections()) {
            int visibleRow = sectionRow - currentRow;
            if (visibleRow >= 0 && visibleRow < 5) {
                int y = top + visibleRow * HEADER_HEIGHT;
                graphics.blit(section.sprite(), left, y, 0.0F, 0.0F,
                        HEADER_WIDTH, HEADER_HEIGHT, HEADER_WIDTH,
                        HEADER_HEIGHT);
            }
            sectionRow += 1 + (section.items().size() + 8) / 9;
        }
    }

    private static int rowIndexForScroll(float scroll, int itemCount) {
        int totalRows = (itemCount + 8) / 9;
        int scrollableRows = Math.max(totalRows - 5, 0);
        return Math.max((int) (scroll * scrollableRows + 0.5F), 0);
    }

    private static void renderShortTitle(CreativeModeInventoryScreen screen,
            GuiGraphics graphics, Component title) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, title, screen.getGuiLeft() + 8,
                screen.getGuiTop() + 6, 0x404040, false);
    }

    private static Component shortTitle(CreativeModeTab tab) {
        if (tab == ScpAdditionsModTabs.SC_PADDITIONS_SC_PS.get()) {
            return Component.translatable("item_group.scp_additions.short_scps");
        }
        if (tab == FacilityModule.SCP_FACILITY_BLOCKS.get()) {
            return Component.translatable("item_group.scp_additions.short_blocks");
        }
        return Component.translatable("item_group.scp_additions.short_items");
    }

    private static boolean isScpAdditionsTab(CreativeModeTab tab) {
        return tab == ScpAdditionsModTabs.SCP_ADDITIONS.get()
                || tab == ScpAdditionsModTabs.SC_PADDITIONS_SC_PS.get()
                || tab == FacilityModule.SCP_FACILITY_BLOCKS.get();
    }
}
