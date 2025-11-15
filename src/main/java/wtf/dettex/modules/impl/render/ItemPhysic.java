package wtf.dettex.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;
import wtf.dettex.common.util.other.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemPhysic extends Module {
    public static ItemPhysic getInstance() {
        return Instance.get(ItemPhysic.class);
    }

    public ItemPhysic() {
        super("ItemPhysic", "Item Physic", ModuleCategory.RENDER);
    }
}
