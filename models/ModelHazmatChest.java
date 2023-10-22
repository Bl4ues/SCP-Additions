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
	public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red,
			float green, float blue, float alpha) {
		steve.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.rotateAngleX = x;
		modelRenderer.rotateAngleY = y;
		modelRenderer.rotateAngleZ = z;
	}

	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity e) {
		this.right_arm.rotateAngleX = MathHelper.cos(f * 0.6662F + (float) Math.PI) * f1;
		this.left_arm.rotateAngleX = MathHelper.cos(f * 0.6662F) * f1;
	}
}