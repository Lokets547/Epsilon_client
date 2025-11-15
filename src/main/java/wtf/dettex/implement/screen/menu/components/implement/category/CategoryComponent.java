package wtf.dettex.implement.screen.menu.components.implement.category;

import net.minecraft.client.gui.DrawContext;
import wtf.dettex.Main;
import wtf.dettex.implement.screen.menu.components.AbstractComponent;
import wtf.dettex.implement.screen.menu.components.implement.module.ModuleComponent;
import wtf.dettex.modules.api.Module;
import wtf.dettex.modules.api.ModuleCategory;

import java.util.ArrayList;
import java.util.List;

public abstract class CategoryComponent extends AbstractComponent {
    protected final List<ModuleComponent> moduleComponents = new ArrayList<>();
    protected final ModuleCategory category;
    protected final Main main = Main.getInstance();

    protected CategoryComponent(ModuleCategory category) {
        this.category = category;
        initializeModules();
    }

    private void initializeModules() {
        List<Module> modules = main
                .getModuleRepository()
                .modules();

        for (Module module : modules) {
            if (module.getCategory() == category) {
                moduleComponents.add(createModuleComponent(module));
            }
        }
    }

    protected ModuleComponent createModuleComponent(Module module) {
        return new ModuleComponent(module);
    }

    protected abstract void renderContent(DrawContext context, int mouseX, int mouseY, float delta);

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderContent(context, mouseX, mouseY, delta);
    }

    public ModuleCategory getCategory() {
        return category;
    }
}

