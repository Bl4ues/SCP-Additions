// Made with Blockbench 4.8.3
// Exported for Minecraft version 1.15 - 1.16 with MCP mappings
// Paste this class into your mod and generate all required imports

public static class Modelscp059infected3 extends EntityModel<Entity> {
	private final ModelRenderer Head;
	private final ModelRenderer cube_r1;
	private final ModelRenderer cube_r2;
	private final ModelRenderer cube_r3;
	private final ModelRenderer cube_r4;
	private final ModelRenderer cube_r5;
	private final ModelRenderer cube_r6;
	private final ModelRenderer Body;
	private final ModelRenderer cube_r7;
	private final ModelRenderer cube_r8;
	private final ModelRenderer cube_r9;
	private final ModelRenderer cube_r10;
	private final ModelRenderer cube_r11;
	private final ModelRenderer RightArm;
	private final ModelRenderer cube_r12;
	private final ModelRenderer cube_r13;
	private final ModelRenderer cube_r14;
	private final ModelRenderer cube_r15;
	private final ModelRenderer cube_r16;
	private final ModelRenderer LeftArm;
	private final ModelRenderer cube_r17;
	private final ModelRenderer cube_r18;
	private final ModelRenderer RightLeg;
	private final ModelRenderer cube_r19;
	private final ModelRenderer cube_r20;
	private final ModelRenderer cube_r21;
	private final ModelRenderer LeftLeg;
	private final ModelRenderer cube_r22;
	private final ModelRenderer cube_r23;
	private final ModelRenderer cube_r24;

	public Modelscp059infected3() {
		textureWidth = 64;
		textureHeight = 64;

		Head = new ModelRenderer(this);
		Head.setRotationPoint(0.0F, 0.0F, 0.0F);
		Head.setTextureOffset(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, 0.0F, false);

		cube_r1 = new ModelRenderer(this);
		cube_r1.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r1);
		setRotationAngle(cube_r1, -2.9408F, -0.0017F, -0.096F);
		cube_r1.setTextureOffset(-2, -2).addBox(-0.5F, 27.9F, -10.75F, 5.0F, 4.0F, 3.0F, 0.0F, false);
		cube_r1.setTextureOffset(-1, -1).addBox(-1.2F, 26.7F, -9.15F, 2.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r2 = new ModelRenderer(this);
		cube_r2.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r2);
		setRotationAngle(cube_r2, -0.0174F, -0.0017F, -0.096F);
		cube_r2.setTextureOffset(-1, -1).addBox(-1.8F, -27.8F, -4.75F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r3 = new ModelRenderer(this);
		cube_r3.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r3);
		setRotationAngle(cube_r3, -0.0173F, 0.0023F, 0.1309F);
		cube_r3.setTextureOffset(-1, -1).addBox(-2.0F, -27.9F, 1.45F, 2.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r4 = new ModelRenderer(this);
		cube_r4.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r4);
		setRotationAngle(cube_r4, -0.5708F, -0.1714F, -0.1322F);
		cube_r4.setTextureOffset(0, 0).addBox(-1.2F, -26.0F, -18.65F, 3.0F, 5.0F, 1.0F, 0.0F, false);

