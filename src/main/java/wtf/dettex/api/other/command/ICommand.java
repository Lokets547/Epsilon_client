package wtf.dettex.api.other.command;

import wtf.dettex.api.other.command.argument.IArgConsumer;
import wtf.dettex.api.other.command.exception.CommandException;
import wtf.dettex.common.QuickLogger;

import java.util.List;
import java.util.stream.Stream;

public interface ICommand extends QuickLogger {
    void execute(String label, IArgConsumer args) throws CommandException;

    Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException;

    String getShortDesc();

    List<String> getLongDesc();

    List<String> getNames();

    default boolean hiddenFromHelp() {
        return false;
    }
}

