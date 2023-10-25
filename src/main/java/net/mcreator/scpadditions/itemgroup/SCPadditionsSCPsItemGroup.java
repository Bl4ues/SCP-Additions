
package net.mcreator.scpadditions.itemgroup;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup;

import net.mcreator.scpadditions.block.Scp1176Block;
import net.mcreator.scpadditions.ScpAdditionsModElements;

@ScpAdditionsModElements.ModElement.Tag
public class SCPadditionsSCPsItemGroup extends ScpAdditionsModElements.ModElement {
	public SCPadditionsSCPsItemGroup(ScpAdditionsModElements instance) {
		super(instance, 201);
	}

	@Override
	public void initElements() {
		tab = new ItemGroup("tabsc_padditions_sc_ps") {
			@OnlyIn(Dist.CLIENT)
			@Override
			public ItemStack createIcon() {
				return new ItemStack(Scp1176Block.block);
			}

			@OnlyIn(Dist.CLIENT)
			public boolean hasSearchBar() {
				return true;
			}
		}.setBackgroundImageName("item_search.png");
	}

	public static ItemGroup tab;
}
