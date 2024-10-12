
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
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

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScpAdditionsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ScpAdditionsMod.MODID);
	public static final RegistryObject<CreativeModeTab> SCP_ADDITIONS = REGISTRY.register("scp_additions",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.scp_additions.scp_additions")).icon(() -> new ItemStack(ScpAdditionsModBlocks.TESLA_GATE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(ScpAdditionsModBlocks.BUTTON_ROFF.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.BUTTON_LOFF.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.TESLA_GATE.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.TESLA_TERMINAL_OFF.get().asItem());
				tabData.accept(ScpAdditionsModItems.SECURITY_CREDENTIALS.get());
				tabData.accept(ScpAdditionsModBlocks.DECON_OPEN.get().asItem());
				tabData.accept(ScpAdditionsModItems.HAZMAT_SUIT_HELMET.get());
				tabData.accept(ScpAdditionsModItems.HAZMAT_SUIT_CHESTPLATE.get());
				tabData.accept(ScpAdditionsModItems.HAZMAT_SUIT_LEGGINGS.get());
				tabData.accept(ScpAdditionsModItems.HAZMAT_SUIT_BOOTS.get());
				tabData.accept(ScpAdditionsModItems.GEIGER_1.get());
				tabData.accept(ScpAdditionsModItems.SPRAY.get());
				tabData.accept(ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_079CONTROLOFF.get().asItem());
			}).withSearchBar().build());
	public static final RegistryObject<CreativeModeTab> SC_PADDITIONS_SC_PS = REGISTRY.register("sc_padditions_sc_ps",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.scp_additions.sc_padditions_sc_ps")).icon(() -> new ItemStack(ScpAdditionsModBlocks.SCP_1176.get())).displayItems((parameters, tabData) -> {
				tabData.accept(ScpAdditionsModBlocks.SCP_059.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_059_CONTAINED.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_059_1.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_079ON.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_294.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_330.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_426.get().asItem());
				tabData.accept(ScpAdditionsModItems.SCP_572.get());
				tabData.accept(ScpAdditionsModBlocks.SCP_902_CLOSED.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914BLOCK.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914CLOCKWORKS.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914BODY.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914DIAL_1TO_1.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914_KEY_WIND.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914_INTAKE.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914_OUTPUT.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.SCP_1176.get().asItem());
			}).withSearchBar().build());
	public static final RegistryObject<CreativeModeTab> SC_PADDITIONS_KEYCARDS = REGISTRY.register("sc_padditions_keycards",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.scp_additions.sc_padditions_keycards")).icon(() -> new ItemStack(ScpAdditionsModBlocks.RIGHT_READER.get())).displayItems((parameters, tabData) -> {
				tabData.accept(ScpAdditionsModItems.LEVEL_1_KEYCARD.get());
				tabData.accept(ScpAdditionsModItems.LEVEL_2_KEYCARD.get());
				tabData.accept(ScpAdditionsModItems.LEVEL_3_KEYCARD.get());
				tabData.accept(ScpAdditionsModItems.LEVEL_4_KEYCARD.get());
				tabData.accept(ScpAdditionsModItems.LEVEL_5_KEYCARD.get());
				tabData.accept(ScpAdditionsModItems.LEVEL_6_KEYCARD.get());
				tabData.accept(ScpAdditionsModBlocks.RIGHT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LEFT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_2_RIGHT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_2_LEFT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_3_RIGHT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_3_LEFT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_4_RIGHT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_4_LEFT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_5_RIGHT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_5_LEFT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_6_RIGHT_READER.get().asItem());
				tabData.accept(ScpAdditionsModBlocks.LV_6_LEFT_READER.get().asItem());
			}).withSearchBar().build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {

		if (tabData.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
			tabData.accept(ScpAdditionsModItems.PLAYING_CARD.get());
			tabData.accept(ScpAdditionsModItems.CREDIT_CARD.get());
			tabData.accept(ScpAdditionsModItems.PIECES_OF_PAPER.get());
			tabData.accept(ScpAdditionsModItems.COIN.get());
			tabData.accept(ScpAdditionsModItems.EMPTY_CUP.get());
		}

		if (tabData.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
			tabData.accept(ScpAdditionsModItems.SCP_330_RED_CANDY.get());
			tabData.accept(ScpAdditionsModItems.SCP_330_GREEN_CANDY.get());
			tabData.accept(ScpAdditionsModItems.SCP_330_YELLOW_CANDY.get());
			tabData.accept(ScpAdditionsModItems.SCP_330_BLUE_CANDY.get());
			tabData.accept(ScpAdditionsModItems.SCP_1176HONEY.get());
		}
	}
}
