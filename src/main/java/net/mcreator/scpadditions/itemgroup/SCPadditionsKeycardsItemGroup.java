
package net.mcreator.scpadditions.itemgroup;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup;

import net.mcreator.scpadditions.block.RightReaderBlock;
import net.mcreator.scpadditions.ScpAdditionsModElements;

@ScpAdditionsModElements.ModElement.Tag
public class SCPadditionsKeycardsItemGroup extends ScpAdditionsModElements.ModElement {
	public SCPadditionsKeycardsItemGroup(ScpAdditionsModElements instance) {
		super(instance, 261);
	}

	@Override
	public void initElements() {
		tab = new ItemGroup("tabsc_padditions_keycards") {
			@OnlyIn(Dist.CLIENT)
			@Override
			public ItemStack createIcon() {
				return new ItemStack(RightReaderBlock.block);
			}

			@OnlyIn(Dist.CLIENT)
			public boolean hasSearchBar() {
				return true;
			}
		}.setBackgroundImageName("item_search.png");
	}

	public static ItemGroup tab;
}
