package wtf.dettex.api.other.command.datatypes;

import wtf.dettex.api.other.command.exception.CommandException;
import wtf.dettex.common.QuickImports;

import java.util.stream.Stream;

public interface IDatatype extends QuickImports {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
