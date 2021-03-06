package simplepets.brainsynder.menu.menuItems;

import simple.brainsynder.api.ItemBuilder;
import simplepets.brainsynder.api.entity.IEntityPet;
import simplepets.brainsynder.api.entity.ambient.IEntityArmorStandPet;
import simplepets.brainsynder.menu.menuItems.base.MenuItemAbstract;
import simplepets.brainsynder.pet.PetDefault;
import simplepets.brainsynder.utils.ValueType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ValueType(type = "boolean", def = "false")
public class Clone extends MenuItemAbstract {

    public Clone(PetDefault type, IEntityPet entityPet) {
        super(type, entityPet);
    }
    public Clone(PetDefault type) {
        super(type);
    }

    @Override
    public ItemBuilder getItem() {
        ItemBuilder item = type.getDataItemByName("clone", 0);
        if (item != null) {
            if (entityPet instanceof IEntityArmorStandPet) {
                IEntityArmorStandPet var = (IEntityArmorStandPet) entityPet;
                item.withName(item.getName().replace("%value%", String.valueOf(var.isOwner())));
            }
        }
        return item;
    }

    @Override
    public List<ItemBuilder> getDefaultItems() {
        ItemBuilder item = ItemBuilder.getSkull(simple.brainsynder.utils.SkullType.PLAYER);
        item.withName("&6IsClone: &e%value%");
        return new ArrayList<>(Collections.singleton(item));
    }

    @Override
    public void onLeftClick() {
        if (entityPet instanceof IEntityArmorStandPet) {
            IEntityArmorStandPet bat = (IEntityArmorStandPet) entityPet;
            if (bat.isOwner()) {
                bat.setOwner(false);
            } else {
                bat.setOwner(true);
            }
        }
    }
}
