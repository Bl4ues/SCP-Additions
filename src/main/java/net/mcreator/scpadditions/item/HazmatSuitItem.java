
package net.mcreator.scpadditions.item;

import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ArmorItem;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.BipedModel;

import net.mcreator.scpadditions.itemgroup.SCPAdditionsItemGroup;
import net.mcreator.scpadditions.ScpAdditionsModElements;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.matrix.MatrixStack;

@ScpAdditionsModElements.ModElement.Tag
public class HazmatSuitItem extends ScpAdditionsModElements.ModElement {
	@ObjectHolder("scp_additions:hazmat_suit_helmet")
	public static final Item helmet = null;
	@ObjectHolder("scp_additions:hazmat_suit_chestplate")
	public static final Item body = null;
	@ObjectHolder("scp_additions:hazmat_suit_leggings")
	public static final Item legs = null;
	@ObjectHolder("scp_additions:hazmat_suit_boots")
	public static final Item boots = null;

	public HazmatSuitItem(ScpAdditionsModElements instance) {
		super(instance, 7);
	}

	@Override
	public void initElements() {
		IArmorMaterial armormaterial = new IArmorMaterial() {
			@Override
			public int getDurability(EquipmentSlotType slot) {
				return new int[]{13, 15, 16, 11}[slot.getIndex()] * 25;
			}

			@Override
			public int getDamageReductionAmount(EquipmentSlotType slot) {
				return new int[]{2, 5, 6, 2}[slot.getIndex()];
			}

			@Override
			public int getEnchantability() {
				return 9;
			}

			@Override
			public net.minecraft.util.SoundEvent getSoundEvent() {
				return (net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("item.armor.equip_leather"));
			}

			@Override
			public Ingredient getRepairMaterial() {
				return Ingredient.EMPTY;
			}

			@OnlyIn(Dist.CLIENT)
			@Override
			public String getName() {
				return "hazmat_suit";
			}

			@Override
			public float getToughness() {
				return 0f;
			}

			@Override
			public float getKnockbackResistance() {
				return 0f;
			}
		};
		elements.items.add(() -> new ArmorItem(armormaterial, EquipmentSlotType.HEAD, new Item.Properties().group(SCPAdditionsItemGroup.tab)) {
			@Override
			@OnlyIn(Dist.CLIENT)
			public BipedModel getArmorModel(LivingEntity living, ItemStack stack, EquipmentSlotType slot, BipedModel defaultModel) {
				BipedModel armorModel = new BipedModel(1);
				armorModel.bipedHead = new ModelHazmatHelmet().head;
				armorModel.isSneak = living.isSneaking();
				armorModel.isSitting = defaultModel.isSitting;
				armorModel.isChild = living.isChild();
				return armorModel;
			}

			@Override
			public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
				return "scp_additions:textures/entities/hazmat.png";
			}
		}.setRegistryName("hazmat_suit_helmet"));
		elements.items.add(() -> new ArmorItem(armormaterial, EquipmentSlotType.CHEST, new Item.Properties().group(SCPAdditionsItemGroup.tab)) {
			@Override
			@OnlyIn(Dist.CLIENT)
			public BipedModel getArmorModel(LivingEntity living, ItemStack stack, EquipmentSlotType slot, BipedModel defaultModel) {
				BipedModel armorModel = new BipedModel(1);
				armorModel.bipedBody = new ModelHazmatChest().body;
				armorModel.bipedLeftArm = new ModelHazmatChest().left_arm;
				armorModel.bipedRightArm = new ModelHazmatChest().right_arm;
				armorModel.isSneak = living.isSneaking();
				armorModel.isSitting = defaultModel.isSitting;
				armorModel.isChild = living.isChild();
				return armorModel;
			}

			@Override
			public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
				return "scp_additions:textures/entities/hazmat.png";
			}
		}.setRegistryName("hazmat_suit_chestplate"));
		elements.items.add(() -> new ArmorItem(armormaterial, EquipmentSlotType.LEGS, new Item.Properties().group(SCPAdditionsItemGroup.tab)) {
			@Override
			@OnlyIn(Dist.CLIENT)
			public BipedModel getArmorModel(LivingEntity living, ItemStack stack, EquipmentSlotType slot, BipedModel defaultModel) {
				BipedModel armorModel = new BipedModel(1);
				armorModel.bipedLeftLeg = new ModelHazmatLeg().left_leg;
				armorModel.bipedRightLeg = new ModelHazmatLeg().right_leg;
				armorModel.isSneak = living.isSneaking();
				armorModel.isSitting = defaultModel.isSitting;
				armorModel.isChild = living.isChild();
				return armorModel;
			}

			@Override
			public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
				return "scp_additions:textures/entities/hazmat.png";
			}
		}.setRegistryName("hazmat_suit_leggings"));
		elements.items.add(() -> new ArmorItem(armormaterial, EquipmentSlotType.FEET, new Item.Properties().group(SCPAdditionsItemGroup.tab)) {
			@Override
			@OnlyIn(Dist.CLIENT)
			public BipedModel getArmorModel(LivingEntity living, ItemStack stack, EquipmentSlotType slot, BipedModel defaultModel) {
				BipedModel armorModel = new BipedModel(1);
				armorModel.bipedLeftLeg = new ModelHazmatBoots().left_leg;
				armorModel.bipedRightLeg = new ModelHazmatBoots().right_leg;
				armorModel.isSneak = living.isSneaking();
				armorModel.isSitting = defaultModel.isSitting;
				armorModel.isChild = living.isChild();
				return armorModel;
			}

			@Override
			public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
				return "scp_additions:textures/entities/hazmat.png";
			}
		}.setRegistryName("hazmat_suit_boots"));
	}

	// Made with Blockbench 4.8.3
	// Exported for Minecraft version 1.15 - 1.16 with MCP mappings
	// Paste this class into your mod and generate all required imports
	public static class ModelHazmatLeg extends EntityModel<Entity> {
		private final ModelRenderer steve;
		private final ModelRenderer right_leg;
		private final ModelRenderer left_leg;

		public ModelHazmatLeg() {
			textureWidth = 128;
			textureHeight = 128;
			steve = new ModelRenderer(this);
			steve.setRotationPoint(-8.0F, 16.0F, 8.0F);
			right_leg = new ModelRenderer(this);
			right_leg.setRotationPoint(6.0F, -3.5F, -8.0F);
			steve.addChild(right_leg);
			right_leg.setTextureOffset(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.5F, 4.0F, 0.0F, false);
			left_leg = new ModelRenderer(this);
			left_leg.setRotationPoint(10.0F, -3.5F, -8.0F);
			steve.addChild(left_leg);
			left_leg.setTextureOffset(24, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.5F, 4.0F, 0.0F, false);
		}

		@Override
		public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue,
				float alpha) {
			steve.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		}

		public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
			modelRenderer.rotateAngleX = x;
			modelRenderer.rotateAngleY = y;
			modelRenderer.rotateAngleZ = z;
		}

		public void setRotationAngles(Entity e, float f, float f1, float f2, float f3, float f4) {
			this.left_leg.rotateAngleX = MathHelper.cos(f * 1.0F) * -1.0F * f1;
			this.right_leg.rotateAngleX = MathHelper.cos(f * 1.0F) * 1.0F * f1;
		}
	}

	// Made with Blockbench 4.8.3
	// Exported for Minecraft version 1.15 - 1.16 with MCP mappings
	// Paste this class into your mod and generate all required imports
	public static class ModelHazmatHelmet extends EntityModel<Entity> {
		private final ModelRenderer steve;
		private final ModelRenderer body;
		private final ModelRenderer head;
		private final ModelRenderer filer_r1;
		private final ModelRenderer filer_r2;

		public ModelHazmatHelmet() {
			textureWidth = 128;
			textureHeight = 128;
			steve = new ModelRenderer(this);
			steve.setRotationPoint(-8.0F, 16.0F, 8.0F);
			body = new ModelRenderer(this);
			body.setRotationPoint(8.0F, 8.0F, -8.0F);
			steve.addChild(body);
			head = new ModelRenderer(this);
			head.setRotationPoint(0.0F, 0.0F, 0.0F);
			head.setTextureOffset(0, 0).addBox(-4.0F, -7.5F, -4.0F, 8.0F, 8.0F, 8.0F, 0.0F, false);
			head.setTextureOffset(0, 60).addBox(-2.25F, -1.25F, -5.0F, 5.0F, 2.0F, 2.0F, 0.0F, false);
			head.setTextureOffset(0, 57).addBox(-0.75F, -2.5F, -5.0F, 2.0F, 1.0F, 2.0F, 0.0F, false);
			head.setTextureOffset(1, 49).addBox(-1.0F, -1.25F, -5.75F, 2.0F, 2.0F, 1.0F, 0.0F, false);
			head.setTextureOffset(22, 42).addBox(-5.0F, -9.5F, -6.6F, 10.0F, 11.0F, 10.9F, 0.0F, false);
			filer_r1 = new ModelRenderer(this);
			filer_r1.setRotationPoint(0.0F, 0.0F, 0.0F);
			head.addChild(filer_r1);
			setRotationAngle(filer_r1, 0.2746F, 0.6541F, 0.1317F);
			filer_r1.setTextureOffset(0, 0).addBox(0.8245F, -1.3599F, -6.5459F, 1.0F, 1.0F, 2.0F, 0.0F, false);
			filer_r2 = new ModelRenderer(this);
			filer_r2.setRotationPoint(0.0F, 0.0F, 0.0F);
			head.addChild(filer_r2);
			setRotationAngle(filer_r2, 0.2542F, -0.6331F, -0.1349F);
			filer_r2.setTextureOffset(0, 0).addBox(-1.2755F, -1.2599F, -6.4459F, 1.0F, 1.0F, 2.0F, 0.0F, false);
		}

		@Override
		public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue,
				float alpha) {
			steve.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
			head.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		}

		public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
			modelRenderer.rotateAngleX = x;
			modelRenderer.rotateAngleY = y;
			modelRenderer.rotateAngleZ = z;
		}

		public void setRotationAngles(Entity e, float f, float f1, float f2, float f3, float f4) {
			this.head.rotateAngleY = f3 / (180F / (float) Math.PI);
			this.head.rotateAngleX = f4 / (180F / (float) Math.PI);
			this.filer_r1.rotateAngleY = f3 / (180F / (float) Math.PI);
			this.filer_r1.rotateAngleX = f4 / (180F / (float) Math.PI);
			this.filer_r2.rotateAngleY = f3 / (180F / (float) Math.PI);
			this.filer_r2.rotateAngleX = f4 / (180F / (float) Math.PI);
		}
	}

	// Made with Blockbench 4.8.3
	// Exported for Minecraft version 1.15 - 1.16 with MCP mappings
	// Paste this class into your mod and generate all required imports
	public static class ModelHazmatBoots extends EntityModel<Entity> {
		private final ModelRenderer steve;
		private final ModelRenderer right_leg;
		private final ModelRenderer left_leg;

		public ModelHazmatBoots() {
			textureWidth = 128;
			textureHeight = 128;
			steve = new ModelRenderer(this);
			steve.setRotationPoint(-8.0F, 16.0F, 8.0F);
			right_leg = new ModelRenderer(this);
			right_leg.setRotationPoint(6.0F, -3.5F, -8.0F);
			steve.addChild(right_leg);
			right_leg.setTextureOffset(6, 50).addBox(-2.0F, 9.5F, -2.0F, 4.0F, 2.5F, 4.0F, 0.0F, false);
			left_leg = new ModelRenderer(this);
			left_leg.setRotationPoint(10.0F, -3.5F, -8.0F);
			steve.addChild(left_leg);
			left_leg.setTextureOffset(6, 50).addBox(-2.0F, 9.5F, -2.0F, 4.0F, 2.5F, 4.0F, 0.0F, true);
		}

		@Override
		public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue,
				float alpha) {
			steve.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		}

		public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
			modelRenderer.rotateAngleX = x;
			modelRenderer.rotateAngleY = y;
			modelRenderer.rotateAngleZ = z;
		}

		public void setRotationAngles(Entity e, float f, float f1, float f2, float f3, float f4) {
			this.left_leg.rotateAngleX = MathHelper.cos(f * 1.0F) * -1.0F * f1;
			this.right_leg.rotateAngleX = MathHelper.cos(f * 1.0F) * 1.0F * f1;
		}
	}

	// Made with Blockbench 4.8.3
	// Exported for Minecraft version 1.15 - 1.16 with MCP mappings
	// Paste this class into your mod and generate all required imports
	public static class ModelHazmatChest extends EntityModel<Entity> {
		private final ModelRenderer steve;
		private final ModelRenderer body;
		private final ModelRenderer body_r8_r1;
		private final ModelRenderer body_r7_r1;
		private final ModelRenderer body_r6_r1;
		private final ModelRenderer body_r5_r1;
		private final ModelRenderer body_r4_r1;
		private final ModelRenderer body_r3_r1;
		private final ModelRenderer body_r2_r1;
		private final ModelRenderer body_r1_r1;
		private final ModelRenderer body2;
		private final ModelRenderer body_r1;
		private final ModelRenderer body_r2;
		private final ModelRenderer body_r3;
		private final ModelRenderer body_r4;
		private final ModelRenderer body_r5;
		private final ModelRenderer body_r6;
		private final ModelRenderer body_r7;
		private final ModelRenderer body_r8;
		private final ModelRenderer right_arm;
		private final ModelRenderer left_arm;

		public ModelHazmatChest() {
			textureWidth = 128;
			textureHeight = 128;
			steve = new ModelRenderer(this);
			steve.setRotationPoint(0.0F, 24.0F, 0.0F);
			body = new ModelRenderer(this);
			body.setRotationPoint(0.0F, -23.5F, 0.0F);
			steve.addChild(body);
			body.setTextureOffset(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, 0.0F, false);
			body.setTextureOffset(0, 16).addBox(-3.4F, 1.0F, 0.7F, 7.0F, 10.0F, 4.0F, 0.0F, false);
			body_r8_r1 = new ModelRenderer(this);
			body_r8_r1.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body_r8_r1);
			setRotationAngle(body_r8_r1, 1.9608F, -0.0024F, -0.1379F);
			body_r8_r1.setTextureOffset(34, 35).addBox(2.3F, -4.0F, -10.1F, 1.0F, 5.0F, 1.0F, 0.0F, false);
			body_r7_r1 = new ModelRenderer(this);
			body_r7_r1.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body_r7_r1);
			setRotationAngle(body_r7_r1, 0.0F, 0.0F, -1.0908F);
			body_r7_r1.setTextureOffset(34, 34).addBox(-5.7F, 6.5F, 3.6F, 1.0F, 1.0F, 1.0F, 0.0F, false);
			body_r6_r1 = new ModelRenderer(this);
			body_r6_r1.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body_r6_r1);
			setRotationAngle(body_r6_r1, 0.9163F, 0.0F, 0.0F);
			body_r6_r1.setTextureOffset(34, 34).addBox(3.6F, 3.5F, -8.2F, 1.0F, 3.0F, 1.0F, 0.0F, false);
			body_r5_r1 = new ModelRenderer(this);
			body_r5_r1.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body_r5_r1);
			setRotationAngle(body_r5_r1, -0.6109F, 0.0F, 0.0F);
			body_r5_r1.setTextureOffset(34, 34).addBox(3.9F, 6.6F, 9.1F, 1.0F, 5.0F, 1.0F, 0.0F, false);
			body_r4_r1 = new ModelRenderer(this);
			body_r4_r1.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body_r4_r1);
			setRotationAngle(body_r4_r1, 0.0F, 0.0F, -0.7854F);
			body_r4_r1.setTextureOffset(34, 34).addBox(-5.2F, 9.2F, 3.6F, 1.0F, 2.0F, 1.0F, 0.0F, false);
			body_r3_r1 = new ModelRenderer(this);
			body_r3_r1.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body_r3_r1);
			setRotationAngle(body_r3_r1, 0.7418F, 0.0F, 0.0F);
			body_r3_r1.setTextureOffset(34, 35).addBox(3.9F, 6.9F, -9.3F, 1.0F, 5.0F, 1.0F, 0.0F, false);
			body_r2_r1 = new ModelRenderer(this);
			body_r2_r1.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body_r2_r1);
			setRotationAngle(body_r2_r1, 0.0F, 0.0F, -0.6109F);
			body_r2_r1.setTextureOffset(34, 34).addBox(-2.0F, 8.4F, -2.2F, 1.0F, 1.0F, 1.0F, 0.0F, false);
			body_r1_r1 = new ModelRenderer(this);
			body_r1_r1.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body_r1_r1);
			setRotationAngle(body_r1_r1, 0.0F, 0.0F, -0.5236F);
			body_r1_r1.setTextureOffset(34, 34).addBox(-2.3F, 10.1F, -2.1F, 1.0F, 2.0F, 1.0F, 0.0F, false);
			body2 = new ModelRenderer(this);
			body2.setRotationPoint(0.0F, -0.5F, 0.0F);
			body.addChild(body2);
			body_r1 = new ModelRenderer(this);
			body_r1.setRotationPoint(0.0F, 12.0F, 0.0F);
			body2.addChild(body_r1);
			setRotationAngle(body_r1, 0.0835F, -1.4176F, -0.942F);
			body_r2 = new ModelRenderer(this);
			body_r2.setRotationPoint(0.0F, 12.0F, 0.0F);
			body2.addChild(body_r2);
			setRotationAngle(body_r2, 0.6117F, 0.6002F, -0.6779F);
			body_r3 = new ModelRenderer(this);
			body_r3.setRotationPoint(0.0F, 12.0F, 0.0F);
			body2.addChild(body_r3);
			setRotationAngle(body_r3, -0.4363F, 0.0F, 0.0F);
			body_r4 = new ModelRenderer(this);
			body_r4.setRotationPoint(0.0F, 10.3F, 0.0F);
			body2.addChild(body_r4);
			setRotationAngle(body_r4, 0.0835F, -1.4176F, -0.942F);
			body_r5 = new ModelRenderer(this);
			body_r5.setRotationPoint(0.0F, 10.3F, 0.0F);
			body2.addChild(body_r5);
			setRotationAngle(body_r5, -0.6981F, 0.0F, 0.0F);
			body_r6 = new ModelRenderer(this);
			body_r6.setRotationPoint(0.0F, 10.3F, 0.0F);
			body2.addChild(body_r6);
			setRotationAngle(body_r6, 0.829F, 0.0F, 0.0F);
			body_r7 = new ModelRenderer(this);
			body_r7.setRotationPoint(0.0F, 10.3F, 0.0F);
			body2.addChild(body_r7);
			setRotationAngle(body_r7, 0.6117F, 0.6002F, -0.6779F);
			body_r8 = new ModelRenderer(this);
			body_r8.setRotationPoint(0.0F, 12.0F, 0.0F);
			body2.addChild(body_r8);
			setRotationAngle(body_r8, 0.829F, 0.0F, 0.0F);
			right_arm = new ModelRenderer(this);
			right_arm.setRotationPoint(-5.0F, 2.5F, 0.0F);
			right_arm.setTextureOffset(16, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);
			left_arm = new ModelRenderer(this);
			left_arm.setRotationPoint(5.0F, 2.5F, 0.0F);
			left_arm.setTextureOffset(32, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);
		}

		@Override
		public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue,
				float alpha) {
			steve.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
			right_arm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
			left_arm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		}

		public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
			modelRenderer.rotateAngleX = x;
			modelRenderer.rotateAngleY = y;
			modelRenderer.rotateAngleZ = z;
		}

		public void setRotationAngles(Entity e, float f, float f1, float f2, float f3, float f4) {
			this.right_arm.rotateAngleX = MathHelper.cos(f * 0.6662F + (float) Math.PI) * f1;
			this.left_arm.rotateAngleX = MathHelper.cos(f * 0.6662F) * f1;
		}
	}

}
