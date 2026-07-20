package net.neoforged.neoforge.client.event;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
public final class ComputeFovModifierEvent extends Event {
    private final Player player; private final float fovModifier; private float newFovModifier;
    public ComputeFovModifierEvent(Player player,float modifier){this.player=player;fovModifier=modifier;newFovModifier=modifier;}
    public Player getPlayer(){return player;} public float getFovModifier(){return fovModifier;}
    public float getNewFovModifier(){return newFovModifier;} public void setNewFovModifier(float value){newFovModifier=value;}
}
