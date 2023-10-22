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
	public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red,
			float green, float blue, float alpha) {
		steve.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.rotateAngleX = x;
		modelRenderer.rotateAngleY = y;
		modelRenderer.rotateAngleZ = z;
	}

	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity e) {
		this.left_leg.rotateAngleX = MathHelper.cos(f * 1.0F) * -1.0F * f1;
		this.right_leg.rotateAngleX = MathHelper.cos(f * 1.0F) * 1.0F * f1;
	}
}