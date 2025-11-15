package wtf.dettex.api.system.logger.implement;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import wtf.dettex.api.system.logger.Logger;

public class ConsoleLogger implements Logger {
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger("Dettex");

    @Override
    public void log(Object message) {
        logger.info("[De{}ttex{}DLC] {}", Formatting.BLUE, Formatting.RED, message);
    }

    @Override
    public void minecraftLog(Text... components) {

    }
}
