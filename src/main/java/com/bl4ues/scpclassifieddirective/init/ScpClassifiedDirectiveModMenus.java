
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package com.bl4ues.scpclassifieddirective.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;

import com.bl4ues.scpclassifieddirective.world.inventory.TeslaTerminalMenu;
import com.bl4ues.scpclassifieddirective.world.inventory.Scp294GuiMenu;
import com.bl4ues.scpclassifieddirective.world.inventory.PlayerCorpseMenu;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

public class ScpClassifiedDirectiveModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ScpClassifiedDirectiveMod.MODID);
	public static final RegistryObject<MenuType<TeslaTerminalMenu>> TESLA_TERMINAL = REGISTRY.register("tesla_terminal", () -> IForgeMenuType.create(TeslaTerminalMenu::new));
	public static final RegistryObject<MenuType<Scp294GuiMenu>> SCP_294_GUI = REGISTRY.register("scp_294_gui", () -> IForgeMenuType.create(Scp294GuiMenu::new));
	public static final RegistryObject<MenuType<PlayerCorpseMenu>> PLAYER_CORPSE = REGISTRY.register("player_corpse", () -> IForgeMenuType.create(PlayerCorpseMenu::new));
}
