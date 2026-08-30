package com.bl4ues.scpclassifieddirective.client;

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
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.AreaUnderConstructionSignModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModTabs;

import java.util.List;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
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
        CreativeModeTab selected = CreativeModeInventoryScreen.selectedTab;
        if (screen.searchBox.getValue().isEmpty()) {
            if (selected == ScpClassifiedDirectiveModTabs.SCP_CLASSIFIED_DIRECTIVE.get()) {
                ensureItemSectionLayout(screen);
            } else if (selected == FacilityModule.SCP_FACILITY_BLOCKS.get()) {
                ensureFacilitySectionLayout(screen);
            } else if (selected ==
                    ScpClassifiedDirectiveModTabs.SC_PADDITIONS_SC_PS.get()) {
                ensureAnomalySectionLayout(screen);
            }
        }
    }

    @SubscribeEvent
    public static void afterRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) return;

        CreativeModeTab selected = CreativeModeInventoryScreen.selectedTab;
        if (isScpClassifiedDirectiveTab(selected)) {
            renderShortTitle(screen, event.getGuiGraphics(), shortTitle(selected));
        }

        if (screen.searchBox.getValue().isEmpty()) {
            if (selected == ScpClassifiedDirectiveModTabs.SCP_CLASSIFIED_DIRECTIVE.get()) {
                renderItemHeaders(screen, event.getGuiGraphics());
            } else if (selected == FacilityModule.SCP_FACILITY_BLOCKS.get()) {
                renderFacilityHeaders(screen, event.getGuiGraphics());
            } else if (selected ==
                    ScpClassifiedDirectiveModTabs.SC_PADDITIONS_SC_PS.get()) {
                renderAnomalyHeaders(screen, event.getGuiGraphics());
            }
        }
    }

    private static void updateTabIcons() {
        long step = System.currentTimeMillis() / ICON_DISPLAY_MILLIS;
        if (step == lastIconStep) return;
        lastIconStep = step;

        setIcon(ScpClassifiedDirectiveModTabs.SCP_CLASSIFIED_DIRECTIVE.get(),
                ScpClassifiedDirectiveModTabs.scpClassifiedDirectiveStacks(), step);
        setIcon(ScpClassifiedDirectiveModTabs.SC_PADDITIONS_SC_PS.get(),
                ScpClassifiedDirectiveModTabs.anomalyTabStacks(), step);
        setIcon(FacilityModule.SCP_FACILITY_BLOCKS.get(),
                AreaUnderConstructionSignModule.creativeTabIconStacks(), step);
    }

    private static void setIcon(CreativeModeTab tab, List<ItemStack> stacks,
            long step) {
        if (stacks.isEmpty()) return;
        int index = Math.floorMod(step, stacks.size());
        tab.iconItemStack = stacks.get(index).copy();
    }

    private static void ensureItemSectionLayout(
            CreativeModeInventoryScreen screen) {
        ensureSectionLayout(screen,
                ScpClassifiedDirectiveModTabs.itemCreativeTabDisplayStacks());
    }

    private static void ensureFacilitySectionLayout(
            CreativeModeInventoryScreen screen) {
        ensureSectionLayout(screen,
                AreaUnderConstructionSignModule.creativeTabDisplayStacks());
    }

    private static void ensureAnomalySectionLayout(
            CreativeModeInventoryScreen screen) {
        ensureSectionLayout(screen,
                ScpClassifiedDirectiveModTabs.anomalyCreativeTabDisplayStacks());
    }

    private static void ensureSectionLayout(
            CreativeModeInventoryScreen screen, List<ItemStack> layout) {
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

    private static void renderItemHeaders(
            CreativeModeInventoryScreen screen, GuiGraphics graphics) {
        int currentRow = rowIndexForScroll(screen.scrollOffs,
                screen.getMenu().items.size());
        int sectionRow = 0;
        int left = screen.getGuiLeft() + 8;
        int top = screen.getGuiTop() + 17;
        for (ScpClassifiedDirectiveModTabs.CreativeSection section :
                ScpClassifiedDirectiveModTabs.itemCreativeSections()) {
            renderHeader(graphics, section.sprite(), sectionRow, currentRow,
                    left, top);
            sectionRow += 1 + (section.items().size() + 8) / 9;
        }
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
            renderHeader(graphics, section.sprite(), sectionRow, currentRow,
                    left, top);
            sectionRow += 1 + (section.items().size() + 8) / 9;
        }
    }

    private static void renderAnomalyHeaders(
            CreativeModeInventoryScreen screen, GuiGraphics graphics) {
        int currentRow = rowIndexForScroll(screen.scrollOffs,
                screen.getMenu().items.size());
        int sectionRow = 0;
        int left = screen.getGuiLeft() + 8;
        int top = screen.getGuiTop() + 17;
        for (ScpClassifiedDirectiveModTabs.CreativeSection section :
                ScpClassifiedDirectiveModTabs.anomalyCreativeSections()) {
            renderHeader(graphics, section.sprite(), sectionRow, currentRow,
                    left, top);
            sectionRow += 1 + (section.items().size() + 8) / 9;
        }
    }

    private static void renderHeader(GuiGraphics graphics,
            net.minecraft.resources.ResourceLocation sprite, int sectionRow,
            int currentRow, int left, int top) {
        int visibleRow = sectionRow - currentRow;
        if (visibleRow < 0 || visibleRow >= 5) return;

        int y = top + visibleRow * HEADER_HEIGHT;
        graphics.blit(sprite, left, y, 0.0F, 0.0F,
                HEADER_WIDTH, HEADER_HEIGHT, HEADER_WIDTH, HEADER_HEIGHT);
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
        if (tab == ScpClassifiedDirectiveModTabs.SC_PADDITIONS_SC_PS.get()) {
            return Component.literal("Anomalies");
        }
        if (tab == FacilityModule.SCP_FACILITY_BLOCKS.get()) {
            return Component.translatable("item_group.scp_classified_directive.short_blocks");
        }
        return Component.translatable("item_group.scp_classified_directive.short_items");
    }

    private static boolean isScpClassifiedDirectiveTab(CreativeModeTab tab) {
        return tab == ScpClassifiedDirectiveModTabs.SCP_CLASSIFIED_DIRECTIVE.get()
                || tab == ScpClassifiedDirectiveModTabs.SC_PADDITIONS_SC_PS.get()
                || tab == FacilityModule.SCP_FACILITY_BLOCKS.get();
    }
}
