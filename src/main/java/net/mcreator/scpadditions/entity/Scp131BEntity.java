package net.mcreator.scpadditions.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Scp131BEntity extends AbstractScp131Entity {
    public Scp131BEntity(EntityType<? extends Scp131BEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String scpName() {
        return "SCP-131-B";
    }

    @Override
    public String textureName() {
        return "scp131b";
    }
}
