from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(
            f"Expected exactly one match in {path}, found {count}: {old[:100]!r}"
        )
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def patch_elevator_editor() -> None:
    path = (
        "src/main/java/net/mcreator/scpadditions/client/gui/"
        "ElevatorArrivalEditorScreen.java"
    )
    replace_once(
        path,
        "import net.minecraft.client.Minecraft;\n"
        "import net.minecraft.client.gui.GuiGraphics;\n",
        "import net.minecraft.client.Minecraft;\n"
        "import net.minecraft.client.gui.Font;\n"
        "import net.minecraft.client.gui.GuiGraphics;\n",
    )
    replace_once(
        path,
        "customZoneField = configureField(new EditBox(",
        "customZoneField = configureField(new CenteredEditBox(",
    )
    replace_once(
        path,
        "floorNumberField = configureField(new EditBox(",
        "floorNumberField = configureField(new CenteredEditBox(",
    )
    replace_once(
        path,
        "    private interface ExpandableSelector {\n",
        "    private static final class CenteredEditBox extends EditBox {\n"
        "        private CenteredEditBox(Font font, int x, int y, int width,\n"
        "                int height, Component message) {\n"
        "            super(font, x, y, width, height, message);\n"
        "        }\n\n"
        "        @Override\n"
        "        protected void renderWidget(GuiGraphics graphics, int mouseX,\n"
        "                int mouseY, float partialTick) {\n"
        "            int offset = Math.max(0, (getHeight() - 9) / 2);\n"
        "            graphics.pose().pushPose();\n"
        "            graphics.pose().translate(0.0F, offset, 0.0F);\n"
        "            super.renderWidget(graphics, mouseX, mouseY, partialTick);\n"
        "            graphics.pose().popPose();\n"
        "        }\n"
        "    }\n\n"
        "    private interface ExpandableSelector {\n",
    )


def patch_context_editor() -> None:
    path = (
        "src/main/java/com/bl4ues/scpinventory/client/gui/"
        "ContextAnchorEditorScreen.java"
    )
    replace_once(
        path,
        "import net.minecraft.client.Minecraft;\n"
        "import net.minecraft.client.gui.GuiGraphics;\n",
        "import net.minecraft.client.Minecraft;\n"
        "import net.minecraft.client.gui.Font;\n"
        "import net.minecraft.client.gui.GuiGraphics;\n",
    )
    for field in ("actionBox", "nameBox", "rangeBox"):
        replace_once(
            path,
            f"{field} = configureField(new EditBox(",
            f"{field} = configureField(new CenteredEditBox(",
        )
    replace_once(
        path,
        "    private enum ButtonStyle {\n",
        "    private static final class CenteredEditBox extends EditBox {\n"
        "        private CenteredEditBox(Font font, int x, int y, int width,\n"
        "                int height, Component message) {\n"
        "            super(font, x, y, width, height, message);\n"
        "        }\n\n"
        "        @Override\n"
        "        protected void renderWidget(GuiGraphics graphics, int mouseX,\n"
        "                int mouseY, float partialTick) {\n"
        "            int offset = Math.max(0, (getHeight() - 9) / 2);\n"
        "            graphics.pose().pushPose();\n"
        "            graphics.pose().translate(0.0F, offset, 0.0F);\n"
        "            super.renderWidget(graphics, mouseX, mouseY, partialTick);\n"
        "            graphics.pose().popPose();\n"
        "        }\n"
        "    }\n\n"
        "    private enum ButtonStyle {\n",
    )


def patch_context_warning_detection() -> None:
    path = (
        "src/main/java/com/bl4ues/scpinventory/context/"
        "ContextConfigManager.java"
    )
    replace_once(
        path,
        "import net.minecraft.world.level.block.Block;\n"
        "import net.minecraft.world.level.block.state.BlockState;\n",
        "import net.minecraft.world.level.block.Block;\n"
        "import net.minecraft.world.level.block.state.BlockBehaviour;\n"
        "import net.minecraft.world.level.block.state.BlockState;\n",
    )
    replace_once(
        path,
        "    private static boolean likelySupportsRightClick(BlockState state) {\n"
        "        if (state == null || state.isAir()) return false;\n"
        "        try {\n"
        "            return state.getBlock().getClass().getMethod(\"use\",\n"
        "                    BlockState.class, Level.class, BlockPos.class,\n"
        "                    Player.class, InteractionHand.class,\n"
        "                    BlockHitResult.class).getDeclaringClass() != Block.class;\n"
        "        } catch (ReflectiveOperationException ignored) {\n"
        "            // Avoid blocking valid custom blocks when another mod changes the\n"
        "            // implementation shape in a way reflection cannot inspect.\n"
        "            return true;\n"
        "        }\n"
        "    }\n",
        "    private static boolean likelySupportsRightClick(BlockState state) {\n"
        "        if (state == null || state.isAir()) return false;\n"
        "        try {\n"
        "            Class<?> declaring = state.getBlock().getClass().getMethod(\"use\",\n"
        "                    BlockState.class, Level.class, BlockPos.class,\n"
        "                    Player.class, InteractionHand.class,\n"
        "                    BlockHitResult.class).getDeclaringClass();\n"
        "            return declaring != Block.class\n"
        "                    && declaring != BlockBehaviour.class;\n"
        "        } catch (ReflectiveOperationException ignored) {\n"
        "            // Avoid blocking valid custom blocks when another mod changes the\n"
        "            // implementation shape in a way reflection cannot inspect.\n"
        "            return true;\n"
        "        }\n"
        "    }\n",
    )


