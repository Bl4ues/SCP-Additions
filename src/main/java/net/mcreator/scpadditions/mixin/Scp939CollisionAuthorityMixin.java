package net.mcreator.scpadditions.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Mixin;

/** Gives the heavy SCP-939 authority over ordinary entity-body collisions. */
@Mixin(value = Scp939Entity.class, remap = false)
public abstract class Scp939CollisionAuthorityMixin extends PathfinderMob {
    protected Scp939CollisionAuthorityMixin(
            EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Body contact from mobs/players must not shove the 939 around. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * Preserve normal 939-to-entity body pushing, except for the victim already
     * underneath it during pin/maul. That pair must be allowed to overlap.
     */
    @Override
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
