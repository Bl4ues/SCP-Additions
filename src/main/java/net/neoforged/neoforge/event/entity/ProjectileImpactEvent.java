package net.neoforged.neoforge.event.entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.Event;
public class ProjectileImpactEvent extends Event {
    private final Projectile projectile; private final HitResult hit;
    public ProjectileImpactEvent(Projectile projectile, HitResult hit) { this.projectile=projectile; this.hit=hit; }
    public Projectile getProjectile() { return projectile; }
    public HitResult getRayTraceResult() { return hit; }
}