		cube_r5 = new ModelRenderer(this);
		cube_r5.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r5);
		setRotationAngle(cube_r5, -0.5933F, 0.0023F, 0.1309F);
		cube_r5.setTextureOffset(0, 0).addBox(-6.7F, -28.7F, -18.45F, 3.0F, 5.0F, 1.0F, 0.0F, false);

		cube_r6 = new ModelRenderer(this);
		cube_r6.setRotationPoint(0.0F, 24.0F, 0.0F);
		Head.addChild(cube_r6);
		setRotationAngle(cube_r6, 0.0176F, 0.0023F, 0.1309F);
		cube_r6.setTextureOffset(-5, -5).addBox(-2.1F, -31.6F, -3.75F, 3.0F, 5.0F, 6.0F, 0.0F, false);

		Body = new ModelRenderer(this);
		Body.setRotationPoint(0.0F, 0.0F, 0.0F);
		Body.setTextureOffset(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, 0.0F, false);

		cube_r7 = new ModelRenderer(this);
		cube_r7.setRotationPoint(0.0F, 24.0F, 0.0F);
		Body.addChild(cube_r7);
		setRotationAngle(cube_r7, -0.1592F, 0.009F, -0.2705F);
		cube_r7.setTextureOffset(-1, -1).addBox(1.4F, -24.9F, -2.05F, 5.0F, 11.0F, 2.0F, 0.0F, false);

		cube_r8 = new ModelRenderer(this);
		cube_r8.setRotationPoint(0.0F, 24.0F, 0.0F);
		Body.addChild(cube_r8);
		setRotationAngle(cube_r8, 0.0176F, 0.002F, 0.1483F);
		cube_r8.setTextureOffset(-1, -1).addBox(-5.7F, -22.9F, 0.45F, 5.0F, 11.0F, 2.0F, 0.0F, false);

		cube_r9 = new ModelRenderer(this);
		cube_r9.setRotationPoint(0.0F, 24.0F, 0.0F);
		Body.addChild(cube_r9);
		setRotationAngle(cube_r9, 0.0177F, -0.0005F, 0.2879F);
		cube_r9.setTextureOffset(-1, -1).addBox(-3.4F, -16.7F, -2.05F, 2.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r10 = new ModelRenderer(this);
		cube_r10.setRotationPoint(0.0F, 24.0F, 0.0F);
		Body.addChild(cube_r10);
		setRotationAngle(cube_r10, 0.0159F, 0.0079F, -0.2007F);
		cube_r10.setTextureOffset(-1, -1).addBox(-0.5F, -17.7F, -2.55F, 3.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r11 = new ModelRenderer(this);
		cube_r11.setRotationPoint(0.0F, 24.0F, 0.0F);
		Body.addChild(cube_r11);
		setRotationAngle(cube_r11, -0.0173F, 0.0023F, 0.1309F);
		cube_r11.setTextureOffset(-1, -1).addBox(-4.0F, -21.8F, -2.85F, 4.0F, 4.0F, 2.0F, 0.0F, false);

		RightArm = new ModelRenderer(this);
		RightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
		RightArm.setTextureOffset(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);

		cube_r12 = new ModelRenderer(this);
		cube_r12.setRotationPoint(5.0F, 22.0F, 0.0F);
		RightArm.addChild(cube_r12);
		setRotationAngle(cube_r12, 0.0436F, 0.0F, 0.0F);
		cube_r12.setTextureOffset(0, 0).addBox(-8.1F, -15.9F, 1.55F, 4.0F, 4.0F, 1.0F, 0.0F, false);

		cube_r13 = new ModelRenderer(this);
		cube_r13.setRotationPoint(5.0F, 22.0F, 0.0F);
		RightArm.addChild(cube_r13);
		setRotationAngle(cube_r13, -0.3491F, 0.0F, 0.0F);
		cube_r13.setTextureOffset(-1, -1).addBox(-8.1F, -19.4F, -5.75F, 2.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r14 = new ModelRenderer(this);
		cube_r14.setRotationPoint(5.0F, 22.0F, 0.0F);
		RightArm.addChild(cube_r14);
		setRotationAngle(cube_r14, 0.0873F, 0.0F, 0.0F);
		cube_r14.setTextureOffset(0, 0).addBox(-7.2F, -22.7F, -0.45F, 3.0F, 7.0F, 1.0F, 0.0F, false);
		cube_r14.setTextureOffset(0, 0).addBox(-7.0F, -23.4F, -0.15F, 2.0F, 4.0F, 1.0F, 0.0F, false);

		cube_r15 = new ModelRenderer(this);
		cube_r15.setRotationPoint(5.0F, 22.0F, 0.0F);
		RightArm.addChild(cube_r15);
		setRotationAngle(cube_r15, -0.192F, 0.0F, 0.0F);
		cube_r15.setTextureOffset(-2, -2).addBox(-8.0F, -23.2F, -5.35F, 4.0F, 2.0F, 3.0F, 0.0F, false);

		cube_r16 = new ModelRenderer(this);
		cube_r16.setRotationPoint(5.0F, 22.0F, 0.0F);
		RightArm.addChild(cube_r16);
		setRotationAngle(cube_r16, 0.3054F, 0.0F, 0.0F);
		cube_r16.setTextureOffset(0, 0).addBox(-6.8F, -24.7F, 4.25F, 2.0F, 4.0F, 1.0F, 0.0F, false);
		cube_r16.setTextureOffset(0, 0).addBox(-8.4F, -20.5F, 4.25F, 2.0F, 4.0F, 1.0F, 0.0F, false);

		LeftArm = new ModelRenderer(this);
		LeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
		LeftArm.setTextureOffset(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);

		cube_r17 = new ModelRenderer(this);
		cube_r17.setRotationPoint(-5.0F, 22.0F, 0.0F);
		LeftArm.addChild(cube_r17);
		setRotationAngle(cube_r17, -0.0175F, 0.0F, 0.0F);
		cube_r17.setTextureOffset(-1, -1).addBox(5.3F, -19.9F, -2.95F, 3.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r17.setTextureOffset(-1, -1).addBox(5.2F, -23.4F, -0.65F, 3.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r17.setTextureOffset(-1, -1).addBox(4.0F, -15.8F, -2.35F, 2.0F, 4.0F, 2.0F, 0.0F, false);
		cube_r17.setTextureOffset(-3, -3).addBox(6.2F, -18.2F, -2.35F, 2.0F, 4.0F, 4.0F, 0.0F, false);

		cube_r18 = new ModelRenderer(this);
		cube_r18.setRotationPoint(-5.0F, 22.0F, 0.0F);
		LeftArm.addChild(cube_r18);
		setRotationAngle(cube_r18, -0.1047F, 0.0F, 0.0F);
		cube_r18.setTextureOffset(-3, -3).addBox(4.8F, -23.6F, -4.65F, 2.0F, 4.0F, 4.0F, 0.0F, false);

		RightLeg = new ModelRenderer(this);
		RightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
		RightLeg.setTextureOffset(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);

		cube_r19 = new ModelRenderer(this);
		cube_r19.setRotationPoint(0.6F, 19.7F, 0.5F);
		RightLeg.addChild(cube_r19);
		setRotationAngle(cube_r19, 0.0173F, 0.0038F, 0.0436F);
		cube_r19.setTextureOffset(0, 0).addBox(-4.3F, -13.5F, -2.55F, 2.0F, 6.0F, 1.0F, 0.0F, false);

		cube_r20 = new ModelRenderer(this);
		cube_r20.setRotationPoint(0.6F, 19.7F, 0.5F);
		RightLeg.addChild(cube_r20);
		setRotationAngle(cube_r20, 0.0163F, 0.007F, -0.1483F);
		cube_r20.setTextureOffset(-1, -1).addBox(0.1F, -12.1F, -2.55F, 3.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r21 = new ModelRenderer(this);
		cube_r21.setRotationPoint(0.6F, 19.7F, 0.5F);
		RightLeg.addChild(cube_r21);
		setRotationAngle(cube_r21, 0.0176F, 0.0023F, 0.1309F);
		cube_r21.setTextureOffset(-1, -1).addBox(-4.4F, -17.7F, -2.55F, 3.0F, 4.0F, 2.0F, 0.0F, false);

		LeftLeg = new ModelRenderer(this);
		LeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
		LeftLeg.setTextureOffset(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);

		cube_r22 = new ModelRenderer(this);
		cube_r22.setRotationPoint(-1.9F, 12.0F, 0.0F);
		LeftLeg.addChild(cube_r22);
		setRotationAngle(cube_r22, 0.0176F, 0.0023F, 0.1309F);
		cube_r22.setTextureOffset(-3, -3).addBox(-1.3F, -12.8F, -2.05F, 3.0F, 10.0F, 4.0F, 0.0F, false);

		cube_r23 = new ModelRenderer(this);
		cube_r23.setRotationPoint(-1.9F, 12.0F, 0.0F);
		LeftLeg.addChild(cube_r23);
		setRotationAngle(cube_r23, 0.0176F, 0.002F, 0.1483F);
		cube_r23.setTextureOffset(-3, -3).addBox(0.9F, -4.4F, -2.65F, 2.0F, 4.0F, 4.0F, 0.0F, false);

		cube_r24 = new ModelRenderer(this);
		cube_r24.setRotationPoint(-1.9F, 12.0F, 0.0F);
		LeftLeg.addChild(cube_r24);
		setRotationAngle(cube_r24, 0.0156F, 0.0084F, -0.2356F);
		cube_r24.setTextureOffset(-3, -3).addBox(4.2F, -6.2F, -2.45F, 2.0F, 4.0F, 4.0F, 0.0F, false);
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