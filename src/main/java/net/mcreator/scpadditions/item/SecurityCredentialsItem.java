
package net.mcreator.scpadditions.item;

import net.minecraftforge.registries.ObjectHolder;

import net.minecraft.world.World;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.item.Rarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.block.BlockState;

import net.mcreator.scpadditions.itemgroup.SCPAdditionsItemGroup;
import net.mcreator.scpadditions.ScpAdditionsModElements;

import java.util.List;

@ScpAdditionsModElements.ModElement.Tag
public class SecurityCredentialsItem extends ScpAdditionsModElements.ModElement {
	@ObjectHolder("scp_additions:security_credentials")
	public static final Item block = null;

	public SecurityCredentialsItem(ScpAdditionsModElements instance) {
		super(instance, 3);
	}

	@Override
	public void initElements() {
		elements.items.add(() -> new ItemCustom());
	}

	public static class ItemCustom extends Item {
		public ItemCustom() {
			super(new Item.Properties().group(SCPAdditionsItemGroup.tab).maxStackSize(64).rarity(Rarity.COMMON));
			setRegistryName("security_credentials");
		}

		@Override
		public boolean hasContainerItem() {
			return true;
		}

		@Override
		public ItemStack getContainerItem(ItemStack itemstack) {
			return new ItemStack(this);
		}

		@Override
		public int getItemEnchantability() {
			return 0;
		}

		@Override
		public float getDestroySpeed(ItemStack par1ItemStack, BlockState par2Block) {
			return 1F;
		}

		@Override
		public void addInformation(ItemStack itemstack, World world, List<ITextComponent> list, ITooltipFlag flag) {
			super.addInformation(itemstack, world, list, flag);
			list.add(new StringTextComponent("A Security Credential for accessing the Tesla Gate Terminal."));
		}
	}
}
