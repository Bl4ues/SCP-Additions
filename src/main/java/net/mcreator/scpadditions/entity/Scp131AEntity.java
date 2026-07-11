package net.mcreator.scpadditions.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Scp131AEntity extends AbstractScp131Entity {
    public Scp131AEntity(EntityType<? extends Scp131AEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String scpName() {
        return "SCP-131-A";
    }

    @Override
    public String textureName() {
        return "scp131a";
    }
}
