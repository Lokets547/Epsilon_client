package wtf.dettex.api.other.command.exception;

import wtf.dettex.api.other.command.ICommand;
import wtf.dettex.api.other.command.argument.ICommandArgument;
import wtf.dettex.common.QuickLogger;

import java.util.List;

public class CommandUnhandledException extends RuntimeException implements ICommandException, QuickLogger {

    public CommandUnhandledException(String message) {
        super(message);
    }

    public CommandUnhandledException(Throwable cause) {
        super(cause);
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
    }
}

