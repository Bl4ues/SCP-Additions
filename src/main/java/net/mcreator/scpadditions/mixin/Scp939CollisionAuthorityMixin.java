package net.mcreator.scpadditions.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Mixin;

/** Gives the heavy SCP-939 authority over ordinary entity-body collisions. */
@Mixin(value = Scp939Entity.class, remap = false)
public abstract class Scp939CollisionAuthorityMixin {
    /** Body contact from mobs/players must not shove the 939 around. */
    public boolean isPushable() {
        return false;
    }

    /**
     * Preserve the normal behaviour where the 939 pushes lighter entities away,
     * except for the player currently underneath it during the pin sequence.
     */
    protected void doPush(Entity other) {
        Scp939Entity self = (Scp939Entity) (Object) this;
        byte action = self.getAction();
        if (other instanceof Player
                && (action == Scp939Entity.ACTION_PIN_LAND
                || action == Scp939Entity.ACTION_MAUL)) {
            return;
        }
        other.push(self);
    }
}