def patch_item_label() -> None:
    replace_once(
        "src/main/java/com/bl4ues/scpinventory/item/ScpItemType.java",
        '    ACCESSORY_HAND("Accessory"),\n',
        '    ACCESSORY_HAND("Accessory (Offhand)"),\n',
    )


def patch_scp330_orientation() -> None:
    path = "src/main/java/net/mcreator/scpadditions/client/Scp330Client.java"
    replace_once(
        path,
        "import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;\n",
        "import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;\n"
        "import com.mojang.blaze3d.vertex.PoseStack;\n"
        "import com.mojang.math.Axis;\n",
    )
    replace_once(
        path,
        "import net.minecraft.client.Minecraft;\n",
        "import net.minecraft.client.Minecraft;\n"
        "import net.minecraft.client.renderer.MultiBufferSource;\n",
    )
    replace_once(
        path,
        "import software.bernie.geckolib.core.animatable.model.CoreGeoBone;\n"
        "import software.bernie.geckolib.core.animation.AnimationState;\n",
        "",
    )
    replace_once(
        path,
        "\n        @Override\n"
        "        public void setCustomAnimations(Scp330BlockEntity animatable,\n"
        "                long instanceId, AnimationState<Scp330BlockEntity> state) {\n"
        "            super.setCustomAnimations(animatable, instanceId, state);\n"
        "            CoreGeoBone root = getAnimationProcessor().getBone(\"scp330\");\n"
        "            if (root == null || !animatable.getBlockState().hasProperty(Scp330Block.FACING)) return;\n"
        "            float rotation = switch (animatable.getBlockState().getValue(Scp330Block.FACING)) {\n"
        "                case NORTH -> (float) Math.PI;\n"
        "                case EAST -> (float) (Math.PI / 2.0D);\n"
        "                case WEST -> (float) (-Math.PI / 2.0D);\n"
        "                default -> 0.0F;\n"
        "            };\n"
        "            root.setRotY(rotation);\n"
        "        }\n",
        "",
    )
    replace_once(
        path,
        "    private static final class Renderer extends GeoBlockRenderer<Scp330BlockEntity> {\n"
        "        private Renderer() {\n"
        "            super(new Model());\n"
        "        }\n"
        "    }\n",
        "    private static final class Renderer extends GeoBlockRenderer<Scp330BlockEntity> {\n"
        "        private Renderer() {\n"
        "            super(new Model());\n"
        "        }\n\n"
        "        @Override\n"
        "        public void render(Scp330BlockEntity animatable, float partialTick,\n"
        "                PoseStack poseStack, MultiBufferSource bufferSource,\n"
        "                int packedLight, int packedOverlay) {\n"
        "            poseStack.pushPose();\n"
        "            if (animatable.getBlockState().hasProperty(Scp330Block.FACING)) {\n"
        "                float rotation = switch (animatable.getBlockState()\n"
        "                        .getValue(Scp330Block.FACING)) {\n"
        "                    case NORTH -> 180.0F;\n"
        "                    case EAST -> 90.0F;\n"
        "                    case WEST -> -90.0F;\n"
        "                    default -> 0.0F;\n"
        "                };\n"
        "                poseStack.translate(0.5D, 0.0D, 0.5D);\n"
        "                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));\n"
        "                poseStack.translate(-0.5D, 0.0D, -0.5D);\n"
        "            }\n"
        "            super.render(animatable, partialTick, poseStack, bufferSource,\n"
        "                    packedLight, packedOverlay);\n"
        "            poseStack.popPose();\n"
        "        }\n"
        "    }\n",
    )


def patch_station_tooltip() -> None:
    path = (
        "src/main/java/net/mcreator/scpadditions/facility/elevator/"
        "CoreRoomElevatorModule.java"
    )
    replace_once(
        path,
        "            tooltip.add(Component.translatable(tooltipKey)\n"
        "                    .withStyle(ChatFormatting.GRAY));\n",
        "            tooltip.add(Component.translatable(tooltipKey)\n"
        "                    .withStyle(ChatFormatting.GRAY));\n"
        "            if (getBlock() == STATION.get()) {\n"
        "                tooltip.add(Component.literal(\n"
        "                        \"Use a Screwdriver to edit this floor's arrival display.\")\n"
        "                        .withStyle(ChatFormatting.DARK_GRAY));\n"
        "            }\n",
    )


def move_titillium_font() -> None:
    source = ROOT / (
        "src/main/resources/assets/scp_additions/"
        "titillium_web_regular.ttf"
    )
    destination = ROOT / (
        "src/main/resources/assets/scp_additions/font/"
        "titillium_web_regular.ttf"
    )
    destination.parent.mkdir(parents=True, exist_ok=True)
    if source.exists():
        source.replace(destination)
    elif not destination.exists():
        raise RuntimeError("Titillium Web font file was not found")


def main() -> None:
    patch_elevator_editor()
    patch_context_editor()
    patch_context_warning_detection()
    patch_item_label()
    patch_scp330_orientation()
    patch_station_tooltip()
    move_titillium_font()


if __name__ == "__main__":
    main()
