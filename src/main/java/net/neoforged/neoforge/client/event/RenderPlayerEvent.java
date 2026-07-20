package net.neoforged.neoforge.client.event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.bus.api.Event;
public class RenderPlayerEvent extends Event {
    private final AbstractClientPlayer entity; private final PlayerRenderer renderer;
    private final float partialTick; private final PoseStack poseStack;
    private final MultiBufferSource buffers; private final int packedLight;
    protected RenderPlayerEvent(AbstractClientPlayer entity,PlayerRenderer renderer,float partialTick,
            PoseStack poseStack,MultiBufferSource buffers,int packedLight){this.entity=entity;this.renderer=renderer;this.partialTick=partialTick;this.poseStack=poseStack;this.buffers=buffers;this.packedLight=packedLight;}
    public AbstractClientPlayer getEntity(){return entity;} public PlayerRenderer getRenderer(){return renderer;}
    public float getPartialTick(){return partialTick;} public PoseStack getPoseStack(){return poseStack;}
    public MultiBufferSource getMultiBufferSource(){return buffers;} public int getPackedLight(){return packedLight;}
    public static final class Pre extends RenderPlayerEvent { public Pre(AbstractClientPlayer e,PlayerRenderer r,float p,PoseStack s,MultiBufferSource b,int l){super(e,r,p,s,b,l);} }
    public static final class Post extends RenderPlayerEvent { public Post(AbstractClientPlayer e,PlayerRenderer r,float p,PoseStack s,MultiBufferSource b,int l){super(e,r,p,s,b,l);} }
}
