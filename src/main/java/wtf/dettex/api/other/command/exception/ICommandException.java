package wtf.dettex.api.other.command.exception;

import net.minecraft.util.Formatting;
import wtf.dettex.api.other.command.ICommand;
import wtf.dettex.api.other.command.argument.ICommandArgument;
import wtf.dettex.common.QuickLogger;

import java.util.List;

public interface ICommandException extends QuickLogger {

    String getMessage();

    default void handle(ICommand command, List<ICommandArgument> args) {
        logDirect(
                this.getMessage(),
                Formatting.RED
        );
    }
}

