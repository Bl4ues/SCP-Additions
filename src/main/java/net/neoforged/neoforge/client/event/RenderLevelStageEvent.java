package net.neoforged.neoforge.client.event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.neoforged.bus.api.Event;
public final class RenderLevelStageEvent extends Event {
    public enum Stage { AFTER_SOLID_BLOCKS }
    private final Stage stage; private final PoseStack poseStack; private final Camera camera;
    public RenderLevelStageEvent(Stage stage, PoseStack poseStack, Camera camera){this.stage=stage;this.poseStack=poseStack;this.camera=camera;}
    public Stage getStage(){return stage;} public PoseStack getPoseStack(){return poseStack;} public Camera getCamera(){return camera;}
}
