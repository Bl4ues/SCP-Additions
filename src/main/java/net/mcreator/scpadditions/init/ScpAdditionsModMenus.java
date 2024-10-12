
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.scpadditions.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;

import net.mcreator.scpadditions.world.inventory.TeslaTerminalMenu;
import net.mcreator.scpadditions.world.inventory.Scp914GuiMenu;
import net.mcreator.scpadditions.world.inventory.Scp294GuiMenu;
import net.mcreator.scpadditions.ScpAdditionsMod;

public class ScpAdditionsModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ScpAdditionsMod.MODID);
	public static final RegistryObject<MenuType<TeslaTerminalMenu>> TESLA_TERMINAL = REGISTRY.register("tesla_terminal", () -> IForgeMenuType.create(TeslaTerminalMenu::new));
	public static final RegistryObject<MenuType<Scp914GuiMenu>> SCP_914_GUI = REGISTRY.register("scp_914_gui", () -> IForgeMenuType.create(Scp914GuiMenu::new));
	public static final RegistryObject<MenuType<Scp294GuiMenu>> SCP_294_GUI = REGISTRY.register("scp_294_gui", () -> IForgeMenuType.create(Scp294GuiMenu::new));
}
