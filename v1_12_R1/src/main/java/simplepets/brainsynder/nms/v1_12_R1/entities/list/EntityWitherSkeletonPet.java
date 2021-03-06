package simplepets.brainsynder.nms.v1_12_R1.entities.list;

import net.minecraft.server.v1_12_R1.World;
import simplepets.brainsynder.api.Size;
import simplepets.brainsynder.api.entity.hostile.IEntityWitherSkeletonPet;
import simplepets.brainsynder.api.pet.IPet;

@Size(width = 0.7F, length = 2.4F)
public class EntityWitherSkeletonPet extends EntitySkeletonPet implements IEntityWitherSkeletonPet {
    public EntityWitherSkeletonPet(World world) {
        super(world);
    }
    public EntityWitherSkeletonPet(World world, IPet pet) {
        super(world, pet);
    }
}
