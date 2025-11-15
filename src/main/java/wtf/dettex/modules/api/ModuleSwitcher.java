package wtf.dettex.modules.api;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Formatting;
import wtf.dettex.modules.api.exception.ModuleException;
import wtf.dettex.event.EventManager;
import wtf.dettex.event.EventHandler;
import wtf.dettex.common.QuickImports;
import wtf.dettex.event.impl.keyboard.KeyEvent;
import wtf.dettex.api.system.logger.implement.ConsoleLogger;
import wtf.dettex.common.QuickLogger;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleSwitcher implements QuickLogger, QuickImports {
    List<Module> modules;

    public ModuleSwitcher(List<Module> modules, EventManager eventManager) {
        this.modules = modules;
        eventManager.register(this);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        for (Module module : modules) {
            if (event.key() == module.getKey() && mc.currentScreen == null) {
                try {
                    handleModuleState(module, event.action());
                } catch (Exception e) {
                    handleException(module.getName(), e);
                }
            }
        }
    }

    private void handleModuleState(Module module, int action) {
        if (module.getType() == 1 && action == 1) {
            module.switchState();
        }
    }

    private void handleException(String moduleName, Exception e) {
        final ConsoleLogger consoleLogger = new ConsoleLogger();

        if (e instanceof ModuleException) {
            logDirect("[" + moduleName + "] " + Formatting.RED + e.getMessage());
        } else {
            consoleLogger.log("Error in module " + moduleName + ": " + e.getMessage());
        }
    }
}
