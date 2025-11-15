package wtf.dettex.api.other.command.manager;

import net.minecraft.util.Pair;
import wtf.dettex.api.other.command.ICommand;
import wtf.dettex.api.other.command.argument.ICommandArgument;
import wtf.dettex.api.other.command.registry.Registry;

import java.util.List;
import java.util.stream.Stream;

public interface ICommandManager {
    Registry<ICommand> getRegistry();

    ICommand getCommand(String name);

    boolean execute(String string);

    boolean execute(Pair<String, List<ICommandArgument>> expanded);

    Stream<String> tabComplete(Pair<String, List<ICommandArgument>> expanded);

    Stream<String> tabComplete(String prefix);
}
