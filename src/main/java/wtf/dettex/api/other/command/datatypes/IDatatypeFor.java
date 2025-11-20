package wtf.dettex.api.other.command.datatypes;

import wtf.dettex.api.other.command.exception.CommandException;

public interface IDatatypeFor<T> extends IDatatype  {
    T get(IDatatypeContext datatypeContext) throws CommandException;
}

