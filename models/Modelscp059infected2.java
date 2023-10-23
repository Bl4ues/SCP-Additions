// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.15 - 1.16 with MCP mappings
// Paste this class into your mod and generate all required imports

public static class Modelscp059infected2 extends EntityModel<Entity> {
	private final ModelRenderer Head;
	private final ModelRenderer cube_r1;
	private final ModelRenderer cube_r2;
	private final ModelRenderer cube_r3;
	private final ModelRenderer cube_r4;
	private final ModelRenderer cube_r5;
	private final ModelRenderer Body;
	private final ModelRenderer cube_r6;
	private final ModelRenderer cube_r7;
	private final ModelRenderer RightArm;
	private final ModelRenderer cube_r8;
	private final ModelRenderer cube_r9;
	private final ModelRenderer cube_r10;
	private final ModelRenderer LeftArm;
	private final ModelRenderer cube_r11;
	private final ModelRenderer RightLeg;
	private final ModelRenderer cube_r12;
	private final ModelRenderer cube_r13;
	private final ModelRenderer LeftLeg;
	private final ModelRenderer cube_r14;

	public Modelscp059infected2() {
		textureWidth = 64;
		textureHeight = 64;

		Head = new ModelRenderer(this);
		Head.setRotationPoint(0.0F, 0.0F, 0.0F);

		cube_r1 = new ModelRenderer(this);
		cube_r1.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r1);
		setRotationAngle(cube_r1, -2.9408F, -0.0017F, -0.096F);
		cube_r1.setTextureOffset(-1, -1).addBox(-1.8F, 25.75F, -9.15F, 2.6F, 4.95F, 2.45F, 0.0F, false);

