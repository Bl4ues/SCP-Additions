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
	public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red,
			float green, float blue, float alpha) {
		steve.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
		head.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.rotateAngleX = x;
		modelRenderer.rotateAngleY = y;
		modelRenderer.rotateAngleZ = z;
	}

	public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity e) {
		this.head.rotateAngleY = f3 / (180F / (float) Math.PI);
		this.head.rotateAngleX = f4 / (180F / (float) Math.PI);
		this.filer_r1.rotateAngleY = f3 / (180F / (float) Math.PI);
		this.filer_r1.rotateAngleX = f4 / (180F / (float) Math.PI);
		this.filer_r2.rotateAngleY = f3 / (180F / (float) Math.PI);
		this.filer_r2.rotateAngleX = f4 / (180F / (float) Math.PI);
	}
}