package com.bl4ues.scpclassifieddirective.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.scp012.Scp012Module;
import com.bl4ues.scpclassifieddirective.scp1576.Scp1576Module;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScpClassifiedDirectiveModTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
                    ScpClassifiedDirectiveMod.MODID);

    private static final ResourceLocation FACILITY_TAB_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "scp_unity_blocks");
    private static final ResourceLocation ANOMALIES_TAB_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "sc_padditions_sc_ps");
    private static final ResourceLocation ITEMS_TAB_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "scp_classified_directive");

    public static final RegistryObject<CreativeModeTab> SCP_CLASSIFIED_DIRECTIVE =
            REGISTRY.register("scp_classified_directive", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_classified_directive.scp_classified_directive"))
                            .icon(() -> new ItemStack(
                                    ScpClassifiedDirectiveModItems.SECURITY_CREDENTIALS.get()))
                            .displayItems((parameters, output) ->
                                    scpClassifiedDirectiveStacks().forEach(output::accept))
                            .withTabsBefore(FACILITY_TAB_ID, ANOMALIES_TAB_ID)
                            .withSearchBar()
                            .hideTitle()
                            .build());

    public static final RegistryObject<CreativeModeTab> SC_PADDITIONS_SC_PS =
            REGISTRY.register("sc_padditions_sc_ps", () ->
                    CreativeModeTab.builder()
                            .title(Component.literal(
                                    "SCP: Classified Directive - Anomalies"))
                            .icon(() -> new ItemStack(Scp012Module.SCP_012_ITEM.get()))
                            .displayItems((parameters, output) ->
                                    anomalyTabStacks().forEach(output::accept))
                            .withTabsBefore(FACILITY_TAB_ID)
                            .withTabsAfter(ITEMS_TAB_ID)
                            .withSearchBar()
                            .hideTitle()
                            .build());

    public record CreativeSection(ResourceLocation sprite,
            List<ItemStack> items) {
    }

    public static List<ItemStack> scpClassifiedDirectiveStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.SECURITY_CREDENTIALS.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.LEVEL_1_KEYCARD.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.LEVEL_2_KEYCARD.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.LEVEL_3_KEYCARD.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.LEVEL_4_KEYCARD.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.LEVEL_5_KEYCARD.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.LEVEL_6_KEYCARD.get()));
        stacks.add(new ItemStack(UnifiedReaderItems.SCREWDRIVER.get()));
        stacks.add(new ItemStack(Scp131Items.ROOMBA_SPAWN_EGG.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.HAZMAT_SUIT.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.PLAYING_CARD.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.CREDIT_CARD.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.PIECES_OF_PAPER.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.COIN.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.EMPTY_CUP.get()));
        return stacks;
    }

    public static List<ItemStack> scpStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(Scp012Module.SCP_012_ITEM.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_079ON.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_106_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_131_A_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_131_B_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_173_SPAWN_EGG.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_294.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_330.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_426.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.SCP_572.get()));
        stacks.add(new ItemStack(Scp714Items.SCP_714.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_902_CLOSED.get()));
        stacks.add(new ItemStack(Scp914Module.SCP_914_ITEM.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_939_SPAWN_EGG.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_1176.get()));
        stacks.add(new ItemStack(Scp1576Module.SCP_1576.get()));
        return stacks;
    }

    /**
     * Non-SCP anomalous objects belong here. Keeping this separate from the SCP
     * list preserves the numeric SCP ordering while sharing one creative tab.
     */
    public static List<ItemStack> anomalousItemStacks() {
        return List.of(new ItemStack(ScpClassifiedDirectiveModItems.ITEM_006.get()));
    }

    public static List<CreativeSection> anomalyCreativeSections() {
        return List.of(
                section("scptab", scpStacks()),
                section("anomalousitemstab", anomalousItemStacks()));
    }

    /** Actual searchable items, without the visual spacer/header rows. */
    public static List<ItemStack> anomalyTabStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (CreativeSection section : anomalyCreativeSections()) {
            section.items().forEach(stack -> stacks.add(stack.copy()));
        }
        return List.copyOf(stacks);
    }

    /**
     * Mirrors the Facility tab layout: one empty nine-slot row is reserved for
     * each graphical section header, followed by that section's real items.
     */
    public static List<ItemStack> anomalyCreativeTabDisplayStacks() {
        List<ItemStack> display = new ArrayList<>();
        for (CreativeSection section : anomalyCreativeSections()) {
            for (int index = 0; index < 9; index++) {
                display.add(ItemStack.EMPTY);
            }
            section.items().forEach(stack -> display.add(stack.copy()));
            while (display.size() % 9 != 0) display.add(ItemStack.EMPTY);
        }
        return display;
    }

    private static CreativeSection section(String sprite,
            List<ItemStack> items) {
        return new CreativeSection(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                        "textures/gui/facility_sections/" + sprite + ".png"),
                List.copyOf(items));
    }

    @SubscribeEvent
    public static void buildTabContentsVanilla(
            BuildCreativeModeTabContentsEvent tabData) {
        if (tabData.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            tabData.accept(ScpClassifiedDirectiveModItems.SCP_330_BLUE_CANDY.get());
            tabData.accept(ScpClassifiedDirectiveModItems.SCP_330_PINK_CANDY.get());
            tabData.accept(ScpClassifiedDirectiveModItems.SCP_330_YELLOW_CANDY.get());
            tabData.accept(ScpClassifiedDirectiveModItems.SCP_1176HONEY.get());
        }
    }
}