		cube_r2 = new ModelRenderer(this);
		cube_r2.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r2);
		setRotationAngle(cube_r2, -0.0174F, -0.0017F, -0.096F);
		cube_r2.setTextureOffset(-1, -1).addBox(-1.8F, -27.75F, -4.75F, 2.0F, 3.95F, 2.45F, 0.0F, false);

		cube_r3 = new ModelRenderer(this);
		cube_r3.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r3);
		setRotationAngle(cube_r3, -0.0173F, 0.0023F, 0.1309F);
		cube_r3.setTextureOffset(-1, -1).addBox(-2.6F, -28.85F, 1.45F, 2.6F, 4.95F, 2.45F, 0.0F, false);

		cube_r4 = new ModelRenderer(this);
		cube_r4.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r4);
		setRotationAngle(cube_r4, -0.5708F, -0.1714F, -0.1322F);
		cube_r4.setTextureOffset(0, 0).addBox(-1.5F, -26.95F, -18.65F, 3.3F, 5.95F, 1.45F, 0.0F, false);

		cube_r5 = new ModelRenderer(this);
		cube_r5.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r5);
		setRotationAngle(cube_r5, 0.0176F, 0.0023F, 0.1309F);
		cube_r5.setTextureOffset(-2, -2).addBox(-2.4F, -32.55F, -3.75F, 3.3F, 5.95F, 3.15F, 0.0F, false);

		Body = new ModelRenderer(this);
		Body.setRotationPoint(0.0F, 0.0F, 0.0F);

		cube_r6 = new ModelRenderer(this);
		cube_r6.setRotationPoint(0.0F, 24.0F, 0.0F);
		Body.addChild(cube_r6);
		setRotationAngle(cube_r6, 0.0176F, 0.002F, 0.1483F);
		cube_r6.setTextureOffset(-1, -1).addBox(-5.0F, -23.05F, 0.45F, 4.3F, 8.15F, 2.45F, 0.0F, false);

		cube_r7 = new ModelRenderer(this);
		cube_r7.setRotationPoint(0.0F, 24.0F, 0.0F);
		Body.addChild(cube_r7);
		setRotationAngle(cube_r7, 0.0177F, -0.0005F, 0.2879F);
		cube_r7.setTextureOffset(-1, -1).addBox(-4.2F, -17.65F, -2.05F, 2.8F, 4.95F, 2.45F, 0.0F, false);

		RightArm = new ModelRenderer(this);
		RightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

		cube_r8 = new ModelRenderer(this);
		cube_r8.setRotationPoint(5.0F, 22.0F, 0.0F);
		RightArm.addChild(cube_r8);
		setRotationAngle(cube_r8, -0.3491F, 0.0F, 0.0F);
		cube_r8.setTextureOffset(-1, -1).addBox(-8.1F, -20.35F, -5.75F, 2.0F, 4.95F, 2.55F, 0.0F, false);

		cube_r9 = new ModelRenderer(this);
		cube_r9.setRotationPoint(5.0F, 22.0F, 0.0F);
		RightArm.addChild(cube_r9);
		setRotationAngle(cube_r9, 0.0873F, 0.0F, 0.0F);
		cube_r9.setTextureOffset(0, 0).addBox(-7.5F, -23.55F, -0.45F, 1.3F, 7.85F, 1.25F, 0.0F, false);
		cube_r9.setTextureOffset(0, 0).addBox(-7.0F, -24.35F, -0.15F, 2.0F, 4.95F, 1.25F, 0.0F, false);

		cube_r10 = new ModelRenderer(this);
		cube_r10.setRotationPoint(5.0F, 22.0F, 0.0F);
		RightArm.addChild(cube_r10);
		setRotationAngle(cube_r10, 0.3054F, 0.0F, 0.0F);
		cube_r10.setTextureOffset(0, 0).addBox(-9.0F, -20.75F, 4.25F, 2.6F, 4.25F, 1.25F, 0.0F, false);

		LeftArm = new ModelRenderer(this);
		LeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);

		cube_r11 = new ModelRenderer(this);
		cube_r11.setRotationPoint(-5.0F, 22.0F, 0.0F);
		LeftArm.addChild(cube_r11);
		setRotationAngle(cube_r11, -0.0175F, 0.0F, 0.0F);
		cube_r11.setTextureOffset(-1, -1).addBox(3.5F, -16.75F, -2.35F, 2.5F, 4.95F, 2.45F, 0.0F, false);
		cube_r11.setTextureOffset(-3, -3).addBox(6.1F, -19.15F, -2.35F, 2.1F, 4.95F, 4.35F, 0.0F, false);

		RightLeg = new ModelRenderer(this);
		RightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);

		cube_r12 = new ModelRenderer(this);
		cube_r12.setRotationPoint(0.6F, 19.7F, 0.5F);
		RightLeg.addChild(cube_r12);
		setRotationAngle(cube_r12, 0.0173F, 0.0038F, 0.0436F);
		cube_r12.setTextureOffset(0, 0).addBox(-4.3F, -13.85F, -2.55F, 2.0F, 6.35F, 1.55F, 0.0F, false);

		cube_r13 = new ModelRenderer(this);
		cube_r13.setRotationPoint(0.6F, 19.7F, 0.5F);
		RightLeg.addChild(cube_r13);
		setRotationAngle(cube_r13, 0.0176F, 0.0023F, 0.1309F);
		cube_r13.setTextureOffset(-1, -1).addBox(-5.2F, -18.65F, -2.55F, 3.8F, 4.95F, 2.45F, 0.0F, false);

		LeftLeg = new ModelRenderer(this);
		LeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);

		cube_r14 = new ModelRenderer(this);
		cube_r14.setRotationPoint(-1.9F, 12.0F, 0.0F);
		LeftLeg.addChild(cube_r14);
		setRotationAngle(cube_r14, 0.0156F, 0.0084F, -0.2356F);
		cube_r14.setTextureOffset(-1, -1).addBox(4.1F, -7.15F, -2.45F, 2.1F, 4.95F, 2.75F, 0.0F, false);
	}

	@Override
	public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red,
			float green, float blue, float alpha) {
		Head.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		Body.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		RightArm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		LeftArm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		RightLeg.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		LeftLeg.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.rotateAngleX = x;
		modelRenderer.rotateAngleY = y;
		modelRenderer.rotateAngleZ = z;
	}

	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity e) {
		this.RightArm.rotateAngleX = MathHelper.cos(f * 0.6662F + (float) Math.PI) * f1;
		this.LeftLeg.rotateAngleX = MathHelper.cos(f * 1.0F) * -1.0F * f1;
		this.Head.rotateAngleY = f3 / (180F / (float) Math.PI);
		this.Head.rotateAngleX = f4 / (180F / (float) Math.PI);
		this.LeftArm.rotateAngleX = MathHelper.cos(f * 0.6662F) * f1;
		this.RightLeg.rotateAngleX = MathHelper.cos(f * 1.0F) * 1.0F * f1;
	}
}