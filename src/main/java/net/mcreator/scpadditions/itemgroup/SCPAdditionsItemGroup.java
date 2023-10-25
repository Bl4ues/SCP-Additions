
package net.mcreator.scpadditions.itemgroup;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup;

import net.mcreator.scpadditions.block.TeslaGateBlock;
import net.mcreator.scpadditions.ScpAdditionsModElements;

@ScpAdditionsModElements.ModElement.Tag
public class SCPAdditionsItemGroup extends ScpAdditionsModElements.ModElement {
	public SCPAdditionsItemGroup(ScpAdditionsModElements instance) {
		super(instance, 36);
	}

	@Override
	public void initElements() {
		tab = new ItemGroup("tabscp_additions") {
			@OnlyIn(Dist.CLIENT)
			@Override
			public ItemStack createIcon() {
				return new ItemStack(TeslaGateBlock.block);
			}

			@OnlyIn(Dist.CLIENT)
			public boolean hasSearchBar() {
				return true;
			}
		}.setBackgroundImageName("item_search.png");
	}

	public static ItemGroup tab;
}
