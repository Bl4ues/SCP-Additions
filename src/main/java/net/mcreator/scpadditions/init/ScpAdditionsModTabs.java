package net.mcreator.scpadditions.init;

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

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.scp012.Scp012Module;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScpAdditionsModTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
                    ScpAdditionsMod.MODID);

    public static final RegistryObject<CreativeModeTab> SCP_ADDITIONS =
            REGISTRY.register("scp_additions", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_additions.scp_additions"))
                            .icon(() -> new ItemStack(
                                    ScpAdditionsModItems.SECURITY_CREDENTIALS.get()))
                            .displayItems((parameters, output) ->
                                    scpAdditionsStacks().forEach(output::accept))
                            .withSearchBar()
                            .hideTitle()
                            .build());

    public static final RegistryObject<CreativeModeTab> SC_PADDITIONS_SC_PS =
            REGISTRY.register("sc_padditions_sc_ps", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_additions.sc_padditions_sc_ps"))
                            .icon(() -> new ItemStack(Scp012Module.SCP_012_ITEM.get()))
                            .displayItems((parameters, output) ->
                                    scpStacks().forEach(output::accept))
                            .withSearchBar()
                            .hideTitle()
                            .build());

    public static List<ItemStack> scpAdditionsStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ScpAdditionsModItems.SECURITY_CREDENTIALS.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_1_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_2_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_3_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_4_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_5_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_6_KEYCARD.get()));
        stacks.add(new ItemStack(UnifiedReaderItems.SCREWDRIVER.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.HAZMAT_SUIT.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.PLAYING_CARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.CREDIT_CARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.COIN.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.EMPTY_CUP.get()));
        return stacks;
    }

    public static List<ItemStack> scpStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(Scp012Module.SCP_012_ITEM.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_079ON.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_106_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_131_A_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_131_B_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_173_SPAWN_EGG.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_294.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_330.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_426.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.SCP_572.get()));
        stacks.add(new ItemStack(Scp714Items.SCP_714.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_902_CLOSED.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.SCP_914_ASSEMBLY_KIT.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914BLOCK.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914CLOCKWORKS.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914BODY.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914DIAL_1TO_1.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_KEY_WIND.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_INTAKE.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_OUTPUT.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_1176.get()));
        return stacks;
    }

    @SubscribeEvent
    public static void buildTabContentsVanilla(
            BuildCreativeModeTabContentsEvent tabData) {
        if (tabData.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            tabData.accept(ScpAdditionsModItems.SCP_330_RED_CANDY.get());
            tabData.accept(ScpAdditionsModItems.SCP_330_GREEN_CANDY.get());
            tabData.accept(ScpAdditionsModItems.SCP_330_YELLOW_CANDY.get());
            tabData.accept(ScpAdditionsModItems.SCP_330_BLUE_CANDY.get());
            tabData.accept(ScpAdditionsModItems.SCP_1176HONEY.get());
        }
    }
}
