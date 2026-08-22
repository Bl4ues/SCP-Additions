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

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.scp012.Scp012Module;
import com.bl4ues.scpclassifieddirective.scp1576.Scp1576Module;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScpClassifiedDirectiveModTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<CreativeModeTab> SCP_CLASSIFIED_DIRECTIVE =
            REGISTRY.register("scp_classified_directive", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_classified_directive.scp_classified_directive"))
                            .icon(() -> new ItemStack(
                                    ScpClassifiedDirectiveModItems.SECURITY_CREDENTIALS.get()))
                            .displayItems((parameters, output) ->
                                    scpClassifiedDirectiveStacks().forEach(output::accept))
                            .withSearchBar()
                            .hideTitle()
                            .build());

    public static final RegistryObject<CreativeModeTab> SC_PADDITIONS_SC_PS =
            REGISTRY.register("sc_padditions_sc_ps", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_classified_directive.sc_padditions_sc_ps"))
                            .icon(() -> new ItemStack(Scp012Module.SCP_012_ITEM.get()))
                            .displayItems((parameters, output) ->
                                    scpStacks().forEach(output::accept))
                            .withSearchBar()
                            .hideTitle()
                            .build());

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
        stacks.add(new ItemStack(ScpClassifiedDirectiveModItems.SCP_914_ASSEMBLY_KIT.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914BLOCK.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914CLOCKWORKS.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914BODY.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914DIAL_1TO_1.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_KEY_WIND.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_INTAKE.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_OUTPUT.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_INTAKE_DOOR.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_914_OUTPUT_DOOR.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_939_SPAWN_EGG.get()));
        stacks.add(new ItemStack(ScpClassifiedDirectiveModBlocks.SCP_1176.get()));
        stacks.add(new ItemStack(Scp1576Module.SCP_1576.get()));
        return stacks;
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
